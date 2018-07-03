#include <global.hpp>

#include <windows.h>
#include <windef.h>
#include <driver.h>

static uint16_t msk = 0xFFFF;

extern "C" JNIEXPORT jint JNICALL Java_prj_epistar_DevPCI1762_getIn(
	JNIEnv * env, 
	jobject thiz,
	jlong handle
){
	if(handle==-1){ return 0; }
	LONG dev = handle;
	uint16_t val=0;
	PT_DioReadPortWord pt;
	pt.port = 0;
	pt.value=&val;
	pt.ValidChannelMask = &msk;	
	DRV_DioReadPortWord(dev,&pt);
	return (jint)(val);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_epistar_DevPCI1762_getOut(
	JNIEnv * env, 
	jobject thiz,
	jlong handle
){
	if(handle==-1){ return 0; }
	LONG dev = handle;
	uint16_t val=0;
	PT_DioGetCurrentDOWord pt;
	pt.port = 0;
	pt.value=&val;
	pt.ValidChannelMask = &msk;	
	DRV_DioGetCurrentDOWord(dev,&pt);
	return (jint)(val);
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_DevPCI1762_setOut(
	JNIEnv * env, 
	jobject thiz,
	jlong handle,
	jint val,
	jchar op
){
	if(handle==-1){ return; }
	jint cur = Java_prj_epistar_DevPCI1762_getOut(env,thiz,handle);
	switch(op){
	case '&'://and
		cur = cur & val;
		break;
	case '|'://or
		cur = cur | val;
		break;
	case '^'://xor
		cur = cur ^ val;
		break;
	default:
		cur = val;
		break;
	}
	LONG dev = handle;
	PT_DioWritePortWord pt;
	pt.port = 0;
	pt.state= cur;
	pt.mask = msk;
	DRV_DioWritePortWord(dev,&pt);
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_DevPCI1762_close(
	JNIEnv * env, 
	jobject thiz,
	jlong handle
){
	if(handle==-1){ return; }
	LONG tmp = handle;
	DRV_DeviceClose(&tmp);
}

extern "C" JNIEXPORT jlong JNICALL Java_prj_epistar_DevPCI1762_open(
	JNIEnv * env, 
	jobject thiz,
	jint id
){
	LONG handle;	
	LRESULT res = DRV_DeviceOpen(id, &handle);
	if(res!=SUCCESS){
		return -1L;
	}
	return (jlong)handle;
}



