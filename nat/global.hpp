/*
 * global.h
 *
 *  Created on: 2013/11/27
 *      Author: qq
 */

#ifndef GLOBAL_H
#define GLOBAL_H

#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>


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

#else
#include <dirent.h>
#include <unistd.h>
#define NAT_EXPORT extern "C"
#endif

#include <iostream>
#include <fstream>
#include <cmath>
#include <list>
#include <string>
#include <vector>

using namespace std;

extern void   setJDouble(JNIEnv *env,jobject thiz,const char* name,double val);
extern double getJDouble(JNIEnv *env,jobject thiz,const char* name);
extern void   setFloat(JNIEnv *env,jobject thiz,const char* name,float val);
extern float  getFloat(JNIEnv *env,jobject thiz,const char* name);
extern void   setJLong(JNIEnv *env,jobject thiz,const char* name,long val);
extern long   getJLong(JNIEnv *env,jobject thiz,const char* name);
extern void   setJInt(JNIEnv *env,jobject thiz,const char* name,int val);
extern int    getJInt(JNIEnv *env,jobject thiz,const char* name);
extern void   setJShort(JNIEnv *env, jobject thiz, const char* name, int16_t val);
extern jshort getJShort(JNIEnv *env, jobject thiz, const char* name);
extern void   setJByte(JNIEnv *env, jobject thiz, const char* name, uint8_t val);
extern jbyte  getJByte(JNIEnv *env,jobject thiz,const char* name);
extern void   setJChar(JNIEnv *env,jobject thiz,const char* name,char val);
extern char   getJChar(JNIEnv *env,jobject thiz,const char* name);
extern void   setJBool(JNIEnv *env,jobject thiz,const char* name,bool val);
extern bool   getJBool(JNIEnv *env,jobject thiz,const char* name);

extern jsize getJString(JNIEnv *env,jobject thiz,const char* name,const char* dst);
extern jsize jstrcpy(JNIEnv* env,jstring src,const char* dst);

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

#endif /* GLOBAL_H_ */

