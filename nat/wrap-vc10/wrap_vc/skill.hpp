/*
 * global.h
 *
 *  Created on: 2013/11/27
 *      Author: qq
 */

#ifndef GLOBAL_H_
#define GLOBAL_H_

#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#ifdef _MSC_VER
//this direction for M$ VC2010
#include <Windows.h>
#define M_PI 3.1415
typedef unsigned char uint8_t;
typedef unsigned int uint32_t;
inline void usleep(int usec){
	usec = usec/1000;
	Sleep(usec);//this is milisecond
}
//#else
//#include <unistd.h>
#endif
#include <iostream>
#include <fstream>
#include <cmath>

#include <opencv/cv.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/contrib/contrib.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/photo/photo.hpp>
//#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/ml/ml.hpp>
#include <opencv2/stitching/stitcher.hpp>

using namespace std;
using namespace cv;

extern cv::Mat* obj2mat(JNIEnv *env,jobject obj);
extern jobjectArray mat2array(JNIEnv *env,Mat& src);
extern void mapPoint(JNIEnv *env, jobject obj, cv::Point& dst);
extern void mapRect(JNIEnv *env, jobject obj, cv::Rect& dst);
extern void mapRect(JNIEnv *env, Rect& src, jobject dst);
extern jsize jstrcpy(JNIEnv* env, jstring src, char* dst);
extern jsize jstrcpy(JNIEnv* env, jstring src, string& dst);
extern bool isSaturation(const Mat& src,bool dark);

extern void cvHaarWavelet(
	Mat &src,
	Mat &dst,
	int NIter
);
extern void cvInvHaarWavelet(
	Mat &src,
	Mat &dst,
	int NIter,
	int SHRINKAGE_TYPE = 0,
	float SHRINKAGE_T = 50
);

inline void check_roi(Rect& roi,Mat& bound){
	if(roi.x<0){
		roi.x = 0;
	}
	if((roi.x+roi.width)>bound.cols){
		roi.width = bound.cols - roi.x;
	}
	if(roi.y<0){
		roi.y = 0;
	}
	if((roi.y+roi.height)>bound.rows){
		roi.height = bound.rows - roi.y;
	}
}

inline void check_bound(Rect& roi,const Mat& bound){
	if(roi.x<0){
		roi.x = 0;		
	}
	if((roi.x+roi.width)>bound.cols){
		roi.width = bound.cols - roi.x;
	}
	if(roi.y<0){
		roi.y = 0;
	}
	if((roi.y+roi.height)>bound.rows){
		roi.height = bound.rows - roi.y;		
	}
}

inline void extend_roi(Rect& roi,int dw,int dh,Mat& bound){
	roi.x=roi.x-dw;
	roi.y=roi.y-dh;
	roi.width = roi.width+2*dw;
	roi.height = roi.height+2*dh;
	check_bound(roi,bound);
}

inline int check_mod(int a, int b){
	int c = a/b;
	if(c==0){
		return a;
	}
	return a%b;
}

inline int pry_down_size(int a){
	int rem = (a%2==0)?(0):(1);
	a=(a/2)+rem;
	return a;
}

#define SATURATION_VAL 5.

inline bool isSaturation(const Mat& src,const Mat& mask){
	Mat node;
	if(
		src.cols>mask.cols &&
		src.rows>mask.rows
	){
		node=src(Rect(
			(src.cols-mask.cols)/2,
			(src.rows-mask.rows)/2,
			mask.cols,
			mask.rows
		));
	}else{
		node = src;
	}
	Scalar avg,dev;
	meanStdDev(node,avg,dev,mask);
	if(dev[0]<SATURATION_VAL){
		return true;
	}
	return false;
}

inline bool isSaturation(const Mat& src){
	Scalar avg,dev;
	meanStdDev(src,avg,dev);
	if(dev[0]<SATURATION_VAL){
		return true;
	}
	return false;
}

inline bool isSeperate(int a, int div, int b){
	a = a-div;
	b = b-div;
	if(a*b<0){
		return true;
	}
	return false;
}

inline void check_32f(Mat& m, Mat& node){
	if(m.type()==CV_32F){
		node = m;
	}else{	
		m.convertTo(node,CV_32FC1);
	}
}

#endif /* GLOBAL_H_ */
