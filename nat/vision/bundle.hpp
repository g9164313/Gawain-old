/*
 * vision.hpp
 *
 *  Created on: 2019年4月3日
 *      Author: qq
 */

#ifndef VISION_BUNDLE_HPP_
#define VISION_BUNDLE_HPP_

#include <jni.h>
#include <vector>
#include <iostream>
#include <fstream>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
using namespace cv;
using namespace std;

#define TICK_BEG \
	{int64 __tick=getTickCount();
#define TICK_END(tag) \
	double __tick_sec = (double)((getTickCount() - __tick))/getTickFrequency();\
	std::cout<<"["tag"] " << __tick_sec <<"sec"<< endl; }
#define TICK_END2(tag,accum) \
	double __tick_sec = (double)((getTickCount() - __tick))/getTickFrequency();\
	accum = accum + __tick_sec;\
	std::cout<<"["tag"] " << __tick_sec <<"sec"<< endl; }

#define CHECK_IN_CONTEXT \
	jclass clzz = env->GetObjectClass(thiz); \
	jfieldID f_cntx = env->GetFieldID(clzz,"context","J");\
	while(false)

#define CHECK_OUT_FALSE \
	env->SetLongField(thiz, f_cntx, (jlong)(0));\
	return JNI_FALSE
#define CHECK_OUT_TRUE(ptr) \
	env->SetLongField(thiz, f_cntx, (jlong)(ptr));\
	return JNI_TRUE

#define PREPARE_CONTEXT \
	jclass clzz = env->GetObjectClass(thiz);\
	jfieldID f_cntx = env->GetFieldID(clzz,"context","J");\
	void* cntx = (void*)env->GetLongField(thiz,f_cntx);\
	if(cntx==NULL){	return; }

#define PREPARE_CAMERA \
	jclass cam_clzz = env->GetObjectClass(objCamera);\
	jfieldID f_buffer= env->GetFieldID(cam_clzz,"cvBuffer","[B");\
	jfieldID f_width = env->GetFieldID(cam_clzz,"cvWidth","I");\
	jfieldID f_height= env->GetFieldID(cam_clzz,"cvHeight","I");\
	jfieldID f_cvtype= env->GetFieldID(cam_clzz,"cvType","I");\
	jobject o_buffer = env->GetObjectField(objCamera, f_buffer);\
	jbyteArray* j_buffer = reinterpret_cast<jbyteArray*>(&o_buffer);\
	jbyte* buffer = env->GetByteArrayElements(*j_buffer,0);\
	while(false)

#define FINISH_CAMERA(img) \
	Mat node(img.rows, img.cols,img.type(),buffer);\
	if(img.type()==CV_8UC3){\
		cvtColor(img, node, COLOR_BGR2RGB);\
	}else{\
		img.copyTo(node);\
	}\
	env->SetIntField(objCamera, f_width , img.cols);\
	env->SetIntField(objCamera, f_height, img.rows);\
	env->SetIntField(objCamera, f_cvtype, img.type());\
	env->ReleaseByteArrayElements(*j_buffer, buffer, 0);\
	while(false)

#endif /* VISION_BUNDLE_HPP_ */
