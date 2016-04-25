#include <global.hpp>
#include <square.hpp>

#define ADP_SIZE 7

static int useScale[2]={1,1};
static int useSpace[2]={1,1};
static bool useTranpose = false;
static bool useBound = true;
static bool useSpeckle = true;

static int boundFarthest= 2;
static int boundLength  =-1;

static int speckleIntensity = 70;
static float speckleRadius  = 1.f;
static float speckleMinArea = 0.5f;

static Mat squrDat,squrInv,squrMsk,squrImg;

static vector<Point> squrCts;

static RotatedRect squrBnd;

static vector<MarkDefect> squrDft;

extern int Yen(Mat& data,double* valCrit);

inline void addMarkSpeckle(vector<Point>& spk){
	//TODO:check defect range~~~
	Point2f cc;
	float rad,area;
	minEnclosingCircle(spk,cc,rad);
	if(rad<speckleRadius){
		return;
	}
	area = contourArea(spk);
	if(area<speckleMinArea){
		return;
	}
	MarkDefect mk;
	mk.desc = DESC_SPECKLE;
	mk.arg[0] = cc.x;
	mk.arg[1] = cc.y;
	mk.arg[2] = rad;
	mk.arg[3] = area;
	squrDft.push_back(mk);
}

void test_speckle(){
	double max,ths;
	minMaxLoc(squrDat,NULL,&max,NULL,NULL);
	Mat& dat=squrInv;
	dat = max - squrDat;
	dat = dat & squrMsk;
	double crit;
	ths = Yen(dat,&crit);
	//imwrite("cc.0.png",dat);
	//cout<<"crit="<<crit<<", ths="<<ths<<endl;
	if(ths<speckleIntensity){
		return;//it is good die!!!!
	}
	threshold(dat,dat,ths,255,THRESH_BINARY);
	vector<vector<Point> > spk;
	findContours(dat,spk,RETR_EXTERNAL,CHAIN_APPROX_NONE);
	for(size_t i=0; i<spk.size(); i++){
		addMarkSpeckle(spk[i]);
	}
}
//--------------------------------------------------//

inline void addMarkBound(int fst,int lst,int depth,Point& p1,Point& p2){
	int len = hypot(p1.x-p2.x,p1.y-p2.y);//check defect range~~~
	if(len<boundLength){
		return;
	}
	MarkDefect mk;
	mk.desc = DESC_BOUNDING;
	mk.arg[0] = fst;
	mk.arg[1] = lst;
	mk.arg[2] = depth;//this is a maximum value~~~
	mk.pts1 = p1;
	mk.pts2 = p2;
	squrDft.push_back(mk);
}

void test_bound(){
	Point2f vtx[4];
	squrBnd.points(vtx);
	Mat bnd = Mat::ones(squrDat.size(),CV_8UC1);
	Scalar dark = Scalar::all(0);
	line(bnd,vtx[0],vtx[1],dark);
	line(bnd,vtx[1],vtx[2],dark);
	line(bnd,vtx[2],vtx[3],dark);
	line(bnd,vtx[3],vtx[0],dark);
	Mat map(squrDat.size(),CV_32FC1);
	distanceTransform(bnd,map,CV_DIST_L1,3);
	Mat dxx,dyy;
	Scharr(map,dxx,-1,1,0,1./32.);
	Scharr(map,dyy,-1,0,1,1./32.);

	int markIdx=-1;
	int markDep=-1;
	Point markFst,markPrj;
	for(size_t i=0; i<squrCts.size(); i++){
		Point2f cc = squrCts[i];
		float dist = map.at<float>(cc);
		if(dist<=boundFarthest){
			if(markIdx>=0){
				addMarkBound(markIdx,i,markDep,markFst,markPrj);//the end of mark!!!
			}
			markIdx=markDep=-1;//reset it~~~
			continue;
		}
		float dx = dxx.at<float>(cc);
		float dy = dyy.at<float>(cc);
		markPrj = cc;
		markPrj = markPrj - Point(dist*dx,dist*dy);
		//int pix = bnd.at<uint8_t>(prg);
		if(dist>markDep){
			markDep = dist;//update the farthest
		}
		if(markIdx<0){
			//the first mark~~~
			markIdx = i;
			markFst = markPrj;
		}
	}
	if(markIdx>=0){
		//dump the last mark~~~~
		addMarkBound(markIdx,markIdx,markDep,markFst,markFst);
	}
	return;
}

void find_contour(){
	squrCts.clear();
	Mat buf;
	double ths = threshold(squrDat,buf,0,0,THRESH_OTSU);
	threshold(squrDat,buf,ths,255,THRESH_BINARY);
	vector<vector<Point> > cts;
	findContours(buf,cts,RETR_EXTERNAL,CHAIN_APPROX_NONE);
	squrCts = cts[0];
	squrBnd = minAreaRect(squrCts);
	//prepare a mask
	squrMsk = Mat::zeros(buf.size(),buf.type());
	drawContours(squrMsk,cts,0,Scalar::all(255));
	floodFill(squrMsk,squrBnd.center,Scalar::all(255));
	drawContours(squrMsk,cts,0,Scalar::all(0));//erase boundary~~~
	//TODO:modify mask???
	const int kern_size = 1;
	Mat kern = getStructuringElement(
		MORPH_RECT,
		Size(2*kern_size + 1,2*kern_size+1),
		Point(kern_size,kern_size)
	);
	Mat node;
	erode(squrMsk,squrMsk,kern);
}

Rect find_target(Mat& dat){
	Rect loc(-1,-1,-1,-1);
	Mat buf(dat.rows/useScale[1],dat.cols/useScale[0],dat.type());
	Mat edg(buf.size(),buf.type());
	resize(dat,buf,buf.size());
	Scalar avg = mean(buf(Rect(0,0,ADP_SIZE,ADP_SIZE)));
	adaptiveThreshold(
		buf,edg,
		255.,
		ADAPTIVE_THRESH_MEAN_C,
		THRESH_BINARY_INV,
		ADP_SIZE,
		avg[0]*5.
	);
	//imwrite("cc.0.png",edg);
	vector<vector<Point> > cts;
	findContours(edg,cts,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
	if(cts.size()>=1){
		//find the biggest!!!
		size_t idx=0, area=0;
		Rect roi;
		for(size_t i=0; i<cts.size(); i++){
			roi = boundingRect(cts[i]);
			size_t val = (roi.width*roi.height);
			if(val>area){
				idx = i;
				area = val;
			}
		}
		roi = boundingRect(cts[idx]);
		int rem[4];
		rem[0] = (useScale[0]==1)?(useSpace[0]):(0);
		rem[1] = (useScale[1]==1)?(useSpace[1]):(0);
		rem[2] = (useScale[0]==1)?(2*useSpace[0]):(0);
		rem[3] = (useScale[1]==1)?(2*useSpace[1]):(0);
		loc.x = roi.x*useScale[0]-rem[0];
		loc.y = roi.y*useScale[1]-rem[1];
		loc.width = roi.width *useScale[0]+rem[2];
		loc.height= roi.height*useScale[1]+rem[3];		
		valid_roi(loc,dat);//valid again!!!
	}
	return loc;
}
//--------------------------------------------------//

int getBoundSize(char tkn){
	Point2f vtx[4];
	squrBnd.points(vtx);
	vector<int> test;
	int off,val[4];
	val[0] =hypot2f(vtx[0],vtx[1]);
	val[1] =hypot2f(vtx[1],vtx[2]);
	//val[2] =hypot2i(vtx[2],vtx[3]);
	//val[3] =hypot2i(vtx[3],vtx[0]);
	switch(tkn){
	case 'w':
		off = squrDat.cols;
		break;
	case 'h':
		off = squrDat.rows;
		break;
	}
	test.push_back(abs(off-val[0]));
	test.push_back(abs(off-val[1]));
	//test.push_back(abs(off-val[2]));
	//test.push_back(abs(off-val[3]));
	int idx = (int)(std::min_element(test.begin(),test.end()) - test.begin());
	return val[idx];
}

void draw_point(vector<Point>& pts){
	Mat img;
	squrImg.copyTo(img);
	Scalar cc(0,0,255);
	for(size_t i=0; i<pts.size(); i++){
		img.at<Vec3b>(pts[i]) = Vec3b(0,0,255);
	}
	imwrite("cc.0.tiff",img);
}

void draw_point(vector<int>&  idx){
	Scalar cc(0,0,255);
	Mat img;
	squrImg.copyTo(img);
	Point p2,p1=squrCts[idx[0]];
	for(size_t i=1; i<idx.size(); i++){
		p2 = squrCts[idx[i]];
		line(img,p1,p2,cc);
		p1 = p2;
	}
	p2 = squrCts[idx[0]];
	line(img,p1,p2,cc);
	imwrite("cc.0.tiff",img);
}

void draw_bound(){
	//Mat ova = Mat::zeros(peekImg.size(),peekImg.type());
	Mat img;
	squrImg.copyTo(img);
	Scalar cc(255,0,0);
	Point2f vtx[4];
	squrBnd.points(vtx);
	line(img,vtx[0],vtx[1],cc);
	line(img,vtx[1],vtx[2],cc);
	line(img,vtx[2],vtx[3],cc);
	line(img,vtx[3],vtx[0],cc);
	//addWeighted(peekImg,0.6,ova,0.4,0.,img);
	imwrite("cc.0.tiff",img);
}

void draw_mark(const char* name){
	if(name==NULL){
		name = "default.png";
	}

	Mat msk;
	msk = Mat::zeros(squrImg.size(),squrImg.type());

	//save the origin picture~~~~
	/*char tmp[500];
	strcpy(tmp,name);
	char* dot = strrchr(tmp,'.');
	strcpy(dot,".org.png");
	imwrite(tmp,peekImg);*/

	Vec3b yow(0,255,255);
	Vec3b red(0,0,255);
	Scalar sRed(0,0,255);
	Scalar sBlue(255,0,0);
	//draw boundary~~~
	Point2f vtx[4];
	squrBnd.points(vtx);
	line(msk,vtx[0],vtx[1],sBlue);
	line(msk,vtx[1],vtx[2],sBlue);
	line(msk,vtx[2],vtx[3],sBlue);
	line(msk,vtx[3],vtx[0],sBlue);
	for(size_t i=0; i<squrCts.size(); i++){
		Point pos = squrCts[i];
		Vec3b pix = msk.at<Vec3b>(pos.y,pos.x);
		if(pix[0]!=0 || pix[1]!=0 || pix[2]!=0){
			continue;
		}
		msk.at<Vec3b>(pos) = yow;
	}
	for(size_t i=0; i<squrDft.size(); i++){
		MarkDefect mk = squrDft[i];
		int i1,i2;
		Point2f cc;
		switch(mk.desc){
		case DESC_BOUNDING:
			i1 = mk.arg[0];
			i2 = mk.arg[1];
			//line(img,mk.pts1,peekCts[i1],cc2);
			while(i1!=i2){
				msk.at<Vec3b>(squrCts[i1]) = red;
				i1++;
			}
			//line(img,peekCts[i2],mk.pts2,cc2);
			line(msk,mk.pts1,mk.pts2,sRed);
			//cout<<"Bound=("<<mk.arg[0]<<","<<mk.arg[1]<<")@"<<mk.arg[2]<<endl;
			break;
		case DESC_SPECKLE:
			cc.x = mk.arg[0];
			cc.y = mk.arg[1];
			circle(msk,cc,mk.arg[2]+3,sRed);
			break;
		}
	}

	addWeighted(squrImg,0.7,msk,0.3,0,msk);
	imwrite(name,msk);
}
//--------------------------------------------------//

NAT_EXPORT void squareRelease(){
	squrDft.clear();
	squrDat.release();
	squrInv.release();
	squrMsk.release();
	squrImg.release();
}

extern "C" JNIEXPORT void JNICALL Java_prj_demon_panBackside_squareRelease(
	JNIEnv * env,
	jobject thiz
){
	squareRelease();
}

NAT_EXPORT int squareIdentify(void* data,int width,int height){
	Mat dat(height,width,CV_8UC1,data);
	Rect roi = find_target(dat);
	if(roi.x<0||roi.y<0){
		return -1;
	}
	squareRelease();
	if(useTranpose==true){
		transpose(dat(roi),squrDat);//TODO:do we need this???
	}else{
		dat(roi).copyTo(squrDat);
	}
	if(squrDat.channels()==1){
		cvtColor(squrDat,squrImg,COLOR_GRAY2BGR);
	}else{
		squrDat.copyTo(squrImg);
	}
	squrInv.create(squrDat.size(),CV_8UC1);

	find_contour();
	if(useBound==true){
		test_bound();
	}
	if(useSpeckle==true){
		test_speckle();
	}
	return squrDft.size();
}

extern "C" JNIEXPORT jlong JNICALL Java_prj_demon_panBackside_squareIdentify(
	JNIEnv * env,
	jobject thiz,
	jlong memPtr,
	jlong memWidth,
	jlong memHeight
){
	return squareIdentify((void*)memPtr,(int)memWidth,(int)memHeight);
}

NAT_EXPORT int squareGetMark(MarkDefect* buf,int off,int size){
	if(off<0){
		off = 0;
	}
	if(size<0){
		size = squrDft.size();
	}
	for(size_t i=off; i<(off+size); i++){
		if(i>=squrDft.size()){
			return -1;
		}
		buf[i] = squrDft[i];
	}
	return 0;
}

NAT_EXPORT long squareGet(int id){
	long val = 0;
	switch(id){
	case PARM_INFO_WIDTH:  val=getBoundSize('w'); break;
	case PARM_INFO_HEIGHT: val=getBoundSize('h'); break;
	case PARM_INFO_CENTER_X: val=squrBnd.center.x; break;
	case PARM_INFO_CENTER_Y: val=squrBnd.center.y; break;

	case PARM_SCALE_X: val=useScale[0]; break;
	case PARM_SCALE_Y: val=useScale[1]; break;
	case PARM_SPACE_X: val=useSpace[0]; break;
	case PARM_SPACE_Y: val=useSpace[1]; break;

	case PARM_TRANPOSE:if(useTranpose==true){ val=1; } break;
	case PARM_BOUND:   if(useBound   ==true){ val=1; } break;
	case PARM_SPECKLE: if(useSpeckle ==true){ val=1; } break;

	case PARM_BOUND_FAR:val=boundFarthest; break;
	case PARM_BOUND_LEN:val=boundLength; break;

	case PARM_SPECKLE_INT:val=speckleIntensity; break;
	case PARM_SPECKLE_RAD:val=cvRound(speckleRadius); break;
	case PARM_SPECKLE_ARE:val=cvRound(speckleMinArea*10); break;
	}
	return val;
}

extern "C" JNIEXPORT jlong JNICALL Java_prj_demon_panBackside_squareGet(
	JNIEnv * env,
	jobject thiz,
	jint jid
){
	return squareGet(jid);
}

NAT_EXPORT void squareSet(int id,long val){
	switch(id){
	case PARM_DUMP_ORIGIN:
		imwrite((char*)val,squrDat);		
		break;
	case PARM_DUMP_SPECKLE:
		imwrite((char*)val,squrInv);
		break;
	case PARM_DUMP_DEFECT:
		draw_mark((char*)val);
		break;

	case PARM_SCALE_X: useScale[0]=val; break;
	case PARM_SCALE_Y: useScale[1]=val; break;
	case PARM_SPACE_X: useSpace[0]=val; break;
	case PARM_SPACE_Y: useSpace[1]=val; break;

	case PARM_TRANPOSE:if(val==0){useTranpose=false;}else{useTranpose=true;} break;
	case PARM_BOUND:   if(val==0){useBound   =false;}else{useBound   =true;} break;
	case PARM_SPECKLE: if(val==0){useSpeckle =false;}else{useSpeckle =true;} break;

	case PARM_BOUND_FAR:boundFarthest=val; break;
	case PARM_BOUND_LEN:boundLength=val; break;

	case PARM_SPECKLE_INT:
		speckleIntensity=val;
		//speckleIntensity=(double)(val%100);
		//speckleIntensity=speckleIntensity/100.;
		break;
	case PARM_SPECKLE_RAD:  
		speckleRadius=val; 
		break;
	case PARM_SPECKLE_ARE:
		speckleMinArea=val;
		speckleMinArea=speckleMinArea/10.f;
		break;
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_demon_panBackside_squareSet(
	JNIEnv * env,
	jobject thiz,
	jint jid,
	jlong jval
){
	squareSet(jid,jval);
}

extern "C" JNIEXPORT void JNICALL Java_prj_demon_panBackside_squareDump(
	JNIEnv * env,
	jobject thiz,
	jint jid,
	jstring jname
){
	char name[500];
	jstrcpy(env,jname,name);
	switch(jid){
	case PARM_DUMP_ORIGIN:
		imwrite(name,squrDat);		
		break;
	case PARM_DUMP_DEFECT:
		draw_mark(name);
		break;
	}
}


