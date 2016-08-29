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
	jobject bundle
){
	MACRO_SETUP_BEG

	Camera* cam = new Camera();
	FlyCapture2::Error err = cam->Connect(0);//GUID ???
	if(err==PGRERROR_OK){
		CameraInfo inf;
		cam->GetCameraInfo(&inf);
		if(err==PGRERROR_OK){

		}else{
			cout<<"fail to get information"<<endl;
		}
		err = cam->StartCapture();
		if(err==PGRERROR_OK){

		}else{
			cout<<"fail to start capture"<<endl;
		}
	}else{
		cout<<"fail to connect"<<endl;
	}

	if(err!=PGRERROR_OK){
		delete cam;
		cam = NULL;
	}

	MACRO_SETUP_END1(cam)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG


}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){

}




