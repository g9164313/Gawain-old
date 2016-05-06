#include <global.hpp>
#include <algorithm>

//some experiments
int main(int argc, char* argv[]) {

	Mat img = imread("123.bmp");

	vector<Point> pts;

	for(int j=0; j<img.rows; j++){
		for(int i=0; i<img.cols; i++){
			Point pos(i,j);
			Vec3b val = img.at<Vec3b>(pos);
			if(val[0]==val[1] && val[2]>(val[1]*2)){
				pts.push_back(pos);
				//cout<<"include "<<pos<<endl;
				cout<<pos.x<<"\t"<<pos.y<<endl;
			}
		}
	}
/*
	RotatedRect rr = fitEllipse(pts);
	Point cc(rr.center.x,rr.center.y);
	line(img,cc+Point(-10,0),cc+Point(10, 0),Scalar(0,200,0));
	line(img,cc+Point(0,-10),cc+Point( 0,10),Scalar(0,200,0));
	ellipse(img,rr,Scalar(0,200,0));
	cout<<"center="<<rr.center<<endl;
	cout<<"size="<<rr.size<<endl;
	cout<<"angle="<<rr.angle<<endl;
*/

	Point2f center;
	float radius;
	minEnclosingCircle(pts,center,radius);
	//draw center and bounding
	Point cc(center.x,center.y);
	line(img,cc+Point(-10,0),cc+Point(10, 0),Scalar(0,200,0));
	line(img,cc+Point(0,-10),cc+Point( 0,10),Scalar(0,200,0));
	circle(img,center,radius,Scalar(0,200,0));
	cout<<"center="<<center<<endl;
	cout<<"size="<<radius<<endl;

	for(int i=0; i<pts.size(); i++){
		img.at<Vec3b>(pts[i]) = Vec3b(0,255,255);
	}
	imwrite("res.png",img);
	return 0;
}
