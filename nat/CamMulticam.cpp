/*
 * CamMulticam.cpp
 *
 *  Created on: 2016年8月8日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>
#include "multicam.h"

struct MC_TOKEN {
	bool signal;
	Mat* matx;
	MCHANDLE channel;
};
typedef struct MC_TOKEN McToken;

static void McCallback(PMCCALLBACKINFO info);

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamMulticam_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jintArray jarrId,
	jobjectArray jarrTxt
){
	MACRO_SETUP_BEG

	char name[500];
	McToken* token = new McToken();
	MCSTATUS status;

	jint indxMcBoard =getJint(env,thiz,"indxMcBoard");

	env->SetLongField(bundle,idCntx,(jlong)(token));//update bundle information

	status = McOpenDriver(NULL);
	if(status!=MC_OK){
		cout<<"fail to open driver"<<endl;
		return;
	}

	status = McCreate(MC_CHANNEL, &(token->channel));
	if(status!=MC_OK){
		cout<<"fail to create channel"<<endl;
		return;
	}
	cout<<"create channel#"<<(token->channel)<<endl;

	status = McSetParamInt(
		MC_BOARD + indxMcBoard,
		MC_BoardTopology,
		MC_BoardTopology_MONO_DECA
	);//use "MC_BOARD" to set topology
	if(status!=MC_OK){
		cout<<"fail to set topology"<<endl;
		return;
	}

	status = McSetParamInt(
		token->channel,
		MC_DriverIndex,
		indxMcBoard
	);//set configuration.1
	if(status!=MC_OK){
		cout<<"fail to set board index"<<endl;
		return;
	}

	status = McSetParamStr(
		token->channel,
		MC_Connector,
		"M"
	);//set configuration.2
	if(status!=MC_OK){
		cout<<"fail to set connector"<<endl;
		return;
	}

	status = McSetParamStr(
		token->channel,
		MC_CamFile,
		"/opt/euresys/multicam/cameras/BASLER/raL12288-66km/raL12288-66km_L12288SP.cam"
	);//set configuration.3
	if(status!=MC_OK){
		cout<<"fail to set camfile"<<endl;
		return;
	}


	/*jsize len = env->GetArrayLength(jarrId);
	jint* arrId = env->GetIntArrayElements(jarrId,NULL);
	for(jsize i=0; i<len; i++){
		MCPARAMID id = arrId[i];
		jstring jtxt = (jstring) (env->GetObjectArrayElement(jarrTxt, i));
		const char *txt = env->GetStringUTFChars(jtxt, 0);
		cout<<"set ID="<<id<<", TXT="<<txt<<endl;
		status = McSetParamStr(token->channel, id, txt);
		if(status!=MC_OK){
			cout<<"fail to set ("<<id<<"):"<<txt<<endl;
		}
		env->ReleaseStringUTFChars(jtxt, txt);
	}*/

	status = McRegisterCallback(token->channel, McCallback, token);
	if(status!=MC_OK){
		cout<<"fail to register callback"<<endl;
		return;
	}

	status = McSetParamInt(
		token->channel,
		MC_SignalEnable + MC_SIG_SURFACE_PROCESSING,
		MC_SignalEnable_ON
	);
	if(status!=MC_OK){
		cout<<"fail to register signal.1"<<endl;
		return;
	}

	status = McSetParamInt(
		token->channel,
		MC_SignalEnable + MC_SIG_ACQUISITION_FAILURE,
		MC_SignalEnable_ON
	);
	if(status!=MC_OK){
		cout<<"fail to register signal.2"<<endl;
		return;
	}

	int32_t sizeX,sizeY,sizePitch;

	McGetParamInt(token->channel, MC_ImageSizeX, &sizeX);
	setJint(env,thiz,"sizeX",sizeX);

	McGetParamInt(token->channel, MC_ImageSizeY, &sizeY);
	setJint(env,thiz,"sizeY",sizeY);

	McGetParamInt(token->channel, MC_BufferPitch, &sizePitch);
	setJint(env,thiz,"sizePitch",sizePitch);

	token->matx = new Mat(480,640,CV_8UC1);

	env->SetLongField(bundle,idMatx,(jlong)(token->matx));//update bundle information

	setJbool(env,thiz,"staOK",true);//finally
}

void McCallback(PMCCALLBACKINFO info){
	McToken* token = ((McToken*)info->Context);
	uint8_t* buff;
	switch(info->Signal){
	case MC_SIG_SURFACE_PROCESSING:
		McGetParamPtr(
			((MCHANDLE)info->SignalInfo),
			MC_SurfaceAddr,
			(PVOID*)&buff
		);
		//update buffer
		printf("update buff=0x%p\n",(void*)buff);
		break;
	case MC_SIG_ACQUISITION_FAILURE:
		break;
	}
	token->signal = true;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamMulticam_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG_V

	MCSTATUS status = 0;

	McToken* token = ((McToken*)cntx);
	token->signal = false;//reset it~~~

	status = McSetParamInt(
		token->channel,
		MC_ChannelState,
		MC_ChannelState_ACTIVE
	);
	if(status==MC_OK){
		while((token->signal==false));
		status= McSetParamInt(
			token->channel,
			MC_ChannelState,
			MC_ChannelState_IDLE
		);
		if(status==MC_OK){
			return;//everything is fine!!!!
		}
	}
	cout<<"fail to active channel#"<<(token->channel)<<", error code ="<<status<<endl;
	setJbool(env,thiz,"staOK",false);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamMulticam_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_BEG

	McToken* token = ((McToken*)cntx);

	cout<<"delete channel#"<<(token->channel)<<endl;
	McDelete(token->channel);

	McCloseDriver();

	delete (token);

	MACRO_CLOSE_END
}
