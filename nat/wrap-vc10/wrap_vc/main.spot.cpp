#include <global.hpp>
#include <dirent.h>


using namespace cv;
using namespace std;

#define MAX_MATS 30
#define POP_SIZE 10
#define SAT_BIN (histBins[0])
#define HUE_BIN (histBins[1])

const int chan[] = { 1, 0 };
const int histBins[] = { 10, 360 };
const float s_range[] = { 0.f, 1.f };
const float h_range[] = { 0.f, 360.f };
const float* histRange[] = { s_range, h_range };

//void getPopulation(vector<Mat>& img, Mat& pop);


#define WIDTH 1280
#define HEIGHT 960
#define SIZE (WIDTH*HEIGHT)


#define BLK_SIZE 16
#define BLK_AREA (BLK_SIZE*BLK_SIZE)

void getBlock(
	uint8_t* yy,
	int32_t x, int32_t y,
	int32_t w, int32_t h,
	Mat& blk
){
	uint8_t tmp[BLK_AREA*2];
	int32_t aa = w*h;
	//YV12~~~
	for(int j=0; j<BLK_SIZE; j++){
		for(int i=0; i<BLK_SIZE; i++){
			int k = i + j*BLK_SIZE;
			int p = (x+i) + (y+j)*w;
			tmp[k] = yy[p];
			int m = i + (j>>1)*BLK_SIZE;
			int n = (x+i) + ((y+j)>>1)*w;
			tmp[BLK_AREA+m] = yy[aa+n];
		}
	}
	Mat _tmp((BLK_SIZE*3)/2,BLK_SIZE,CV_8UC1,tmp);
	cvtColor(_tmp,blk,CV_YUV420sp2BGR);
	//YV12~~~
	return;
}

void projByStddev(
	vector<uint8_t*>& img,
	int w, int h,
	Mat proj[]
){
	int fps = img.size();

	for(int j=0; j<h; j+=BLK_SIZE){
		for(int i=0; i<w; i+=BLK_SIZE){

			Mat blk(BLK_SIZE,BLK_SIZE,CV_8UC3);
			Mat seq(BLK_AREA,fps,CV_8UC3);

			for(int k=0; k<fps; k++){
				uint8_t* ptr = img[k];
				getBlock(ptr,i,j,w,h,blk);
				for(int n=0; n<BLK_SIZE; n++){
					for(int m=0; m<BLK_SIZE; m++){
						seq.at<Vec3b>(m+n*BLK_SIZE,k) = blk.at<Vec3b>(n,m);
					}
				}
			}

			for(int k=0; k<BLK_AREA; k++){
				Scalar avg, dev;
				meanStdDev(seq.row(k),avg,dev);
				proj[0].at<float>(j+k/BLK_SIZE,i+k%BLK_SIZE) = avg[0]/dev[0];
				proj[1].at<float>(j+k/BLK_SIZE,i+k%BLK_SIZE) = avg[0]/dev[1];
				proj[2].at<float>(j+k/BLK_SIZE,i+k%BLK_SIZE) = avg[0]/dev[2];
			}
		}
	}
}

void get_planar(
	vector<Mat>& lst,
	uint8_t* color,
	int w, int h,
	int off
){
	uint8_t* buf = new uint8_t[(w*h)/2];
	Mat img(h,w/2,CV_8UC1,buf);
	for(int j=0; j<h; j++){
		for(int i=off; i<w; i+=2){
			buf[j*(w/2) + (i/2)] = color[j*w + i];
		}
	}
	equalizeHist(img,img);
	lst.push_back(img);
}


void projByStddev2(
	vector<uint8_t*>& buf,
	int w, int h,
	Mat proj[]
){
	vector<Mat> yy;
	vector<Mat> cb;
	vector<Mat> cr;

	int fps = buf.size();

	for(int k=0; k<fps; k++){
		uint8_t* ptr = buf[k];
		Mat gray(h,w,CV_8UC1,ptr);
		//equalizeHist(gray,gray);
		yy.push_back(gray);
		ptr = ptr + (w*h);
		get_planar(cb,ptr,w,h/2,0);
		get_planar(cr,ptr,w,h/2,1);
	}

	for(int j=0; j<h; j++){
		for(int i=0; i<w; i++){
			Mat val(1,fps,CV_32FC3);
			for(int k=0; k<fps; k++){
				Vec3f _v;
				_v[0] = yy[k].at<uint8_t>(j,i);
				_v[1] = cb[k].at<uint8_t>(j>>1,i>>1);
				_v[2] = cr[k].at<uint8_t>(j>>1,i>>1);
				val.at<Vec3f>(0,k) = _v;
			}
			Scalar avg, dev;
			meanStdDev(val,avg,dev);
			proj[0].at<float>(j,i) = dev[0];
			proj[1].at<float>(j>>1,i>>1) = dev[1];
			proj[2].at<float>(j>>1,i>>1) = dev[2];
		}
	}
}

uint8_t* convert_raw(const char* name, int w, int h){

	int size = (w*h*3)/2;
	uint8_t* buf = new uint8_t[size];

	ifstream fd(name,ifstream::binary);
	fd.read((char*)buf,size);
	fd.close();

	Mat src((h*3)/2,w,CV_8UC1,buf);
	Mat dst(h,w,CV_8UC3);
	cvtColor(src,dst,CV_YUV420sp2BGR);
	imwrite("result1.png",dst);
	cout<<"done!!!"<<endl;
	return buf;
}

void gen_list(
	const char* fmt,
	int idx, int cnt,
	vector<uint8_t*>& lst
){
	char name[100];
	for(int i=0; i<cnt; i++){
		sprintf(name,fmt,idx,i);
		ifstream fd(name,ios::binary|ios::ate);
		uint32_t size = fd.tellg();
		uint8_t* buf = new uint8_t[size];
		fd.seekg(0,fd.beg);
		fd.read((char*)buf,size);
		fd.close();
		lst.push_back(buf);
	}
}

void clear_list(vector<uint8_t*>& lst){
	for(int i=0; i<lst.size(); i++){
		delete lst[i];
	}
	lst.clear();
}

int main(int argc, char* argv[]) {


	Mat proj[] = {
		Mat(HEIGHT,WIDTH,CV_32FC1),
		Mat(HEIGHT/2,WIDTH/2,CV_32FC1),
		Mat(HEIGHT/2,WIDTH/2,CV_32FC1)
	};

	for(int vol=0; vol<4; vol++){

		vector<uint8_t*> img;

		gen_list("./acer/aa/img%02d.%02d.raw",vol,60,img);

		projByStddev2(img,WIDTH,HEIGHT,proj);

		//ofstream fd;
		//fd.open("res0.txt");
		//fd<<res0<<endl;
		//fd.close();
		for(int i=0; i<3; i++){
			char name[64];
			sprintf(name,"result%02d.%02d.png",i,vol);
			Mat dst, map;

			map.type();
			normalize(proj[i],dst,0,255,NORM_MINMAX,CV_8UC1);
			applyColorMap(dst, map, COLORMAP_JET);
			imwrite(name,map);
			cout<<"dump map("<<name<<")"<<endl;
		}

		clear_list(img);
	}

	cout<<"done!!!"<<endl;
	return 0;
}

/*
int main(int argc, char* argv[]) {
	img_roi.x = atoi(argv[1]);
	img_roi.y = atoi(argv[2]);
	img_roi.width = atoi(argv[3]);
	img_roi.height = atoi(argv[4]);

	int period = 15;

	DIR * dir;
	dir = opendir(argv[5]);
	if (!dir) {
		cout<<"fail to open...."<<endl;
		exit(EXIT_FAILURE);
	}
	while(1){
		struct dirent * entry;
		entry = readdir(dir);
		if(!entry){
			break;
		}
		if(entry->d_type!=DT_REG){
			continue;
		}
		//printf("read %s\n", entry->d_name);

		String fll_path = argv[5];

		fll_path = fll_path + "/" + + entry->d_name;

		img_mat.push_back(imread(fll_path.c_str()));

		if(img_mat.size()>=period){
			Mat pop = Mat::zeros(1,POP_SIZE,CV_32F);
			getPopulation(img_mat,pop);
			cout<<pop<<endl;
			img_mat.clear();
		}
	}
	if (closedir(dir)) {
		cout<<"fail to close"<<endl;
		exit(EXIT_FAILURE);
	}

	return 0;
}

void getPopulation(vector<Mat>& img, Mat& pop) {

	int i, fps = img.size();

	Mat src[MAX_MATS];
	if (fps >= MAX_MATS) {
		fps = MAX_MATS;
	}
	for (i = 0; i < fps; i++) {
		Mat hsv, tmp, roi = img[i](img_roi);
		roi.convertTo(tmp, CV_32F);
		tmp *= (1. / 255.);
		cvtColor(tmp, hsv, CV_BGR2HSV);
		src[i] = hsv;
		char name[100];
		 sprintf(name,"/mnt/sdcard/Download/snap.%02d.png",i);
		 imwrite(name,roi);
	}
	Mat hist;
	calcHist(src, fps, chan, Mat(), hist, 2, histBins, histRange);

	Mat accum = Mat::zeros(1, HUE_BIN, CV_32F);
	i = (SAT_BIN * 1) / 2;
	for (; i < SAT_BIN; i++) {
		accum = accum + hist.row(i);
	}

	for (i = 0; i < POP_SIZE; i++) {
		Point pos;
		minMaxLoc(accum, NULL, NULL, NULL, &pos);
		pop.at<float>(0, i) = pos.x;
		accum.at<float>(0, pos.x) = 0.f; //reset it!!!!
	}

	String txt = "pop=[";
	for (i = 0; i < POP_SIZE; i++) {
		char tmp[60];
		int val = pop.at<float>(0, i);
		sprintf(tmp, "%3d,", val);
		txt = txt + tmp;
	}
}
*/

