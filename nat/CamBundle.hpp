#ifndef CAMBUNDLE_HPP
#define CAMBUNDLE_HPP

#include <jni.h>
#include <string>
#include <iostream>

extern jint* intArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jintArray& arr);
extern jlong* longArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jlongArray& arr);
extern jfloat* floatArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jfloatArray& arr);

using namespace std;

//--------------------------------------------//

#define MACRO_SETUP_BEG \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(clzz,"ptrCntx","J"); \
	jfieldID idMatx = env->GetFieldID(clzz,"ptrMatx","J");

#define MACRO_SETUP_END(cntx) \
	if(cntx==NULL){ \
		env->SetLongField(bundle,idCntx,0); \
		env->SetLongField(bundle,idMatx,0); \
		return; \
	}\
	MACRO_SETUP_END_V(cntx, new Mat());

//remeber to allocate matx buffer~~~
#define MACRO_SETUP_END_V(cntx,matx) \
	env->SetLongField(bundle,idCntx,(jlong)(cntx)); \
	env->SetLongField(bundle,idMatx,(jlong)(matx));

#define MACRO_SETUP_CNTX(cntx) env->SetLongField(bundle,idCntx,(jlong)(cntx));
#define MACRO_SETUP_MATX(matx) env->SetLongField(bundle,idMatx,(jlong)(matx));

//--------------------------------------------//

#define MACRO_CHECK_CNTX \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(clzz,"ptrCntx","J"); \
	void* cntx = (void*)(env->GetLongField(bundle,idCntx));

#define MACRO_BUNDLE_CHECK_CNTX_VOID \
	MACRO_CHECK_CNTX \
	if(matx==NULL){ return; }

#define MACRO_CHECK_MATX \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idMatx = env->GetFieldID(clzz,"ptrMatx","J"); \
	Mat* matx = (Mat*)(env->GetLongField(bundle,idMatx));

#define MACRO_BUNDLE_CHECK_MATX_VOID \
	MACRO_CHECK_MATX \
	if(matx==NULL){ return; }

#define MACRO_BUNDLE_CHECK_MATX_NULL \
	MACRO_CHECK_MATX \
	if(matx==NULL){ return NULL; }

//--------------------------------------------//

#define MACRO_FETCH_BEG \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(clzz,"ptrCntx","J"); \
	void* cntx = (void*)(env->GetLongField(bundle,idCntx)); \
	if(cntx==NULL){ return;	} \
	jfieldID idMatx = env->GetFieldID(clzz,"ptrMatx","J"); \
	Mat* matx = (Mat*)(env->GetLongField(bundle,idMatx)); \
	if(matx==NULL){ return; } \
	Mat& buff = *matx;

#define MACRO_FETCH_BEG_V \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(clzz,"ptrCntx","J"); \
	jlong cntx = env->GetLongField(bundle,idCntx); \
	if(cntx==0L){ return;	} \
	jfieldID idMatx = env->GetFieldID(clzz,"ptrMatx","J"); \
	Mat* matx = (Mat*)(env->GetLongField(bundle,idMatx));\
	if(matx==NULL){ return; } \
	Mat& buff = *matx;

//--------------------------------------------//

#define MACRO_CLOSE_BEG \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(clzz,"ptrCntx","J"); \
	void* cntx = (void*)(env->GetLongField(bundle,idCntx)); \
	if(cntx==NULL){	return; }

#define MACRO_CLOSE_END \
	jfieldID idMatx = env->GetFieldID(clzz,"ptrMatx","J"); \
	Mat* matx = (Mat*)(env->GetLongField(bundle,idMatx));\
	if(matx!=NULL){ matx->release(); delete matx; }\
	env->SetLongField(bundle,idCntx,0); \
	env->SetLongField(bundle,idMatx,0);

#endif
