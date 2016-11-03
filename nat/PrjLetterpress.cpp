/*
 * FilterLetterpress.cpp
 *
 *  Created on: 2016年10月3日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>

static vector<Point> shapeCross,shapeRect;

static Mat grndImage[2], grndAccum[2];

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implInitEnviroment(
	JNIEnv* env,
	jobject thiz,
	jstring jname0,
	jstring jname1
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
	//cout<<"score=("<<minScore[0]<<","<<minScore[1]<<"), debug="<<debugMode<<endl;
	/*id = env->GetFieldID(clzz,"score","[F");
	obj = env->GetObjectField(thiz,id);
	jfloatArray arr2=*(reinterpret_cast<jfloatArray*>(&obj));
	env->GetFloatArrayRegion(arr2,0,2,minScore);*/
}

static Point findTarget(
	Mat& ova,
	vector<Point>& shape,
	vector<vector<Point> >& cts,
	bool checkRect,
	int numVertex,
	float silimarity,
	float* score
){
	Point loca(-1,-1);
	int cnt = 0;
	double score_sum = 0.;
	double score_min = 1.;
	for(int i=0; i<cts.size(); i++){
		vector<Point> approx;
		Mat points(cts[i]);
		approxPolyDP(points, approx, param[5], true);
		//cout<<"vtx="<<approx.size()<<",min="<<numVertex<<endl;
		int vtx = approx.size();
		if(vtx<numVertex || (3*numVertex)<vtx){
			continue;
		}
		double area = contourArea(approx);
		if(area<(30*30)){
			continue;//noise???
		}
		RotatedRect rect = minAreaRect(approx);
		if(checkRect==true){
			double bound = rect.size.width * rect.size.height;
			double ratio = min(area,bound)/max(area,bound);
			if(ratio<0.8){
				continue;
			}
		}
		//if(isContourConvex(approx)==checkConvex){
		//	continue;
		//}
		double score_shp = matchShapes(
			shape,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		if(debugMode==2){
			drawPolyline(ova,approx,true);
		}
		//cout<<"score="<<score_shp<<endl;
		if(score_shp<silimarity){
			//we found a similar target~~~
			if(cnt==0){
				loca = rect.center;
			}else{
				loca.x += rect.center.x;
				loca.y += rect.center.y;
			}
			cnt++;
			score_sum = score_sum + score_shp;
			if(debugMode==3){
				drawPolyline(ova,approx,true);
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
	Mat kern = getStructuringElement(
		MORPH_RECT,
		Size(param[4],param[4]),
		Point(-1,-1)
	);
	erode(nod1,nod1,kern);
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
		cts,false,5,minScore[0],
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
		cros,50,
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

