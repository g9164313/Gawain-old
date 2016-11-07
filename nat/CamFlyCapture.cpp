/*
 * CamFlycapture.cpp
 *
 *  Created on: 2016年8月12日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>
#include "FlyCapture2.h"
#include "FlyCapture2GUI.h"

using namespace FlyCapture2;

static Camera* getCameraBySerial(
	JNIEnv* env,
	BusManager& bus,
	unsigned int serial,int total
){
	FlyCapture2::Error error;
	PGRGuid guid;
	for(int i=0; i<total; i++){
		error = bus.GetCameraFromIndex(i,&guid);
		Camera t_cam;
		error = t_cam.Connect(&guid);
		if(error!=PGRERROR_OK){
			continue;
		}
		CameraInfo inf;
		error = t_cam.GetCameraInfo(&inf);
		if(error==PGRERROR_OK){
			if(inf.serialNumber==serial){
				cout<<"match serial:"<<serial<<endl;
				t_cam.Disconnect();
				Camera* cam = new Camera();
				cam->Connect(&guid);
				return cam;
			}
		}
		t_cam.Disconnect();
	}
	return NULL;
}

static Camera* getCameraByIndex(
	JNIEnv* env,
	BusManager& bus,
	unsigned int index,int total
){
	FlyCapture2::Error error;
	PGRGuid guid;
	error = bus.GetCameraFromIndex(index,&guid);
	if(error!=PGRERROR_OK){
		loge(env,"fail to get camera");
		return NULL;
	}
	Camera* cam = new Camera();
	error = cam->Connect(&guid);
	if(error!=PGRERROR_OK){
		delete cam;
		loge(env,"fail to connect camera");
		return NULL;
	}
	return cam;
}

static int checkType(JNIEnv* env,Camera* cam){
	//Do we have better methods ???
	FlyCapture2::Error error;
	CameraInfo inf;
	error = cam->GetCameraInfo(&inf);
	if(error==PGRERROR_OK){
		if(inf.isColorCamera==true){
			return CV_8UC3;
		}
	}else{
		logw(env,"fail to get information");
	}
	return CV_8UC1;
}

static void checkFormat7(
	JNIEnv* env,
	jobject thiz,
	Camera* cam
){
	FlyCapture2::Error error;
	const Mode k_fmt7Mode = MODE_0;
	bool valid;
	Format7ImageSettings fmt7setting;
	Format7PacketInfo fmt7packet;
	jintArray jfmt7int;
	jint* fmt7int = intArray2Ptr(
		env,
		env->GetObjectClass(thiz),
		thiz,
		"fmt7setting",jfmt7int
	);
	if(fmt7int[0]<0){
		goto EXIT_CHECK_FMT;
	}

	fmt7setting.mode = k_fmt7Mode;
	fmt7setting.offsetX= fmt7int[1];
	fmt7setting.offsetY= fmt7int[2];
	fmt7setting.width  = fmt7int[3];
	fmt7setting.height = fmt7int[4];
	fmt7setting.pixelFormat = UNSPECIFIED_PIXEL_FORMAT;

	error = cam->ValidateFormat7Settings(
		&fmt7setting,
		&valid,
		&fmt7packet
	);
	if(error!=PGRERROR_OK){
		cout<<"fmt7setting: "<<
			"mode="<<k_fmt7Mode<<"，"<<
			"("<<fmt7int[1]<<"，"<<fmt7int[2]<<
			")-"<<fmt7int[3]<<"x"<<fmt7int[4]<<endl;
		loge(env,"fail to valid fmt7setting");
		goto EXIT_CHECK_FMT;
	}
	if(valid==false){
		fmt7int[1] = fmt7int[2] = fmt7int[3] = fmt7int[4] = -1;//we fail!!!
		loge(env,"invalid fmt7setting");
		goto EXIT_CHECK_FMT;
	}
	error = cam->SetFormat7Configuration(
		&fmt7setting,
		fmt7packet.recommendedBytesPerPacket
	);
	if(error!=PGRERROR_OK){
		loge(env,"fail to configure fmt7setting");
		goto EXIT_CHECK_FMT;
	}
	logv(env,
		"format change to (%d,%d)-%dx%d",
		fmt7int[1],fmt7int[2],fmt7int[3],fmt7int[4]
	);
EXIT_CHECK_FMT:
	env->ReleaseIntArrayElements(jfmt7int,fmt7int,0);
}

static void checkProperty(
	JNIEnv* env,
	jobject thiz,
	Camera* cam
){
	FlyCapture2::Error error;
	Property prop;

	prop.type = SHUTTER;
	error = cam->GetProperty(&prop);
	if(error==PGRERROR_OK){
		cout<<"shutter="<<prop.absValue<<"("<<prop.absControl<<")"<<endl;
	}

	prop.type = GAIN;
	error = cam->GetProperty(&prop);
	if(error==PGRERROR_OK){
		cout<<"GAIN="<<prop.absValue<<"("<<prop.absControl<<")"<<endl;
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implSetup(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint index,
	jboolean isSerial
){
	FlyCapture2::Error error;
	BusManager bus;
	unsigned int total;
	unsigned int type;
	MACRO_SETUP_BEG

	error = bus.GetNumOfCameras(&total);
	if(error!=PGRERROR_OK){
		loge(env,"fail to get number of camera");
		goto EXIT_SETUP;
	}
	if(total==0){
		goto EXIT_SETUP;
	}

	Camera* cam;
	if(isSerial==JNI_TRUE){
		cam = getCameraBySerial(env,bus,index,total);
	}else{
		cam = getCameraByIndex(env,bus,index,total);
	}
	if(cam==NULL){
		goto EXIT_SETUP;
	}

	type = checkType(env,cam);
	checkFormat7(env,thiz,cam);
	cam->StartCapture();
	checkProperty(env,thiz,cam);

EXIT_SETUP:
	MACRO_SETUP_END2(cam,type)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implFetch(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG
	if(cntx==NULL){
		return;
	}
	Camera* cam = (Camera*)cntx;
	Image img;
	FlyCapture2::Error err = cam->RetrieveBuffer(&img);
	if(err!=PGRERROR_OK){
		loge(env,"fail to fetch image");
		return;
	}
	Mat src(
		img.GetRows(),
		img.GetCols(),
		type,
		img.GetData(),
		img.GetReceivedDataSize()/img.GetRows()
	);
	//scale???
	//Mat dst;
	//resize(src,dst,Size(img.GetCols()/2,img.GetRows()/2));
	MACRO_FETCH_COPY(src)
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implClose(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_CLOSE_BEG
	if(cntx!=NULL){
		Camera* cam = (Camera*)cntx;
		cam->StopCapture();
		cam->Disconnect();
		delete cam;
	}
	CameraControlDlg* dlg = (CameraControlDlg*)(getJlong(env,thiz,"ptrDlgCtrl"));
	if(dlg!=NULL){
		delete dlg;
		setJlong(env,thiz,"ptrDlgCtrl",0);
	}
	MACRO_CLOSE_END
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamFlyCapture_implShowCtrl(
	JNIEnv* env,
	jobject thiz,
	jobject bundle
){
	MACRO_FETCH_BEG
	if(cntx==NULL){
		return;
	}
	Camera* cam = (Camera*)cntx;
	CameraControlDlg* dlg = (CameraControlDlg*)(getJlong(env,thiz,"ptrDlgCtrl"));
	if(dlg==NULL){
		dlg = new CameraControlDlg();
		dlg->Connect(cam);
		dlg->SetTitle(">_<");
		setJlong(env,thiz,"ptrDlgCtrl",(jlong)dlg);
	}
	if(dlg->IsVisible()==false){
		dlg->ShowModal();
	}
}


