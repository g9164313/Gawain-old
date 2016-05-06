#include <global.hpp>
#include <CamBundle.hpp>

void unsharpen(Mat& src) {
	Mat msk(src.size(), src.type());
	src.copyTo(msk);
	double scale;
	minMaxLoc(src, NULL, &scale);
	scale = scale * 0.2;
	GaussianBlur(src, src, Size(), 4.3, 4.3);
	msk = msk * (1 + scale) + src * (-scale);
	msk.copyTo(src);
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


extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_namedWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname
){
	char name[500];
	jstrcpy(env,jname,name);
	namedWindow(name,CV_WINDOW_AUTOSIZE);//TODO~~~~
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

extern "C" JNIEXPORT long JNICALL Java_narl_itrc_Misc_imRead(
	JNIEnv * env,
	jobject thiz,
	jstring jname
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat* img = new Mat();
	(*img) = imread(name);
	return (jlong)img;
}

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

