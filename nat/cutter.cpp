#include <global.hpp>
#include <cutter.hpp>

#define STRATEGY_OSTU     0
#define STRATEGY_OSTU_INV 1
#define STRATEGY_GRABCUT  2
#define STRATEGY_CANNY    3

vector<Point>& find_biggest(vector<vector<Point> >&cts);
void shift_vertex(Point2f vtx[4]);
void stra_grabcut(Mat& img,Mat& msk);
void stra_canny(Mat& img,Mat& msk);

static int  useShrink= 4;
static bool useEqual = false;
static bool useCLAHE = false;
static bool useApprx = true;
static int  Strategy = STRATEGY_CANNY;

void cutter(Mat& src,Mat& dst,int dw,int dh){

	Mat nod1,nod2,nod3;

	if(useShrink>1){
		resize(src,nod1,Size(src.cols/useShrink,src.rows/useShrink));
	}else{
		useShrink=0;
		nod1 = src;
	}
	//imwrite("cc.1.png",nod1);

	if(useEqual==true){
		equalizeHist(nod1,nod2);
	}else if(useCLAHE==true){
		Ptr<CLAHE> ago = createCLAHE();
		ago->apply(nod1,nod2);
	}else{
		nod2 = nod1;
	}
	//imwrite("cc.2.png",nod2);

	nod3 = Mat(nod2.size(),CV_8UC1);
	switch(Strategy){
	case STRATEGY_CANNY:
		stra_canny(nod2,nod3);
		break;
	case STRATEGY_GRABCUT:
		if(nod2.channels()==1){
			Mat tmp;
			cvtColor(nod2,tmp,COLOR_GRAY2BGR);
			stra_grabcut(tmp,nod3);
			cvtColor(tmp,nod2,COLOR_BGR2GRAY);
		}else{
			stra_grabcut(nod1,nod3);
		}
		break;
	case STRATEGY_OSTU_INV:
		threshold(nod2,nod3,0,255,THRESH_OTSU|THRESH_BINARY_INV);
		break;
	case STRATEGY_OSTU:
	default:
		threshold(nod2,nod3,0,255,THRESH_OTSU|THRESH_BINARY);
		break;
	}
	//imwrite("cc.3.png",nod3);

	//find a contour~~~
	vector<vector<Point> > cts;
	findContours(nod3,cts,RETR_EXTERNAL,CHAIN_APPROX_NONE);

	vector<Point> _cts;
	switch(cts.size()){
	case 0:
		cerr<<"fail to find target"<<endl;//WTF!!!
		return;
	case 1:
		_cts = cts[0];
		break;
	default:
		_cts = find_biggest(cts);
		break;
	}
	if(useApprx==true){
		approxPolyDP(_cts,_cts,4,true);
	}else{
		approxPolyDP(_cts,_cts,4,true);
	}
	RotatedRect obj = minAreaRect(_cts);

	//bounding~~~
	Point2f vtx[4];
	obj.points(vtx);
	shift_vertex(vtx);
	if(useShrink>1){
		vtx[0] = vtx[0] * useShrink;
		vtx[1] = vtx[1] * useShrink;
		vtx[2] = vtx[2] * useShrink;
		vtx[3] = vtx[3] * useShrink;
	}
	Point2f dir1[3] = {
		vtx[0],
		vtx[1],
		vtx[3]
	};
	Point2f dir2[3] = {
		Point2f(0,0),
		Point2f(hypot2f(vtx[0],vtx[1]),0),
		Point2f(0,hypot2f(vtx[0],vtx[3]))
	};
	if(dw<=0){
		dw = cvRound(dir2[1].x)+useShrink/2;
	}
	if(dh<=0){
		dh = cvRound(dir2[2].y)+useShrink/2;
	}
	Size blk(dw,dh);
	if(dst.empty()==true){
		dst = Mat(blk,src.type());
	}else if(blk!=dst.size()){
		cerr<<"relocate destination"<<endl;
		dst = Mat(blk,src.type());
	}
	warpAffine(
		src,dst,
		getAffineTransform(dir1,dir2),
		dst.size()
	);
	//imwrite("cc.4.png",dst);
}
//------------------------//

void stra_canny(Mat& img,Mat& msk){
	msk.setTo(0);
	int bw = (msk.cols*1)/100;
	int bh = (msk.rows*1)/100;
	Scalar cc = Scalar::all(255);
	rectangle(msk,Rect(0,0,msk.cols,bh),cc,-1);
	rectangle(msk,Rect(0,0,bw,msk.rows),cc,-1);
	rectangle(msk,Rect(msk.cols-bw,0,bw,msk.rows),cc,-1);
	rectangle(msk,Rect(0,msk.rows-bh,msk.cols,bh),cc,-1);
	Scalar avg1 = mean(img,msk);

	msk.setTo(0);
	bw = msk.cols/3;
	bh = msk.rows/3;
	rectangle(msk,Rect(bw,bh,bw,bh),cc,-1);
	Scalar avg2 = mean(img,msk);

	float diff = abs(avg2[0]-avg1[0]);
	Canny(img,msk,diff*1.5,diff*3,3);
}

void stra_grabcut(Mat& img,Mat& msk){
	int bw = (msk.cols*1)/100;
	int bh = (msk.rows*1)/100;
	Rect zone(
		bw,bh,
		msk.cols-2*bw,msk.rows-2*bh
	);
	Mat bgd,fgd;
	grabCut(img,msk,zone,bgd,fgd,1,GC_INIT_WITH_RECT);
	grabCut(img,msk,zone,bgd,fgd,1,GC_EVAL);

	Mat tab=Mat::zeros(256,1,CV_8UC1);
	tab.at<uint8_t>(GC_BGD,0) = 0;
	tab.at<uint8_t>(GC_FGD,0) = 255;
	tab.at<uint8_t>(GC_PR_BGD,0) = 0;
	tab.at<uint8_t>(GC_PR_FGD,0) = 255;
	LUT(msk,tab,msk);
}
//------------------------//

void shift_vertex(Point2f vtx[4]){
	int idx=0;
	double len = HUGE_VAL;
	for(int i=0; i<4; i++){
		double v = hypot(vtx[i].x,vtx[i].y);
		if(v<len){
			idx = i;
			len = v;
		}
	}
	Point2f tmp[4];
	int pos=0;
	while(pos<4){
		tmp[pos] = vtx[idx];
		pos++;
		idx++;
		idx = idx % 4;
	}
	vtx[0] = tmp[0];
	vtx[2] = tmp[2];
	//test clock-wise
	if(tmp[1].x<tmp[3].x){
		vtx[1] = tmp[3];
		vtx[3] = tmp[1];
	}else{
		vtx[1] = tmp[1];
		vtx[3] = tmp[3];
	}
}

vector<Point>& find_biggest(vector<vector<Point> >& cts){
	size_t indx=0;
	float area = 0.f;
	for(size_t i=0; i<cts.size(); i++){
		/*RotatedRect obj = minAreaRect(cts[i]);
		float v = obj.size.width * obj.size.height;
		if(v>=area){
			indx=i;
			area=v;
		}*/
		float v = contourArea(cts[i]);
		if(v>=area){
			indx=i;
			area=v;
		}
	}
	return cts[indx];
}



