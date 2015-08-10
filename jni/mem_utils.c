

#include <stdlib.h>
#include <mem_utils.h>

int newIntArray(unsigned int size, int** arrayPointer) {
	unsigned int numBytes = size * sizeof(int);
	*arrayPointer = (int*) malloc(numBytes);
	if (arrayPointer == NULL) {
		return INT_ARRAY_ERROR;
	}

	memset(*arrayPointer, 0, numBytes);
	return MEMORY_OK;
}

int newUnsignedIntArray(unsigned int size, unsigned int** arrayPointer) {
	unsigned int numBytes = size * sizeof(unsigned int);
	*arrayPointer = (int*) malloc(numBytes);
	if (arrayPointer == NULL) {
		return INT_ARRAY_ERROR;
	}

	memset(*arrayPointer, 0, numBytes);
	return MEMORY_OK;
}

int newDoubleArray(unsigned int size, double** arrayPointer) {
	unsigned int numBytes = size * sizeof(double);
	*arrayPointer = (double*) malloc(numBytes);
	if (arrayPointer == NULL) {
		return DOUBLE_ARRAY_ERROR;
	}

	memset(*arrayPointer, 0, numBytes);
	return MEMORY_OK;
}

int newUnsignedCharArray(unsigned int size, unsigned char** arrayPointer) {
	unsigned int numBytes = size * sizeof(unsigned char);
	*arrayPointer = (unsigned char*) malloc(numBytes);
	if (arrayPointer == NULL) {
		return UCHAR_ARRAY_ERROR;
	}

	memset(*arrayPointer, 0, numBytes);
	return MEMORY_OK;
}

int newFloatArray(unsigned int size, float** arrayPointer) {
	unsigned int numBytes = size * sizeof(float);
	*arrayPointer = (float*) malloc(numBytes);
	if (arrayPointer == NULL) {
		return FLOAT_ARRAY_ERROR;
	}

	memset(*arrayPointer, 0, numBytes);
	return MEMORY_OK;
}

void freeIntArray(int** arrayPointer) {
	if (*arrayPointer != NULL) {
		free(*arrayPointer);
		*arrayPointer = NULL;
	}
}

void freeUnsignedIntArray(unsigned int** arrayPointer) {
	if (*arrayPointer != NULL) {
		free(*arrayPointer);
		*arrayPointer = NULL;
	}
}

void freeDoubleArray(double** arrayPointer) {
	if (*arrayPointer != NULL) {
		free(*arrayPointer);
		*arrayPointer = NULL;
	}
}

void freeUnsignedCharArray(unsigned char** arrayPointer) {
	if (*arrayPointer != NULL) {
		free(*arrayPointer);
		*arrayPointer = NULL;
	}
}

void freeFloatArray(float** arrayPointer) {
	if (*arrayPointer != NULL) {
		free(*arrayPointer);
		*arrayPointer = NULL;
	}
}
