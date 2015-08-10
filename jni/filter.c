/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications Japan, Inc.
 * Copyright (C) 2013 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */

#include <bitmap.h>
#include <mem_utils.h>
#include <colour_space.h>
#include <math.h>
#include <android/log.h>
#include <stdlib.h>

#define  LOG_TAG    "filter.c"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#undef PI
#define PI 3.1415926535897932f

#define min(x,y)  (x <= y) ? x : y
#define max(x,y)  (x >= y) ? x : y

#define componentCeiling(x) ((x > 255) ? 255 : x)
#define clampComponent(x) ((x > 255) ? 255 : (x < 0) ? 0 : x)

#define blackAndWhite(r, g, b) ((r * 0.3f) + (g * 0.59f) + (b * 0.11f))

#define TILT_SHIFT_BLUR_RADIUS 3
#define TILT_SHIFT_ALPHA_GRADIENT_SIZE 0.07f

// Same as hard light layer mode in GIMP
static unsigned char hardLightLayerPixelComponents(unsigned char maskComponent, unsigned char imageComponent) {
	return (maskComponent > 128) ? 255 - (( (255 - (2 * (maskComponent-128)) ) * (255-imageComponent) )/256) : (2*maskComponent*imageComponent)/256;
}

// Same as overlay layer mode in GIMP
// overlayComponent is one of the rgb components of the overlay colour (0 - 255)
// underlayComponent is one of the rgb components of the underlay colour (0 - 255)
static unsigned char overlayPixelComponents(unsigned int overlayComponent, unsigned char underlayComponent, float alpha) {
	float underlay = underlayComponent * alpha;
	return (unsigned char)((underlay / 255) * (underlay + ((2.0f * overlayComponent) / 255) * (255 - underlay)));
}

// Same as multiply layer mode in GIMP
// Alpha is in the range of 0.0 - 1.0
static unsigned char multiplyPixelComponentsWithAlpha(unsigned char overlayComponent, float alpha, unsigned char underlayComponent) {
	return ((float)((int)underlayComponent * ((int)overlayComponent * alpha))/255);
}

static unsigned char multiplyPixelComponents(unsigned char overlayComponent, unsigned char underlayComponent) {
	return (((int)underlayComponent * (overlayComponent))/255);
}

// Same as grain merge layer mode in GIMP
static unsigned char grainMergePixelsComponent(unsigned char overlayComponent, unsigned char underlayComponent) {
	register int component = ((int)underlayComponent+overlayComponent)-128;
	component = (component > 255) ? 255 : (component < 0) ? 0 : component;

	return component;
}

// Same as screen layer mode in GIMP
static unsigned char screenPixelComponent(unsigned char maskPixelComponent, float alpha, unsigned char imagePixelComponent) {
	return 255.0f - (((255.0f - ((float)maskPixelComponent*alpha)) * (255.0f - imagePixelComponent)) / 255.0f);
}

// Same as subtract layer mode in GIMP
static unsigned char subtractPixelComponent(unsigned char overlayComponent, unsigned char underlayComponent) {
	return max((int)underlayComponent-overlayComponent, 0);
}

// Same as darken layer mode in GIMP
static unsigned char darkenPixelsComponent(unsigned char overlay, unsigned char underlay) {
	return min(underlay, overlay);
}

static unsigned char greyscaleInvertMaskScreenComponent(unsigned char maskGreyPixel, float alpha, unsigned char imagePixelComponent) {
	return screenPixelComponent(maskGreyPixel, (1.0f-((float)maskGreyPixel/255.0f))*alpha, imagePixelComponent);
}

// brightness is between -1.0 to +1.0;
// colourChannel is 0.0 - 1.0 representing either the red, green, or blue channel
static float applyBrightnessToPixelComponent(float colourComponent, float brightness) {
	float scaled = brightness/2;
	if (scaled < 0.0) {
		return colourComponent * ( 1.0f + scaled);
	} else {
		return colourComponent + ((1.0f - colourComponent) * scaled);
	}
}

// contrast is between -1.0 to +1.0;
// pixelComponent is either r, g, or b scaled from 0.0 - 1.0
static float applyContrastToPixelComponent(float pixelComponent, float contrast) {
	return min(1.0f, ((pixelComponent - 0.5f) * (tan ((contrast + 1) * PI/4) ) + 0.5f));
}


void gammaCorrection(Bitmap* bitmap) {
	unsigned char* red = (*bitmap).red;
	unsigned char* green = (*bitmap).green;
	unsigned char* blue = (*bitmap).blue;
	unsigned int length = (*bitmap).width * (*bitmap).height;

	unsigned int i;
	float redAverage = 0;
	float greenAverage = 0;
	float blueAverage = 0;
	unsigned int n = 1;
	for (i = 0; i < length; i++) {
		redAverage = ((n-1)*redAverage + red[i])/n;
		greenAverage = ((n-1)*greenAverage + green[i])/n;
		blueAverage = ((n-1)*blueAverage + blue[i])/n;
		n++;
	}

	float gammaRed = log(128.0f/255)/log(redAverage/150);
	float gammaGreen = log(128.0f/255)/log(greenAverage/220);
	float gammaBlue = log(128.0f/255)/log(blueAverage/200);
	int redLut[256];
	int greenLut[256];
	int blueLut[256];
	for (i = 0; i < 256; i++) {
		redLut[i] = -1;
		greenLut[i] = -1;
		blueLut[i] = -1;
	}
	for (i = 0; i < length; i++) {
		if (redLut[red[i]] == -1) {
			redLut[red[i]] = clampComponent(255.0f * powf((red[i]/255.0f), gammaRed));
		}
		red[i] = redLut[red[i]];

		if (greenLut[green[i]] == -1) {
			greenLut[green[i]] = clampComponent(255.0f * powf((green[i]/255.0f), gammaGreen));
		}
		green[i] = greenLut[green[i]];

		if (blueLut[blue[i]] == -1) {
			blueLut[blue[i]] = clampComponent(255.0f * powf((blue[i]/255.0f), gammaBlue));
		}
		blue[i] = blueLut[blue[i]];
	}
}

// amount is 0.0 to 1.0
// threshold is 0 to 255

int unsharpMask(Bitmap* bitmap, int radius, float amount, int threshold) {
	unsigned char* red = (*bitmap).red;
	unsigned char* green = (*bitmap).green;
	unsigned char* blue = (*bitmap).blue;
	unsigned int length = (*bitmap).width * (*bitmap).height;

	// Create blur
	unsigned char* blurRed;
	unsigned char* blurGreen;
	unsigned char* blurBlue;
	int resultCode = newUnsignedCharArray(length, &blurRed);
	if (resultCode != MEMORY_OK) {
		return resultCode;
	}
	resultCode = newUnsignedCharArray(length, &blurGreen);
	if (resultCode != MEMORY_OK) {
		freeUnsignedCharArray(&blurRed);
		return resultCode;
	}
	resultCode = newUnsignedCharArray(length, &blurBlue);
	if (resultCode != MEMORY_OK) {
		freeUnsignedCharArray(&blurRed);
		freeUnsignedCharArray(&blurGreen);
		return resultCode;
	}

	float blurRadius = radius/3.0f;
	resultCode = stackBlur(&blurRadius, (*bitmap).red, (*bitmap).green, (*bitmap).blue, &((*bitmap).width), &((*bitmap).height), blurRed, blurGreen, blurBlue);
	if (resultCode != MEMORY_OK) {
		freeUnsignedCharArray(&blurRed);
		freeUnsignedCharArray(&blurGreen);
		freeUnsignedCharArray(&blurBlue);
		return resultCode;
	}

	int i, j;
	short int lut[256][256];
	float a = (4 * amount) + 1;
	for (i = 0; i < 256; i++) {
		for (j = 0; j < 256; j++) {
			lut[i][j] = -1;//clampComponent((int) (a * (i - j) + j));
		}
	}
	for (i = length; i--;) {
		int r1 = red[i];
		int g1 = green[i];
		int b1 = blue[i];

		int r2 = blurRed[i];
		int g2 = blurGreen[i];
		int b2 = blurBlue[i];

		if (fabs(r1 - r2) >= threshold) {
			if (lut[r1][r2] == -1) {
				lut[r1][r2] = clampComponent((int) ((a + 1) * (r1 - r2) + r2));
			}
			r1 = lut[r1][r2]; //clampComponent((int) ((a + 1) * (r1 - r2) + r2));
		}
		if (fabs(g1 - g2) >= threshold) {
			if (lut[g1][g2] == -1) {
				lut[g1][g2] = clampComponent((int) ((a + 1) * (g1 - g2) + g2));
			}
			g1 = lut[g1][g2]; //clampComponent((int) ((a + 1) * (g1 - g2) + g2));
		}
		if (fabs(b1 - b2) >= threshold) {
			if (lut[b1][b2] == -1) {
				lut[b1][b2] = clampComponent((int) ((a + 1) * (b1 - b2) + b2));
			}
			b1 = lut[b1][b2]; //clampComponent((int) ((a + 1) * (b1 - b2) + b2));
		}

		red[i] = r1;
		green[i] = g1;
		blue[i] = b1;
	}

	freeUnsignedCharArray(&blurRed);
	freeUnsignedCharArray(&blurGreen);
	freeUnsignedCharArray(&blurBlue);
}

// Normalise the colours
void normaliseColours(Bitmap* bitmap) {
	unsigned char* red = (*bitmap).red;
	unsigned char* green = (*bitmap).green;
	unsigned char* blue = (*bitmap).blue;

	unsigned int histogram[3][256];

	unsigned int channel, i;
	for (channel = 3; channel--;) {
		for (i = 256; i--;) {
			histogram[channel][i] = 0;
		}
	}

	unsigned int width = (*bitmap).width;
	unsigned int height = (*bitmap).height;

	register unsigned int n = 0;
	unsigned int x, y;
	for (y = height; y--;) {
		for (x = width; x--;) {
			histogram[0][red[n]]++;
			histogram[1][green[n]]++;
			histogram[2][blue[n]]++;
			n++;
		}
	}

	float count = width * height;
	float percentage;
	float nextPercentage;
	unsigned int low = 0;
	unsigned int high = 255;
	double mult;

	for (channel = 3; channel--;) {
		nextPercentage = (float) histogram[channel][0] / count;
		for (i = 0; i <= 255; i++) {
			percentage = nextPercentage;
			nextPercentage += (float) histogram[channel][i + 1] / count;
			if (fabs(percentage - 0.012) < fabs(nextPercentage - 0.012)) {
				//low = i;
				if(i>10)low = i-8;else low=i;
				break;
			}
		}

		nextPercentage = (float) histogram[channel][255] / count;
		for (i = 255; i >= 0; i--) {
			percentage = nextPercentage;
			nextPercentage += histogram[channel][i - 1] / count;
			if (fabs(percentage - 0.012) < fabs(nextPercentage - 0.012)) {
				//high = i;
				if(i<245)high = i+8;else high=i;
				break;
			}
		}

		mult = (float) 255.0 / (high - low);

		if(mult>1.0)mult=1.0;

		for (i = low; i--;) {
			histogram[channel][i] = 0;
		}
		for (i = 255; i > high; i--) {
			histogram[channel][i] = 255;
		}

		float base = 0;
		for (i = low; i <= high; i++) {
			histogram[channel][i] = (int) base;
			base += mult;
		}
	}

	n = 0;
	for (y = height; y--;) {
		for (x = width; x--;) {
			red[n] = histogram[0][red[n]];
			green[n] = histogram[1][green[n]];
			blue[n] = histogram[2][blue[n]];
			n++;
		}
	}
}

void applyUnderwater(Bitmap* bitmap) {
	//unsharpMask(bitmap, 3, 0.25f, 2);
	gammaCorrection(bitmap);
	normaliseColours(bitmap);
}

void applyUnderwaterTest(Bitmap* bitmap) {
	//unsharpMask(bitmap, 3, 0.25f, 2);
	gammaCorrection(bitmap);
	normaliseColours(bitmap);
}



