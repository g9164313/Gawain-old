#include <global.hpp>
#include <dirent.h>

using namespace std;
using namespace cv;

extern void test_one(string inName, int idx);
extern int test_plural(string inName,bool count=false,int skip=0);
extern int test_plural_dir(string inName,bool count=false);

Mat genHistSign(Mat& a){
	vector<Vec4i> lst;//four coordinates and one weight~~
	for(int j=0; j<a.rows; j++){
		for(int i=0; i<a.cols; i++){
			Vec3b pix = a.at<Vec3b>(j,i);
			int key1 = 
				(((int)pix[0])<<16) | 
				(((int)pix[1])<<8) |
				(((int)pix[2]));
			int idx = -1;
			for(size_t k=0; k<lst.size(); k++){
				Vec4i tmp = lst[k];
				int key2 = 
					(((int)tmp[0])<<16) | 
					(((int)tmp[1])<<8) |
					(((int)tmp[2]));
				if(key1==key2){
					idx = k;
					break;
				}
			}
			if(idx<0){
				lst.push_back(Vec4i(pix[0],pix[1],pix[2],1));
			}else{
				lst[idx][3]++;
			}
		}
	}

	size_t cnt = lst.size();	
	Mat sign = Mat::zeros(lst.size(),4,CV_32FC1);
	for(size_t k=0; k<lst.size(); k++){
		Vec4i val = lst[k];
		sign.at<float>(k,0) = (float)val[3];//weight
		sign.at<float>(k,1) = val[0]/255.f;//coordinate - red
		sign.at<float>(k,2) = val[1]/255.f;//coordinate - green
		sign.at<float>(k,3) = val[2]/255.f;//coordinate - blue
	}	
	return sign;
}

double cmpHist(Mat& a, Mat& b){
	Mat sa = genHistSign(a);
	Mat sb = genHistSign(b);
	return EMD(sa,sb,CV_DIST_L2);
}

extern void registeration(
	Mat& imgSrc, Mat& imgRef,
	Point* locate,
	Point* center,
	float* angle,
	float* scale	
);
extern void shift_image(
	Mat& img,
	Point locate,
	Point center,
	double angle
);

int main(int argc, char* argv[]) {

	Mat& ga = imread("nir.0.jpg",IMREAD_GRAYSCALE);
	Mat& gb = imread("rgb.0.jpg",IMREAD_GRAYSCALE);

	//remove(logName);
	//timeval t1, t2;
	//float time;
	//gettimeofday(&t1, NULL);
	DWORD tt;
	tt = GetTickCount();

	//double dist = cmpHist(aa,bb);
	//printf("distance = %.4f\n\n",dist);
	Rect roi(0,932,1850,1850);
	Mat& aa = ga(roi);
	Mat& bb = gb(roi);
	float ang;
	Point loca,cent;
	registeration(aa,bb,&loca,&cent,&ang,NULL);
	printf(
		"locate=(%d,%d)\n"
		"center=(%d,%d)\n"
		"angle =%.3f\n",
		loca,cent,ang
	);

	//imwrite("ttt.0.png",aa);
	//imwrite("ttt.1.png",bb);

	Mat cb = imread("rgb.0.jpg");
	cent.x = cent.x + roi.x;
	cent.y = cent.y + roi.y;
	shift_image(cb,loca,cent,0.);	
	imwrite("nir.1.png",cb);

	//time = (float)(t2.tv_sec-t1.tv_sec)*1000;
	//time += (float)(t2.tv_usec-t1.tv_usec)/1000;
	//printf("\n elapse=%.2fms \n",time);
	tt = GetTickCount() - tt;
	printf("estimate:%dms\n",tt);
	//dumpHist();
	return 0;
}

