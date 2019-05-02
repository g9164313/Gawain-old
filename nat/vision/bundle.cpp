/*
 * ImgData.cpp
 *
 *  Created on: 2019年4月17日
 *      Author: qq
 */
#include <errno.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <sys/shm.h>
#include <global.hpp>
#include <vision/bundle.hpp>
#include "pipeImage.hpp"

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_DevCamera_pipeFetch(
	JNIEnv* env,
	jobject thiz,
	jbyteArray objKey,
	jobject objImgData
){
	jclass img_clazz  = env->GetObjectClass(objImgData);

	jobject arrPool = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"pool","[B"));
	jobject arrOver = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"over","[B"));
	jobject arrMesg = env->GetObjectField(objImgData, env->GetFieldID(img_clazz,"mesg","[B"));
	jbyteArray* objPool = reinterpret_cast<jbyteArray*>(&arrPool);
	jbyteArray* objOver = reinterpret_cast<jbyteArray*>(&arrOver);
	jbyteArray* objMesg = reinterpret_cast<jbyteArray*>(&arrMesg);
	jbyte* pool = env->GetByteArrayElements(*objPool, NULL);
	jbyte* over = env->GetByteArrayElements(*objOver, NULL);
	jbyte* mesg = env->GetByteArrayElements(*objMesg, NULL);

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
	if(0<=msgrcv(key.pid, &pack, 0, MSG_COMMIT, IPC_NOWAIT)){
		//cout<<"shake-0"<<endl;
		pack.type = MSG_SHAKE1;
		((uint32_t*)(pack.data))[0] = width;
		((uint32_t*)(pack.data))[1] = height;
		((uint32_t*)(pack.data))[2] = cvtype;
		((uint32_t*)(pack.data))[3] = snap;
		((uint32_t*)(pack.data))[4] = lenPool;
		((uint32_t*)(pack.data))[5] = lenOver;
		void* smem = shmat(key.mid,NULL,0);//attach share memory
		memcpy(smem, pool, lenPool);
		msgsnd(key.pid, &pack, MSG_LENGTH, 0);
		//cout<<"shake-1"<<endl;
		if(0<msgrcv(key.pid, &pack, MSG_LENGTH, MSG_SHAKE2, 0)){
			memcpy(
				pack.data,
				mesg,
				MSG_LENGTH
			);
			memcpy(over, ((uint8_t*)smem)+lenPool, lenOver);
		}
		shmdt(smem);
		//cout<<"shake-2"<<endl;
	}
	//cout<<"ERROR:"<<strerror(errno)<<"("<<errno<<")"<<endl;

	env->ReleaseByteArrayElements(objKey, keys, JNI_FALSE);
	env->ReleaseByteArrayElements(*objPool, pool, JNI_FALSE);
	env->ReleaseByteArrayElements(*objOver, over, JNI_FALSE);
	env->ReleaseByteArrayElements(*objMesg, mesg, JNI_FALSE);
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


extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_ImgData_reflector(
	JNIEnv* env,
	jobject thiz,
	jbyteArray objSrc,
	jbyteArray objDst
){
	jclass dat_clzz  = env->GetObjectClass(thiz);
	jint width = env->GetIntField(thiz,
		env->GetFieldID(dat_clzz,"cvWidth","I")
	);
	jint height= env->GetIntField(thiz,
		env->GetFieldID(dat_clzz,"cvHeight","I")
	);
	jint cvtype= env->GetIntField(thiz,
		env->GetFieldID(dat_clzz,"cvType","I")
	);
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

