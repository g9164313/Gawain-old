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

static void getSysInfo(
	JNIEnv * env,
	jobject thiz,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);
	
	RET_FUNC(
		LKIF2_GetSamplingCycle,
		((LKIF_SAMPLINGCYCLE*)(arg + 0))
	);
	RET_FUNC(
		LKIF2_GetMutualInterferencePrevention,
		((LKIF_MUTUAL_INTERFERENCE_PREVENTION*)(arg + 1))
	);
	RET_FUNC(
		LKIF2_GetToleranceComparatorOutputFormat,
		((LKIF_TOLERANCE_COMPARATOR_OUTPUT_FORMAT*)(arg + 2))
	);
	RET_FUNC(
		LKIF2_GetStrobeTime,
		((LKIF_STOROBETIME*)(arg + 3))
	);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}
static void setSysInfo(
	JNIEnv * env,
	jobject thiz,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	RET_FUNC(
		LKIF2_SetSamplingCycle,
		((LKIF_SAMPLINGCYCLE)(arg[0]))
	);
	RET_FUNC(
		LKIF2_SetMutualInterferencePrevention,
		((LKIF_MUTUAL_INTERFERENCE_PREVENTION)(arg[1]))
	);
	RET_FUNC(
		LKIF2_SetToleranceComparatorOutputFormat,
		((LKIF_TOLERANCE_COMPARATOR_OUTPUT_FORMAT)(arg[2]))
	);
	RET_FUNC(
		LKIF2_SetStrobeTime,
		((LKIF_STOROBETIME)(arg[3]))
	);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(getSysInfo)(
	JNIEnv * env,
	jobject thiz,
	jintArray objArgs
) {
	LKIF2_StopMeasure();
	getSysInfo(env, thiz, objArgs);
	LKIF2_StartMeasure();
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(setSysInfo)(
	JNIEnv * env,
	jobject thiz,
	jintArray objArgs
) {
	LKIF2_StopMeasure();
	setSysInfo(env, thiz, objArgs);
	LKIF2_StartMeasure();
}
//------------------------------------//

static void getHead(
	JNIEnv* env,
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
		LKIF2_GetAbleMinMax,
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

static void setHead(
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

extern "C" JNIEXPORT void JNI_PREFIX_NAME(getHead)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
){
	LKIF2_StopMeasure();
	getHead(env, thiz, idx, objArgs);
	LKIF2_StartMeasure();
}
extern "C" JNIEXPORT void JNI_PREFIX_NAME(setHead)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	LKIF2_StopMeasure();
	setHead(env, thiz, idx, objArgs);
	LKIF2_StartMeasure();
}
//------------------------------------//

static void getOuts(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	RET_FUNC(
		LKIF2_GetTolerance,
		(idx, (int*)(arg + 0), (int*)(arg + 1), (int*)(arg + 2))
	);
	RET_FUNC(
		LKIF2_GetCalcMethod,
		(idx, (LKIF_CALCMETHOD*)(arg + 3), (int*)(arg + 4))
	);
	RET_FUNC(
		LKIF2_GetCalcTarget,
		(idx, (LKIF_CALCTARGET*)(arg + 5))
	);
	RET_FUNC(
		LKIF2_GetScaling,
		(idx, (int*)(arg + 6), (int*)(arg + 7), (int*)(arg + 8), (int*)(arg + 9))
	);
	RET_FUNC(
		LKIF2_GetFilter,
		(idx, (LKIF_FILTERMODE*)(arg + 10), (LKIF_FILTERPARA*)(arg + 11))
	);
	RET_FUNC(
		LKIF2_GetTriggerMode,
		(idx, (LKIF_TRIGGERMODE*)(arg + 12))
	);
	RET_FUNC(
		LKIF2_GetOffset,
		(idx, (int*)(arg + 13))
	);
	RET_FUNC(
		LKIF2_GetCalcMode,
		(idx, (LKIF_CALCMODE*)(arg + 14))
	);
	RET_FUNC(
		LKIF2_GetAnalogScaling,
		(idx, (int*)(arg + 15), (int*)(arg + 16), (int*)(arg + 17), (int*)(arg + 18))
	);
	RET_FUNC(
		LKIF2_GetDisplayUnit,
		(idx, (LKIF_DISPLAYUNIT*)(arg + 19))
	);
	RET_FUNC(
		LKIF2_GetMeasureType,
		(idx, (LKIF_MEASURETYPE*)(arg + 20))
	);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}

static void setOuts(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	RC rc;
	jint* arg = env->GetIntArrayElements(objArgs, NULL);

	RET_FUNC(
		LKIF2_SetTolerance,
		(idx, (int)(arg[0]), (int)(arg[1]), (int)(arg[2]))
	);
	RET_FUNC(
		LKIF2_SetCalcMethod,
		(idx, (LKIF_CALCMETHOD)(arg[3]), (int)(arg[4]))
	);
	RET_FUNC(
		LKIF2_SetCalcTarget,
		(idx, (LKIF_CALCTARGET)(arg[5]))
	);
	RET_FUNC(
		LKIF2_SetScaling,
		(idx, (int)(arg[6]), (int)(arg[7]), (int)(arg[8]), (int)(arg[9]))
	);
	RET_FUNC(
		LKIF2_SetFilter,
		(idx, (LKIF_FILTERMODE)(arg[10]), (LKIF_FILTERPARA)(arg[11]))
	);
	RET_FUNC(
		LKIF2_SetTriggerMode,
		(idx, (LKIF_TRIGGERMODE)(arg[12]))
	);
	RET_FUNC(
		LKIF2_SetOffset,
		(idx, (int)(arg[13]))
	);
	RET_FUNC(
		LKIF2_SetCalcMode,
		(idx, (LKIF_CALCMODE)(arg[14]))
	);
	RET_FUNC(
		LKIF2_SetAnalogScaling,
		(idx, (int)(arg[15]), (int)(arg[16]), (int)(arg[17]), (int)(arg[18]))
	);
	RET_FUNC(
		LKIF2_SetDisplayUnit,
		(idx, (LKIF_DISPLAYUNIT)(arg[19]))
	);
	RET_FUNC(
		LKIF2_SetMeasureType,
		(idx, (LKIF_MEASURETYPE)(arg[20]))
	);

	env->ReleaseIntArrayElements(objArgs, arg, 0);
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(getOuts)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	LKIF2_StopMeasure();
	getOuts(env, thiz, idx, objArgs);
	LKIF2_StartMeasure();
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(setOuts)(
	JNIEnv * env,
	jobject thiz,
	jint idx,
	jintArray objArgs
) {
	LKIF2_StopMeasure();
	setOuts(env, thiz, idx, objArgs);
	LKIF2_StartMeasure();
}
//------------------------------------//

extern "C" JNIEXPORT void JNI_PREFIX_NAME(implResetValue)(
	JNIEnv * env,
	jobject thiz,
	jint outID
) {
	RC rc;
	RET_FUNC(LKIF2_SetResetSingle,(outID));
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(measure)(
	JNIEnv * env,
	jobject thiz,
	jcharArray objRest,
	jfloatArray objMeas
) {
	jchar*  rest = env->GetCharArrayElements (objRest, NULL);
	jfloat* meas = env->GetFloatArrayElements(objMeas, NULL);

	jint cnt = env->GetArrayLength(objRest);

	RC rc;
	LKIF_FLOATVALUE_OUT vals[12];
	RET_FUNC(LKIF2_GetCalcDataMulti, (LKIF_OUTNO_ALL,vals));

	for (jint i=0; i<12; i++) {
		jint j = vals[i].OutNo;
		if (j >= cnt) {
			continue;
		}
		switch (vals[i].FloatResult) {
		case LKIF_FLOATRESULT_VALID      : rest[j] = '@'; break;
		case LKIF_FLOATRESULT_RANGEOVER_P: rest[j] = '+'; break;
		case LKIF_FLOATRESULT_RANGEOVER_N: rest[j] = '-'; break;
		case LKIF_FLOATRESULT_WAITING    : rest[j] = 'w'; break;
		case LKIF_FLOATRESULT_ALARM      : rest[j] = 'a'; break;
		case LKIF_FLOATRESULT_INVALID    : rest[j] = 'x'; break;
		}
		meas[j] = vals[i].Value;
	}
	env->ReleaseCharArrayElements (objRest, rest, 0);
	env->ReleaseFloatArrayElements(objMeas, meas, 0);
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

	getSysInfo(env, thiz, getJIntArray(env, thiz, "info"));
	
	//below lines, show how to access 2-dimension java array~~~
	jclass  clz = env->GetObjectClass(thiz);
	jobject obj;
	jobjectArray arr1;//array rows
	jintArray    arr2;//array columns
	//int cnt = env->GetArrayLength(arr1);

	obj = env->GetObjectField(
		thiz,
		env->GetFieldID(clz, "head", "[[I")
	);
	arr1= *(reinterpret_cast<jobjectArray*>(&obj)); 
	for (int idx = 0; idx<cnt_head;  idx++) {
		obj = env->GetObjectArrayElement(arr1, idx);
		arr2 = *(reinterpret_cast<jintArray*>(&obj));
		//jint* ptr = env->GetIntArrayElements(arr2, NULL);
		//ptr[0] = 100;//test~~~~
		//env->ReleaseIntArrayElements(arr2, ptr, 0);
		getHead(env, thiz, idx, arr2);
	}

	obj = env->GetObjectField(
		thiz,
		env->GetFieldID(clz, "outs", "[[I")
	);
	arr1 = *(reinterpret_cast<jobjectArray*>(&obj));
	for (int idx = 0; idx < cnt_outs; idx++) {
		obj = env->GetObjectArrayElement(arr1, idx);
		arr2 = *(reinterpret_cast<jintArray*>(&obj));
		getOuts(env, thiz, idx, arr2);
	}
}

extern "C" JNIEXPORT jint JNI_PREFIX_NAME(openUSB)(
	JNIEnv * env,
	jobject thiz
) {	
	RC rc;
	DEC_FUNC(LKIF2_OpenDeviceUsb,());
	if (rc != RC_OK) {
		return (jint)rc;
	}
	LKIF2_StopMeasure();
	if (rc == RC_OK) {
		prepare_payload(env,thiz);
	}
	LKIF2_StartMeasure();
	return (jint)rc;
}

extern "C" JNIEXPORT void JNI_PREFIX_NAME(closeDev)(
	JNIEnv * env,
	jobject thiz
) {
	LKIF2_CloseDevice();
}



