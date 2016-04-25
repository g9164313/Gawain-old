#include <skill.hpp>

extern Point findHole(Mat& img,int diameter);

int main(int argc, char* argv[]) {
	Mat& pic = imread("test.1.png");
	//Mat& pic = imread("test.6.jpg");
	//Mat& pic = imread("test.8.jpg");
	int ww = pic.cols/3;
	int hh = pic.rows/3;
	Mat& roi = pic(Rect(ww,hh,ww,hh));
	imwrite("cc.1.png",roi);
	Point a = findHole(roi,-1);
	printf("loca=(%d,%d)\n",a.x,a.y);
	return 0;
}

Point findHole(Mat& img,int diameter){
	Point loc;
	Mat map;
	//cvtColor(img,gray,COLOR_BGR2GRAY);
	cvtColor(img,map,COLOR_BGR2HSV);
	Size ss = img.size();
	Mat ch[] = {
		Mat(ss,CV_8UC1),
		Mat(ss,CV_8UC1),
		Mat(ss,CV_8UC1)
	};
	split(map,ch);
	Scalar avg,dev;
	meanStdDev(ch[0],avg,dev);
	Canny(ch[0],ch[1],dev[0],dev[0]*2);
 	imwrite("cc.2.png",ch[0]);
	imwrite("cc.3.png",ch[1]);
	/*	
	imwrite("cc.2.h.png",ch[0]);
	imwrite("cc.2.s.png",ch[1]);
	imwrite("cc.2.v.png",ch[2]);
	if(diameter<=0){
		diameter = (ss.height*70)/100;
		//diameter = (diameter*50)/100;
	}
	int rad = 10;//trival value~~~
	Mat kern = getStructuringElement(
		MORPH_ELLIPSE,
		Size(2*rad+1,2*rad+1),
		Point(rad,rad)
	);
	dilate(ch[0],ch[1],kern);
	erode(ch[1],ch[2],kern);
	imwrite("cc.3.png",ch[2]);*/
	return loc;
}
