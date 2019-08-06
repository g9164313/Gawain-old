/*
 * stubber.cpp
 *
 *  Created on: 2019年7月11日
 *      Author: qq
 */
#include <global.hpp>
#include <vision.hpp>

extern Scalar cc[];
extern void train_mask(Mat& pool, Mat& mask, double nuVal);
extern Mat extract_mask(Mat& pool, Ptr<SVM>& ptr);

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub1(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera
){
	STUBBER_PREPARE(objCamera);
	STUBBER_LOOP_HEAD;
	STUBBER_EMBED_HEAD;
	Mat msk1;
	getMask(env, objFilm, &msk1, NULL, NULL);
	//imwrite("pool.png",pool);
	//imwrite("msk1.png",msk1);
	if(pool.empty()==true || msk1.empty()==true){
		cout<<"Data is empty!!!"<<endl;
		STUBBER_EMBED_DONE;
		return;
	}
	jfloat valNu = env->GetFloatField(
		thiz,
		env->GetFieldID(
			env->GetObjectClass(thiz),
			"valNu", "F"
		)
	);
	cout<<"got nu="<<valNu<<endl;
	train_mask(pool, msk1, (double)valNu);
	STUBBER_EMBED_TAIL;
	STUBBER_LOOP_TAIL(false);
	cout<<"train done"<<endl;
}

void copy_to_overlay(Mat& dat, Mat& over){
	Mat bgra[4];
	split(over,bgra);
	dat.copyTo(bgra[1]);
	dat.copyTo(bgra[3]);
	merge(bgra, 4, over);
}

void draw_path(
	const Point& mrk,
	const Point& wap,
	const vector<Point>& path,
	Mat& over
){
	//cout<<"path="<<path.size()<<endl;
	for(int i=0; i<path.size(); i++){
		circle(over, path[i], 3, cc[1]);
	}
	circle(over, mrk, 3, cc[0]);
	circle(over, wap, 3, cc[0]);
	arrowedLine(over, mrk, wap, cc[2], 3);
}

extern vector<Point> findPath(
	const Mat& mask,
	const Point& mark,
	double* dist = NULL
);

extern Point findWayPoint(
	const Point& mark,
	const double nearDist,
	vector<Point>& path
);

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub2(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera,
	jint showState
){
	STUBBER_PREPARE(objCamera);

	Point mrk = getPoint(env, objFilm, 1);
	Ptr<SVM> svm = SVM::load("mask.xml");
	//cout<<"mark="<<mrk<<endl;
	jmethodID mid_prober = env->GetMethodID(
		env->GetObjectClass(thiz),
		"moveProber","(II)I"
	);

	int cntWalk = 0;

	STUBBER_LOOP_HEAD;
	Mat msk;
	STUBBER_EMBED_HEAD;
	over = over * 0;//clear overlay data
	//get road information from origin image
	msk = extract_mask(pool, svm);
	if(showState==1){
		imwrite("img.png",pool);
		copy_to_overlay(msk,over);
		cout<<"show mask"<<endl;
		STUBBER_EMBED_DONE;
		return;
	}
	STUBBER_EMBED_TAIL;

	//get contour information from mask
	double dist;
	vector<Point> path = findPath(msk, mrk, &dist);
	Point wpt = findWayPoint(mrk, dist, path);

	STUBBER_EMBED_HEAD;
	draw_path(mrk, wpt, path, over);
	if(showState==2){
		STUBBER_EMBED_DONE;
		return;
	}
	STUBBER_EMBED_TAIL;

	Point vec = wpt - mrk;
	cntWalk = env->CallIntMethod(
		thiz, mid_prober,
		vec.x, vec.y
	);

	STUBBER_LOOP_TAIL(0<cntWalk);

	cout<<"walk done"<<endl;
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub3(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera
){

	STUBBER_PREPARE(objCamera);
	Point mrk = getPoint(env, objFilm, 1);
	STUBBER_LOOP_HEAD;
	STUBBER_EMBED_HEAD;
	circle(pool, Point(100,100), 50+rand()%50, Scalar(0,50,0),3);
	STUBBER_EMBED_TAIL;
	STUBBER_LOOP_TAIL(true);
}

