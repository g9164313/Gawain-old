/*
 * cam_vidcap.cpp
 *
 *  Created on: 2016年3月31日
 *      Author: qq
 */
#include <global.hpp>
#include <vision.hpp>

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
	jobject objImgData
){
	PREPARE_CONTEXT;
	PREPARE_IMG_DAT(objImgData);

	VideoCapture* vid = (VideoCapture*)(env->GetLongField(thiz,f_cntx));
	if(vid->grab()==false){
		return;
	}
	Mat img[60];
	if(snap>60){ snap = 60; } //maximum capability
	for(int i=0; i<snap; i++){
		vid->retrieve(img[i]);
	}

	FINISH_IMG_DAT(objImgData, img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CapVidcap_implDone(
	JNIEnv* env,
	jobject thiz
){
	PREPARE_CONTEXT;
	VideoCapture* vid = (VideoCapture*)(cntx);
	vid->release();
	delete vid;
	CHECK_OUT_CONTEXT;
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
