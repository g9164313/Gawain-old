/*
 * main-xx5.cpp
 *
 *  Created on: 2018年11月1日
 *      Author: qq
 */
#include <iostream>
#include <vector>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

void draw_mark(Mat& ova, Mat& mark, Point arch, Size dime);

extern const Scalar colorJet[];

void setBlock(Size dimesion, Point position);
void setBlock(Size dimesion);
void setBlock(Point position);
size_t getCount(Mat& src, size_t* cntX=NULL, size_t* cntY=NULL);
Mat getBlock(Mat& src, int idx, Rect* roi=NULL);

int main(int args, char** argv){

	const char* path = "./wafer/train/OK/000-000.png";
	Mat ova = imread(path, IMREAD_COLOR);
	Mat img = imread(path, IMREAD_GRAYSCALE);

	setBlock(Size(8,8));

	int cnt = getCount(img);

	Mat img1 = getBlock(img, 0);

	Mat pyr2, pyr3, out1, out2, out3;
	pyrDown(img1, pyr2);
	pyrDown(pyr2, pyr3);

	Size sss(224,224);
	resize(img1, out1, sss, INTER_NEAREST);
	resize(pyr2, out2, sss, INTER_NEAREST);
	resize(pyr3, out3, sss, INTER_NEAREST);

	imwrite("./wafer/block0/class1/0001.png",out1);
	imwrite("./wafer/block0/class2/0001.png",out2);
	imwrite("./wafer/block0/class3/0001.png",out3);

	return 0;
}

static Size dim(8,8);
static Point pts(-1,-1);
static size_t maxIdx = 0;

void setBlock(Size dimesion, Point position){
	dim = dimesion;
	pts = position;
	maxIdx = 0;//reset again for next turn~~~
}
void setBlock(Size dimesion){
	dim = dimesion;
	maxIdx = 0;//reset again for next turn~~~
}
void setBlock(Point position){
	pts = position;
	maxIdx = 0;//reset again for next turn~~~
}

size_t getCount(Mat& src, size_t* cntX, size_t* cntY){
	size_t cx = (src.cols / dim.width);
	size_t cy = (src.rows / dim.height);
	maxIdx = cx * cy;
	if(cntX!=NULL){ *cntX = cx; }
	if(cntY!=NULL){ *cntY = cy; }
	return maxIdx;
}

Mat getBlock(Mat& src, int idx, Rect* roi){
	size_t grid_x, grid_y;
	if(pts.x<0 || pts.y<0){
		//Default starter location is from center
		grid_x = (src.cols % dim.width )/2;
		grid_y = (src.rows % dim.height)/2;
	}else{
		grid_x = pts.x;
		grid_y = pts.y;
	}
	size_t  cnt_x, cnt_y;
	getCount(src, &cnt_x, &cnt_y);
	if(idx>=maxIdx){
		return Mat();
	}
	Rect zone = Rect(
		grid_x + (idx%cnt_x)*dim.width,
		grid_y + (idx/cnt_x)*dim.height,
		dim.width,
		dim.height
	);
	if(roi!=NULL){
		*roi = zone;
	}
	return src(zone);
}

void draw_mark(Mat& ova, Mat& mark, Point arch, Size dime){

	double max;
	minMaxLoc(mark,NULL,&max);

	for(int j=0; j<mark.rows; j++){
		for(int i=0; i<mark.cols; i++){
			int xx = arch.x + dime.width * i;
			int yy = arch.y + dime.height * j;
			double val = (double)mark.at<int32_t>(j,i);
			uint32_t idx = (uint32_t)((val/max)*255.);
			rectangle(
				ova,
				Rect(
					xx+1,
					yy+1,
					dime.width-1,
					dime.height-1
				),
				colorJet[idx],
				1
			);
			/*char buf[3];
			sprintf(buf, "%d", mark.at<int32_t>(j,i));
			xx = xx + 3;
			yy = yy + dime.height - 3;
			putText(ova, buf, Point(xx,yy), FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0,200,0));
			imwrite("cc1.png",ova);*/
		}
	}
	imwrite("cc1.png",ova);
}



