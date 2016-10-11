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

static Camera* getCameraBySerial(
	JNIEnv* env,
	BusManager& bus,
	unsigned int serial,int total
){
	FlyCapture2::Error error;
	PGRGuid guid;
	for(int i=0; i<total; i++){
		error = bus.GetCameraFromIndex(i,&guid);
		Camera t_cam;
		error = t_cam.Connect(&guid);
		if(error!=PGRERROR_OK){
			continue;
		}
		CameraInfo inf;
		error = t_cam.GetCameraInfo(&inf);
		if(error==PGRERROR_OK){
			if(inf.serialNumber==serial){
				cout<<"match serial:"<<serial<<endl;
				t_cam.Disconnect();
				Camera* cam = new Camera();
				cam->Connect(&guid);
				return cam;
			}
		}
		t_cam.Disconnect();
	}
	return NULL;
}

static Camera* getCameraByIndex(
	JNIEnv* env,
	BusManager& bus,
	unsigned int index,int total
){
	FlyCapture2::Error error;
	PGRGuid guid;
	error = bus.GetCameraFromIndex(index,&guid);
	if(error!=PGRERROR_OK){
		loge(env,"fail to get camera");
		return NULL;
	}
	Camera* cam = new Camera();
	error = cam->Connect(&guid);
	if(error!=PGRERROR_OK){
		delete cam;
		loge(env,"fail to connect camera");
		return NULL;
	}
	return cam;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint index,
	jboolean isSerial
){
	MACRO_SETUP_BEG

	FlyCapture2::Error error;
	BusManager bus;
	unsigned int total;

	error = bus.GetNumOfCameras(&total);
	if(error!=PGRERROR_OK){
		loge(env,"fail to get number of camera");
		MACRO_SETUP_END0()
		return;
	}
	if(total==0){
		MACRO_SETUP_END0()
		return;
	}

	Camera* cam;
	if(isSerial==JNI_TRUE){
		cam = getCameraBySerial(env,bus,index,total);
	}else{
		cam = getCameraByIndex(env,bus,index,total);
	}
	if(cam==NULL){
		MACRO_SETUP_END0()
		return;
	}

	CameraInfo inf;
	int type = CV_8UC1;
	error = cam->GetCameraInfo(&inf);
	if(error==PGRERROR_OK){
		if(inf.isColorCamera==true){
			type = CV_8UC3;
		}
	}else{
		logw(env,"fail to get information");
	}
	cam->StartCapture();
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




