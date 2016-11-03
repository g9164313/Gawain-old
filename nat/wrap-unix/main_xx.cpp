#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <global.hpp>
#include <utils_ipc.hpp>
#include <algorithm>

/**
 * Parameter for AOI. Their meanings are : <p>
 * 0: Binary Threshold.<p>
 * 1: Canny Threshold.<p>
 * 2: Canny Threshold, but only offset value.<p>
 * 3: Canny Aperture.<p>
 * 4: Dilate Size.<p>
 * 5: Approximates Epsilon.<p>
 */
#define PARAM_SIZE 6
static int param[PARAM_SIZE] = {
	100,
	300,50,5,
	5,7
};

static Mat getEdge(Mat& img,vector<vector<Point> >& cts){
	Mat nod1,nod2;
	Canny(
		img,nod1,
		param[1],
		param[1]+param[2],
		param[3],
		true
	);
	if(param[4]!=1){
		Mat kern = getStructuringElement(
			MORPH_ELLIPSE,
			Size(param[4],param[4]),
			Point(-1,-1)
		);
		dilate(nod1,nod2,kern);
	}else{
		nod2 = nod1;
	}
	Mat tmp;
	nod2.copyTo(tmp);
	circle(tmp,Point(568,464),25,Scalar::all(0),-1);
	findContours(
		tmp,cts,
		RETR_LIST,CHAIN_APPROX_SIMPLE
	);
	return nod2;
}

extern void drawImage(Mat& overlay,const Mat& src);
extern void drawEdgeMap(Mat& overlay,const Mat& edge);
extern void drawContour(
	Mat& overlay,
	vector<vector<Point> >& cts
);
extern void drawRectangle(
	Mat& overlay,
	Point center,
	int width,
	int height,
	const Scalar& color,
	int thickness=1,
	int lineType=LINE_8
);
extern void drawPolyline(
	Mat& overlay,
	vector<Point>& cts,
	bool closed,
	int thickness=1,
	int lineType=LINE_8
);

void init_shape(vector<Point>& sh1, vector<Point>& sh2){
	sh1.push_back(Point(0  ,0  ));
	sh1.push_back(Point(100,0  ));
	sh1.push_back(Point(100,100));
	sh1.push_back(Point(0  ,100));
	const int wh=35,dp=20;
	sh2.push_back(Point(wh      , 0       ));
	sh2.push_back(Point(wh+dp   , 0       ));
	sh2.push_back(Point(wh+dp   , wh      ));
	sh2.push_back(Point(wh+dp+wh, wh      ));
	sh2.push_back(Point(wh+dp+wh, wh+dp   ));
	sh2.push_back(Point(wh+dp   , wh+dp   ));
	sh2.push_back(Point(wh+dp   , wh+dp+wh));
	sh2.push_back(Point(wh      , wh+dp+wh));
	sh2.push_back(Point(wh      , wh+dp   ));
	sh2.push_back(Point(0       , wh+dp   ));
	sh2.push_back(Point(0       , wh      ));
	sh2.push_back(Point(wh      , wh      ));
}

void filterHanning(Mat& src,const int quarter){
	Mat _src;
	src.convertTo(_src,CV_32FC1);
	Size area;
	if(quarter<=0 || 4<quarter){
		area.width = src.cols;
		area.height= src.rows;
	}else{
		area.width = src.cols*2;
		area.height= src.rows*2;
	}
	Mat hann,_han;
	createHanningWindow(hann,area,CV_32F);
	switch(quarter){
	case 1:
		_han = hann(Rect(
			src.cols,0,
			src.cols,src.rows
		));
		break;
	case 2:
		_han = hann(Rect(
			0,0,
			src.cols,src.rows
		));
		break;
	case 3:
		_han = hann(Rect(
			0,src.rows,
			src.cols,src.rows
		));
		break;
	case 4:
		_han = hann(Rect(
			src.cols,src.rows,
			src.cols,src.rows
		));
		break;
	default:
		_han = hann;
		break;
	}
	multiply(_src,_han,_src);
	_src.convertTo(src,CV_8UC1);
	//normalize(_src,src,0,255,NORM_MINMAX,CV_8UC1);
}

int main(int argc, char* argv[]) {
	//const char* name = "./gg2/16138125-2016-09-20-154907.pgm";
	//const char* name = "./gg3/snap_fail1.png";
	const char* name = "./aa1.png";

	Mat img = imread(name,IMREAD_GRAYSCALE);
	Mat ova = Mat::zeros(img.size(),CV_8UC4);
	drawImage(ova,img);

	vector<Point> shaprRect,shapeCross;
	init_shape(shaprRect,shapeCross);


	equalizeHist(img,img);
	imwrite("cc1.png",img);
	//threshold(img,img,100,255,THRESH_BINARY);
	//imwrite("cc2.png",img);

	/*int rw,dw;
	rw=550; dw=90;
	Mat rectA = Mat::zeros(rw,rw,CV_8UC1);
	rectangle(rectA,Rect(dw/2,dw/2,rw-dw,rw-dw),Scalar::all(255),dw);

	rw=150; dw=20;
	Mat rectB = Mat::zeros(rw,rw,CV_8UC1);
	rectangle(rectB,Rect(dw/2,dw/2,rw-dw,rw-dw),Scalar::all(255),dw);


	Mat temp = rectA;
	Mat result(
		img.cols-temp.cols+1,
		img.rows-temp.rows+1,
		CV_32FC1
	);
	matchTemplate(
		img,temp,
		result,
		CV_TM_CCORR_NORMED
	);

	double valMin, valMax;
	Point posMin, posMax;
	minMaxLoc(
		result,
		&valMin,&valMax,
		&posMin,&posMax
	);
	cout<<"min="<<valMin<<" @ "<<posMin<<endl;
	cout<<"max="<<valMax<<" @ "<<posMax<<endl;

	posMax.x = posMax.x + temp.cols/2;
	posMax.y = posMax.y + temp.cols/2;

	drawRectangle(ova,posMin,30,30,Scalar(0,0,255),3);//red
	drawRectangle(ova,posMax,30,30,Scalar(255,0,0),3);//blue
	*/

	/*Mat nod1;// = getEdge(img,cts);
	threshold(img,nod1,param[0],255,THRESH_BINARY);
	Mat kern = getStructuringElement(
		MORPH_RECT,
		Size(param[4],param[4]),
		Point(-1,-1)
	);
	erode(nod1,nod1,kern);
	//drawEdgeMap(ova,nod1);
	//imwrite("./gg3/cc2.png",ova);
	//return -1;

	vector<vector<Point> > cts;
	findContours(
		nod1,cts,
		RETR_LIST,CHAIN_APPROX_SIMPLE
	);

	double score=-1.;
	for(int i=0; i<cts.size(); i++){
		vector<Point> approx;
		Mat points(cts[i]);
		approxPolyDP(points, approx, param[5], true);
		if(approx.size()<4){
			continue;
		}
		//if(isContourConvex(approx)==false){
		//	continue;
		//}
		double score = matchShapes(
			shapeCross,approx,
			CV_CONTOURS_MATCH_I3,0
		);
		if(score>0.7){
			cout<<"score["<<i<<"]="<<score<<endl;
			continue;
		}
		drawPolyline(ova,approx,true);
		float val = (1-score)*100;
		cout<<"SCORE["<<i<<"]="<<val<<endl;
	}
	//drawEdgeMap(ova,edg);
	//drawContour(ova,cts);*/
	//imwrite("./final.png",ova);
	return 0;
}
//--------------------------------------//

int main_kkk(int argc, char* argv[]){
	//generate color mapping~~~~
	Mat src(1,256,CV_8UC1);
	Mat dst(1,256,CV_8UC3);
	for(int i=0; i<256; i++){
		src.at<uint8_t>(0,i) = i;
	}
	//cout<<src<<endl;
	applyColorMap(src,dst,COLORMAP_JET);
	cout<<"static Scalar mapJetColor[]={"<<endl<<"\t";
	for(int i=0; i<256; i+=32){
		Vec3b pix = dst.at<Vec3b>(0,i);
		printf("Scalar(%3d,%3d,%3d,255), ",pix[2],pix[1],pix[0]);
		if(i%4==3){
			printf("\n\t");
		}
	}
	cout<<"};"<<endl;
	//cout<<dst<<endl;
	return 0;
}
//--------------------------------------//

/*extern "C" int IsBlurred(
	const uint8_t* const luminance,
	const int width,
	const int height,
	float* blur,
	float* extent
);
int main2(int argc, char* argv[]) {

	//cout<<"load "<<argv[1]<<endl;

	//Mat img = imread(argv[1],IMREAD_GRAYSCALE);
	//Mat img = imread("aaa.pgm",IMREAD_GRAYSCALE);
	//Mat img = imread("test1.jpg",IMREAD_GRAYSCALE);
	//Mat img = imread("test2.png",IMREAD_GRAYSCALE);
	Mat img = imread("test5.png",IMREAD_GRAYSCALE);

	float parm[2]={0.f,0.f};
	IsBlurred(
		img.ptr(),
		img.cols,
		img.rows,
		parm+0,
		parm+1
	);
	cout<<"@ blur="<<parm[0]<<" @ "<<parm[1]<<endl;
	return 1;
}*/



/*extern void cutter(Mat& src,Mat& dst,int dw,int dh);

int main2(int argc, char* argv[]) {

	String tmpDir("/home/qq/labor/aaa/gpm2/temp");
	String srcDir("/home/qq/labor/aaa/gpm2/chip1");
	vector<string> name;
	list_dir(srcDir.c_str(),name);

	cout<<"pass.1:"<<endl;
	int width=0,height=0;
	for(size_t i=0; i<name.size(); i++){
		cout<<"process "<<name[i];
		string path=srcDir+"/"+name[i];
		Mat img = imread(path,IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
		Mat obj;
		//double tt = (double)getTickCount();
		cutter(img,obj,-1,-1);
		//tt = (((double)getTickCount() - tt)/getTickFrequency())*1000;
		//cout<<"extimate:"<<tt<<"ms"<<endl;
		//cout<<"obj size="<<obj.size()<<endl;
		width +=obj.size().width;
		height+=obj.size().height;
		//path=tmpDir+"/"+name[i];
		//imwrite(path,obj);
	}
	width = width / name.size();
	height= height/ name.size();

	cout<<"pass.2:"<<endl;
	Ptr<BackgroundSubtractorMOG2> mog2 = createBackgroundSubtractorMOG2(name.size(),50,false);
	Mat msk;
	for(size_t i=0; i<name.size(); i++){
		cout<<"cutting & model"<<name[i];
		//Mat img = imread(srcDir+"/"+name[i],IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
		Mat obj;
		//cutter(img,obj,width,height);
		//imwrite(tmpDir+"/"+name[i],obj);
		double tt = (double)getTickCount();
			mog2->apply(obj,msk);
		tt = (((double)getTickCount() - tt)/getTickFrequency())*1000;
		cout<<"estimate:"<<tt<<"ms"<<endl;
	}
	mog2->getBackgroundImage(msk);
	mog2->clear();
	imwrite(tmpDir+"/model.tif",msk);
	mog2->save("model.xml");

	cout<<"pass.3:"<<endl;//test sample~~~
	string samDir("/home/qq/labor/aaa/gpm2/20150309/BottomSide");
	string dffDir("/home/qq/labor/aaa/gpm2/xxxx");
	list_dir(samDir.c_str(),name);
	for(size_t i=0; i<name.size(); i++){
		cout<<"cutting & model"<<name[i];
		//Mat img = imread(samDir+"/"+name[i],IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
		Mat obj;
		//cutter(img,obj,width,height);
		double tt = (double)getTickCount();
			mog2->apply(obj,msk,0);
		tt = (((double)getTickCount() - tt)/getTickFrequency())*1000;
		cout<<"estimate:"<<tt<<"ms"<<endl;
		char buff[500];
		sprintf(buff,"%04ld-aaa.tif",i);
		imwrite(dffDir+"/"+buff,obj);
		sprintf(buff,"%04ld-bbb.tif",i);
		imwrite(dffDir+"/"+buff,msk);
	}
	return 0;
}*/
//--------------------------------------------------------------//

/*NAT_EXPORT int tearTileNxN(
	const char* nameDst,
	const char* nameSrc,
	long tid,
	long cntX,long cntY,
	int scale,
	int grid
);
NAT_EXPORT void tearTileByGid(const char* nameSrc,int gx,int gy,const char* nameDst);
NAT_EXPORT void panoramaTile(FILE* fdSrc,FILE* fdDst);
extern void gridMake(
	FILE* fdSrc,
	const char* imgPano,
	int scale,
	FILE* fdDst,
	const char* imgGrid
);
extern void gridMeas(FILE* fdDst,const char* ymlMap,FILE* fdSrc);


int main1(int argc, char* argv[]) {

	RawHead hd;
	const char* name1 = "grab.6.raw";
	const char* name2 = "pano.6.raw";
	const char* name21= "pano.6.jpg";
	const char* name3 = "grid.6.raw";
	const char* name31= "grid.6.png";
	const char* name4 = "meas.6.raw";
	const char* name41= "meas.6.png";
	double t;

	//Mat edg1;
	//Canny(img,edg1,128,128);
	//imwrite("ggyy2.png",edg1);

	/*fdSrc = fopen(name1,"rb");
	fdDst = fopen(name2,"wb+");
	fseek(fdSrc,0L,SEEK_SET);
	fread(&hd,sizeof(hd),1,fdDst);
	t = (double)getTickCount();
	panoramaTile(fdSrc,fdDst);
	t = ((double)getTickCount() - t)/getTickFrequency();
	cout<<"extimate:"<<t<<"sec"<<endl;
	fclose(fdSrc);
	fclose(fdDst);
	tearTileNxN(name21,name2,0,-1,-1,-1,0);*/

	/*fdSrc = fopen(name2,"rb");
	fdDst = fopen(name3,"wb+");
	fread(&hd,sizeof(hd),1,fdSrc);
	cout<<"make grid"<<endl;
	t = (double)getTickCount();
	gridMake(
		fdSrc,name21,1,
		fdDst,name31
	);
	t = ((double)getTickCount() - t)/getTickFrequency();
	cout<<"extimate:"<<t<<"sec"<<endl;
	fclose(fdSrc);
	fclose(fdDst);
	tearTileNxN(name31,name3,0,-1,-1,-1,0);
	//tearTileByGid(name3,0,3,"grid.tif");*/

	/*fdSrc = fopen(name3,"rb");
	fdDst = fopen(name4,"wb+");
	cout<<"measure grid"<<endl;
	t = (double)getTickCount();
	gridMeas(fdDst,"meas.yml",fdSrc);
	t = ((double)getTickCount() - t)/getTickFrequency();
	cout<<"extimate:"<<t<<"sec"<<endl;
	fclose(fdSrc);
	fclose(fdDst);
	tearTileNxN(name41,name4,0,-1,-1,-1,0);

	return 0;
}
//--------------------------------------------------------------//

static RNG rng(-1);
static Scalar randomColor(){
  int d1,d2;
  int rr = rng.uniform(90,165);
  d1 = rng.uniform(rr,255);
  d2 = rng.uniform(0,rr);
  int gg = rr+std::max(d1-rr,rr-d2);
  d1 = rng.uniform(rr,255);
  d2 = rng.uniform(0,rr);
  int bb = rr+std::max(d1-rr,rr-d2);
  return Scalar(bb,gg,rr);
}
 */


