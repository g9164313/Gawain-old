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
//#if defined WIN32 || defined _WIN32 || defined _WINDLL
#ifdef _MSC_VER
//this direction for M$ VC2010
#include <Windows.h>
#define M_PI 3.1415
#define NAT_EXPORT extern "C" __declspec(dllexport)
typedef unsigned char uint8_t;
typedef unsigned char uint8;
typedef unsigned short uint16_t;
typedef short int16_t;
typedef unsigned int uint32_t;
typedef int int32;
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
#ifdef VISION
#include <vision.hpp>
#endif

using namespace std;

extern void setJdouble(JNIEnv *env,jobject thiz,const char* name,double val);
extern double getJdouble(JNIEnv *env,jobject thiz,const char* name);
extern void setJfloat(JNIEnv *env,jobject thiz,const char* name,float val);
extern float getJfloat(JNIEnv *env,jobject thiz,const char* name);
extern void setJlong(JNIEnv *env,jobject thiz,const char* name,long val);
extern long getJlong(JNIEnv *env,jobject thiz,const char* name);
extern void setJint(JNIEnv *env,jobject thiz,const char* name,int val);
extern int getJint(JNIEnv *env,jobject thiz,const char* name);
extern void setJchar(JNIEnv *env,jobject thiz,const char* name,char val);
extern char getJchar(JNIEnv *env,jobject thiz,const char* name);
extern void setJbool(JNIEnv *env,jobject thiz,const char* name,bool val);
extern bool getJbool(JNIEnv *env,jobject thiz,const char* name);

extern jsize jstrcpy(JNIEnv* env, jstring src, const char* dst);
extern jsize jstrcpy(JNIEnv* env, jstring src, string& dst);

extern jbyte*   byteArray2Ptr  (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jbyteArray  & arr);
extern jchar*   charArray2Ptr  (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jcharArray  & arr);
extern jint*    intArray2Ptr   (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jintArray   & arr);
extern jfloat*  floatArray2Ptr (JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jfloatArray & arr);
extern jdouble* doubleArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jdoubleArray& arr);

extern void callThreadJoin(JNIEnv* env,jobject thiz,const char* name);

#endif /* GLOBAL_H_ */

