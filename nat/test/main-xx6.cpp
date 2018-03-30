/*
 * main-xx6.cpp
 *
 *  Created on: 2018年3月15日
 *      Author: qq
 */
#include <fstream>
#include <algorithm>
#include <iostream>
#include <vector>
#include <opencv2/opencv.hpp>
#include <ctime>
#include <cmath>

using namespace cv;
using namespace std;

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
	Scalar(0,0,255),
	Scalar(0,255,0),
	Scalar(255,0,0),
	Scalar(255,255,0),
	Scalar(0,255,255),
	Scalar(255,0,255),
};

bool ladder_y_axis(Vec4f A, Vec4f B){
	Point aa(
		(A[0]+A[2])/2,
		(A[1]+A[3])/2
	);
	Point bb(
		(B[0]+B[2])/2,
		(B[1]+B[3])/2
	);
	if(abs(aa.y-bb.y)>3.f){
		if(aa.y>bb.y){
			return false;
		}
		return true;
	}
	if(aa.x>bb.x){
		return false;
	}
	return true;
}

bool ladder_x_axis(Vec4f A, Vec4f B){
	Point aa(
		(A[0]+A[2])/2,
		(A[1]+A[3])/2
	);
	Point bb(
		(B[0]+B[2])/2,
		(B[1]+B[3])/2
	);
	if(abs(aa.x-bb.x)>3.f){
		if(aa.x>bb.x){
			return false;
		}
		return true;
	}
	if(aa.y>bb.y){
		return true;
	}
	return false;
}

vector<Vec4f> group_line(char tkn, vector<Vec4f>& lst, int gapSize){

	if(tkn=='x' || tkn=='X' || tkn=='h' || tkn=='H'){
		sort(lst.begin(), lst.end(), ladder_y_axis);
	}else if(tkn=='y' || tkn=='Y' || tkn=='v' || tkn=='V'){
		sort(lst.begin(), lst.end(), ladder_x_axis);
	}

	float bound = gapSize/2.f;

	vector<Vec4f> grp;

	Point2f aa, bb;

	for(size_t i=0, j=0; i<lst.size(); i++){

		aa.x = (lst[i][0] + lst[i][2])/2.f;
		aa.y = (lst[i][1] + lst[i][3])/2.f;
		//cout<<"<<<"<<aa<<">>>"<<endl;
		for(j=i+1; j<lst.size(); j++){

			bb.x = (lst[j][0] + lst[j][2])/2.f;
			bb.y = (lst[j][1] + lst[j][3])/2.f;

			float v1=0.f, v2=0.f;

			if(tkn=='x' || tkn=='X' || tkn=='h' || tkn=='H'){
				v1 = aa.y;
				v2 = bb.y;
			}else if(tkn=='y' || tkn=='Y' || tkn=='v' || tkn=='V'){
				v1 = aa.x;
				v2 = bb.x;
			}
			if(abs(v1-v2)>=bound){
				j--;
				break;
			}
			//cout<<bb<<endl;
		}
		//cout<<"-------"<<endl;

		if(j>=lst.size()){
			j--;//this is tail, just subtract one~~~
		}
		//find average slope, normal all vectors...
		int cnt = j - i + 1;
		if(cnt<=0){
			continue;
		}

		Mat dat(cnt, 2, CV_32FC1);
		for(size_t k=0; k<cnt; k++){
			aa.x = lst[k][0];
			aa.y = lst[k][1];
			bb.x = lst[k][2];
			bb.y = lst[k][3];
			float dist = norm(bb-aa);
			dat.at<float>(k,0) = (bb.x - aa.x)/dist;
			dat.at<float>(k,1) = (bb.y - aa.y)/dist;
		}
		//cout<<dat<<endl;

		PCA pca(dat,Mat(),CV_PCA_DATA_AS_ROW);
		//cout<<endl<<pca.eigenvectors<<endl;
		//cout<<endl<<pca.mean<<endl;
		bb.x = pca.eigenvectors.at<float>(0,0);
		bb.y = pca.eigenvectors.at<float>(0,1);

		//find vertex along board....
		int avg = 0;
		for(size_t k=i; k<=j; k++){
			aa.x = lst[k][0];
			aa.y = lst[k][1];
			if(tkn=='x' || tkn=='X' || tkn=='h' || tkn=='H'){
				avg = avg + aa.y - (aa.x * bb.y) / bb.x;
			}else if(tkn=='y' || tkn=='Y' || tkn=='v' || tkn=='V'){
				avg = avg + aa.x - (aa.y * bb.x) / bb.y;
			}
		}
		avg = avg/cnt;

		if(tkn=='x' || tkn=='X' || tkn=='h' || tkn=='H'){
			grp.push_back(Vec4f(0, avg, bb.x, bb.y));
		}else if(tkn=='y' || tkn=='Y' || tkn=='v' || tkn=='V'){
			grp.push_back(Vec4f(avg, 0, bb.x, bb.y));
		}

		i = j;//swap index, for next turn
	}
	return grp;
}

/**
 * get mesh grid from wafer image.<p>
 * every element in matrix stores location value(16-bit).
 */
Mat findMeshFromWafer(Mat& img, int gapSize){

	vector<Vec4f> lst, hori, vert, h_grp, v_grp;

	Ptr<LineSegmentDetector> algo = createLineSegmentDetector(LSD_REFINE_NONE);
	algo->detect(img, lst);

	double paramSpaceRatio = 0.1;
	double maxGapSize = gapSize * (1.+paramSpaceRatio);
	double minGapSize = gapSize * (1.-paramSpaceRatio);

	for(size_t i=0, k=0; i<lst.size(); i++){

		Point a1(lst[i][0],lst[i][1]);

		Point a2(lst[i][2],lst[i][3]);

		double len = norm(a1-a2);
		if(len<minGapSize || maxGapSize<len){
			continue;
		}

		double deg = (atan((a1.y-a2.y)/(a2.x - a1.x)) * 180.)/M_PI;

		if( (0<=deg && deg<45) || (-45<deg && deg<0)){
			hori.push_back(lst[i]);
			//Point2f aa(lst[i][0], lst[i][1]);
			//Point2f bb(lst[i][2], lst[i][3]);
			//char txt[100];
			//sprintf(txt,"%d",(int)k++);
			//putText(ova, txt, aa, FONT_HERSHEY_SCRIPT_SIMPLEX, 0.5, color[k%3]);
			//arrowedLine(ova, aa, bb, color[k%3], 1, 4, 0, 0.02);
		}else{
			vert.push_back(lst[i]);
		}
	}

	h_grp = group_line('x', hori, gapSize);
	v_grp = group_line('y', vert, gapSize);
	///for(size_t i=0; i<h_grp.size(); i++){
	//	Point2f p1(h_grp[i][0], h_grp[i][1]);
	//	Point2f vv(h_grp[i][2], h_grp[i][3]);
	//	Point2f p2 = p1 + vv*ova.cols*1.4;
	//	arrowedLine(ova, p1, p2, color[1], 2, 4, 0, 0.02);
	//}
	//for(size_t i=0; i<v_grp.size(); i++){
	//	Point2f p1(v_grp[i][0], v_grp[i][1]);
	//	Point2f vv(v_grp[i][2], v_grp[i][3]);
	//	Point2f p2 = p1 + vv*ova.rows*1.4;
	//	arrowedLine(ova, p1, p2, color[2], 2, 4, 0, 0.02);
	//}

	Mat mesh(v_grp.size(), h_grp.size(), CV_16UC2);

	for(size_t j=0; j<v_grp.size(); j++){
		for(size_t i=0; i<h_grp.size(); i++){

			Point2f pA(v_grp[j][0], v_grp[j][1]);
			float sA = v_grp[j][3] / v_grp[j][2];

			Point2f pB(h_grp[i][0], h_grp[i][1]);
			float sB = h_grp[i][3] / h_grp[i][2];

			float cx = (sA * pA.x - sB * pB.x - pA.y + pB.y) / (sA-sB);

			float cy = ((-pA.x+pB.x)*sA*sB + sB*pA.y -sA*pB.y )/ (sB-sA);

			mesh.at<Vec2w>(j,i) = Vec2w(cx, cy);//TODO: negtive value
		}
	}
	return mesh;
}

void markedDieFromMesh(Mat& img, Mat& mesh){

	Scalar green(0,250,0);

	for(size_t j=0; j<mesh.rows; j++){
		for(size_t i=0; i<mesh.cols; i++){
			Vec2w pos = mesh.at<Vec2w>(j,i);
			Point2f p0(pos[0],pos[1]);
			Point2f a1 = p0 + Point2f(0,10);
			Point2f a2 = p0 - Point2f(0,10);
			Point2f b1 = p0 + Point2f(10,0);
			Point2f b2 = p0 - Point2f(10,0);
			line(img, a1, a2, green, 3);
			line(img, b1, b2, green, 3);
		}
	}
	imwrite("dd1.png",img);
}

void pickupDieFromMesh(Mat& img, Mat& mesh, const char* dir){

	Scalar green(0,250,0);

	int maxWidth=0, maxHeight=0;

	for(size_t j=0; j<mesh.rows-1; j++){
		for(size_t i=0; i<mesh.cols-1; i++){

			Vec2w tp_lf = mesh.at<Vec2w>(j  ,i  );
			Vec2w tp_rh = mesh.at<Vec2w>(j  ,i+1);
			Vec2w bm_rh = mesh.at<Vec2w>(j+1,i+1);
			Vec2w bm_lf = mesh.at<Vec2w>(j+1,i  );

			int lf = min(tp_lf[0], bm_lf[0]);
			int rh = max(tp_rh[0], bm_rh[0]);
			int tp = min(tp_lf[1], tp_rh[1]);
			int bm = max(bm_rh[1], bm_lf[1]);

			int ww = rh - lf;
			int hh = bm - tp;

			if(lf<0 || tp<0){
				continue;
			}
			if(img.cols<=rh || img.rows<=bm){
				continue;
			}

			if(ww>maxWidth ){
				maxWidth = ww;
			}
			if(hh>maxHeight){
				maxHeight= hh;
			}
		}
	}
	cout<<"maximum size=("<<maxWidth<<" , "<<maxHeight<<")"<<endl;

	char name[200];

	for(size_t j=0; j<mesh.rows-1; j++){
		for(size_t i=0; i<mesh.cols-1; i++){

			Vec2w tp_lf = mesh.at<Vec2w>(j  ,i  );
			Vec2w tp_rh = mesh.at<Vec2w>(j  ,i+1);
			Vec2w bm_rh = mesh.at<Vec2w>(j+1,i+1);
			Vec2w bm_lf = mesh.at<Vec2w>(j+1,i  );

			int lf = min(tp_lf[0], bm_lf[0]);
			int rh = max(tp_rh[0], bm_rh[0]);
			int tp = min(tp_lf[1], tp_rh[1]);
			int bm = max(bm_rh[1], bm_lf[1]);

			if(lf<0 || tp<0){
				continue;
			}
			if(img.cols<=rh || img.rows<=bm){
				continue;
			}

			Rect roi(lf,tp,(rh-lf+1),(bm-tp+1));

			int rem_w = (roi.width <maxWidth )?(maxWidth - roi.width ):(0);
			int rem_h = (roi.height<maxHeight)?(maxHeight- roi.height):(0);

			Mat zone = img(roi);
			Mat node;
			copyMakeBorder(
				zone, node,
				0, rem_h,
				0, rem_w,
				BORDER_REPLICATE
			);

			sprintf(name,"%s/%03d-%03d.png", dir, (int)i, (int)j);
			imwrite(name,node);

			sprintf(name,"%03d-%03d", (int)i, (int)j);
			putText(img, name,
				Point(roi.x+10, roi.y+roi.height/3),
				FONT_HERSHEY_SCRIPT_SIMPLEX,
				0.5, green
			);
		}
	}

	sprintf(name,"%s/mapper.png", dir);
	imwrite(name,img);
}

int main(int argc, char* argv[]) {

	Mat img = imread("./wafer/seg1.png",IMREAD_GRAYSCALE);
	Mat ova = imread("./wafer/seg1.png",IMREAD_COLOR);

	const int DIE_LENGTH = 140;

	double start = double(getTickCount());

	Mat mesh = findMeshFromWafer(img,DIE_LENGTH);

	double duration_ms = (double(getTickCount()) - start) * 1000 / getTickFrequency();
	std::cout << "FindLine took " << duration_ms << " ms." << std::endl;

	//markedDieFromMesh(ova, mesh);

	pickupDieFromMesh(ova, mesh, "./wafer/dies");

	cout<<"Done!!"<<endl;
	return 0;
}




