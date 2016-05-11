/*
 * CamBundle.cpp
 *
 *  Created on: 2016年5月2日
 *      Author: qq
 */
#ifndef VISION
#define VISION
#endif
#include <global.hpp>
#include <CamBundle.hpp>

void setContext(JNIEnv* env,jobject bnd,void* ptr){
	jfieldID fid = env->GetFieldID(
		env->GetObjectClass(bnd),
		"ptrCntx",
		"J"
	);
	env->SetLongField(bnd,fid,(jlong)ptr);
}

void* getContext2(JNIEnv* env,jobject bnd){
	jfieldID fid = env->GetFieldID(
		env->GetObjectClass(bnd),
		"ptrCntx",
		"J"
	);
	jlong ptr = env->GetLongField(bnd,fid);
	return (void*)ptr;
}

void updateEnbl(JNIEnv* env,jobject bnd,bool flag){
	jclass clzz = env->GetObjectClass(bnd);
	jmethodID mid = env->GetMethodID(
		clzz,
		"updateOptEnbl",
		"(Z)V"
	);

}

void updateMesg(JNIEnv* env,jobject bnd,const char* txt){
	jclass clzz = env->GetObjectClass(bnd);
	jmethodID mid = env->GetMethodID(
		clzz,
		"updateMsgLast",
		"(Ljava/lang/String;)V"
	);
	env->CallVoidMethod(bnd,mid,env->NewStringUTF(txt));
}

void updateEnblMesg(JNIEnv* env,jobject bnd,bool flag,const char* txt){

	//env->CallVoidMethod(bnd,midOptEnbl,(flag)?(JNI_TRUE):(JNI_FALSE));
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_refreshInf(
	JNIEnv * env,
	jobject thiz,
	jobject bundle
){
	jclass clzz = env->GetObjectClass(bundle);
	jlongArray jlongAttr;
	jlong* matx = longArray2Ptr(env,clzz,bundle,"ptrMatx",jlongAttr);
	Mat* _src = (Mat*)(matx[0]);
	env->ReleaseLongArrayElements(jlongAttr,matx,0);
	if(_src==NULL){
		return;//WTF???
	}

	Mat& src = *_src;
	env->SetIntField(
		bundle,
		env->GetFieldID(clzz,"infoType","I"),
		src.type()
	);
	env->SetIntField(
		bundle,
		env->GetFieldID(clzz,"infoWidth","I"),
		src.size().width
	);
	env->SetIntField(
		bundle,
		env->GetFieldID(clzz,"infoHeight","I"),
		src.size().height
	);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_mapOverlay(
	JNIEnv * env,
	jobject thiz,
	jobject bundle
){
	jclass clzz = env->GetObjectClass(bundle);
	jlongArray jlongAttr;
	jlong* matx = longArray2Ptr(env,clzz,bundle,"ptrMatx",jlongAttr);
	Mat* _src = (Mat*)(matx[0]);
	Mat* _ova = (Mat*)(matx[1]);
	if(_src==NULL){
		env->ReleaseLongArrayElements(jlongAttr,matx,0);
		return;//WTF???
	}

	Mat& src = *_src;
	if(_ova==NULL){
		_ova = new Mat(src.size(),CV_8UC3);
	}else if(_ova->size()!=src.size()){
		//image is different, so we need to release old and create new one~~~
		_ova->release();
		delete _ova;
		_ova = new Mat(src.size(),CV_8UC3);
	}
	Mat& ova = *_ova;
	switch(src.type()){
	case CV_8UC1:
		cvtColor(src,ova,COLOR_GRAY2BGR);
		break;
	case CV_8UC3:
		src.copyTo(ova);
		break;
	}
	matx[1] = (jlong)_ova;//override it again!!!
	env->ReleaseLongArrayElements(jlongAttr,matx,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_releasePtr(
	JNIEnv * env,
	jobject thiz,
	jobject bundle
){
	//reset pointers~~~
	jclass clzz = env->GetObjectClass(bundle);
	jlongArray jlongAttr;
	jintArray jintArr;
	jsize cnt;
	jlong* ptr;
	ptr = longArray2Ptr(env,clzz,bundle,"ptrMatx",jlongAttr);
	cnt = env->GetArrayLength(jlongAttr);
	for(jsize i=0; i<cnt; i++){
		Mat* mm = (Mat*)(ptr[i]);
		if(mm!=NULL){
			mm->release();
			delete mm;
		}
		ptr[i] = 0;
	}
	env->ReleaseLongArrayElements(jlongAttr,ptr,0);

	//reset PIN and ROI position~~~
	jint* pos;
	pos = intArray2Ptr(env,clzz,thiz,"roiPos",jintArr);
	cnt = env->GetArrayLength(jintArr)/ROI_COLS;
	for(jsize i=0; i<cnt; i++){
		pos[i*ROI_COLS+0] = 0;//set ROI as none~~~
	}
	env->ReleaseIntArrayElements(jintArr,pos,0);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_narl_itrc_CamBundle_getData(
	JNIEnv* env,
	jobject thiz /*this object is already 'CamBundle'*/,
	jlong ptrMat
){
	if(ptrMat==0L){
		return NULL;
	}
	Mat& img =  *((Mat*)ptrMat);
	if(img.empty()==true){
		return NULL;
	}
	vector<uchar> buf;
	imencode(".png",img,buf);
	jbyteArray arrBuf = env->NewByteArray(buf.size());
	env->SetByteArrayRegion(arrBuf,0,buf.size(),(jbyte*)&buf[0]);
	return arrBuf;
}
//-------------------------------------//

static Scalar markStroke(0x05,0xF0,0xF0);

static Scalar markWheel[] = {
	Scalar(0x8B,0x7D,0x60),
	Scalar(0x9E,0x9E,0x9E),
	Scalar(0x48,0x55,0x79),
	Scalar(0x22,0x57,0xFF),
	Scalar(0x00,0x98,0xFF),
	Scalar(0x07,0xC1,0xFF),
	Scalar(0x3B,0xEB,0xFF),
	Scalar(0x39,0xDC,0xCD),
	Scalar(0x4A,0xC3,0x8B),
	Scalar(0x50,0xAF,0x4C),
	Scalar(0x88,0x96,0x00),
	Scalar(0xD4,0xBC,0x00),
	Scalar(0xF4,0xA9,0x03),
	Scalar(0xF3,0x96,0x21),
	Scalar(0xB5,0x51,0x3F),
	Scalar(0xB7,0x3A,0x67),
	Scalar(0xB0,0x27,0x9C),
	Scalar(0x36,0x43,0xF4),
	Scalar(0x63,0x1E,0xE9)
};

void drawMarkCross(Mat& img,Point& pp,Scalar& color,int space=4){
	//It must be a color-image!!!
	Vec3b org = img.at<Vec3b>(pp);
	Point cc0 = pp + Point(-space,-space);
	Point cc1 = pp + Point( space,-space);
	Point cc2 = pp + Point( space, space);
	Point cc3 = pp + Point(-space, space);
	line(img,cc0,cc2,color);
	line(img,cc1,cc3,color);
}

void drawMarkPin(Mat& img,Point& pp,Scalar& color,int space=4){
	//keep the origin, then overwrite it~~
	Vec3b org = img.at<Vec3b>(pp);
	drawMarkCross(img,pp,color,space);
	img.at<Vec3b>(pp) = org;
}

static void markPinValue(
	Mat& src,
	Point& pts,
	float* pinVal
){
	switch(src.type()){
	case CV_8UC1:{
		uint8_t pix = src.at<uint8_t>(pts);
		pinVal[0] = pix;
		pinVal[1] = pix;
		pinVal[2] = pix;
		pinVal[3] = pix;
		}break;
	case CV_8UC3:{
		Vec3b pix = src.at<Vec3b>(pts);
		pinVal[0] = pix[0];
		pinVal[1] = pix[1];
		pinVal[2] = pix[2];
		pinVal[3] = 0;
		}break;
	}
}

static void markRoiBoundary(
	Mat& src,
	Mat& ova,
	int idx,
	int* roiPos,float* roiVal
){
	Scalar& clr = markWheel[(idx*ROI_SIZE)%19];

	int off = (idx*ROI_COLS);
	int zoneType = roiPos[off+0];
	if(zoneType==0){
		return;
	}
	Rect zone(
		roiPos[off+1],
		roiPos[off+2],
		roiPos[off+3],
		roiPos[off+4]
	);
	Point zone_lt(
		roiPos[off+1],
		roiPos[off+2]
	);
	Point zone_cc(
		zone.x+zone.width/2,
		zone.y+zone.height/2
	);

	int shape =0+(zoneType&0x00000FF);
	int stroke=1+(zoneType&0x0FFFF00)>>8;
	int chann =0+(zoneType&0xF000000)>>24;

	switch(shape){
	case 1:{
		drawMarkPin(ova,zone_lt,clr,10);
		markPinValue(src,zone_lt,roiVal+off);
		}break;
	case 2:{//rectangle
		rectangle(ova,zone,clr,stroke+1);
		drawMarkCross(ova,zone_cc,clr,10);
		}break;
	case 3:{//circle
		int radius = std::min(zone.width,zone.height)/2;
		circle(ova,zone_cc,radius,clr);
		drawMarkCross(ova,zone_cc,clr,10);
		}break;
	}

	/*Scalar avg,dev;
	meanStdDev(roi,avg,dev,msk);
	switch(src.type()){
	case CV_8UC1:
		break;
	case CV_8UC3:
		break;
	}*/
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_markData(
	JNIEnv* env,
	jobject thiz /*this object is already 'CamBundle'*/
){
	jclass clzz = env->GetObjectClass(thiz);
	jlongArray jlongAttr;
	jlong* matx = longArray2Ptr(env,clzz,thiz,"ptrMatx",jlongAttr);
	Mat* _src = (Mat*)(matx[0]);
	Mat* _ova = (Mat*)(matx[1]);
	env->ReleaseLongArrayElements(jlongAttr,matx,0);
	if(_ova==NULL||_src==NULL){
		return;
	}

	Mat& ova = *_ova;
	Mat& src = *_src;

	jintArray jintArr0;
	jfloatArray jfloatArr0;
	jint*   roiPos = intArray2Ptr(env,clzz,thiz,"roiPos",jintArr0);
	jfloat* roiVal = floatArray2Ptr(env,clzz,thiz,"roiVal",jfloatArr0);
	for(int i=0; i<ROI_SIZE; i++){
		markRoiBoundary(src,ova,i,roiPos,roiVal);
	}
	env->ReleaseIntArrayElements(jintArr0,roiPos,0);
	env->ReleaseFloatArrayElements(jfloatArr0,roiVal,0);

	jint* roiTmp = intArray2Ptr(env,clzz,thiz,"roiTmp",jintArr0);
	if(roiTmp[0]>=0 && roiTmp[1]>=0){
		Point c0,c1;
		c0.x = roiTmp[0];
		c0.y = roiTmp[1];
		c1.x = roiTmp[2];
		c1.y = roiTmp[3];
		line(ova,c0,c1,markStroke);
		circle(ova,c1,4,markStroke);
	}
	env->ReleaseIntArrayElements(jintArr0,roiTmp,0);
}


