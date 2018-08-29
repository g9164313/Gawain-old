#include <Windows.h>
#include <jni.h>
#include <Dask.h>

extern jsize jstrcpy(JNIEnv* env, jstring src, const char* dst);

#define _beg_boo(objArray, ptrx, value) \
	jboolean* ptrx = env->GetBooleanArrayElements(objArray,NULL); \
	BOOLEAN value;
#define _end_boo(objArray, ptrx, value) \
	ptrx[0] = (value!=0)?(JNI_TRUE):(JNI_FALSE); \
	env->ReleaseBooleanArrayElements(objArray,ptrx,NULL);

#define _beg_u8(objArray, ptrx, value) \
	jbyte* ptrx = env->GetByteArrayElements(objArray,NULL); \
	U8 value;
#define _end_u8(objArray, ptrx, value) \
	ptrx[0] = (U8)value; \
	env->ReleaseByteArrayElements(objArray,ptrx,NULL);

#define _beg_u16(objArray, ptrx, value) \
	jint* ptrx = env->GetIntArrayElements(objArray,NULL); \
	U16 value;
#define _end_u16(objArray, ptrx, value) \
	ptrx[0] = value; \
	env->ReleaseIntArrayElements(objArray,ptrx,NULL);

#define _beg_i16(objArray, ptrx, value) \
	jint* ptrx = env->GetIntArrayElements(objArray,NULL); \
	I16 value;
#define _end_i16(objArray, ptrx, value) \
	ptrx[0] = value; \
	env->ReleaseIntArrayElements(objArray,ptrx,NULL);

#define _beg_u32(objArray, ptrx, value) \
	jint* ptrx = env->GetIntArrayElements(objArray,NULL); \
	U32 value;
#define _end_u32(objArray, ptrx, value) \
	ptrx[0] = (jint)value; \
	env->ReleaseIntArrayElements(objArray,ptrx,NULL);

#define _beg_f32(objArray, ptrx, value) \
	jfloat* ptrx = env->GetFloatArrayElements(objArray,NULL); \
	F32 value;
#define _end_f32(objArray, ptrx, value) \
	ptrx[0] = (jfloat)value; \
	env->ReleaseFloatArrayElements(objArray,ptrx,NULL);

#define _beg_f64(objArray, ptrx, value) \
	jdouble* ptrx = env->GetDoubleArrayElements(objArray,NULL); \
	F64 value;
#define _end_f64(objArray, ptrx, value) \
	ptrx[0] = (jdouble)value; \
	env->ReleaseDoubleArrayElements(objArray,ptrx,NULL);

#define _beg_handle(objArray, ptrx, value) \
	jlong* ptrx = env->GetLongArrayElements(objArray,NULL); \
	HANDLE value;
#define _end_handle(objArray, ptrx, value) \
	ptrx[0] = (jlong)value; \
	env->ReleaseLongArrayElements(objArray,ptrx,NULL);

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_RegisterCard(JNIEnv* env, jobject thiz, jint CardType, jint card_num){
	return Register_Card((U16) CardType, (U16) card_num);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_ReleaseCard(JNIEnv* env, jobject thiz, jint CardNumber){
	return Release_Card((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9111Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigSource, jint TrgMode,  jint TraceCnt){
	return AI_9111_Config((U16) CardNumber, (U16) TrigSource, (U16) TrgMode,  (U16) TraceCnt);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9112Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigSource){
	return AI_9112_Config((U16) CardNumber, (U16) TrigSource);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9113Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigSource){
	return AI_9113_Config((U16) CardNumber, (U16) TrigSource);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9114Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigSource){
	return AI_9114_Config((U16) CardNumber, (U16) TrigSource);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9114PreTrigConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint PreTrgEn, jint TraceCnt){
	return AI_9114_PreTrigConfig((U16) CardNumber, (U16) PreTrgEn, (U16) TraceCnt);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9116Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint PostCnt, jint MCnt, jint ReTrgCnt){
	return AI_9116_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U16) PostCnt, (U16) MCnt, (U16) ReTrgCnt);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9116CounterInterval(JNIEnv* env, jobject thiz, jint CardNumber, jint ScanIntrv,  jint SampIntrv){
	return AI_9116_CounterInterval((U16) CardNumber, (U32) ScanIntrv,  (U32) SampIntrv);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9118Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ModeCtrl, jint FunCtrl,  jint BurstCnt, jint PostCnt){
	return AI_9118_Config((U16) CardNumber, (U16) ModeCtrl, (U16) FunCtrl,  (U16) BurstCnt, (U16) PostCnt);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9221Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jboolean AutoResetBuf){
	return AI_9221_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,   AutoResetBuf);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9221CounterInterval(JNIEnv* env, jobject thiz, jint CardNumber, jint ScanIntrv,  jint SampIntrv){
	return AI_9221_CounterInterval((U16) CardNumber, (U32) ScanIntrv,  (U32) SampIntrv);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9812Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrgMode, jint TrgSrc,  jint TrgPol, jint ClkSel, jint TrgLevel, jint PostCnt){
	return AI_9812_Config((U16) CardNumber, (U16) TrgMode, (U16) TrgSrc,  (U16) TrgPol, (U16) ClkSel, (U16) TrgLevel, (U16) PostCnt);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9812SetDiv(JNIEnv* env, jobject thiz, jint CardNumber, jint PacerVal){
	return AI_9812_SetDiv((U16) CardNumber, (U32) PacerVal);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9524Config(JNIEnv* env, jobject thiz, jint CardNumber, jint Group, jint XMode,  jint ConfigCtrl, jint TrigCtrl, jint TrigValue){
	return AI_9524_Config((U16) CardNumber, (U16) Group, (U16) XMode,  (U16) ConfigCtrl, (U16) TrigCtrl, (U32) TrigValue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9524PollConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint Group, jint PollChannel,  jint PollRange, jint PollSpeed){
	return AI_9524_PollConfig((U16) CardNumber, (U16) Group, (U16) PollChannel,  (U16) PollRange, (U16) PollSpeed);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9524SetDSP(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint Mode,  jint DFStage, jint SPKRejThreshold){
	return AI_9524_SetDSP((U16) CardNumber, (U16) Channel, (U16) Mode,  (U16) DFStage, (U32) SPKRejThreshold);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9524GetEOCEvent(JNIEnv* env, jobject thiz, jint CardNumber, jint Group, jlongArray hEvent){	
//	_beg_handle(hEvent,arg1,val1)
//	jint res = AI_9524_GetEOCEvent((U16) CardNumber, (U16) Group, &val1);
//	_end_handle(hEvent,arg1,val1)
//	return res;
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9222Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTriggerCnt, jboolean AutoResetBuf){
//	return AI_9222_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTriggerCnt,  AutoResetBuf);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9222CounterInterval(JNIEnv* env, jobject thiz, jint CardNumber, jint ScanIntrv,  jint SampIntrv){
//	return AI_9222_CounterInterval((U16) CardNumber, (U32) ScanIntrv,  (U32) SampIntrv);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9223Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTriggerCnt, jboolean AutoResetBuf){
//	return AI_9223_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTriggerCnt,  AutoResetBuf);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI9223CounterInterval(JNIEnv* env, jobject thiz, jint CardNumber, jint ScanIntrv,  jint SampIntrv){
//	return AI_9223_CounterInterval((U16) CardNumber, (U32) ScanIntrv,  (U32) SampIntrv);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI922AConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTriggerCnt, jboolean AutoResetBuf){
//	return AI_922A_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTriggerCnt,  AutoResetBuf);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AI922ACounterInterval(JNIEnv* env, jobject thiz, jint CardNumber, jint ScanIntrv,  jint SampIntrv){
//	return AI_922A_CounterInterval((U16) CardNumber, (U32) ScanIntrv,  (U32) SampIntrv);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncCheck(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray Stopped, jintArray AccessCnt){
	_beg_boo(Stopped, arg1, val1)
	_beg_u32(AccessCnt, arg2, val2)
	jint res = AI_AsyncCheck((U16) CardNumber, &val1, &val2);
	_end_boo(Stopped, arg1, val1)
	_end_u32(AccessCnt, arg2, val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncClear(JNIEnv* env, jobject thiz, jint CardNumber, jintArray AccessCnt){
	_beg_u32(AccessCnt, arg, val)
	jint res = AI_AsyncClear((U16) CardNumber, &val);
	_end_u32(AccessCnt, arg, val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncDblBufferHalfReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray HalfReady,  jbooleanArray StopFlag){
	_beg_boo(HalfReady, arg1, val1)
	_beg_boo(StopFlag, arg2, val2)
	jint res = AI_AsyncDblBufferHalfReady((U16) CardNumber, &val1,  &val2);
	_end_boo(HalfReady, arg1, val1)
	_end_boo(StopFlag, arg2, val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncDblBufferMode(JNIEnv* env, jobject thiz, jint CardNumber, jboolean Enable){
	return AI_AsyncDblBufferMode((U16) CardNumber,  Enable);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncDblBufferTransfer(JNIEnv* env, jobject thiz, jint CardNumber, jintArray Buffer){
	//TODO:jint res = AI_AsyncDblBufferTransfer((U16) CardNumber, Buffer);
	return -1;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncDblBufferOverrun(JNIEnv* env, jobject thiz, jint CardNumber, jint op,  jintArray overrunFlag){
	//jint res = AI_AsyncDblBufferOverrun((U16) CardNumber, (U16) op,  overrunFlag);
	return -1;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncDblBufferHandled(JNIEnv* env, jobject thiz, jint CardNumber){
	return AI_AsyncDblBufferHandled((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncDblBufferToFile(JNIEnv* env, jobject thiz, jint CardNumber){
	return AI_AsyncDblBufferToFile((U16) CardNumber);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIAsyncReTrigNextReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray Ready,  jbooleanArray StopFlag, jintArray RdyTrigCnt){
//	_beg_boo(Ready, arg1, val1)
//	_beg_boo(StopFlag, arg2, val2)
//	_beg_u16(RdyTrigCnt, arg3, val3)
//	jint res = AI_AsyncReTrigNextReady((U16) CardNumber, &val1,  &val2, &val3);
//	_end_boo(Ready, arg1, val1)
//	_end_boo(StopFlag, arg2, val2)
//	_end_u16(RdyTrigCnt, arg3, val3)
//	return res;
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContReadChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jshortArray Buffer, jint ReadCount, jdouble SampleRate, jint SyncMode){
	jshort* arg = env->GetShortArrayElements(Buffer,NULL);
	jint res = AI_ContReadChannel((U16) CardNumber, (U16) Channel, (U16) AdRange, (U16*)arg, (U32)ReadCount, (double)SampleRate, (U16)SyncMode);
	env->ReleaseShortArrayElements(Buffer,arg,NULL);
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContReadMultiChannels(JNIEnv* env, jobject thiz, jint CardNumber, jint NumChans,  jintArray Chans, jintArray AdRanges, jintArray Buffer, jint ReadCount, jdouble SampleRate,  jint SyncMode){
	return -1;//TODO:jint res = AI_ContReadMultiChannels((U16) CardNumber, (U16) NumChans,  Chans, AdRanges, Buffer, (U32) ReadCount, (double) SampleRate,  (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContScanChannels(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jintArray Buffer, jint ReadCount, jdouble SampleRate, jint SyncMode){
	return -1;//TODO:jint res = AI_ContScanChannels((U16) CardNumber, (U16) Channel, (U16) AdRange,  Buffer, (U32) ReadCount, (double) SampleRate, (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContReadChannelToFile(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel,  jint AdRange, jstring FileName, jint ReadCount, jdouble SampleRate, jint SyncMode){
	char name[500];
	jstrcpy(env,FileName,name);
	return AI_ContReadChannelToFile((U16) CardNumber, (U16) Channel,  (U16) AdRange, (U8*)name, (U32) ReadCount, (double) SampleRate, (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContReadMultiChannelsToFile(JNIEnv* env, jobject thiz, jint CardNumber, jint NumChans,  jintArray Chans, jintArray AdRanges, jstring FileName, jint ReadCount, jdouble SampleRate,  jint SyncMode){
	//char name[500];
	//jstrcpy(env,FileName,name);
	//return AI_ContReadMultiChannelsToFile((U16) CardNumber, (U16) NumChans,  Chans, AdRanges, (U8*)name, (U32) ReadCount, (double) SampleRate,  (U16) SyncMode);
	return -1;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContScanChannelsToFile(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel,  jint AdRange, jstring FileName, jint ReadCount, jdouble SampleRate, jint SyncMode){
	char name[500];
	jstrcpy(env,FileName,name);
	return AI_ContScanChannelsToFile((U16) CardNumber, (U16) Channel,  (U16) AdRange, (U8*)name, (U32) ReadCount, (double) SampleRate, (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContStatus(JNIEnv* env, jobject thiz, jint CardNumber, jintArray Status){
	_beg_u16(Status,arg,val)
	jint res = AI_ContStatus((U16) CardNumber, &val);
	_end_u16(Status,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContBufferSetup(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer, jint ReadCount,  jintArray BufferId){
	_beg_u16(BufferId,arg,val)
	jint res = AI_ContBufferSetup((U16) CardNumber, Buffer, (U32) ReadCount,  &val);
	_end_u16(BufferId,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContBufferReset(JNIEnv* env, jobject thiz, jint CardNumber){
	return AI_ContBufferReset((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIEventCallBack(JNIEnv* env, jobject thiz, jint CardNumber, jint mode, jint EventType,  jint callbackAddr){
	return AI_EventCallBack((U16) CardNumber, (I16) mode, (I16) EventType,  (U32) callbackAddr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIInitialMemoryAllocated(JNIEnv* env, jobject thiz, jint CardNumber, jintArray MemSize){
	_beg_u32(MemSize,arg,val)
	jint res = AI_InitialMemoryAllocated((U16) CardNumber, &val);
	_end_u32(MemSize,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIReadChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jintArray Value){
	_beg_u16(Value,arg,val)
	jint res = AI_ReadChannel((U16) CardNumber, (U16) Channel, (U16) AdRange,  &val);
	_end_u16(Value,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIReadChannel32(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jintArray Value){
	_beg_u32(Value,arg,val)
	jint res = AI_ReadChannel32((U16) CardNumber, (U16) Channel, (U16) AdRange,  &val);
	_end_u32(Value,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIVReadChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jdoubleArray voltage){
	_beg_f64(voltage,arg,val)
	jint res = AI_VReadChannel((U16) CardNumber, (U16) Channel, (U16) AdRange,  &val);
	_end_f64(voltage,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIScanReadChannels(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jintArray Buffer){
	_beg_u16(Buffer,arg,val)
	jint res = AI_ScanReadChannels((U16) CardNumber, (U16) Channel, (U16) AdRange,  &val);
	_end_u16(Buffer,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIScanReadChannels32(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint AdRange,  jintArray Buffer){
	_beg_u32(Buffer,arg,val)
	jint res = AI_ScanReadChannels32((U16) CardNumber, (U16) Channel, (U16) AdRange,  &val);
	_end_u32(Buffer,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIReadMultiChannels(JNIEnv* env, jobject thiz, jint CardNumber, jint NumChans, jintArray Chans,  jintArray AdRanges, jintArray Buffer){
	return -1;//TODO:jint res = AI_ReadMultiChannels((U16) CardNumber, (U16) NumChans, Chans,  AdRanges, Buffer);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIVoltScale(JNIEnv* env, jobject thiz, jint CardNumber, jint AdRange, jint reading,  jdoubleArray voltage){
	return -1;//TODO:AI_VoltScale((U16) CardNumber, (U16) AdRange, (I32) reading,  voltage);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIVoltScale32(JNIEnv* env, jobject thiz, jint CardNumber, jint adRange, jint reading,  jdoubleArray voltage){
	return -1;//TODO:AI_VoltScale32((U16) CardNumber, (U16) adRange, (I32) reading,  voltage);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIContVScale(JNIEnv* env, jobject thiz, jint CardNumber, jint adRange, jbyteArray readingArray,  jdoubleArray voltageArray, jint count){
	return -1;//TODO:AI_ContVScale((U16) CardNumber, (U16) adRange, readingArray,  voltageArray, (I32) count);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AISetTimeOut(JNIEnv* env, jobject thiz, jint CardNumber, jint TimeOut){
	return AI_SetTimeOut((U16) CardNumber, (U32) TimeOut);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO6202Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTrgCnt, jint DLY1Cnt, jint DLY2Cnt, jboolean AutoResetBuf){
	return AO_6202_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTrgCnt, (U32) DLY1Cnt, (U32) DLY2Cnt,  AutoResetBuf);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO6208AConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint V2AMode){
	return AO_6208A_Config((U16) CardNumber, (U16) V2AMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO6308AConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint V2AMode){
	return AO_6308A_Config((U16) CardNumber, (U16) V2AMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO6308VConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint OutputPolarity,  jdouble refVoltage){
	return AO_6308V_Config((U16) CardNumber, (U16) Channel, (U16) OutputPolarity,  (double) refVoltage);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO9111Config(JNIEnv* env, jobject thiz, jint CardNumber, jint OutputPolarity){
	return AO_9111_Config((U16) CardNumber, (U16) OutputPolarity);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO9112Config(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jdouble refVoltage){
	return AO_9112_Config((U16) CardNumber, (U16) Channel, (double) refVoltage);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO9222Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTrgCnt, jint DLY1Cnt, jint DLY2Cnt, jboolean AutoResetBuf){
//	return AO_9222_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTrgCnt, (U32) DLY1Cnt, (U32) DLY2Cnt,  AutoResetBuf);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AO9223Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTrgCnt, jint DLY1Cnt, jint DLY2Cnt, jboolean AutoResetBuf){
//	return AO_9223_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTrgCnt, (U32) DLY1Cnt, (U32) DLY2Cnt,  AutoResetBuf);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOAsyncCheck(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray Stopped, jintArray AccessCnt){
	_beg_boo(Stopped,arg1,val1)
	_beg_u32(AccessCnt,arg2,val2)
	jint res = AO_AsyncCheck((U16) CardNumber, &val1, &val2);
	_end_boo(Stopped,arg1,val1)
	_end_u32(AccessCnt,arg2,val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOAsyncClear(JNIEnv* env, jobject thiz, jint CardNumber, jintArray AccessCnt, jint stop_mode){
	_beg_u32(AccessCnt,arg,val)
	jint res = AO_AsyncClear((U16) CardNumber, &val, (U16) stop_mode);
	_end_u32(AccessCnt,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOAsyncDblBufferHalfReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray bHalfReady){
	_beg_boo(bHalfReady,arg,val)
	jint res = AO_AsyncDblBufferHalfReady((U16) CardNumber, &val);
	_end_boo(bHalfReady,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOAsyncDblBufferMode(JNIEnv* env, jobject thiz, jint CardNumber, jboolean Enable){
	return AO_AsyncDblBufferMode((U16) CardNumber,  (BOOLEAN)Enable);
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOContBufferCompose(JNIEnv* env, jobject thiz, jint CardNumber, jint TotalChnCount,  jint ChnNum, jint UpdateCount, jbyteArray ConBuffer, jbyteArray Buffer){
	return AO_ContBufferCompose((U16) CardNumber, (U16) TotalChnCount,  (U16) ChnNum, (U32) UpdateCount, ConBuffer, Buffer);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOContBufferReset(JNIEnv* env, jobject thiz, jint CardNumber){
	return AO_ContBufferReset((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOContBufferSetup(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer, jint WriteCount,  jintArray BufferId){
	_beg_u16(BufferId,arg,val)
	jint res = AO_ContBufferSetup((U16) CardNumber, Buffer, (U32) WriteCount, &val);
	_end_u16(BufferId,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOContStatus(JNIEnv* env, jobject thiz, jint CardNumber, jintArray Status){
	_beg_u16(Status,arg,val)
	jint res = AO_ContStatus((U16) CardNumber, &val);
	_end_u16(Status,arg,val)
	return res;
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOContWriteChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint BufId,  jint WriteCount, jint Iterations, jint CHUI, jint definite, jint SyncMode){
	return AO_ContWriteChannel((U16) CardNumber, (U16) Channel, (U16) BufId,  (U32) WriteCount, (U32) Iterations, (U32) CHUI, (U16) definite, (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOContWriteMultiChannels(JNIEnv* env, jobject thiz, jint CardNumber, jint NumChans,  jintArray Chans, jint BufId, jint WriteCount, jint Iterations, jint CHUI,  jint definite, jint SyncMode){
	_beg_u16(Chans,arg,val)
	jint res = AO_ContWriteMultiChannels((U16) CardNumber, (U16) NumChans, &val, (U16) BufId, (U32) WriteCount, (U32) Iterations, (U32) CHUI,  (U16) definite, (U16) SyncMode);
	_end_u16(Chans,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOEventCallBack(JNIEnv* env, jobject thiz, jint CardNumber, jint mode, jint EventType,  jint callbackAddr){
	return AO_EventCallBack((U16) CardNumber, (I16) mode, (I16) EventType,  (U32) callbackAddr);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOInitialMemoryAllocated(JNIEnv* env, jobject thiz, jint CardNumber, jintArray MemSize){
//	_beg_u32(MemSize,arg,val)
//	jint res = AO_InitialMemoryAllocated((U16) CardNumber, &val);
//	_end_u32(MemSize,arg,val)
//	return res;
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOSetTimeOut(JNIEnv* env, jobject thiz, jint CardNumber, jint TimeOut){
	return AO_SetTimeOut((U16) CardNumber, (U32) TimeOut);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOSimuVWriteChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Group, jdoubleArray VBuffer){
	_beg_f64(VBuffer,arg,val)
	jint res = AO_SimuVWriteChannel((U16) CardNumber, (U16) Group, &val);
	_end_f64(VBuffer,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOSimuWriteChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Group, jintArray Buffer){
	_beg_i16(Buffer,arg,val)
	jint res = AO_SimuWriteChannel((U16) CardNumber, (U16) Group, &val);
	_end_i16(Buffer,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOVoltScale(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jdouble Voltage,  jintArray binValue){
	_beg_i16(binValue,arg,val)
	jint res = AO_VoltScale((U16) CardNumber, (U16) Channel, (double) Voltage, &val);
	_end_i16(binValue,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOVWriteChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jdouble Voltage){
	return AO_VWriteChannel((U16) CardNumber, (U16) Channel, (double) Voltage);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOWriteChannel(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint Value){
	return AO_WriteChannel((U16) CardNumber, (U16) Channel, (I16) Value);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7200Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigSource, jint ExtTrigEn,  jint TrigPol, jint I_REQ_Pol){
	return DI_7200_Config((U16) CardNumber, (U16) TrigSource, (U16) ExtTrigEn,  (U16) TrigPol, (U16) I_REQ_Pol);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7233ForceLogic(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl){
//	return DI_7233_ForceLogic((U16) CardNumber, (U16) ConfigCtrl);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7300AConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint PortWidth, jint TrigSource,  jint WaitStatus, jint Terminator, jint I_REQ_Pol, jboolean clear_fifo,  jboolean disable_di){
	return DI_7300A_Config((U16) CardNumber, (U16) PortWidth, (U16) TrigSource,  (U16) WaitStatus, (U16) Terminator, (U16) I_REQ_Pol,  clear_fifo,   disable_di);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7300BConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint PortWidth, jint TrigSource,  jint WaitStatus, jint Terminator, jint I_Cntrl_Pol, jboolean clear_fifo,  jboolean disable_di){
	return DI_7300B_Config((U16) CardNumber, (U16) PortWidth, (U16) TrigSource,  (U16) WaitStatus, (U16) Terminator, (U16) I_Cntrl_Pol,  clear_fifo,   disable_di);
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7350Config(JNIEnv* env, jobject thiz, jint CardNumber, jint DIPortWidth, jint DIMode,  jint DIWaitStatus, jint DIClkConfig){
	return DI_7350_Config((U16) CardNumber, (U16) DIPortWidth, (U16) DIMode,  (U16) DIWaitStatus, (U16) DIClkConfig);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7350ExportSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DPAMode, jint CLK_DPAVlaue){
	return DI_7350_ExportSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DPAMode, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7350ExtSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DDAMode, jint CLK_DPAMode, jint CLK_DDAVlaue, jint CLK_DPAVlaue){
	return DI_7350_ExtSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DDAMode, (U16) CLK_DPAMode, (U16) CLK_DDAVlaue, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7350SoftTriggerGen(JNIEnv* env, jobject thiz, jint CardNumber){
	return DI_7350_SoftTriggerGen((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7350TrigHSConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigConfig, jint DI_IPOL,  jint DI_REQSrc, jint DI_ACKSrc, jint DI_TRIGSrc, jint StartTrigSrc,  jint PauseTrigSrc, jint SoftTrigOutSrc, jint SoftTrigOutLength, jint TrigCount){
	return DI_7350_TrigHSConfig((U16) CardNumber, (U16) TrigConfig, (U16) DI_IPOL,  (U16) DI_REQSrc, (U16) DI_ACKSrc, (U16) DI_TRIGSrc, (U16) StartTrigSrc,  (U16) PauseTrigSrc, (U16) SoftTrigOutSrc, (U32) SoftTrigOutLength, (U32) TrigCount);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7350BurstHandShakeDelay(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Delay){
	return DI_7350_BurstHandShakeDelay((U16) CardNumber, (U8) Delay);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360Config(JNIEnv* env, jobject thiz, jint CardNumber, jint DIPortWidth, jint DIMode,  jint DIWaitStatus, jint DIClkConfig){
	return DI_7360_Config((U16) CardNumber, (U16) DIPortWidth, (U16) DIMode,  (U16) DIWaitStatus, (U16) DIClkConfig);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360ExportSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DPAMode, jint CLK_DPAVlaue){
	return DI_7360_ExportSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DPAMode, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360ExtSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DDAMode, jint CLK_DPAMode, jint CLK_DDAVlaue, jint CLK_DPAVlaue){
	return DI_7360_ExtSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DDAMode, (U16) CLK_DPAMode, (U16) CLK_DDAVlaue, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360SoftTriggerGen(JNIEnv* env, jobject thiz, jint CardNumber){
	return DI_7360_SoftTriggerGen((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360TrigHSConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigConfig, jint DI_IPOL,  jint DI_REQSrc, jint DI_ACKSrc, jint DI_TRIGSrc, jint StartTrigSrc,  jint PauseTrigSrc, jint SoftTrigOutSrc, jint SoftTrigOutLength, jint TrigCount){
	return DI_7360_TrigHSConfig((U16) CardNumber, (U16) TrigConfig, (U16) DI_IPOL,  (U16) DI_REQSrc, (U16) DI_ACKSrc, (U16) DI_TRIGSrc, (U16) StartTrigSrc,  (U16) PauseTrigSrc, (U16) SoftTrigOutSrc, (U32) SoftTrigOutLength, (U32) TrigCount);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360BurstHandShakeDelay(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Delay){
	return DI_7360_BurstHandShakeDelay((U16) CardNumber, (U8) Delay);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360HighSpeedMode(JNIEnv* env, jobject thiz, jint CardNumber, jint wEnable){
	return DI_7360_HighSpeedMode((U16) CardNumber, (U16) wEnable);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI7360SetDelayStep(JNIEnv* env, jobject thiz, jint CardNumber, jint Step){
	return DI_7360_SetDelayStep((U16) CardNumber, (U32) Step);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI9222Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTriggerCnt, jboolean AutoResetBuf){
	return DI_9222_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTriggerCnt,  AutoResetBuf);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DI9223Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTriggerCnt, jboolean AutoResetBuf){
	return DI_9223_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTriggerCnt,  AutoResetBuf);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncCheck(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray Stopped, jintArray AccessCnt){
	_beg_boo(Stopped,arg1,val1)
	_beg_u32(AccessCnt,arg2,val2)
	jint res = DI_AsyncCheck((U16) CardNumber, &val1, &val2);
	_end_boo(Stopped,arg1,val1)
	_end_u32(AccessCnt,arg2,val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncClear(JNIEnv* env, jobject thiz, jint CardNumber, jintArray AccessCnt){
	_beg_u32(AccessCnt,arg,val)
	jint res = DI_AsyncClear((U16) CardNumber, &val);
	_end_u32(AccessCnt,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncDblBufferHalfReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray HalfReady){
	_beg_boo(HalfReady,arg,val)
	jint res = DI_AsyncDblBufferHalfReady((U16) CardNumber, &val);
	_end_boo(HalfReady,arg,val)
	return res;
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncDblBufferHandled(JNIEnv* env, jobject thiz, jint CardNumber){
	return DI_AsyncDblBufferHandled((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncDblBufferMode(JNIEnv* env, jobject thiz, jint CardNumber, jboolean Enable){
	return DI_AsyncDblBufferMode((U16) CardNumber,  Enable);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncDblBufferToFile(JNIEnv* env, jobject thiz, jint CardNumber){
	return DI_AsyncDblBufferToFile((U16) CardNumber);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncDblBufferTransfer(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer){
	return DI_AsyncDblBufferTransfer((U16) CardNumber, Buffer);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncDblBufferOverrun(JNIEnv* env, jobject thiz, jint CardNumber, jint op,  jintArray overrunFlag){
	_beg_u16(overrunFlag,arg,val)
	jint res = DI_AsyncDblBufferOverrun((U16) CardNumber, (U16) op, &val);
	_end_u16(overrunFlag,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncMultiBuffersHandled(JNIEnv* env, jobject thiz, jint CardNumber, jint bufcnt,  jintArray  bufs){
	_beg_u16(bufs,arg,val)
	jint res = DI_AsyncMultiBuffersHandled((U16) CardNumber, (U16) bufcnt, &val);
	_end_u16(bufs,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncMultiBufferNextReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray NextReady,  jintArray BufferId){
	_beg_boo(NextReady,arg1,val1)
	_beg_u16(BufferId,arg2,val2)
	jint res = DI_AsyncMultiBufferNextReady((U16) CardNumber, &val1, &val2);
	_end_boo(NextReady,arg1,val1)
	_end_u16(BufferId,arg2,val2)
	return res;
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIAsyncReTrigNextReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray Ready,  jbooleanArray StopFlag, jintArray RdyTrigCnt){
	_beg_boo(Ready,arg1,val1)
	_beg_boo(StopFlag,arg2,val2)
	_beg_u16(RdyTrigCnt,arg3,val3)
	jint res = DI_AsyncReTrigNextReady((U16) CardNumber, &val1, &val2, &val3);
	_end_boo(Ready,arg1,val1)
	_end_boo(StopFlag,arg2,val2)
	_end_u16(RdyTrigCnt,arg3,val3)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContBufferReset(JNIEnv* env, jobject thiz, jint CardNumber){
	return DI_ContBufferReset((U16) CardNumber);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContBufferSetup(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer, jint ReadCount,  jintArray BufferId){
	return -1;//TODO:jint res = DI_ContBufferSetup((U16) CardNumber, Buffer, (U32) ReadCount,  BufferId);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContMultiBufferSetup(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer,  jint ReadCount, jintArray BufferId){
	return -1;//TODO:jint res = DI_ContMultiBufferSetup((U16) CardNumber, Buffer,  (U32) ReadCount, BufferId);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContMultiBufferStart(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jdouble SampleRate){
	return DI_ContMultiBufferStart((U16) CardNumber, (U16) Port, (double) SampleRate);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContReadPort(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jbyteArray Buffer,  jint ReadCount, jdouble SampleRate, jint SyncMode){
	return DI_ContReadPort((U16) CardNumber, (U16) Port, Buffer,  (U32) ReadCount, (double) SampleRate, (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContReadPortToFile(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jstring FileName,  jint ReadCount, jdouble SampleRate, jint SyncMode){
	char name[500];
	jstrcpy(env,FileName,name);
	return DI_ContReadPortToFile((U16) CardNumber, (U16) Port, (U8*)name,  (U32) ReadCount, (double) SampleRate, (U16) SyncMode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIContStatus(JNIEnv* env, jobject thiz, jint CardNumber, jintArray Status){
	_beg_u16(Status,arg,val)
	jint res = DI_ContStatus((U16) CardNumber, &val);
	_end_u16(Status,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIEventCallBack(JNIEnv* env, jobject thiz, jint CardNumber, jint mode, jint EventType,  jint callbackAddr){
	return DI_EventCallBack((U16) CardNumber, (I16) mode, (I16) EventType,  (U32) callbackAddr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIInitialMemoryAllocated(JNIEnv* env, jobject thiz, jint CardNumber, jintArray DmaSize){
	_beg_u32(DmaSize,arg,val)
	jint res = DI_InitialMemoryAllocated((U16) CardNumber, &val);
	_end_u32(DmaSize,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIReadLine(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Line, jintArray State){
	_beg_u16(State,arg,val)
	jint res = DI_ReadLine((U16) CardNumber, (U16) Port, (U16) Line, &val);
	_end_u16(State,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIReadPort(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jintArray Value){
	_beg_u32(Value,arg,val)
	jint res = DI_ReadPort((U16) CardNumber, (U16) Port, &val);
	_end_u32(Value,arg,val)
	return res;
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DISetTimeOut(JNIEnv* env, jobject thiz, jint CardNumber, jint TimeOut){
//	return DI_SetTimeOut((U16) CardNumber, (U32) TimeOut);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7200Config(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigSource, jint OutReqEn,  jint OutTrigSig){
	return DO_7200_Config((U16) CardNumber, (U16) TrigSource, (U16) OutReqEn,  (U16) OutTrigSig);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7300AConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint PortWidth, jint TrigSource,  jint WaitStatus, jint Terminator, jint O_REQ_Pol){
	return DO_7300A_Config((U16) CardNumber, (U16) PortWidth, (U16) TrigSource,  (U16) WaitStatus, (U16) Terminator, (U16) O_REQ_Pol);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7300BConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint PortWidth, jint TrigSource,  jint WaitStatus, jint Terminator, jint O_Cntrl_Pol, jint FifoThreshold){
	return DO_7300B_Config((U16) CardNumber, (U16) PortWidth, (U16) TrigSource,  (U16) WaitStatus, (U16) Terminator, (U16) O_Cntrl_Pol, (U32) FifoThreshold);
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7300BSetDODisableMode(JNIEnv* env, jobject thiz, jint CardNumber, jint Mode){
	return DO_7300B_SetDODisableMode((U16) CardNumber, (U16) Mode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7350Config(JNIEnv* env, jobject thiz, jint CardNumber, jint DOPortWidth, jint DOMode,  jint DOWaitStatus, jint DOClkConfig){
	return DO_7350_Config((U16) CardNumber, (U16) DOPortWidth, (U16) DOMode,  (U16) DOWaitStatus, (U16) DOClkConfig);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7350ExportSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DPAMode, jint CLK_DPAVlaue){
	return DO_7350_ExportSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DPAMode, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7350ExtSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DDAMode, jint CLK_DPAMode, jint CLK_DDAVlaue, jint CLK_DPAVlaue){
	return DO_7350_ExtSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DDAMode, (U16) CLK_DPAMode, (U16) CLK_DDAVlaue, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7350SoftTriggerGen(JNIEnv* env, jobject thiz, jint CardNumber){
	return DO_7350_SoftTriggerGen((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7350TrigHSConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigConfig, jint DO_IPOL,  jint DO_REQSrc, jint DO_ACKSrc, jint DO_TRIGSrc, jint StartTrigSrc,  jint PauseTrigSrc, jint SoftTrigOutSrc, jint SoftTrigOutLength, jint TrigCount){
	return DO_7350_TrigHSConfig((U16) CardNumber, (U16) TrigConfig, (U16) DO_IPOL,  (U16) DO_REQSrc, (U16) DO_ACKSrc, (U16) DO_TRIGSrc, (U16) StartTrigSrc,  (U16) PauseTrigSrc, (U16) SoftTrigOutSrc, (U32) SoftTrigOutLength, (U32) TrigCount);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7350BurstHandShakeDelay(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Delay){
	return DO_7350_BurstHandShakeDelay((U16) CardNumber, (U8) Delay);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7360Config(JNIEnv* env, jobject thiz, jint CardNumber, jint DOPortWidth, jint DOMode,  jint DOWaitStatus, jint DOClkConfig){
	return DO_7360_Config((U16) CardNumber, (U16) DOPortWidth, (U16) DOMode,  (U16) DOWaitStatus, (U16) DOClkConfig);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7360ExportSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_Mode, jint CLK_DPAMode, jint CLK_DPAVlaue){
	return DO_7360_ExportSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_Mode, (U16) CLK_DPAMode, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7360ExtSampCLKConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint CLK_Src,  jint CLK_DDAMode, jint CLK_DPAMode, jint CLK_DDAVlaue, jint CLK_DPAVlaue){
	return DO_7360_ExtSampCLKConfig((U16) CardNumber, (U16) CLK_Src,  (U16) CLK_DDAMode, (U16) CLK_DPAMode, (U16) CLK_DDAVlaue, (U16) CLK_DPAVlaue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7360SoftTriggerGen(JNIEnv* env, jobject thiz, jint CardNumber){
	return DO_7360_SoftTriggerGen((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7360TrigHSConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint TrigConfig, jint DO_IPOL,  jint DO_REQSrc, jint DO_ACKSrc, jint DO_TRIGSrc, jint StartTrigSrc,  jint PauseTrigSrc, jint SoftTrigOutSrc, jint SoftTrigOutLength, jint TrigCount){
	return DO_7360_TrigHSConfig((U16) CardNumber, (U16) TrigConfig, (U16) DO_IPOL,  (U16) DO_REQSrc, (U16) DO_ACKSrc, (U16) DO_TRIGSrc, (U16) StartTrigSrc,  (U16) PauseTrigSrc, (U16) SoftTrigOutSrc, (U32) SoftTrigOutLength, (U32) TrigCount);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO7360BurstHandShakeDelay(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Delay){
	return DO_7360_BurstHandShakeDelay((U16) CardNumber, (U8) Delay);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_EDO9111Config(JNIEnv* env, jobject thiz, jint CardNumber, jint EDO_Fun){
	return EDO_9111_Config((U16) CardNumber, (U16) EDO_Fun);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO9222Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTrgCnt, jint DLY1Cnt, jint DLY2Cnt, jboolean AutoResetBuf){
//	return DO_9222_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTrgCnt, (U32) DLY1Cnt, (U32) DLY2Cnt,  AutoResetBuf);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DO9223Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ConfigCtrl, jint TrigCtrl,  jint ReTrgCnt, jint DLY1Cnt, jint DLY2Cnt, jboolean AutoResetBuf){
//	return DO_9223_Config((U16) CardNumber, (U16) ConfigCtrl, (U16) TrigCtrl,  (U32) ReTrgCnt, (U32) DLY1Cnt, (U32) DLY2Cnt,  AutoResetBuf);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOAsyncCheck(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray Stopped, jintArray AccessCnt){
	_beg_boo(Stopped,arg1,val1)
	_beg_u32(AccessCnt,arg2,val2)
	jint res = DO_AsyncCheck((U16) CardNumber, &val1, &val2);
	_end_boo(Stopped,arg1,val1)
	_end_u32(AccessCnt,arg2,val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOAsyncClear(JNIEnv* env, jobject thiz, jint CardNumber, jintArray AccessCnt){
	_beg_u32(AccessCnt,arg,val)
	jint res = DO_AsyncClear((U16) CardNumber, &val);
	_end_u32(AccessCnt,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOAsyncMultiBufferNextReady(JNIEnv* env, jobject thiz, jint CardNumber, jbooleanArray NextReady,  jintArray BufferId){
	_beg_boo(NextReady,arg1,val1)
	_beg_u16(BufferId,arg2,val2)
	jint res = DO_AsyncMultiBufferNextReady((U16) CardNumber, &val1, &val2);
	_end_boo(NextReady,arg1,val1)
	_end_u16(BufferId,arg2,val2)
	return res;
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContBufferReset(JNIEnv* env, jobject thiz, jint CardNumber){
//	return DO_ContBufferReset((U16) CardNumber);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContBufferSetup(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer, jint WriteCount, jintArray BufferId){
	return -1;//TODO:return DO_ContBufferSetup((U16) CardNumber, Buffer, (U32) WriteCount, BufferId);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContMultiBufferSetup(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer,  jint WriteCount, jintArray BufferId){
	return -1;//TODO:return DO_ContMultiBufferSetup((U16) CardNumber, Buffer,  (U32) WriteCount, BufferId);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContMultiBufferStart(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jdouble fSampleRate){
	return DO_ContMultiBufferStart((U16) CardNumber, (U16) Port, fSampleRate);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContStatus(JNIEnv* env, jobject thiz, jint CardNumber, jintArray Status){
	_beg_u16(Status,arg,val)
	jint res = DO_ContStatus((U16) CardNumber, &val);
	_end_u16(Status,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContWritePort(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jbyteArray Buffer,  jint WriteCount, jint Iterations, jdouble SampleRate, jint SyncMode){
	return DO_ContWritePort((U16) CardNumber, (U16) Port, Buffer,  (U32) WriteCount, (U16) Iterations, (double) SampleRate, (U16) SyncMode);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOContWritePortEx(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jbyteArray Buffer,  jint WriteCount, jint Iterations, jdouble SampleRate, jint SyncMode){
//	return DO_ContWritePortEx((U16) CardNumber, (U16) Port, Buffer,  (U32) WriteCount, (U16) Iterations, (double) SampleRate, (U16) SyncMode);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOEventCallBack(JNIEnv* env, jobject thiz, jint CardNumber, jint mode, jint EventType,  jint callbackAddr){
	return DO_EventCallBack((U16) CardNumber, (I16) mode, (I16) EventType,  (U32) callbackAddr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOInitialMemoryAllocated(JNIEnv* env, jobject thiz, jint CardNumber, jintArray MemSize){
	return -1;//TODO:return DO_InitialMemoryAllocated((U16) CardNumber, MemSize);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOPGStart(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray Buffer, jint WriteCount,  jdouble SampleRate){
	return DO_PGStart((U16) CardNumber, Buffer, (U32) WriteCount,  (double) SampleRate);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOPGStop(JNIEnv* env, jobject thiz, jint CardNumber){
	return DO_PGStop((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOReadLine(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Line, jintArray Value){
	_beg_u16(Value,arg,val)
	jint res = DO_ReadLine((U16) CardNumber, (U16) Port, (U16) Line, &val);
	_end_u16(Value,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOReadPort(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jintArray Value){
	_beg_u32(Value,arg,val)
	jint res = DO_ReadPort((U16) CardNumber, (U16) Port, &val);
	_end_u32(Value,arg,val)
	return res;
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOResetOutput(JNIEnv* env, jobject thiz, jint CardNumber, jint reset){
	return DO_ResetOutput((U16) CardNumber, (U16) reset);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOSetTimeOut(JNIEnv* env, jobject thiz, jint CardNumber, jint TimeOut){
	return DO_SetTimeOut((U16) CardNumber, (U32) TimeOut);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOSimuWritePort(JNIEnv* env, jobject thiz, jint CardNumber, jint NumChans, jintArray Buffer){
	return -1;//TODO: jint res = DO_SimuWritePort((U16) CardNumber, (U16) NumChans, Buffer);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOWriteExtTrigLine(JNIEnv* env, jobject thiz, jint CardNumber, jint Value){
	return DO_WriteExtTrigLine((U16) CardNumber, (U16) Value);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOWriteLine(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Line, jint Value){
	return DO_WriteLine((U16) CardNumber, (U16) Port, (U16) Line, (U16) Value);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOWritePort(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Value){
	return DO_WritePort((U16) CardNumber, (U16) Port, (U32) Value);
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIO7300SetInterrupt(JNIEnv* env, jobject thiz, jint CardNumber, jint AuxDIEn, jint T2En,  jlongArray Event){	
	DIO_7300SetInterrupt((U16) CardNumber, (I16) AuxDIEn, (I16) T2En,  Event);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIO7350AFIConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint AFI_Port, jint AFI_Enable,  jint AFI_Mode, jint AFI_TrigOutLen){
	return DIO_7350_AFIConfig((U16) CardNumber, (U16) AFI_Port, (U16) AFI_Enable,  (U16) AFI_Mode, (U32) AFI_TrigOutLen);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIO7360AFIConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint AFI_Port, jint AFI_Enable,  jint AFI_Mode, jint AFI_TrigOutLen){
	return DIO_7360_AFIConfig((U16) CardNumber, (U16) AFI_Port, (U16) AFI_Enable,  (U16) AFI_Mode, (U32) AFI_TrigOutLen);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOAUXDI_EventMessage(JNIEnv* env, jobject thiz, jint CardNumber, jint AuxDIEn,  jlong windowHandle, jint message, jbyteArray callbackAddr(JNIEnv* env, jobject thiz, )){
	return DIO_AUXDI_EventMessage((U16) CardNumber, ((I32)) AuxDIEn,   windowHandle, (U32) message, callbackAddr());
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOCOSInterruptCounter(JNIEnv* env, jobject thiz, jint CardNumber, jint Counter_Num,  jint Counter_Mode, jint DI_Port, jint DI_Line, jintArray Counter_Value){
	_beg_u32(Counter_Value,arg,val)
	jint res = DIO_COSInterruptCounter((U16) CardNumber, (U16) Counter_Num, (U16) Counter_Mode, (U16) DI_Port, (U16) DI_Line, &val);
	_end_u32(Counter_Value,arg,val)
	return res;
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOGetCOSLatchData(JNIEnv* env, jobject thiz, jint CardNumber, jintArray CosLData){
	_beg_u16(CosLData,arg,val)
	jint res = DIO_GetCOSLatchData((U16) CardNumber, &val);
	_end_u16(CosLData,arg,val)
	return res;
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOGetCOSLatchData32(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Port, jintArray CosLData){
//	_beg_u32(CosLData,arg,val)
//	jint res = DIO_GetCOSLatchData32((U16) CardNumber, (U8) Port, &val);
//	_end_u32(CosLData,arg,val)
//	return res;
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOGetCOSLatchDataInt32(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Port, jintArray CosLData){
//	_beg_u32(CosLData,arg,val)
//	jint res = DIO_GetCOSLatchDataInt32((U16) CardNumber, (U8) Port, &val);
//	_end_u32(CosLData,arg,val)
//	return res;
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOGetPMLatchData32(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jintArray PMLData){
//	_beg_u32(PMLData,arg,val)
//	jint res = DIO_GetPMLatchData32((U16) CardNumber, (U16) Port, &val);
//	_end_u32(PMLData,arg,val)
//	return res;
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOINT1_EventMessage(JNIEnv* env, jobject thiz, jint CardNumber, jint Int1Mode,  jlong windowHandle, jint message, jbyteArray callbackAddr(JNIEnv* env, jobject thiz, )){
//	return DIO_INT1_EventMessage((U16) CardNumber, ((I32)) Int1Mode,   windowHandle, (U32) message, callbackAddr());
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOINT2_EventMessage(JNIEnv* env, jobject thiz, jint CardNumber, jint Int2Mode,  jlong windowHandle, jint message, jbyteArray callbackAddr(JNIEnv* env, jobject thiz, )){
//	return DIO_INT2_EventMessage((U16) CardNumber, ((I32)) Int2Mode,   windowHandle, (U32) message, callbackAddr());
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOINT_Event_Message(JNIEnv* env, jobject thiz, jint CardNumber, jint mode, jlong evt,  jlong windowHandle, jint message, jint callbackAddr){
	return DIO_INT_Event_Message((U16) CardNumber, (I16) mode, (HANDLE)evt, (HANDLE)windowHandle, (U16) message, (U32) callbackAddr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOLineConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Line, jint Direction){
	return DIO_LineConfig((U16) CardNumber, (U16) Port, (U16) Line, (U16) Direction);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOLinesConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Linesdirmap){
	return DIO_LinesConfig((U16) CardNumber, (U16) Port, (U16) Linesdirmap);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOPortConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Direction){
	return DIO_PortConfig((U16) CardNumber, (U16) Port, (U16) Direction);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOPMConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint PM_ChnEn,  jint PM_ChnType){
//	return DIO_PMConfig((U16) CardNumber, (U16) Channel, (U16) PM_ChnEn,  (U16) PM_ChnType);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOPMControl(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint PM_Start,  jlongArray hEvent, jboolean ManualReset){
//	_beg_handle(hEvent,arg,val)
//	jint res = DIO_PMControl((U16) CardNumber, (U16) Port, (U16) PM_Start,  &val, (BOOLEAN)ManualReset);
//	_end_handle(hEvent,arg,val)
//	return res;
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOSetCOSInterrupt(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint ctlA, jint ctlB, jint ctlC){
	return DIO_SetCOSInterrupt((U16) CardNumber, (U16) Port, (U16) ctlA, (U16) ctlB, (U16) ctlC);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOSetCOSInterrupt32(JNIEnv* env, jobject thiz, jint CardNumber, jbyte Port, jint ctl,  jlongArray Event, jboolean ManualReset){
	return -1;//TODO:return DIO_SetCOSInterrupt32((U16) CardNumber, (U8) Port, (U32) ctl,  Event,  ManualReset);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOSetPMInterrupt32(JNIEnv* env, jobject thiz, jint CardNumber, jint Port, jint Ctrl,  jint Pattern1, jint Pattern2, jlongArray hEvent, jboolean ManualReset){
	return -1;//TODO:return DIO_SetPMInterrupt32((U16) CardNumber, (U16) Port, (U32) Ctrl,  (U32) Pattern1, (U32) Pattern2, hEvent,  ManualReset);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOSetDualInterrupt(JNIEnv* env, jobject thiz, jint CardNumber, jint Int1Mode, jint Int2Mode,  jlongArray Event){
	return -1;//TODO:return DIO_SetDualInterrupt((U16) CardNumber, (I32) Int1Mode, (I32) Int2Mode,  Event);
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOT2_EventMessage(JNIEnv* env, jobject thiz, jint CardNumber, jint T2En,  jlong windowHandle, jint message, jbyteArray callbackAddr(JNIEnv* env, jobject thiz, )){
//	return DIO_T2_EventMessage((U16) CardNumber, ((I32)) T2En,   windowHandle, (U32) message, callbackAddr());
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOVoltLevelConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint PortType, jint VoltLevel){
//	return DIO_VoltLevelConfig((U16) CardNumber, (U16) PortType, (U16) VoltLevel);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIOAFIConfig(JNIEnv* env, jobject thiz, jint wCardNumber, jint wAFI_Port,  	jint wAFI_Enable, jint wAFI_Mode, jint dwAFI_TrigOutLen){
//	return DIO_AFIConfig((U16) wCardNumber, (U16) wAFI_Port,  	(U16) wAFI_Enable, (U16) wAFI_Mode, (U32) dwAFI_TrigOutLen);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTR8554CK1Config(JNIEnv* env, jobject thiz, jint CardNumber, jint ClockSource){
	return CTR_8554_CK1_Config((U16) CardNumber, (U16) ClockSource);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTR_8554_ClkSrc_Config(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jint ClockSource){
	return CTR_8554_ClkSrc_Config((U16) CardNumber, (U16) Ctr, (U16) ClockSource);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTR8554DebounceConfig(JNIEnv* env, jobject thiz, jint CardNumber, jint DebounceClock){
	return CTR_8554_Debounce_Config((U16) CardNumber, (U16) DebounceClock);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRClear(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jint State){
	return CTR_Clear((U16) CardNumber, (U16) Ctr, (U16) State);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRRead(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jintArray Value){
	_beg_u32(Value,arg,val)
	jint res = CTR_Read((U16) CardNumber, (U16) Ctr, &val);
	_end_u32(Value,arg,val)
	return res;
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRReadAll(JNIEnv* env, jobject thiz, jint CardNumber, jint CtrCnt, jintArray Ctr, jintArray Value){
	_beg_u16(Ctr,arg1,val1)
	_beg_u32(Value,arg2,val2)
	jint res = CTR_Read_All((U16) CardNumber, (U16) CtrCnt, &val1, &val2);
	_end_u16(Ctr,arg1,val1)
	_end_u32(Value,arg2,val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRSetup(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jint Mode, jint Count,  jint BinBcd){
	return CTR_Setup((U16) CardNumber, (U16) Ctr, (U16) Mode, (U32) Count,  (U16) BinBcd);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRSetupAll(JNIEnv* env, jobject thiz, jint CardNumber, jint CtrCnt, jintArray Ctr, jintArray Mode,  jintArray Count, jintArray BinBcd){
	_beg_u16(Ctr, arg1, val1)
	_beg_u16(Mode, arg2, val2)
	_beg_u32(Count, arg3, val3)
	_beg_u16(BinBcd, arg4, val4)
	jint res = CTR_Setup_All((U16) CardNumber, (U16) CtrCnt, &val1, &val2, &val3, &val4);
	_end_u16(Ctr, arg1, val1)
	_end_u16(Mode, arg2, val2)
	_end_u32(Count, arg3, val3)
	_end_u16(BinBcd, arg4, val4)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRStatus(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jintArray Value){
	_beg_u32(Value, arg, val)
	jint res = CTR_Status((U16) CardNumber, (U16) Ctr, &val);
	_end_u32(Value, arg, val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_CTRUpdate(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jint Count){
	return CTR_Update((U16) CardNumber, (U16) Ctr, (U32) Count);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GCTRClear(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr){
	return GCTR_Clear((U16) CardNumber, (U16) GCtr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GCTRRead(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jintArray Value){
	_beg_u32(Value, arg, val)
	jint res = GCTR_Read((U16) CardNumber, (U16) GCtr, &val);
	_end_u32(Value, arg, val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GCTRSetup(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jint GCtrCtrl, jint Count){
	return GCTR_Setup((U16) CardNumber, (U16) GCtr, (U16) GCtrCtrl, (U32) Count);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTC9524PG_Config(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jint PulseGenNum){
	return GPTC_9524_PG_Config((U16) CardNumber, (U16) GCtr, (U32) PulseGenNum);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCClear(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr){
	return GPTC_Clear((U16) CardNumber, (U16) GCtr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCControl(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jint ParamID, jint Value){
	return GPTC_Control((U16) CardNumber, (U16) GCtr, (U16) ParamID, (U16) Value);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCEventCallBack(JNIEnv* env, jobject thiz, jint CardNumber, jint Enabled, jint EventType,  jint callbackAddr){
	return GPTC_EventCallBack((U16) CardNumber, (I16) Enabled, (I16) EventType,  (U32) callbackAddr);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCEventSetup(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jint Mode, jint Ctrl,  jint LVal_1, jint LVal_2){
	return GPTC_EventSetup((U16) CardNumber, (U16) GCtr, (U16) Mode, (U16) Ctrl,  (U32) LVal_1, (U32) LVal_2);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCRead(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jintArray Value){
	_beg_u32(Value, arg, val)
	jint res = GPTC_Read((U16) CardNumber, (U16) GCtr, &val);
	_end_u32(Value, arg, val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCSetup(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jint Mode, jint SrcCtrl,  jint PolCtrl, jint LReg1_Val, jint LReg2_Val){
	return GPTC_Setup((U16) CardNumber, (U16) GCtr, (U16) Mode, (U16) SrcCtrl,  (U16) PolCtrl, (U32) LReg1_Val, (U32) LReg2_Val);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTCStatus(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jintArray Value){
	_beg_u16(Value,arg,val)
	jint res = GPTC_Status((U16) CardNumber, (U16) GCtr, &val);
	_end_u16(Value,arg,val)
	return res;
}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTC9524GetTimerEvent(JNIEnv* env, jobject thiz, jint CardNumber, jint GCtr, jlongArray hEvent){
//	_beg_handle(hEvent,arg,val)
//	jint res = GPTC_9524_GetTimerEvent((U16) CardNumber, (U16) GCtr, &val);
//	_end_handle(hEvent,arg,val)
//	return res;
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GPTC9524SetCombineEcdData(JNIEnv* env, jobject thiz, jint CardNumber, jboolean enable){
//	return GPTC_9524_SetCombineEcdData((U16) CardNumber,  enable);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_WDTControl(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jint action){
	return WDT_Control((U16) CardNumber, (U16) Ctr, (U16) action);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_WDTReload(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jfloat ovflowSec,  jfloatArray actualSec){
	_beg_f32(actualSec,arg,val)
	jint res = WDT_Reload((U16) CardNumber, (U16) Ctr, (float) ovflowSec,  &val);
	_end_f32(actualSec,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_WDTSetup(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jfloat ovflowSec,  jfloatArray actualSec, jlongArray Event){
	_beg_f32(actualSec,arg1,val1)
	_beg_handle(Event,arg2,val2)
	jint res = WDT_Setup((U16) CardNumber, (U16) Ctr, (float) ovflowSec,  &val1, &val2);
	_end_f32(actualSec,arg1,val1)
	_end_handle(Event,arg2,val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_WDTStatus(JNIEnv* env, jobject thiz, jint CardNumber, jint Ctr, jintArray Value){
	_beg_u32(Value,arg,val)
	jint res = WDT_Status((U16) CardNumber, (U16) Ctr, &val);
	_end_u32(Value,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIGetEvent(JNIEnv* env, jobject thiz, jint CardNumber, jlongArray Event){
	_beg_handle(Event,arg,val)
	jint res = AI_GetEvent((U16) CardNumber, &val);
	_end_handle(Event,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AOGetEvent(JNIEnv* env, jobject thiz, jint CardNumber, jlongArray Event){
	_beg_handle(Event,arg,val)
	jint res = AO_GetEvent((U16) CardNumber, &val);
	_end_handle(Event,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIGetEvent(JNIEnv* env, jobject thiz, jint CardNumber, jlongArray Event){
	_beg_handle(Event,arg,val)
	jint res = DI_GetEvent((U16) CardNumber, &val);
	_end_handle(Event,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOGetEvent(JNIEnv* env, jobject thiz, jint CardNumber, jlongArray Event){
	_beg_handle(Event,arg,val)
	jint res = DO_GetEvent((U16) CardNumber, &val);
	_end_handle(Event,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_AIGetView(JNIEnv* env, jobject thiz, jint CardNumber, jintArray View){
	_beg_u32(View,arg,val)
	jint res = AI_GetView((U16) CardNumber, &val);
	_end_u32(View,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DIGetView(JNIEnv* env, jobject thiz, jint CardNumber, jintArray View){
	_beg_u32(View,arg,val)
	jint res = DI_GetView((U16) CardNumber, &val);
	_end_u32(View,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_DOGetView(JNIEnv* env, jobject thiz, jint CardNumber, jintArray View){
	_beg_u32(View,arg,val)
	jint res = DO_GetView((U16) CardNumber, &val);
	_end_u32(View,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetActualRate(JNIEnv* env, jobject thiz, jint CardNumber, jdouble SampleRate, jdoubleArray ActualRate){
	_beg_f64(ActualRate,arg,val)
	jint res = GetActualRate((U16) CardNumber, (double) SampleRate, &val);
	_end_f64(ActualRate,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetActualRate9524(JNIEnv* env, jobject thiz, jint CardNumber, jint Group, jdouble SampleRate,  jdoubleArray ActualRate){
	_beg_f64(ActualRate,arg,val)
	jint res = GetActualRate_9524((U16) CardNumber, (U16) Group, (double) SampleRate,  &val);
	_end_f64(ActualRate,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetBaseAddr(JNIEnv* env, jobject thiz, jint CardNumber, jintArray BaseAddr, jintArray BaseAddr2){
	_beg_u32(BaseAddr, arg1, val1)
	_beg_u32(BaseAddr2, arg2, val2)
	jint res = GetBaseAddr((U16) CardNumber, &val1, &val2);
	_end_u32(BaseAddr, arg1, val1)
	_end_u32(BaseAddr2, arg2, val2)
	return -1;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetCardIndexFromID(JNIEnv* env, jobject thiz, jint CardNumber, jintArray cardType, jintArray cardIndex){
	_beg_u16(cardType, arg1, val1)
	_beg_u16(cardIndex, arg2, val2)
	jint res = GetCardIndexFromID((U16) CardNumber, &val1, &val2);
	_end_u16(cardType, arg1, val1)
	_end_u16(cardIndex, arg2, val2)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetCardType(JNIEnv* env, jobject thiz, jint CardNumber, jintArray cardType){
	_beg_u16(cardType, arg, val)
	jint res = GetCardType((U16) CardNumber, &val);
	_end_u16(cardType, arg, val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetLCRAddr(JNIEnv* env, jobject thiz, jint CardNumber, jintArray LcrAddr){
	_beg_u32(LcrAddr,arg,val)
	jint res = GetLCRAddr((U16) CardNumber, &val);
	_end_u32(LcrAddr,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCIEEPROMLoadData(JNIEnv* env, jobject thiz, jint CardNumber, jint block, jintArray data){
	return -1;//TODO:return PCI_EEPROM_LoadData((U16) CardNumber, (U16) block, data);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCIEEPROMSaveData(JNIEnv* env, jobject thiz, jint CardNumber, jint block, jint data){
	return -1;//TODO:return PCI_EEPROM_SaveData((U16) CardNumber, (U16) block, (U16) data);
}

//TODO: extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_*  PCI_Buffer_Alloc (JNIEnv* env, jobject thiz, jint CardNumber, jlong BufferSize){
//	return *  PCI_Buffer_Alloc ((U16) CardNumber, (size_t) BufferSize);
//}

//extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCIBufferFree (JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray BufferAddr){
//	return PCI_Buffer_Free ((U16) CardNumber, BufferAddr);
//}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCIGetSerialNumber(JNIEnv* env, jobject thiz, jint wCardNumber, jbyteArray SerialString, jbyte numberOfElements, jbyteArray  actualread){
	return -1;//TODO:return PCI_GetSerialNumber((U16) wCardNumber, SerialString, (U8) numberOfElements,  actualread);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_EMGShutDownControl(JNIEnv* env, jobject thiz, jint CardNumber, jbyte ctrl){
	return EMGShutDownControl((U16) CardNumber, (U8) ctrl);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_EMGShutDownStatus(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray sts){
	return -1;//TODO:return EMGShutDownStatus((U16) CardNumber, sts);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_GetInitPattern(JNIEnv* env, jobject thiz, jint CardNumber, jbyte patID, jintArray pattern){
	_beg_u32(pattern,arg,val)
	jint res = GetInitPattern((U16) CardNumber, (U8) patID, &val);
	_end_u32(pattern,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_HotResetHoldControl(JNIEnv* env, jobject thiz, jint CardNumber, jbyte enable){
	return HotResetHoldControl((U16) CardNumber, (U8) enable);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_HotResetHoldStatus(JNIEnv* env, jobject thiz, jint CardNumber, jbyteArray sts){
	_beg_u8(sts,arg,val)
	jint res = HotResetHoldStatus((U16) CardNumber, &val);
	_end_u8(sts,arg,val)
	return res;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_IdentifyLEDControl(JNIEnv* env, jobject thiz, jint CardNumber, jbyte ctrl){
	return IdentifyLED_Control((U16) CardNumber, (U8) ctrl);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SetInitPattern(JNIEnv* env, jobject thiz, jint CardNumber, jbyte patID, jint pattern){
	return SetInitPattern((U16) CardNumber, (U8) patID, (U32) pattern);
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCI9524AcquireADCalConst(JNIEnv* env, jobject thiz, jint CardNumber, jint Group,  jint ADC_Range, jint ADC_Speed, jintArray CalDate, jfloatArray CalTemp, jintArray ADC_offset,  jintArray ADC_gain, jdoubleArray Residual_offset, jdoubleArray Residual_scaling){
	return -1;//TODO:return PCI9524_Acquire_AD_CalConst((U16) CardNumber, (U16) Group,  (U16) ADC_Range, (U16) ADC_Speed, CalDate, CalTemp, ADC_offset,  ADC_gain, Residual_offset, Residual_scaling);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCI9524AcquireDACalConst(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel,  jintArray CalDate, jfloatArray CalTemp, jbyteArray DAC_offset, jbyteArray DAC_linearity,  jfloatArray Gain_factor){
	return -1;//TODO:return PCI9524_Acquire_DA_CalConst((U16) CardNumber, (U16) Channel,  CalDate, CalTemp, DAC_offset, DAC_linearity,  Gain_factor);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCI9524ReadEEProm(JNIEnv* env, jobject thiz, jint CardNumber, jint ReadAddr, jbyteArray ReadData){
	return -1;//TODO:return PCI9524_Read_EEProm((U16) CardNumber, (U16) ReadAddr, ReadData);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCI9524ReadRemoteSPI(JNIEnv* env, jobject thiz, jint CardNumber, jint Addr, jbyteArray RdData){
	return -1;//TODO:return PCI9524_Read_RemoteSPI((U16) CardNumber, (U16) Addr, RdData);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCI9524WriteEEProm(JNIEnv* env, jobject thiz, jint CardNumber, jint WriteAddr, jbyteArray WriteData){
	return -1;//TODO:return PCI9524_Write_EEProm((U16) CardNumber, (U16) WriteAddr, WriteData);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCI9524WriteRemoteSPI(JNIEnv* env, jobject thiz, jint CardNumber, jint Addr, jbyte WrtData){
	return PCI9524_Write_RemoteSPI((U16) CardNumber, (U16) Addr, (U8) WrtData);
}*/

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCIDBAutoCalibrationALL(JNIEnv* env, jobject thiz, jint CardNumber){
	return PCI_DB_Auto_Calibration_ALL((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCIEEPROMCALConstantUpdate(JNIEnv* env, jobject thiz, jint CardNumber, jint bank){
	return PCI_EEPROM_CAL_Constant_Update((U16) CardNumber, (U16) bank);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PCILoadCALData(JNIEnv* env, jobject thiz, jint CardNumber, jint bank){
	return PCI_Load_CAL_Data((U16) CardNumber, (U16) bank);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SSISourceConn(JNIEnv* env, jobject thiz, jint CardNumber, jint sigCode){
	return SSI_SourceConn((U16) CardNumber, (U16) sigCode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SSISourceDisConn(JNIEnv* env, jobject thiz, jint CardNumber, jint sigCode){
	return SSI_SourceDisConn((U16) CardNumber, (U16) sigCode);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SSISourceClear(JNIEnv* env, jobject thiz, jint CardNumber){
	return SSI_SourceClear((U16) CardNumber);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PWMOutput(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel, jint high_interval,  jint low_interval){
	return PWM_Output((U16) CardNumber, (U16) Channel, (U32) high_interval,  (U32) low_interval);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_PWMStop(JNIEnv* env, jobject thiz, jint CardNumber, jint Channel){
	return PWM_Stop((U16) CardNumber, (U16) Channel);
}

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CSetup(JNIEnv* env, jobject thiz, jint CardNumber, jint I2C_Port, jint I2C_Config,  jint I2C_SetupValue1, jint I2C_SetupValue2){
	return I2C_Setup((U16) CardNumber, (U16) I2C_Port, (U16) I2C_Config,  (U32) I2C_SetupValue1, (U32) I2C_SetupValue2);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CControl(JNIEnv* env, jobject thiz, jint CardNumber, jint I2C_Port, jint I2C_CtrlParam,  jint I2C_CtrlValue){
	return I2C_Control((U16) CardNumber, (U16) I2C_Port, (U16) I2C_CtrlParam,  (U32) I2C_CtrlValue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CStatus(JNIEnv* env, jobject thiz, jint CardNumber, jint I2C_Port, jintArray I2C_Status){
	return -1;//return I2C_Status((U16) CardNumber, (U16) I2C_Port, I2C_Status);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CRead(JNIEnv* env, jobject thiz, jint CardNumber, jint I2C_Port, jint I2C_SlaveAddr,  jint I2C_CmdAddrBytes, jint I2C_DataBytes, jint I2C_CmdAddr, jintArray I2C_Data){
	return -1;//TODO:return I2C_Read((U16) CardNumber, (U16) I2C_Port, (U16) I2C_SlaveAddr,  (U16) I2C_CmdAddrBytes, (U16) I2C_DataBytes, (U32) I2C_CmdAddr, I2C_Data);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CWrite(JNIEnv* env, jobject thiz, jint CardNumber, jint I2C_Port, jint I2C_SlaveAddr,  jint I2C_CmdAddrBytes, jint I2C_DataBytes, jint I2C_CmdAddr, jint I2C_Data){
	return I2C_Write((U16) CardNumber, (U16) I2C_Port, (U16) I2C_SlaveAddr,  (U16) I2C_CmdAddrBytes, (U16) I2C_DataBytes, (U32) I2C_CmdAddr, (U32) I2C_Data);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CPhaseShiftSet(JNIEnv* env, jobject thiz, jint wCardNumber, jbyte bEnable){
	return PCI_I2C_PhaseShift_Set((U16) wCardNumber, (U8) bEnable);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_I2CPhaseShiftCheck(JNIEnv* env, jobject thiz, jint wCardNumber, jbooleanArray bEnable){
	_beg_boo(bEnable,arg,val)
	jint res = PCI_I2C_PhaseShift_Check((U16) wCardNumber, &val);
	_end_boo(bEnable,arg,val)
	return res;
}*/

/*extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SPISetup(JNIEnv* env, jobject thiz, jint CardNumber, jint SPI_Port, jint SPI_Config,  jint SPI_SetupValue1, jint SPI_SetupValue2){
	return SPI_Setup((U16) CardNumber, (U16) SPI_Port, (U16) SPI_Config,  (U32) SPI_SetupValue1, (U32) SPI_SetupValue2);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SPIControl(JNIEnv* env, jobject thiz, jint CardNumber, jint SPI_Port, jint SPI_CtrlParam,  jint SPI_CtrlValue){
	return SPI_Control((U16) CardNumber, (U16) SPI_Port, (U16) SPI_CtrlParam,  (U32) SPI_CtrlValue);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SPIStatus(JNIEnv* env, jobject thiz, jint CardNumber, jint SPI_Port, jintArray SPI_Status){
	return -1;//return SPI_Status((U16) CardNumber, (U16) SPI_Port, SPI_Status);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SPIRead(JNIEnv* env, jobject thiz, jint CardNumber, jint SPI_Port, jint SPI_SlaveAddr,  jint SPI_CmdAddrBits, jint SPI_DataBits, jint SPI_FrontDummyBits,  jint SPI_CmdAddr, jintArray SPI_Data){
	return -1;//return SPI_Read((U16) CardNumber, (U16) SPI_Port, (U16) SPI_SlaveAddr,  (U16) SPI_CmdAddrBits, (U16) SPI_DataBits, (U16) SPI_FrontDummyBits,  (U32) SPI_CmdAddr, SPI_Data);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_scada_DevDask_SPIWrite(JNIEnv* env, jobject thiz, jint CardNumber, jint SPI_Port, jint SPI_SlaveAddr,  jint SPI_CmdAddrBits, jint SPI_DataBits, jint SPI_FrontDummyBits,  jint SPI_TailDummyBits, jint SPI_CmdAddr, jint SPI_Data){
	return SPI_Write((U16) CardNumber, (U16) SPI_Port, (U16) SPI_SlaveAddr,  (U16) SPI_CmdAddrBits, (U16) SPI_DataBits, (U16) SPI_FrontDummyBits,  (U16) SPI_TailDummyBits, (U32) SPI_CmdAddr, (U32) SPI_Data);
}*/

