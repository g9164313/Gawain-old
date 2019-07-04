/*
 * process_1.cpp
 *
 *  Created on: 2019年6月5日
 *      Author: qq
 */
#include <iostream>
#include <algorithm>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include <opencv2/ml.hpp>
using namespace std;
using namespace cv;
using namespace ml;

#include <opencv2/features2d.hpp>
#include <opencv2/xfeatures2d.hpp>
#include <opencv2/xobjdetect.hpp>
#include <opencv2/line_descriptor.hpp>
#include <opencv2/ccalib/randpattern.hpp>
using namespace xfeatures2d;
using namespace xobjdetect;
using namespace randpattern;
using namespace line_descriptor;

extern Rect txt2rect(const char* txt);

double norm_number(double val, double hypt){
	bool _p = (val>0)?(true):(false);
	double _v = abs(val / hypt);
	if(_v<=0.2){
		return 0.;
	}else{
		val = 1.;
	}
	return (_p)?(val):(-1.*val);
}

int main(int argc, char* argv[]) {

	RNG rng(33582);

	Mat img = imread("ggyy2.jpg");

	Point aa[] = {
		Point(269,222),
		Point(307,291),
		Point(366,371),
		Point(365,395),
		Point(362,417),
	};
	Point bb[] = {
		Point(),
		Point(),
		Point(),
		Point(),
		Point(),
	};
	for(int i=0; i<5; i++){
		bb[i].x = 374 + rng.uniform(1, 30);
		bb[i].y = 216 + rng.uniform(1, 30);
	}

	Mat dat(10,3,CV_32F);
	Mat txt;
	for(int i=0; i<5; i++){
		Vec3b pix = img.at<Vec3b>(aa[i]);
		Vec3f val(pix);
		val = val / 255.;
		dat.at<Vec3f>(i) = val;
		txt.push_back(255);
	}
	for(int i=0; i<5; i++){
		Vec3b pix = img.at<Vec3b>(bb[i]);
		Vec3f val(pix);
		val = val / 255.;
		dat.at<Vec3f>(i+5) = val;
		txt.push_back(0);
	}
	cout<<dat<<endl;

	Ptr<SVM> ptr = SVM::create();
	ptr->setType(SVM::C_SVC);
	ptr->setKernel(SVM::RBF);
	ptr->setTermCriteria(cvTermCriteria(CV_TERMCRIT_ITER, 500, FLT_EPSILON));
	ptr->train(dat, ml::ROW_SAMPLE, txt);

	for(int y=137; y<495; y++){
		for(int x=188; x<495; x++){
			Vec3b pix = img.at<Vec3b>(Point(x,y));
			Vec3f val(pix);
			val = val / 255.;
			Mat sample(1,3,CV_32F);
			Mat result(1,1,CV_32F);
			sample.at<Vec3f>(0) = val;
			ptr->predict(sample, result);
			int rest = result.at<float>(0);
			if(rest!=0){
				img.at<Vec3b>(Point(x,y)) = Vec3b(0,255,0);
			}
		}
	}
	imwrite("cc1.png", img);
	return 0;
}

int main2(int argc, char* argv[]) {
	if(argc!=11){
		return -1;
	}
	Rect roi = txt2rect(argv[1]);
	Mat view[] = {
		imread(argv[2]),
		imread(argv[3]),
		imread(argv[4]),
		imread(argv[5]),
		imread(argv[6]),
		imread(argv[7]),
		imread(argv[8]),
		imread(argv[9]),
		imread(argv[10]),
	};

	vector<KeyPoint> kps[9];
	Mat des[9];
	{
		Ptr<SIFT> ptr = SIFT::create();
		for(int i=0; i<9; i++){
			Mat img, msk;
			medianBlur(view[i],img,11);
			msk = Mat::zeros(img.size(),CV_8UC1);
			if(i==0){
				rectangle(msk, roi, Scalar::all(255), -1);
			}else{
				Rect zone(
					roi.x - roi.width,
					roi.y - roi.height,
					roi.width *3,
					roi.height*3
				);
				rectangle(msk, zone, Scalar::all(255), -1);
			}
			ptr->detectAndCompute(img, msk, kps[i], des[i]);
//#define SHOW_1
#ifdef SHOW_1
			Mat node;
			drawKeypoints(view[i], kps[i], node);
			imshow("dump...", node);
			waitKey(0);
#endif
		}
	}
	vector<Point2f> plan1, plan2;
	{
		vector<DMatch> pair;
		Ptr<BFMatcher> ptr = BFMatcher::create(NORM_L2, true);

		for(int i=1; i<9; i++){

			ptr->match(des[i],des[0], pair);

			for(int j=0; j<pair.size(); j++){
				DMatch& mm = pair[j];
				KeyPoint& k1 = kps[i][mm.queryIdx];
				KeyPoint& k2 = kps[0][mm.trainIdx];

				Point2f dff = k1.pt - k2.pt;
				plan1.push_back(dff);

				double hyp = hypot(dff.x,dff.y);

				Point2f vec;
				vec.x = norm_number(dff.x,hyp) * 20.;
				vec.y = norm_number(dff.y,hyp) * 20.;
				plan2.push_back(vec);
				//cout<<dff<<"==="<<vec<<endl;
			}

//#define SHOW_2
#ifdef SHOW_2
			circle(view[i],loca,5,Scalar(0,200,0),-1);
			Mat node;
			drawKeypoints(view[i], kps[i], node);
			//imwrite("cc1.png",node);
			imshow("dump...", node);
			waitKey(0);
#endif
#define SHOW_3
#ifdef SHOW_3
			for(int j=1; j<pair.size(); j++){
				DMatch& mm = pair[j];
				KeyPoint& p1 = kps[i][mm.queryIdx];
				KeyPoint& p2 = kps[0][mm.trainIdx];
				double res = norm(p1.pt - p2.pt);
			}
			Mat node;
			drawMatches(
				view[i], kps[i],
				view[0], kps[0],
				pair,
				node
			);
			imwrite("cc2.png",node);
			imshow("dump...", node);
			waitKey(0);
#endif
		}
	}

	Mat hh = findHomography(plan1, plan2);
	//cout<<hh<<endl;
//#define SHOW_4
#ifdef SHOW_4
	{
		Mat node;
		warpPerspective(view[0], node, hh, view[0].size());
		imwrite("cc1.png",node);
	}
#endif
	return 0;
}


/*
	Point2f p0(355,317);

	Point2f pp[] ={
		p0,

		Point2f(354,251) - p0,
		Point2f(440,314) - p0,
		Point2f(355,390) - p0,
		Point2f(270,320) - p0,

		Point2f(271,255) - p0,
		Point2f(433,250) - p0,
		Point2f(443,386) - p0,
		Point2f(261,393) - p0,
	};

	Point2f gg[] ={
		p0,

		Point2f(0,-20),
		Point2f(20, 0),
		Point2f(0, 20),
		Point2f(-20,0),

		Point2f(-20,-20),
		Point2f( 20,-20),
		Point2f( 20, 20),
		Point2f(-20, 20)
	};

	vector<Point2f> aa;
	vector<Point2f> bb;

	aa.push_back(pp[1]); bb.push_back(gg[1]);
	aa.push_back(pp[2]); bb.push_back(gg[2]);
	aa.push_back(pp[3]); bb.push_back(gg[3]);
	aa.push_back(pp[4]); bb.push_back(gg[4]);
	aa.push_back(pp[5]); bb.push_back(gg[5]);
	aa.push_back(pp[6]); bb.push_back(gg[6]);
	aa.push_back(pp[7]); bb.push_back(gg[7]);
	aa.push_back(pp[8]); bb.push_back(gg[8]);

	Mat hh = findHomography(aa, bb);
	cout<<hh<<endl;

	cout<<type2str(hh.type())<<endl<<endl;

	Mat cc(3,1,CV_64FC1);
	cc.at<double>(0,0) = 350. - p0.x;
	cc.at<double>(1,0) = 250. - p0.y;
	cc.at<double>(2,0) = 1.;
	Mat res = hh*cc;
	cout<<res<<endl;

	//Mat img1 = imread("check-WW.png");
	//Mat img2;
	///warpPerspective(img1, img2, hh, img1.size());
	//imwrite("cc3.png",img2);

	//PIPE_PREP("/home/qq/.gawain/conf.properties");

	//do{
		//PIPE_OPEN;
		//Mat img(height, width, cvtype, smem);
		//Mat gray;
		//cvtColor(img, gray, COLOR_BGR2GRAY);
		//Mat ova(height, width, CV_8UC4, over);
		//ova = ova * 0;

		//Mat gray = imread("./chessboard.png");
		//Mat gray = imread("./ggyy.png");

		//line(img,Point(800,0),Point(0,600),Scalar(100,100,0,200),10);
		//pixel is 'BGRA'

		//PIPE_CLOSE;

	//}while(true);

	//vector<KeyLine> res;
	//Ptr<BinaryDescriptor> ptr = BinaryDescriptor::createBinaryDescriptor();
	//ptr->detect(src,res);
	//drawKeylines(src,res,dst);
	//ptr->clear();

	//vector<KeyPoint> pts;
	//Ptr<Feature2D> feat = SIFT::create();
	//feat->detect(src, pts);
	//Mat dst;
	//drawKeypoints(src,pts,dst);

	//imwrite("./cc1.png",dst);
 */
