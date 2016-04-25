#include <global.hpp>
#include <grabber.hpp>
#include <CamBase.hpp>

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevGrabber_looperVidcap(
	JNIEnv* env,
	jobject thiz,
	jint id
){
	GBundle bnd(env,thiz);

	VideoCapture vid(id);
	if(vid.isOpened()==false){
		bnd.setLastMsg("fail to open!!");
		return;
	}

	//vid.set(CV_CAP_PROP_FRAME_WIDTH ,1280);
	//vid.set(CV_CAP_PROP_FRAME_HEIGHT,720);
	vid.set(CV_CAP_PROP_FRAME_WIDTH ,640);
	vid.set(CV_CAP_PROP_FRAME_HEIGHT,480);
	int ww = (int)vid.get(CV_CAP_PROP_FRAME_WIDTH);
	int hh = (int)vid.get(CV_CAP_PROP_FRAME_HEIGHT);

	Mat img(hh,ww,CV_8UC3);
	do{
		//char k = (char)cvWaitKey(1);
		//if(k==27){
		//	break;//user press ESC key~~~
		//}
		//cvWaitKey() will launch GTK+ event
		vid >> img;
		if(img.empty()==true){
			bnd.setLastMsg("it is empty!!");
			break;
		}
		bnd.callback(img);
	}while(!bnd.checkExit());

	vid.release();
}
//----------------//





