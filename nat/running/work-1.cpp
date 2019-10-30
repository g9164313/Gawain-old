/*
 * process_1.cpp
 *
 *  Created on: 2019年6月5日
 *      Author: qq
 */
#include <iostream>
#include <algorithm>
#include <vector>
#include <string>
#include <vision.hpp>

using namespace std;

Scalar cc[] = {
	Scalar(0, 3, 250, 178),
	Scalar(0, 250, 3, 178),
	Scalar(3,250,250, 178),
	Scalar(250,250,3, 178),
	Scalar(250,3,250, 178),
	Scalar(0,120,255, 178)
};

Mat valid_data(Mat& pool, Ptr<SVM>& ptr){
	size_t cnt = pool.cols * pool.rows;
	Mat data, mask;
	Mat text(cnt, 1, CV_32F);
	pool.reshape(1, cnt)
		.convertTo(data, CV_32FC3, 1./255., -128./255.);
	ptr->predict(data, text);
	text.reshape(1, pool.rows)
		.convertTo(mask, CV_8UC1, 255, 0.);
	//we mark front-ground, and train, so inverse it
	//threshold(mask, mask, 100., 255., THRESH_BINARY_INV);
	return mask;
}

Mat train_valid_data(Mat& pool, Mat& mask, double nuVal){
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
	cout<<"train data..."<<endl;
	Ptr<SVM> ptr = SVM::create();
	TICK_BEG;
	ptr->setType(SVM::ONE_CLASS);
	//ptr->setKernel(SVM::RBF);
	ptr->setKernel(SVM::LINEAR);
	ptr->setNu(nuVal);
	ptr->train(Mat(data).reshape(1), ml::ROW_SAMPLE, Mat());
	ptr->save("mask.xml");
	TICK_END;
	cout<<"valid data..."<<endl;
	return valid_data(pool,ptr);
}

void erase_blob(Mat& img, const vector<Point>& path){
	vector<vector<Point> > cts;
	cts.push_back(path);
	drawContours(img, cts, 0, Scalar::all(0), CV_FILLED);
}

Mat extract_mask(Mat& pool, Ptr<SVM>& ptr){
	Mat mask = valid_data(pool,ptr);
	//trick, do a border...
	rectangle(
		mask,
		Rect(0,0,mask.cols,mask.rows),
		Scalar::all(0), 2
	);
	//imwrite("mask-data.png",mask);
	//remove some noise~~~
	Mat kern = getStructuringElement(MORPH_ELLIPSE,Size(3,3));
	dilate(mask,mask,kern);
	erode(mask,mask,kern);
	//imwrite("cc2.png",mask);

	//remove some blob
	vector<vector<Point> > cts;
	findContours(
		mask, cts,
		RETR_EXTERNAL, CHAIN_APPROX_NONE
	);
	//Mat dbg = Mat::zeros(mask.size(), mask.type());
	//for(int i=0; i<cts.size(); i++){ drawContours(dbg, cts, i, Scalar::all(255)); }
	//imwrite("cc3.png",dbg);

	cts.erase(remove_if(
		cts.begin(), cts.end(),
		[&mask,&pool](const vector<Point>& path){
			double area = contourArea(path,false);
			if(area<1000.){
				erase_blob(mask,path);
				return true;//ignore little blob~~~
			}
			double peri = arcLength( path,false);
			if(peri<50.){
				erase_blob(mask, path);
				return true;//too short!!!
			}
			//cout<<"area/peri="<<area<<"/"<<peri<<"="<<(peri/area)<<endl;
			if((peri/area)<=0.3){
				erase_blob(mask, path);
				return true;//it may be a blob~~~
			}
			//vector<vector<Point> > tmp; tmp.push_back(path);
			//drawContours(pool, tmp, 0, Scalar::all(0));
			//imwrite("cc-x.png",pool);
			return false;
		}
	), cts.end());
	//Mat dbg = Mat::zeros(mask.size(), mask.type());
	//dbg = dbg * 0;
	//for(int i=0; i<cts.size(); i++){ drawContours(dbg, cts, i, Scalar::all(255)); }
	//imwrite("cc4.png",dbg);
	return mask;
}

bool check_endpoint(
	const Mat& data,
	const Point& point
){
	Mat blk = data(Rect(
		point.x-1, point.y-1,
		3, 3
	));
	int cnt = countNonZero(blk);
	if(cnt>3){
		return false;
	}
	if(cnt==3){
		uint8_t flg = 0;
		flg = flg | (blk.at<uint8_t>(0,0) & blk.at<uint8_t>(0,1));
		flg = flg | (blk.at<uint8_t>(0,1) & blk.at<uint8_t>(0,2));
		flg = flg | (blk.at<uint8_t>(0,2) & blk.at<uint8_t>(1,2));
		flg = flg | (blk.at<uint8_t>(1,2) & blk.at<uint8_t>(2,2));
		flg = flg | (blk.at<uint8_t>(2,1) & blk.at<uint8_t>(2,2));
		flg = flg | (blk.at<uint8_t>(2,0) & blk.at<uint8_t>(2,1));
		flg = flg | (blk.at<uint8_t>(1,0) & blk.at<uint8_t>(2,0));
		flg = flg | (blk.at<uint8_t>(0,0) & blk.at<uint8_t>(1,0));
		if(flg==0){
			return false;
		}
	}
	//cout<<"P:"<<point<<"-->"<<endl<<blk<<endl<<endl;
	return true;
}

/**
 * re-sort the sequence of dots.<p>
 */
vector<Point> dots2path(const Mat& data){
	vector<Point> path;
	if(data.empty()==true){
		return path;
	}
	vector<Point> dots;
	findNonZero(data, dots);//list from top to bottom, left to right.
	//step.1 find end-point, at least~~~
	auto it = dots.begin();
	do{
		if(check_endpoint(data, *it)==true){
			path.push_back(*it);
			dots.erase(it);
			break;
		}else{
			++it;
		}
	}while(it!=dots.end());
	//step.2 gather dots one by one
	do{
		Point& aa = *(path.end()-1);
		int dst = 2;
		for(auto dd=dots.begin(); dd!=dots.end(); ++dd){
			Point& bb = *dd;
			int dx = abs(aa.x - bb.x);
			int dy = abs(aa.y - bb.y);
			if((dx+dy)<=dst){
				it = dd;
				dst= dx + dy;
				if(dst==1){
					break;//trick, we find a neighborhood
				}
			}
		}
		path.push_back(*it);
		dots.erase(it);
	}while(dots.size()!=0);
	//step.3 judge which one is tail or head
	Point head, tail;
	head = *path.begin();
	tail = *(path.end()-1);
	if(head.x>tail.x){
		std::reverse(path.begin(), path.end());
	}
	return path;
}

vector<Point> findPath(
	const Mat& mask,
	const Point& mark
){
	//skeletonize track road~~~
	Mat node;
	thinning(mask,node);
	//imwrite("cc5.png",node);
	Mat txt;
	int cnt = connectedComponents(
		node, txt,
		8, CV_16U
	);
	//find a path, nearst mark~~~
	Mat tmp = Mat::zeros(txt.size(), CV_16U);
	Mat near_dot;
	double max_dist = DBL_MAX;
	Rect brd(1, 1, txt.cols-1, txt.rows-1);
	for(int i=1; i<cnt; i++){//component is one-index
		Mat dots, dmap, dist;
		tmp = i;
		//just compare with label, let label show
		compare(txt, tmp, dots, CMP_EQ);
		//trick, do a border...
		rectangle(dots, brd, Scalar::all(0), 3);
		//check whether point is in the path~~
		if(dots.at<uint8_t>(mark)!=0){
			max_dist = 0.;
			near_dot = dots;
			break;
		}
		//get nearest point from dot-clouds
		threshold(dots,dmap,1.,255.,THRESH_BINARY_INV);
		distanceTransform(dmap,dist,DIST_L2,3);
		double dval = dist.at<float>(mark);
		if(dval<max_dist){
			max_dist = dval;
			near_dot = dots;
		}
	}
	//imwrite("cc6.png",near_dot);
	return dots2path(near_dot);//gather all points to path~~~
}

double dist2line(
	const Point p0,
	const Point ln_p1,
	const Point ln_p2
){
	double x0 = p0.x;
	double y0 = p0.y;
	double x1 = ln_p1.x;
	double y1 = ln_p1.y;
	double x2 = ln_p2.x;
	double y2 = ln_p2.y;
	return
		abs((y2-y1)*x0 - (x2-x1)*y0 + x2*y1 - y2*x1) /
		hypot(y2-y1,x2-x1);
}

/**
 *list all way-points and return the next way-point.<p>
 */
Point findWaypoint(
	const Point& mark,
	vector<Point>& path
){
	if(path.size()==0){
		return mark;
	}
	approxPolyDP(path, path, 1.3, false);
	//step.1: find the nearest segment
	Point waypoint = mark;
	double min_dist = DBL_MAX;
	auto it = path.begin();
	for(; it!=(path.end()-1); ++it){
		Point pp = (*(it+0) + *(it+1))/2;
		double dist = cv::norm(pp-mark);
		if(dist<min_dist){
			waypoint = pp;
			min_dist = dist;
			it = path.erase(path.begin(), it);
		}
	}
	//step.2:check whether mark is on line
	it = path.begin();
	if(dist2line(mark, *it, *(it+1))<=10){
		path.erase(it);
	}else{
		path.erase(it);
		path.insert(it, waypoint);
	}
	if(path.size()==0){
		return mark;//we reach the end!!!
	}
	return path[0];
}

///~~~~test unit~~~~

int main(int argc, char* argv[]) {
	Mat img = imread("ggyy-2.png");
	Ptr<SVM> svm = SVM::load("mask.xml");
	Mat msk = extract_mask(img,svm);

	//Point mrk(51, 417);
	Point mrk(61, 446);

	vector<Point> path = findPath(msk, mrk);
	Point wapt = findWaypoint(mrk, path);

	circle(img, mrk, 5, cc[0], 2);
	for(int i=0; i<path.size(); i++){
		circle(img, path[i], 3, cc[2]);
	}
	arrowedLine(img, mrk, wapt, cc[3], 1);
	imwrite("cc9.png",img);

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




