/*
 * main-stuffer.cpp
 *
 *  Created on: 2018年6月15日
 *      Author: qq
 */
#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <global.hpp>
#include <fstream>
#include <algorithm>
#include <iostream>

using namespace cv;
using namespace std;

const char* WIN_TITLE="Cropper";

const int IMG_SIZE = 32;
const int IMG_SIZE2 = (IMG_SIZE/2);

const int SCALE = 4;

const int BLOB_SIZE = 8;
const int SCALE_BLOB_SIZE = (BLOB_SIZE*SCALE);
const int SCALE_BLOB_SIZE2= (BLOB_SIZE*SCALE)/2;
const int SCALE_BLOB_SIZE4= (BLOB_SIZE*SCALE)/4;

struct UserBundle {
	Mat *src;
	list<Rect> zone;//it is 1:1 scale as source image
	int cx, cy;//scale is same as overlay and view layer
	char c_type;
	Mat *ova, *vew;
};

extern char** listFileName(const char* path, const char* part, int* size);

static void draw_zone(struct UserBundle& bundle, Mat& vew){
	list<Rect>::iterator ptr = bundle.zone.begin();
	for(
		ptr =bundle.zone.begin();
		ptr!=bundle.zone.end();
		++ptr
	){
		Rect roi = *ptr;//just copy it!!!
		roi.x = roi.x * SCALE;
		roi.y = roi.y * SCALE;
		roi.width = roi.width * SCALE;
		roi.height= roi.height* SCALE;
		rectangle(vew, roi, Scalar(0,200,200),1);
	}
}

static void refresh_view(struct UserBundle& bundle, int cx, int cy){
	Mat& ova = *(bundle.ova);
	Mat& vew = *(bundle.vew);
	ova.copyTo(vew);//clear previous view~~~

	draw_zone(bundle,vew);

	switch(bundle.c_type){
	case 'i'://insert mode
		rectangle(vew,
			Rect(
				cx-SCALE_BLOB_SIZE2, cy-SCALE_BLOB_SIZE2,
				SCALE_BLOB_SIZE, SCALE_BLOB_SIZE
			),
			Scalar(0,200,0), 1
		);
		break;
	case 'd'://delete mode
		line(vew,
			Point(cx-SCALE_BLOB_SIZE4, cy-SCALE_BLOB_SIZE4),
			Point(cx+SCALE_BLOB_SIZE4, cy+SCALE_BLOB_SIZE4),
			Scalar(0,0,200), 1
		);
		line(vew,
			Point(cx+SCALE_BLOB_SIZE4, cy-SCALE_BLOB_SIZE4),
			Point(cx-SCALE_BLOB_SIZE4, cy+SCALE_BLOB_SIZE4),
			Scalar(0,0,200), 1
		);
		break;
	default:
	case 'n'://normal mode
		break;
	}

	imshow(WIN_TITLE,vew);
}

static void remove_zone(struct UserBundle& bundle, int cx, int cy){
	Point cursor(cx,cy);
	list<Rect>::iterator itor;
	for(
		itor =bundle.zone.begin();
		itor!=bundle.zone.end();
		++itor
	){
		Rect roi = *itor;//just copy it!!!
		roi.x = roi.x * SCALE;
		roi.y = roi.y * SCALE;
		roi.width = roi.width * SCALE;
		roi.height= roi.height* SCALE;
		if(roi.contains(cursor)==true){
			bundle.zone.erase(itor);
			break;
		}
	}
}

static bool check_zone_existing(struct UserBundle& bundle, Rect& mark){
	list<Rect>::iterator itor;
	for(
		itor =bundle.zone.begin();
		itor!=bundle.zone.end();
		++itor
	){
		Rect zone = *itor;//just copy it!!!
		if((zone&mark).area()>0){
			return true;
		}
	}
	return false;
}

static void dump_cifra10_format(struct UserBundle& bundle, ofstream& out){

	Mat& src = *(bundle.src);
	Mat& ova = *(bundle.ova);
	Mat& vew = *(bundle.vew);

	ova.copyTo(vew);//clear previous view~~~
	imshow(WIN_TITLE,vew);

	int idx = 1;
	for(int yy=0; yy<=(src.rows-IMG_SIZE); yy+=IMG_SIZE){
		for(int xx=0; xx<=(src.cols-IMG_SIZE); xx+=IMG_SIZE){

			Rect mark(xx, yy, IMG_SIZE, IMG_SIZE);
			Mat roi = src(mark);
			ova.copyTo(vew);//clear previous view~~~

			if(check_zone_existing(bundle, mark)==false){
				Rect s_mark = mark;
				s_mark.x *= SCALE;
				s_mark.y *= SCALE;
				s_mark.width *= SCALE;
				s_mark.height*= SCALE;
				rectangle(vew, s_mark, Scalar(200,200,0), 5);
				out.put(idx);//1~64:Good part
				printf("%03d] --> OK\n",idx++);
			}else{
				out.put(0);//0:Not Good part
				printf("%03d] --> NG\n",idx++);
			}
			out.write((char*)roi.data, IMG_SIZE*IMG_SIZE);
			draw_zone(bundle,vew);//indicate that we skip this area
			imshow(WIN_TITLE,vew);

			waitKey(100);//just for delay~~~
		}
	}
}

static void draw_grid_number(struct UserBundle& bundle){
	Mat& src = *(bundle.src);
	Mat& ova = *(bundle.ova);
	Mat& vew = *(bundle.vew);
	ova.copyTo(vew);//clear previous view~~~
	int idx = 0;
	for(int yy=0; yy<=(src.rows-IMG_SIZE); yy+=IMG_SIZE){
		for(int xx=0; xx<=(src.cols-IMG_SIZE); xx+=IMG_SIZE){
			char buf[10];
			sprintf(buf,"%03d",idx);
			putText(vew,
				buf,
				Point(10+xx*SCALE+10, 15+yy*SCALE),
				FONT_HERSHEY_SIMPLEX,
				0.5,
				Scalar(200,200,0)
			);
			idx++;
		}
	}
	imshow(WIN_TITLE,vew);
}

static void eventOnMouse(int eid, int cx, int cy, int flags, void* userdata){

	struct UserBundle& bundle = *((UserBundle*)userdata);
	if(cx>=0 && cy>=0){
		bundle.cx = cx;
		bundle.cy = cy;
	}
	switch(eid){
	case EVENT_RBUTTONDOWN:
	case EVENT_RBUTTONUP:
		draw_grid_number(bundle);//show grid number~~~
		return;
	case EVENT_LBUTTONDOWN:
		return;
	case EVENT_LBUTTONUP:
		switch(bundle.c_type){
		case 'i'://insert mode
			bundle.zone.push_back(Rect(
				(cx-SCALE_BLOB_SIZE2)/SCALE,
				(cy-SCALE_BLOB_SIZE2)/SCALE,
				BLOB_SIZE, BLOB_SIZE
			));
			break;
		case 'd'://delete mode
			remove_zone(bundle, cx, cy);
			break;
		}
		break;
	case EVENT_MOUSEMOVE:
		break;
	}
	refresh_view(bundle, cx, cy);
}

int main(int argc, char* argv[]){
	const char* IMG_PATH="./wafer/dies";
	const char* OUT_NAME="./data-cifar10/die_batch.bin";
	//const char* OUT_NAME="./data-cifar10/die_test.bin";

	int lstSize = 0;
	char** lstName = listFileName(IMG_PATH, ".png", &lstSize);

	static int key = 0;
	static int idx = 79;

	struct UserBundle bundle;
	bundle.c_type = 'i';

	ofstream out(OUT_NAME,ios::out|ios::binary);

	namedWindow(WIN_TITLE, WINDOW_NORMAL);

	for(;key!=0x001B;){

		char bufName[200];
		sprintf(bufName,"%s/%s",IMG_PATH, lstName[idx]);
		cout<<"load image : "<<lstName[idx]<<endl;

		Mat src = imread(bufName,IMREAD_GRAYSCALE);
		Mat ova = imread(bufName,IMREAD_COLOR);
		Mat vew;

		resize(ova, ova, ova.size()*SCALE);
		resizeWindow(WIN_TITLE,
			ova.size().width,
			ova.size().height
		);

		bundle.src = &src;
		bundle.ova = &ova;
		bundle.vew = &vew;

		setMouseCallback(WIN_TITLE, eventOnMouse, &bundle);

		eventOnMouse(-1, -1, -1, -1, &bundle);

		key = waitKey();
		//printf("key=0x%04X\n",key);
		key = key & 0xFFFF;
		switch(key){
		case 0xFFBE://key F1, F2 is 0xFFBF, F12 is 0xFFC9, so on...
			//--insert mode--
			bundle.c_type = 'i';
			break;
		case 0xFFBF://key F2
			//--delete mode--
			bundle.c_type = 'd';
			break;
		case 0xFFC0://key F3
			//--normal mode--
			bundle.c_type = 'n';
			break;
		case 0xFFC1://key F4
			//--dump mode--
			dump_cifra10_format(bundle,out);
			break;
		case 0xFF51://key left
			//swipe to previous image file~~~
			if((idx-1)>=0){
				idx-=1;
			}
			bundle.zone.clear();
			break;
		case 0xFF52://key up
			break;
		case 0xFF53://key right
			//swipe to next image file~~~
			if(idx<lstSize){
				idx+=1;
			}
			bundle.zone.clear();
			break;
		case 0xFF54://key down
			break;
		}

		eventOnMouse(-1, -1, -1, -1, &bundle);
	}

	out.close();
	cout<<"Done~~~"<<endl;
	destroyWindow(WIN_TITLE);

	delete[] lstName;
	return 0;
}
//--------------------------------------//

void gen_sample_die(char* outName, char* lstName[]){

	ofstream out(outName,ios::out|ios::binary);

	int idx = 0;

	char* name = lstName[idx];

	const int ROI_WIDTH = 32;
	const int ROI_HEIGHT= 32;

	int total = 0;
	while(name!=NULL){

		Mat ref = imread(name,IMREAD_GRAYSCALE);

		int tkn = 1;
		for(int j=0; j<ref.rows/ROI_HEIGHT; j++){

			for(int i=0; i<ref.cols/ROI_WIDTH; i++){

				out.put(tkn++);

				Mat roi = ref(Rect(
					i*ROI_WIDTH,j*ROI_HEIGHT,
					ROI_WIDTH,ROI_HEIGHT
				));

				out.write((char*)roi.data, ROI_WIDTH*ROI_HEIGHT);
				total++;
			}
		}
		cout<<"Token="<<tkn<<endl;

		name = lstName[++idx];
	}
	cout<<"Total data = "<<total<<endl;
	out.close();
}

void gen_cifra10_sample(const char* name){

	const int COUNT=5000;

	const int WW= 32;
	const int HH= 32;

	Mat img = Mat::zeros(HH, WW, CV_8UC1);

	ofstream out(name,ios::out|ios::binary);

	for(int i=0; i<COUNT; i++){

		if(rand()%2==0){
			//draw circle
			out.put(0);
			int xx = WW/2 + rand()%12 - 6;
			int yy = HH/2 + rand()%12 - 6;
			int rad= 6 + rand()%(6);
			int thk= rand()%2+1;
			circle(img, Point(xx,yy), rad, Scalar::all(255), thk);
		}else{
			//draw line
			out.put(1);
			int a1 = rand()%(WW);
			int a2 = rand()%(HH);
			int b1 = rand()%(WW-5)+5;
			int b2 = rand()%(HH-5)+5;
			b1 = a1 + b1 - WW/2;
			b2 = a2 + b2 - HH/2;
			int thk= rand()%2+1;
			line(img,Point(a1,a2), Point(b1,b2), Scalar::all(255), thk);
		}

		out.write((char*)img.data, WW*HH);

		printf("progress %04d/%04d\n", i, COUNT);

		img = img * 0;//clear image, for next turn~~~
	}

	out.close();
}

int main2(int argc, char* argv[]){
	srand (time(NULL));
	gen_cifra10_sample("./data-cifar10/die_batch.bin");
	gen_cifra10_sample("./data-cifar10/die_test.bin");
	return 0;
}




