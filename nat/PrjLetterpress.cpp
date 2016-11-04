/*
 * FilterLetterpress.cpp
 *
 *  Created on: 2016年10月3日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>

static Mat grndImage[2], grndAccum[2];

static vector<Point> shapeCross;

static Mat templateRect;

void init_shape(){
	const int wh=35,dp=20;
	shapeCross.clear();
	shapeCross.push_back(Point(wh      , 0       ));
	shapeCross.push_back(Point(wh+dp   , 0       ));
	shapeCross.push_back(Point(wh+dp   , wh      ));
	shapeCross.push_back(Point(wh+dp+wh, wh      ));
	shapeCross.push_back(Point(wh+dp+wh, wh+dp   ));
	shapeCross.push_back(Point(wh+dp   , wh+dp   ));
	shapeCross.push_back(Point(wh+dp   , wh+dp+wh));
	shapeCross.push_back(Point(wh      , wh+dp+wh));
	shapeCross.push_back(Point(wh      , wh+dp   ));
	shapeCross.push_back(Point(0       , wh+dp   ));
	shapeCross.push_back(Point(0       , wh      ));
	shapeCross.push_back(Point(wh      , wh      ));
}

void init_template(){

	const Scalar clr = Scalar::all(255);

	const int innWidth = 140;
	const int innBoard = 4;

	templateRect = Mat::zeros(
		innWidth+innBoard,
		innWidth+innBoard,
		CV_8UC1
	);
	int cc = templateRect.cols/2;
	Rect inn(
		cc - innWidth/2,
		cc - innWidth/2,
		innWidth, innWidth
	);
	rectangle(templateRect,inn,clr,innBoard);
	//imwrite("cc1.png",tempRect);
}

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implInitEnviroment(
	JNIEnv* env,
	jobject thiz,
	jstring jname0,
	jstring jname1
){
	init_shape();

	init_template();

	char name[500];
	if(jname0!=NULL){
		jstrcpy(env,jname0,name);
		grndImage[0] = imread(name,IMREAD_GRAYSCALE);
	}
	if(jname1!=NULL){
		jstrcpy(env,jname1,name);
		grndImage[1] = imread(name,IMREAD_GRAYSCALE);
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implTrainGrnd(
	JNIEnv* env,
	jobject thiz,
	jobject bundle0,
	jobject bundle1
){
	Mat src[2];
	src[0] = prepare_data(env,bundle0);
	src[1] = prepare_data(env,bundle1);
	//static int idx=0;
	for(int i=0; i<2; i++){
		Mat _src;
		src[i].convertTo(_src,CV_32FC1);
		if(grndAccum[i].empty()==true){
			_src.copyTo(grndAccum[i]);
		}else{
			grndAccum[i] = grndAccum[i] + _src;
		}
		//char name[100];
		//sprintf(name,"test%02d-%d.png",idx,i+1);
		//imwrite(name,src[i]);
	}
	//idx++;
}

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implTrainDone(
	JNIEnv* env,
	jobject thiz,
	jstring jname0,
	jstring jname1,
	int count
){
	char name[2][500];
	jstrcpy(env,jname0,name[0]);
	jstrcpy(env,jname1,name[1]);
	for(int i=0; i<2; i++){
		grndAccum[i] = grndAccum[i] / count;
		imwrite(name[i],grndAccum[i]);
		grndAccum[i].release();
	}
}

static float findShape(
	Mat& ova,
	vector<Point>& shape,
	vector<vector<Point> >& cts,
	int* param,
	Point& loca
){
	int cnt = 0;
	double score_sum = 0.;
	for(int i=0; i<cts.size(); i++){
		vector<Point> approx;
		Mat points(cts[i]);
		approxPolyDP(points, approx, param[3], true);
		//cout<<"vtx="<<approx.size()<<",min="<<numVertex<<endl;
		int vtx = approx.size();
		if(vtx<4){
			continue;
		}
		double area = contourArea(approx);
		if(area<(30*30)){
			continue;//noise???
		}
		RotatedRect rect = minAreaRect(approx);
		//if(checkRect==true){
		//	double bound = rect.size.width * rect.size.height;
		//	double ratio = min(area,bound)/max(area,bound);
		//	if(ratio<0.8){
		//		continue;
		//	}
		//}
		//if(isContourConvex(approx)==checkConvex){
		//	continue;
		//}
		double score_shp = matchShapes(
			shape,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		if(param[0]==2){
			drawPolyline(ova,approx,true);
		}
		//cout<<"score="<<score_shp<<endl;
		if(score_shp>0.1){
			continue;
		}
		//we found a similar target~~~
		if(cnt==0){
			loca = rect.center;
		}else{
			loca.x = loca.x + rect.center.x;
			loca.y = loca.y + rect.center.y;
		}
		cnt++;
		if(param[0]==3){
			drawPolyline(ova,approx,true);
		}
	}
	float score;
	if(cnt!=0){
		score = score_sum / cnt;
		score = (1.f-score)*100.f;
		loca.x = loca.x / cnt;
		loca.y = loca.y / cnt;
	}else{
		score = 0.;
		loca.x = -1;
		loca.y = -1;
	}
	return score;
}


extern "C" JNIEXPORT jfloat JNICALL Java_prj_letterpress_WidAoiViews_implFindCross(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint idx,
	jintArray jparam,
	jintArray jloca
){
	MACRO_PREPARE
	if(cntx==NULL){
		return -1.;
	}

	jint param[10];
	env->GetIntArrayRegion(jparam,0,17,param);

	cout<<param[0]<<".Param[1~4]={"
		<<param[1]<<", "<<param[2]<<", "<<param[3]<<", "<<param[4]<<", "
	<<"}"<<endl;

	Mat src(height,width,type,buff);
	Mat img = checkMono(src);
	Mat ova = Mat::zeros(src.size(),CV_8UC4);

	Mat nod1;
	threshold(img,nod1,param[1],255,THRESH_BINARY);
	Mat kern = getStructuringElement(
		MORPH_RECT,
		Size(param[2],param[2]),
		Point(-1,-1)
	);
	erode(nod1,nod1,kern);
	if(param[0]==1){
		drawEdgeMap(ova,nod1);//for edge mapping~~
		MACRO_SET_IMG_INFO(ova);
		return -1.;
	}

	vector<vector<Point> > cts;
	findContours(
		nod1,cts,
		RETR_LIST,CHAIN_APPROX_SIMPLE
	);
	if(cts.size()==0){
		return -2.;
	}

	jint* _loca = env->GetIntArrayElements(jloca,NULL);
	float score;
	Point loca;
	score = findShape(ova,shapeCross,cts,param,loca);
	_loca[0] = loca.x;
	_loca[1] = loca.y;
	env->ReleaseIntArrayElements(jloca,_loca,0);

	if(param[0]==0){
		drawCrossT(ova,loca,30,Scalar(0,255,255),3);
	}
	MACRO_SET_IMG_INFO(ova);
	return score;
}

extern "C" JNIEXPORT jfloat JNICALL Java_prj_letterpress_WidAoiViews_implFindRect(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint idx,
	jintArray jparam,
	jintArray jloca
){
	MACRO_PREPARE
	if(cntx==NULL){
		return -1.;
	}

	jint param[10];
	env->GetIntArrayRegion(jparam,0,17,param);

	cout<<param[0]<<".Param[9~12]={"
		<<param[9]<<", "<<param[10]<<", "<<param[11]<<", "<<param[12]<<", "
	<<"}"<<endl;

	Mat src(height,width,type,buff);
	Mat img = checkMono(src);
	Mat ova = Mat::zeros(src.size(),CV_8UC4);

	img = img - grndImage[idx];
	equalizeHist(img,img);
	threshold(img,img,param[9],255,THRESH_BINARY);
	Mat kern = getStructuringElement(
		MORPH_ELLIPSE,
		Size(param[10],param[10]),
		Point(-1,-1)
	);
	erode(img,img,kern);
	dilate(img,img,kern);
	//imwrite("cc2.png",image);

	Mat ex_img;
	copyMakeBorder(
		img,ex_img,
		templateRect.rows/2,templateRect.rows/2,
		templateRect.cols/2,templateRect.cols/2,
		BORDER_CONSTANT
	);
	//imwrite("cc3.png",ex_img);

	Mat result(
		ex_img.cols-templateRect.cols+1,
		ex_img.rows-templateRect.rows+1,
		CV_32FC1
	);
	matchTemplate(
		ex_img,templateRect,
		result,
		CV_TM_CCORR_NORMED
	);
	jint* _loca = env->GetIntArrayElements(jloca,NULL);
	double score;
	Point loca;
	minMaxLoc(
		result,
		NULL,&score,
		NULL,&loca
	);
	_loca[0] = loca.x;
	_loca[1] = loca.y;
	env->ReleaseIntArrayElements(jloca,_loca,0);

	drawRectangle(ova,loca,30,30,Scalar(255,0,0),3);
	MACRO_SET_IMG_INFO(ova);
	return score * 100.f;
}

