
#ifndef VISION
#define VISION
#endif
#include <global.hpp>
#include "vision/CamBundle.hpp"

void unsharpen(Mat& src,int rad,double scale) {
	rad = (rad%2==0)?(rad+1):(rad);
	Mat msk(src.size(), src.type());
	src.copyTo(msk);
	GaussianBlur(
		src,src,
		Size(rad,rad),
		0
	);
	msk = msk * (1+scale) + src * (1-scale);
	msk.copyTo(src);
}
//-----------------------------------//

Mat thresClamp(const Mat& src,int lower,int upper){
	Mat dst = Mat::zeros(src.size(),CV_8UC1);
	Mat upp = Mat::zeros(src.size(),CV_8UC1);
	Mat low = Mat::zeros(src.size(),CV_8UC1);
	threshold(src,low,lower,255.,THRESH_BINARY);
	threshold(src,upp,upper,255,THRESH_BINARY_INV);
	bitwise_and(low,upp,dst);
	return dst;
}
//-----------------------------------//

Mat filterVariance(
	const Mat& src,
	const int radius,
	double* min,
	double* max
){
	Size range(radius,radius);
	Mat node1;
	src.convertTo(node1, CV_32F);
	Mat mu;
	blur(node1, mu, range);
	Mat mu2;
	blur(node1.mul(node1), mu2, range);
	Mat sigma;
	cv::sqrt(mu2 - mu.mul(mu), sigma);
	Mat dst;
	normalize(sigma, dst, 0., 255., NORM_MINMAX, CV_8UC1);
	if(min!=NULL||max!=NULL){
		double _min,_max;
		minMaxLoc(sigma,&_min,&_max);
		if(min!=NULL){ *min = _min; }
		if(max!=NULL){ *max = _max; }
	}
	return dst;
}
//-----------------------------------//


int findMaxBlob(vector<vector<Point> >& blob){
	int idx=0;
	double max=0;
	for(size_t i=0; i<blob.size(); i++){
		double area =0;//contourArea(blob[i]);
		if(area>max){
			max = area;
			idx = i;
		}
	}
	return idx;
}

void drawBlob(const char* name,Mat& _img,vector<Point>& blob){
	Mat img(_img.size(),CV_8UC3);
	cvtColor(_img,img,COLOR_GRAY2BGR);
	vector<vector<Point> > tmp;
	tmp.push_back(blob);
	drawContours(img,tmp,0,Scalar(0,200,0));
	imwrite(name,img);
}
//-----------------------------------//

void toColor(Mat& src,Mat& dst){
	dst.release();
	int type = src.type();
	switch(type){
	case CV_8UC3:
		dst = src;
		break;
	case CV_8UC1:
		dst = Mat(src.size(),CV_8UC3);
		cvtColor(src,dst,COLOR_GRAY2BGR);
		break;
	default:
		cerr<<"how to become color("<<type<<") ??"<<endl;
		break;
	}
}
//-----------------------------------//

/*extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_namedWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname
){
	char name[500];
	jstrcpy(env,jname,name);
	namedWindow(name,CV_WINDOW_AUTOSIZE);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_renderWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jlong ptr
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat& img = *((Mat*)ptr);
	imshow(name,img);
	waitKey(25);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_destroyWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname
){
	if(jname==NULL){
		destroyAllWindows();
	}else{
		char name[500];
		jstrcpy(env,jname,name);
		destroyWindow(name);
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_imWrite(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jlong ptrMat
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat& img = *((Mat*)ptrMat);
	imwrite(name,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_imWriteRoi(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jlong ptrMat,
	jintArray arrRoi
){
	char name[500];
	jstrcpy(env,jname,name);

	jint* val = env->GetIntArrayElements(arrRoi,NULL);
	Rect zone(
		val[0],val[1],
		val[2],val[3]
	);
	Mat& img = *((Mat*)ptrMat);
	Mat roi = img(zone);
	imwrite(name,roi);
	env->ReleaseIntArrayElements(arrRoi,val,0);
}

extern "C" JNIEXPORT long JNICALL Java_narl_itrc_Misc_imRead(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	int flags
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat* img = new Mat();
	(*img) = imread(name,flags);
	return (jlong)img;
}*/

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_imRelease(
	JNIEnv * env,
	jobject thiz,
	jlong ptrMat
){
	Mat* img = (Mat*)ptrMat;
	img->release();
	delete img;
}

extern "C" JNIEXPORT jlong JNICALL Java_narl_itrc_Misc_imCreate(
	JNIEnv * env,
	jobject thiz,
	jint width,
	jint height,
	jint type
){
	Mat* img = new Mat();
	img->create(height,width,type);
	return (long)img;
}

Mat* obj2mat(JNIEnv *env, jobject obj) {
	jlong addr = 0L;
	jclass _clazz = env->GetObjectClass(obj);
	jfieldID id;
	id = env->GetFieldID(_clazz, "nativeObj", "J");
	addr = env->GetLongField(obj, id);
	return (Mat*)addr;
}
jobjectArray mat2array(JNIEnv *env, Mat& src) {
	jclass clazz = env->FindClass("[F");
	if (clazz == NULL) {
		return NULL;
	}
	jobjectArray dst = env->NewObjectArray(src.rows, clazz, NULL);
	float* ptr = (float*)src.data;
	for (int j = 0; j<src.rows; j++) {
		jfloatArray row = env->NewFloatArray(src.cols);
		env->SetFloatArrayRegion(row, 0, src.cols, ptr);
		env->SetObjectArrayElement(dst, j, row);
		ptr = ptr + src.cols;
	}
	return dst;
}

void mapRect(JNIEnv *env, jobject src, Rect& dst) {
	jclass _clazz = env->GetObjectClass(src);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "I");
	dst.x = env->GetIntField(src, id);
	id = env->GetFieldID(_clazz, "y", "I");
	dst.y = env->GetIntField(src, id);
	id = env->GetFieldID(_clazz, "width", "I");
	dst.width = env->GetIntField(src, id);
	id = env->GetFieldID(_clazz, "height", "I");
	dst.height = env->GetIntField(src, id);
}
void mapRect(JNIEnv *env, Rect& src, jobject dst) {
	jclass _clazz = env->GetObjectClass(dst);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "I");
	env->SetIntField(dst, id, src.x);
	id = env->GetFieldID(_clazz, "y", "I");
	env->SetIntField(dst, id, src.y);
	id = env->GetFieldID(_clazz, "width", "I");
	env->SetIntField(dst, id, src.width);
	id = env->GetFieldID(_clazz, "height", "I");
	env->SetIntField(dst, id, src.height);
}
Rect mapZone(JNIEnv *env,jobject src,const char* dstName) {
	jclass _clazz = env->GetObjectClass(src);
	jfieldID id;
	jobject obj;
	jintArray arr;
	jint* ptr;
	id =env->GetFieldID(_clazz,dstName,"[I");
	obj=env->GetObjectField(src,id);
	arr = *(reinterpret_cast<jintArray*>(&obj));
	Rect zone;
	ptr = env->GetIntArrayElements(arr,0);
	zone.x = ptr[0];
	zone.y = ptr[1];
	zone.width = ptr[2];
	zone.height= ptr[3];
	env->ReleaseIntArrayElements(arr,ptr,0);
	return zone;
}


void mapPoint(JNIEnv *env, jobject src, Point& dst) {
	jclass _clazz = env->GetObjectClass(src);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "D");
	dst.x = env->GetDoubleField(src, id);
	id = env->GetFieldID(_clazz, "y", "D");
	dst.y = env->GetDoubleField(src, id);
}
void mapPoint(JNIEnv *env, Point& src, jobject dst) {
	jclass _clazz = env->GetObjectClass(dst);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "D");
	env->SetDoubleField(dst, id, src.x);
	id = env->GetFieldID(_clazz, "y", "D");
	env->SetDoubleField(dst, id, src.y);
}

