/*
 * CamBundle.cpp
 *
 *  Created on: 2016年5月2日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>
#ifndef VISION
#define VISION
#endif

extern "C" JNIEXPORT jbyteArray JNICALL Java_narl_itrc_CamBundle_getData(
	JNIEnv* env,
	jobject bundle /*this object is already 'CamBundle'*/
){
	MACRO_BUNDLE_CHECK_MATX_NULL
	Mat& img = *((Mat*)matx);
	if(img.empty()==true){
		return NULL;
	}

	vector<uchar> buf;
	imencode(".jpg",img,buf);
	jbyteArray arrBuf = env->NewByteArray(buf.size());
	env->SetByteArrayRegion(arrBuf,0,buf.size(),(jbyte*)&buf[0]);
	return arrBuf;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_saveImage(
	JNIEnv * env,
	jobject bundle,
	jstring jname
){
	MACRO_BUNDLE_CHECK_MATX_VOID
	Mat& img = *((Mat*)matx);
	if(img.empty()==true){
		return;//we have problem!!!
	}
	char name[500];
	jstrcpy(env,jname,name);
	imwrite(name,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_saveImageROI(
	JNIEnv * env,
	jobject bundle,
	jstring jname,
	jintArray jroi
){
	MACRO_BUNDLE_CHECK_MATX_VOID
	Mat& img = *((Mat*)matx);
	if(img.empty()==true){
		return;//we have problem!!!
	}
	jint* buf = env->GetIntArrayElements(jroi,NULL);
	Mat roi = img(Rect(
		buf[0],buf[1],
		buf[2],buf[3]
	));
	char name[500];
	jstrcpy(env,jname,name);
	imwrite(name,roi);
	env->ReleaseIntArrayElements(jroi,buf,0);
}

