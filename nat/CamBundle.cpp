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
	jobject bundle /*this object must be class 'CamBundle'*/
){
	MACRO_PREPARE
	if(buff==NULL){
		return NULL;
	}
	Mat img(height,width,type,buff);

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
	MACRO_PREPARE
	if(buff==NULL){
		return;
	}
	Mat img(height,width,type,buff);

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
	MACRO_PREPARE
	if(buff==NULL){
		return;
	}
	Mat img(height,width,type,buff);

	jint* roi = env->GetIntArrayElements(jroi,NULL);
	Mat _img = img(Rect(
		roi[0],roi[1],
		roi[2],roi[3]
	));
	char name[500];
	jstrcpy(env,jname,name);
	imwrite(name,_img);
	env->ReleaseIntArrayElements(jroi,roi,0);
}

