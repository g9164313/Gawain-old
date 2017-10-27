/*
 * CamDummy.cpp
 *
 *  Created on: 2017年10月27日
 *      Author: qq
 */
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <global.hpp>
#include <vision/CamBundle.hpp>

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamDummy_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG

	Mat img(height,width,format,cntx);

	fetchCallback(env,thiz,bundle,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamDummy_loadImageFile(
	JNIEnv* env,
	jobject thiz,
	jstring jname,
	jint type
){
	char name[200];
	jstrcpy(env,jname,name);

	int fmt = (type==CV_8UC1)?(IMREAD_GRAYSCALE):(IMREAD_COLOR);

	Mat img = imread(name,fmt);
	if(img.empty()==true){
		return;//fail to open file!!!
	}

	jclass clzz = env->GetObjectClass(thiz);
	jlong len = env->CallLongMethod(
		thiz,
		env->GetMethodID(clzz,"getAligmentLength","(III)J"),
		(jint)img.cols,
		(jint)img.rows,
		(jint)type
	);
	jlong ptr = (jlong)realloc(NULL,(size_t)len);
	env->CallVoidMethod(
		thiz,
		env->GetMethodID(clzz,"pushChunkCallback","(JJIII)V"),
		ptr, len,
		(jint)img.cols,
		(jint)img.rows,
		(jint)type
	);

	memcpy(
		(void*)ptr,
		(void*)img.ptr(),
		img.total() * img.elemSize()
	);
}

