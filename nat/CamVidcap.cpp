/*
 * cam_vidcap.cpp
 *
 *  Created on: 2016年3月31日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_SETUP_BEG

	jclass _clzz=env->GetObjectClass(thiz);
	jint dom = env->GetIntField(
		thiz,
		env->GetFieldID(_clzz,"capDomain","I")
	);
	jint idx = env->GetIntField(
		thiz,
		env->GetFieldID(_clzz,"capIndex","I")
	);

	VideoCapture* vid = new VideoCapture();
	if(dom==CAP_IMAGES){
		jstring j_name = (jstring)env->GetObjectField(
			thiz,
			env->GetFieldID(_clzz,"capConfig","Ljava/lang/String;")
		);
		char txtName[500];
		jstrcpy(env,j_name,txtName);
		vid->open(txtName,dom);
		logv(env,"[VID] image sequence:%s",txtName);
	}else{
		vid->open(dom+idx);
		logv(env,"[VID] identify=%d+%d",dom,idx);
	}

	if(vid->isOpened()==false){
		delete vid;
		vid = NULL;//mark it again~~~
		logv(env,"[VID] fail to open device");
	}

	MACRO_SETUP_END(vid,0,0,0,0)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamVidcap_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG

	VideoCapture& vid = *((VideoCapture*)(cntx));
	if(vid.grab()==false){
		return;
	}
	Mat tmp;
	vid.retrieve(tmp);

	MACRO_FETCH_COPY(tmp)
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
	MACRO_PREPARE
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
	MACRO_PREPARE
	VideoCapture* vid = (VideoCapture*)cntx;
	if(vid==NULL){
		return 0.;
	}
	return vid->get(id);
}



