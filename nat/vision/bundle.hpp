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

#define CHECK_OUT_CONTEXT \
	env->SetLongField(thiz, f_cntx, (jlong)(0))
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

#define PREPARE_IMG_DAT(obj) \
	jclass dat_clzz  = env->GetObjectClass(obj); \
	jmethodID mid = env->GetMethodID(dat_clzz,"requestPool","(III)V"); \
	jfieldID f_width = env->GetFieldID(dat_clzz,"cvWidth","I"); \
	jfieldID f_height= env->GetFieldID(dat_clzz,"cvHeight","I"); \
	jfieldID f_type = env->GetFieldID(dat_clzz,"cvType","I"); \
	jfieldID f_snap = env->GetFieldID(dat_clzz,"snap" ,"I"); \
	jint snap = env->GetIntField(obj, f_snap); \
	jobject o_pool= env->GetObjectField( obj, env->GetFieldID(dat_clzz,"pool","[B") ); \
	while(false)

#define FINISH_IMG_DAT(obj, img) \
	size_t total_size = img[0].total() * img[0].elemSize(); \
	int width = img[0].cols, height = img[0].rows, cvtype = img[0].type();\
	env->SetIntField(obj, f_width , width); \
	env->SetIntField(obj, f_height, height); \
	env->SetIntField(obj, f_type  , cvtype); \
	env->SetIntField(obj, f_snap  , snap); \
	if(o_pool==NULL){ \
		env->CallVoidMethod( \
			objImgData, mid, \
			(jint)total_size, (jint)width, (jint)height \
		); \
		return; \
	} \
	jbyteArray* j_pool = reinterpret_cast<jbyteArray*>(&o_pool); \
	size_t pool_size = env->GetArrayLength(*j_pool); \
	if(pool_size<total_size){ \
		env->CallVoidMethod( \
			objImgData, mid, \
			(jint)total_size, (jint)width, (jint)height \
		); \
		return; \
	} \
	jbyte* pool = env->GetByteArrayElements(*j_pool,0); \
	for(int i=0; i<snap; i++){ \
		Mat node(height, width, cvtype, pool + i * total_size); \
		img[i].copyTo(node); \
	} \
	env->ReleaseByteArrayElements(*j_pool, pool, 0); \
	while(false)

#endif /* VISION_BUNDLE_HPP_ */
