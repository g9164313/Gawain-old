/*
 * ImgData.cpp
 *
 *  Created on: 2019年4月17日
 *      Author: qq
 */
#include <errno.h>
#include <global.hpp>
#include "pipeImage.hpp"
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
using namespace cv;
using namespace std;

Mat skeletonize(const Mat& img){
	Mat _img;
	img.copyTo(_img);
	Mat skel = Mat::zeros(img.size(), CV_8UC1);
	Mat kern = getStructuringElement(MORPH_ELLIPSE,Size(3,3));
	do{
		Mat node, temp;
		erode(_img, node, kern);
		dilate(node, temp, kern);
		subtract(_img, temp, temp);
		bitwise_or(skel, temp, skel);
		node.copyTo(_img);
	}while(countNonZero(_img)!=0);
	/*Mat skel;
	img.copyTo(skel);
	do{
		Mat node;
		distanceTransform(skel, node, DIST_L2, 5);

		threshold(node, node, 1, 255, THRESH_BINARY);
		//imwrite("cc1.png",node);
		if(countNonZero(node)==0){
			break;
		}
		//node.copyTo(skel);
	}while(true);*/
	return skel;
}

static void copy_ref_mark(
	JNIEnv * env,
	jobject objFilm,
	const jint offset,
	const jint value[4]
){
	jclass film_clzz  = env->GetObjectClass(objFilm);
	jobject o_mark = env->GetObjectField(
		objFilm,
		env->GetFieldID(film_clzz,"refMark","[I")
	);
	jintArray* j_mark = reinterpret_cast<jintArray*>(&o_mark);
	jint* ptrMark = env->GetIntArrayElements(*j_mark,0);
	memcpy(
		(void*)value,
		(void*)(ptrMark+offset*4),
		sizeof(jint)*4
	);
	env->ReleaseIntArrayElements(*j_mark, ptrMark, JNI_ABORT);
}

Point getPoint(
	JNIEnv * env,
	jobject objFilm,
	const int oneBaseIndex
){
	jint value[4];
	copy_ref_mark(
		env, objFilm,
		oneBaseIndex-1,
		value
	);
	return Point(value[0], value[1]);
}

Rect getROI(
	JNIEnv * env,
	jobject objFilm,
	const int oneBaseIndex
){
	jint value[4];
	copy_ref_mark(
		env, objFilm,
		oneBaseIndex-1,
		value
	);
	return Rect(
		value[0], value[1],
		value[2], value[3]
	);
}

void getMask(
	JNIEnv * env,
	jobject objFilm,
	Mat* mask1,
	Mat* mask2,
	Mat* mask3
){
	jclass film_clzz  = env->GetObjectClass(objFilm);
	jmethodID mirror_mask_mid = env->GetMethodID(film_clzz,"mirrorMask","()V");
	//try to refresh byte array~~
	env->CallVoidMethod(objFilm, mirror_mask_mid);
	//ok, buffer had data, just get it~~
	jobject o_mask = env->GetObjectField(
		objFilm,
		env->GetFieldID(film_clzz,"bufMask","[B")
	);
	int width = env->GetIntField(
		objFilm,
		env->GetFieldID(film_clzz,"cvWidth","I")
	);
	int height= env->GetIntField(
		objFilm,
		env->GetFieldID(film_clzz,"cvHeight","I")
	);
	jbyteArray* j_mask = reinterpret_cast<jbyteArray*>(&o_mask);
	jsize len = env->GetArrayLength(*j_mask);
	if(len<(width*height*4)){
		return;
	}
	jbyte* ptrMask = env->GetByteArrayElements(*j_mask,0);
	Mat img(height, width, CV_8UC4, ptrMask);
	Mat BGRA[4];//blue, green red, alpha
	split(img,BGRA);
	if(mask1!=NULL){ BGRA[2].copyTo(*mask1); }
	if(mask2!=NULL){ BGRA[1].copyTo(*mask2); }
	if(mask3!=NULL){ BGRA[0].copyTo(*mask3); }
	env->ReleaseByteArrayElements(*j_mask, ptrMask, 0);
}

void getMark(
	JNIEnv * env,
	jobject objFilm,
	Point pin[4],
	Rect roi[4]
){
	jclass fim_clzz  = env->GetObjectClass(objFilm);
	jobject o_mark = env->GetObjectField(
		objFilm,
		env->GetFieldID(fim_clzz,"mark","[I")
	);
	jintArray* j_mark = reinterpret_cast<jintArray*>(&o_mark);
	jint* ptr = env->GetIntArrayElements(*j_mark,0);
	for(int i=0; i<4; i++){
		pin[i].x = ptr[i*4+0];
		pin[i].y = ptr[i*4+1];
		roi[i].x = ptr[16+i*4+0];
		roi[i].y = ptr[16+i*4+1];
		roi[i].width = ptr[16+i*4+2];
		roi[i].height= ptr[16+i*4+3];
	}
	env->ReleaseIntArrayElements(*j_mark, ptr, 0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_ImgFilm_reflector(
	JNIEnv* env,
	jobject thiz,
	jbyteArray objSrc,
	jbyteArray objDst
){
	jclass dat_clzz  = env->GetObjectClass(thiz);
	jint width = env->GetIntField(thiz,env->GetFieldID(dat_clzz,"cvWidth" ,"I"));
	jint height= env->GetIntField(thiz,env->GetFieldID(dat_clzz,"cvHeight","I"));
	jint cvtype= env->GetIntField(thiz,env->GetFieldID(dat_clzz,"cvType"  ,"I"));
	jbyte* p_src = env->GetByteArrayElements(objSrc,0);
	jbyte* p_dst = env->GetByteArrayElements(objDst,0);

	Mat src(height, width, cvtype , p_src);
	Mat dst(height, width, CV_8UC3, p_dst);
	switch(cvtype){
	case CV_8UC1:
		cvtColor(src, dst, COLOR_GRAY2BGR);
		break;
	case CV_8UC3:
		cvtColor(src, dst, COLOR_RGBA2BGR);
		break;
	}

	env->ReleaseByteArrayElements(objSrc, p_src, 0);
	env->ReleaseByteArrayElements(objDst, p_dst, 0);
}





extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_DevCamera_pipeFetch(
	JNIEnv* env,
	jobject thiz,
	jbyteArray objKey,
	jobject objImgData,
	jboolean sync
){
	jclass img_clazz  = env->GetObjectClass(objImgData);

	jobject arrPool = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"pool","[B"));
	jobject arrOver = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"over","[B"));
	jobject arrMesg = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"mesg","[B"));
	jobject arrMark = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"mark","[I"));
	jbyteArray* objPool = reinterpret_cast<jbyteArray*>(&arrPool);
	jbyteArray* objOver = reinterpret_cast<jbyteArray*>(&arrOver);
	jbyteArray* objMesg = reinterpret_cast<jbyteArray*>(&arrMesg);
	jintArray*  objMark = reinterpret_cast<jintArray* >(&arrMark);
	jbyte* pool = env->GetByteArrayElements(*objPool, NULL);
	jbyte* over = env->GetByteArrayElements(*objOver, NULL);
	jbyte* mesg = env->GetByteArrayElements(*objMesg, NULL);
	jint*  mark = env->GetIntArrayElements (*objMark, NULL);

	jint width = env->GetIntField(objImgData,env->GetFieldID(img_clazz,"cvWidth" ,"I"));
	jint height= env->GetIntField(objImgData,env->GetFieldID(img_clazz,"cvHeight","I"));
	jint cvtype= env->GetIntField(objImgData,env->GetFieldID(img_clazz,"cvType"  ,"I"));
	jint snap  = env->GetIntField(objImgData,env->GetFieldID(img_clazz,"snap"    ,"I"));
	size_t lenPool = env->GetArrayLength(*objPool);
	size_t lenOver = env->GetArrayLength(*objOver);

	jclass cam_clazz  = env->GetObjectClass(thiz);
	jbyte* keys = env->GetByteArrayElements(objKey, JNI_FALSE);

	PIPE_KEY& key = *((PIPE_KEY*)keys);
	if(key.pid==0 || key.mid==0){
		jobject objName = env->GetStaticObjectField(
			cam_clazz,
			env->GetStaticFieldID(cam_clazz, "nodeName", "Ljava/lang/String;")
		);
		jstring* strName = reinterpret_cast<jstring*>(&objName);
		const char* name = env->GetStringUTFChars(*strName, JNI_FALSE);
		key_t kid = ftok(name,1);
		if(kid>0){
			key.pid = msgget(kid, IPC_CREAT|0666);
			key.mid = shmget(kid, lenPool+lenOver, IPC_CREAT|0666);
		}
		env->ReleaseStringUTFChars(*strName, name);
	}
	if(key.pid<0 || key.mid<0){
		env->ReleaseByteArrayElements(objKey, keys, JNI_FALSE);
		return;
	}

	MSG_PACK pack;
	if(0<=msgrcv(
		key.pid,
		&pack, 0,
		MSG_COMMIT,
		(sync==JNI_TRUE)?(0):(IPC_NOWAIT)
	)){
		//cout<<"shake-0"<<endl;
		pack.type = MSG_SHAKE1;
		PACK_MSG_DATA;
		uint8_t* smem = (uint8_t*)shmat(key.mid,NULL,0);//attach share memory
		memcpy(smem, pool, lenPool);
		msgsnd(key.pid, &pack, MSG_LENGTH, 0);
		//cout<<"shake-1"<<endl;
		//wait for user process
		if(0<msgrcv(key.pid, &pack, MSG_LENGTH, MSG_SHAKE2, 0)){
			memcpy(mesg, pack.data, MSG_LENGTH);//restore user, data
			memcpy(over, smem+lenPool, lenOver);//make a shadow of image
		}
		shmdt(smem);
		//cout<<"shake-2"<<endl;
	}
	//cout<<"ERROR:"<<strerror(errno)<<"("<<errno<<")"<<endl;

	env->ReleaseByteArrayElements(objKey, keys, JNI_FALSE);
	env->ReleaseByteArrayElements(*objPool, pool, JNI_FALSE);
	env->ReleaseByteArrayElements(*objOver, over, JNI_FALSE);
	env->ReleaseByteArrayElements(*objMesg, mesg, JNI_FALSE);
	env->ReleaseIntArrayElements (*objMark, mark, JNI_FALSE);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_DevCamera_pipeClose(
	JNIEnv* env,
	jobject thiz,
	jbyteArray objKey
){
	jclass cam_clazz  = env->GetObjectClass(thiz);
	jbyte* keys = env->GetByteArrayElements(objKey, JNI_FALSE);
	PIPE_KEY& key = *((PIPE_KEY*)keys);
	if(key.mid>0){
		shmctl(key.mid, IPC_RMID, NULL);
	}
	if(key.pid>0){
		msgctl(key.pid, IPC_RMID, NULL);
	}
	key.pid = key.mid = 0;
	env->ReleaseByteArrayElements(objKey, keys, JNI_FALSE);
}

