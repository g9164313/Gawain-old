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
#include <windows.h>
#include <stdint.h>
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
#include <unistd.h>
#endif

#include <jni.h>
//JNI routine support!!!
#ifdef _JAVASOFT_JNI_H_

#define _set_jvalue(token,type) \
	jclass _clzz = env->GetObjectClass(thiz); \
	jfieldID fid = env->GetFieldID(_clzz,name,token); \
	env->Set##type##Field(thiz,fid,val)

#define _get_jvalue(token,type) \
	jclass _clzz = env->GetObjectClass(thiz); \
	jfieldID fid = env->GetFieldID(_clzz,name,token); \
	return env->Get##type##Field(thiz,fid)

#define _get_jarray(token,type) \
	jfieldID fid; \
	jclass clz; \
	jobject obj; \
	clz = env->GetObjectClass(thiz); \
	fid = env->GetFieldID(clz, name, token); \
	obj = env->GetObjectField(thiz, fid); \
	return *(reinterpret_cast<j##type##Array*>(&obj))


inline void setJDouble(JNIEnv *env, jobject thiz, const char* name, double val){
	_set_jvalue("D",Double);
}
inline double getJDouble(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("D",Double);
}
inline jdoubleArray& getJDoubleArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[D", double);
}

inline void setJFloat(JNIEnv *env, jobject thiz, const char* name, float val){
	_set_jvalue("F",Float);
}
inline float getJFloat(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("F",Float);
}
inline jfloatArray& getJFloatArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[F", float);
}

inline void setJLong(JNIEnv *env, jobject thiz, const char* name, long val){
	_set_jvalue("J",Int);
}
inline long getJLong(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("J",Long);
}
inline jlongArray& getJLongArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[J", long);
}

inline void setJInt(JNIEnv *env, jobject thiz, const char* name, int val){
	_set_jvalue("I",Int);
}
inline int getJInt(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("I",Int);
}
inline jintArray getJIntArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[I", int);
}

inline void setJShort(JNIEnv *env, jobject thiz, const char* name, int16_t val){
	_set_jvalue("S",Short);
}
inline jshort getJShort(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("S",Short);
}
inline jshortArray& getJShortArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[S", short);
}

inline void setJChar(JNIEnv *env, jobject thiz, const char* name, uint16_t val){
	_set_jvalue("C",Char);
}
inline uint16_t getJChar(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("C",Char);
}
inline jcharArray& getJCharArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[C", char);	
}

inline void setJByte(JNIEnv *env, jobject thiz, const char* name, uint8_t val){
	_set_jvalue("B",Byte);
}
inline uint8_t getJByte(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("B",Byte);
}
inline jbyteArray& getJByteArray(JNIEnv* env, jobject thiz, const char* name) {
	_get_jarray("[B", byte);
}

inline void setJBoolean(JNIEnv *env, jobject thiz, const char* name, bool val){
	_set_jvalue("Z",Boolean);
}
inline bool getJBoolean(JNIEnv *env, jobject thiz, const char* name){
	_get_jvalue("Z",Boolean);
}

inline void getJString(JNIEnv* env, jobject thiz, const char* name, const char* dest) {
	jclass   clzz= env->GetObjectClass(thiz);
	jfieldID fid = env->GetFieldID(clzz, name, "Ljava/lang/String;");
	jobject  obj = env->GetObjectField(thiz, fid);
	jstring& str = *(reinterpret_cast<jstring*>(&obj));
	env->GetStringUTFRegion(
		str, 
		0, 
		env->GetStringLength(str), 
		(char*)dest
	);
}

inline string jstrcpy(JNIEnv* env, jstring str){
	jboolean is_copy = JNI_FALSE;
	//size_t len = env->GetStringLength(src);
	const char *_str = env->GetStringUTFChars(str, &is_copy);
	string dst(_str);
	env->ReleaseStringUTFChars(str, _str);
	return dst;
}

inline size_t jstrcpy(JNIEnv* env, jstring str, const char* dest){
	jboolean is_copy = JNI_FALSE;
	const char *_str = env->GetStringUTFChars(str, &is_copy);
#ifndef _MSC_VER
	strcpy((char*)dest, _str);
#else
	strcpy_s((char*)dest, 512, _str);
#endif
	env->ReleaseStringUTFChars(str, _str);
	return env->GetStringLength(str);
}

#endif //_JAVASOFT_JNI_H_

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

