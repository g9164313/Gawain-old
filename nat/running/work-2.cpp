/*
 * work-2.cpp
 *
 *  Created on: 2019年9月19日
 *      Author: qq
 */
#include <set>
#include <iostream>
#include <iterator>
#include <algorithm>
#include <vision.hpp>
using namespace std;

extern set<string> listName(string path, string appx);

int main(int argc, char* argv[]) {
	Rect zone_l( 528,2064, 650,650);
	Rect zone_r(3970,2068, 650,650);
	Mat data_o = imread("w-mark/TestImage-2019-9-19 162905.BMP", IMREAD_GRAYSCALE);

	Mat ova, img, tmp;
	//img = data_o(zone_l);
	//tmp = imread("template2.bmp", IMREAD_GRAYSCALE);
	blur(data_o(zone_l),img,Size(5,5));
	blur(imread("template1.bmp", IMREAD_GRAYSCALE),tmp,Size(5,5));

	Ptr<GeneralizedHoughGuil> ptr;
	//ptr = createGeneralizedHoughBallard();
	//ptr->setVotesThreshold(50);
	ptr = createGeneralizedHoughGuil();
	ptr->setAngleThresh(1500);
	ptr->setScaleThresh(80);
	ptr->setPosThresh(500);
	ptr->setTemplate(tmp);

	vector<Vec4f> pos;
	ptr->detect(img, pos);

	for(Vec4f val:pos){
		float posx = val[0];
		float posy = val[1];
		float scale = val[2];
		float angle = val[3];
		printf("(%3.2f, %3.2f)\n",posx+zone_l.x, posy+zone_l.y);
	}
	printf("done!!\n");

	//blur(data_o(zone_l), img, Size(5,5));
	//cvtColor(img,ova,COLOR_GRAY2RGB);
	//cvtColor(ova,img,COLOR_BGR2GRAY);
	//imwrite("cc1.png",img);
	return 0;
}

Mat crop(Mat src){
	int ox=0, oy=0;
	if(src.cols>149){
		ox = (src.cols - 149)/2;
	}
	if(src.rows>149){
		oy = (src.rows - 149)/2;
	}
	return src(Rect(ox,oy,149,149));
}

int main2(int argc, char* argv[]) {

	Mat img1, img2, img3, segm;

	Ptr<BackgroundSubtractor> ptr;

	set<string> names = listName("wafer/raw/slice-ok","");

	ptr = createBackgroundSubtractorMOG();
	//ptr = createBackgroundSubtractorGMG(names.size());

	for(string name:names){
		img1 = crop(imread(name.c_str(),IMREAD_GRAYSCALE));
		ptr->apply(img1, img2, 0.9);
	}

	//ptr.dynamicCast<BackgroundSubtractorGMG>()->setUpdateBackgroundModel(false);

	//img1 = imread("wafer/raw/slice-ng/006-003.png",IMREAD_GRAYSCALE);
	//ptr->apply(img1(Rect(0,0,149,149)), img2, -1.);
	//imwrite("wafer/check/cc1.png",img2);

	names = listName("wafer/raw/slice-ng","");
	for(string name:names){
		cout<<"predict "<<name<<endl;
		img1 = imread(name,IMREAD_GRAYSCALE);
		ptr->apply(crop(img1), img2, -1.);

		Mat ova = crop(imread(name));
		vector<vector<Point> > cts;
		findContours(
			img2, cts,
			RETR_EXTERNAL, CHAIN_APPROX_NONE
		);
		for(int i=0; i<cts.size(); i++){
			drawContours(ova, cts, i, Scalar(0,255,0));
		}
		name = name.substr(name.find_last_of("\\/")+1);
		imwrite("wafer/check/"+name,ova);
	}

	cout<<"Done!!"<<endl;
	return 0;
}

