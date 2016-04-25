#include <global.hpp>
#if defined _MSC_VER
#include <DIO3232.h>

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_open(
	JNIEnv * env,
	jobject thiz
){
	jclass clzz = env->GetObjectClass(thiz);
	jint cid = env->GetIntField(thiz,env->GetFieldID(clzz,"cid","I"));
	jint sta = dio3232_initial();
	env->SetIntField(thiz, env->GetFieldID(clzz, "sta", "I"), sta);
	//get some information~~~
	u16 vid, addr;
	dio3232_info(cid,&vid,&addr);
	env->SetIntField(thiz, env->GetFieldID(clzz, "vid", "I"), vid);
	env->SetIntField(thiz, env->GetFieldID(clzz, "addr", "I"), addr);
}

extern "C" JNIEXPORT jboolean JNICALL Java_prj_reheating_DevDIO3232_readIBit(
	JNIEnv * env,
	jobject thiz,
	jint bit
) {
	jclass clzz = env->GetObjectClass(thiz);
	jint cid = env->GetIntField(thiz, env->GetFieldID(clzz, "cid", "I"));
	u8 res;
	jint sta = dio3232_read_in_point(cid,bit,&res);
	env->SetIntField(thiz, env->GetFieldID(clzz, "sta", "I"), sta);
	if (res == 1) {
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_prj_reheating_DevDIO3232_readOBit(
	JNIEnv * env,
	jobject thiz, 
	jint bit
) {
	jclass clzz = env->GetObjectClass(thiz);
	jint cid = env->GetIntField(thiz, env->GetFieldID(clzz, "cid", "I"));
	u8 res;
	jint sta = dio3232_read_out_point(cid, bit, &res);
	env->SetIntField(thiz, env->GetFieldID(clzz, "sta", "I"), sta);
	if (res == 1) {
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_writeOBit(
	JNIEnv * env,
	jobject thiz, 
	jint bit, 
	jboolean val
) {
	jclass clzz = env->GetObjectClass(thiz);
	jint cid = env->GetIntField(thiz, env->GetFieldID(clzz, "cid", "I"));
	u8 res = (val == JNI_TRUE) ? (1) : (0);
	jint sta = dio3232_set_out_point(cid, bit, res);
	env->SetIntField(thiz, env->GetFieldID(clzz, "sta", "I"), sta);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_reheating_DevDIO3232_getPort(
	JNIEnv * env,
	jobject thiz, 
	jint port
) {
	jclass clzz = env->GetObjectClass(thiz);
	jint cid = env->GetIntField(thiz, env->GetFieldID(clzz, "cid", "I"));
	u8 res;
	jint sta = dio3232_read_port(cid,port,&res);
	env->SetIntField(thiz, env->GetFieldID(clzz, "sta", "I"), sta);
	return (jint)res;
}

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_setPort(
	JNIEnv * env,
	jobject thiz,
	jint port, 
	jint val
) {
	jclass clzz = env->GetObjectClass(thiz);
	jint cid = env->GetIntField(thiz, env->GetFieldID(clzz, "cid", "I"));
	jint sta = dio3232_set_port(cid, port, (u8)(val & 0xFF));
	env->SetIntField(thiz, env->GetFieldID(clzz, "sta", "I"), sta);
}

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_close(
	JNIEnv * env,
	jobject thiz
) {
	dio3232_close();
}
#else
//dummy for linux~~
extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_open(
	JNIEnv * env,
	jobject thiz
) {
	jclass clzz = env->GetObjectClass(thiz);
	env->SetIntField(thiz, env->GetFieldID(clzz, "vid", "I"), 0x55AA);
	env->SetIntField(thiz, env->GetFieldID(clzz, "addr", "I"), 0x55AA);
}

extern "C" JNIEXPORT jboolean JNICALL Java_prj_reheating_DevDIO3232_readIBit(
	JNIEnv * env,
	jobject thiz,
	jint bit
) {
	return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_prj_reheating_DevDIO3232_readOBit(
	JNIEnv * env,
	jobject thiz,
	jint bit
) {
	return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_writeOBit(
	JNIEnv * env,
	jobject thiz,
	jint bit,
	jboolean val
) {
}

extern "C" JNIEXPORT jint JNICALL Java_prj_reheating_DevDIO3232_getPort(
	JNIEnv * env,
	jobject thiz,
	jint port
) {
	return 0xFF;
}

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_setPort(
	JNIEnv * env,
	jobject thiz,
	jint port,
	jint val
) {
}

extern "C" JNIEXPORT void JNICALL Java_prj_reheating_DevDIO3232_close(
	JNIEnv * env,
	jobject thiz
) {
}
#endif
