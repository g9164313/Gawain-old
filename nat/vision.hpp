/*
 * vision.hpp
 *
 *  Created on: 2019年4月3日
 *      Author: qq
 */

#ifndef VISION_HPP_
#define VISION_HPP_

#include <jni.h>
#include <vector>
#include <iostream>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include <opencv2/ml.hpp>
#include <opencv2/ximgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/xfeatures2d.hpp>
#include <opencv2/xobjdetect.hpp>
#include <opencv2/line_descriptor.hpp>
#include <opencv2/ccalib/randpattern.hpp>

using namespace std;
using namespace cv;
using namespace cv::ml;
using namespace cv::ximgproc;
using namespace cv::xfeatures2d;
using namespace cv::xobjdetect;
using namespace cv::randpattern;
using namespace cv::line_descriptor;

#define TICK_BEG \
	{int64 __tick=getTickCount()
#define TICK_END \
	double __tick_sec = (double)((getTickCount() - __tick))/getTickFrequency();\
	std::cout<<"[TICK] "<< __tick_sec <<"sec"<< endl; } while(false)
#define TICK_END2(accum) \
	double __tick_sec = (double)((getTickCount() - __tick))/getTickFrequency();\
	accum = accum + __tick_sec;\
	std::cout<<"[TICK] "<< __tick_sec <<"sec"<< endl; } while(false)

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


#define PREPARE_FILM(obj) \
	jclass film_clzz  = env->GetObjectClass(obj); \
	jmethodID mid_req_pool = env->GetMethodID(film_clzz,"requestPool","(III)V"); \
	jfieldID f_width = env->GetFieldID(film_clzz,"cvWidth","I"); \
	jfieldID f_height= env->GetFieldID(film_clzz,"cvHeight","I"); \
	jfieldID f_type = env->GetFieldID(film_clzz,"cvType","I"); \
	jfieldID f_snap = env->GetFieldID(film_clzz,"snap" ,"I"); \
	jint snap = env->GetIntField(obj, f_snap); \
	jobject o_pool= env->GetObjectField(obj, env->GetFieldID(film_clzz,"pool","[B") ); \
	while(false)

#define FINISH_FILM(obj, img) \
	size_t total_size = img[0].total() * img[0].elemSize();\
	int width = img[0].cols, height = img[0].rows, cvtype = img[0].type();\
	env->SetIntField(obj, f_width , width);\
	env->SetIntField(obj, f_height, height);\
	env->SetIntField(obj, f_type  , cvtype);\
	env->SetIntField(obj, f_snap  , snap);\
	if(o_pool==NULL){\
		env->CallVoidMethod(obj, mid_req_pool, (jint)total_size, (jint)width, (jint)height );\
		return; \
	} \
	jbyteArray* j_pool = reinterpret_cast<jbyteArray*>(&o_pool); \
	size_t pool_size = env->GetArrayLength(*j_pool); \
	if(pool_size<total_size){ \
		env->CallVoidMethod(obj, mid_req_pool, (jint)total_size, (jint)width, (jint)height );\
		return; \
	} \
	jbyte* pool = env->GetByteArrayElements(*j_pool,0); \
	for(int i=0; i<snap; i++){ \
		Mat node(height, width, cvtype, pool + i * total_size); \
		img[i].copyTo(node); \
	} \
	env->ReleaseByteArrayElements(*j_pool, pool, 0); \
	while(false)

#define STUBBER_PREPARE(objCamera) \
	jclass clzz_cam= env->GetObjectClass(objCamera); \
	jclass clzz_flm= env->FindClass("narl/itrc/vision/ImgFilm"); \
	jobject objFilm= env->CallObjectMethod(objCamera, env->GetMethodID(clzz_cam,"getFilm","()Lnarl/itrc/vision/ImgFilm;")); \
	int width = env->GetIntField(objFilm, env->GetFieldID(clzz_flm,"cvWidth" ,"I"));\
	int height= env->GetIntField(objFilm, env->GetFieldID(clzz_flm,"cvHeight","I"));\
	int cvType= env->GetIntField(objFilm, env->GetFieldID(clzz_flm,"cvType"  ,"I"));\
	int c_snap= env->GetIntField(objFilm, env->GetFieldID(clzz_flm,"snap"    ,"I"));\
	jmethodID mid_done = env->GetMethodID(clzz_cam, "isTaskDone", "()Z");\
	jmethodID mid_sync = env->GetMethodID(clzz_cam, "doSync", "(Z)V")

#define STUBBER_LOOP_HEAD \
		do { if(env->CallBooleanMethod(objCamera, mid_done)==JNI_TRUE){ break; }

#define STUBBER_EMBED_HEAD \
		do{ env->CallVoidMethod(objCamera, mid_sync, JNI_TRUE); \
		jobject obj_pool = env->GetObjectField(objFilm, env->GetFieldID(clzz_flm,"pool","[B"));\
		jobject obj_over = env->GetObjectField(objFilm, env->GetFieldID(clzz_flm,"over","[B"));\
		jbyteArray* arr_pool = reinterpret_cast<jbyteArray*>(&obj_pool);\
		jbyteArray* arr_over = reinterpret_cast<jbyteArray*>(&obj_over);\
		jbyte* ptrPool = env->GetByteArrayElements(*arr_pool,NULL); \
		jbyte* ptrOver = env->GetByteArrayElements(*arr_over,NULL); \
		Mat pool(height, width, cvType, ptrPool); \
		Mat over(height, width, CV_8UC4, ptrOver)
#define STUBBER_EMBED_DONE \
		env->ReleaseByteArrayElements(*arr_pool, ptrPool, 0);\
		env->ReleaseByteArrayElements(*arr_over, ptrOver, 0);\
		env->CallVoidMethod(objCamera, mid_sync, JNI_FALSE)
#define STUBBER_EMBED_TAIL \
		STUBBER_EMBED_DONE; } while(false)

#define STUBBER_LOOP_TAIL(flag) \
		}while(flag)

extern Point getPoint(
	JNIEnv * env,
	jobject objFilm,
	const int oneBaseIndex
);
extern Rect getROI(
	JNIEnv * env,
	jobject objFilm,
	const int oneBaseIndex
);
extern void getMask(
	JNIEnv * env,
	jobject objFilm,
	Mat* mask1,
	Mat* mask2,
	Mat* mask3
);

#endif /* VISION_HPP_ */
