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

using namespace cv;
using namespace xobjdetect;
using namespace ximgproc;
using namespace xfeatures2d;
using namespace line_descriptor;

Mat filterVariance(Mat& src, int rad){
	Size ksize(rad,rad);
	Mat nod, avg, mu2, sig;
	src.convertTo(nod,CV_32FC1);
	blur(nod, avg, ksize);
	blur(nod.mul(nod),mu2,ksize);
	sqrt(mu2-avg.mul(avg),sig);
	normalize(sig, nod, 0, 255, NORM_MINMAX,CV_8UC1);
	return nod;
}

Scalar color[]={
	Scalar(0,0,250),
	Scalar(0,250,0),
	Scalar(250,0,0),
	Scalar(255,255,0),
	Scalar(0,255,255),
	Scalar(255,0,255),
};

const float MIN_DIFF_X = 10.f;

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

int main(int argc, char** argv){

	const char* name = "ipc-2.bmp";
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

	//fitting path....
	Vec4f base;
	fitLine(path,base,CV_DIST_FAIR, 0, 2, 0.01);
	double s1 = base[1]/base[0];
	double s2 = s1*s1;
	for(int i=0; i<path.size(); i++){
		Point2f a(base[2],base[3]);
		Point2f b(path[i]);
		path[i].x = (s2 * a.x - s1*(a.y-b.y) + b.x) / (1. + s2);
		path[i].y = (s2 * b.y - s1*(a.x-b.x) + a.y) / (1. + s2);
		circle(ova,path[i],5,color[0],-1);
	}

	//dump path~~~
	cout<<"dist=[";
	for(int i=0; i<(path.size()-1); i++){
		Point2f& aa = path[i+0];
		Point2f& bb = path[i+1];
		double dist = norm(aa-bb);
		printf("%.3f;..\n",dist);
	}
	cout<<"];"<<endl;

	//draw points along line
	//for(int i=0; i<(path.size()-1); i++){
	//	Point2f& aa = path[i+0];
	//	Point2f& bb = path[i+1];
	//	arrowedLine(ova,aa,bb,color[i%3],2);
	//}
	//imwrite("result-3.png",ova(Rect(1000,0,2000,500)));
	imwrite("cc1.png",ova);
	return 0;
}
//-----------------------------------------------//

int main9(int argc, char** argv){
	Mat img = imread("sputter-1.png", IMREAD_GRAYSCALE);
	/*Ptr<BinaryDescriptor> bd = BinaryDescriptor::createBinaryDescriptor();
	vector<KeyLine> keylines;
	bd->detect(img,keylines);
	for(size_t i=0; i<keylines.size(); i++){
		cout<< "(" << keylines[i].startPointX << "," << keylines[i].startPointY << ")-(" <<
				keylines[i].endPointX << "," << keylines[i].endPointY << ")" <<endl;
	}
	Mat descriptors;
	bd->compute(img, keylines, descriptors);*/
	imwrite("tttt.pnm",img);
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

int main8(int argc, char** argv){

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

int main7(int argc, char* argv[]) {
	//test writing speed
	for(int i=1;i<=5; i++){
		long width = (1<<(i+10));
		void* buf = malloc(width*width);
		Mat img(width,width,CV_8UC1,buf);
		img = img * 0;
		//randu(img,Scalar(0),Scalar(255));
		char name[60];
		sprintf(name,"volbin-%d.tif",i);
		cout<<"write "<<name<<", size="<<(width*width)<<"bytes."<<endl;

		TICK_BEG
		imwrite(name,img);
		TICK_END("I/O")
		free(buf);
		cout<<endl;
	}
	return 0;
}
//-------------------------------//

int main6(int argc, char* argv[]) {

	long width = (1<<15);
	long height= (1<<15);
	void* buf = malloc(width*height);

	double accumTick = 0.;

	const int TEST_ROUND=5;

	for(int i=0; i<TEST_ROUND; i++){

		Mat img(height,width,CV_8UC1,buf);

		randu(img,Scalar(0),Scalar(255));

		/*long cnt;
		TICK_BEG
		threshold(img,img,200.,0.,THRESH_TOZERO);
		cnt = countNonZero(img);
		long size = (width*height)/1000000;
		cout<<"size="<<width<<"x"<<height<<"="<<size<<"MByte"<<endl;
		cout<<"result="<<cnt<<endl;
		TICK_END2("count",accumTick)*/

		//TICK_BEG
		//vector<uchar> buf(512*1024*1024);
		/*vector<uchar> buf;
		buf.reserve(512*1024*1024);
		imencode(".tiff",img,buf);
		long size = buf.size()/1000000;
		cout<<"compressed size:"<<size<<"MByte"<<endl;
		buf.clear();*/
		//TICK_END("encode")

		cout<<endl;
	}

	accumTick = accumTick/TEST_ROUND;

	cout<<endl<<"average time="<<accumTick<<"sec"<<endl;

	free(buf);
	//Mat ref = imread("qq0.png",IMREAD_GRAYSCALE);
	//Mat src = imread("qq3.png",IMREAD_GRAYSCALE);
	//TICK_BEG
	//registration(ref,src);
	//TICK_END("regist")
	//imwrite("aa1.png",ref);
	//imwrite("aa2.png",src);
	//imposition("aa3.png",ref,src);
	return 0;
}

//--------------------------------------------//

//------------------------------//

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

int main4(int argc, char** argv){

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

