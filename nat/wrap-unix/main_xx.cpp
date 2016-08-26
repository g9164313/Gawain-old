#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <global.hpp>
#include <utils_ipc.hpp>
#include <algorithm>

/*void list_dir(string path,vector<string>& lst,string prex);

extern Mat cutOutBounding(Mat& img,Mat& msk,int width,int height);
extern void removeNoise(Mat& msk,int* board);

int main5(int argc, char* argv[]) {

	string pathBack="./cam0/back";
	vector<string> lstBack;
	list_dir(pathBack,lstBack,"");

	string pathFore="./cam0/fore";
	vector<string> lstFore;
	list_dir(pathFore,lstFore,"Chip1");

	string pathMeas="./cam0/meas";
	vector<string> lstMeas;
	list_dir(pathMeas,lstMeas,"Sp01L120");

	IpcToken tkn("cam1");

	tkn.exec("IDFY");
	cout<<"CMD=idfy @ RESP="<<tkn.getMsg()<<endl;

	tkn.exec("MEM0,6576,4384,mono");
	cout<<"RESP="<<tkn.getMsg()<<endl;
	void* buff = tkn.getMem(0);
	if(buff==NULL){
		cout<<"something is wrong!!!"<<endl;
		return -1;
	}

	/*tkn.exec("CLR,back");
	cout<<"RESP="<<tkn.getMsg()<<endl;
	tkn.exec("CLR,fore");
	cout<<"RESP="<<tkn.getMsg()<<endl;

	for(size_t i=0; i<lstBack.size(); i++){
		string name = pathBack+"/"+lstBack[i];
		Mat img = imread(name,IMREAD_GRAYSCALE);
		Mat nod(img.rows,img.cols,CV_8UC1,buff);
		img.copyTo(nod);
		tkn.exec("SAVE,BACK");
		cout<<"CMD=save,back @ RESP="<<tkn.getMsg()<<endl;
	}
	for(size_t i=0; i<lstFore.size(); i++){
		string name = pathFore+"/"+lstFore[i];
		Mat img = imread(name,IMREAD_GRAYSCALE);
		Mat nod(img.rows,img.cols,CV_8UC1,buff);
		img.copyTo(nod);
		tkn.exec("SAVE,FORE");
		cout<<"CMD=save,fore @ RESP="<<tkn.getMsg()<<endl;
	}
	tkn.exec("TRAN");
	cout<<"CMD=tran @ RESP="<<tkn.getMsg()<<endl;*/

	/*for(size_t i=0; i<lstMeas.size(); i++){
		string name = pathMeas+"/"+lstMeas[i];
		Mat img = imread(name,IMREAD_GRAYSCALE);
		Mat nod(img.rows,img.cols,CV_8UC1,buff);
		img.copyTo(nod);
		tkn.exec("MEAS");
		cout<<"CMD=MEAS @ RESP="<<tkn.getMsg()<<endl;
	}

	//tkn.pipeSend("gfgfggf");
	//tkn.pipeRecv(NULL);
	//cout<<"RESP>>"<<tkn.getMsg()<<endl;
	return 0;
}*/

extern "C" int IsBlurred(
	const uint8_t* const luminance,
	const int width,
	const int height,
	float* blur,
	float* extent
);
int main(int argc, char* argv[]) {

	//Mat img = imread("aaa.pgm",IMREAD_GRAYSCALE);
	Mat img = imread("test1.jpg",IMREAD_GRAYSCALE);
	//Mat img = imread("test2.png",IMREAD_GRAYSCALE);
	//Mat img = imread("qq1.png",IMREAD_GRAYSCALE);

	float parm[2];
	IsBlurred(
		img.ptr(),
		img.cols,
		img.rows,
		parm+0,
		parm+1
	);
	cout<<"@ blur="<<parm[0]<<" @ "<<parm[1]<<endl;
	return 1;
}



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

NAT_EXPORT int tearTileNxN(
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

	/*RawHead hd;
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
	tearTileNxN(name41,name4,0,-1,-1,-1,0);*/

	return 0;
}
//--------------------------------------------------------------//

/*
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


