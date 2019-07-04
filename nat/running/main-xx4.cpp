#include <fstream>
#include <iostream>
#include <algorithm>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include <vision/pipeImage.hpp>
using namespace std;
using namespace cv;

#include <opencv2/features2d.hpp>
#include <opencv2/xfeatures2d.hpp>
#include <opencv2/xobjdetect.hpp>
#include <opencv2/line_descriptor.hpp>
using namespace xfeatures2d;
using namespace xobjdetect;
using namespace line_descriptor;

extern string type2str(int type);

int main(int argc, char* argv[]) {

	Mat img1 = imread("./check-OO.png");
	Mat img2 = imread("./check-NE.png");

	Mat img3, img4;
	GaussianBlur(img1, img3, Size(11,11), 2.3);
	GaussianBlur(img2, img4, Size(11,11), 2.3);

	Mat mask = Mat::zeros(img3.size(),CV_8UC1);
	rectangle(mask,Rect(0,0,700,600),Scalar(255),-1);

	Mat desc1, desc2;
	vector<KeyPoint> kps1, kps2;
	Ptr<SIFT> ptr = SIFT::create();
	//ptr->detect(img1, kps1, desc1);
	//ptr->detect(img2, kps2, desc2);
	ptr->detectAndCompute(img3, mask, kps1, desc1);
	ptr->detectAndCompute(img4, mask, kps2, desc2);
	cout<<"keypoints="<<kps1.size()<<","<<kps2.size()<<endl;

	vector<DMatch> pair;
	BFMatcher::create(NORM_L2, true)->match(desc1,desc2,pair);
	cout<<"pairs="<<pair.size()<<endl;

	for(int i=0; i<pair.size(); i++){
		DMatch& mm = pair[i];
		printf("%2d) [%2d][%2d] d=%.3f\n",
			i,
			mm.queryIdx,
			mm.trainIdx,
			mm.distance
		);
	}
	cout<<endl;

	Mat node;
	drawMatches(
		img1, kps1,
		img2, kps2,
		pair,
		node
	);
	//drawKeypoints(img2,kps2,img2);
	imwrite("cc1.png",node);

	return 0;
}

int main2(int argc, char* argv[]) {

	Ptr<WBDetector> det = WBDetector::create();
#define TRAIN
#ifdef TRAIN
	RNG rng(13579);

	/*Mat img1 = imread("./check-OO.png");
	Rect roi = Rect(296,266,24*5,24*5);
	for(int i=0; i<10; i++){
		Mat tmp;
		int ss = rng.uniform(1,4)*2+1;
		float sigma = rng.uniform(0.1,2.);
		GaussianBlur(img1(roi),tmp,Size(ss,ss),sigma);
		char name[80];
		sprintf(name,"./object-pos/%03d.png",i);
		imwrite(name, tmp);
	}
	Mat img2 = imread("./check-bng.png");
	for(int i=0; i<10; i++){
		Mat tmp;
		int ss = rng.uniform(1,4)*2+1;
		float sigma = rng.uniform(0.1,2.);
		GaussianBlur(img2,tmp,Size(ss,ss),sigma);
		char name[80];
		sprintf(name,"./object-neg/%03d.png",i);
		imwrite(name, tmp);
	}*/

	det->train("./object-pos","./object-neg");
	FileStorage fs("ggyy.xml", FileStorage::WRITE);
	fs << "target";
	det->write(fs);

#else
	FileStorage fs("ggyy.xml", FileStorage::READ);
	det->read(fs.getFirstTopLevelNode());

	Mat img2 = imread("./check-EE.png");
	vector<Rect> bboxes;
	vector<double> confidences;
	det->detect(img2,bboxes,confidences);

    for (size_t i = 0; i < bboxes.size(); ++i) {
    	Rect& rr = bboxes[i];
    	char txt[80];
    	sprintf(txt,"conf=%.2f",confidences[i]);
    	putText(img2,txt,
    		Point(rr.x, rr.y+30),
			FONT_HERSHEY_COMPLEX, 6,
			Scalar(128,128,0)
		);
        rectangle(img2, rr, Scalar(255, 0, 0));
        cout<<"box-confidences="<<confidences[i]<<endl;
    }
    imwrite("cc1.png",img2);
#endif

	return 0;
}
//-----------------------------------//


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

	float e0 = hypot(vtx[0].x, vtx[0].y);
	float e1 = hypot(vtx[1].x, vtx[1].y);
	float e2 = hypot(vtx[2].x, vtx[2].y);

	if(!(e1<e0 && e1<e2)){
		//the first vertex is bottom-right.
		angle = angle + 90.f;
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

	double a_tar = getAngle(r_tar);
	double a_ref = getAngle(r_ref);

	//adjust rotation
	Mat h1 = getRotationMatrix2D(r_ref.center,a_ref-a_tar,1.);
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
	matchTemplate(
		m_tar, m_ref,
		result,
		CV_TM_CCORR
	);//TODO: mask for speed???

	Point maxLoc;

	minMaxLoc(result, NULL, NULL, NULL, &maxLoc, Mat());

	h2.at<double>(0,2) = maxLoc.x;
	h2.at<double>(1,2) = maxLoc.y;
	transform(refer, refer, h2);
}

Scalar getMoment(const Mat& img, const Mat& msk){

	Scalar result;

	double area;

	Mat img1, img2, img3, img4;

	img.convertTo(img1,CV_64FC1);

	if(msk.empty()==false){
		img1 = img1 * msk;
		area = countNonZero(msk);
	}else{
		area = img.cols * img.rows;
	}

	//blow lines refer to ImageJ(https://github.com/imagej/imagej1/blob/master/ij/process/FloatStatistics.java)
	Scalar avg,dev;
	meanStdDev(img1,avg,dev,msk);

	pow(img1, 2., img2);
	pow(img1, 3., img3);
	pow(img1, 4., img4);

	float sum2 = sum(img2)[0];
	float sum3 = sum(img3)[0];
	float sum4 = sum(img4)[0];
	float avg2 = avg[0] * avg[0];
	float var  = dev[0] * dev[0];

	double skewness = ((sum3 - 3.0*avg[0]*sum2)/area + 2.0*avg[0]*avg2) / (var*dev[0]);

	double kurtosis = (((sum4 - 4.0*avg[0]*sum3 + 6.0*avg2*sum2)/area - 3.0*avg2*avg2)/(var*var)-3.0);

	result[0] = avg[0];
	result[1] = dev[0];
	result[2] = skewness;
	result[3] = kurtosis;

	return result;
}

Mat genMask(const Size& size, const vector<Point>& contour, int dist=1, int epsilon=-1){

	vector<Point> cts;

	if(epsilon>=1){
		approxPolyDP(contour, cts, epsilon, true);
	}else{
		cts = contour;
	}

	Scalar cc0 = Scalar::all(0);
	Scalar cc1 = Scalar::all(1);

	Mat msk;

	if(dist>=1){

		//mask is inside contour.
		dist = (dist==1)?(1):(2*dist);


		msk = Mat::ones(size,CV_8UC1);

		polylines(msk, cts, true, cc0, dist, LINE_8);

		floodFill(msk, Point(0,0), cc0);

	}else if(dist<-1){

		//mask is outside contour.
		dist = -2*dist;

		msk = Mat::zeros(size,CV_8UC1);

		polylines(msk, cts, true, cc1, dist, LINE_8);

		RotatedRect box = minAreaRect(cts);

		floodFill(msk, box.center, cc1);
	}
	return msk;
}

void drawRect(const Mat& ova, const vector<Rect>& tile){

	Mat msk = Mat::zeros(ova.size(),CV_8UC1);

	for(size_t i=0; i<tile.size(); i++){
		rectangle(msk,tile[i],Scalar::all(255),-1);
	}

	vector<vector<Point> > cts;

	findContours(
		msk, cts,
		RETR_EXTERNAL,
		CHAIN_APPROX_SIMPLE
	);

	drawContours(ova, cts, -1 ,Scalar(0,0,255), 1);
}

Mat process_2(const Mat& src, const Mat& mask){

	int chan[] = { 0 };
	Mat hist;
	int bins[] = { 256 };
	float rng1[] = {0.f, 255.f};
	const float* rang[] = { rng1 };
	calcHist(
		&src, 1, chan,
		mask,
		hist, 1, bins, rang
	);

	normalize(hist, hist, 1.,0., NORM_L1);

	Sobel(hist,hist,-1,0,1);

	double minVal, maxVal;
	Point maxLoc;
	minMaxLoc(hist,NULL,&maxVal,NULL,&maxLoc);

	minVal = maxVal * 0.01;

	int lower=maxLoc.y, upper=maxLoc.y;

	//find range from left
	for(int i=0; i<maxLoc.y; i++){
		if(hist.at<float>(0,i)>=minVal){
			lower = i;
			break;
		}
	}
	//find range from right
	for(int i=255; i>maxLoc.y; --i){
		if(hist.at<float>(0,i)>=minVal){
			upper = i;
			break;
		}
	}
	//create look-up table
	Mat tab = Mat::zeros(1,256,CV_8UC1);
	int delta = 256 / (upper - lower);
	int value = delta;
	for(int i=lower; i<=upper; i++){
		tab.at<uint8_t>(0,i) = value;
		value += delta;
	}
	for(int i=upper; i<256; i++){
		tab.at<uint8_t>(0,i) = 255;
	}
	cout<<tab<<endl;
	Mat dst;
	LUT(src, tab, dst);
	return dst;
}


vector<Rect> process_1(const Mat& src, const Mat& msk){

	Mat img = (255 - src);

	if(msk.empty()==false){
		img = img.mul(msk);
	}

	equalizeHist(img,img);

	morphologyEx(
		img, img,
		MORPH_OPEN,
		getStructuringElement(MORPH_ELLIPSE,Size(25,25))
	);

	imwrite("cc1.png",img);

	threshold(img, img, 200, 255, THRESH_BINARY);

	vector<vector<Point> > cts_all;

	findContours(
		img, cts_all,
		RETR_EXTERNAL,
		CHAIN_APPROX_SIMPLE
	);

	const int TILE_SIZE = 5;

	vector<Rect> result;

	for(size_t i=0; i<cts_all.size(); i++){

		vector<Point> cts = cts_all[i];

		Rect box = boundingRect(cts);

		Point pp;

		int lf = box.x + (box.width %TILE_SIZE)/2;
		int tp = box.y + (box.height%TILE_SIZE)/2;
		int rh = (box.x+box.width);
		int bm = (box.y+box.height);

		for(size_t yy=tp; yy<bm; yy+=TILE_SIZE){

			for(size_t xx=lf; xx<rh; xx+=TILE_SIZE){

				pp.x = xx;
				pp.y = yy;
				if(pointPolygonTest(cts,pp,false)<0){
					continue;
				}
				pp.x = xx+TILE_SIZE;
				pp.y = yy;
				if(pointPolygonTest(cts,pp,false)<0){
					continue;
				}
				pp.x = xx;
				pp.y = yy+TILE_SIZE;
				if(pointPolygonTest(cts,pp,false)<0){
					continue;
				}
				pp.x = xx+TILE_SIZE;
				pp.y = yy+TILE_SIZE;
				if(pointPolygonTest(cts,pp,false)<0){
					continue;
				}

				result.push_back(Rect(xx,yy,TILE_SIZE,TILE_SIZE));
			}
		}
	}

	return result;
}


extern Mat variance(const Mat& src, const int radius);

int main1(int argc, char* argv[]) {

	//const char* name1 = "./cv-sample2/13.png";
	//const char* name2 = "./cv-sample2/14.png";
	const char* name1 = "./sample-pad/aaa.png";
	const char* name2 = "./sample-pad/template.png";

	Mat src1 = imread(name1,IMREAD_GRAYSCALE);
	Mat src2 = imread(name2,IMREAD_GRAYSCALE);

	Mat ova1 = imread(name1,IMREAD_COLOR);
	Mat ova2 = imread(name2,IMREAD_COLOR);

	Mat b_src1;
	threshold(src1,b_src1,150,255,THRESH_BINARY);

	//meld(src1,src2,"cc2.png");

	vector<Point> cts1, cts2, cts_a;
	RotatedRect box1 = getShape(b_src1,cts1,15);
	RotatedRect box2 = getShape(src2,cts2,15);

	//matchShape(cts1,cts2);
	//polylines(ova1, cts1, true, Scalar(0,250,0), 1, LINE_4);
	//polylines(ova1, cts2, true, Scalar(0,0,255), 1, LINE_4);
	//imwrite("cc3.png",ova1);

	//create a mask;
	Mat msk = genMask(src1.size(), cts1, 7);

	Mat dst1 = process_2(src1,msk);

	dst1 = 255 - dst1;

	morphologyEx(
		dst1, dst1,
		MORPH_OPEN,
		getStructuringElement(MORPH_ELLIPSE,Size(30,30))
	);

	imwrite("cc1.png",dst1);

	//vector<Rect> tile = process_1(src1,msk);
	//drawRect(ova1,tile);
	//imwrite("cc2.png", ova1);

	//Scalar res = getMoment(equ_src,Mat());

	//Mat msk1,msk2,msk,mask;
	//threshold(skw, msk1,  5, 255, THRESH_BINARY);
	//threshold(skw, msk2, -5, 255, THRESH_BINARY_INV);
	//msk = msk1 + msk2;
	//msk.convertTo(mask,CV_8UC1);
	//imwrite("cc2.png",mask);

	/*Mat hist;
	int histChan[] = {0};
	int histSize[] = {10};
	float _range[] = {-1.f, 1.f};
	const float* histRang[] = {_range};
	calcHist(
		&skw, 1, histChan, Mat(),
		hist, 1, histSize, histRang
	);
	cout<<"HIST:"<<hist<<endl;*/

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
