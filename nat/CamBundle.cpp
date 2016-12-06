/*
 * CamBundle.cpp
 *
 *  Created on: 2016年5月2日
 *      Author: qq
 */
#include <global.hpp>
#include <CamBundle.hpp>
#ifndef VISION
#define VISION
#endif

Mat prepare_data(JNIEnv * env,jobject bundle){
	MACRO_PREPARE
	return Mat(height,width,type,buff);
}

/**
 * set a overlay picture to indicate information
 * @param bnd - just 'bundle'
 * @param ova - 4-channel image (PNG format)
 */
void set_img_array(
	JNIEnv * env,
	jobject bnd,
	jfieldID fid,
	const Mat& img,
	const char* name,
	const char* ext
){
	vector<uchar> buf;
	imencode(ext,img,buf);
	jbyteArray arr = env->NewByteArray(buf.size());
	env->SetByteArrayRegion(
		arr,
		0,buf.size(),
		(jbyte*)&buf[0]
	);
	env->SetObjectField(bnd,fid,arr);
}

void drawImage(Mat& overlay,const Mat& src){
	Mat chan[4];//Blue,Green,Red,Alpha
	split(overlay,chan);
	if(src.type()==CV_8UC1){
		chan[0] = src;
		chan[1] = src;
		chan[2] = src;
	}
	chan[3] = 255;
	merge(chan,4,overlay);
}

void drawEdgeMap(Mat& overlay,const Mat& edge){
	if(edge.type()!=CV_8UC1){
		cerr<<"[drawEdge] only support mono picture"<<endl;
		return;
	}
	Mat chan[4];//Blue,Green,Red,Alpha
	split(overlay,chan);
	chan[1] += (edge);
	chan[3] += (edge);
	merge(chan,4,overlay);
}

void drawRectangle(
	Mat& overlay,
	Rect rec,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	rectangle(overlay,rec,clr,thickness,lineType);
}

void drawRectangle(
	Mat& overlay,
	Point center,
	int width,
	int height,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	width = width/2;
	height= height/2;
	Point pt1 = center;
	Point pt2 = center;
	pt1.x = pt1.x - width;
	pt1.y = pt1.y - height;
	pt2.x = pt2.x + width;
	pt2.y = pt2.y + height;
	rectangle(overlay,pt1,pt2,clr,thickness,lineType);
}

void drawCrossT(
	Mat& overlay,
	Point pts,
	int lineSize,
	const Scalar& color,
	int thickness
){
	Scalar clr = color;
	clr[3] = 255;
	lineSize = lineSize/2;
	Point p1,p2;
	p1 = p2 = pts;
	p1.x = p1.x - lineSize;
	p2.x = p2.x + lineSize;
	line(overlay,p1,p2,clr,thickness);
	p1 = p2 = pts;
	p1.y = p1.y - lineSize;
	p2.y = p2.y + lineSize;
	line(overlay,p1,p2,clr,thickness);
}

void drawCrossX(
	Mat& overlay,
	Point pts,
	int lineSize,
	const Scalar& color,
	int thickness
){
	Scalar clr = color;
	clr[3] = 255;
	lineSize = ceil((lineSize*1.414213562f)/4.f);
	Point p1,p2;
	p1 = p2 = pts;
	p1.x = p1.x - lineSize;
	p1.y = p1.y - lineSize;
	p2.x = p2.x + lineSize;
	p2.y = p2.y + lineSize;
	line(overlay,p1,p2,clr,thickness);
	p1 = p2 = pts;
	p1.x = p1.x + lineSize;
	p1.y = p1.y - lineSize;
	p2.x = p2.x - lineSize;
	p2.y = p2.y + lineSize;
	line(overlay,p1,p2,clr,thickness);
}

static Scalar mapJetColor[]={
	Scalar(  0,  0,128,255),
	Scalar(  0,  0,255,255),
	Scalar(  0,128,255,255),
	Scalar(  2,255,254,255),
	Scalar(130,255,126,255),
	Scalar(255,252,  0,255),
	Scalar(255,124,  0,255),
	Scalar(252,  0,  0,255)
};
void drawContour(
	Mat& overlay,
	vector<vector<Point> >& cts
){
	for(int i=0; i<cts.size(); i++){
		polylines(
			overlay, cts[i],
			false,
			mapJetColor[i%8],
			1, LINE_8
		);
	}
}

static unsigned int mapIndex = 0;
void drawPolyline(
	Mat& overlay,
	vector<Point>& cts,
	bool closed,
	int thickness,
	int lineType
){
	polylines(
		overlay, cts, closed,
		mapJetColor[mapIndex%8],
		thickness, lineType
	);
	mapIndex++;
}

void drawPolyline(
	Mat& overlay,
	vector<Point>& cts,
	bool closed,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	polylines(
		overlay, cts, closed,
		clr,
		thickness, lineType
	);
}
//----------------------------//

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_saveImage(
	JNIEnv * env,
	jobject bundle,
	jstring jname
){
	MACRO_PREPARE
	if(buff==NULL){
		return;
	}
	Mat img(height,width,type,buff);

	char name[500];
	jstrcpy(env,jname,name);
	imwrite(name,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_saveImageROI(
	JNIEnv * env,
	jobject bundle,
	jstring jname,
	jintArray jroi
){
	MACRO_PREPARE
	if(buff==NULL){
		return;
	}
	Mat img(height,width,type,buff);

	jint* roi = env->GetIntArrayElements(jroi,NULL);
	Mat _img = img(Rect(
		roi[0],roi[1],
		roi[2],roi[3]
	));
	char name[500];
	jstrcpy(env,jname,name);
	imwrite(name,_img);
	env->ReleaseIntArrayElements(jroi,roi,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_loadImage(
	JNIEnv * env,
	jobject bundle,
	jstring jname,
	int flag
){
	MACRO_READY
	char name[500];
	jstrcpy(env,jname,name);
	Mat img = imread(name,flag);
	if(img.empty()==true){
		return;
	}
	MACRO_RESET_FIELD(0L,img.cols,img.rows,img.type())
	set_img_array(env,bundle,idImgBuff,img,"imgBuff",".png");
}

