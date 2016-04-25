#include <global.hpp>
#include <grabber.hpp>
#include <CamBase.hpp>
#include <pylon/PylonIncludes.h>

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

class EventConf:public CConfigurationEventHandler {
public:
	int type;

	EventConf(GBundle* ptr):type(CV_8UC1){
		bnd = ptr;
	}

	virtual void OnOpened(CInstantCamera& cam){
		//camera.MaxNumBuffer = 5;
		//cam.GetDeviceInfo().GetModelName()

		INodeMap& node = cam.GetNodeMap();

		checkType(node);

		loadParam(cam,node);
	}
private:
	GBundle* bnd;

	void loadParam(CInstantCamera& cam,INodeMap& node){
		char path[500]={0};
		if(bnd->isMock()==false){
			bnd->getTxt("cfgName",path);
		}
		if(path[0]==0){
			return;
		}
		cout<<"["<<bnd->getName()<<"]load configure file:"<<path<<endl;
		try{
			CFeaturePersistence::Load(path,&node,true);
		}catch (GenICam::GenericException &e){
			cout<<"["<<bnd->getName()<<"]An exception occurred!"<<endl<<e.GetDescription()<<endl;
		}
	}

	void checkType(INodeMap& node){
		CEnumerationPtr pxf(node.GetNode("PixelFormat"));
		if(IsAvailable(pxf->GetEntryByName("Mono8"))){
			type = CV_8UC1;
		}else if(
			IsAvailable(pxf->GetEntryByName("Mono10")) ||
			IsAvailable(pxf->GetEntryByName("Mono12"))
		){
			type = CV_16UC1;
		}else if(
			IsAvailable(pxf->GetEntryByName("BayerGR8")) ||
			IsAvailable(pxf->GetEntryByName("BayerRG8")) ||
			IsAvailable(pxf->GetEntryByName("BayerGB8")) ||
			IsAvailable(pxf->GetEntryByName("BayerBG8"))
		){
			type = CV_8UC3;
		}else{
			cout<<"["<<bnd->getName()<<"]unknown type="<<pxf->ToString()<<endl;
		}
	}
};

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevGrabber_looperPylon(
	JNIEnv* env,
	jobject thiz,
	jint id
) {
	GBundle bnd(env, thiz);

	PylonAutoInitTerm autoInitTerm;//call PylonInitialize and PylonTerminate

	try {
		// Get the transport layer factory.
		CTlFactory& trans = CTlFactory::GetInstance();

        // Get all attached devices and exit application if no device is found.
		DeviceInfoList_t lstDev;
		if(trans.EnumerateDevices(lstDev)==0){
			bnd.setLastMsg("enumeration:0");
			return;
		}

		CInstantCameraArray cam(lstDev.size());
		if(id>=lstDev.size()){
			char txt[100];
			sprintf(txt,"devices=%ld",lstDev.size());
			bnd.setLastMsg(txt);
			return;
		}

		cam[id].Attach(trans.CreateDevice(lstDev[id]));
		//cout<< camera.GetDeviceInfo().GetModelName()<<endl;

		EventConf* cconf = new EventConf(&bnd);

		cam[id].RegisterConfiguration(cconf,RegistrationMode_Append,Cleanup_None);

		cam[id].Open();

		cam[id].StartGrabbing(GrabStrategy_OneByOne,GrabLoop_ProvidedByUser);

		CGrabResultPtr res;
		do{
			cam.RetrieveResult(3000,res,TimeoutHandling_ThrowException);
			//Image grabbed successfully?
			if(res->GrabSucceeded()==true){
				// Access the image data.
				bnd.callback(
					res->GetBuffer(),
					res->GetWidth(),
					res->GetHeight(),
					cconf->type
				);
			}else{
				char txt[500];
				sprintf(txt,
					"[%s]%d @ %s ",
					bnd.getName(),
					res->GetErrorCode(),
					res->GetErrorDescription().c_str()
				);
				bnd.setLastMsg(txt);
			}
		}while(cam.IsGrabbing()==true && !bnd.checkExit());

		delete cconf;

	} catch (GenICam::GenericException &e) {
		bnd.setLastMsg(e.GetDescription());
		return;
	}
}
//----------------//

