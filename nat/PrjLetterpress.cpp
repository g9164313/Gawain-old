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
 * 6: minimum score for Cross-T.<p>
 * 7: minimum score for Rectangle.<p>
 */
#define PARAM_SIZE 8
static int param[PARAM_SIZE] = {120,300,50,5,5,7,70,70};//this is mapping from java-code

static float minScore[]={0.03,0.03};

static int debugMode = 0;

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implInitParam(
	JNIEnv* env,
	jobject thiz
){
	jclass clzz = env->GetObjectClass(thiz);
	jfieldID id;
	jobject obj;
	id = env->GetFieldID(clzz,"param","[I");
	obj = env->GetObjectField(thiz,id);
	jintArray arr1=*(reinterpret_cast<jintArray*>(&obj));
	env->GetIntArrayRegion(arr1,0,PARAM_SIZE,param);
	minScore[0] = (float)((100-param[6]))/100.f;
	minScore[1] = (float)((100-param[7]))/100.f;

	debugMode = env->GetIntField(
		thiz,
		env->GetFieldID(clzz,"debugMode","I")
	);
	/*id = env->GetFieldID(clzz,"score","[F");
	obj = env->GetObjectField(thiz,id);
	jfloatArray arr2=*(reinterpret_cast<jfloatArray*>(&obj));
	env->GetFloatArrayRegion(arr2,0,2,minScore);*/
}

static Point findTarget(
	Mat& ova,
	vector<Point>& shape,
	vector<vector<Point> >& cts,
	bool checkConvex,
	int numVertex,
	float silimarity,
	float* score
){
	Point loca(-1,-1);
	if(debugMode==2){
		drawContour(ova,cts);
		return loca;
	}
	int cnt = 0;
	double score_sum = 0.;
	double score_min = 1.;
	for(int i=0; i<cts.size(); i++){
		vector<Point> approx;
		Mat points(cts[i]);
		approxPolyDP(points, approx, param[5], true);
		//cout<<"vtx="<<approx.size()<<",min="<<numVertex<<endl;
		if(approx.size()<numVertex){
			continue;
		}
		//if(isContourConvex(approx)==checkConvex){
		//	continue;
		//}
		double score_shp = matchShapes(
			shape,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		//cout<<"score="<<score_shp<<endl;
		if(score_shp<silimarity){
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
			if(debugMode==3){
				drawPolyline(ova,approx);
			}
		}
		if(score_shp<score_min){
			score_min = score_shp;//at least, we have minimum~~~
		}
	}
	if(cnt!=0){
		loca.x = loca.x / cnt;
		loca.y = loca.y / cnt;
	}
	if(score!=NULL){
		float val =0.;
		if(cnt==0){
			val = score_min;
		}else{
			val = score_sum / cnt;
		}
		(*score) = (1.f - val) * 100.f;
	}
	return loca;
}


extern "C" JNIEXPORT jfloat JNICALL Java_prj_letterpress_WidAoiViews_implFindCros(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
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
	if(debugMode==1){
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
	float score;
	Point res = findTarget(
		ova,
		shapeCross,
		cts,false,4,minScore[0],
		&score
	);
	loca[0] = res.x;
	loca[1] = res.y;
	env->ReleaseIntArrayElements(jloca,loca,0);
	if(debugMode==0){
		drawCrossT(ova,res,30,Scalar(0,255,255),3);
	}
	MACRO_SET_IMG_INFO(ova);
	return score;
}

extern "C" JNIEXPORT jfloat JNICALL Java_prj_letterpress_WidAoiViews_implFindRect(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
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
	Point cros(mask[0],mask[1]);
	circle(
		nod2,
		cros,25,
		Scalar::all(0),-1
	);//erase something~~~
	env->ReleaseIntArrayElements(jmask,mask,0);
	if(debugMode==1){
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
	float score;
	Point res = findTarget(
		ova,
		shapeRect,
		cts,true,4,minScore[1],
		&score
	);
	loca[0] = res.x;
	loca[1] = res.y;
	env->ReleaseIntArrayElements(jloca,loca,0);
	if(debugMode==0){
		drawCrossT(ova,cros,30,Scalar(0,255,255),3);
		drawRectangle(ova,res,30,30,Scalar(255,0,0),3);
	}
	MACRO_SET_IMG_INFO(ova);
	return score;
}

