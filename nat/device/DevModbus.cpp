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

	modbus_t* ctx = modbus_new_rtu(
		name,
		baud,
		mask, data-'0', stop-'0'
	);
	//modbus_set_debug(ctx, TRUE);
	if (modbus_connect(ctx)<0){
		modbus_free(ctx);
		ctx = NULL;//reset it!!!
		cout << "MODBUS FAIL:" << name << endl;
	}else{
		//modbus_set_response_timeout(ctx, 3, 0);
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

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implSlaveID)(
	JNIEnv * env,
	jobject thiz,
	jint slave_id
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if (ctx == NULL) {
		return;
	}
	modbus_set_slave(ctx, slave_id);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implReadC)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return;
	}
	jsize   len = env->GetArrayLength(jbuf);
	uint8_t* flg = new uint8_t[len];
	//MODBUS_FC_READ_INPUT_REGISTERS = 4
	if (modbus_read_bits(ctx, addr, len, flg) < 0) {
		cout << "Modbus readC fail::" << addr << "-" << len << endl;
	}
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	for(jsize i=0; i<len; i++){
		buf[i] = (jshort)flg[i];
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
	delete flg;
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implReadS)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return;
	}
	jsize   len = env->GetArrayLength(jbuf);
	uint8_t* flg = new uint8_t[len];
	//MODBUS_FC_READ_INPUT_REGISTERS = 4
	if (modbus_read_input_bits(ctx, addr, len, flg) < 0) {
		cout << "Modbus readS fail::" << addr << "-" << len << endl;
	}
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	for(jsize i=0; i<len; i++){
		buf[i] = (jshort)flg[i];
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
	delete flg;
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
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	//MODBUS_FC_READ_HOLDING_REGISTERS = 3
	if (modbus_read_registers(ctx, addr, len, (uint16_t*)buf) < 0) {
		cout << "Modbus readH fail::" << addr << "-" << len << endl;
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL JNI_PREFIX_NAME(implReadI)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return;
	}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	//MODBUS_FC_READ_INPUT_REGISTERS = 4
	if (modbus_read_input_registers(ctx, addr, len, (uint16_t*)buf) < 0) {
		cout << "Modbus readI fail::"<<addr<<"-"<<len<<endl;
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT jint JNICALL JNI_PREFIX_NAME(implWrite)(
	JNIEnv * env,
	jobject thiz,
	jint addr,
	jshortArray jbuf
) {
	jint res = -2;
	modbus_t* ctx = (modbus_t*)getJLong(env, thiz, "handle");
	if(ctx==NULL){
		return res;
	}
	jsize   len = env->GetArrayLength(jbuf);
	jshort* buf = env->GetShortArrayElements(jbuf,NULL);
	res = modbus_write_registers(ctx, addr, len, (uint16_t*)buf);
	if ( res< 0) {
		cout << "Modbus write fail::" << addr << "-" << len << endl;
	}
	env->ReleaseShortArrayElements(jbuf,buf,0);
	return res;
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
	modbus_write_bit(ctx,0,0);
}


