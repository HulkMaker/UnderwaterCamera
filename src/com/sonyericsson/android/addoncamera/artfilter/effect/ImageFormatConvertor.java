/*
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
/**
 * @file ImageFormatConvertor.java
 */
package com.sonyericsson.android.addoncamera.artfilter.effect;

/**
 * JNI for native effect engine.
 *
 */
public class ImageFormatConvertor {

    static {
        System.loadLibrary("nativeeffectengine");
    }

    // This is ID of java instance.
    // JNI will identify the target java object by this integer.
    // At construction, this field is injected by native function.
    private int mEffectEngineHandler;

    /**
     * CONSTRUCTOR.
     *
     * @param inputFormat
     * @param outputFormat
     * @param width
     * @param height
     */
    public ImageFormatConvertor(
            String inputFormat,
            String outputFormat,
            int width,
            int height) {
        int ret;

        // Create native instance.
        ret = nativeCreateNativeObject();
        NativeEffectEngine.throwExceptionAccordingTo(ret);

        // Prepare.
        ret = prepare(inputFormat, outputFormat, width, height);
        NativeEffectEngine.throwExceptionAccordingTo(ret);
    }

    /**
     * Create native instance
     *
     * @return native instance pointer.
     */
    private final native int nativeCreateNativeObject();

    /**
     * Prepare image format converter.
     *
     * @param inputFormat
     * @param outputFormat
     * @param width
     * @param height
     * @return error code
     */
    public int prepare(
            String inputFormat,
            String outputFormat,
            int width,
            int height) {
        return nativePrepareImageFormatConverter(inputFormat, outputFormat, width, height);
    }

    private final native int nativePrepareImageFormatConverter(
            String inputFormat, String outputFormat, int width, int height);

    /**
     * Do convert format.
     *
     * @param inputBuf
     * @param outputBuf
     * @param isMonoToneRequired
     * @return error code
     */
    public int doConvertImageFormat(
            byte[] inputBuffer,
            byte[] outputBuffer) {
        return nativeDoConvertImageFormat(inputBuffer, outputBuffer, false);
    }

    private final native int nativeDoConvertImageFormat(
            byte[] inputBuf, byte[] outputBuf, boolean isMonoToneRequired);

    /**
     * Release image format converter.
     *
     * @return error code
     */
    public int release() {
        return nativeReleaseImageFormatConverter();
    }

    private final native int nativeReleaseImageFormatConverter();
}
