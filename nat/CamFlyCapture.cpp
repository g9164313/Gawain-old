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

	int type;
	Camera* cam = new Camera();
	FlyCapture2::Error err = cam->Connect(0);//GUID ???
	if(err==PGRERROR_OK){
		CameraInfo inf;
		cam->GetCameraInfo(&inf);
		if(err==PGRERROR_OK){
			if(inf.isColorCamera==true){
				type = CV_8UC3;
			}else{
				type = CV_8UC1;
			}
		}else{
			loge(env,"fail to get information");
		}
		cam->StartCapture();
	}else{
		loge(env,"fail to connect");
	}

	if(err!=PGRERROR_OK){
		delete cam;
		cam = NULL;
	}

	MACRO_SETUP_END2(cam,type)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG
	if(cntx==NULL){
		return;
	}

	Camera* cam = (Camera*)cntx;
	Image img;
	FlyCapture2::Error err = cam->RetrieveBuffer(&img);
	if(err!=PGRERROR_OK){
		loge(env,"fail to fetch image");
		return;
	}
	Mat tmp(
		img.GetRows(),
		img.GetCols(),
		type,
		img.GetData(),
		(double)img.GetReceivedDataSize()/(double)img.GetRows()
	);

	MACRO_FETCH_COPY(tmp)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_BEG
	if(cntx!=NULL){
		Camera* cam = (Camera*)cntx;
		cam->StopCapture();
		cam->Disconnect();
		delete cam;
	}
	MACRO_CLOSE_END
}




