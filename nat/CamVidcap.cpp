/*
 * cam_vidcap.cpp
 *
 *  Created on: 2016年3月31日
 *      Author: qq
 */
#include <global.hpp>
#include <grabber.hpp>
#include <CamBundle.hpp>

jint getCapID(JNIEnv* env,jobject camvidcap){
	jclass clzz=env->GetObjectClass(camvidcap);
	jfieldID idDom = env->GetFieldID(clzz,"capDomain","I");
	jfieldID idIdx = env->GetFieldID(clzz,"capIndex","I");
	jint dom = env->GetIntField(camvidcap,idDom);
	jint idx = env->GetIntField(camvidcap,idIdx);
	return dom+idx;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_SETUP_BEG

	VideoCapture* vid = new VideoCapture();
	vid->open(getCapID(env,thiz));
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

extern "C" JNIEXPORT jboolean JNICALL Java_narl_itrc_CamVidcap_setProp(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id,
	jdouble val
){
	MACRO_CHECK_CNTX
	VideoCapture* vid = (VideoCapture*)cntx;
	if(vid==NULL){
		return JNI_FALSE;
	}
	bool flag = vid->set(id,val);
	return (flag==true)?(JNI_TRUE):(JNI_FALSE);
}

extern "C" JNIEXPORT jdouble JNICALL Java_narl_itrc_CamVidcap_getProp(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id
){
	MACRO_CHECK_CNTX
	VideoCapture* vid = (VideoCapture*)cntx;
	if(vid==NULL){
		return 0.;
	}
	return vid->get(id);
}



