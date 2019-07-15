/*
 * CamFilter.cpp
 *  Put some 'little' wrapper
 *  Created on: 2016年7月18日
 *      Author: qq
 */
#include <global.hpp>
#include "opencv2/text.hpp"
#include <iomanip>

#include "../running/deprecated/vision.hpp"
using namespace cv::text;

static char fileNM1[200],fileNM2[200],fileGRP[200];//full-path file name

void filterNMText(Mat& src,vector<Rect>& boxes){

	// Extract channels to be processed individually
	vector<Mat> channels;
	computeNMChannels(src, channels);
	//int cn = (int) channels.size();
	//for (int c = 0; c < cn - 1; c++){
		//Append negative channels to detect ER-
		//'ER-' is bright regions over dark background
	//	Mat tmp = 255 - channels[c];
	//	channels.push_back(tmp);
	//}

	// Create ERFilter objects with the 1st and 2nd stage default classifiers
	Ptr<ERFilter> er_filter1 = createERFilterNM1(
		loadClassifierNM1(fileNM1),
		16,
		0.00015f,
		0.13f,
		0.2f,
		true,
		0.1f
	);
	Ptr<ERFilter> er_filter2 = createERFilterNM2(
		loadClassifierNM2(fileNM2),
		0.5
	);

	vector<vector<ERStat> > regions(channels.size());
	// Apply the default cascade classifier to each independent channel (could be done in parallel)
	for (int c = 0; c < (int) channels.size(); c++) {
		er_filter1->run(channels[c], regions[c]);
		er_filter2->run(channels[c], regions[c]);
	}

	// Detect character groups
	vector<vector<Vec2i> > group;
	/*erGrouping(
		src, channels,
		regions,
		group, boxes,
		ERGROUPING_ORIENTATION_HORIZ
	);*/
	erGrouping(
		src, channels,
		regions,
		group, boxes,
		ERGROUPING_ORIENTATION_ANY,
		fileGRP,
		0.5
	);
}

extern "C" JNIEXPORT jintArray JNICALL Java_prj_daemon_FilterNMText_implCookData(
	JNIEnv* env,
	jobject thiz,
	jlong ptrMatx
){
	jclass _clazz = env->GetObjectClass(thiz);
	jstring jname_nm1 = (jstring)env->GetObjectField(
		thiz,
		env->GetFieldID(_clazz,"fileNM1","Ljava/lang/String;")
	);
	jstring jname_nm2 = (jstring)env->GetObjectField(
		thiz,
		env->GetFieldID(_clazz,"fileNM2","Ljava/lang/String;")
	);
	jstring jname_grp = (jstring)env->GetObjectField(
		thiz,
		env->GetFieldID(_clazz,"fileGRP","Ljava/lang/String;")
	);
	//re-mapping path
	jstrcpy(env,jname_nm1,fileNM1);
	jstrcpy(env,jname_nm2,fileNM2);
	jstrcpy(env,jname_grp,fileGRP);
	//cout<<"fileNM1="<<fileNM1<<endl;
	//cout<<"fileNM2="<<fileNM2<<endl;
	//cout<<"fileGRP="<<fileGRP<<endl;

	Mat& img = *((Mat*)ptrMatx);
	vector<Rect> box;
	filterNMText(img,box);
	jint cnt = box.size();
	if(cnt==0){
		return NULL;
	}
	jintArray result =  env->NewIntArray(cnt*4);
	jint buff[cnt*4];
	for(int i=0; i<cnt; i++){
		buff[i*4+0] = box[i].x;
		buff[i*4+1] = box[i].y;
		buff[i*4+2] = box[i].width;
		buff[i*4+3] = box[i].height;
	}
	env->SetIntArrayRegion(result,0,cnt*4,buff);
	return result;
}
//--------------------------------------//



