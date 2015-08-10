package com.example.lightweightcamera;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

public class Util {
    private static final String TAG = Util.class.getSimpleName();

    /**
     * A help method for saving the compressed picture data to SDCARD
     *
     * @param fullPathFileName
     * @param data
     * @throws IOException
     */
    public static void saveData(String fullPathFileName, byte[] data)
            throws IOException {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(String.format(fullPathFileName));
            outStream.write(data);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "(util) failed to write the file...");
            throw e;
        }
    }

    public static void addPictureToMediaStore(Context context, String saveDir,
            String fileName) {
        String currentDateAndTime = MainActivity.dateTimeStampFormat
                .format(Calendar.getInstance().getTime());
        // make sure that the pictures is visible in the gallery
        ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, fileName);
        values.put(Images.Media.DISPLAY_NAME, fileName);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATE_TAKEN, currentDateAndTime);
        values.put(Images.Media.DATE_MODIFIED, currentDateAndTime);
        values.put(Images.Media.DESCRIPTION, "photo taken with testCam");
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, saveDir + fileName);

        Uri uri = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        context.sendBroadcast(new Intent(Intent.ACTION_VIEW, uri));
    }

    /**
     * A help method for adding a video to media store
     *
     * @param context
     * @param saveDir
     * @param fileName
     */
    public static void addVideoToMediaStore(Context context, String saveDir,
            String fileName) {
        String currentDateAndTime = MainActivity.dateTimeStampFormat
                .format(Calendar.getInstance().getTime());
        ContentValues values = new ContentValues();
        values.put(Video.Media.TITLE, fileName);
        values.put(Video.Media.DISPLAY_NAME, fileName + ".mp4");
        values.put(Video.Media.MIME_TYPE, "video/mp4");
        values.put(Video.Media.DATE_TAKEN, currentDateAndTime);
        values.put(Video.Media.DATE_MODIFIED, currentDateAndTime);
        values.put(Video.Media.DESCRIPTION, "Video taken with testCam");
        values.put(Video.Media.DATA, saveDir + fileName + ".mp4");
        Uri uri = context.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI, values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                uri));
    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static Camera.Size choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return parms.getPreviewSize();
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
        return parms.getPreviewSize();
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }

}

