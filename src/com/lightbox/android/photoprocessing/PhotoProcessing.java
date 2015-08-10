/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications Japan, Inc.
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */

package com.lightbox.android.photoprocessing;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;

/**
 * PhotoProcessing
 * @author Nilesh Patel
 */
public class PhotoProcessing {
	/** Used to tag logs */
	@SuppressWarnings("unused")
	private static final String TAG = "PhotoProcessing";


	public static Bitmap filterPhoto(Bitmap bitmap, int position) {
		if (bitmap != null) { //USe current bitmap in native memory
			sendBitmapToNative(bitmap);
		}
		switch (position) {
		case 0: // Original
			break;
		case 1: // Underwater full processing;
			nativeApplyUnderwater();
			break;
		case 2: // Underwater
			nativeApplyUnderwaterTest();
			break;

		}
		Bitmap filteredBitmap = getBitmapFromNative(bitmap);
		nativeDeleteBitmap();
		return filteredBitmap;
	}

	public static Bitmap applyEditAction(Bitmap bitmap, int position) {
		switch (position) {
		case 0: // Flip
			bitmap = flipHorizontally(bitmap);
			break;
		case 1: // Rotate 90 right
			bitmap = rotate(bitmap, 90);
			break;
		case 2: // Rotate 90 left
			bitmap = rotate(bitmap, 270);
			break;
		case 3: // Rotate 180
			bitmap = rotate(bitmap, 180);
			break;
		}

		return bitmap;
	}


	///////////////////////////////////////////////

	static {
		System.loadLibrary("photoprocessing");
	}

	public static native int nativeInitBitmap(int width, int height);
	public static native void nativeGetBitmapRow(int y, int[] pixels);
	public static native void nativeSetBitmapRow(int y, int[] pixels);
	public static native int nativeGetBitmapWidth();
	public static native int nativeGetBitmapHeight();
	public static native void nativeDeleteBitmap();
	public static native int nativeRotate90();
	public static native void nativeRotate180();
	public static native void nativeFlipHorizontally();

	public static native void nativeApplyUnderwater();
	public static native void nativeApplyUnderwaterTest();
//	public static native void nativeApplyHDR();

	public static native void nativeLoadResizedJpegBitmap(byte[] jpegData, int size, int maxPixels);
	public static native void nativeResizeBitmap(int newWidth, int newHeight);

	private static void sendBitmapToNative(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		nativeInitBitmap(width, height);
		int[] pixels = new int[width];
		for (int y = 0; y < height; y++) {
			bitmap.getPixels(pixels, 0, width, 0, y, width, 1);
			nativeSetBitmapRow(y, pixels);
		}
	}

	private static Bitmap getBitmapFromNative(Bitmap bitmap) {
		int width = nativeGetBitmapWidth();
		int height = nativeGetBitmapHeight();

		if (bitmap == null || width != bitmap.getWidth() || height != bitmap.getHeight() || !bitmap.isMutable()) { //in case it was rotated and the dimensions changed
			Config config = Config.ARGB_8888;
			if (bitmap != null) {
				config = bitmap.getConfig();
				bitmap.recycle();
			}
			bitmap = Bitmap.createBitmap(width, height, config);
		}

		int[] pixels = new int[width];
		for (int y = 0; y < height; y++) {
			nativeGetBitmapRow(y, pixels);
			bitmap.setPixels(pixels, 0, width, 0, y, width, 1);
		}

		return bitmap;
	}

	public static Bitmap makeBitmapMutable(Bitmap bitmap) {
		sendBitmapToNative(bitmap);
		return getBitmapFromNative(bitmap);
	}

	public static Bitmap rotate(Bitmap bitmap, int angle) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Config config = bitmap.getConfig();
		nativeInitBitmap(width, height);
		sendBitmapToNative(bitmap);

		if (angle == 90) {
			nativeRotate90();
			bitmap.recycle();
			bitmap = Bitmap.createBitmap(height, width, config);
			bitmap = getBitmapFromNative(bitmap);
			nativeDeleteBitmap();
		} else if (angle == 180) {
			nativeRotate180();
			bitmap.recycle();
			bitmap = Bitmap.createBitmap(width, height, config);
			bitmap = getBitmapFromNative(bitmap);
			nativeDeleteBitmap();
		} else if (angle == 270) {
			nativeRotate180();
			nativeRotate90();
			bitmap.recycle();
			bitmap = Bitmap.createBitmap(height, width, config);
			bitmap = getBitmapFromNative(bitmap);
			nativeDeleteBitmap();
		}

		return bitmap;
	}

	public static Bitmap flipHorizontally(Bitmap bitmap) {
		nativeInitBitmap(bitmap.getWidth(), bitmap.getHeight());
		sendBitmapToNative(bitmap);
		nativeFlipHorizontally();
		bitmap = getBitmapFromNative(bitmap);
		nativeDeleteBitmap();
		return bitmap;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// ԴͼƬ�ĸ߶ȺͿ��
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			// �����ʵ�ʿ�ߺ�Ŀ���ߵı���
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			// ѡ���͸�����С�ı�����ΪinSampleSize��ֵ��������Ա�֤����ͼƬ�Ŀ�͸�
			// һ��������ڵ���Ŀ��Ŀ�͸ߡ�
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return inSampleSize;
	}
	//����Ҫ�ķֱ��ʼ���ͼƬ������������ڴ治������
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
	        int reqWidth, int reqHeight) {
		// ��һ�ν�����inJustDecodeBounds����Ϊtrue������ȡͼƬ��С
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeResource(res, resId, options);
	    // �������涨��ķ�������inSampleSizeֵ
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
	    // ʹ�û�ȡ����inSampleSizeֵ�ٴν���ͼƬ
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeResource(res, resId, options);
	}
}
