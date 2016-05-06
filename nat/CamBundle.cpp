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
	pos = intArray2Ptr(env,clzz,thiz,"pinPos",jintArr);
	cnt = env->GetArrayLength(jintArr);
	for(jsize i=0; i<cnt; i++){
		pos[i] = -1;//all x,y is negative~~~
	}
	env->ReleaseIntArrayElements(jintArr,pos,0);

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
	Scalar(0x36,0x43,0xF4),
	Scalar(0x63,0x1E,0xE9),
	Scalar(0xB0,0x27,0x9C),
	Scalar(0xB7,0x3A,0x67),
	Scalar(0xB5,0x51,0x3F),
	Scalar(0xF3,0x96,0x21),
	Scalar(0xF4,0xA9,0x03),
	Scalar(0xD4,0xBC,0x00),
	Scalar(0x88,0x96,0x00),
	Scalar(0x50,0xAF,0x4C),
	Scalar(0x4A,0xC3,0x8B),
	Scalar(0x39,0xDC,0xCD),
	Scalar(0x3B,0xEB,0xFF),
	Scalar(0x07,0xC1,0xFF),
	Scalar(0x00,0x98,0xFF),
	Scalar(0x22,0x57,0xFF),
	Scalar(0x48,0x55,0x79),
	Scalar(0x9E,0x9E,0x9E),
	Scalar(0x8B,0x7D,0x60)
};

void drawMarkPin(Mat& img,Point& pp,Scalar& color){
	//It must be a color-image!!!
	const int CROSS_SPACE=4;
	Vec3b org = img.at<Vec3b>(pp);
	Point cc0 = pp + Point(-CROSS_SPACE,0);
	Point cc1 = pp + Point( CROSS_SPACE,0);
	Point cc2 = pp + Point(0, CROSS_SPACE);
	Point cc3 = pp + Point(0,-CROSS_SPACE);
	line(img,cc0,cc1,color);
	line(img,cc2,cc3,color);
	img.at<Vec3b>(pp) = org;
}

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

static void markPinData(
	Mat& src,
	Mat& ova,
	int idx,
	int* pinPos,float* pinVal
){
	Point cc;
	cc.x = pinPos[2*idx+0];
	cc.y = pinPos[2*idx+1];
	if(cc.x<0 || cc.y<0){
		return;
	}
	switch(src.type()){
	case CV_8UC1:{
		uint8_t pix = ova.at<uint8_t>(cc);
		pinVal[PIN_COLS*idx+0] = pix;
		pinVal[PIN_COLS*idx+1] = pix;
		pinVal[PIN_COLS*idx+2] = pix;
		pinVal[PIN_COLS*idx+3] = pix;
		}break;
	case CV_8UC3:{
		Vec3b pix = ova.at<Vec3b>(cc);
		pinVal[PIN_COLS*idx+0] = pix[0];
		pinVal[PIN_COLS*idx+1] = pix[1];
		pinVal[PIN_COLS*idx+2] = pix[2];
		pinVal[PIN_COLS*idx+3] = 0;
		}break;
	}
	drawMarkPin(ova,cc,markWheel[(idx*PR_SIZE)%19]);
}

static void markRoiData(
	Mat& src,
	Mat& ova,
	int idx,
	int* roiPos,float* roiVal
){
	int zoneType = roiPos[ROI_COLS*idx+0];
	if(zoneType==0){
		return;
	}

	Rect zone;
	zone.x = roiPos[ROI_COLS*idx+1];
	zone.y = roiPos[ROI_COLS*idx+2];
	zone.width = roiPos[ROI_COLS*idx+3];
	zone.height= roiPos[ROI_COLS*idx+4];

	Point zone_c(
		zone.x+zone.width/2,
		zone.y+zone.height/2
	);

	Mat roi = src(zone);
	Mat msk = Mat::zeros(roi.size(),CV_8UC1);

	int shape =(zoneType&0x00F);
	int stroke=(zoneType&0x0F0)>>4;
	int chann =(zoneType&0xF00)>>8;

	Scalar& clr = markWheel[(idx*PR_SIZE)%19];
	switch(shape){
	case 1:{//rectangle
		Rect mask(0,0,msk.cols,msk.rows);
		rectangle(msk,mask,Scalar::all(255),-1);
		rectangle(ova,zone,clr);
		if(stroke==0){
			drawMarkCross(ova,zone_c,clr,10);
		}else{
			mask.x+=stroke;
			mask.y+=stroke;
			mask.width -=(2*stroke);
			mask.height-=(2*stroke);
			rectangle(msk,mask,Scalar::all(0),-1);
		}
		}break;
	case 2:{//circle
		Point mask_c(msk.cols/2,msk.rows/2);
		int radius = std::min(msk.cols,msk.rows)/2;
		circle(msk,mask_c,radius,Scalar::all(255),-1);
		circle(ova,zone_c,radius,clr);
		if(stroke==0){
			drawMarkCross(ova,zone_c,clr,10);
		}else{
			radius -= stroke;
			circle(msk,mask_c,radius,Scalar::all(0),-1);
		}
		}break;
	}

	Scalar avg,dev;
	meanStdDev(roi,avg,dev,msk);
	switch(src.type()){
	case CV_8UC1:
		break;
	case CV_8UC3:
		break;
	}
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

	jintArray jintArr0,jintArr1;
	jfloatArray jfloatArr0,jfloatArr1;
	jint*   pinPos = intArray2Ptr(env,clzz,thiz,"pinPos",jintArr0);
	jfloat* pinVal = floatArray2Ptr(env,clzz,thiz,"pinVal",jfloatArr0);
	jint*   roiPos = intArray2Ptr(env,clzz,thiz,"roiPos",jintArr1);
	jfloat* roiVal = floatArray2Ptr(env,clzz,thiz,"roiVal",jfloatArr1);
	for(int i=0; i<PR_SIZE; i++){
		markPinData(src,ova,i,pinPos,pinVal);
		markRoiData(src,ova,i,roiPos,roiVal);
	}
	env->ReleaseIntArrayElements(jintArr0,pinPos,0);
	env->ReleaseFloatArrayElements(jfloatArr0,pinVal,0);
	env->ReleaseIntArrayElements(jintArr1,roiPos,0);
	env->ReleaseFloatArrayElements(jfloatArr1,roiVal,0);

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


