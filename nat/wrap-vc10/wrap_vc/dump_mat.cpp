#include <opencv2/core/core_c.h>
#include <opencv2/core/core.hpp>
#include <opencv2/ml/ml.hpp>

#include <iostream>
#include <cstdio>
#include <vector>

using namespace std;
using namespace cv;


/* Print a single-channel 2D matrix, with default range being the whole matrix */
void printMat(CvMat *A, const char *name, int rowBeg, int rowEnd, int colBeg, int colEnd) {
	
	/* If the (row, col) range is all in default => Use the full range */
	if (rowBeg == -1 && rowEnd == -1 && colBeg == -1 && colEnd == -1) {
		rowBeg = 0;
		rowEnd = A->rows - 1;
		colBeg = 0;
		colEnd = A->cols - 1;
	}

	/* Check for validity of the privided (row, col) range */
	if (rowBeg < 0 || rowEnd >= A->rows || rowBeg > rowEnd || colBeg < 0
			|| colEnd >= A->cols || colBeg > colEnd) {
		printf("printMat(): Invalid range of (rowBeg, rowEnd, colBeg, colEnd).");
	}

	cout << endl << name << "(" << rowBeg << ":" << rowEnd << ", " << colBeg
			<< ":" << colEnd << ") =\n\n";
	for (int i = rowBeg; i <= rowEnd; i++) {
		switch (CV_MAT_DEPTH(A->type)) {
		case CV_32F:
		case CV_64F:
			for (int j = colBeg; j <= colEnd; j++)
				/* Use C's printf() for convenience :p */
				printf("%7.3f ", (float) cvGetReal2D(A, i, j));
			break;
		case CV_8U:
		case CV_8S:
		case CV_16U:
		case CV_16S:
		case CV_32S:
			for (int j = colBeg; j <= colEnd; j++)
				/* Use C's printf() for convenience :p */
				printf("%4d ", (int) cvGetReal2D(A, i, j));
			break;
		default:
			cerr << "printMat(): Matrix type not supported.\n";
			exit(EXIT_FAILURE);
			break;
		}
		cout << endl;
	}
	cout << endl;
}

/* Print a single-channel image, with default range being the whole matrix */
/* This is the overloaded version for IplImage */
void printMat(IplImage *A, const char *name, int rowBeg, int rowEnd, int colBeg,
		int colEnd) {
	/* If the (row, col) range is all in default => Use the full range */
	if (rowBeg == -1 && rowEnd == -1 && colBeg == -1 && colEnd == -1) {
		rowBeg = 0;
		rowEnd = A->height - 1;
		colBeg = 0;
		colEnd = A->width - 1;
	}

	/* Check for validity of the privided (row, col) range */
	if (rowBeg < 0 || rowEnd >= A->height || rowBeg > rowEnd || colBeg < 0
			|| colEnd >= A->width || colBeg > colEnd) {
		printf("printMat(): Invalid range of (rowBeg, rowEnd, colBeg, colEnd).");
	}

	cout << endl << name << "(" << rowBeg << ":" << rowEnd << ", " << colBeg
			<< ":" << colEnd << ") =\n\n";
	for (int i = rowBeg; i <= rowEnd; i++) {
		switch (A->depth) {
		case IPL_DEPTH_32F:
		case IPL_DEPTH_64F:
			for (int j = colBeg; j <= colEnd; j++)
				/* Use C's printf() for convenience :p */
				printf("%7.3f ", (float) cvGetReal2D(A, i, j));
			break;
		case IPL_DEPTH_8U:
		case IPL_DEPTH_8S:
		case IPL_DEPTH_16U:
		case IPL_DEPTH_16S:
		case IPL_DEPTH_32S:
			for (int j = colBeg; j <= colEnd; j++)
				/* Use C's printf() for convenience :p */
				printf("%4d ", (int) cvGetReal2D(A, i, j));
			break;
		case IPL_DEPTH_1U:
			for (int j = colBeg; j <= colEnd; j++)
				/* Use C's printf() for convenience :p */
				printf("%1d ", (int) cvGetReal2D(A, i, j));
			break;
		default:
			cerr << "printMat(): Matrix type not supported.\n";
			exit(EXIT_FAILURE);
			break;
		}
		cout << endl;
	}
	cout << endl;
}

