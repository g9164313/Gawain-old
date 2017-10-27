/*
 * cam_vidcap.cpp
 *
 *  Created on: 2016年3月31日
 *      Author: qq
 */
#include <global.hpp>
#include <vision/CamBundle.hpp>

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamVidcap_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	jclass s_clzz = env->GetObjectClass(thiz);
	jint dom = env->GetIntField(thiz,env->GetFieldID(s_clzz,"capDomain","I"));
	jint idx = env->GetIntField(thiz,env->GetFieldID(s_clzz,"capIndex" ,"I"));

	VideoCapture* vid = new VideoCapture();
	cout<<"capId="<<dom<<"+"<<idx<<endl;

	if(dom==CAP_IMAGES){
		char seqName[500];
		getJString(env,thiz,"seqName",seqName);
		vid->open(seqName,dom);
		//logv(env,"[VIDCAP] image sequence:%s",seqName);
	}else{
		vid->open(dom+idx);
		//logv(env,"[VIDCAP] capId = %d+%d",dom,idx);
	}
	if(vid->isOpened()==false){
		//logv(env,"[VIDCAP] fail to open device");
		return;
	}

	setupCallback(
		env,bundle,
		vid,
		vid->get(CAP_PROP_FRAME_WIDTH),
		vid->get(CAP_PROP_FRAME_HEIGHT),
		vid->get(CAP_PROP_FORMAT)
	);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamVidcap_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG

	VideoCapture& vid = *((VideoCapture*)(cntx));
	if(vid.grab()==false){
		return;
	}
	Mat img;
	vid.retrieve(img);

	fetchCallback(env,thiz,bundle,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamVidcap_implClose(
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

extern "C" JNIEXPORT jboolean JNICALL Java_narl_itrc_vision_CamVidcap_setProp(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id,
	jdouble val
){
	MACRO_PREPARE_BEG
	VideoCapture* vid = (VideoCapture*)cntx;
	if(vid==NULL){
		return JNI_FALSE;
	}
	bool flag = vid->set(id,val);
	return (flag==true)?(JNI_TRUE):(JNI_FALSE);
}

extern "C" JNIEXPORT jdouble JNICALL Java_narl_itrc_vision_CamVidcap_getProp(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id
){
	MACRO_PREPARE_BEG
	VideoCapture* vid = (VideoCapture*)cntx;
	if(vid==NULL){
		return 0.;
	}
	return vid->get(id);
}



