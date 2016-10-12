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

extern Scalar mapJetColor[];
void drawContour(
	Mat& overlay,
	vector<vector<Point> >& cts
){
	for(int i=0; i<cts.size(); i++){
		polylines(
			overlay, cts[i],
			true,
			mapJetColor[i%256],
			1, LINE_8
		);
	}
}

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


Scalar mapJetColor[] = {
	Scalar(  0,  0,128,255), Scalar(  0,  0,132,255), Scalar(  0,  0,136,255), Scalar(  0,  0,140,255),
	Scalar(  0,  0,144,255), Scalar(  0,  0,148,255), Scalar(  0,  0,152,255), Scalar(  0,  0,156,255),
	Scalar(  0,  0,160,255), Scalar(  0,  0,164,255), Scalar(  0,  0,168,255), Scalar(  0,  0,172,255),
	Scalar(  0,  0,176,255), Scalar(  0,  0,180,255), Scalar(  0,  0,184,255), Scalar(  0,  0,188,255),
	Scalar(  0,  0,192,255), Scalar(  0,  0,196,255), Scalar(  0,  0,200,255), Scalar(  0,  0,204,255),
	Scalar(  0,  0,208,255), Scalar(  0,  0,212,255), Scalar(  0,  0,216,255), Scalar(  0,  0,220,255),
	Scalar(  0,  0,224,255), Scalar(  0,  0,228,255), Scalar(  0,  0,232,255), Scalar(  0,  0,236,255),
	Scalar(  0,  0,240,255), Scalar(  0,  0,244,255), Scalar(  0,  0,248,255), Scalar(  0,  0,252,255),
	Scalar(  0,  0,255,255), Scalar(  0,  4,255,255), Scalar(  0,  8,255,255), Scalar(  0, 12,255,255),
	Scalar(  0, 16,255,255), Scalar(  0, 20,255,255), Scalar(  0, 24,255,255), Scalar(  0, 28,255,255),
	Scalar(  0, 32,255,255), Scalar(  0, 36,255,255), Scalar(  0, 40,255,255), Scalar(  0, 44,255,255),
	Scalar(  0, 48,255,255), Scalar(  0, 52,255,255), Scalar(  0, 56,255,255), Scalar(  0, 60,255,255),
	Scalar(  0, 64,255,255), Scalar(  0, 68,255,255), Scalar(  0, 72,255,255), Scalar(  0, 76,255,255),
	Scalar(  0, 80,255,255), Scalar(  0, 84,255,255), Scalar(  0, 88,255,255), Scalar(  0, 92,255,255),
	Scalar(  0, 96,255,255), Scalar(  0,100,255,255), Scalar(  0,104,255,255), Scalar(  0,108,255,255),
	Scalar(  0,112,255,255), Scalar(  0,116,255,255), Scalar(  0,120,255,255), Scalar(  0,124,255,255),
	Scalar(  0,128,255,255), Scalar(  0,132,255,255), Scalar(  0,136,255,255), Scalar(  0,140,255,255),
	Scalar(  0,144,255,255), Scalar(  0,148,255,255), Scalar(  0,152,255,255), Scalar(  0,156,255,255),
	Scalar(  0,160,255,255), Scalar(  0,164,255,255), Scalar(  0,168,255,255), Scalar(  0,172,255,255),
	Scalar(  0,176,255,255), Scalar(  0,180,255,255), Scalar(  0,184,255,255), Scalar(  0,188,255,255),
	Scalar(  0,192,255,255), Scalar(  0,196,255,255), Scalar(  0,200,255,255), Scalar(  0,204,255,255),
	Scalar(  0,208,255,255), Scalar(  0,212,255,255), Scalar(  0,216,255,255), Scalar(  0,220,255,255),
	Scalar(  0,224,255,255), Scalar(  0,228,255,255), Scalar(  0,232,255,255), Scalar(  0,236,255,255),
	Scalar(  0,240,255,255), Scalar(  0,244,255,255), Scalar(  0,248,255,255), Scalar(  0,252,255,255),
	Scalar(  2,255,254,255), Scalar(  6,255,250,255), Scalar( 10,255,246,255), Scalar( 14,255,242,255),
	Scalar( 18,255,238,255), Scalar( 22,255,234,255), Scalar( 26,255,230,255), Scalar( 30,255,226,255),
	Scalar( 34,255,222,255), Scalar( 38,255,218,255), Scalar( 42,255,214,255), Scalar( 46,255,210,255),
	Scalar( 50,255,206,255), Scalar( 54,255,202,255), Scalar( 58,255,198,255), Scalar( 62,255,194,255),
	Scalar( 66,255,190,255), Scalar( 70,255,186,255), Scalar( 74,255,182,255), Scalar( 78,255,178,255),
	Scalar( 82,255,174,255), Scalar( 86,255,170,255), Scalar( 90,255,166,255), Scalar( 94,255,162,255),
	Scalar( 98,255,158,255), Scalar(102,255,154,255), Scalar(106,255,150,255), Scalar(110,255,146,255),
	Scalar(114,255,142,255), Scalar(118,255,138,255), Scalar(122,255,134,255), Scalar(126,255,130,255),
	Scalar(130,255,126,255), Scalar(134,255,122,255), Scalar(138,255,118,255), Scalar(142,255,114,255),
	Scalar(146,255,110,255), Scalar(150,255,106,255), Scalar(154,255,102,255), Scalar(158,255, 98,255),
	Scalar(162,255, 94,255), Scalar(166,255, 90,255), Scalar(170,255, 86,255), Scalar(174,255, 82,255),
	Scalar(178,255, 78,255), Scalar(182,255, 74,255), Scalar(186,255, 70,255), Scalar(190,255, 66,255),
	Scalar(194,255, 62,255), Scalar(198,255, 58,255), Scalar(202,255, 54,255), Scalar(206,255, 50,255),
	Scalar(210,255, 46,255), Scalar(214,255, 42,255), Scalar(218,255, 38,255), Scalar(222,255, 34,255),
	Scalar(226,255, 30,255), Scalar(230,255, 26,255), Scalar(234,255, 22,255), Scalar(238,255, 18,255),
	Scalar(242,255, 14,255), Scalar(246,255, 10,255), Scalar(250,255,  6,255), Scalar(254,255,  1,255),
	Scalar(255,252,  0,255), Scalar(255,248,  0,255), Scalar(255,244,  0,255), Scalar(255,240,  0,255),
	Scalar(255,236,  0,255), Scalar(255,232,  0,255), Scalar(255,228,  0,255), Scalar(255,224,  0,255),
	Scalar(255,220,  0,255), Scalar(255,216,  0,255), Scalar(255,212,  0,255), Scalar(255,208,  0,255),
	Scalar(255,204,  0,255), Scalar(255,200,  0,255), Scalar(255,196,  0,255), Scalar(255,192,  0,255),
	Scalar(255,188,  0,255), Scalar(255,184,  0,255), Scalar(255,180,  0,255), Scalar(255,176,  0,255),
	Scalar(255,172,  0,255), Scalar(255,168,  0,255), Scalar(255,164,  0,255), Scalar(255,160,  0,255),
	Scalar(255,156,  0,255), Scalar(255,152,  0,255), Scalar(255,148,  0,255), Scalar(255,144,  0,255),
	Scalar(255,140,  0,255), Scalar(255,136,  0,255), Scalar(255,132,  0,255), Scalar(255,128,  0,255),
	Scalar(255,124,  0,255), Scalar(255,120,  0,255), Scalar(255,116,  0,255), Scalar(255,112,  0,255),
	Scalar(255,108,  0,255), Scalar(255,104,  0,255), Scalar(255,100,  0,255), Scalar(255, 96,  0,255),
	Scalar(255, 92,  0,255), Scalar(255, 88,  0,255), Scalar(255, 84,  0,255), Scalar(255, 80,  0,255),
	Scalar(255, 76,  0,255), Scalar(255, 72,  0,255), Scalar(255, 68,  0,255), Scalar(255, 64,  0,255),
	Scalar(255, 60,  0,255), Scalar(255, 56,  0,255), Scalar(255, 52,  0,255), Scalar(255, 48,  0,255),
	Scalar(255, 44,  0,255), Scalar(255, 40,  0,255), Scalar(255, 36,  0,255), Scalar(255, 32,  0,255),
	Scalar(255, 28,  0,255), Scalar(255, 24,  0,255), Scalar(255, 20,  0,255), Scalar(255, 16,  0,255),
	Scalar(255, 12,  0,255), Scalar(255,  8,  0,255), Scalar(255,  4,  0,255), Scalar(255,  0,  0,255),
	Scalar(252,  0,  0,255), Scalar(248,  0,  0,255), Scalar(244,  0,  0,255), Scalar(240,  0,  0,255),
	Scalar(236,  0,  0,255), Scalar(232,  0,  0,255), Scalar(228,  0,  0,255), Scalar(224,  0,  0,255),
	Scalar(220,  0,  0,255), Scalar(216,  0,  0,255), Scalar(212,  0,  0,255), Scalar(208,  0,  0,255),
	Scalar(204,  0,  0,255), Scalar(200,  0,  0,255), Scalar(196,  0,  0,255), Scalar(192,  0,  0,255),
	Scalar(188,  0,  0,255), Scalar(184,  0,  0,255), Scalar(180,  0,  0,255), Scalar(176,  0,  0,255),
	Scalar(172,  0,  0,255), Scalar(168,  0,  0,255), Scalar(164,  0,  0,255), Scalar(160,  0,  0,255),
	Scalar(156,  0,  0,255), Scalar(152,  0,  0,255), Scalar(148,  0,  0,255), Scalar(144,  0,  0,255),
	Scalar(140,  0,  0,255), Scalar(136,  0,  0,255), Scalar(132,  0,  0,255), Scalar(128,  0,  0,255),
};


