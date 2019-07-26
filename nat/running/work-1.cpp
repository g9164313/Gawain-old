/*
 * process_1.cpp
 *
 *  Created on: 2019年6月5日
 *      Author: qq
 */
#include <iostream>
#include <algorithm>
#include <vision.hpp>

Scalar cc[] = {
	Scalar(0, 3, 250, 178),
	Scalar(0, 250, 3, 178),
	Scalar(3,250,250, 178),
	Scalar(250,250,3, 178)
};

void train_mask(Mat& pool, Mat& mask){
	Mat node;
	pool.convertTo(node, CV_32F, 1./255., -128./255.);
	vector<Vec3f> data;
	for(int y=0; y<node.rows; y++){
		for(int x=0; x<node.cols; x++){
			if(mask.at<uint8_t>(y,x)!=0){
				continue;
			}
			data.push_back(node.at<Vec3f>(y,x));
		}
	}
	cout<<"prepare data..."<<endl<<endl;
	Ptr<SVM> ptr = SVM::create();
	TICK_BEG;
	ptr->setType(SVM::ONE_CLASS);
	ptr->setKernel(SVM::RBF);
	//ptr->setKernel(SVM::LINEAR);
	ptr->setNu(0.00003);
	ptr->train(Mat(data).reshape(1), ml::ROW_SAMPLE, Mat());
	ptr->save("mask.xml");
	TICK_END;
}

Mat extract_mask(Mat& pool, Ptr<SVM>& ptr, bool showMask){
	size_t cnt = pool.cols * pool.rows;
	Mat dat, msk;
	Mat txt(cnt, 1, CV_32F);
	pool.reshape(1, cnt)
		.convertTo(dat, CV_32FC3, 1./255., -128./255.);
	ptr->predict(dat, txt);
	txt.reshape(1, pool.rows)
		.convertTo(msk, CV_8UC1, 200, 0.);
	//we mark frontground, and train, so inverse it
	threshold(msk, msk, 100., 255., THRESH_BINARY_INV);
	Mat krn = getStructuringElement(MORPH_ELLIPSE,Size(3,3));
	erode(msk,msk,krn);
	dilate(msk,msk,krn);
	/*Ptr<BinaryDescriptor> det = BinaryDescriptor::createBinaryDescriptor();
	vector<KeyLine> lines;
	det->detect(msk, lines);
	Mat tmp = Mat::zeros(msk.size(), CV_8UC1);
	for(KeyLine kl:lines){
		Point aa(kl.startPointX, kl.startPointY);
		Point bb(kl.endPointX, kl.endPointY);
		line(tmp, aa, bb, Scalar(255),6);
	}
	bitwise_and(msk,tmp,msk);
	imwrite("cc1.png",msk);
	Mat temp;
	cvtColor(msk,temp,COLOR_GRAY2BGR);
	drawKeylines(temp,lines,temp);
	imwrite("cc1.png",temp);*/
	//skeletonize track road~~~
	if(showMask==false){
		thinning(msk,msk);
	}
	return msk;
}

Point extract_path(Mat& msk, Point& mrk, vector<Point>& path){
	path.clear();
	//find all contours~~~
	vector<vector<Point> > cts;
	findContours(msk,cts,RETR_EXTERNAL,CHAIN_APPROX_NONE);
	if(cts.size()==0){
		return mrk;
	}
	//find a longest track road...
	vector<Point> road = *std::max_element(
		cts.begin(), cts.end(),
		[](const vector<Point>& aa, const vector<Point>& bb){
			double _a = arcLength(aa,false);
			double _b = arcLength(bb,false);
			return (_a<_b);
		}
	);
	if(road.size()<=50){
		return mrk;
	}
	approxPolyDP(road, path, 2, false);
	return *std::min_element(
		road.begin(), road.end(),
		[&mrk](const Point& aa, const Point& bb){
			double a = norm(aa-mrk);
			double b = norm(bb-mrk);
			return (a<b);
		}
	);
}

int main(int argc, char* argv[]) {
	Mat img = imread("img.png");

	Point mrk(370,409); circle(img, mrk, 3, cc[1]);

	Ptr<SVM> svm = SVM::load("mark.xml");

	Mat msk = extract_mask(img,svm,false);
	imwrite("cc1.png",msk);

	vector<Point> pts;
	Point wp1 = extract_path(msk, mrk, pts);
	circle(img, wp1, 3, cc[0]);
	for(int i=0; i<pts.size()/2; i++){
		circle(img, pts[i], 3, cc[0]);
		arrowedLine(img, pts[i], pts[i+1], cc[1], 1);
	}
	imwrite("cc2.png",img);

	cout<<"done!!"<<endl;
	return 0;
}



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

int main2(int argc, char* argv[]) {
	if(argc!=11){
		return -1;
	}
	Rect roi;
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
