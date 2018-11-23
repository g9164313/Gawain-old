#include <jni.h>
#include <string>
#include <errno.h>
#include <modbus.h>
#include <iostream>

#define TYPE_RTU 0
#define TYPE_TCP 1

using namespace std;

extern jsize jstrcpy(JNIEnv* env, jstring src,const char* dst);

inline void setPtrCntx(JNIEnv * env,jobject thiz,jlong val){
	jclass clzz = env->GetObjectClass(thiz);
	jfieldID fid = env->GetFieldID(clzz,"ptrCntx","J");
	env->SetLongField(thiz,fid,val);
}

inline jlong getPtrCntx(JNIEnv * env,jobject thiz){
	jclass clzz = env->GetObjectClass(thiz);
	jfieldID fid = env->GetFieldID(clzz,"ptrCntx","J");
	return env->GetLongField(thiz,fid);
}

inline jint getRtuAddr(JNIEnv * env,jobject thiz){
	jclass clzz = env->GetObjectClass(thiz);
	jfieldID fid = env->GetFieldID(clzz,"rtuAddr","I");
	return env->GetLongField(thiz,fid);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_openRtu(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jint  baud,
	jint  data_bit,
	jchar parity,
	jint  stop_bit
) {
	char name[500];
	jstrcpy(env,jname,name);
	cout<<"[MODBUS] RTU-->"<<name<<','<<baud<<','<<data_bit<<','<<parity<<','<<stop_bit<<','<<endl;
	modbus_t *ctx = modbus_new_rtu(
		name,
		baud,
		parity,
		data_bit,
		stop_bit
	);
	if( modbus_connect(ctx)<0 ){
		modbus_free(ctx);
		setPtrCntx(env,thiz,0L);
		cout<<"[MODBUS] fail to connect..."<<endl;
	}else{
		setPtrCntx(env,thiz,(jlong)ctx);
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_openTcp(
	JNIEnv *env,
	jclass thiz,
	jstring jIpaddr,
	jint port
) {
	char ipaddr[32];
	jstrcpy(env, jIpaddr, ipaddr);
	cout<<"[MODBUS] TCP-->"<<ipaddr<<':'<<port<<endl;
	modbus_t *ctx = modbus_new_tcp(
		ipaddr,
		port
	);
	if( modbus_connect(ctx)<0 ) {
		modbus_free(ctx);
		setPtrCntx(env,thiz,0L);
		cout<<"[MODBUS] fail to connect..."<<endl;
	}else{
		setPtrCntx(env,thiz,(jlong)ctx);
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_close(
	JNIEnv * env,
	jobject thiz
) {
	modbus_t* ctx = (modbus_t*)getPtrCntx(env,thiz);
	if(ctx==NULL){
		return;
	}
	modbus_close(ctx);
	modbus_free(ctx);
	setPtrCntx(env,thiz,0L);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_readH(
	JNIEnv * env,
	jobject thiz,
	jint off,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getPtrCntx(env,thiz);
	if(ctx==NULL){
		return;
	}
	jint addr = getRtuAddr(env,thiz);
	if(addr>0){
		modbus_set_slave(ctx,addr);
	}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	modbus_read_input_registers(ctx, off, len, (uint16_t*)buf);
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_readR(
	JNIEnv * env,
	jobject thiz,
	jint off,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getPtrCntx(env,thiz);
	if(ctx==NULL){
		return;
	}
	jint addr = getRtuAddr(env,thiz);
	if(addr>0){
		modbus_set_slave(ctx,addr);
	}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	modbus_read_registers(ctx, off, len, (uint16_t*)buf);
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_write(
	JNIEnv * env,
	jobject thiz,
	jint off,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getPtrCntx(env,thiz);
	if(ctx==NULL){
		return;
	}
	jint addr = getRtuAddr(env,thiz);
	if(addr>0){
		modbus_set_slave(ctx,addr);
	}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	modbus_write_registers(ctx,off,len,(uint16_t*)buf);
	env->ReleaseShortArrayElements(jbuf,buf,0);
}




