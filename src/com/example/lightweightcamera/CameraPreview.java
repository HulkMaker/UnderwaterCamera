package com.example.lightweightcamera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;


public class CameraPreview extends SurfaceView implements Callback {


    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private boolean isPreviewRunning = false;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        if(mCamera != null) {
            mSurfaceHolder = holder;
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
            } catch (Exception e) {
                //Log.d(TAG, "failed to setPreviewDisplay");
                e.printStackTrace();
            }
        }
    }

    private Camera.Size previewSize = null;
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mSurfaceHolder.getSurface() == null) {
            return;
        }
        mSurfaceHolder = holder;

        if(isPreviewRunning) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                //              Log.d(IceCamActivity.TAG, "stop preview threw an Exception");
                e.printStackTrace();
            }
        }

        try {
            // mCamera.setPreviewDisplay(mSurfaceHolder);

            Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            Util.choosePreviewSize(params, MainActivity.PREVIEW_WIDTH, MainActivity.PREVIEW_HEIGHT);

            mCamera.setParameters(params);

            previewSize = params.getPreviewSize();
            ((MainActivity) this.getContext()).changeSurfaceSize(mSurfaceHolder, this, previewSize.width, previewSize.height);

            mCamera.startPreview();

            ((MainActivity) this.getContext()).enableOneShotPreviewCallback(true);

            isPreviewRunning = true;
        } catch (Exception e) {
            //          Log.e(IceCamActivity.TAG, "Failed to set camera preview...");
            e.printStackTrace();
        }
    }

    //@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    public Camera.Size getPreviewSize() {
        return previewSize;
    }
}

