/*
 * regist.cpp
 *
 *  Created on: 2016年3月28日
 *      Author: qq
 */
#include <global.hpp>
#include <vision.hpp>
//#define CHECK_STEP

static void dftMapping(Mat& src,Mat& dst);
static float freqPolar(Mat& ref,Mat& src);
static void tranShift(Mat& src,Point& offset);
static void tranRotate(Mat& src,double angle,Point* _org=NULL);

void registration(
	Mat& _ref,
	Mat& _src,
	double* angle,
	double* response
){
	//this will translate 'Reference image'
	CV_Assert(_ref.type() == CV_8UC1);//gray image only
	CV_Assert(_ref.type() == _src.type());
	CV_Assert(_ref.size == _src.size);

	Mat ref,src;
	if(_ref.cols==_ref.rows){
		check32f(_ref,ref);
		check32f(_src,src);
	}else{
		//modify ratio size to square~~~
		//because Fourier spectra must keep same size, then rotation angle will be right~~~
		int bnd = std::min(_ref.cols,_ref.rows);
		Rect roi(
			(_ref.cols-bnd)/2,
			(_ref.rows-bnd)/2,
			bnd,bnd
		);
		_ref(roi).convertTo(ref,CV_32FC1);
		_src(roi).convertTo(src,CV_32FC1);
	}
	double ang = -1.*freqPolar(ref,src);
	tranRotate(_ref,ang);
	if(angle!=NULL){
		*angle = ang;//positive is anti-clockwise, otherwise negative is clockwise~~
	}

	ref.release();
	src.release();

	check32f(_ref,ref);
	check32f(_src,src);
	Mat hann;
	createHanningWindow(hann,ref.size(),CV_32F);
	Point off = phaseCorrelate(ref,src,hann,response);//Hanning will overwrite data
	tranShift(_ref,off);
}

void imposition(
	const char* name,
	Mat& ref,
	Mat& src
){
	Mat node;
	addWeighted(
		ref,0.55,
		src,0.45,1.0,
		node
	);
	imwrite(name,node);
}

static float freqPolar(Mat& ref,Mat& src){
	Mat fft1,fft2;
	dftMapping(ref,fft1);
	dftMapping(src,fft2);
#ifdef CHECK_STEP
	dump32f("cc0.ref.png",fft1);
	dump32f("cc0.src.png",fft2);
#endif

	Mat hann;
	createHanningWindow(hann,fft1.size(), CV_32F);
	hann = 1.-hann;
	multiply(hann, fft1, fft1);
	multiply(hann, fft2, fft2);
#ifdef CHECK_STEP
	dump32f("cc1.ref.png",fft1);
	dump32f("cc1.src.png",fft2);
#endif

	Point cent(fft1.cols/2,fft1.rows/2);
	double scale = fft1.cols/2;

	Mat poa1 = Mat::zeros(fft1.size(),CV_32FC1);
	linearPolar(
		fft1,poa1,
		cent,scale,
		INTER_LINEAR
	);
	Mat poa2 = Mat::zeros(fft1.size(),CV_32FC1);
	linearPolar(
		fft2,poa2,
		cent,scale,
		INTER_LINEAR
	);
#ifdef CHECK_STEP
	dump32f("cc2.ref.png",poa1);
	dump32f("cc2.src.png",poa2);
#endif

	hann = 1.-hann;//invert this matrix again!!!
	Point2d res = phaseCorrelate(poa1,poa2,hann);
	return (res.y*360.)/(float)(poa1.rows);
}

static void dftMapping(Mat& src,Mat& dst){
	Mat temp,freq;
	Mat plane[] = {
		Mat(src.size(),CV_32FC1),
		Mat(src.size(),CV_32FC1),
	};
	src.copyTo(plane[0]);//prepare image~~~
	merge(plane,2,temp);
	dft(temp,freq);
	split(freq,plane);
	temp.release();
	freq.release();
	magnitude(plane[0],plane[1],temp);
	temp += Scalar::all(1);
	log(temp,freq);

	int cx = freq.cols/2;
	int cy = freq.rows/2;

	Mat q0(freq, Rect(0, 0, cx, cy));   // Top-Left - Create a ROI per quadrant
	Mat q1(freq, Rect(cx, 0, cx, cy));  // Top-Right
	Mat q2(freq, Rect(0, cy, cx, cy));  // Bottom-Left
	Mat q3(freq, Rect(cx, cy, cx, cy)); // Bottom-Right

	temp.release();
	q0.copyTo(temp);// swap quadrants (Top-Left with Bottom-Right)
	q3.copyTo(q0);
	temp.copyTo(q3);

	q1.copyTo(temp);// swap quadrant (Top-Right with Bottom-Left)
	q2.copyTo(q1);
	temp.copyTo(q2);

	temp.release();
	normalize(freq,temp,0.0,1.,NORM_MINMAX);
	threshold(temp,dst,0.5,0.,THRESH_TOZERO);
}

static void tranRotate(Mat& src,double angle,Point* _org){
	Point org(src.cols/2,src.rows/2);
	if(_org!=NULL){
		org = *_org;
	}
	Mat tmp;
	warpAffine(
		src,src,
		getRotationMatrix2D(org,angle,1.0),
		src.size(),
		INTER_LINEAR,
		BORDER_REPLICATE
	);
}

static void tranShift(Mat& src,Point& offset){
	Point2f v1[3] = {
		Point2f(0,0),
		Point2f(src.cols,0),
		Point2f(0,src.rows),
	};
	Point2f v2[3] = {
		Point2f(offset),
		Point2f(offset)+v1[1],
		Point2f(offset)+v1[2]
	};
	warpAffine(
		src,src,
		getAffineTransform(v1,v2),
		src.size(),
		BORDER_REPLICATE
	);
}




