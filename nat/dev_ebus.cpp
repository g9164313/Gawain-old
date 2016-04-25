#include <global.hpp>
#include <grabber.hpp>

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

/*static void check_option(GrabBundle* bnd,PvGenParameterArray* dev,PvGenParameterArray* stm){
	if(bnd->env==NULL){
		return;
	}
	jbyteArray jopt;
	jbyte* opt = ArrayByte2Ptr(bnd->env,bnd->clzz,bnd->thiz,"optContext",jopt);
	if(opt[0]==0){
		bnd->env->ReleaseByteArrayElements(jopt,opt,0);
		return;
	}
	char* txt = strtok((char*)opt,";");
	while(txt!=NULL){		
		char typ = txt[0];
		char tkn[30]={0};
		char* val = strchr(txt,'=');
		strncpy(tkn,txt+1,val-txt-1);
		val++;
		PvGenType type;
		switch(typ){
		case '?':
			break;
		case '#':{ 
			dev->SetEnumValue(tkn,val); 
			}break;
		case '!':{ 
			dev->SetBooleanValue(tkn,true); 
			}break;
		case '$':{
			PvInt64 v = atoi(val);
			dev->SetIntegerValue(tkn,v);
			}break;
		case '%':{
			double v=0.1;
			dev->SetFloatValue(tkn,v);
			}break;
		case '@':{
			}break;
		}
		//cout<<"typ="<<typ<<", tkn="<<tkn<<", val="<<val<<endl;
		txt = strtok(NULL,";");
	}	
	opt[0] = 0;//reset it for next turn~~~
	bnd->env->ReleaseByteArrayElements(jopt,opt,0);
	return;
}*/

//parmStm->SetEnumValue("ExposureMode","Timed");
//parmStm->SetIntegerValue("ExposureTimeRaw",1200);

static void looper_inner(
	GBundle& bnd,
	PvDevice& dev,
	PvStream& stm,
	PvPipeline& pip
){
	//Get stream parameters/stats
	PvGenParameterArray *parmStm = stm.GetParameters();
	PvGenParameterArray *parmDev = dev.GetGenParameters();
	do{
		PvResult res,chk;
		PvBuffer* buf;		
		res = pip.RetrieveNextBuffer(&buf,50000,&chk);
		if(res.IsOK()==false){
			bnd.setLastMsg("fail to retrieve buffer");
			return;
		}
		if(chk.IsOK()==true){
			if(buf->GetPayloadType()==PvPayloadTypeImage){
				PvImage* img = buf->GetImage();
				bnd.callback(
					img->GetDataPointer(),
					img->GetWidth(),
					img->GetHeight(),
					CV_8UC1
				);
			}
			//double rate;
			//parmStm->GetFloatValue("AcquisitionRateAverage",rate);
			//bnd.updateFPS(rate);
		}
		pip.ReleaseBuffer(buf);
	}while(!bnd.checkExit());
}

static void looper_ebus(
	GBundle& bnd,
	PvDeviceInfo* info
){
	PvResult res;
	PvDevice dev;	
	res = dev.Connect(info);
	if(res.IsOK()==false){
		bnd.setLastMsg("fail to connect device");
		return;
	}
	cout<<"connect to ["<<info->GetMACAddress().GetAscii()<<"]"<<endl;

	//Get device parameters need to control streaming
    PvGenParameterArray *parmDev = dev.GetGenParameters();

	dev.NegotiatePacketSize();
	
	PvStream stm;
	stm.Open(info->GetIPAddress());
	// Have to set the Device IP destination to the Stream
    dev.SetStreamDestination(stm.GetLocalIPAddress(),stm.GetLocalPort());

	// Create the PvPipeline object
	PvPipeline pip(&stm);
    // Reading payload size from device
    PvInt64 lSize = 0;
	parmDev->GetIntegerValue("PayloadSize",lSize);
	// Set the Buffer size and the Buffer count
    pip.SetBufferSize(static_cast<PvUInt32>(lSize));
    pip.SetBufferCount(10);
    // IMPORTANT: the pipeline needs to be "armed", or started before 
    // we instruct the device to send us images
    pip.Start();

    //load configure~~~
    if(bnd.cfgName[0]!=0){
    	cout<<"load configure:"<<bnd.cfgName<<endl;
    	PvConfigurationReader rd;
    	rd.Load(PvString(bnd.cfgName));
    	rd.Restore(0,&dev);
    }

    // TLParamsLocked is optional but when present, it MUST be set to 1
    // before sending the AcquisitionStart command
	parmDev->SetIntegerValue("TLParamsLocked",1);
	parmDev->ExecuteCommand("GevTimestampControlReset");
	//parmDev->ExecuteCommand("TriggerSoftware");
	//parmDev->SetEnumValue("AcquisitionMode","SingleFrame");
	parmDev->SetEnumValue("AcquisitionMode","Continuous");
    // The pipeline is already "armed", we just have to tell the device
    // to start sending us images	
	res = parmDev->ExecuteCommand("AcquisitionStart");	
	if(res.IsOK()==true){
		looper_inner(bnd,dev,stm,pip);
		parmDev->ExecuteCommand("AcquisitionStop");
	}else{
		bnd.setLastMsg("fail to execute [AcquisitionStart]");
	}
	
	parmDev->SetIntegerValue("TLParamsLocked",0);
	pip.Stop();
	stm.Close();
	dev.Disconnect();
}

PvDeviceInfo* GetSelectedByIndex(PvSystem& sys,int id){

	sys.SetDetectionTimeout(2000);
	if(sys.Find().IsOK()==false){
		return NULL;
	}
	//check configuration~~~	
	for(size_t i=0; i<sys.GetInterfaceCount(); i++){
		PvInterface *eth = sys.GetInterface(i);
		char txtIP[60];
		strcpy(txtIP,eth->GetIPAddress().GetAscii());
		cout<<"eth-ip:"<<txtIP<<endl;
		int numIP[4];
		sscanf(txtIP,"%d.%d.%d.%d",numIP,numIP+1,numIP+2,numIP+3);
		numIP[3]++;
		for(size_t j=0; j<eth->GetDeviceCount(); j++){
			PvDeviceInfo* dev = eth->GetDeviceInfo(j);
			if (dev->IsIPConfigurationValid()==true){				
				continue;
			}
			sprintf(txtIP,"%d.%d.%d.%d",numIP[0],numIP[1],numIP[2],numIP[3]);
			numIP[3]++;
			PvDevice::SetIPConfiguration(
				dev->GetMACAddress(), 
				PvString(txtIP),
				eth->GetSubnetMask()
			);
			cout<<"config "<<dev->GetModel().GetAscii()<<" to "<<txtIP<<endl;
		}
	}
	//find again~~~
	cout<<"waiting..."<<endl;
	usleep(1000);
	if(sys.Find().IsOK()==false){
		return NULL;
	}
	int counter=0;
	for(size_t i=0; i<sys.GetInterfaceCount(); i++){
		PvInterface *eth = sys.GetInterface(i);
		for(size_t j=0; j<eth->GetDeviceCount(); j++){
			PvDeviceInfo* dev = eth->GetDeviceInfo(j);
			if(id==counter){
				return dev;
			}
			counter++;
		}
	}
	return NULL;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevGrabber_looperEBus(
	JNIEnv* env, 
	jobject thiz,
	jint id
){
	GBundle bnd(env,thiz);

	PvDeviceInfo* dev = NULL;
	if(id<=-1){
		//query user by GUI~~~
		PvDeviceFinderWnd wnd;
		if(wnd.ShowModal().IsOK()==false){
			bnd.setLastMsg("eBus modal fail!!!");
			return;
		}
		dev = wnd.GetSelected();//don't let window pop from stack!!!!!
		if(dev==NULL){
			bnd.setLastMsg("fail to select device");
			return;
		}
		looper_ebus(bnd,dev);
	}else{
		//select the camera by index~~~
		PvSystem sys;
		dev = GetSelectedByIndex(sys,id);
		if(dev==NULL){
			bnd.setLastMsg("fail to select device");
			return;
		}
		looper_ebus(bnd,dev);
	}	
	return;
}
