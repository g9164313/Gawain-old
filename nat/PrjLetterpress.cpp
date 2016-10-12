/*
 * FilterLetterpress.cpp
 *
 *  Created on: 2016年10月3日
 *      Author: qq
 */
#include <vision.hpp>
#include <CamBundle.hpp>

static vector<Point> shapeRect,shapeCross;

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implInitShape(
	JNIEnv* env,
	jobject thiz
){
	const int len = 100;
	shapeRect.clear();
	shapeRect.push_back(Point(0  ,0  ));
	shapeRect.push_back(Point(len,0  ));
	shapeRect.push_back(Point(len,len));
	shapeRect.push_back(Point(0  ,len));

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

static int param[4] = {2000,5000,7,3};

void getParam(
	JNIEnv* env,
	jobject thiz
){
	jfieldID id = env->GetFieldID(
		env->GetObjectClass(thiz),
		"param","[I"
	);
	jobject obj = env->GetObjectField(thiz,id);
	jintArray arr=*(reinterpret_cast<jintArray*>(&obj));
	env->GetIntArrayRegion(arr,0,4,param);
}

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implFindTarget(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint step
){
	MACRO_PREPARE
	if(buff==NULL){
		return;
	}
	Mat src(height,width,type,buff);
	Mat img = checkMono(src);
	Mat ova = Mat::zeros(img.size(),CV_8UC4);

	getParam(env,thiz);

	Mat nod1,nod2;
	Canny(img,nod1,param[0],param[1],param[2],true);

	Mat kern = getStructuringElement(
		MORPH_ELLIPSE,
		Size(param[3],param[3]),
		Point(-1,-1)
	);
	dilate(nod1,nod2,kern);
	if(step==1){
		drawEdge(ova,nod2);
		MACRO_SET_IMG_INFO(ova);
		return;
	}

	vector<vector<Point> > cts;
	findContours(nod2,cts,RETR_LIST,CHAIN_APPROX_SIMPLE);
	if(step==2){
		cout<<"findContours="<<cts.size()<<endl;
		drawContour(ova,cts,Scalar(0,255,0));
		MACRO_SET_IMG_INFO(ova);
		return;
	}
	if(cts.size()==0){
		cerr<<"fail to find contours!!"<<endl;
		return;
	}

	vector<Point> locaRect,locaCross;
	for(int i=0; i<cts.size(); i++){
		vector<Point> approx;

		Mat points(cts[i]);
		approxPolyDP(points, approx, 7, true);

		if(approx.size()<4){
			continue;
		}
		//test minimum enclose square~~
		double scoreRect = matchShapes(
			shapeRect,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		double scoreCross = matchShapes(
			shapeCross,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		RotatedRect rect = minAreaRect(approx);
		if(scoreRect<scoreCross){
			if(scoreRect>0.7){
				continue;
			}
			locaRect.push_back(rect.center);
		}else{
			if(scoreCross>0.7){
				continue;
			}
			locaCross.push_back(rect.center);
		}
	}
	if(locaRect.size()>0){
		Point vtx = average(locaRect);
		if(step==3){
			Rect rr(
				vtx.x-50,vtx.y-50,
				100,100
			);
			drawRectangle(ova,rr,Scalar(0,255,255,255));
		}
	}
	if(locaCross.size()>0){
		Point vtx = average(locaCross);
		if(step==3){
			drawCrossT(ova,vtx,Scalar(255,0,0,255),3,30);
		}
	}
	if(step==3){
		MACRO_SET_IMG_INFO(ova);
	}
}

