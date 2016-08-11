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
	jstring jstrTopology,
	jstring jstrConnect,
	jstring jstrCamfile,
	jintArray jarrId,
	jobjectArray jarrTxt
){
	MACRO_SETUP_BEG

	char txtBuff[500];
	McToken* token = new McToken();
	MCSTATUS status;

	jint indxMcBoard =getJint(env,thiz,"indxMcBoard");

	MACRO_SETUP_CNTX(token);//update bundle information

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

	jstrcpy(env,jstrTopology,txtBuff);

	status = McSetParamInt(
		MC_BOARD + indxMcBoard,
		MC_BoardTopology,
		MC_BoardTopology_MONO_DECA
	);//use "MC_BOARD" to set topology,it must be called after creating channel
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

	jstrcpy(env,jstrConnect,txtBuff);
	status = McSetParamStr(
		token->channel,
		MC_Connector,
		txtBuff
	);//set configuration.2
	if(status!=MC_OK){
		cout<<"fail to set connector"<<endl;
		return;
	}

	jstrcpy(env,jstrConnect,txtBuff);
	status = McSetParamStr(
		token->channel,
		MC_CamFile,
		txtBuff
	);//set configuration.3
	if(status!=MC_OK){
		cout<<"fail to set camfile"<<endl;
		return;
	}

	//set all parameters!!!
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

	MACRO_SETUP_MATX((token->matx))//update bundle information

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
		printf("buff=0x%p\n",(void*)buff);
		cout<<endl;
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
	int i = MC_SurfaceCount;
	MCSTATUS status = 0;

	McToken* token = ((McToken*)cntx);
	token->signal = false;//reset it~~~

	status = McSetParamInt(
		token->channel,
		MC_ChannelState,
		MC_ChannelState_ACTIVE
	);
	if(status==MC_OK){
		while((token->signal==false)){
			//wait amd dump old data???
		}
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

	McDelete(token->channel);

	McCloseDriver();

	delete (token);

	MACRO_CLOSE_END
}
