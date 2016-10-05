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

void drawEdge(Mat& overlay,const Mat& edge){
	if(edge.type()!=CV_8UC1){
		cerr<<"[drawEdge] only support mono picture"<<endl;
		return;
	}
	Mat chan[4];//Blue,Green,Red,Alpha
	split(overlay,chan);
	chan[1] = chan[1] + (edge*255);
	chan[3] = chan[3] + (edge*255);
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
	Point pt1,
	Point pt2,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	rectangle(overlay,pt1,pt2,clr,thickness,lineType);
}

void drawCrossT(
	Mat& overlay,
	Point pts,
	const Scalar& color,
	int thickness,
	int lineSize
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
	const Scalar& color,
	int thickness,
	int lineSize
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

void drawPolyline(
	Mat& overlay,
	vector<Point>& cts,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	polylines(overlay, cts, true, clr, thickness, lineType);
}

void drawContour(
	Mat& overlay,
	vector<vector<Point> >& cts,
	const Scalar& color
){
	Scalar clr = color;
	clr[3] = 255;
	drawContours(
		overlay,
		cts, -1,
		clr
	);
}

/*extern "C" JNIEXPORT jbyteArray JNICALL Java_narl_itrc_CamBundle_getData(
	JNIEnv* env,
	jobject bundle
){
	MACRO_PREPARE
	if(buff==NULL){
		return NULL;
	}
	Mat img(height,width,type,buff);
	vector<uchar> buf;
	imencode(".jpg",img,buf);
	jbyteArray arrBuf = env->NewByteArray(buf.size());
	env->SetByteArrayRegion(
		arrBuf,
		0,buf.size(),
		(jbyte*)&buf[0]
	);
	return arrBuf;
}*/

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


