/*
 * global.h
 *
 *  Created on: 2013/11/27
 *      Author: qq
 */

#ifndef GLOBAL_H
#define GLOBAL_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdint.h>
#include <string.h>

#include <iostream>
#include <fstream>
#include <cmath>
#include <list>
#include <vector>
#include <string>

using namespace std;

#ifdef _MSC_VER
//this direction for M$ VC2010
#include <Windows.h>
#define M_PI 3.1415
#define NAT_EXPORT extern "C" __declspec(dllexport)
typedef signed char  int8_t;
typedef signed short int16_t;
typedef signed int   int32;
typedef unsigned char  uint8_t;
typedef unsigned short uint16_t;
typedef unsigned int   uint32_t;
inline void usleep(int usec){
	usec = usec/1000;
	Sleep(usec);//this is milisecond
}
inline void msleep(int msec){
	Sleep(msec);//this is milisecond
}
#endif

#include <jni.h>

#define _set_jvalue(token,type) \
	jclass _clzz = env->GetObjectClass(thiz); \
	jfieldID id = env->GetFieldID(_clzz,name,token); \
	env->Set##type##Field(thiz,id,val)

#define _get_jvalue(token,type) \
	jclass _clzz = env->GetObjectClass(thiz); \
	jfieldID id = env->GetFieldID(_clzz,name,token); \
	return env->Get##type##Field(thiz,id)

inline void setJDouble(JNIEnv *env, jobject thiz, const char* name, double val){
	_set_jvalue("D",Double);
}
inline double getJDouble(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("D",Double);
}
inline void setJFloat(JNIEnv *env, jobject thiz, const char* name, float val){
	_set_jvalue("F",Float);
}
inline float getJFloat(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("F",Float);
}
inline void setJLong(JNIEnv *env, jobject thiz, const char* name, long val){
	_set_jvalue("J",Int);
}
inline long getJLong(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("J",Long);
}
inline void setJInt(JNIEnv *env, jobject thiz, const char* name, int val){
	_set_jvalue("I",Int);
}
inline int getJInt(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("I",Int);
}
inline void setJShort(JNIEnv *env, jobject thiz, const char* name, int16_t val){
	_set_jvalue("S",Short);
}
inline jshort getJShort(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("S",Short);
}
inline void setJChar(JNIEnv *env, jobject thiz, const char* name, uint16_t val){
	_set_jvalue("C",Char);
}
inline uint16_t getJChar(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("C",Char);
}
inline void setJByte(JNIEnv *env, jobject thiz, const char* name, uint8_t val){
	_set_jvalue("B",Byte);
}
inline uint8_t getJByte(JNIEnv *env,jobject thiz, const char* name){
	_get_jvalue("B",Byte);
}
inline void setJBoolean(JNIEnv *env, jobject thiz, const char* name, bool val){
	_set_jvalue("Z",Boolean);
}
inline bool getJBoolean(JNIEnv *env,jobject thiz, const char* name){
	_get_jvalue("Z",Boolean);
}

inline string jstrcpy(JNIEnv* env, jstring src){
	jboolean is_copy = JNI_FALSE;
	//size_t len = env->GetStringLength(src);
	const char *_src = env->GetStringUTFChars(src, &is_copy);
	string dst(_src);
	env->ReleaseStringUTFChars(src, _src);
	return dst;
}

inline size_t jstrcpy(JNIEnv* env, jstring src, const char* dst){
	jboolean is_copy = JNI_FALSE;
	const char *_src = env->GetStringUTFChars(src, &is_copy);
	strcpy((char*)dst, _src);
	env->ReleaseStringUTFChars(src, _src);
	return env->GetStringLength(src);
}

/**
TODO: moving!!!
extern jobjectArray create2DArray(JNIEnv * env, size_t cols, size_t rows, jint* value[]);
extern jobjectArray create2DArray(JNIEnv * env, size_t cols, size_t rows, jlong** value);

extern jbyte*   byteArray2Ptr  (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jbyteArray  & arr);
extern jchar*   charArray2Ptr  (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jcharArray  & arr);
extern jint*    intArray2Ptr   (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jintArray   & arr);
extern jlong*   longArray2Ptr  (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jlongArray  & arr);
extern jfloat*  floatArray2Ptr (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jfloatArray & arr);
extern jdouble* doubleArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jdoubleArray& arr);

extern void logv(JNIEnv* env,const char* fmt,...);
extern void logw(JNIEnv* env,const char* fmt,...);
extern void loge(JNIEnv* env,const char* fmt,...);
**/

#endif /* GLOBAL_H_ */

