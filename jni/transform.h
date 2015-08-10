

#ifndef TRANSFORM
#define TRANSFORM
#endif

static const char FLIP_HORIZONTALLY = 'h';
static const char FLIP_VERTICALLY = 'v';
static const char ROTATE_90 = 'r';
static const char ROTATE_180 = 'u';
static const char CROP = 'c';

typedef struct {
	float cropBounds[4]; //left, top, right, bottom
	unsigned char* transforms;
	int size;
} TransformList;
