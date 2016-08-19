/*
 * CamFlycapture.cpp
 *
 *  Created on: 2016年8月12日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>
#include "FlyCapture2.h"

using namespace FlyCapture2;

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
){
	Camera* cam = new Camera();


}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){

}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){

}




