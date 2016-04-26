/*
 * cam_ebus.cpp
 *
 *  Created on: 2016年4月15日
 *      Author: qq
 */
#include <global.hpp>
#include <grabber.hpp>
#include <CamBundle.hpp>

#include <PvTypes.h>
#include <PvSystem.h>
#include <PvInterface.h>
#include <PvDevice.h>
#include <PvStream.h>
#include <PvPipeline.h>
#include <PvBuffer.h>
#include <PvBufferConverter.h>
#include <PvDeviceFinderWnd.h>
#include <PvConfigurationWriter.h>
#include <PvConfigurationReader.h>

class EBusBundle {
public:
	PvSystem sys;
	PvDevice dev;
    PvStream stm;
    PvDeviceInfo* inf;
    PvPipeline* pip;
    PvGenParameterArray* parm;

    int imgType,imgWidth,imgHeight;

	EBusBundle(CamBundle* cb,int idx):cam(cb),parm(NULL),imgBuff(NULL){
		inf = selectByIndex(idx);
		if(inf==NULL){
			return;
		}
		pip = new PvPipeline(&stm);
		initDevice();
		/*PvDeviceFinderWnd wnd;
		if(wnd.ShowModal().IsOK()==false){
			return;
		}
		inf = wnd.GetSelected();*///don't let window pop from stack!!!!!
	}
	~EBusBundle(){
		if(parm!=NULL){
			parm->ExecuteCommand("AcquisitionStop");
		}
		if(imgBuff!=NULL){
			pip->ReleaseBuffer(imgBuff);
			pip->Stop();
			delete pip;
		}
		stm.Close();
		dev.Disconnect();
	}
	void fetchImage();
private:
    PvBuffer* imgBuff;
	CamBundle* cam;
	PvDeviceInfo* selectByIndex(int devIndex);
	void initDevice();
};

PvDeviceInfo* EBusBundle::selectByIndex(int devIndex){
	sys.SetDetectionTimeout(1000);
	cam->updateMsgLast("searching...");
	if(sys.Find().IsOK()==false){
		return NULL;
	}
	//check configuration~~~
	size_t indx=0;
	PvInterface  *ethn = NULL;
	PvDeviceInfo *info = NULL;
	for(size_t i=0; i<sys.GetInterfaceCount(); i++){
		ethn = sys.GetInterface(i);
		char txtIP[60];
		strcpy(txtIP,ethn->GetIPAddress().GetAscii());
		cout<<"HOST IP:"<<txtIP<<endl;
		int numIP[4];
		sscanf(txtIP,"%d.%d.%d.%d",numIP,numIP+1,numIP+2,numIP+3);
		numIP[3]++;
		for(size_t j=0; j<ethn->GetDeviceCount(); j++){
			indx++;
			if(indx<devIndex){
				continue;
			}
			indx = j;//keep this index for the next configure turn...
			info = ethn->GetDeviceInfo(j);
			if (info->IsIPConfigurationValid()==true){
				return info;//we find a valid device~~~
			}
			sprintf(txtIP,"%d.%d.%d.%d",numIP[0],numIP[1],numIP[2],numIP[3]);
			numIP[3]++;
			PvDevice::SetIPConfiguration(
				info->GetMACAddress(),
				PvString(txtIP),
				ethn->GetSubnetMask()
			);
			cout<<"configure "<<info->GetModel().GetAscii()<<" to "<<txtIP<<endl;
			break;//we need to configure again!!!!
		}
	}
	if(sys.Find().IsOK()==true){
		return ethn->GetDeviceInfo(indx);
	}
	return NULL;
}

void EBusBundle::initDevice(){
	PvResult res = dev.Connect(inf);
	if(res.IsOK()==false){
		cout<<"PvDeviceInfo isn't correct."<<endl;
		return;
	}
	parm = dev.GetGenParameters();
	dev.NegotiatePacketSize();
	stm.Open(inf->GetIPAddress());
	dev.SetStreamDestination(
		stm.GetLocalIPAddress(),
		stm.GetLocalPort()
	);
	//inf->GetIPAddress().GetAscii();

	PvInt64 lSize = 0;
	parm->GetIntegerValue("PayloadSize",lSize);
	// Set the Buffer size and the Buffer count
	pip->SetBufferSize(static_cast<PvUInt32>(lSize));
	pip->SetBufferCount(10);
	//IMPORTANT: the pipeline needs to be "armed", or started before
	//we instruct the device to send us images
	pip->Start();

	//load configure~~~
	/*if(bnd.cfgName[0]!=0){
		PvConfigurationReader reader;
		reader.Load(PvString(bnd.cfgName));
		reader.Restore(0,dev);
	}*/

    //TLParamsLocked is optional but when present, it MUST be set to 1
    //before sending the AcquisitionStart command
	parm->SetIntegerValue("TLParamsLocked",1);
	parm->ExecuteCommand("GevTimestampControlReset");

	//The pipeline is already "armed", we just have to tell the device
	//to start sending us images
	//parm->SetEnumValue("AcquisitionMode","SingleFrame");//some issue~~~
	//parm->SetEnumValue("AcquisitionMode","MultiFrame");//occupy all buffer
	parm->SetEnumValue("AcquisitionMode","Continuous");//some issue~~~
	parm->ExecuteCommand("AcquisitionStart");

	//prepare some information
	PvInt64 typ,ww,hh;
	parm->GetIntegerValue("Height",hh);
	parm->GetIntegerValue("Width",ww);
	parm->GetEnumValue("PixelFormat",typ);
	switch(typ){
	case 0x1080001: imgType=CV_8UC1; break;
	case 0x1100003: imgType=CV_16UC1; break;//Mono10
	case 0x10c0004: imgType=CV_16UC1; break;//Mono10Packed
	case 0x1100005: imgType=CV_16UC1; break;//Mono12
	case 0x10c0006: imgType=CV_16UC1; break;//Mono12Packed
	default:
		imgType=CV_8UC1;
		cout<<"unknown type - "<<typ<<endl;
		return;
	}
	imgWidth = ww;
	imgHeight= hh;
}

void EBusBundle::fetchImage(){
	PvBuffer* buff;
	PvResult res = pip->RetrieveNextBuffer(&buff,100);
	if(res.IsOK()==false){
		//cout<<"Timeout!!!"<<endl;
		//cam->updateMsgLast("Timeout!!!");
		return;
	}
	if(buff->GetPayloadType()==PvPayloadTypeImage){
		PvImage* img = buff->GetImage();
		if(imgBuff!=NULL){
			pip->ReleaseBuffer(imgBuff);//release previous image
		}
		imgBuff = buff;//keep this for next turn...
		cam->updateSource(
			imgHeight,
			imgWidth,
			imgType,
			img->GetDataPointer()
		);
		//cout<<"Get Image@"<<img->GetWidth()<<"x"<<img->GetHeight()<<endl;
		//parm->ExecuteCommand("AcquisitionStart");
	}else{
		pip->ReleaseBuffer(buff);
	}
}
//------------------------------------------//

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamEBus_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id,
	jstring jconfigName
){
	CamBundle* cam = initContext(env,bundle);//it is necessary

	EBusBundle* bnd = new EBusBundle(cam,id);//it may be a 'long' procedure
	if((bnd->inf)!=NULL){
		cam->ctxt = bnd;//assign resource~~~
		cam->updateEnableState(true,"open camera via EBus");
		cam->updateInfo(
			bnd->imgType,
			bnd->imgWidth,
			bnd->imgHeight
		);
	}else{
		delete bnd;
		finalContext(env,bundle,cam,"fail to open!!!");
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamEBus_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint indx
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_FETCH_CHECK
	EBusBundle& bnd = *((EBusBundle*)(cam->ctxt));
	bnd.fetchImage();
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamEBus_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_CHECK
	if(cam->ctxt!=NULL){
		delete (EBusBundle*)(cam->ctxt);//release resource~~~
	}
	finalContext(env,bundle,cam,"close camera via EBus");
}



