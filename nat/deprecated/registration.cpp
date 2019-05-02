/*
 * registration.cpp
 *
 *  Created on: 2017年2月7日
 *      Author: qq
 */

#include <global.hpp>
#include "vision.hpp"

static void img2dat(const Mat& img, Mat& dat){
	int size = std::min(img.cols,img.rows);
	//size = (size*80)/100;
	Rect roi(
		(img.cols-size)/2,
		(img.rows-size)/2,
		size,
		size
	);
	check32f(img,roi,dat);
}

static void trans_offset(Mat& dat,Point& off){
	if(off.x==0 && off.y==0){
		return;//skip this condition~~~
	}
	Point2f v1[3] = {
		Point2f(0,0),
		Point2f(dat.cols,0),
		Point2f(0,dat.rows),
	};
	Point2f v2[3] = {
		Point2f(0+off.x,0+off.y),
		Point2f(dat.cols+off.x,0+off.y),
		Point2f(0+off.x,dat.rows+off.y),
	};
	warpAffine(
		dat,dat,
		getAffineTransform(v1,v2),
		dat.size(),
		INTER_LINEAR,
		BORDER_CONSTANT
	);
}

static void trans_rotate(Mat& dat,double angle){
	if(angle==0.){
		return;//skip this condition~~~
	}
	Point org(dat.cols/2,dat.rows/2);
	warpAffine(
		dat,dat,
		getRotationMatrix2D(org,-angle,1.0),
		dat.size(),
		INTER_LINEAR,
		BORDER_CONSTANT
	);//clock-wise is positive angle, 0 degree is to vertical direction!!!
}

static double regist_data(
	Mat* imgRef,
	Mat* datRef,
	Mat* datSrc,
	Mat* porSrc,
	Mat* Hann,
	Size* Border,
	Point* Center,
	double* Radius
){
	Mat& img = *imgRef;
	Mat& ref = *datRef;
	Mat& src = *datSrc;
	Mat& p_src = *porSrc;
	Mat& hann = *Hann;
	Size& border = *Border;
	Point& center = *Center;
	double radius = *Radius;

	double offset_resp=0.,rotate_resp=0.;

	Point offset = phaseCorrelate(ref,src,hann,&offset_resp);
	if(offset_resp>0.9){
		cout<<"Finally!! offset=("<<offset.x<<","<<offset.y<<")"<<endl;
		cout<<"Finally!! offset-resp="<<offset_resp<<endl<<endl;
		trans_offset(img,offset);//Finally, meet a good answer,so we change origin image...
		return offset_resp;
	}

	Mat p_ref;
	linearPolar(
		ref, p_ref,
		center, radius,
		INTER_LINEAR + WARP_FILL_OUTLIERS
	);
	Point rotate = phaseCorrelate(p_ref,p_src,hann,&rotate_resp);
	double angle = cvRound((rotate.y*360.)/border.width);//degree
	if(rotate_resp>0.9){
		cout<<"Finally!! rotate=("<<rotate.x<<","<<rotate.y<<") @ "<<angle<<endl;
		cout<<"Finally!! rotate-resp="<<rotate_resp<<endl<<endl;
		trans_rotate(img,angle);//Finally, meet a good answer,so we change origin image...
		return rotate_resp;//Good match, we done!!!
	}

	if(offset_resp>rotate_resp){
		cout<<"offset=("<<offset.x<<","<<offset.y<<")"<<endl;
		cout<<"offset-resp="<<offset_resp<<endl<<endl;
		trans_offset(ref,offset);
		trans_offset(img,offset);//also change origin image~~~
		int dx = cvRound(offset.x);
		int dy = cvRound(offset.y);
		if(dx==0 && dy==0){
			return offset_resp;//we reach limit!!!
		}
	}else{
		cout<<"rotate=("<<rotate.x<<","<<rotate.y<<") @ "<<angle<<endl;
		cout<<"rotate-resp="<<rotate_resp<<endl<<endl;
		trans_rotate(ref,angle);
		trans_rotate(img,angle);//also change origin image~~~
		if(angle==0.){
			return rotate_resp;//we reach limit!!!
		}
	}
	/*Mat temp;
	getRectSubPix(
		ref,
		border,center+offset,
		temp
	);
	temp.copyTo(ref);*/
	//dump32f("cc3.png",ref);

	return regist_data(
		imgRef,
		datRef,
		datSrc,
		porSrc,
		Hann,
		Border,
		Center,
		Radius
	);
}

/**
 * Fit reference-image to source-image.<p>
 * Attention!! Reference-image will be modified.<p>
 */
void registration(Mat& imgRef, Mat& imgSrc){

	Mat datRef,datSrc,porSrc;

	img2dat(imgRef,datRef);
	img2dat(imgSrc,datSrc);

	Size border;
	Point center;

	border.width = border.height = imgRef.cols;
	center.x = center.y = imgRef.cols/2;

	double radius = border.width/2;
	linearPolar(
		datSrc, porSrc,
		center, radius,
		INTER_LINEAR + WARP_FILL_OUTLIERS
	);
	Mat hann;
	createHanningWindow(hann,border,CV_32F);

	regist_data(
		&imgRef,
		&datRef,
		&datSrc,
		&porSrc,
		&hann,
		&border,
		&center,
		&radius
	);
}
