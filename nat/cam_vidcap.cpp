/*
 * cam_vidcap.cpp
 *
 *  Created on: 2016年3月31日
 *      Author: qq
 */
#include <global.hpp>
#include <grabber.hpp>
#include <CamBundle.hpp>

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id
){
	CamBundle* cam = initContext(env,bundle);//it is necessary~~~
	VideoCapture* vid;
	if(id<0){
		//automatically select a camera,how to try cameras?
		for(int i=0; i<10; i++){
			vid = new VideoCapture(id);
			if(vid->isOpened()==true){
				break;
			}
			delete vid;
			vid = NULL;//reset it~~~
		}
		if(vid==NULL){
			cam->updateEnableState(false,"no valid capturer");
			return;
		}
	}else{
		vid = new VideoCapture(id);
	}
	if(vid->isOpened()==true){
		cam->ctxt = vid;//assign it~~~
		cam->updateEnableState(true,"open via Vidcap");
	}else{
		cam->updateEnableState(false,"fail to open Vidcap");
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_FETCH_CHECK
	cam->updateSource();
	VideoCapture& vid =*((VideoCapture*)(cam->ctxt));//acquire image~~~
	Mat& img = cam->updateSource();
	vid>>img;
	cam->updateOverlay();
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_CHECK
	cam->updateEnableState(false,"close vidcap");
	if(cam->ctxt!=NULL){
		VideoCapture* vid = (VideoCapture*)(cam->ctxt);
		vid->release();
		delete vid;
	}
	delete cam;
}





