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
	MACRO_SETUP_BEG

	VideoCapture* vid = new VideoCapture(id);
	if(vid->isOpened()==false){
		delete vid;
		vid = NULL;//mark it again~~~
	}

	MACRO_SETUP_END(vid)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG

	VideoCapture& vid = *((VideoCapture*)(cntx));
	vid>>buff;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_BEG

	VideoCapture* vid = (VideoCapture*)cntx;
	vid->release();
	delete vid;

	MACRO_CLOSE_END
}





