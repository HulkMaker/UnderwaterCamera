/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications Japan, Inc.
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
/**
 * @file NativeEffectEngine.java
 */
package com.sonyericsson.android.addoncamera.artfilter.effect;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * JNI for native effect engine.
 *
 */
public class NativeEffectEngine implements EffectEngine {

    static {
        System.loadLibrary("nativeeffectengine");
    }

    // Error codes for native library.

    // Throw exception according to error code.
    static void throwExceptionAccordingTo(int errorCode) {
        switch (errorCode) {
            case FX_ENGINE_OK:
                // NOP.
                return;

            case FX_ENGINE_DOPROCESS:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_DOPROCESS");
                break;

            case FX_ENGINE_ERROR_GENERAL_ERROR:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_GENERAL_ERROR");
                break;

            case FX_ENGINE_ERROR_PARAM:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_PARAM");
                break;

            case FX_ENGINE_ERROR_STATE:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_STATE");
                break;

            case FX_ENGINE_ERROR_MALLOC:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_MALLOC");
                break;

            case FX_ENGINE_ERROR_IO:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_IO");
                break;

            case FX_ENGINE_ERROR_UNSUPPORTED:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_UNSUPPORTED");
                break;

            case FX_ENGINE_ERROR_PROCESS:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_PROCESS");
                break;

            case FX_ENGINE_ERROR_UNKNOWN:
                Log.e("TraceLog", "NativeEffectEngine:FX_ENGINE_ERROR_UNKNOWN");
                break;

            default:
                Log.e("TraceLog", "NativeEffectEngine:UNEXPECTED");
                break;
        }

        throw new RuntimeException("ErrorCode:" + errorCode);
    }

    // This is ID of java instance.
    // JNI will identify the target java object by this integer.
    // At construction, this field is injected by native function.
    private int mEffectEngineHandler;

    // Effect name of this engine.
    private final EffectMode mEffect;

    // CONSTRUCTOR
    public NativeEffectEngine(EffectMode effect) {
        mEffect = EffectMode.NOEFFECT;

        // Create native instance.
        int ret = nativeCreateNativeObject();

        // Check.
        throwExceptionAccordingTo(ret);
    }

    /**
     * get effect name for CPU engine. CPU engine is used only pencil effect.
     *
     * @param effect
     * @return effect name definition for CPU engine
     */
    private String getEngineString(EffectMode effect) {
        return "NO_EFFECT";
    }
    /**
     * Create native instance
     *
     * @return native instance pointer.
     */
    private final native int nativeCreateNativeObject();

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#initialize(int, int)
     */
    @Override
    public final void initialize(String inputFormat, String outputFormat, int width, int height) {
        // Call native method.
        int ret = nativeInitialize(getEngineString(mEffect), inputFormat, outputFormat, width,
                height);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeInitialize(
            String effect, String inputFormat, String outputFormat, int width, int height);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter.effect.EffectEngineInterface#finish()
     */
    @Override
    public final void finish() {
        // Call native method.
        int ret = nativeFinish();
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeFinish();

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#setEffectParam(java.lang.String, int[])
     */
    @Override
    public final void setEffectParam(String effect, int[] param) {
        // Call native method.
        int ret = nativeSetEffectParam(getEngineString(mEffect), param);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeSetEffectParam(String effect, int[] param);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#createConvertedPreview(int, int, byte[], byte[])
     */
    @Override
    public final void convertByteArrayFromYuvToRgb888(
            int width, int height, byte[] inputYuv, byte[] outputRgb888) {
        // Call native method.
        int ret = nativeConvertByteArrayFromYuvToRgb888(
                width, height, inputYuv, outputRgb888);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeConvertByteArrayFromYuvToRgb888(
            int width, int height, byte[] inputYuv,
            byte[] outputRgb888);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#doEffectCameraPreview(byte[], android.graphics.Bitmap)
     */
    @Override
    public final void createEffectedBitmapFromRgb888ByteArray(
            byte[] inputRgb888, Bitmap outputBmp) {
        // Call native method.
        int ret = nativeCreateEffectedBitmapFromRgb888ByteArray(inputRgb888, outputBmp);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeCreateEffectedBitmapFromRgb888ByteArray(
            byte[] inputRgb888, Bitmap outputBmp);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#prepareOneShotEffectPicture(int, int)
     */
    @Override
    public final void prepareOneShotEffectPicture(int pictWidth, int pictHeight) {
        // Call native method.
        int ret = nativePrepareOneShotEffectPicture(pictWidth, pictHeight);

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativePrepareOneShotEffectPicture(int width, int height);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#releaseOneShotEffectPicture()
     */
    @Override
    public final void releaseOneShotEffectPicture() {
        // Call native method.
        int ret = nativeReleaseOneShotEffectPicture();

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeReleaseOneShotEffectPicture();

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#setOneShotEffectPictureElement(int[], int)
     */
    @Override
    public final void setOneShotEffectPictureElement(int[] pixels, int length) {
        // Call native method.
        int ret = nativeSetOneShotEffectPictureElement(pixels, length);

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeSetOneShotEffectPictureElement(int[] pixels, int length);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#doOneShotEffectPicture(android.graphics.Bitmap)
     */
    @Override
    public final void doOneShotEffectPicture(Bitmap output) {
        // Call native method.
        int ret = nativeDoOneShotEffectPicture(output);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeDoOneShotEffectPicture(
            Bitmap output);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#doResizeToPictureSize(
     *         android.graphics.Bitmap, android.graphics.Bitmap)
     */
    @Override
    public final void doResizeToPictureSize(Bitmap output, Bitmap previewImg) {
        // Call native method.
        int ret = nativeDoResizeToPictureSize(output,
                previewImg.getWidth(), previewImg.getHeight(), previewImg);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeDoResizeToPictureSize(Bitmap output, int width, int height,
            Bitmap img);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#prepareHarrisEffectPicture(int, int)
     */
    @Override
    public final void prepareHarrisEffectPicture(int pictWidth, int pictHeight) {
        // Call native method.
        int ret = nativePrepareHarrisEffectPicture(pictWidth, pictHeight);

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativePrepareHarrisEffectPicture(int pictWidth, int pictHeight);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#releaseHarrisEffectPicture()
     */
    @Override
    public final void releaseHarrisEffectPicture() {
        // Call native method.
        int ret = nativeReleaseHarrisEffectPicture();

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeReleaseHarrisEffectPicture();

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#addHarrisEffectPictureElement(int, int[], int)
     */
    @Override
    public final void addHarrisEffectPictureElement(int index, int[] pixels, int length) {
        // Call native method.
        int ret = nativeAddHarrisEffectPictureElement(index, pixels, length);

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeAddHarrisEffectPictureElement(
            int index, int[] pixels, int length);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#doHarrisEffectPicture(android.graphics.Bitmap)
     */
    @Override
    public final void doHarrisEffectPicture(Bitmap output) {
        // Call native method.
        int ret = nativeDoHarrisEffectPicture(output);

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeDoHarrisEffectPicture(Bitmap output);

    /* (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#doEffectOnPreview(byte[], android.graphics.Bitmap)
     */
    @Override
    public final void doEffectOnPreview(byte[] input, byte[] output,
            ImageFormatConvertor imgConvertor) {
        // Call native method.
        int ret = nativeDoEffectOnPreview(input, output, imgConvertor);
        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeDoEffectOnPreview(byte[] input, byte[] output,
            ImageFormatConvertor imgConvertor);

    /*
     * (non-Javadoc)
     * @see com.sonyericsson.android.addoncamera.artfilter
     * .effect.EffectEngineInterface#fetchRgbColorLevelOnPreviewFrame(int, int,
     * int, int[])
     */
    @Override
    public final void fetchRgbColorLevelOnPreviewFrame(
            int x, int y, int sampleSize, int[] rgbLevel) {
        // Call native method.
        int ret = nativeFetchRgbColorLevelOnPreviewFrame(x, y, sampleSize, rgbLevel);

        // Check.
        throwExceptionAccordingTo(ret);
    }

    private final native int nativeFetchRgbColorLevelOnPreviewFrame(
            int x, int y, int sampleSize, int[] rgbLevel);

    public static final native void nativeConvertByteArrayToBitmap(
            int width, int height, String format, byte[] input, Bitmap output);

    /**
     * Prepare image format converter.
     *
     * @param inputFormat
     * @param outputFormat
     * @param width
     * @param height
     * @return error code
     */
    public static final native int nativePrepareImageFormatConverter(
            String inputFormat, String outputFormat, int width, int height);

    /**
     * Do convert format.
     *
     * @param inputBuf
     * @param outputBuf
     * @param isMonoToneRequired
     * @return error code
     */
    public static final native int nativeDoConvertImageFormat(
            byte[] inputBuf, byte[] outputBuf, boolean isMonoToneRequired);

    /**
     * Release image format converter.
     *
     * @return error code
     */
    public static final native int nativeReleaseImageFormatConverter();

    /**
     * Resize YVU420SP frame.
     *
     * @param srcYvu
     * @param srcWidth
     * @param srcHeight
     * @param dstYvu
     * @param sampleSize
     */
    public static final native void nativeResizeYvu420sp(
            byte[] srcYvu,
            int srcWidth,
            int srcHeight,
            byte[] dstYvu,
            int sampleSize);

    /**
     * Get current effect parameters for Instrumentation test.
     * DON'T USE FOR PRODUCT.
     * */
    private final native int nativeForTestGetEffectParam(String effect, int[] params);

}
