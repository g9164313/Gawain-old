#include <global.hpp>
#include <grabber.hpp>
#include <Windows.h>
extern void registration(const char* name);
extern void tearRectangle(const char* nameDst,const char* nameSrc,Rect& rect);
extern void tear1Tile(const char* nameOut,const char* name,long idx);
extern void tear4Tile(const char* name,long idx);

/*void copy_file(const char* src, const char* dst){
	FILE* ss = fopen(src,"rb");
	FILE* dd = fopen(dst,"wb");
	uint8_t buf[1000*1000];
    size_t size;
	while (size=fread(buf,1,sizeof(buf),ss)){ fwrite(buf,1,size,dd); }
	fclose(ss);
	fclose(dd);
}*/

int main(int argc, char* argv[]) {
	char* tmp = "C:\\labor\\Gawain\\lib\\cv\\ggyy";	
	char* src = "C:\\labor\\Gawain\\grabber.raw";
	char* bak = "C:\\labor\\Gawain\\grabber.raw.bak";
	
	//tearRectangle("C:\\labor\\Gawain\\dump3.png",src,Rect(-1,-1,-2,-2));
	//tearRectangle("C:\\labor\\Gawain\\dump2.png",tmp,Rect(-1,-1,-2,-2));
	//tearRectangle("C:\\labor\\Gawain\\dump1.png",bak,Rect(-1,-1,-2,-2));
	//tear4Tile(src,9);

	/*tear1Tile("C:\\labor\\Gawain\\tt1.png",src,0);
	tear1Tile("C:\\labor\\Gawain\\tta.png",src,1);
	Mat aa = imread("C:\\labor\\Gawain\\tt1.png",IMREAD_COLOR);
	Mat bb = imread("C:\\labor\\Gawain\\tta.png",IMREAD_COLOR);
	int cc = bb.channels();
	vector<Mat> imgs;
	imgs.push_back(aa);
	imgs.push_back(bb);
	Mat pano;
	Stitcher stitcher = Stitcher::createDefault();
    Stitcher::Status status = stitcher.stitch(imgs,pano);
	imwrite("C:\\labor\\Gawain\\dump.png",pano);*/
	return 0;
}

/*
double unsharpSigma = 3.3f;
double unsharpAmount = 0.4;
int unsharpIter = 2;

Mat unsharpMaskEx(Mat& src){
	//this process for dark-field~~~
	Mat msk,ref;
	src.copyTo(msk);
	double scale;
	minMaxLoc(src,&scale,NULL);
	scale = scale * unsharpAmount;
	for(int i=0; i<unsharpIter; i++){
		GaussianBlur(msk, ref, Size(), unsharpSigma, unsharpSigma);
		msk = msk * (1 + scale) + ref * (-scale);
	}
	//msk.copyTo(ref);
	bilateralFilter(msk,ref,10,70,70);
	threshold(ref,ref,0,255,THRESH_BINARY|THRESH_OTSU);
	msk.release();
	return ref;
}
*/
