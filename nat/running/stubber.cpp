/*
 * stubber.cpp
 *
 *  Created on: 2019年7月11日
 *      Author: qq
 */
#include <global.hpp>
#include <vision.hpp>

extern Scalar cc[];
extern Mat train_valid_data(Mat& pool, Mat& mask, double nuVal);
extern Mat extract_mask(Mat& pool, Ptr<SVM>& ptr);

static void copy_to_overlay(Mat& dat, Mat& over){
	Mat bgra[4];
	split(over,bgra);
	dat.copyTo(bgra[1]);
	dat.copyTo(bgra[3]);
	merge(bgra, 4, over);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub1(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera
){
	STUBBER_PREPARE(objCamera);
	STUBBER_LOOP_HEAD;
	STUBBER_EMBED_HEAD;
	Mat bckg;//background-mask
	getMask(env, objFilm, &bckg, NULL, NULL);
	if(pool.empty()==true || bckg.empty()==true){
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
	Mat msk = train_valid_data(pool, bckg, (double)valNu);
	copy_to_overlay(msk, over);
	STUBBER_EMBED_TAIL;
	STUBBER_LOOP_TAIL(false);
	cout<<"train done"<<endl;
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
	if(mrk==wap){
		circle(over, mrk, 3, cc[0]);
	}else{
		circle(over, mrk, 3, cc[0]);
		circle(over, wap, 3, cc[0]);
		arrowedLine(over, mrk, wap, cc[2], 3);
	}
}

extern vector<Point> findPath(
	const Mat& mask,
	const Point& mark
);
extern Point findWaypoint(
	const Point& mark,
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

	Mat msk;
	STUBBER_LOOP_HEAD;
		STUBBER_EMBED_HEAD;
		over = over * 0;//clear overlay data
		//get road information from origin image
		msk = extract_mask(pool, svm);
		if(showState==1){
			copy_to_overlay(msk,over);
			//imwrite("valid-1.png",pool);
			//imwrite("valid-2.png",msk);
			cout<<"test data"<<endl;
			STUBBER_EMBED_DONE;
			return;
		}
		STUBBER_EMBED_TAIL;

	//get contour information from mask
	vector<Point> path;
	Point wapt;

		STUBBER_EMBED_HEAD;
		path = findPath(msk, mrk);
		wapt = findWaypoint(mrk, path);
		draw_path(mrk, wapt, path, over);
		if(showState==2){
			STUBBER_EMBED_DONE;
			return;
		}
		STUBBER_EMBED_TAIL;

	Point vec = wapt - mrk;
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
	Ptr<SVM> svm = SVM::load("mask.xml");

	STUBBER_PREPARE(objCamera);

	Point mrk = getPoint(env, objFilm, 1);
	jmethodID mid_move = env->GetMethodID(
		env->GetObjectClass(thiz),
		"moveProberAlongPath","([I)V"
	);

	Mat msk;
	vector<Point> path;
	Point wapt;
	STUBBER_EMBED_HEAD;
	over = over * 0;//clear overlay data
	msk = extract_mask(pool, svm);
	double dist;
	path = findPath(msk, mrk);
	wapt = findWaypoint(mrk, path);
	draw_path(mrk, wapt, path, over);
	STUBBER_EMBED_TAIL;

	jsize cnt = path.size()*2;
	jintArray obj_arg = env->NewIntArray(cnt);
	jint* buf = new jint[cnt];
	for(int i=0; i<path.size(); i++){
		Point& pp = path[i];
		//cout<<"P"<<i<<")"<<pp<<endl;
		buf[i*2+0] = pp.x;
		buf[i*2+1] = pp.y;
	}
	env->SetIntArrayRegion(obj_arg, 0, cnt, buf);
	env->CallVoidMethod(thiz, mid_move, obj_arg);
	env->DeleteLocalRef(obj_arg);
	delete buf;

	cout<<"travel done"<<endl;
}

