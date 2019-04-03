/*
 * cam_vidcap.cpp
 *
 *  Created on: 2016年3月31日
 *      Author: qq
 */
#include <global.hpp>
#include <vision/bundle.hpp>

extern "C" JNIEXPORT jboolean JNICALL Java_narl_itrc_vision_CapVidcap_implSetup(
	JNIEnv* env,
	jobject thiz
){
	CHECK_IN_CONTEXT;

	VideoCapture* vid = new VideoCapture();
	if(vid->open(0)==false){
		delete vid;
		CHECK_OUT_FALSE;
	}
	CHECK_OUT_TRUE(vid);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CapVidcap_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject objCamera
){
	PREPARE_CONTEXT;
	PREPARE_CAMERA;

	VideoCapture* vid = (VideoCapture*)(env->GetLongField(thiz,f_cntx));
	if(vid->grab()==false){
		return;
	}
	Mat img;
	vid->retrieve(img);

	FINISH_CAMERA(img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CapVidcap_implDone(
	JNIEnv* env,
	jobject thiz
){
	PREPARE_CONTEXT;

	VideoCapture* vid = (VideoCapture*)(cntx);
	vid->release();
	delete vid;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CapVidcap_setFrameSize(
	JNIEnv* env,
	jobject thiz,
	const jint width,
	const jint height
){
	PREPARE_CONTEXT;

	VideoCapture* vid = (VideoCapture*)(cntx);
	vid->set(CAP_PROP_FRAME_WIDTH ,width );
	vid->set(CAP_PROP_FRAME_HEIGHT,height);
}
