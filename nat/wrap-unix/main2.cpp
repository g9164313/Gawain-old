#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <global.hpp>
#include <algorithm>
#include <opencv2/face.hpp>
#include "../util_ipc.hpp"

using namespace cv;
using namespace face;

int main5(int argc, char* argv[]){

	Ptr<LBPHFaceRecognizer> mod = createLBPHFaceRecognizer();

	return 0;
}
//--------------------------------------------//

Mat reduce_dark(Mat& src){

	Mat msk = Mat::ones(src.size(),CV_8UC1);

	int bnd = (std::min(src.cols,src.rows)*10)/100;

	msk(Rect(bnd,bnd,src.cols-2*bnd,src.rows-2*bnd)) = 0;

	double max;
	minMaxIdx(src, NULL, &max, NULL, NULL, msk);

	Scalar avg,dev;
	meanStdDev(src,avg,dev,msk);

	Mat edg;
	threshold(src,edg,max+dev[0],0.,THRESH_TOZERO);

	return edg;
}

Mat bound_nonzero(Mat& src){
	int i,cnt;

	int top = 0;
	for(i=top; i<src.rows/2-1; i++){
		cnt = countNonZero(src.row(i));
		if(cnt!=0){
			top = i;
			break;
		}
	}

	int bottom = src.rows-1;
	for(i=bottom; i>src.rows/2+1; --i){
		cnt = countNonZero(src.row(i));
		if(cnt!=0){
			bottom = i;
			break;
		}
	}

	int left = 0;
	for(i=left; i<src.cols/2-1; i++){
		cnt = countNonZero(src.col(i));
		if(cnt!=0){
			left = i;
			break;
		}
	}

	int right = src.cols - 1;
	for(i=right; i>src.cols/2+1; --i){
		cnt = countNonZero(src.col(i));
		if(cnt!=0){
			right = i;
			break;
		}
	}

	Rect roi(left,top,right-left,bottom-top);

	Mat res;

	src(roi).copyTo(res);

	return res;
}


Mat align_center(Mat& Src){

	Mat src;

	const int BORD = 4;

	copyMakeBorder(
		Src,src,
		0,0,
		BORD,BORD,
		BORDER_CONSTANT,
		Scalar(0,0,0)
	);

	Mat res = Mat::zeros(Src.size(),src.type());

	for(int i=0; i<src.rows; i++){

		Mat _src = src.row(i);

		Moments mm = moments(_src,true);

		float cx = mm.m10 / mm.m00;

		Mat _dst;

		getRectSubPix(
			_src,
			Size(_src.cols-BORD*2,1),Point2f(cx,0),
			_dst
		);

		//printf("%03d) mx = %.2f\n",i,cx);

		_dst.copyTo(res.row(i));
	}

	//cout<<endl;
	return res;
}

int main(int argc, char* argv[]){

	Mat src = imread("./wiggle/snap-10.tif",IMREAD_GRAYSCALE);

	Rect roi;

	TICK_BEG

	src = reduce_dark(src);

	src = bound_nonzero(src);

	src = align_center(src);

	TICK_END("thres")

	//Rect rect(bord,bord,src.cols-bord*2,src.rows-bord*2);
	//TICK_BEG
	//grabCut( src, msk, rect, bgd, fgd, 1, GC_INIT_WITH_RECT);
	//msk = 250 * (msk - GC_PR_BGD);
	//TICK_END("grabcut")

	imwrite("./wiggle/mask.png",src);

	return 0;
}
//-------------------------------------------//

int main3(int argc, char* argv[]) {

	Mat src = imread("./wiggle/snap-02.png",IMREAD_GRAYSCALE);

	Mat dst = Mat::zeros(src.size(),CV_8UC1);

	//decide the first base-line~~~
	int baseLine,baseEdge;
	double maxVal,pixVal;
	Mat prevLine;
	for(
		baseLine=0, maxVal=0.;
		baseLine<src.rows/3 && maxVal<200;
		baseLine++
	){
		prevLine =src.row(baseLine);
		prevLine.copyTo(dst.row(baseLine));
		minMaxLoc(prevLine,NULL,&maxVal);
	}
	prevLine.copyTo(dst.row(baseLine));

	//decide the first edge from left to right
	prevLine =src.row(baseLine);
	maxVal = maxVal / 10.;
	for(
		baseEdge=0, pixVal=0.f;
		baseEdge<src.cols && pixVal<maxVal;
		baseEdge++
	){

		pixVal = prevLine.at<uint8_t>(0,baseEdge);
	}


	const int segRadius = 3;

	//for(int i=baseLine+1; i<src.rows; i++){
	for(int i=baseLine+1; i<baseLine+100; i++){

		Mat prvSegment= prevLine.colRange(baseEdge-segRadius, baseEdge+segRadius+1);
		//cout<<"prvSegment = "<<prvSegment<<endl<<endl;

		Mat curLine =src.row(i);
		//cout<<"curLine = "<<prevLine<<endl;

		vector<double> difValue;

		for(
			float stp= baseEdge - 1.f;
			stp <= baseEdge + 1.f;
			stp = stp + 0.1f
		){

			Mat difSegment;

			getRectSubPix(
				curLine,
				Size(segRadius*2+1,1),
				Point2f(stp,0.f),
				difSegment
			);

			Mat dif;

			absdiff(prvSegment,difSegment,dif);

			Scalar val = sum(dif);

			//cout<<"difSegment = "<<difSegment<<", ("<<val[0]<<endl;

			difValue.push_back(val[0]);
		}

		int idx = min_element(difValue.begin(), difValue.end()) - difValue.begin();

		cout<<"check-"<<idx<<endl;
		cout<<endl;

		getRectSubPix(
			curLine,
			Size(curLine.cols,1),
			Point2f(curLine.cols/2-1.f+0.1*((float)idx),0.f),
			prevLine
		);

		//cout<<"prvLine = "<<prevLine<<endl;
		prevLine.copyTo(dst.row(i));
	}


	imwrite("./wiggle/cc1.png",dst);

	return 0;
}

int main2(int argc, char* argv[]) {

	Mat aa(3,3,CV_8UC1);

	aa.at<uint8_t>(0,0) = 10;
	aa.at<uint8_t>(0,1) = 70;
	aa.at<uint8_t>(0,2) = 33;

	aa.at<uint8_t>(1,0) = 33;
	aa.at<uint8_t>(1,1) = 173;
	aa.at<uint8_t>(1,2) = 50;

	aa.at<uint8_t>(2,0) = 67;
	aa.at<uint8_t>(2,1) = 33;
	aa.at<uint8_t>(2,2) = 200;

	cout<<"src="<<endl<<aa<<endl<<endl;

	Mat bb;
	getRectSubPix(aa,Size(5,5),Point2f(0.2f,0.2f),bb);

	cout<<"dst="<<endl<<bb<<endl;

	return 0;
}

/*

extern Mat cutOutBounding(Mat& img,Mat& msk,int width,int height);

extern void removeNoise(Mat& msk,int* board);

extern void list_dir(string path,vector<string>& lst,string prex);

int main1(int argc, char* argv[]) {
	const int VAR_BACK = 16;
	const int VAR_FORE = 200;

#define DIR_NAME "./cam2/"
	string pathBack=DIR_NAME"back";
	string pathFore=DIR_NAME"fore";
	string pathMeas=DIR_NAME"meas";
	vector<string> lstBack;
	vector<string> lstFore;
	vector<string> lstMeas;
	list_dir(pathBack,lstBack,"");
	list_dir(pathFore,lstFore,"BottomSide");
	list_dir(pathMeas,lstMeas,"BottomSide");

	//step.1
	Ptr<BackgroundSubtractorMOG2> modBack = createBackgroundSubtractorMOG2(lstBack.size(),VAR_BACK);
	for(size_t i=0; i<lstBack.size(); i++){
		string name = pathBack+"/"+lstBack[i];
		cout<<"train back:"<<name<<endl;
		Mat img = imread(name);
		Mat msk(img.size(),CV_8UC1);
		modBack->apply(img,msk,0.9);
	}

	Mat backImg;
	modBack->getBackgroundImage(backImg);
	imwrite("./modBack.png",backImg);

#define TRAIN_FORE
#ifdef TRAIN_FORE
	//step.2
	int ow=0,oh=0;
	for(size_t i=0; i<lstFore.size(); i++){
		string name = pathFore+"/"+lstFore[i];
		Mat img = imread(name);
		Mat msk(img.size(),CV_8UC1);
		modBack->apply(img,msk,0);
		//imwrite(pathFore+"/aaa.tif",msk);
		Mat obj = cutOutBounding(img,msk,-1,-1);
		ow=ow + obj.size().width;
		oh=oh + obj.size().height;
		cout<<name<<"@ target size=("<<obj.size().width<<"x"<<obj.size().height<<endl;
		string o_name = lstFore[i];
		o_name = o_name.erase(0,3);
		o_name = "obj-"+o_name;
		imwrite(pathFore+"/"+o_name,obj);
	}
	ow = ow / lstFore.size();
	oh = oh / lstFore.size();
	//return 0;

	for(size_t i=0; i<lstFore.size(); i++){
		string name = pathFore+"/"+lstFore[i];
		Mat img = imread(name);
		Mat msk(img.size(),CV_8UC1);
		modBack->apply(img,msk,0);
		Mat obj = cutOutBounding(img,msk,ow,oh);
		string o_name = lstFore[i];
		o_name = o_name.erase(0,3);
		o_name = "obj-"+o_name;
		cout<<"dump "<<o_name<<"@ target size=("<<obj.size().width<<"x"<<obj.size().height<<endl;
		imwrite(pathFore+"/"+o_name,obj);//check data~~~
	}
#endif

	//step.3
#ifndef TRAIN_FORE
	int ow=-1,oh=-1;
#endif
	list_dir(pathFore,lstFore,"obj-");
	Ptr<BackgroundSubtractorMOG2> modFore = createBackgroundSubtractorMOG2(lstFore.size(),VAR_FORE);
	for(size_t i=0; i<lstFore.size(); i++){
		string name = pathFore+"/"+lstFore[i];
		cout<<"train fore:"<<name<<endl;
		Mat img = imread(name);
		Mat msk(img.size(),CV_8UC1);
		modFore->apply(img,msk,0.9);
#ifndef TRAIN_FORE
		ow=img.cols;
		oh=img.rows;
#endif
	}

	Mat foreImg;
	modFore->getBackgroundImage(foreImg);
	imwrite("./modFore.png",foreImg);

	//step.4 - measure sample data~~~~
	for(size_t i=0; i<lstMeas.size(); i++){
		string name = pathMeas+"/"+lstMeas[i];
		Mat img = imread(name);

		Mat msk1(img.size(),CV_8UC1);
		TICK_BEG
		modBack->apply(img,msk1,0);
		cout<<"measure "<<name<<"@";//print some head~~~
		TICK_END("measure")

		Mat obj = cutOutBounding(img,msk1,ow,oh);

		Mat msk2(obj.size(),CV_8UC1);
		modFore->apply(obj,msk2,0);

		string m_name = "mak-"+lstMeas[i];//generate mask
		imwrite(pathMeas+"/"+m_name,msk2);//generate mask

		int skip[]={10,10,10,10};
		removeNoise(msk2,skip);

		vector<vector<Point> > cts;
		findContours(msk2,cts,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
		int dots=countNonZero(msk2);
		cout<<"cts="<<cts.size()<<",dots="<<dots;
		string r_name = "obj-"+lstMeas[i];
		imwrite(pathMeas+"/"+r_name,obj);
		if(cts.size()!=0){
			//we have some spot~~~
			drawContours(obj,cts,-1,Scalar(0,0,255));
			r_name = "rst-"+lstMeas[i];
			imwrite(pathMeas+"/"+r_name,obj);
		}
		/tring o_name = "obj"+lstMeas[i].erase(0,3);
		imwrite(pathMeas+"/"+o_name,obj);
		string r_name = "res"+lstMeas[i];
		Mat res;
		obj.copyTo(res,msk2);
		imwrite(pathMeas+"/"+r_name,res);
		cout<<"@"<<o_name<<"-->"<<r_name<<endl;
		int cnt=countNonZero(msk2);
		cout<<"measure:"<<name<<", cnt="<<cnt;
		cout<<endl;
	}

	return 0;
}
*/
