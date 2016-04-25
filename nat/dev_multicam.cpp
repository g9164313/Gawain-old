#include <global.hpp>
#include <grabber.hpp>
#include <multicam.h>

void eventHandle(PMCCALLBACKINFO info){

	GBundle& bnd = *((GBundle*)info->Context);
	uint8_t* ptr;

	switch(info->Signal){
	case MC_SIG_SURFACE_PROCESSING:
		McGetParamPtr(
			(MCHANDLE)info->SignalInfo,
			MC_SurfaceAddr,
			(PVOID*)&ptr
		);
		bnd.callback(ptr,640,480,CV_8UC1);
		break;

	case MC_SIG_ACQUISITION_FAILURE:
		break;

	default:
		fprintf(stderr,"Signal not handled:%d",info->Signal);
		break;
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevGrabber_looperMulticam(
	JNIEnv* env,
	jobject thiz,
	jint id
){
	GBundle bnd(env,thiz);

	MCHANDLE hChannel = 0;
	MCSTATUS status;

	status = McOpenDriver(NULL);
	if(status!=MC_OK){
		bnd.setLastMsg("fail to open driver!!");
		return;
	}

	status = McSetParamStr(MC_CONFIGURATION,MC_ErrorLog,"multicam-error.log");
	if(status!=MC_OK){
		bnd.setLastMsg("fail to set error-log");
	}

	// Set the board topology to support 2 cameras (only with a Grablink Expert 2)
	// status = McSetParamInt(MC_BOARD + 0, MC_BoardTopology, MC_BoardTopology_DUO);

	// Set the board topology to support 10 taps mode (only with a Grablink Full)
	// status = McSetParamInt(MC_BOARD + 0, MC_BoardTopology, MC_BoardTopology_MONO_DECA);

	status = McCreate(MC_CHANNEL,&hChannel);
	if(status!=MC_OK){
		bnd.setLastMsg("fail to create channel");
		McCloseDriver();
		return;
	}

	// Link the channel to a board!!!
	status = McSetParamInt(hChannel,MC_DriverIndex,id);

	// For all GrabLink boards but Grablink Expert 2 and Dualbase
	//status = McSetParamStr(hChannel,MC_Connector,"M");

	// For Grablink Expert 2 and Dualbase
	// status = McSetParamStr(hChannel, MC_Connector, "A");
	// if (status != MC_OK) goto Finalize;

	// Choose the camera camfile
	if(env==NULL){
		//user must modify this camera file!!!
		status = McSetParamStr(hChannel,MC_CamFile,"BASLER/L101k_L2048SP");
	}else{
		status = McSetParamStr(hChannel,MC_CamFile,bnd.cfgName);
		cout<<"[board "<<id<<"] load camfile - "<<bnd.cfgName<<endl;
	}
	if(status!=MC_OK){
		McDelete(hChannel);
		McCloseDriver();
		bnd.setLastMsg("fail to load cam-file");
		return;
	}

	status = McRegisterCallback(hChannel,eventHandle,&bnd);
	if(status!=MC_OK){
		McDelete(hChannel);
		McCloseDriver();
		bnd.setLastMsg("fail to register callback");
		return;
	}

	//Mat img(480,640,CV_8UC3);
	do{
		//wait for callback~~~~
		//bnd.callback(img);
	}while(!bnd.checkExit());

	McDelete(hChannel);
	McCloseDriver();
}
