#include <fstream>
#include <algorithm>
#include <iostream>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

void meld(const Mat& src1, const Mat& src2, const char* name){
    Mat result;
    addWeighted( src1, 0.5, src2, 0.5, 0.0, result);
    imwrite(name,result);
}

RotatedRect getShape(
	const Mat& img,
	vector<Point>& cts,
	double epsilon
){
	vector<vector<Point> > ctors;

	findContours(
		img, ctors,
		RETR_EXTERNAL,
		CHAIN_APPROX_SIMPLE
	);

	size_t idx = -1;
	for(size_t i=0, max=0; i<ctors.size(); i++){
		size_t c = ctors[i].size();
		if(c>max){
			idx = i;
			max = c;
		}
	}

	approxPolyDP(ctors[idx], cts, epsilon, true);

	return minAreaRect(cts);
}

/**
 * modify angle from function, minAreaRect().<p>
 * This function may 'flip' data, so we will get a wrong angle.<p>
 * In other word, it means the first vertex may be bottom-right.<p>
 */
double getAngle(RotatedRect& rect){

	double angle = rect.angle;

	Point2f vtx[4];
	rect.points(vtx);

	Point2f va, vb;

	float e0 = hypot(vtx[0].x, vtx[0].y);
	float e1 = hypot(vtx[1].x, vtx[1].y);
	float e2 = hypot(vtx[2].x, vtx[2].y);

	if(!(e1<e0 && e1<e2)){
		//the first vertex is bottom-right.
		//why ???
		angle = angle + 84.5f;
	}
	return angle;
}

/**
 * 'refer' mean standard or golden-sample.<p>
 */
void matchShape(
	const vector<Point>& target,
	vector<Point>& refer
){
	RotatedRect r_tar = minAreaRect(target);
	RotatedRect r_ref = minAreaRect(refer);

	double a_ref = getAngle(r_tar);
	double a_tar = getAngle(r_ref);

	//adjust rotation
	Mat h1 =getRotationMatrix2D(r_ref.center, -a_ref+a_tar, 1.);
	transform(refer, refer, h1);

	//adjust offset to corner, top-left
	Mat h2 = Mat::eye(2,3,CV_64FC1);
	h2.at<double>(0,2) = -r_ref.center.x + r_ref.size.width/2.f;
	h2.at<double>(1,2) = -r_ref.center.y + r_ref.size.height/2.f;
	transform(refer, refer, h2);

	//try to match the location where we can have maximum correlation value.
	Rect b_tar = r_tar.boundingRect();

	Mat m_tar = Mat::zeros(
		r_tar.center.y + b_tar.height,
		r_tar.center.x + b_tar.width,
		CV_8UC1
	);
	polylines(m_tar, target, false, Scalar::all(255), 1, LINE_4);

	Mat m_ref = Mat::zeros(
		b_tar.height,
		b_tar.width,
		CV_8UC1
	);
	polylines(m_ref, refer, false, Scalar::all(255), 1, LINE_4);

	Mat result(
		m_tar.cols - m_ref.cols + 1,
		m_tar.rows - m_ref.rows + 1,
		CV_32FC1
	);
	matchTemplate(m_tar, m_ref, result, CV_TM_CCORR);//TODO: mask for speed???

	Point maxLoc;

	minMaxLoc(result, NULL, NULL, NULL, &maxLoc, Mat());

	h2.at<double>(0,2) = maxLoc.x;
	h2.at<double>(1,2) = maxLoc.y;
	transform(refer, refer, h2);
}

int main(int argc, char* argv[]) {

	const char* name1 = "./cv_sample2/10-1.png";
	const char* name2 = "./cv_sample2/9-1.png";
	//const char* name1 = "./pad_sample/aaa.png";
	//const char* name2 = "./pad_sample/bbb.png";

	Mat src1 = imread(name1,IMREAD_GRAYSCALE);
	Mat src2 = imread(name2,IMREAD_GRAYSCALE);

	Mat ova1 = imread(name1,IMREAD_COLOR);
	Mat ova2 = imread(name2,IMREAD_COLOR);

	threshold(src1,src1,150,255,THRESH_BINARY);

	//meld(src1,src2,"cc2.png");

	vector<Point> cts1, cts2;
	getShape(src1,cts1,2);
	getShape(src2,cts2,2);

	//RotatedRect rr = minAreaRect(cts1);
	//Point2f vv[4]; rr.points(vv);
	//line(ova1,vv[1],vv[2],Scalar(250,0,250),2);
	//line(ova1,vv[2],vv[3],Scalar(250,0,250),2);
	//polylines(ova1, cts1, true, Scalar(0,250,0), 1, LINE_8);

	matchShape(cts1,cts2);

	polylines(ova1, cts1, true, Scalar(0,250,0), 1, LINE_4);
	polylines(ova1, cts2, true, Scalar(0,0,255), 1, LINE_4);
	imwrite("cc3.png",ova1);
	return 0;
}

/*float trimShape(vector<Point>& aa, vector<Point>& bb){

	int count = aa.size() - bb.size();

	vector<Point>& ref = (count>0)?(bb):(aa);
	vector<Point>& tar = (count>0)?(aa):(bb);

	count = abs(count);

	Ptr<ShapeContextDistanceExtractor> dist = createShapeContextDistanceExtractor();

	float score = dist->computeDistance(ref,tar);

	//choose one point and let distance small.
#ifdef BRUTE_FORCE

	while(count>0){

		vector<float> bins;

		for(size_t i=0; i<tar.size(); i++){

			Point tmp = tar[i];

			tar.erase(tar.begin()+i);

			bins.push_back(dist->computeDistance(ref,tar));

			tar.insert(tar.begin()+i, tmp);
		}

		int idx = distance(
			bins.begin(),
			min_element(bins.begin(), bins.end())
		);

		tar.erase(tar.begin()+idx);

		count--;
	}
#else

	for(size_t i=0; count>0; i++){

		if(i>=tar.size()){
			tar.erase(tar.end()-count,tar.end());
			break;
		}

		Point tmp = tar[i];

		tar.erase(tar.begin()+i);

		float s_val = dist->computeDistance(ref,tar);

		if(s_val<score){
			score = s_val;
			count--;
		}else{
			tar.insert(tar.begin()+i, tmp);
		}
	}
#endif
	return dist->computeDistance(ref,tar);
}*/

/*Mat hat = estimateAffinePartial2D(_b,_a);
cout<<hat<<endl;
Mat _hat(3,3,hat.type());
_hat.at<double>(0,0) = hat.at<double>(0,0);
_hat.at<double>(0,1) = hat.at<double>(0,1);
_hat.at<double>(0,2) = hat.at<double>(0,2);
_hat.at<double>(1,0) = hat.at<double>(1,0);
_hat.at<double>(1,1) = hat.at<double>(1,1);
_hat.at<double>(1,2) = hat.at<double>(1,2);
_hat.at<double>(2,0) = 0.0;
_hat.at<double>(2,1) = 0.0;
_hat.at<double>(2,2) = 1.0;

Mat res;
warpPerspective(ova2, res, _hat, src1.size());
meld(ova1,res,"cc1.png");*/

/*vector<Point3f>& eign = (*ptr_eign);

Mat tmp(cts.size(), 2, CV_32FC1);
for(size_t i=0; i<cts.size(); i++){
	tmp.at<float>(i,0) = cts[i].x;
	tmp.at<float>(i,1) = cts[i].y;
}
PCA pca(tmp,noArray(),PCA::DATA_AS_ROW);

eign.push_back(Point3f(
	pca.eigenvectors.at<float>(0,0),
	pca.eigenvectors.at<float>(0,1),
	pca.eigenvalues.at<float>(0,0)
));
eign.push_back(Point3f(
	pca.eigenvectors.at<float>(1,0),
	pca.eigenvectors.at<float>(1,1),
	pca.eigenvalues.at<float>(0,1)
));*/


/*Ptr<AffineTransformer> trf = createAffineTransformer(true);
vector<DMatch> match;
vector<Point2f> f_a, f_b;
for(size_t i=0; i<_a.size(); i++){
	match.push_back(DMatch(i,i,0));
	f_a.push_back(Point2f(_a[i].x,_a[i].y));
	f_b.push_back(Point2f(_b[i].x,_b[i].y));
}
trf->estimateTransformation(_b,_a,match);
trf->applyTransformation(f_b,f_a);
Mat res;
trf->warpImage(src2,res);
imwrite("cc1.png",res);*/


/*vector<Point3f> eign1, eign2;
getShape(src1,cts1,&eign1);
getShape(src2,cts2,&eign2);

Point2f tri0[3], tri1[3], tri2[3];

tri0[0] = Point2f(128,128);
tri0[1] = tri0[0] + Point2f(1,0);
tri0[2] = tri0[0] + Point2f(0,1);

tri1[0] = Point2f(128,128);
tri1[1] = tri1[0] + Point2f(eign1[0].x,eign1[0].y);
tri1[2] = tri1[0] + Point2f(eign1[1].x,eign1[1].y);

tri2[0] = Point2f(128,128);
tri2[1] = tri2[0] + Point2f(eign2[0].x,eign2[0].y);
tri2[2] = tri2[0] + Point2f(eign2[1].x,eign2[1].y);

Mat ww21 = getAffineTransform(tri2,tri1);
Mat _res;
warpAffine(src2, _res, ww21, src1.size());
imwrite("cc1.png",_res);
meld(src1,_res,"cc2.png");*/

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
