/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications Japan, Inc.
 * Copyright (C) 2012 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
/**
 * @file EffectEngine.java
 *
 */

package com.sonyericsson.android.addoncamera.artfilter.effect;

import android.graphics.Bitmap;

/**
 * Effect engine interface.
 *
 */
public interface EffectEngine {

    // No error.
    public static final int FX_ENGINE_OK = 0x00000000;

    // Now on processing.
    public static final int FX_ENGINE_DOPROCESS = 0x00000001;

    // General error.
    public static final int FX_ENGINE_ERROR_GENERAL_ERROR = 0x80000000;

    // Invalid arguments.
    public static final int FX_ENGINE_ERROR_PARAM = 0x80000001;

    // Invalid state.
    public static final int FX_ENGINE_ERROR_STATE = 0x80000002;

    // Memory error.
    public static final int FX_ENGINE_ERROR_MALLOC = 0x80000004;

    // I/O error.
    public static final int FX_ENGINE_ERROR_IO = 0x80000008;

    // Not supported.
    public static final int FX_ENGINE_ERROR_UNSUPPORTED = 0x80000010;

    // cannt finish all process
    public static final int FX_ENGINE_ERROR_PROCESS = 0x80000020;

    // Unknown error.
    public static final int FX_ENGINE_ERROR_UNKNOWN = 0xC0000000;

    // Color format definitions.
    public static final String FX_RGB565 = "RGB565";
    public static final String FX_RGB888 = "RGB888";
    public static final String FX_YVU420SP = "YVU420_SEMIPLANAR";

    /**
     * Initialize native library.
     *
     * @param input image format
     * @param output image format
     * @param width of output image
     * @param height of output image
     * @return error code
     */
    public abstract void initialize(
            String inputFormat, String outputFormat, int width, int height);

    /**
     * Finalize native instance.
     *
     * @return error code.
     */
    public abstract void finish();

    /**
     * Set effect parameters.
     *
     * @param effect effect name
     * @param param parameter int array
     * @return error code
     */
    public abstract void setEffectParam(String effect, int[] param);

    /**
     * Convert from YUV420PLANAR to RGB888.
     *
     * @param width width of input/output preview
     * @param height height of input/output preview
     * @param inputYuv input preview byte array
     * @param outputRgb888 output preview byte array
     * @return error code
     */
    public abstract void convertByteArrayFromYuvToRgb888(
            int width, int height, byte[] inputYuv, byte[] outputRgb888);

    /**
     * Do effect on preview frame byte array.
     *
     * @param inputRgb888 preview buffer (RGB888)
     * @param outputBmp Bitmap instance
     * @return error code
     */
    public abstract void createEffectedBitmapFromRgb888ByteArray(
            byte[] inputRgb888, Bitmap outputBmp);

    /**
     * Prepare one shot effect.
     *
     * @param pictWidth
     * @param pictHeight
     * @return error code
     */
    public abstract void prepareOneShotEffectPicture(int pictWidth, int pictHeight);

    /**
     * Release one shot effect.
     *
     * @return error code
     */
    public abstract void releaseOneShotEffectPicture();

    /**
     * Set one shot effect element.
     *
     * @param pixels int array
     * @param length
     * @return error code
     */
    public abstract void setOneShotEffectPictureElement(int[] pixels, int length);

    /**
     * Do effect on one shot taken picture pixel array (int array).
     *
     * @param output output Bitmap instance
     * @return error code
     */
    public abstract void doOneShotEffectPicture(Bitmap output);

    /**
     * Do resize frame to picture size.
     *
     * @param output output Bitmap instance
     * @param img preivew image data
     * @return error code
     */
    public abstract void doResizeToPictureSize(Bitmap output, Bitmap preview);

    /**
     * Prepare Harris effect.
     *
     * @param pictWidth
     * @param pictHeight
     * @return error code
     */
    public abstract void prepareHarrisEffectPicture(int pictWidth, int pictHeight);

    /**
     * Release Harris effect.
     *
     * @return error code
     */
    public abstract void releaseHarrisEffectPicture();

    /**
     * Add Harris effect element.
     *
     * @param index frame number x of 3
     * @param pixels int array
     * @param length
     * @return error code
     */
    public abstract void addHarrisEffectPictureElement(int index, int[] pixels, int length);

    /**
     * Do Harris effect on 3 taken pictures.
     *
     * @param output
     * @return error code
     */
    public abstract void doHarrisEffectPicture(Bitmap output);

    /**
     * Convert and Effect. for Preview.
     *
     * @param input input preview byte array
     * @param output output byte array
     * @param imgConvertor
     */
    public abstract void doEffectOnPreview(byte[] input, byte[] output,
            ImageFormatConvertor imgConvertor);

    /**
     * Get color information on preview frame.
     *
     * @param x pixel position on preview frame.
     * @param y pixel position on preview frame.
     * @param sampleSize area size of color sample. 0=1 pixel, 1=1 pixel and around 8 pixels.
     * @param colors int array, 0=red, 1=green, 2=blue.
     */
    public abstract void fetchRgbColorLevelOnPreviewFrame(int x, int y, int sampleSize,
            int[] rgbLevel);

    /**
     * Get current effect for Instrumentation test.
     * DON'T USE FOR PRODUCT.
     * */
    //public abstract EffectMode forTestGetCurrentEffect();

    /**
     * Get current effect parameters for Instrumentation test.
     * DON'T USE FOR PRODUCT.
     * */
    //public abstract int[] forTestGetEffectParam();
}
