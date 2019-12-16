#include <modbus.h>
#include <global.hpp>

#define JNI_PREFIX_NAME(name) Java_narl_itrc_DevModbus_##name

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implOpenRtu)(
	JNIEnv * env,
	jobject thiz
) {
	char name[500];
	getJString(env, thiz, "rtuName", name);
	jint  baud = getJInt (env, thiz, "rtuBaud");
	jbyte data = getJByte(env, thiz, "rtuData");
	jbyte mask = getJByte(env, thiz, "rtuMask");
	jbyte stop = getJByte(env, thiz, "rtuStop");

	modbus_t *ctx = modbus_new_rtu(
		name,
		baud,
		mask, data-'0', stop-'0'
	);
	if( modbus_connect(ctx) != TRUE){
		modbus_free(ctx);
		ctx = NULL;//reset it!!!
		cout << "MODBUS FAIL:" << name << endl;
	}else {
		cout << "MODBUS RTU:" << name << endl;
	}
	setJLong(env, thiz, "handle", (long)ctx);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implOpenTcp)(
	JNIEnv *env,
	jclass thiz
) {
	char name[25];
	getJString(env, thiz, "tcpName", name);
	int  port = getJInt(env, thiz, "tcpPort");
	
	modbus_t *ctx = modbus_new_tcp(name,port);
	if (modbus_connect(ctx)<0) {
		modbus_free(ctx);
		ctx = NULL;//reset it!!!
		cout << "MODBUS FAIL:"<< name << endl;
	}else{
		cout << "MODBUS TCP:" << name << "#" << port << endl;
	}
	setJLong(env, thiz, "handle", (long)ctx);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevModbus_close(
	JNIEnv * env,
	jobject thiz
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	modbus_close(ctx);
	modbus_free(ctx);
	setJLong(env, thiz, "handle", 0L);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implReadH)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return;
	}
	//int16_t sid = getJShort(env, thiz, "slave");
	//if (sid > 0) {
	//	modbus_set_slave(ctx, sid);
	//}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	if (modbus_read_input_registers(ctx, addr, len, (uint16_t*)buf) < 0) {
		cout << "Modbus readH fail!!" << endl;
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implReadR)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return;
	}
	//jint sid = getJInt(env, thiz, "slave");
	//if (sid > 0) {
	//	modbus_set_slave(ctx, sid);
	//}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	if (modbus_read_registers(ctx, addr, len, (uint16_t*)buf) < 0) {
		cout << "Modbus readR fail!!" << endl;
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implWrite)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return;
	}
	//jint sid = getJInt(env, thiz, "slave");
	//if (sid > 0) {
	//	modbus_set_slave(ctx, sid);
	//}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	if (modbus_write_registers(ctx, addr, len, (uint16_t*)buf) < 0) {
		cout << "MODBUS write fail!!" << endl;
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implWriteBit)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jboolean value
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if (ctx == NULL) {
		return;
	}
	//jint sid = getJInt(env, thiz, "slave");
	//if (sid > 0) {
	//	modbus_set_slave(ctx, sid);
	//}

	modbus_write_bit(ctx,0,0);
}


