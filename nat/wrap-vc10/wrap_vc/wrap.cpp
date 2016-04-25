#include <global.hpp>
#include <dirent.h>

using namespace std;
using namespace cv;

ofstream logFile;

//string oneInt=".\\sample.7\\21\\myfilename001.tiff";
//string oneInt="ttt.png";
/*void test_one(string inName){
	Mat ref = imread("gg.0.png",IMREAD_GRAYSCALE);
	Mat src = imread("gg.15.png",IMREAD_GRAYSCALE);
	Mat out;
	registeration(src,ref,NULL);	
	addWeighted(src,0.3,ref,0.7,0.,out);
	imwrite("cc.0.png",ref);
	imwrite("cc.1.png",out);
	return;
}*/

static Mat histCube;
void test_one(string inName, int idx){
	Mat src = imread(inName);
	
	Mat roi,_roi = src(Rect(222,194,85,85));

	//fastNlMeansDenoisingColored(_roi,roi);
	roi = _roi;

	/*Scalar rgb,hue;	
	meanStdDev(roi,rgb,Scalar());

	Mat tmp,node;
	roi.convertTo(tmp,CV_32F);
	tmp = tmp / 255.;
	cvtColor(tmp,node,COLOR_BGR2HSV);
	meanStdDev(node,hue,Scalar());

	char buff[500];
	sprintf(buff,"%2d  %.2f  %.2f  %.2f,   %.2f  %.2f  %.2f\n",
		idx,
		rgb[2],rgb[1],rgb[0],
		hue[0],hue[1],hue[2]
	);
	logFile<<buff;
	if(idx%logIndex==0){
		logFile<<endl<<endl;
	}*/

	//area = roi.cols * roi.rows;
	const float bit8[] = {0,256};
	const float* rang[] = {bit8,bit8,bit8};
	int bins[] = {256,256,256};
	int chan[] = {2,1,0};
	Mat hist[3];

	chan[0] = 2;
	calcHist(
		&roi, 1, 
		chan, 
		Mat(), 
		hist[0], 1, 
		bins, rang
	);

	chan[0] = 1;
	calcHist(
		&roi, 1, 
		chan, 
		Mat(), 
		hist[1], 1, 
		bins, rang
	);

	chan[0] = 0;
	calcHist(
		&roi, 1, 
		chan, 
		Mat(), 
		hist[2], 1, 
		bins, rang
	);
	
	for(int j=0; j<hist[0].rows; j++){
		char buff[500];
		sprintf(buff,"%.2f  %.2f  %.2f\n",
			hist[0].at<float>(j,0),
			hist[1].at<float>(j,0),
			hist[2].at<float>(j,0)
		);
		logFile<<buff;
	}
	return;
}

string plurIn="./sample/test";
int test_plural(string inName,bool count,int skip){
	int cnt = 0;
	DIR* dir;
	dir = opendir(inName.c_str());
	if (!dir) {
		cout<<"fail to open...."<<endl;
		exit(EXIT_FAILURE);
		return cnt;
	}
	if(count==false){
		cout<<"===["<<inName<<"]==="<<endl;
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
		//check appendx
		if(
			strstr(entry->d_name,".tiff")==NULL &&
			strstr(entry->d_name,".TIFF")==NULL &&
			strstr(entry->d_name,".png")==NULL &&
			strstr(entry->d_name,".jpg")==NULL &&
			strstr(entry->d_name,".bmp")==NULL
		){
			continue;
		}
		if(count==false){
			if(skip>0){
				skip--;
				continue;
			}
			string _in="";
			_in = _in+inName+"\\"+entry->d_name;
			cout<<"process "+_in<<endl;
			test_one(_in, ++cnt);
		}else{
			cnt++;
		}
	}
	if (closedir(dir)) {
		cout<<"fail to close"<<endl;
		exit(EXIT_FAILURE);
	}
	return cnt;
}

static int fileCount=0;
static vector<string> dirName;
int test_plural_dir(string inName,bool count=false){
	int cnt = 0;
	DIR* dir;
	dir = opendir(inName.c_str());
	if (!dir) {
		cout<<"fail to open...."<<endl;
		exit(EXIT_FAILURE);
		return cnt;
	}
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
			strcmp(entry->d_name,".")==0||
			strcmp(entry->d_name,"..")==0
		){
			continue;
		}
		if(count==false){
			cout<<"find "<<entry->d_name<<endl;
			dirName.push_back(string(entry->d_name));
		}else{
			cnt++;
		}
	}
	if (closedir(dir)) {
		cout<<"fail to close"<<endl;
		exit(EXIT_FAILURE);
	}
	return cnt;
}



