#include <global.hpp>
#include <pylon/PylonIncludes.h>
#include "vision/CamBundle.hpp"

#if defined ( USE_GIGE )
// Settings for Basler GigE Vision cameras
#include <pylon/gige/BaslerGigEInstantCamera.h>
typedef Pylon::CBaslerGigEInstantCamera Camera_t;
using namespace Basler_GigECameraParams;
using namespace Basler_GigEStreamParams;
#elif defined ( USE_USB )
// Settings to use Basler USB cameras.
#include <pylon/usb/BaslerUsbInstantCamera.h>
typedef Pylon::CBaslerUsbInstantCamera Camera_t;
using namespace Basler_UsbCameraParams;
#else
#error camera type is not specified. For example, define USE_GIGE for using GigE cameras
#endif

using namespace Pylon;
using namespace GenApi;

class PylonBundle:public CConfigurationEventHandler {
public:
	int imgType,imgWidth,imgHeight;
	char confName[100];
	CamBundle* bnd;
	Camera_t dev;

	PylonBundle(CamBundle* boundle,IPylonDevice* device):
		imgType(CV_8UC1),bnd(boundle),imgWidth(0),imgHeight(0)
	{
		confName[0] = 0;
		dev.Attach(device);
	}

	~PylonBundle(){
		dev.StopGrabbing();
	}

	void steup(){
		dev.RegisterConfiguration(
			this,
			RegistrationMode_Append,
			Cleanup_None
		);
		dev.Open();
		dev.StartGrabbing(
			GrabStrategy_LatestImages,
			GrabLoop_ProvidedByUser
		);
	}

	virtual void OnOpened(CInstantCamera& cam){
		//camera.MaxNumBuffer = 5;
		//cam.GetDeviceInfo().GetModelName()
		INodeMap& node = cam.GetNodeMap();
		checkType(node);
		loadParam(cam,node);
	}

private:
	void loadParam(CInstantCamera& dev,INodeMap& node){
		if(confName[0]==0){
			return;
		}
		cout<<"load configure file:"<<confName<<endl;
		try{
			CFeaturePersistence::Load(confName,&node,true);
		}catch (GenICam::GenericException &e){
			bnd->updateMsgLast(e.GetDescription());
		}
	}

	void checkType(INodeMap& node){
		CEnumerationPtr pxf(node.GetNode("PixelFormat"));
		if(IsAvailable(pxf->GetEntryByName("Mono8"))){
			imgType = CV_8UC1;
		}else if(
			IsAvailable(pxf->GetEntryByName("Mono10")) ||
			IsAvailable(pxf->GetEntryByName("Mono12"))
		){
			imgType = CV_16UC1;
		}else if(
			IsAvailable(pxf->GetEntryByName("BayerGR8")) ||
			IsAvailable(pxf->GetEntryByName("BayerRG8")) ||
			IsAvailable(pxf->GetEntryByName("BayerGB8")) ||
			IsAvailable(pxf->GetEntryByName("BayerBG8"))
		){
			imgType = CV_8UC3;
		}else{
			bnd->updateMsgLast("unknown type:"+pxf->ToString());
		}

		CIntegerPtr ww(node.GetNode("Width"));
		imgWidth = ww->GetValue();

		CIntegerPtr hh(node.GetNode("Height"));
		imgHeight= hh->GetValue();
	}
};


extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamPylon_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint id,
	jstring jconfigName
){
	CamBundle* cam = initContext(env,bundle);//it is necessary~~~

	PylonInitialize();
	try {
		CTlFactory& trans = CTlFactory::GetInstance();

		DeviceInfoList_t lstDev;
		int cnt = trans.EnumerateDevices(lstDev);
		if(cnt==0 || id>=cnt){
			char txt[100];
			sprintf(txt,"devices=%d@%d",cnt,id);
			cam->updateMsgLast(txt);
			return;
		}
		cout<<"find device="<<cnt<<endl;

		IPylonDevice* dev = trans.CreateDevice(lstDev[id]);
		PylonBundle* bnd = new PylonBundle(cam,dev);
		jstrcpy(env,jconfigName,bnd->confName);
		bnd->steup();

		//dev->

		cam->ctxt = bnd;//assign resource~~~
		cam->updateEnableState(true,"open camera via Pylon");
		cam->updateInfo(
			bnd->imgType,
			bnd->imgWidth,
			bnd->imgHeight
		);
	} catch (GenICam::GenericException &e) {
		cam->updateEnableState(false,e.GetDescription());
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamPylon_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint indx
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_FETCH_CHECK
	PylonBundle& bnd = *((PylonBundle*)(cam->ctxt));

	CGrabResultPtr res;
	try{
		bnd.dev.RetrieveResult(500,res,TimeoutHandling_Return);
		if(res->GrabSucceeded()==false){
			cam->updateMsgLast("fail to fetch image");
			return;
		}
		uint32_t width =res->GetWidth();
		uint32_t height=res->GetHeight();
		uint8_t* buffer=(uint8_t*)res->GetBuffer();
	}catch(GenICam::GenericException& e){
		//camera has removed!!!!!
		cout<<"[ERROR]:"<<e.GetDescription()<<endl;
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamPylon_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_CHECK
	cam->updateMsgLast("close camera via Pylon");
	if(cam->ctxt!=NULL){
		delete (PylonBundle*)(cam->ctxt);//release resource~~~
	}
	PylonTerminate();
	delete cam;
}

extern "C" JNIEXPORT jlong JNICALL Java_narl_itrc_vision_CamPylon_getExposure(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jlongArray jarrInf
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_GATHER_CHECK_RES0
	PylonBundle& bnd = *((PylonBundle*)(cam->ctxt));

	IInteger& val = bnd.dev.ExposureTimeRaw;
	jlong _v = val.GetValue();
	if(jarrInf==NULL){
		return _v;
	}

	jlong* jinf = env->GetLongArrayElements(jarrInf,NULL);
	jinf[0] = _v;
	jinf[1] = val.GetMin();
	jinf[2] = val.GetMax();
	jinf[3] = val.GetInc();
	env->ReleaseLongArrayElements(jarrInf,jinf,0);
	return _v;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamPylon_setExposure(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jlong val
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_FETCH_CHECK
	PylonBundle& bnd = *((PylonBundle*)(cam->ctxt));

	bnd.dev.ExposureTimeRaw.SetValue(val);
}

extern "C" JNIEXPORT jlong JNICALL Java_narl_itrc_vision_CamPylon_getGain(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jlongArray jarrInf
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_GATHER_CHECK_RES0
	PylonBundle& bnd = *((PylonBundle*)(cam->ctxt));

	IInteger& val = bnd.dev.GainRaw;
	jlong _v = val.GetValue();
	if(jarrInf==NULL){
		return _v;
	}
	jlong* jinf = env->GetLongArrayElements(jarrInf,NULL);
	jinf[0] = _v;
	jinf[1] = val.GetMin();
	jinf[2] = val.GetMax();
	jinf[3] = val.GetInc();
	env->ReleaseLongArrayElements(jarrInf,jinf,0);
	return _v;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_CamPylon_setGain(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jlong val
){
	CamBundle* cam = getContext(env,bundle);
	MACRO_FETCH_CHECK
	PylonBundle& bnd = *((PylonBundle*)(cam->ctxt));

	bnd.dev.GainRaw.SetValue(val);
}




