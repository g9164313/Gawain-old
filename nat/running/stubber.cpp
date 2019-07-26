/*
 * stubber.cpp
 *
 *  Created on: 2019年7月11日
 *      Author: qq
 */
#include <global.hpp>
#include <vision.hpp>

extern Scalar cc[];
extern void train_mask(Mat& pool, Mat& mask);
extern Mat extract_mask(Mat& pool, Ptr<SVM>& ptr, bool showMask);
extern Point extract_path(Mat& msk, Point& mrk, vector<Point>& path);

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub1(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera
){
	STUBBER_PREPARE(objCamera);
	STUBBER_DO;

	Mat msk1;
	getMask(env, objFilm, &msk1, NULL, NULL);
	//imwrite("pool.png",pool);
	//imwrite("msk1.png",msk1);
	if(pool.empty()==true || msk1.empty()==true){
		cout<<"Data is empty!!!"<<endl;
		STUBBER_RELEASE;
		return;
	}
	train_mask(pool,msk1);
	STUBBER_WHILE(false);
	cout<<"train done"<<endl;
}

void copy_to_overlay(Mat& dat, Mat& over){
	Mat bgra[4];
	split(over,bgra);
	dat.copyTo(bgra[1]);
	dat.copyTo(bgra[3]);
	merge(bgra, 4, over);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub2(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera,
	jboolean showMask
){
	STUBBER_PREPARE(objCamera);

	Point mrk = getPoint(env, objFilm, 1);
	Ptr<SVM> svm = SVM::load("mask.xml");
	//cout<<"mark="<<mrk<<endl;
	jmethodID mid_prober = env->GetMethodID(
		env->GetObjectClass(thiz),
		"moveProber","(II)V"
	);
	STUBBER_DO;
	//clear overlay data
	over = over * 0;
	//get road information from origin image
	Mat msk = extract_mask(
		pool,svm,
		(showMask==JNI_TRUE)?(true):(false)
	);
	if(showMask==JNI_TRUE){
		imwrite("img.png",pool);
		imwrite("msk.png",msk);
		copy_to_overlay(msk,over);
		cout<<"show mask"<<endl;
		STUBBER_RELEASE;
		return;
	}
	//get contour information from mask
	vector<Point> pts;
	Point wp1 = extract_path(msk, mrk, pts);
	circle(over, wp1, 3, cc[0]);
	for(int i=0; i<pts.size(); i++){
		circle(over, pts[i], 3, cc[0]);
	}
	arrowedLine(over, mrk, wp1, cc[1], 3);

	//command platform to move....
	Point diff = wp1 - mrk;
	env->CallVoidMethod(
		thiz, mid_prober,
		diff.x, diff.y
	);

	STUBBER_WHILE(false);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub3(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera
){

	STUBBER_PREPARE(objCamera);
	Point mrk = getPoint(env, objFilm, 1);
	STUBBER_DO;


	STUBBER_WHILE(false);
}

