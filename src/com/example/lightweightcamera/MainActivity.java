
package com.example.lightweightcamera;

import android.graphics.*;
import android.media.CameraProfile;
import android.view.*;
import com.example.UnderwaterCamera.R;
import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.sonyericsson.android.addoncamera.artfilter.effect.EffectEngine;
import com.sonyericsson.android.addoncamera.artfilter.effect.NativeEffectEngine;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CameraPreview testCamPrev;
    private FrameLayout previewFrameLayout;
    private Context mContext;
    private Camera mCamera;
    private Object mCameraExtension;
    private TextView mTextView;
    private Button mCtrlBtn, mScreenShot, mPhotoShot;
    private ImageView mImageView1, mImageView2;

    private MediaRecorder mMediaRecorder;
    private boolean isRecording;

    public static SimpleDateFormat dateTimeStampFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSS");
    public static String saveDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/testCam/";
    public static final String TAG = "testCam";

    public static final String FRONT_CAM_ACTION = "com.sonymobile.testCamActivity.FRONT_CAM";
    public static final String BACK_CAM_ACTION = "com.sonymobile.testCamActivity.BACK_CAM";

    private String fileName;
    private String fileNamePrefix0 = "PIC_ORIGINAL_";
    private String fileNamePrefix1 = "PIC_PROCESSED_";
    private String fileNamePrefix2 = "PIC_COMPARE_";
    private boolean isTakingPic = false;
    private Object picTakeSyncObj = new Object();
    private Object picThreadSyncObj = new Object();
    private Object autoFocusSyncObj = new Object();
    private Object zoomSyncObj = new Object();
    private Object previewSyncObj = new Object();
    private int nrOfPics;
    private int camToUse = CameraInfo.CAMERA_FACING_BACK;   // set the back cam as default
    private boolean useAutoFocus;
    private long pictureSavingTime = 0;
    private byte[] mFirstPreviewFrame;
    private byte[] mPreviewFrame;

    // used when calculating the FPS
    private long mLastTime, now;
    private int frameSampleTime, frameSamplesCollected, fps, time;
    private ArrayList<Integer> collectedFpsRates;

    private Camera.PictureCallback mPictureJpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();

            pictureSavingTime = System.currentTimeMillis();
            fileName =  fileNamePrefix0 + dateTimeStampFormat.format(Calendar.getInstance().getTime()) + ".jpg";
            try {
                Util.saveData(saveDir + fileName, data);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Failed to save picture", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            Util.addPictureToMediaStore(mContext, saveDir, fileName);

            synchronized (picTakeSyncObj) {
                picTakeSyncObj.notifyAll();
            }
        }
    };

    private Camera.AutoFocusCallback mAutoFocusCallback = new  Camera.AutoFocusCallback() {
        public void onAutoFocus(final boolean success, Camera camera) {
            if(useAutoFocus) {
                synchronized (autoFocusSyncObj) {
                    autoFocusSyncObj.notifyAll();
                }
            }
        }
    };


    ////////////////////////////////////----------------------------------------------------------
    public void enableOneShotPreviewCallback(boolean enable) {
        if (mCamera == null) {
            return;
        }
        if (enable) {
            mCamera.setOneShotPreviewCallback(mPrevCallback);
        } else {
            mCamera.setPreviewCallback(null);
            mFirstPreviewFrame = null;
            mPreviewFrame = null;
        }
    }
    private Camera.PreviewCallback mPrevCallback = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            synchronized (previewSyncObj) {
                mPreviewFrame = data;
                if (mFirstPreviewFrame == null) {
                    mFirstPreviewFrame = data;
                }

                android.util.Log.d("TONY", "parse color+++++++++++++++++");
                // parse it and try to extract main color
                parse(data);
            }
        }
    };

    public static final int PREVIEW_WIDTH = 1280;
    public static final int PREVIEW_HEIGHT = 720;
    private static byte[] output  = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 3];

    // Get sample engine, used for YUV->RGB convert.
    private static final int[] dummyParam = {0};

    private static final NativeEffectEngine mFullColorEngineSample = new NativeEffectEngine(null);

    public interface PreviewFrameAnalysisCallback {
        public void onAnalysisCompleted(Bitmap original, Bitmap processing);
    }

    private PreviewFrameAnalysisCallback myCallback = new PreviewFrameAnalysisCallback() {
        @Override
        public void onAnalysisCompleted(final Bitmap original, final Bitmap processed) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (original != null) {
                        mImageView1.setImageBitmap(original);
                    }
                    if (processed != null) {
                        mImageView2.setImageBitmap(processed);
                    }
                    enableOneShotPreviewCallback(true);
                }
            });
        }
    };

    // Small culled out ratio.
    private static final int DEFAULT_SMALL_FRAME_CULLED_OUT_RATIO = 2;
    private static final int ONESHOT_SMALL_FRAME_CULLED_OUT_RATIO = 1;
    private static final int ENGINE_ACCEPTABLE_MIN_SIZE = 64;
    public int calcurateSmallFrameRatio(int width, int height) {
        int ratio;
        if (width / DEFAULT_SMALL_FRAME_CULLED_OUT_RATIO
                < ENGINE_ACCEPTABLE_MIN_SIZE
                || height / DEFAULT_SMALL_FRAME_CULLED_OUT_RATIO
                    < ENGINE_ACCEPTABLE_MIN_SIZE) {
            ratio = ONESHOT_SMALL_FRAME_CULLED_OUT_RATIO;
        } else {
            ratio = DEFAULT_SMALL_FRAME_CULLED_OUT_RATIO;
        }
        return ratio;
    }

    private int smallFrameCulledOutRatio = -1;
    private int scaledWidth = -1;
    private int scaledHeight = -1;
    private byte[] scaledYuv = null;
    private byte[] scaledRgb888 = null;
    private Bitmap bitmap = null;

    void parse(final byte[] yuv) {
        if (smallFrameCulledOutRatio == -1) {
            Size size = testCamPrev.getPreviewSize();
            smallFrameCulledOutRatio = calcurateSmallFrameRatio(size.width, size.height);
            scaledWidth = size.width; // size.width / smallFrameCulledOutRatio;
            scaledHeight = size.height; //size.height / smallFrameCulledOutRatio;
            scaledYuv = new byte[scaledWidth * scaledHeight * 3 / 2];
            scaledRgb888 = new byte[scaledWidth * scaledHeight * 3];
            bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        }

//        new Thread() {
//            @Override
//            public void run() {
                // color extractor
//                Size size = testCamPrev.getPreviewSize();
//                NativeEffectEngine.nativeResizeYvu420sp(yuv, size.width, size.height, scaledYuv, smallFrameCulledOutRatio);
//
//                mFullColorEngineSample.convertByteArrayFromYuvToRgb888(scaledWidth, scaledHeight, scaledYuv, scaledRgb888);
//
//                bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
//                NativeEffectEngine.nativeConvertByteArrayToBitmap(scaledWidth, scaledHeight,
//                        NativeEffectEngine.FX_RGB888, scaledRgb888, bitmap);
//
//                // parse the resize Bitmap here
//                Object result = bitmap;

                // underwater
//                Size size = testCamPrev.getPreviewSize();
                mFullColorEngineSample.convertByteArrayFromYuvToRgb888(scaledWidth, scaledHeight, yuv, scaledRgb888);
                NativeEffectEngine.nativeConvertByteArrayToBitmap(scaledWidth, scaledHeight,
                        NativeEffectEngine.FX_RGB888, scaledRgb888, bitmap);

                // split the full size bitmap into left and right
                Bitmap bmp1 = Bitmap.createBitmap(bitmap, 0, 0, scaledWidth / 2, scaledHeight);
                Bitmap bmp2 = Bitmap.createBitmap(bitmap, scaledWidth / 2, 0, scaledWidth / 2, scaledHeight);

                if (bmp1 != null && !bmp1.isMutable()) {
                    bmp1 = PhotoProcessing.makeBitmapMutable(bmp1);
                }
                bmp1 = PhotoProcessing.filterPhoto(bmp1, 2);


//                YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, size.width, size.height, null);//
//                fileName =  fileNamePrefix + size.width + "x" + size.height + ".jpg";//
//                FileOutputStream outStream = null;
//                try {
//                    outStream = new FileOutputStream(String.format(saveDir + fileName));
//                    yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, outStream);
//                } catch (IOException e) {
//                    Log.e(MainActivity.TAG, "(util) failed to write the file...");
//                }

                myCallback.onAnalysisCompleted(bmp1, bmp2);
//            }
//        }.start();
    }
    /////////////////////////////////////////////////////////////////////

    private Camera.PreviewCallback mPrevFpsCallback = new PreviewCallback() {
        // we don't get this call during video recording....
        public void onPreviewFrame(byte[] data, Camera camera) {
            // reuse the byte array
            camera.addCallbackBuffer(data);
            now = System.currentTimeMillis();
            if (mLastTime != 0) {
                //Time difference between now and last time we were here
                time = (int) (now - mLastTime);
                frameSampleTime += time;
                frameSamplesCollected++;
                //After 10 frames
                if (frameSamplesCollected == 10) {
                    //Update the fps variable
                    fps = (int) (10000 / frameSampleTime);
                    // store the value
                    collectedFpsRates.add(fps);
                    //Reset the sampletime + frames collected
                    frameSampleTime = 0;
                    frameSamplesCollected = 0;
                }
            }
            mLastTime = now;
        }
    };

    public byte[] getFirstPreviewFrame() {
        synchronized (previewSyncObj) {
            return mFirstPreviewFrame;
        }
    }

    public byte[] getPreviewFrame() {
        synchronized (previewSyncObj) {
            return mPreviewFrame;
        }
    }

    private Camera.OnZoomChangeListener mZoomChangeListener = new Camera.OnZoomChangeListener() {
        public void onZoomChange(final int zoomValue, boolean stopped, Camera camera) {
            Log.d(TAG, "zoomValue: " +zoomValue +", stopped: " +stopped);
            if (stopped) {
                synchronized (zoomSyncObj) {
                    zoomSyncObj.notifyAll();
                }
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewFrameLayout = (FrameLayout)findViewById(R.id.camera_preview);
        mContext = this.getApplication().getApplicationContext();

        Intent intent = getIntent();
        if(intent.getAction().equals(FRONT_CAM_ACTION))
            camToUse = CameraInfo.CAMERA_FACING_FRONT;
        else if(intent.getAction().equals(BACK_CAM_ACTION))
            camToUse = CameraInfo.CAMERA_FACING_BACK;

        camToUse = CameraInfo.CAMERA_FACING_BACK;

        File dir = new File(saveDir);
        if(!dir.exists())
            dir.mkdir();

        // mTextView = (TextView)findViewById(R.id.testCaseText);
        //mCtrlBtn = (Button)findViewById(R.id.buttin);
        mImageView1 = (ImageView) findViewById(R.id.im1);
        mImageView2 = (ImageView) findViewById(R.id.im2);

        mScreenShot = (Button) findViewById(R.id.screenshot);
        mPhotoShot = (Button) findViewById(R.id.photoshot);

        mScreenShot.setOnClickListener(new View.OnClickListener() {

                                           @Override
                                           public void onClick(View v) {
                                               synchronized (previewSyncObj) {
                                                   // split the full size bitmap into left and right
                                                   Bitmap bmp1 = Bitmap.createBitmap(bitmap, 0, 0, scaledWidth / 2, scaledHeight);
                                                   Bitmap bmp2 = Bitmap.createBitmap(bitmap, scaledWidth / 2, 0, scaledWidth / 2, scaledHeight);

                                                   if (bmp1 != null && !bmp1.isMutable()) {
                                                       bmp1 = PhotoProcessing.makeBitmapMutable(bmp1);
                                                   }
                                                   bmp1 = PhotoProcessing.filterPhoto(bmp1, 2);

                                                   Bitmap fullsize = bitmap.copy(bitmap.getConfig(), true);
                                                   Canvas canvas = new Canvas(fullsize);
                                                   canvas.drawBitmap(bmp1, 0, 0, new Paint());
                                                   canvas.drawBitmap(bmp2, bmp1.getWidth(), 0, new Paint());

                                                   fileName =  fileNamePrefix2 + dateTimeStampFormat.format(Calendar.getInstance().getTime()) + ".jpg";
                                                   FileOutputStream os = null;
                                                   try {
                                                       os = new FileOutputStream(saveDir + fileName);
                                                       fullsize.compress(Bitmap.CompressFormat.JPEG, 100, os);
                                                   } catch (IOException e) {
                                                       Toast.makeText(MainActivity.this, "Failed to save picture", Toast.LENGTH_LONG).show();
                                                       e.printStackTrace();
                                                   } finally {
                                                       try {
                                                           os.close();
                                                       } catch (IOException exc) {}
                                                   }
                                                   Util.addPictureToMediaStore(mContext, saveDir, fileName);

                                                   bmp1.recycle();
                                                   bmp2.recycle();
                                                   fullsize.recycle();
                                               }
                                           }
                                       });

        mPhotoShot.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                fileName =  fileNamePrefix0 + dateTimeStampFormat.format(Calendar.getInstance().getTime()) + ".jpg";
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(saveDir + fileName);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Failed to save picture", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } finally {
                    try {
                        os.close();
                    } catch (IOException exc) {}
                }
                Util.addPictureToMediaStore(mContext, saveDir, fileName);


                Bitmap processed = bitmap;
                if (bitmap != null && !bitmap.isMutable()) {
                    processed = PhotoProcessing.makeBitmapMutable(bitmap);
                }
                processed = PhotoProcessing.filterPhoto(processed, 2);

                fileName =  fileNamePrefix1 + dateTimeStampFormat.format(Calendar.getInstance().getTime()) + ".jpg";
                try {
                    os = new FileOutputStream(saveDir + fileName);
                    processed.compress(Bitmap.CompressFormat.JPEG, 100, os);
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Failed to save picture", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } finally {
                    try {
                        os.close();
                    } catch (IOException exc) {}
                }
                Util.addPictureToMediaStore(mContext, saveDir, fileName);
            }
        });

//        mCtrlBtn.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                if (isRecording) {
//                    MainActivity.this.stopVideoRecording();
//                    mCtrlBtn.setText("start");
//                } else {
//                    try {
//                        MainActivity.this.startVideoRecording(0, 0);
//                    } catch (IllegalStateException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                    mCtrlBtn.setText("stop");
//                }
//                enableOneShotPreviewCallback(true);
//                mCtrlBtn.setEnabled(false);
//            }
//
//        });

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int i) {
                        int height = getWindow().getDecorView().getHeight();
                        Log.i(TAG, "Current height: " + height);
                    }
                });
    }

    protected void onResume() {
        super.onResume();
        (getWindow()).addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startCamera(camToUse);
    }

    protected void onPause() {
        super.onPause();
        shutDownCurrentCameraInstance();

        //mFullColorEngineSample.finish();
    }

    protected void onDestroy(){
        super.onDestroy();
        previewFrameLayout = null;
    }

    private void startCamera(int cameraToOpen) throws RuntimeException {
        try {
            mCamera = Camera.open(cameraToOpen);
            //Class.forName("com.sonyericsson.cameraextension.CameraExtension");
            //mCameraExtension = com.sonyericsson.cameraextension.CameraExtension.open(mCamera, cameraToOpen);
        } catch (RuntimeException e) {
            Log.e(TAG, "(startCamera) Failed to open camera");
            throw e;
        }
//        catch (ClassNotFoundException e) {
//            Log.e(TAG, "Sony CameraExtension class not found");
//        }

        testCamPrev = new CameraPreview(MainActivity.this, mCamera);
        previewFrameLayout.addView(testCamPrev,0);
    }

    private void shutDownCurrentCameraInstance() {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.setOneShotPreviewCallback(null);
        mCamera.release();
        mCamera = null;
        previewFrameLayout.removeView(testCamPrev);

        if (mCameraExtension != null) {
            //((com.sonyericsson.cameraextension.CameraExtension)mCameraExtension).release();
            mCameraExtension = null;
        }
    }

    private void startFPSmeasuring() {
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        int prevFormat = mCamera.getParameters().getPreviewFormat();
        int bufferSize = size.width * size.height* ImageFormat.getBitsPerPixel(prevFormat)/8;
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.addCallbackBuffer(new byte[bufferSize]);
        mCamera.addCallbackBuffer(new byte[bufferSize]);
        mCamera.addCallbackBuffer(new byte[bufferSize]);
        mCamera.setPreviewCallbackWithBuffer(mPrevFpsCallback);
    }

    private void stopFPSmeasuring() {
        mCamera.setPreviewCallbackWithBuffer(null);
    }


    // --- Methods that can be invoked from Instrumentation ---

    public CameraInfo getCameraInfo(int cameraId) {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        return cameraInfo;
    }

    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public void setTestCaseText(final String text) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mTextView.setText(text);
            }
        });
    }

    public void setDisplayOrientation(int degrees) throws RuntimeException {
        mCamera.setDisplayOrientation(degrees);
    }

    /**
     * Adds a ZoomChangeListener to the camera instance
     */
    public void enableZoomListener() {
        mCamera.setZoomChangeListener(mZoomChangeListener);
    }

    /**
     * perform a smooth zoom to the specified zoom value
     * @param zoomValue - the zoom value to zoom in/out to
     * @throws RuntimeException - if the zoom value is illegal.
     */
    public void smoothZoomTo(int zoomValue) throws RuntimeException{
        mCamera.startSmoothZoom(zoomValue);
    }

    public long getPictureSavingTime()
    {
        return pictureSavingTime;
    }

    /**
     * returns the last pictures name
     */
    public String getLastPictureFileName() throws RuntimeException {
        if (fileName == null) {
            throw new RuntimeException("Picture file name is null");
        }
        return saveDir + fileName;
    }

    /**
     * returns the last video name
     */
    public String getLastVideoFileName() throws RuntimeException {
        if (fileName == null) {
            throw new RuntimeException("Video file name is null");
        }
        return saveDir + fileName + ".mp4";
    }
    /**
     * returns the save dir.
     */
    public String getSaveDir() {
        return saveDir ;
    }

    /**
     * Stops the preview
     */
    public void stopPreview() {
        mCamera.stopPreview();
    }

    /**
     * starts the preview
     */
    public void startPreview() {
        mCamera.startPreview();
    }

    /**
     * returns the current camera parameter
     * @return - current Camera.Parameter object
     */
    public Camera.Parameters getCurrentParameters() throws RuntimeException {
        Parameters params = mCamera.getParameters();
        if (params == null) {
            throw new RuntimeException("Parameter object null");
        }
        return params;
    }

    /**
     * set camera parameters
     * @param params - Camera.Parameter object to set
     * @throws RuntimeException - Throws RuntimeException if the parameters are invalid in some way
     */
    public void setCameraParameters(Camera.Parameters params) throws RuntimeException{
        mCamera.setParameters(params);
    }

    /**
     * Start and stops FPS measuring
     * @param enable - true for enabling FPS measuring, false  for disabling it
     */
    public void enableFpsMeasuring(boolean enable) {
        if(enable) {
            collectedFpsRates = new ArrayList<Integer>();
            startFPSmeasuring();
        } else {
            stopFPSmeasuring();
        }
    }

    public void enablePreviewCallback(boolean enable) {
        if (enable) {
            mCamera.setPreviewCallback(mPrevCallback);
        } else {
            mCamera.setPreviewCallback(null);
            mFirstPreviewFrame = null;
            mPreviewFrame = null;
        }
    }

    /**
     * get's the mean fps rate. You have to call enableFpsMeasuring(true) and let it collect data for
     * a while and then call enableFpsMeasuring(false) before use this method.
     * @return
     */
    public double getMeanFpsRate() {
        double meanValue = 0;
        if(collectedFpsRates.size()>0) {
            for(Integer i : collectedFpsRates) {
                meanValue = meanValue +i;
            }
            meanValue = meanValue/collectedFpsRates.size();
        }
        return meanValue;
    }

    /**
     * Turns on and off Auto Focus
     * @param enable - true enable AF, false disable AF
     */
    public void enableAutoFocus(boolean enable) {
        useAutoFocus = enable;
    }

    /**
     * takes pictures
     * @param nrOfPics - the number of pics to take
     */
    public void takePicture(final int nrOfPics) {
        this.nrOfPics = nrOfPics;
        if(!isTakingPic) {
            new Thread(new Runnable() {
                public void run() {
                    isTakingPic = true;
                    for(int i=0;i<nrOfPics;i++) {
                        if(useAutoFocus) {
                            synchronized (autoFocusSyncObj) {
                                mCamera.autoFocus(mAutoFocusCallback);
                                try {
                                    // wait for AF to complete
                                    autoFocusSyncObj.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        // don't know if we need to set the preview CB to null in Ice cream
                        mCamera.setPreviewCallback(null);    // this is needed since there is a bug in android framework (see Notes in DMS01055353)
                        mCamera.takePicture(null, null, mPictureJpegCallback);

                        synchronized (picTakeSyncObj) {
                            try {
                                Log.d(TAG, "wait for pic callback");
                                picTakeSyncObj.wait();
                            } catch (InterruptedException e) {
                                Log.e(TAG, "(takePicture) got interupted when waiting...");
                                e.printStackTrace();
                            }
                        }
                    }
                    isTakingPic = false;
                    synchronized (picThreadSyncObj) {
                        picThreadSyncObj.notifyAll();
                    }
                }
            }).start();

        }
    }

    /**
     * Waits until the picture take threads to complete. This waits maximum 10*nr_of_pics sec
     * @throws RuntimeException
     */
    public void waitForPicsToBeTaken() throws RuntimeException {
        int totWaitTime = 10 * 1000 * nrOfPics ;
        synchronized (picThreadSyncObj) {
            long startWaitTime = System.currentTimeMillis();
            try {
                picThreadSyncObj.wait(totWaitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if ((System.currentTimeMillis() - startWaitTime) >= totWaitTime) {
                throw new RuntimeException("Jpeg callback was not recieved. Failing test case.");
            }
        }
    }

    public void waitForZoomToStop() throws RuntimeException {
        int totWaitTime = 10 * 1000 ;
        synchronized (zoomSyncObj) {
            long startWaitTime = System.currentTimeMillis();
            try {
                zoomSyncObj.wait(totWaitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if ((System.currentTimeMillis() - startWaitTime) >= totWaitTime) {
                throw new RuntimeException("OnZoomChangeListener with stop never received. Failing test case.");
            }
        }
    }

    /**
     * Starts to record video with dimension width x height, or preview size
     * as dimension of 0 x 0 is used.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void startVideoRecording(int width, int heigth) throws IllegalStateException,
            IOException {
    if(!isRecording) {
            Camera.Size prevSize = mCamera.getParameters().getPreviewSize();
            if (width == 0 && heigth == 0) {
                width = prevSize.width;
                heigth = prevSize.height;
            }

            List<Size> videoSize = mCamera.getParameters().getSupportedVideoSizes();
            boolean found = false;
            for (Size size : videoSize) {
                if (width == size.width && heigth ==  size.height) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Toast.makeText(MainActivity.this, "current video size (" + width + "*" + heigth + ") is not supported.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            fileName =  fileNamePrefix0 + dateTimeStampFormat.format(Calendar.getInstance().getTime());
            mMediaRecorder = new MediaRecorder();
            mCamera.unlock();   // this should not be necessary in ice cream
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
            mMediaRecorder.setVideoSize(width,heigth);
            mMediaRecorder.setPreviewDisplay(testCamPrev.getSurfaceHolder().getSurface());
            mMediaRecorder.setOutputFile(saveDir + fileName + ".mp4");
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;
        }
    }

    /**
     * Stops an ongoing video recording and add the video to the gallery
     */
    public void stopVideoRecording() {
        if(isRecording) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            try {
                mCamera.reconnect();
            } catch (Exception e) {
                Log.e(TAG, "failed to reconnect to camera");
                e.printStackTrace();
            }
            Util.addVideoToMediaStore(this, saveDir, fileName);
            isRecording = false;
        }
    }

    public Camera getCamera(){
        return mCamera;
    }

    public boolean isUseSonyExtensionCamera() {
        return mCameraExtension != null;
    }

    public Object getCameraExtension() {
        return mCameraExtension;
    }

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;

    public void changeSurfaceSize(SurfaceHolder holder, SurfaceView view, int width, int height) {
        // get screen size
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        int dw = size.x > size.y ? size.x : size.y;
        int dh = size.x > size.y ? size.y : size.x;

        // calculate aspect ratio
        double ar = (double) width / (double) height;
        // calculate display aspect ratio
        double dar = (double) dw / (double) dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_FIT_HORIZONTAL:
                dh = (int) (dw / ar);
                break;
            case SURFACE_FIT_VERTICAL:
                dw = (int) (dh * ar);
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_ORIGINAL:
                dh = height;
                dw = width;
                break;
        }

        // holder.setFixedSize(width, height);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = dw;
        lp.height = dh;
        view.setLayoutParams(lp);
        view.invalidate();
    }
}
