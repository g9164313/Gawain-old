#include <sys/stat.h>
#include <global.hpp>
#include <zlib.h>
#include <vector>
#include <list>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include <opencv2/line_descriptor.hpp>
#include "opencv2/stitching/detail/blenders.hpp"
#include <opencv2/ximgproc.hpp>
#include <opencv2/xobjdetect.hpp>
#include <opencv2/xphoto.hpp>
#include <opencv2/xfeatures2d.hpp>
#include "opencv2/calib3d/calib3d.hpp"

using namespace cv;
using namespace xobjdetect;
using namespace ximgproc;
using namespace xfeatures2d;
using namespace line_descriptor;

extern char** listFileName(const char* path, const char* part, int* size);

void spot_spot(const char* name, Mat& img, int xx, int yy){

	const int ks = 9;

	Mat roi = img(Rect(
		xx - ks/2,
		yy - ks/2,
		ks, ks
	));

	Mat py1, py2;
	pyrDown(roi, py1);

	int _x = py1.cols/2;
	int _y = py1.rows/2;

	int val = py1.at<uint8_t>(_y,_x);

	val = 200;

	py1.at<uint8_t>(_y  , _x  ) = val;

	val = (val * 7) / 10;

	py1.at<uint8_t>(_y-1, _x  ) = val;
	py1.at<uint8_t>(_y+1, _x  ) = val;
	py1.at<uint8_t>(_y  , _x-1) = val;
	py1.at<uint8_t>(_y  , _x+1) = val;

	pyrUp(py1, py2, roi.size());
	//resize(py2, py2, roi.size());
	//cout<<py2<<endl<<"------------"<<endl;

	py2.copyTo(roi);
}


int main(int argc, char** argv){

	srand (time(NULL));

	//crop all ROI of images
	int cnt = 0;

	const char* SRC_PATH = "./wafer/raw/slice-ok";

	const char* DST_PATH1 = "./wafer/train/OK";
	const char* DST_PATH2 = "./wafer/train/NG";

	char** lst = listFileName(SRC_PATH, ".png", &cnt);

	char name[200];

	const int ROI_SIZE = 32;

	for(int i=0; i<cnt-10; i++){

		Mat img, roi;

		sprintf(name, "%s/%s", SRC_PATH, lst[i]);
		img = imread(name, IMREAD_GRAYSCALE);

		roi = img(Rect(
			(img.cols-139)/2, (img.rows-139)/2,
			139, 139
		));

		sprintf(name, "%s/%s", DST_PATH1, lst[i]);
		imwrite(name,roi);

		//-------------------------//

		for(int j=0; j<200; j++){

			sprintf(name, "%s/%s", SRC_PATH, lst[i]);
			img = imread(name, IMREAD_GRAYSCALE);

			roi = img(Rect(
				(img.cols-139)/2, (img.rows-139)/2,
				139, 139
			));

			int _w = (roi.cols * 8 )/ 10;
			int ox = (roi.cols - _w)/ 2;
			int xx = rand() % _w + ox;

			int _h = (roi.rows * 8 )/ 10;
			int oy = (roi.rows - _h)/ 2;
			int yy = rand() % _h + oy;

			spot_spot(name, roi, xx, yy);

			sprintf(name, "%s/%X_%s", DST_PATH2, j, lst[i]);
			imwrite(name, roi);

			img = imread("",IMREAD_GRAYSCALE);//Is it bug???
		}
		cout<<"process "<<lst[i]<<endl;
	}

	return 0;
}
//-----------------------------------------------//


Scalar color[]={
	Scalar(0,0,250),
	Scalar(0,250,0),
	Scalar(250,0,0),
	Scalar(255,255,0),
	Scalar(0,255,255),
	Scalar(255,0,255),
};

const float MIN_DIFF_X = 30.f;

bool ladder_by_horizontal(Point2f a, Point2f b){
	if(abs(a.x-b.x)>MIN_DIFF_X){
		if(a.x>b.x){
			return false;
		}
		return true;
	}
	if(a.y>b.y){
		return false;
	}
	return true;
}

Point2f findShortest(
	Point2f& aa,
	Point2f& bb,
	vector<Point2f>& set
){
	int idx = -1;
	double max = -1;
	Point2f cc;
	for(int i=0; i<set.size(); i++){
		cc = set[i];
		double dist = norm(cc-bb);
		if(dist<max || idx==-1){
			idx = i;
			max = dist;
		}
	}
	cc = set[idx];
	double diff = norm(bb-aa) + norm(cc-bb) - norm(cc-aa);
	if(diff<1){
		//cout<<"diff="<<diff<<endl;
		return cc;
	}
	return Point2f(-1,-1);
}

bool intersection(
	Point2f o1, Point2f p1,
	Point2f o2, Point2f p2,
	Point2f &r
){
    Point2f x = o2 - o1;
    Point2f d1 = p1 - o1;
    Point2f d2 = p2 - o2;

    float cross = d1.x*d2.y - d1.y*d2.x;
    if (abs(cross) < /*EPS*/1e-8)
        return false;

    double t1 = (x.x * d2.y - x.y * d2.x)/cross;
    r = o1 + d1 * t1;
    return true;
}

int main1(int argc, char** argv){

	const char* name = "ipc-1.bmp";
	//const char* name = "undist.png";
	const int RECT_SIZE = 18;
	const int RECT_AREA = RECT_SIZE*RECT_SIZE;

	Mat ova = imread(name, IMREAD_COLOR);
	Mat img = imread(name, IMREAD_GRAYSCALE);
	Mat tmp = Mat::zeros(img.size(),CV_8UC1);
	Mat res = Mat::zeros(img.size(),CV_8UC1);
	Mat kern = getStructuringElement(MORPH_RECT,Size(RECT_SIZE,RECT_SIZE));

	medianBlur(img,tmp,5);
	morphologyEx(tmp,res,MORPH_OPEN,kern);
	//double min,max;
	//minMaxLoc(res,&min,&max);
	threshold(res,tmp,100,255,THRESH_BINARY);
	//addWeighted(img,0.3,tmp,0.7,0.,res);

	//gather all points
	vector<Point2f> pool;
	vector<vector<Point> > cts;
	findContours(tmp,cts,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
	for(int i=0; i<cts.size(); i++){
		double area = contourArea(cts[i]);
		if(area<RECT_AREA){
			continue;
		}
		Point2f center;
		float radius;
		minEnclosingCircle(cts[i],center,radius);
		//circle(ova,center,radius,color[5]);
		pool.push_back(center);
	}
	sort(pool.begin(), pool.end(), ladder_by_horizontal);

	//link all point along horizontal direction
	vector<vector<Point2f> > group;
	for(int i=0; i<pool.size(); i++){
		Point2f& aa = pool[i];
		vector<Point2f> segment;
		segment.push_back(aa);
		for(int j=i+1; i<pool.size(); j++){
			Point2f& bb = pool[j];
			if(abs(aa.x-bb.x)>(MIN_DIFF_X*1.5)){
				break;
			}
			segment.push_back(bb);
			i+=1;
		}
		group.push_back(segment);
	}

	//start to travel, find points which align in one direction
	vector<Point2f> path;
	vector<Point2f>& seed = group[0];
	for(int i=0; i<seed.size(); i++){
		Point2f aa, bb, cc;
		aa = bb = seed[i];
		path.push_back(aa);
		for(int j=2; j<group.size(); j+=2){
			cc = findShortest(aa,bb,group[j]);
			if(cc.x<0){
				path.clear();
				break;
			}
			path.push_back(cc);
			aa = bb;
			bb = cc;
		}
		if(path.size()!=0){
			break;//we only need the first path....
		}
	}

	if(path.size()==0){
		cout<<"we failed...."<<endl;
		return -1;
	}

	//draw points along line
	/*for(int i=0; i<(path.size()-1); i++){
		Point2f& aa = path[i+0];
		Point2f& bb = path[i+1];
		arrowedLine(ova,aa,bb,color[i%3],2);
	}
	imwrite("cc1.png",ova);*/

	vector<vector<Point3f> > objpt(1);
	vector<vector<Point2f> > imgpt(1);

	//calculate statistic
	double avg_gap=0., dev_gap=0.;
	double cnt_gap = path.size()-1;
	for(int i=0; i<path.size()-1; i++){
		double gap = norm(path[i]-path[i+1]);
		avg_gap = avg_gap + gap;
		printf("%.2f,  %.2f; ...\n", path[i].x, path[i].y);
	}
	avg_gap = avg_gap / cnt_gap;

	for(int i=0; i<path.size()-1; i++){
		double gap = norm(path[i]-path[i+1]);
		dev_gap = dev_gap + (gap -avg_gap) * (gap -avg_gap);
	}
	dev_gap = dev_gap / cnt_gap;
	dev_gap = sqrt(dev_gap);

	cout<<"AVG="<<avg_gap<<endl;
	cout<<"DEV="<<dev_gap<<endl<<endl;

	//fitting-line~~~
	Vec4f accu;
	fitLine(path,accu,CV_DIST_L1, 0, 0.1, 0.01);


	double slope1 = accu[1] / accu[0];

	Point2f base_o1(0       , accu[3] - (slope1 * (accu[2]-0       )));
	Point2f base_p1(img.cols, accu[3] - (slope1 * (accu[2]-img.cols)));

	double slope2 = -accu[0] / accu[1];

	for(int i=0; i<path.size(); i++){

		Point2f cross;

		Point2f path_o1(path[i].x - ((path[i].y-0       )/slope2), 0       );
		Point2f path_p1(path[i].x - ((path[i].y-img.rows)/slope2), img.rows);

		intersection(
			base_o1, base_p1,
			path_o1, path_p1,
			cross
		);

		Point3f aa;
		aa.x = cross.x;
		aa.y = cross.y;
		aa.z = 0;

		Point2f bb;
		bb.x = path[i].x;
		bb.y = path[i].y;

		objpt[0].push_back(aa);
		imgpt[0].push_back(bb);

		//circle(ova,Point2f(aa.x,aa.y),6,color[0],-1);
		//circle(ova,bb,3,color[1],-1);
		//line(ova, path_o1, path_p1, color[1]);
		//line(ova, base_o1, base_p1, color[0]);
		//circle(ova, Point2f(accu[2],accu[3]),6,color[0],-1);
		circle(ova, Point2f(aa.x,aa.y),6,color[0],-1);
		circle(ova, bb,3,color[1],-1);
	}
	imwrite("cc3.png",ova);

	Mat cameCoeffs;// = Mat::zeros(3,3,CV_32FC1);
	Mat distCoeffs;
	vector<Mat> rvecs, tvecs;

	/*cameCoeffs.at<float>(0,0) = 500000;
	cameCoeffs.at<float>(0,2) = img.size().width/2;
	cameCoeffs.at<float>(1,1) = 500000;
	cameCoeffs.at<float>(1,2) = img.size().height/2;
	cameCoeffs.at<float>(2,2) = 1.;*/

	double err = calibrateCamera(
		objpt, imgpt, img.size(),
		cameCoeffs, distCoeffs,
		rvecs, tvecs,
		CV_CALIB_ZERO_TANGENT_DIST
	);
	cout<<cameCoeffs<<endl<<endl;
	cout<<distCoeffs<<endl<<endl;

	double k1 = distCoeffs.at<double>(0,0);
	cout<<"c="<<k1*img.cols/2<<endl;

	Mat bbb;
	undistort(img,bbb,cameCoeffs,distCoeffs);
	imwrite("undist.png",bbb);

	FileStorage fs("cam.yml",FileStorage::WRITE);
	fs<<"camCoeffs"<<cameCoeffs;
	fs<<"distCoeffs"<<distCoeffs;
	fs.release();

	//Mat ccc = Mat::zeros(img.size(),CV_8UC1);
	//addWeighted(img,0.5, bbb,0.5, 0.0,ccc);
	//imwrite("cc4.png",ccc);

	return 0;
}
//-----------------------------------------------//

int main2(int argc, char** argv){

	const char* src_name = "./solar/aa_01.jpg";
	const char* dst_name = "./solar/cc_01.tiff";

	Mat src = imread(src_name, IMREAD_GRAYSCALE);

	Mat camera;
	Mat distor;

	FileStorage fs("cam.yml",FileStorage::READ);
	fs["camCoeffs"] >> camera;
	fs["distCoeffs"] >> distor;

	cout<<"Coeff ="<<camera<<endl<<endl;
	cout<<"Distor="<<distor<<endl<<endl;

	Mat dst;
	undistort(src, dst, camera, distor);
	imwrite(dst_name, dst);

	return 0;
}

//-----------------------------------------------//

vector<Point2f> Points(vector<KeyPoint> keypoints)
{
    vector<Point2f> res;
    for(unsigned i = 0; i < keypoints.size(); i++) {
        res.push_back(keypoints[i].pt);
    }
    return res;
}

int main4(int argc, char** argv){

	Mat img1 = imread("aa3.jpg", IMREAD_GRAYSCALE);
	Mat img2 = imread("aa4.jpg", IMREAD_GRAYSCALE);
	//Mat img2 = imread("./cv_sample1/box_in_scene.png", IMREAD_GRAYSCALE);

	vector<KeyPoint> keypoints1,keypoints2;
	Mat descriptors1, descriptors2;

	Ptr<Feature2D> surf = SIFT::create();
	surf->detectAndCompute(img1, Mat(), keypoints1, descriptors1);
	surf->detectAndCompute(img2, Mat(), keypoints2, descriptors2);

	vector<DMatch> matches;
	Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create( "BruteForce" );
	matcher->match(descriptors1,descriptors2,matches);
	//FlannBasedMatcher matcher;
	//matcher.match(keypoints1,keypoints2,matches);


	vector<DMatch> mm;
	vector<Point2f> mp1, mp2;
	for(vector<DMatch>::iterator iter=matches.begin(); iter<matches.end(); ++iter) {
		DMatch& obj = (*iter);
		if(obj.distance<100){
			//cout<<"IDX="<<obj.queryIdx<<","<<obj.trainIdx<<" @ "<<obj.distance<<endl;
			mm.push_back(obj);
			mp1.push_back(keypoints1[obj.queryIdx].pt);
			mp2.push_back(keypoints2[obj.trainIdx].pt);
		}
	}

	Mat img_matches;
	drawMatches(img1,keypoints1, img2,keypoints2, mm, img_matches);
	imwrite("qq1.png",img_matches);

	Mat homography = findHomography(mp1,mp2,CV_RANSAC);
	cout<<homography<<endl;

	Mat t_img1;
	warpPerspective(img1,t_img1,homography,img2.size());
	//imwrite("qq2.png",t_img1);

	Mat dst;
	addWeighted(t_img1,0.7, img2,0.3, 0., dst);
	imwrite("qq3.png",dst);
	//Mat qq1;
	//drawKeypoints(img1,keypoints1,qq1);
	return 0;
}
//-------------------------------//


#include "opencv2/stitching.hpp"

using namespace cv::detail;

static double rateFrame(Mat& frame){

    unsigned long int sum = 0;
    unsigned long int size = frame.cols * frame.rows;
    Mat edges;
    cvtColor(frame, edges, CV_BGR2GRAY);
    GaussianBlur(edges, edges, Size(7, 7), 1.5, 1.5);
    imwrite("cc1.png",edges);

    Canny(edges, edges, 0, 30, 3);
    imwrite("cc2.png",edges);

    MatIterator_<uchar> it, end;
    for (
    	it = edges.begin<uchar>(), end = edges.end<uchar>();
    	it != end;
    	++it
	){
        sum += (*it != 0);
    }
    return (double) sum / (double) size;
}

int main3(int argc, char** argv){

	//Mat aa1 = imread("./aa1.jpg");
	Mat aa1 = imread("./artificial-1.png");
	Mat aa2 = imread("./aa2.jpg");
	Mat aa3 = imread("./aa2.jpg");

	double rate = rateFrame(aa1);
	cout<<"%%%"<<rate<<endl;

	Mat pano;
	vector<Mat> imgs;
	imgs.push_back(aa1);
	imgs.push_back(aa2);
	imgs.push_back(aa3);
	Stitcher stitcher = Stitcher::createDefault(false);
	Stitcher::Status status = stitcher.stitch(imgs, pano);

	imwrite("cc3.png",pano);
	//aa1.convertTo(aa1_s, CV_16SC3);
	//aa2.convertTo(aa2_s, CV_16SC3);

	Mat aa1_s, aa2_s, dst, dst_m;
	Mat msk1 = Mat::ones(aa1.size(), CV_8UC1);
	Mat msk2 = Mat::ones(aa2.size(), CV_8UC1);

	//FeatherBlender qq;
	MultiBandBlender qq;

	Mat edg;
	Canny(aa1,edg,500,1000);

	qq.prepare(Rect(0,0,aa1.cols,aa1.rows*2));
	//qq.feed(aa1_s,msk1,Point(0,0));
	//qq.feed(aa2_s,msk2,Point(0,1213));
	qq.feed(aa1,msk1,Point(0,0));
	qq.feed(aa2,msk2,Point(0,0));
	qq.blend(dst,dst_m);

	imwrite("cc3.png",dst);

	return 0;
}
//------------------------------//

