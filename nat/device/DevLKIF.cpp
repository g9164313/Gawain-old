#include "LKIF2.h"
#include <global.hpp>

#define JNI_PREFIX_NAME(name) Java_prj_daemon_DevLKIF2_##name

#define DEC_FUNC(name, args) \
	rc = ##name##args; \
	do{ if (rc != RC_OK) { \
		cout << "[FAIL]:"#name << "=x" << hex << rc << endl; \
	}} while(false)

#define RET_FUNC(name, args) \
	rc = ##name##args; \
	do{ if (rc != RC_OK) { \
		cout << "[FAIL]:"#name << "=x" << hex << rc << endl; \
		return; \
	}} while(false)

//------------------------------------//

extern "C" JNIEXPORT void JNI_PREFIX_NAME(getSysInfo)(
	JNIEnv * env,
	jobject thiz,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(setSysInfo)(
	JNIEnv * env,
	jobject thiz,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}
//------------------------------------//

extern "C" JNIEXPORT void JNI_PREFIX_NAME(getHead)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	RET_FUNC(
		LKIF2_GetAbleMode, 
		(idx, (LKIF_ABLEMODE*)(arg + 0))
	);
	RET_FUNC(
		LKIF2_GetAbleMinMax , 
		(idx, (int*)(arg + 1), (int*)(arg + 2))
	);
	RET_FUNC(
		LKIF2_GetMeasureMode, 
		(idx, (LKIF_MEASUREMODE*)(arg + 3))
	);
	RET_FUNC(
		LKIF2_GetBasicPoint,
		(idx, (LKIF_BASICPOINT*)(arg + 4))
	);
	RET_FUNC(
		LKIF2_GetReflectionMode,
		(idx, (LKIF_REFLECTIONMODE*)(arg + 5))
	);
	RET_FUNC(
		LKIF2_GetMask,
		(idx, (BOOL*)(arg + 6), (int*)(arg + 7), (int*)(arg + 8))		
	);
	RET_FUNC(
		LKIF2_GetMedian,
		(idx, (LKIF_MEDIAN*)(arg + 9))
	);
	RET_FUNC(
		LKIF2_GetLaserCtrlGroup,
		(idx, (LKIF_LASER_CTRL_GROUP*)(arg + 10))
	);
	RET_FUNC(
		LKIF2_GetRange,
		(idx, (LKIF_RANGE*)(arg + 11))
	);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(setHead)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	RET_FUNC(
		LKIF2_SetAbleMode,
		(idx, (LKIF_ABLEMODE)(arg[0]))
	);
	RET_FUNC(
		LKIF2_SetAbleMinMax,
		(idx, (int)(arg[1]), (int)(arg[2]))
	);
	RET_FUNC(
		LKIF2_SetMeasureMode,
		(idx, (LKIF_MEASUREMODE)(arg[3]))
	);
	RET_FUNC(
		LKIF2_SetBasicPoint,
		(idx, (LKIF_BASICPOINT)(arg[4]))
	);
	RET_FUNC(
		LKIF2_SetReflectionMode,
		(idx, (LKIF_REFLECTIONMODE)(arg[5]))
	);
	RET_FUNC(
		LKIF2_SetMask,
		(idx, (BOOL)(arg[6]), (int)(arg[7]), (int)(arg[8]))
	);
	RET_FUNC(
		LKIF2_SetMedian,
		(idx, (LKIF_MEDIAN)(arg[9]))
	);
	RET_FUNC(
		LKIF2_SetLaserCtrlGroup,
		(idx, (LKIF_LASER_CTRL_GROUP)(arg[10]))
	);
	RET_FUNC(
		LKIF2_SetRange,
		(idx, (LKIF_RANGE)(arg[11]))
	);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}
//------------------------------------//

extern "C" JNIEXPORT void JNI_PREFIX_NAME(getOuts)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(setOuts)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}
//------------------------------------//

extern "C" JNIEXPORT void JNI_PREFIX_NAME(measure)(
	JNIEnv * env,
	jobject thiz
) {
	RC rc;

}
//------------------------------------//

static void prepare_payload(
	JNIEnv* env,
	jobject thiz
) {
	RC rc;
	
	int cnt_head=0, cnt_outs=0;	
	DEC_FUNC(LKIF2_GetNumOfUsedHeads,(&cnt_head));
	DEC_FUNC(LKIF2_GetNumOfUsedOut  ,(&cnt_outs));

	jmethodID mid = env->GetMethodID(
		env->GetObjectClass(thiz),
		"preparePayload",
		"(II)V"
	);
	env->CallVoidMethod(
		thiz, mid,
		cnt_head,
		cnt_outs
	);
}

extern "C" JNIEXPORT jint JNI_PREFIX_NAME(openUSB)(
	JNIEnv * env,
	jobject thiz
) {
	RC rc = LKIF2_OpenDeviceUsb();
	if (rc == RC_OK) {
		prepare_payload(env,thiz);
	}
	return (jint)rc;
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(closeDev)(
	JNIEnv * env,
	jobject thiz
) {
	LKIF2_CloseDevice();
}



