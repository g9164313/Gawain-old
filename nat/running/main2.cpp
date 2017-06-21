#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fstream>
#include <global.hpp>
#include <algorithm>
#include <opencv2/face.hpp>
#include "../util_ipc.hpp"

using namespace cv;
using namespace face;

extern int test_sfr(
	Mat& img,
	double scale,
	vector<double>& frq,
	vector<double>& sfr
);

Point findRedCross(Mat& img){
	Point loc;

	Mat chan[3];
	split(img,chan);

	Mat out1,out2,out3;
	bitwise_xor(chan[2],chan[0],out1);
	bitwise_xor(chan[2],chan[1],out2);
	bitwise_and(out1,out2,out3);

	threshold(out3,out1,254,255,THRESH_BINARY);

	const int len = 11;

	out2 = Mat::zeros(len,len,CV_8UC1);
	line(out2, Point(5,0), Point(len/2,len), Scalar::all(255));
	line(out2, Point(0,5), Point(len,len/2), Scalar::all(255));

	out3 = Mat::zeros(
		img.rows - out2.rows + 1,
		img.cols - out2.cols + 1,
		CV_32FC1
	);

	matchTemplate(out1, out2, out3, CV_TM_SQDIFF_NORMED);

	minMaxLoc(out3,NULL,NULL,&loc,NULL);

	loc.x = loc.x + len/2;
	loc.y = loc.y + len/2;

	circle(img,loc,5,Scalar(0,255,255));

	return loc;
}

#define MACRO_CHECK(tag,per) {\
	roi = gray(tag); \
	idx = test_sfr(roi, scale, frq, sfr); \
	idx = (int)(frq.size()*per); \
	val = (int)(sfr[idx]*10000); \
	printf("%d\t",val); \
	sprintf(txt,"%d",val); \
	putText(img, txt, Point(tag.x,tag.y)+off, FONT_HERSHEY_COMPLEX, 1., clr); \
}

void check_all_mtf(const char* name, const char* out_dir){

	Point off(5,30);
	Scalar clr(0,255,255);

	Mat img = imread(name);
	Mat gray = imread(name,IMREAD_GRAYSCALE);

	Point aa = findRedCross(img);
	if(aa.x<(img.cols/2-50) || (img.cols/2+50)<aa.x){
		printf("0\t0\t0\t0\t0\t\n");
		return;
	}
	if(aa.x<(img.cols/2-50) || (img.cols/2+50)<aa.x){
		printf("0\t0\t0\t0\t0\t\n");
		return;
	}

	//first test~~~
	/*Rect cent (aa.x- 27, aa.y+ 13, 48, 60);
	Rect lf_tp(aa.x-285, aa.y-176, 48, 60);
	Rect lf_bm(aa.x-285, aa.y+105, 48, 60);
	Rect rh_tp(aa.x+238, aa.y-168, 48, 60);
	Rect rh_bm(aa.x+238, aa.y+107, 48, 60);*/

	//second test~~~
	/*Rect cent (aa.x- 44, aa.y+ 13, 78, 111);
	Rect lf_tp(aa.x-256, aa.y-181, 54, 60);
	Rect lf_bm(aa.x-262, aa.y+118, 54, 60);
	Rect rh_tp(aa.x+209, aa.y-177, 54, 60);
	Rect rh_bm(aa.x+213, aa.y+122, 54, 60);*/

	//third test~~~
	Rect cent (aa.x- 28, aa.y+ 11, 48, 60);
	Rect lf_tp(aa.x-249, aa.y-157, 48, 60);
	Rect lf_bm(aa.x-253, aa.y+ 87, 48, 60);
	Rect rh_tp(aa.x+208, aa.y-150, 48, 60);
	Rect rh_bm(aa.x+208, aa.y+ 90, 48, 60);

	rectangle(img,cent,clr);
	rectangle(img,lf_tp ,clr);
	rectangle(img,lf_bm ,clr);
	rectangle(img,rh_tp ,clr);
	rectangle(img,rh_bm ,clr);

	int idx,val;
	char txt[100];
	double scale = (1./5.)*1000.;
	vector<double> frq, sfr;
	Mat roi;

	MACRO_CHECK(cent,0.5);
	MACRO_CHECK(lf_tp,0.5);
	MACRO_CHECK(rh_tp,0.5);
	MACRO_CHECK(lf_bm,0.5);
	MACRO_CHECK(rh_bm,0.5);
	printf("\n");

	if(out_dir!=NULL){
		string fs_name(name);
		fs_name = fs_name.substr(
			0,
			fs_name.find_last_of(".")
		);
		fs_name = fs_name.substr(
			fs_name.find_last_of("/")+1,
			fs_name.length()
		);
		sprintf(txt,"%s/%s.png",out_dir,fs_name.c_str());
		imwrite(txt,img);
	}else{
		imwrite("dd.png",img);
	}

	/*printf("RH_BM:MTF50=(%.1f, %.1f)\n\n",frq[idx],sfr[idx]);
	printf("Freq    SFR    \n");
	for(int i=0; i<frq.size(); i++){
		printf("%d) %.2f    %.2f\n", i, frq[i], sfr[i]);
	}
	printf("===[%ld]===\n",frq.size());*/
}

int main(int argc, char* argv[]){

#define DIR_NAME "fisheye-test3"

	//const char* name = "./fisheye-test3/20170615_13_54_15_CurrentImage.bmp";
	//check_all_mtf(name,"./"DIR_NAME"/result");
	//return 1;

	printf("file\tCenter\tLT\tRT\tLB\tRB\n");
	ifstream lstFile("./"DIR_NAME"/list.txt");
	string line;
	while(getline(lstFile, line)){
		printf("%s\t",line.c_str());
		char name[100];
		sprintf(name,"./"DIR_NAME"/%s",line.c_str());
		check_all_mtf(name,"./"DIR_NAME"/result");
	}
	return 0;
}
//--------------------------------------------//

extern void registration(Mat& imgRef,Mat& imgSrc);

int main5(int argc, char* argv[]){

	Mat ref = imread("./reg-ref.bmp",IMREAD_GRAYSCALE);

	Mat src1 = imread("./reg-src-a15.bmp",IMREAD_GRAYSCALE);
	Mat src2 = imread("./reg-src-a30.bmp",IMREAD_GRAYSCALE);
	Mat src3 = imread("./reg-src-a45.bmp",IMREAD_GRAYSCALE);
	Mat src4 = imread("./reg-src-a60.bmp",IMREAD_GRAYSCALE);

	Mat src5 = imread("./reg-src-o+13+13.bmp",IMREAD_GRAYSCALE);
	Mat src6 = imread("./reg-src-o+35+35.bmp",IMREAD_GRAYSCALE);

	Mat src7 = imread("./reg-src-test.bmp",IMREAD_GRAYSCALE);


	Mat src = src7;

	//registration(ref,src);

	Mat sum;
	addWeighted( ref, 0.5, src, 0.5, 0.0, sum);

	imwrite("cc4.bmp",sum);
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

int main4(int argc, char* argv[]){

	Vec4i gg;

	/*Mat src = imread("./wiggle/snap-04.png",IMREAD_GRAYSCALE);

	Rect roi;

	TICK_BEG

	src = reduce_dark(src);

	src = bound_nonzero(src);

	src = align_center(src);

	TICK_END("thres")
	*/

	//Rect rect(bord,bord,src.cols-bord*2,src.rows-bord*2);
	//TICK_BEG
	//grabCut( src, msk, rect, bgd, fgd, 1, GC_INIT_WITH_RECT);
	//msk = 250 * (msk - GC_PR_BGD);
	//TICK_END("grabcut")

	//imwrite("./wiggle/mask.png",src);

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
