/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications Japan, Inc.
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */
#ifndef BITMAP
#define BITMAP
#endif

#include <transform.h>

static const int INCONSISTENT_BITMAP_ERROR = 5;

typedef struct {
	unsigned int width;
	unsigned int height;

	unsigned int redWidth;
	unsigned int redHeight;
	unsigned int greenWidth;
	unsigned int greenHeight;
	unsigned int blueWidth;
	unsigned int blueHeight;

	unsigned char* red;
	unsigned char* green;
	unsigned char* blue;

	TransformList transformList;
} Bitmap;
