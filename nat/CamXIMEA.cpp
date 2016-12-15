/*
 * CamXIMEA.cpp
 *
 *  Created on: 2016年12月12日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>
#include <m3api/xiApi.h> // Linux, OSX
#include <memory.h>

#define HandleResult(env,res,place) \
	if (res!=XI_OK) { \
		logv(env,"Error after %s (%d)\n",place,res);\
		return; \
	}

static void getInfo(
	JNIEnv* env,
	jobject thiz,
	DWORD id,
	const char* nameParam,
	const char* nameField
){
	char info[500];
	XI_RETURN stat = xiGetDeviceInfoString(
		id,
		nameParam,
		info,sizeof(info)
	);
	if(stat!=XI_OK){
		logv(env,"fail to get %s \n",nameParam);
		return;
	}
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID _id = env->GetFieldID(_clazz,nameField,"Ljava/lang/String;");
	jstring _obj = env->NewStringUTF(info);
	env->SetObjectField(thiz,_id,_obj);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamXIMEA_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_SETUP_BEG

	HANDLE xiH = NULL;
	XI_RETURN stat = XI_OK;

	DWORD id = 0;//TODO: how to decide which device ID?

	stat = xiOpenDevice(id, &xiH);
	HandleResult(env,stat,"xiOpenDevice");

	//get device information
	getInfo(env,thiz,id,XI_PRM_DEVICE_SN,"infoSN");
	getInfo(env,thiz,id,XI_PRM_DEVICE_NAME,"infoName");
	getInfo(env,thiz,id,XI_PRM_DEVICE_INSTANCE_PATH,"infoPathInst");
	getInfo(env,thiz,id,XI_PRM_DEVICE_LOCATION_PATH,"infoPathLoca");
	getInfo(env,thiz,id,XI_PRM_DEVICE_TYPE,"infoType");

	//start to be ready!!!
	stat = xiStartAcquisition(xiH);
	HandleResult(env,stat,"xiStartAcquisition");

	MACRO_SETUP_END1(xiH)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamXIMEA_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG
	if(cntx==NULL){
		return;
	}

	HANDLE xiH = (HANDLE)cntx;
	XI_RETURN stat = XI_OK;

	XI_IMG img;
	//memset(&img,0,sizeof(img));//??do we need this???
	img.size = sizeof(XI_IMG);

	stat = xiGetImage(xiH, 5000, &img);
	HandleResult(env,stat,"xiGetImage");

	int fmt = CV_8UC1;//default type, it is also equal to XI_RAW8 and XI_MONO8
	switch(img.frm){
	case XI_RAW16:
	case XI_MONO16:
		fmt = CV_16UC1;
		break;
	case XI_RGB24:
		fmt = CV_8UC3;
		break;
	//case XI_RGB32: break;//no support
	//case XI_RGB_PLANAR: break;//no support
	//case XI_FRM_TRANSPORT_DATA: break;//no support
	}
	Mat src(
		img.height,
		img.width,
		fmt,
		img.bp
	);
	MACRO_FETCH_COPY(src)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamXIMEA_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_BEG
	if(cntx!=NULL){
		xiStopAcquisition((HANDLE)cntx);
		xiCloseDevice((HANDLE)cntx);
	}
	MACRO_CLOSE_END
}
//------------------------------------------//

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_vision_CamXIMEA_implSetParamInt(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jstring prm,
	jint val
){
	MACRO_PREPARE_CNTX
	if(cntx==NULL){
		return XI_INVALID_HANDLE;
	}
	char name[200];
	jstrcpy(env,prm,name);
	return xiSetParamInt((HANDLE)cntx, name, val);
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_vision_CamXIMEA_implSetParamFloat(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jstring prm,
	jfloat val
){
	MACRO_PREPARE_CNTX
	if(cntx==NULL){
		return XI_INVALID_HANDLE;
	}
	char name[200];
	jstrcpy(env,prm,name);
	return xiSetParamFloat((HANDLE)cntx, name, val);
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_vision_CamXIMEA_implSetParamString(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jstring prm,
	jstring val
){
	MACRO_PREPARE_CNTX
	if(cntx==NULL){
		return XI_INVALID_HANDLE;
	}
	int i = 0;
	string gg ="" + i;

	char name[200];
	jstrcpy(env,prm,name);
	char value[500];
	jstrcpy(env,val,value);
	size_t length = strlen(value);
	return xiSetParamString((HANDLE)cntx, name, value, length);
}
//------------------------------------------//

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_vision_CamXIMEA_implGetParamInt(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jstring prm
){
	MACRO_PREPARE_CNTX
	if(cntx==NULL){
		return XI_INVALID_HANDLE;
	}
	char name[200];
	jstrcpy(env,prm,name);
	jint value;
	XI_RETURN stat = xiGetParamInt((HANDLE)cntx, name, &value);
	return value;
}

extern "C" JNIEXPORT jfloat JNICALL Java_narl_itrc_vision_CamXIMEA_implGetParamFloat(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jstring prm
){
	MACRO_PREPARE_CNTX
	if(cntx==NULL){
		return XI_INVALID_HANDLE;
	}
	char name[200];
	jstrcpy(env,prm,name);
	jfloat value;
	XI_RETURN stat = xiGetParamFloat((HANDLE)cntx, name, &value);
	return value;
}

extern "C" JNIEXPORT jstring JNICALL Java_narl_itrc_vision_CamXIMEA_implGetParamString(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jstring prm
){
	MACRO_PREPARE_CNTX
	if(cntx==NULL){
		return NULL;
	}
	char name[200];
	jstrcpy(env,prm,name);
	char value[500] = {0};
	XI_RETURN stat = xiGetParamString((HANDLE)cntx, name, value, sizeof(value));
	if(stat!=XI_OK){
		return NULL;
	}
	return env->NewStringUTF(value);
}





