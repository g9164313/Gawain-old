#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <iostream>
#include <fstream>
#include <cmath>
#include <modbus.h>

using namespace std;

/*jsize jstrcpy(JNIEnv* env, jstring src, char* dst){
 jboolean is_copy = JNI_FALSE;
 const char *_src = env->GetStringUTFChars(src, &is_copy);
 strcpy(dst, _src);
 env->ReleaseStringUTFChars(src, _src);
 return env->GetStringLength(src);
 }*/

static modbus_t *dev = NULL;

extern "C" JNIEXPORT void JNICALL Java_prj_refuge_DevCDR06_open(JNIEnv *env,
		jclass thiz, jstring jIpaddr) {
	char ipaddr[32];
	jstrcpy(env, jIpaddr, ipaddr);
	cout << "[MODBUS] IP:" << ipaddr << endl;
	dev = modbus_new_tcp(ipaddr, 502);
	int res = modbus_connect(dev);
	if (res < 0) {
		modbus_free(dev);
		dev = NULL;
		cout << "[MODBUS] fail to connect..." << endl;
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_refuge_DevCDR06_close(JNIEnv *env,
		jclass thiz, jstring jIpaddr) {
	if (dev == NULL) {
		return;
	}
	modbus_close(dev);
	modbus_free(dev);
	dev = NULL;
}

extern "C" JNIEXPORT void JNICALL Java_prj_refuge_DevCDR06_read(JNIEnv *env,
		jclass thiz, jchar tkn, jint addr, jshortArray jReg) {
	if (dev == NULL) {
		return;
	}
	jshort* reg = env->GetShortArrayElements(jReg, 0);
	jsize cnt = env->GetArrayLength(jReg);
	switch (tkn) {
	case 'i':
	case 'I':
		modbus_read_input_registers(dev, addr, cnt, (uint16_t*) reg);
		break;
	case 'h':
	case 'H':
	default:
		//holding register
		modbus_read_registers(dev, addr, cnt, (uint16_t*) reg);
		break;
	}
	env->ReleaseShortArrayElements(jReg, reg, 0);
}

