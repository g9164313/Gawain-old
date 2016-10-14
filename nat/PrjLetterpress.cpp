/*
 * FilterLetterpress.cpp
 *
 *  Created on: 2016年10月3日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>

static vector<Point> shapeCross,shapeRect;

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implInitShape(
	JNIEnv* env,
	jobject thiz
){
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

	const int len = 100;
	shapeRect.clear();
	shapeRect.push_back(Point(0  ,0  ));
	shapeRect.push_back(Point(len,0  ));
	shapeRect.push_back(Point(len,len));
	shapeRect.push_back(Point(0  ,len));
}

/**
 * Parameter for AOI. Their meanings are : <p>
 * 0: Binary Threshold.<p>
 * 1: Canny Threshold.<p>
 * 2: Canny Threshold, but only offset value.<p>
 * 3: Canny Aperture.<p>
 * 4: Dilate Size.<p>
 * 5: Approximates Epsilon.<p>
 */
#define PARAM_SIZE 6
static int param[PARAM_SIZE] = {10,30,10,3,1,7};//this is mapping from java-code

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implInitParam(
	JNIEnv* env,
	jobject thiz
){
	jfieldID id = env->GetFieldID(
		env->GetObjectClass(thiz),
		"param","[I"
	);
	jobject obj = env->GetObjectField(thiz,id);
	jintArray arr=*(reinterpret_cast<jintArray*>(&obj));
	env->GetIntArrayRegion(arr,0,PARAM_SIZE,param);
}

static Point findTarget(
	vector<Point>& shape,
	vector<vector<Point> >& cts,
	double* score
){
	int cnt = 0;
	Point loca(-1,-1);
	double score_sum = 0.;
	for(int i=0; i<cts.size(); i++){
		vector<Point> approx;
		Mat points(cts[i]);
		approxPolyDP(points, approx, param[5], true);
		if(approx.size()<6){
			continue;
		}
		if(isContourConvex(approx)==true){
			continue;
		}
		double score_shp = matchShapes(
			shapeCross,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		if(score_shp<0.07){
			//we found a similar target~~~
			RotatedRect rect = minAreaRect(approx);
			if(cnt==0){
				loca = rect.center;
			}else{
				loca.x += rect.center.x;
				loca.y += rect.center.y;
			}
			cnt++;
			score_sum = score_sum + score_shp;
		}
	}
	if(cnt!=0){
		loca.x = loca.x / cnt;
		loca.y = loca.y / cnt;
	}
	if(score!=NULL){
		if(cnt==0){
			*score = -1;
		}else{
			*score = score_sum / cnt;
		}
	}
	return loca;
}


extern "C" JNIEXPORT jdouble JNICALL Java_prj_letterpress_WidAoiViews_implFindCros(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint debug,
	jintArray jloca
){
	MACRO_PREPARE
	if(cntx==NULL){
		return -1.;
	}
	Mat src(height,width,type,buff);
	Mat img = checkMono(src);
	Mat ova = Mat::zeros(src.size(),CV_8UC4);

	Mat nod1;
	threshold(img,nod1,param[0],255,THRESH_BINARY);
	if(debug==1){
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

	jint* loca = env->GetIntArrayElements(jloca,NULL);
	double score;
	Point res = findTarget(shapeCross,cts,&score);
	loca[0] = res.x;
	loca[1] = res.y;
	env->ReleaseIntArrayElements(jloca,loca,0);
	switch(debug){
	case 3: drawContour(ova,cts); break;
	case 0: drawCrossT(ova,res,30,Scalar(255,0,0),3); break;
	}

	MACRO_SET_IMG_INFO(ova);
	return score;
}

extern "C" JNIEXPORT jdouble JNICALL Java_prj_letterpress_WidAoiViews_implFindRect(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jint debug,
	jintArray jmask,
	jintArray jloca
){
	MACRO_PREPARE
	if(cntx==NULL){
		return -1.;
	}
	Mat src(height,width,type,buff);
	Mat img = checkMono(src);
	Mat ova = Mat::zeros(src.size(),CV_8UC4);

	Mat nod1,nod2;
	Canny(
		img,nod1,
		param[1],
		param[1]+param[2],
		param[3],
		true
	);
	if(param[4]!=1){
		Mat kern = getStructuringElement(
			MORPH_ELLIPSE,
			Size(param[4],param[4]),
			Point(-1,-1)
		);
		dilate(nod1,nod2,kern);
	}else{
		nod2 = nod1;
	}
	jint* mask = env->GetIntArrayElements(jmask,NULL);
	circle(
		nod2,
		Point(mask[0],mask[1]),25,
		Scalar::all(0),-1
	);//erase something~~~
	env->ReleaseIntArrayElements(jmask,mask,0);
	if(debug==1){
		drawEdgeMap(ova,nod2);//for edge mapping~~
		MACRO_SET_IMG_INFO(ova);
		return -1.;
	}

	vector<vector<Point> > cts;
	findContours(
		nod2,cts,
		RETR_LIST,CHAIN_APPROX_SIMPLE
	);
	if(cts.size()==0){
		return -2.;
	}

	jint* loca = env->GetIntArrayElements(jloca,NULL);
	double score;
	Point res = findTarget(shapeRect,cts,&score);
	loca[0] = res.x;
	loca[1] = res.y;
	env->ReleaseIntArrayElements(jloca,loca,0);
	switch(debug){
	case 3: drawContour(ova,cts); break;
	case 0: drawRectangle(ova,res,30,30,Scalar(255,0,0),3); break;
	}

	MACRO_SET_IMG_INFO(ova);
	return -1.;
}

