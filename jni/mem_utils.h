

#ifndef MEM_UTILS
#define MEM_UTILS
#endif

static const int MEMORY_OK = 0;
static const int INT_ARRAY_ERROR = 1;
static const int DOUBLE_ARRAY_ERROR = 2;
static const int UCHAR_ARRAY_ERROR = 3;
static const int FLOAT_ARRAY_ERROR = 4;
static const int JNI_GET_INT_ARRAY_ERROR = 5;

int newIntArray(unsigned int size, int** arrayPointer);
int newUnsignedIntArray(unsigned int size, unsigned int** arrayPointer);
int newDoubleArray(unsigned int size, double** arrayPointer);
int newUnsignedCharArray(unsigned int size, unsigned char** arrayPointer);
int newFloatArray(unsigned int size, float** arrayPointer);
void freeIntArray(int** arrayPointer);
void freeUnsignedIntArray(unsigned int** arrayPointer);
void freeDoubleArray(double** arrayPointer);
void freeUnsignedCharArray(unsigned char** arrayPointer);
void freeFloatArray(float** arrayPointer);
