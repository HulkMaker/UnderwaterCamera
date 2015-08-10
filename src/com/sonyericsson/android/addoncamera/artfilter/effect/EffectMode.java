/*
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */

/**
 * @file EffectMode.java
 *
 */
package com.sonyericsson.android.addoncamera.artfilter.effect;



/**
 * Definition of effect mode.
 *
 */
public enum EffectMode {
    NOEFFECT(               //Normal
            "NO_EFFECT",
             -1,//R.string.cam_strings_effect_no_effect_txt,
            2);

    /** Effect logic identification name. */
    private final String mEffectString;

    /** Effect label resource ID . */
    private final int mEffectLabelResId;

    /** Required frame count. PreviewEffector uses this number of output frame. */
    private final int mOutputBufferCount;

    /**
     * Effect name for Native library.
     *
     * @param effect name
     */
    private EffectMode(
            String effectName,
            int effectLabelResId,
            int outputBufferCount) {
        mEffectString = effectName;
        mEffectLabelResId = effectLabelResId;
        mOutputBufferCount = outputBufferCount;
    }

    /**
     * Get effect name for native library.
     * @return effectName
     */
    @Override
    public String toString() {
        return mEffectString;
    }

    /**
     * Effect label string resource.
     *
     * @return label resource ID
     */
    public int getEffectLabelResId() {
        return mEffectLabelResId;
    }

    /**
     * Output buffer count for this effect.
     *
     * @return number of buffer
     */
    public int getOutputBufferCount() {
        return mOutputBufferCount;
    }


    // Default Effect.
    public static EffectMode getDefaultEffect() {
        return EffectMode.NOEFFECT;
    }
}
