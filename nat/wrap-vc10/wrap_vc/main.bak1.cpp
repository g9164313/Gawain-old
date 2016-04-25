#include <global.hpp>
#include <dirent.h>
#include <IX_TempEvaluation.h>

using namespace std;
using namespace cv;
const char* logName="gg.sci";
ofstream logFile;

double valInit=-1.;

extern double lightGetHxS(Mat& src);
extern int IXGlucoseLocation(Mat& gray_image,Point& pos);

//string oneInt=".\\sample.7\\21\\myfilename001.tiff";
string oneInt="gg00.png";
string oneOut="dump.png";
void test_one(string inName, string outName){
	Mat ova = imread(inName);

	Mat roi=ova(Rect(614,308,120,120));
	double valGluo = lightGetHxS(roi);
	double ratio=1.;
	if(valInit<0){
		valInit = valGluo;
		ratio = 1.;
	}else{
		ratio = valGluo / valInit;
	}
	printf("%0.3f,%0.3f;\n",valGluo,ratio);
	logFile<<valGluo<<","<<ratio<<","<<endl;
}

string plurIn="./sample.2";
string plurOut="./aaa";
void test_plural(string inName,string outName){
	DIR* dir;
	dir = opendir(inName.c_str());
	if (!dir) {
		cout<<"fail to open...."<<endl;
		exit(EXIT_FAILURE);
	}

	logFile.open(logName,ios::app);
	logFile<<outName<<"=["<<endl;

	while(1){
		struct dirent * entry;
		entry = readdir(dir);
		if(!entry){
			break;
		}
		if(entry->d_type!=DT_REG){
			continue;
		}
		//check appendx
		if(
			strstr(entry->d_name,".tiff")==NULL &&
			strstr(entry->d_name,".png")==NULL &&
			strstr(entry->d_name,".jpg")==NULL &&
			strstr(entry->d_name,".bmp")==NULL
		){
			continue;
		}
		string _in="", _out="";
		_in = _in+inName+"/"+entry->d_name;
		//_out = _out+outName+"/"+entry->d_name;
		_out = outName;
		cout<<"process "+_in<<endl;
		test_one(_in,_out);
	}

	valInit = -1;//reset this variable~~
	cout<<endl;
	logFile<<"];"<<endl;
	logFile.close();

	if (closedir(dir)) {
		cout<<"fail to close"<<endl;
		exit(EXIT_FAILURE);
	}
}

void test_finddir(string inName){
	DIR* dir;
	dir = opendir(inName.c_str());
	if (!dir) {
		cout<<"fail to open...."<<endl;
		exit(EXIT_FAILURE);
	}

	String prefix=inName.substr(inName.find_last_of("/\\")+1);
	int index=1;
	cout<<"find "<<inName<<endl;
	while(1){
		struct dirent * entry;
		entry = readdir(dir);
		if(!entry){
			break;
		}
		if(entry->d_type!=DT_DIR){
			continue;
		}
		if(
			strcmp(entry->d_name,".")==0 ||
			strcmp(entry->d_name,"..")==0
		){
			continue;
		}
		cout<<" --"<<entry->d_name<<endl;

		char _in[200],_out[64];
		sprintf(_in,"%s/%s",inName.c_str(),entry->d_name);
		sprintf(_out,"gg%sx%d",prefix.c_str(),index);
		test_plural(_in,_out);
		index++;
	}
	closedir(dir);
	cout<<endl<<"!!!COMPLETE!!!"<<endl<<endl;
}

void append_command(){
	logFile.open(logName,ios::app);
	logFile<<"gg0=[gg0x1($,1),gg0x2($,1),gg0x3($,1),gg0x4($,1),gg0x5($,1),gg0x6($,1),gg0x7($,1),gg0x8($,1)];"<<endl;
	logFile<<"gg50=[gg50x1($,1),gg50x2($,1),gg50x3($,1),gg50x4($,1),gg50x5($,1),gg50x6($,1),gg50x7($,1),gg50x8($,1)];"<<endl;
	logFile<<"gg100=[gg100x1($,1),gg100x2($,1),gg100x3($,1),gg100x4($,1),gg100x5($,1),gg100x6($,1),gg100x7($,1),gg100x8($,1)];"<<endl;
	logFile<<"gg150=[gg150x1($,1),gg150x2($,1),gg150x3($,1),gg150x4($,1),gg150x5($,1),gg150x6($,1),gg150x7($,1),gg150x8($,1)];"<<endl;
	logFile<<"gg200=[gg200x1($,1),gg200x2($,1),gg200x3($,1),gg200x4($,1),gg200x5($,1),gg200x6($,1),gg200x7($,1),gg200x8($,1)];"<<endl;
	logFile<<"gg250=[gg250x1($,1),gg250x2($,1),gg250x3($,1),gg250x4($,1),gg250x5($,1),gg250x6($,1),gg250x7($,1),gg250x8($,1)];"<<endl;
	logFile<<"gg300=[gg300x1($,1),gg300x2($,1),gg300x3($,1),gg300x4($,1),gg300x5($,1),gg300x6($,1),gg300x7($,1),gg300x8($,1)];"<<endl;
	logFile<<"gg350=[gg350x1($,1),gg350x2($,1),gg350x3($,1),gg350x4($,1),gg350x5($,1),gg350x6($,1),gg350x7($,1),gg350x8($,1)];"<<endl;
	logFile<<"gg400=[gg400x1($,1),gg400x2($,1),gg400x3($,1),gg400x4($,1),gg400x5($,1),gg400x6($,1),gg400x7($,1),gg400x8($,1)];"<<endl;
	logFile<<"gg450=[gg450x1($,1),gg450x2($,1),gg450x3($,1),gg450x4($,1),gg450x5($,1),gg450x6($,1),gg450x7($,1),gg450x8($,1)];"<<endl;
	logFile<<"gg500=[gg500x1($,1),gg500x2($,1),gg500x3($,1),gg500x4($,1),gg500x5($,1),gg500x6($,1),gg500x7($,1),gg500x8($,1)];"<<endl;
	logFile.close();
}


int main(int argc, char* argv[]) {
	remove(logName);

	//timeval t1, t2;
	//float time;
	//gettimeofday(&t1, NULL);

	for(int i=0; i<=500; i+=50){
		char dir_name[100];
		sprintf(dir_name,"./sample.3/%d",i);
		test_finddir(dir_name);
	}
	append_command();

	//test_plural("./sample.3/50/iPhone004-20150114-17.00.40[1]-longTerm","g50_1");
	//test_one(oneInt,oneOut);

	//time = (float)(t2.tv_sec-t1.tv_sec)*1000;
	//time += (float)(t2.tv_usec-t1.tv_usec)/1000;
	//printf("\n elapse=%.2fms \n",time);
	return 0;
}

/*
  LARGE_INTEGER frequency;        // ticks per second
    LARGE_INTEGER t1, t2;           // ticks
    double elapsedTime;

    // get ticks per second
    QueryPerformanceFrequency(&frequency);

    // start timer
    QueryPerformanceCounter(&t1);

    // do something
    // ...

    // stop timer
    QueryPerformanceCounter(&t2);

    // compute and print the elapsed time in millisec
    elapsedTime = (t2.QuadPart - t1.QuadPart) * 1000.0 / frequency.QuadPart;
    cout << elapsedTime << " ms.\n";
 */

/*
 * 	imwrite("cc.0.png",out);
	Scharr(out, dxx, CV_32FC1, 1, 0);
	Scharr(out, dyy, CV_32FC1, 0, 1);
	cartToPolar(dxx,dyy,mag,ang);
	double min,max;
	minMaxLoc(
		mag,
		&min,&max,
		NULL,NULL
	);
	normalize(mag,mag,0,255,NORM_MINMAX);
	imwrite("cc.1.png",mag);

	for(int j=0; j<ang.rows; j++){
		for(int i=0; i<ang.cols; i++){
			float v = ang.at<float>(j,i);
			if(v<(M_PI/2.)){
				ang.at<float>(j,i) = 0;
			}else if(v<(M_PI)){
				ang.at<float>(j,i) = 128;
			}else if(v<(M_PI*1.5)){
				ang.at<float>(j,i) = 128;
			}else{
				ang.at<float>(j,i) = 0;
			}
		}
	}
	imwrite("cc.2.png",ang);
 */


//extern float focusFFT(Mat& src,int resp);
//Mat src = imread("./pic/snap0.1.png",IMREAD_GRAYSCALE);
//float val = focusFFT(src,128);
//printf("res=%.2f\n",val);

//crop -> (93,84) @ 1638x1092
//int geom[8] = {
//		93,84,
//		1638,1092,
//		6,4,
//		273,273
//};

/*
 * Mat src[2] = {
		imread("./pic/test7.png",IMREAD_GRAYSCALE),
		imread("./pic/test8.png",IMREAD_GRAYSCALE),
	};
	Mat hann;
	createHanningWindow(
		hann,
		src[0].size(),
		CV_32F
	);
	double resp;
	Point pos = phaseCorrelateRes(
		Mat_<float>(src[0]),
		Mat_<float>(src[1]),
		hann,
		&resp
	);
*/
//crossFreq(a,b,c);
/*int rows = (a.rows>b.rows)?(a.rows):(b.rows);
int cols = (a.cols>b.cols)?(a.cols):(b.cols);
copyMakeBorder(
	a, a,
	0,rows-a.rows,
	0,cols-a.cols,
	BORDER_CONSTANT,
	Scalar::all(0)
);
double resp;
Point pos = phaseCorrelateRes(Mat_<float>(a),Mat_<float>(b),Mat(),&resp);*/
//magFreq(c,c);
//imwrite("gg.png",img);

//cvtFreq(b,c);
//idft(c,out,DFT_SCALE|DFT_REAL_OUTPUT);
//double min,max;
//minMaxLoc(out,&min,&max);
//out.convertTo(d,CV_8U);
//minMaxLoc(d,&min,&max);
//imwrite("res1.png",d);
//cout<<"pos=("<<pos.x<<","<<pos.y<<")"<<endl;

/*

double unsharpSigma = 4.3f;
double unsharpAmount = 0.2;
int unsharpIter = 2;

Mat unsharpMaskEx(Mat& src){
	//this process for dark-field~~~
	Mat msk,ref;
	src.copyTo(msk);
	double scale;
	minMaxLoc(src,NULL,&scale);
	scale = scale * unsharpAmount;
	for(int i=0; i<unsharpIter; i++){
		GaussianBlur(msk, ref, Size(), unsharpSigma, unsharpSigma);
		msk = msk * (1 + scale) + ref * (-scale);
	}
	msk.copyTo(ref);
	//bilateralFilter(msk,ref,10,70,70);
	//threshold(ref,ref,0,255,THRESH_BINARY|THRESH_OTSU);
	return ref;
}


 morphologyEx(
		a,
		node,
		CV_MOP_OPEN,
		getStructuringElement(MORPH_CROSS, ELEMENT_SIZE)
	);
	double min,max;
	minMaxLoc(node,&min,&max);
	threshold(node,node,(max*90)/100,255,THRESH_BINARY);
	vector<vector<Point> > cont;
	findContours(
		node,
		cont,
		CV_RETR_EXTERNAL,
		CV_CHAIN_APPROX_SIMPLE
	);
	list<Point> dots;
	for(size_t i=0; i<cont.size(); i++){
		RotatedRect box = minAreaRect(cont[i]);
		Point cts(box.center.x,box.center.y);
		//sort point!!!
		list<Point>::iterator it;
		for(it=dots.begin(); it!=dots.end(); it++){
			if(cts.y>=(*it).y){
				continue;
			}
			it--;
			for(; it!=dots.begin(); --it){

			}
			break;
		}

		circle(
			b,
			pts,
			5,
			Scalar(0,255,0),
			-1
		);
		char buff[100];
		sprintf(buff,"%d",(int)i);
		string txt=buff;
		putText(
			b,
			txt,
			pts,
			FONT_HERSHEY_SIMPLEX,
			2.,
			Scalar(0,255,100)
		);
		drawContours(
			b,
			cont,
			(int)i,
			Scalar(0,255,0),
			3
		);
	}
	imwrite("res.png",b);
*/
