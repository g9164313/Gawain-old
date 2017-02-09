#ifndef VISION_H
#define VISION_H

#include <jni.h>
#include <vector>
#include <iostream>
#include <fstream>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

//Do we need the below procedure~~~
extern cv::Mat* obj2mat(JNIEnv *env,jobject obj);
extern jobjectArray mat2array(JNIEnv *env,Mat& src);
extern void mapPoint(JNIEnv *env, jobject obj, cv::Point& dst);
extern void mapRect(JNIEnv *env, jobject obj, cv::Rect& dst);
extern void mapRect(JNIEnv *env, Rect& src, jobject dst);


extern Mat thresClamp(const Mat& src,int lower,int upper);

extern Mat filterVariance(
	const Mat& src,
	const int radius,
	double* min=NULL,
	double* max=NULL
);

inline Mat checkMono(Mat& src,int* _r){
	Mat dst;

	int typ =src.type();
	if(typ==CV_8UC1){
		dst = src;
	}else if(typ==CV_8UC3){
		Mat tmp(src.size(),CV_8UC3);
		cvtColor(src,tmp,COLOR_BGR2GRAY);
		dst = tmp;
	}else{
		cerr<<"no support convert"<<endl;
		dst = src;
	}

	if(_r!=NULL){
		cout<<"roi=["<<_r[0]<<","<<_r[1]<<"="<<_r[2]<<"x"<<_r[3]<<"]"<<endl;
		Rect roi(_r[0],_r[1],_r[2],_r[3]);
		return dst(roi);
	}
	return dst;
}

inline Mat checkMono(JNIEnv* env,jintArray roi,Mat& src){
	jint _roi[10];
	env->GetIntArrayRegion(roi,0,4,_roi);
	return checkMono(src,_roi);
}

inline Mat checkROI(Mat& src,int* _r){
	if(_r==NULL){
		return src;
	}
	Rect roi(_r[0],_r[1],_r[2],_r[3]);
	cout<<"roi=["<<_r[0]<<","<<_r[1]<<"="<<_r[2]<<"x"<<_r[3]<<"]"<<endl;
	return src(roi);
}

inline Mat checkROI(JNIEnv* env,jintArray roi,Mat& src){
	jint _roi[10];
	env->GetIntArrayRegion(roi,0,4,_roi);
	return checkROI(src,_roi);
}

#define TICK_BEG \
	{int64 __tick=getTickCount();
#define TICK_END(tag) \
	double __tick_sec = (double)((getTickCount() - __tick))/getTickFrequency();\
	std::cout<<"["tag"] " << __tick_sec <<"sec"<< endl; }
#define TICK_END2(tag,accum) \
	double __tick_sec = (double)((getTickCount() - __tick))/getTickFrequency();\
	accum = accum + __tick_sec;\
	std::cout<<"["tag"] " << __tick_sec <<"sec"<< endl; }

inline Point average(vector<Point>& pts){
	Point avg(0,0);
	size_t cnt = pts.size();
	for(size_t i=0; i<cnt; i++){
		avg = avg + pts[i];
	}
	avg.x = avg.x / cnt;
	avg.y = avg.y / cnt;
	return avg;
}

inline void maskPixel(Mat& img,InputArray msk){
	Mat tmp;
	img.copyTo(tmp,msk);
	img = tmp;
}

inline bool check_roi(Rect& roi,Mat& bound){
	if(roi.x<0 || roi.y<0 || roi.width<=0 || roi.height<=0){
		return false;
	}
	if((roi.x+roi.width)>bound.cols){
		return false;
	}
	if((roi.y+roi.height)>bound.rows){
		return false;
	}
	return true;
}

inline bool checkDimension(const Mat& src,const Mat& ref){
	if(src.cols==ref.cols && src.rows==ref.rows){
		return true;
	}
	return false;
}

inline void check32f(const Mat& src,Mat& dst){
	if(src.type()==CV_32FC1){
		dst = src;
	}else{
		src.convertTo(dst,CV_32FC1);
	}
}

inline void check32f(const Mat& src,Rect& roi,Mat& dst){
	if(src.type()==CV_32FC1){
		src(roi).copyTo(dst);
	}else{
		src(roi).convertTo(dst,CV_32FC1);
	}
}

inline void valid_roi(Rect& roi,const Mat& bound){
	if(roi.x<0){
		roi.x = 0;
	}
	if((roi.x+roi.width)>bound.cols){
		roi.width = bound.cols - roi.x;
	}
	if(roi.y<0){
		roi.y = 0;
	}
	if((roi.y+roi.height)>bound.rows){
		roi.height = bound.rows - roi.y;
	}
}

inline void extend_roi(Rect& roi,int dw,int dh,Mat& bound){
	roi.x=roi.x-dw;
	roi.y=roi.y-dh;
	roi.width = roi.width+2*dw;
	roi.height = roi.height+2*dh;
	valid_roi(roi,bound);
}

inline void shrink2org(Point& p,int xx,int yy){
	if(xx>0){ p.x=p.x-xx; if(p.x<0){p.x=0;} }
	if(yy>0){ p.y=p.y-yy; if(p.y<0){p.y=0;} }
}

inline void dump32f(const char* name,Mat& src){
	Mat tmp;
	check32f(src,tmp);
	normalize(tmp,tmp,0,255,NORM_MINMAX);
	imwrite(name,tmp);
}

inline void dump32f_log(const char* name,Mat& src){
	Mat tmp;
	check32f(src,tmp);
	log(tmp,tmp);
	normalize(tmp,tmp,0,255,NORM_MINMAX);
	imwrite(name,tmp);
}

inline void dumpOverlay(const char* name,Mat& srcA,Mat& srcB){
	Mat tmp;//check again~~~
	addWeighted(srcA,0.5,srcB,0.5,0,tmp);
	imwrite(name,tmp);
}

template<typename T>
inline static T square(const T a) {
  return a * a;
}

inline Rect cutLeft(Mat& img,int off){
	int ww = img.cols;
	int hh = img.rows;
	Rect rng;
	rng.x = ww - off;
	rng.y = 0;
	rng.width = off;
	rng.height= hh;
	return rng;
}

inline Rect cutRight(Mat& img,int off){
	int ww = img.cols;
	int hh = img.rows;
	Rect rng;
	rng.x = 0;
	rng.y = 0;
	rng.width = off;
	rng.height= hh;
	return rng;
}

inline Rect cutTop(Mat& img,int off){
	int ww = img.cols;
	int hh = img.rows;
	Rect rng;
	rng.x = 0;
	rng.y = 0;
	rng.width = ww;
	rng.height= off;
	return rng;
}

inline Rect cutBottom(Mat& img,int off){
	int ww = img.cols;
	int hh = img.rows;
	Rect rng;
	rng.x = 0;
	rng.y = hh-off;
	rng.width = ww;
	rng.height= off;
	return rng;
}

inline Rect cutLeft(Mat& img,int nu,int de){
	int off = (img.cols*nu)/de;
	return cutLeft(img,off);
}

inline Rect cutRight(Mat& img,int nu,int de){
	int off = (img.cols*nu)/de;
	return cutRight(img,off);
}

inline Rect cutTop(Mat& img,int nu,int de){
	int off = (img.rows*nu)/de;
	return cutTop(img,off);
}

inline Rect cutBottom(Mat& img,int nu,int de){
	int off = (img.rows*nu)/de;
	return cutBottom(img,off);
}

inline Point point2line(Point qq,Point p0,Point p1){
	Mat det(2,2,CV_32FC1);
	Mat cof(2,1,CV_32FC1);
	Mat ans(2,1,CV_32FC1);
	det.at<float>(0,0) = p1.x - p0.x;
	det.at<float>(0,1) = p1.y - p0.y;
	det.at<float>(1,0) = p0.y - p1.y;
	det.at<float>(1,1) = p1.x - p0.x;
	cof.at<float>(0,0) = qq.x*(p1.x-p0.x)+qq.y*(p1.y-p0.y);
	cof.at<float>(1,0) = p0.y*(p1.x-p0.x)-p0.x*(p1.y-p0.y);
	ans = det.inv() * cof;
	return Point(ans.at<float>(0,0),ans.at<float>(1,0));
}

inline Point intersect(Point* vtx){
	//'vertex' is clock -wise
	Point p;
	double x1=vtx[0].x,x2=vtx[2].x;
	double y1=vtx[0].y,y2=vtx[2].y;
	double x3=vtx[1].x,x4=vtx[3].x;
	double y3=vtx[1].y,y4=vtx[3].y;
	p.x=cvRound(
		((x1*y2-y1*x2)*(x3-x4)-(x1-x2)*(x3*y4-y3*x4)) /
		((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4))
	);
	p.y=cvRound(
		((x1*y2-y1*x2)*(y3-y4)-(y1-y2)*(x3*y4-y3*x4)) /
		((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4))
	);
	return p;
}

inline Point intersect(Vec4f& la,Vec4f& lb){
	Point p;
	double x1=la[2],x2=la[2]+la[0];
	double y1=la[3],y2=la[3]+la[1];
	double x3=lb[2],x4=lb[2]+lb[0];
	double y3=lb[3],y4=lb[3]+lb[1];
	p.x=cvRound(
		((x1*y2-y1*x2)*(x3-x4)-(x1-x2)*(x3*y4-y3*x4)) /
		((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4))
	);
	p.y=cvRound(
		((x1*y2-y1*x2)*(y3-y4)-(y1-y2)*(x3*y4-y3*x4)) /
		((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4))
	);
	return p;
}

inline int hypot2i(Point& a,Point& b){
	return hypot(a.x-b.x,a.y-b.y);
}

inline float hypot2f(Point& a,Point& b){
	return hypot(a.x-b.x,a.y-b.y);
}

inline float hypot2f(Point2f& a,Point2f& b){
	return hypot(a.x-b.x,a.y-b.y);
}

inline void thresholdRange(Mat& in,Mat& out,int lev1, int lev2){
	if(lev1>lev2){
		int tmp=lev2;
		lev2=lev1;
		lev1=tmp;
	}
	Mat out1(in.size(),CV_8UC1);
	threshold(in,out1,lev1,255,THRESH_BINARY);
	Mat out2(in.size(),CV_8UC1);
	threshold(in,out2,lev2,255,THRESH_BINARY_INV);
	out = out1 & out2;
}

inline bool IsInRange(float val,float base,float delta){
	if(delta>=abs(val-base)){
		return true;
	}
	return false;
}

inline float ratio(float aa,float bb){
	if(aa>bb){
		std::swap(aa,bb);
	}
	return aa/bb;
}
//----------------------------//

template <typename T> double find_dot(
	T& v1,T& v2,
	float* d1=NULL,float* d2=NULL
){
	float len;
	if(d1!=NULL){ *d1=hypot(v1.x,v1.y); }
	if(d2!=NULL){ *d2=hypot(v2.x,v2.y);	}
	return v1.ddot(v2);
}

template <typename T> double find_cos(T& v1,T& v2){
	float val;
	float d1,d2;
	val = find_dot(v1,v2,&d1,&d2);
	val = val / (d1*d2);
	return val;
}

template <typename T> double find_rad(T& v1,T& v2){
	return acos(find_cos(v1,v2));
}

template <typename T> double find_deg(T& v1,T& v2){
	double val = find_rad(v1,v2);
	val = (val*180.)/M_PI;
	val = fmod(val,180.);
	return val;
}

template <typename T> double find_dot(
	T& p0,
	T& p1,T& p2,
	float* d01=NULL,float* d02=NULL
){
	Point2f v1(p1.x-p0.x,p1.y-p0.y);
	Point2f v2(p2.x-p0.x,p2.y-p0.y);
	return find_dot(v1,v2,d01,d02);
}

template <typename T> double find_cos(
	T& p0,
	T& p1,T& p2
){
	Point2f v1(p1.x-p0.x,p1.y-p0.y);
	Point2f v2(p2.x-p0.x,p2.y-p0.y);
	return find_cos(v1,v2);
}

template <typename T> double find_deg(
	T& p0,
	T& p1,T& p2
){
	Point2f v1(p1.x-p0.x,p1.y-p0.y);
	Point2f v2(p2.x-p0.x,p2.y-p0.y);
	return find_deg(v1,v2);
}

template <typename T> double find_rad(
	T& p0,
	T& p1,T& p2
){
	Point2f v1(p1.x-p0.x,p1.y-p0.y);
	Point2f v2(p2.x-p0.x,p2.y-p0.y);
	return find_rad(v1,v2);
}

inline int check_mod(int a, int b){
	int c = a/b;
	if(c==0){
		return a;
	}
	return a%b;
}

inline int pry_down_size(int a){
	int rem = (a%2==0)?(0):(1);
	a=(a/2)+rem;
	return a;
}

inline Mat norm_32f(const Mat& src){
    Mat dst;
    normalize(src, dst, 0., 255., NORM_MINMAX);
    return dst;
}

#endif


