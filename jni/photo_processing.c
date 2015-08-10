/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications Japan, Inc.
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */

#include <jni.h>
#include <stdlib.h>
#include <bitmap.h>
#include <mem_utils.h>
#include <android/log.h>

#define  LOG_TAG    "PREVIEW_CACHE_IMAGE_PROCESSING"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static Bitmap bitmap;

int Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeInitBitmap(JNIEnv* env, jobject thiz, jint width, jint height) {
	return initBitmapMemory(&bitmap, width, height);
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeGetBitmapRow(JNIEnv* env, jobject thiz, jint y, jintArray pixels) {
	int cpixels[bitmap.width];
	getBitmapRowAsIntegers(&bitmap, (int)y, &cpixels);
	(*env)->SetIntArrayRegion(env, pixels, 0, bitmap.width, cpixels);
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeSetBitmapRow(JNIEnv* env, jobject thiz, jint y, jintArray pixels) {
	int cpixels[bitmap.width];
	(*env)->GetIntArrayRegion(env, pixels, 0, bitmap.width, cpixels);
	setBitmapRowFromIntegers(&bitmap, (int)y, &cpixels);
}

int Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeGetBitmapWidth(JNIEnv* env, jobject thiz) {
	return bitmap.width;
}

int Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeGetBitmapHeight(JNIEnv* env, jobject thiz) {
	return bitmap.height;
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeDeleteBitmap(JNIEnv* env, jobject thiz) {
	deleteBitmap(&bitmap);
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeFlipHorizontally(JNIEnv* env, jobject thiz) {
	flipHorizontally(&bitmap, 1, 1, 1);
}

int Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeRotate90(JNIEnv* env, jobject thiz) {
	int resultCode = rotate90(&bitmap, 1, 1, 1);
	if (resultCode != MEMORY_OK) {
		return resultCode;
	}

	//All the component dimensions should have changed, so copy the correct dimensions
	bitmap.width = bitmap.redWidth;
	bitmap.height = bitmap.redHeight;
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeRotate180(JNIEnv* env, jobject thiz) {
	rotate180(&bitmap, 1, 1, 1);
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeApplyUnderwater(JNIEnv* env, jobject thiz) {
	applyUnderwater(&bitmap);
}

void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeApplyUnderwaterTest(JNIEnv* env, jobject thiz) {
	applyUnderwaterTest(&bitmap);
}
//void Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeApplyUnderwaterTest(JNIEnv* env, jobject thiz) {
//	applyUnderwaterTest(&bitmap);
//}

int Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeLoadResizedJpegBitmap(JNIEnv* env, jobject thiz, jbyteArray bytes, jint jpegSize, jint maxPixels) {
	char* jpegData = (char*) (*env)->GetPrimitiveArrayCritical(env, bytes, NULL);

	if (jpegData == NULL) {
		LOGE("jpeg data was null");
		return JNI_GET_INT_ARRAY_ERROR;
	}

	int resultCode = decodeJpegData(jpegData, jpegSize, maxPixels, &bitmap);
	if (resultCode != MEMORY_OK) {
		deleteBitmap(&bitmap);
		LOGE("error decoding jpeg resultCode=%d", resultCode);
		return resultCode;
	}

	(*env)->ReleasePrimitiveArrayCritical(env, bytes, jpegData, 0);

	return MEMORY_OK;
}

int Java_com_lightbox_android_photoprocessing_PhotoProcessing_nativeResizeBitmap(JNIEnv* env, jobject thiz, jint newWidth, jint newHeight) {
	unsigned char* newRed;
	int resultCode = newUnsignedCharArray(newWidth*newHeight, &newRed);
	if (resultCode != MEMORY_OK) {
		return resultCode;
	}
	resizeChannelBicubic(bitmap.red, bitmap.width, bitmap.height, newRed, (int)newWidth, (int)newHeight);
	freeUnsignedCharArray(&bitmap.red);
	bitmap.red = newRed;
	bitmap.redWidth = newWidth;
	bitmap.redHeight = newHeight;

	unsigned char* newGreen;
	resultCode = newUnsignedCharArray(newWidth*newHeight, &newGreen);
	if (resultCode != MEMORY_OK) {
		return resultCode;
	}
	resizeChannelBicubic(bitmap.green, bitmap.width, bitmap.height, newGreen, (int)newWidth, (int)newHeight);
	freeUnsignedCharArray(&bitmap.green);
	bitmap.green = newGreen;
	bitmap.greenWidth = newWidth;
	bitmap.greenHeight = newHeight;

	unsigned char* newBlue;
	resultCode = newUnsignedCharArray(newWidth*newHeight, &newBlue);
	if (resultCode != MEMORY_OK) {
		return resultCode;
	}
	resizeChannelBicubic(bitmap.blue, bitmap.width, bitmap.height, newBlue, (int)newWidth, (int)newHeight);
	freeUnsignedCharArray(&bitmap.blue);
	bitmap.blue = newBlue;
	bitmap.blueWidth = newWidth;
	bitmap.blueHeight = newHeight;

	bitmap.width = newWidth;
	bitmap.height = newHeight;
}
