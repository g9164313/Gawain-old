#include <global.hpp>
#include <CamBundle.hpp>

/*int IJIsoData(int[] data) {
	// This is the original ImageJ IsoData implementation, here for backward compatibility.
	int level;
	int maxValue = data.length - 1;
	double result, sum1, sum2, sum3, sum4;
	int count0 = data[0];
	data[0] = 0;//set to zero so erased areas aren't included
	int countMax = data[maxValue];
	data[maxValue] = 0;
	int min = 0;
	while ((data[min]==0) && (min<maxValue))
	min++;
	int max = maxValue;
	while ((data[max]==0) && (max>0))
	max--;
	if (min>=max) {
		data[0]= count0; data[maxValue]=countMax;
		level = data.length/2;
		return level;
	}
	int movingIndex = min;
	int inc = Math.max(max/40, 1);
	do {
		sum1=sum2=sum3=sum4=0.0;
		for (int i=min; i<=movingIndex; i++) {
			sum1 += (double)i*data[i];
			sum2 += data[i];
		}
		for (int i=(movingIndex+1); i<=max; i++) {
			sum3 += (double)i*data[i];
			sum4 += data[i];
		}
		result = (sum1/sum2 + sum3/sum4)/2.0;
		movingIndex++;
	}while ((movingIndex+1)<=result && movingIndex<max-1);
	data[0]= count0; data[maxValue]=countMax;
	level = (int)Math.round(result);
	return level;
}

int defaultIsoData(int[] data) {
	// This is the modified IsoData method used by the "Threshold" widget in "Default" mode
	int n = data.length;
	int[] data2 = new int[n];
	int mode=0, maxCount=0;
	for (int i=0; i<n; i++) {
		int count = data[i];
		data2[i] = data[i];
		if (data2[i]>maxCount) {
			maxCount = data2[i];
			mode = i;
		}
	}
	int maxCount2 = 0;
	for (int i = 0; i<n; i++) {
		if ((data2[i]>maxCount2) && (i!=mode))
		maxCount2 = data2[i];
	}
	int hmax = maxCount;
	if ((hmax>(maxCount2*2)) && (maxCount2!=0)) {
		hmax = (int)(maxCount2 * 1.5);
		data2[mode] = hmax;
	}
	return IJIsoData(data2);
}*/

int Yen(Mat& data,double* valCrit) {
	/* normalized histogram */
	int histSize = 256;
	Mat norm_histo;
	calcHist(&data, 1, NULL, Mat(), norm_histo, 1, &histSize, 0);
	float total = norm(norm_histo, NORM_L1);
	norm_histo = norm_histo / total;
	double P1[256];/* cumulative normalized histogram */
	double P1_sq[256];
	double P2_sq[256];
	P1[0] = norm_histo.at<float>(0, 0);
	for (int ih = 1; ih < 256; ih++) {
		P1[ih] = P1[ih - 1] + norm_histo.at<float>(ih, 0);
	}
	P1_sq[0] = norm_histo.at<float>(0, 0) * norm_histo.at<float>(0, 0);
	for (int ih = 1; ih < 256; ih++) {
		P1_sq[ih] = P1_sq[ih - 1] + norm_histo.at<float>(ih, 0) * norm_histo.at<float>(ih, 0);
	}
	P2_sq[255] = 0.0;
	for (int ih = 254; ih >= 0; ih--) {
		P2_sq[ih] = P2_sq[ih + 1] + norm_histo.at<float>(ih + 1, 0) * norm_histo.at<float>(ih + 1, 0);
	}
	/* Find the threshold that maximizes the criterion */
	int threshold = -1;
	double crit;
	double max_crit = -DBL_MAX; //Double.MIN_VALUE;
	for (int it = 0; it < 256; it++) {
		double e1 =((P1_sq[it]*P2_sq[it])>0.0 ? log(P1_sq[it] * P2_sq[it]) : 0.0);
		double e2 =((P1[it]*(1.0-P1[it]))>0.0 ?	log(P1[it] * (1.0 - P1[it])) : 0.0);
		crit = -1.0*e1 + 2*e2;
		if (crit > max_crit) {
			max_crit = crit;
			threshold = it;
		}
	}
	if(valCrit!=NULL){
		(*valCrit) = max_crit;
	}
	return threshold;
}
//-----------------------------------//

void unsharpen(Mat& src) {
	Mat msk(src.size(), src.type());
	src.copyTo(msk);
	double scale;
	minMaxLoc(src, NULL, &scale);
	scale = scale * 0.2;
	GaussianBlur(src, src, Size(), 4.3, 4.3);
	msk = msk * (1 + scale) + src * (-scale);
	msk.copyTo(src);
}
//-----------------------------------//

int findMaxBlob(vector<vector<Point> >& blob){
	int idx=0;
	double max=0;
	for(size_t i=0; i<blob.size(); i++){
		double area =0;//contourArea(blob[i]);
		if(area>max){
			max = area;
			idx = i;
		}
	}
	return idx;
}

void drawBlob(const char* name,Mat& _img,vector<Point>& blob){
	Mat img(_img.size(),CV_8UC3);
	cvtColor(_img,img,COLOR_GRAY2BGR);
	vector<vector<Point> > tmp;
	tmp.push_back(blob);
	drawContours(img,tmp,0,Scalar(0,200,0));
	imwrite(name,img);
}
//-----------------------------------//

struct PointST {
	int x;
	int y;
	float SWT;
};

struct Ray {
	PointST p;
	PointST q;
	std::vector<PointST> points;
};

/*void _stroke_width_transform(Mat& edgeImage, Mat& gradientX, Mat& gradientY,
		bool dark_on_light, Mat& SWTImage, std::vector<Ray> & rays) {
	// First pass
	float prec = .05;
	for (int row = 0; row < edgeImage.rows; row++) {
		for (int col = 0; col < edgeImage.cols; col++) {
			if (edgeImage.at<uchar>(row, col) == 0) {
				continue;
			}
			Ray r;

			PointST p;
			p.x = col;
			p.y = row;
			r.p = p;
			vector<PointST> points;
			points.push_back(p);

			float curX = (float) col + 0.5;
			float curY = (float) row + 0.5;
			int curPixX = col;
			int curPixY = row;
			float G_x = gradientX.at<float>(row, col);
			float G_y = gradientY.at<float>(row, col);
			// normalize gradient
			float mag = sqrt((G_x * G_x) + (G_y * G_y));
			if (dark_on_light) {
				G_x = -G_x / mag;
				G_y = -G_y / mag;
			} else {
				G_x = G_x / mag;
				G_y = G_y / mag;
			}

			while (true) {
				curX += G_x * prec;
				curY += G_y * prec;
				if ((int) (floor(curX)) != curPixX
						|| (int) (floor(curY)) != curPixY) {
					curPixX = (int) (floor(curX));
					curPixY = (int) (floor(curY));
					// check if pixel is outside boundary of image
					if ((curPixX < 0) || (curPixX >= SWTImage.cols)
							|| (curPixY < 0) || (curPixY >= SWTImage.rows)) {
						break;
					}
					PointST pnew;
					pnew.x = curPixX;
					pnew.y = curPixY;
					points.push_back(pnew);

					if (edgeImage.at<uchar>(curPixY, curPixX) > 0) {
						r.q = pnew;
						// dot product
						float G_xt = gradientX.at<float>(curPixY, curPixX);
						float G_yt = gradientY.at<float>(curPixY, curPixX);
						mag = sqrt((G_xt * G_xt) + (G_yt * G_yt));
						if (dark_on_light) {
							G_xt = -G_xt / mag;
							G_yt = -G_yt / mag;
						} else {
							G_xt = G_xt / mag;
							G_yt = G_yt / mag;
						}

						if (acos(G_x * -G_xt + G_y * -G_yt) < M_PI / 2.0) {
							float length = hypot((float) r.q.x - (float) r.p.x,
									(float) r.q.y - (float) r.p.y);
							for (vector<PointST>::iterator pit = points.begin();
									pit != points.end(); pit++) {
								if (SWTImage.at<float>(pit->y, pit->x) < 0) {
									SWTImage.at<float>(pit->y, pit->x) = length;
								} else {
									SWTImage.at<float>(pit->y, pit->x) =
											std::min(length,
													SWTImage.at<float>(pit->y,
															pit->x));
								}
							}
							r.points = points;
							rays.push_back(r);
						}
						break;
					}
				}
			}
		}
	}
}*/

bool _stroke_width_sort(const PointST &lhs, const PointST &rhs) {
	return lhs.SWT < rhs.SWT;
}

/*void _stroke_width_filter(Mat& SWTImage, std::vector<Ray>& rays) {
	for (vector<Ray>::iterator rit = rays.begin(); rit != rays.end(); rit++) {
		for (vector<PointST>::iterator pit = rit->points.begin();
				pit != rit->points.end(); pit++) {
			pit->SWT = SWTImage.at<float>(pit->y, pit->x);
		}
		sort(rit->points.begin(), rit->points.end(), &_stroke_width_sort);
		float median = (rit->points[rit->points.size() / 2]).SWT;
		for (vector<PointST>::iterator pit = rit->points.begin();
				pit != rit->points.end(); pit++) {
			SWTImage.at<float>(pit->y, pit->x) = std::min(pit->SWT, median);
		}
	}
}*/

Mat strokeWidth(Mat& edge, bool dark_on_light, vector<Ray>& rays) {
	Mat img, gradx, grady;
	edge.convertTo(img, CV_32FC1, 1. / 255., 0);

	GaussianBlur(img, img, Size(5, 5), 0.);
	Scharr(img, gradx, -1, 1, 0);
	Scharr(img, grady, -1, 0, 1);
	medianBlur(gradx, gradx, 3);
	medianBlur(grady, grady, 3);

	Mat swt = Mat::ones(img.size(), CV_32FC1);
	swt = swt * -1.;
	//_stroke_width_transform(edge, gradx, grady, dark_on_light, swt, rays);
	//_stroke_width_filter(swt,rays);
	//dumpVal("cc.0.png",cc);
	return swt;
}
//-----------------------------------//

void toColor(Mat& src,Mat& dst){
	dst.release();
	int type = src.type();
	switch(type){
	case CV_8UC3:
		dst = src;
		break;
	case CV_8UC1:
		dst = Mat(src.size(),CV_8UC3);
		cvtColor(src,dst,COLOR_GRAY2BGR);
		break;
	default:
		cerr<<"how to become color("<<type<<") ??"<<endl;
		break;
	}
}
//-----------------------------------//

extern "C" JNIEXPORT jbyteArray JNICALL Java_narl_itrc_CamBundle_getData(
	JNIEnv* env,
	jobject thiz /*this object is already 'CamBundle'*/,
	jlong ptrMat
){
	if(ptrMat==0L){
		return NULL;
	}
	Mat& img =  *((Mat*)ptrMat);
	if(img.empty()==true){
		return NULL;
	}
	vector<uchar> buf;
	imencode(".png",img,buf);
	jbyteArray arrBuf = env->NewByteArray(buf.size());
	env->SetByteArrayRegion(arrBuf,0,buf.size(),(jbyte*)&buf[0]);
	return arrBuf;
}

static Scalar clrMark[] = {
	Scalar(  5,240,240),
	Scalar(  5,  5,240),
	Scalar(  5,240,  5),
	Scalar(240,  5,  5),
};

void DrawPinMark(Mat& img,Point& pp,Scalar& color){
	//It must be a color-image!!!
	const int CROSS_SPACE=4;
	Vec3b org = img.at<Vec3b>(pp);
	Point cc0 = pp + Point(-CROSS_SPACE,0);
	Point cc1 = pp + Point( CROSS_SPACE,0);
	Point cc2 = pp + Point(0, CROSS_SPACE);
	Point cc3 = pp + Point(0,-CROSS_SPACE);
	line(img,cc0,cc1,color);
	line(img,cc2,cc3,color);
	img.at<Vec3b>(pp) = org;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_CamBundle_markData(
	JNIEnv* env,
	jobject thiz /*this object is already 'CamBundle'*/
){
	CamBundle* cam = getContext(env,thiz);
	Mat* _src = cam->getMat(0);
	Mat* _ova = cam->getMat(1);
	if(_ova==NULL){
		return;
	}
	Mat& ova = *_ova;
	Mat& src = *_src;

	jintArray jintArr;
	jfloatArray jfloatArr;

	jint*   pinPos = intArray2Ptr(env,cam->clzz,thiz,"pinPos",jintArr);
	jfloat* pinVal = floatArray2Ptr(env,cam->clzz,thiz,"pinVal",jfloatArr);
	for(int i=0; i<PR_SIZE; i++){
		Point cc;
		cc.x = pinPos[2*i+0];
		cc.y = pinPos[2*i+1];
		if(cc.x<0 || cc.y<0){
			continue;
		}
		switch(src.type()){
		case CV_8UC1:{
			uint8_t pix = ova.at<uint8_t>(cc);
			pinVal[PIN_COLS*i+0] = pix;
			pinVal[PIN_COLS*i+1] = pix;
			pinVal[PIN_COLS*i+2] = pix;
			pinVal[PIN_COLS*i+3] = pix;
			}break;
		case CV_8UC3:{
			Vec3b pix = ova.at<Vec3b>(cc);
			pinVal[PIN_COLS*i+0] = pix[0];
			pinVal[PIN_COLS*i+1] = pix[1];
			pinVal[PIN_COLS*i+2] = pix[2];
			pinVal[PIN_COLS*i+3] = 0;
			}break;
		}
		DrawPinMark(ova,cc,clrMark[i]);
	}
	env->ReleaseIntArrayElements(jintArr,pinPos,0);
	env->ReleaseFloatArrayElements(jfloatArr,pinVal,0);

	/*jintArray jarr;
	jint* roival = intArray2Ptr(env,cam->clzz,thiz,"roiVal",jarr);
	for(int i=0; i<ROI_SIZE; i++){
		//check whether we need to mark ROI
		int typ= roival[i*ROI_COLS+0];
		if(typ==0){
			continue;
		}
		int xx = roival[i*ROI_COLS+1];
		int yy = roival[i*ROI_COLS+2];
		int ww = roival[i*ROI_COLS+3];
		int hh = roival[i*ROI_COLS+4];
		rectangle(*img,Rect(xx,yy,ww,hh),Scalar(5,240,5));
	}
	env->ReleaseIntArrayElements(jarr,roival,0);*/
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_namedWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname
){
	char name[500];
	jstrcpy(env,jname,name);
	namedWindow(name,CV_WINDOW_AUTOSIZE);//TODO~~~~
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_renderWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jlong ptr
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat& img = *((Mat*)ptr);
	imshow(name,img);
	waitKey(25);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_destroyWindow(
	JNIEnv * env,
	jobject thiz,
	jstring jname
){
	if(jname==NULL){
		destroyAllWindows();
	}else{
		char name[500];
		jstrcpy(env,jname,name);
		destroyWindow(name);
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_imwrite(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jlong ptrMat
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat& img = *((Mat*)ptrMat);
	imwrite(name,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_imread(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jlong ptrMat
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat& img = *((Mat*)ptrMat);
	img = imread(name,IMREAD_ANYDEPTH);
}

class ThresStdDev : public ParallelLoopBody {
private:
	int rem,lv1,lv2;
	double thrs;
	Mat node;
	Mat* edg;
public:
	ThresStdDev(
		Mat& _node,Mat* _edg,
		double val,
		int _lv1, int _lv2,
		int _rem
	):node(_node),edg(_edg),
		thrs(val),
		lv1(_lv1),lv2(_lv2),
		rem(_rem)
	{
	}
	virtual void operator()(const Range &r) const {
		int ww =(edg->cols);
		for(int i=r.start; i<r.end; i++){
			int px = i%ww;
			int py = i/ww;
			Rect roi(px,py,rem*2+1,rem*2+1);
			Scalar avg,dev;
			meanStdDev(node(roi),avg,dev);
			if(avg[0]<lv1 || lv2<avg[0]){
				continue;
			}
			if(dev[0]<=thrs){
				edg->at<uint8_t>(py,px) = 255;
			}else{
				edg->at<uint8_t>(py,px) = 0;
			}
			//if(px%2==py%2){ edg->at<uint8_t>(py,px)=255; }
		}
	}
};

void thresholdSmooth(
	Mat& img,
	Mat& edg,
	double stdDev,
	int level1, int level2,
	int blockSize
){
	if(blockSize%2==0){
		blockSize+=1;
	}
	int rem = blockSize/2;
	Mat node(
		img.rows+rem*2+1,
		img.cols+rem*2+1,
		img.type()
	);
	copyMakeBorder(
		img,node,
		rem,rem+1,
		rem,rem+1,
		BORDER_REPLICATE
	);
	parallel_for_(
		Range(0,edg.cols*edg.rows),
		ThresStdDev(node,&edg,stdDev,level1,level2,rem)
	);
}

void thresholdHist(
	Mat& img,
	Mat& edg,
	Mat& hist,
	int width,int height
){
	Mat res(img.size(),CV_32FC1);
	Mat histImg;
	int histSize = hist.rows;
	for(int j=0; j<(img.rows-height); j++){
		for(int i=0; i<(img.cols-width); i++){
			Rect roi(i,j,width,height);
			Mat _img = img(roi);
			calcHist(&_img,1,NULL,Mat(),histImg,1,&histSize,NULL);
			double diff = compareHist(hist,histImg,CV_COMP_CORREL);
			res.at<float>(j,i) = diff;
		}
	}
	normalize(res,res,255,0,NORM_INF,CV_32F);
	imwrite("cc.0.png",res);
}

/*NAT_EXPORT void linkTile(const char* nameSrc,Point2i* off){
 //calculate the offset for every tile~~~
 RawHead hd;
 FILE* fd = fopen(nameSrc,"rb");
 fread(&hd,sizeof(hd),1,fd);
 for(size_t tid=0; tid<(hd.tileCols*hd.tileRows); tid++){
 Point2i dt(0,0);
 if(hd.tileFlag==TILE_INTERLEAVE){
 long gy=tid/hd.tileCols;
 char dir=(gy%2==0)?('<'):('>');
 if(registTile(tid,dir,dt,hd,fd)==false){
 registTile(tid,'^',dt,hd,fd);
 }
 }else{
 if(registTile(tid,'<',dt,hd,fd)==false){
 registTile(tid,'^',dt,hd,fd);
 }
 }
 off[tid] = dt;
 }
 fclose(fd);
 }*/
//load result from file
/*FileStorage fs(nameSrc,FileStorage::READ);
 Mat buf;
 fs["delta"]>>buf;
 for(size_t i=0; i<buf.rows; i++){
 Point2i tmp;
 tmp.x = buf.at<int>(i,0);
 tmp.y = buf.at<int>(i,1);
 off.push_back(tmp);
 }
 fs.release();*/
//save result to file
/*char name[500]={0};
 strncpy(name,nameSrc,(dot-nameSrc));
 strncat(name,".yaml",5);
 Mat buf(off.size(),2,CV_32SC1);
 FileStorage fs(name,FileStorage::WRITE);
 for(size_t i=0; i<off.size(); i++){
 buf.at<int>(i,0) = off[i].x;
 buf.at<int>(i,1) = off[i].y;
 }
 fs<<"delta"<<buf;
 fs.release();*/

Mat* obj2mat(JNIEnv *env, jobject obj) {
	jlong addr = 0L;
	jclass _clazz = env->GetObjectClass(obj);
	jfieldID id;
	id = env->GetFieldID(_clazz, "nativeObj", "J");
	addr = env->GetLongField(obj, id);
	return (Mat*)addr;
}
jobjectArray mat2array(JNIEnv *env, Mat& src) {
	jclass clazz = env->FindClass("[F");
	if (clazz == NULL) {
		return NULL;
	}
	jobjectArray dst = env->NewObjectArray(src.rows, clazz, NULL);
	float* ptr = (float*)src.data;
	for (int j = 0; j<src.rows; j++) {
		jfloatArray row = env->NewFloatArray(src.cols);
		env->SetFloatArrayRegion(row, 0, src.cols, ptr);
		env->SetObjectArrayElement(dst, j, row);
		ptr = ptr + src.cols;
	}
	return dst;
}

void mapRect(JNIEnv *env, jobject src, Rect& dst) {
	jclass _clazz = env->GetObjectClass(src);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "I");
	dst.x = env->GetIntField(src, id);
	id = env->GetFieldID(_clazz, "y", "I");
	dst.y = env->GetIntField(src, id);
	id = env->GetFieldID(_clazz, "width", "I");
	dst.width = env->GetIntField(src, id);
	id = env->GetFieldID(_clazz, "height", "I");
	dst.height = env->GetIntField(src, id);
}
void mapRect(JNIEnv *env, Rect& src, jobject dst) {
	jclass _clazz = env->GetObjectClass(dst);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "I");
	env->SetIntField(dst, id, src.x);
	id = env->GetFieldID(_clazz, "y", "I");
	env->SetIntField(dst, id, src.y);
	id = env->GetFieldID(_clazz, "width", "I");
	env->SetIntField(dst, id, src.width);
	id = env->GetFieldID(_clazz, "height", "I");
	env->SetIntField(dst, id, src.height);
}

void mapPoint(JNIEnv *env, jobject src, Point& dst) {
	jclass _clazz = env->GetObjectClass(src);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "D");
	dst.x = env->GetDoubleField(src, id);
	id = env->GetFieldID(_clazz, "y", "D");
	dst.y = env->GetDoubleField(src, id);
}
void mapPoint(JNIEnv *env, Point& src, jobject dst) {
	jclass _clazz = env->GetObjectClass(dst);
	jfieldID id;
	id = env->GetFieldID(_clazz, "x", "D");
	env->SetDoubleField(dst, id, src.x);
	id = env->GetFieldID(_clazz, "y", "D");
	env->SetDoubleField(dst, id, src.y);
}

