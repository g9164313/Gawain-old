#include <global.hpp>

using namespace std;
using namespace cv;

static const int ALPHA=4;//this variable means how many sub-pixel

void dump32FC1(const char* title,Mat& dat){
	printf("%s=[\n",title);
	for(int i=0; i<dat.cols; i++){
		float v = dat.at<float>(0,i);
		//if(i%8==0){
		//	printf("..\n");
		//}
		printf("%d,  %.4f\n",i+1,v);
	}
	printf("];\n");
}

int locate_centroids(Mat& img,vector<Point2f>& shift){
	Mat node;
	Sobel(img,node,CV_32FC1,1,0,1);
	int sz=node.rows;
	double dt;
	Moments mo;
	Point2f pt;
	for(int i=0; i<sz; i++){
		mo = moments(node.row(i));
		pt.x = i;
		pt.y = mo.m10/mo.m00;
		shift.push_back(pt);
	}
	//TODO:check slang edge in boundary~~~
	return 0;
}

int bin_to_regular_xgrid(vector<Point2f>& edge, Mat& img, Mat& bin){
	Mat cnt = Mat::zeros(bin.size(),CV_32SC1);
	for(int j=0; j<edge.size(); j++){
		int align =  cvRound(bin.cols/2 - (edge[j].x*ALPHA));//we shift edge to the center~~
		for(int i=0; i<img.cols; i++){
			int pos = i*ALPHA + align;
			if(pos<0 || bin.cols<=pos){
				break;
			}
			float pix = img.at<uint8_t>(j,i);
			bin.at<float>(0,pos) = bin.at<float>(0,pos) + pix;
			cnt.at<int>(0,pos) = cnt.at<int>(0,pos) + 1;
		}
	}
	for(int i=0; i<bin.cols; i++){
		int total = cnt.at<int>(0,i);
		if(total==0){
			//TODO:stuff this value~~~
			continue;
		}
		bin.at<float>(0,i) = bin.at<float>(0,i) / total;
	}
	normalize(
		bin, bin,
		0., 1.,
		NORM_MINMAX
	);
	return 0;
}

int calculate_derivative(Mat& signal){
	//Sobel(signal,signal,CV_32FC1,1,0,1);
	#define SPACE 1
	//#define SPACE 0
	double dt0=0., dt1=0.;
	Mat tmp;
	signal.copyTo(tmp);
	int cols = signal.cols;
	for(int i=1; i<cols-SPACE; i++){
		float val  = tmp.at<float>(0,i+SPACE) - tmp.at<float>(0,i-1);
		if(SPACE==1){
			val = val / 2.f;
		}
		signal.at<float>(0,i) = val;
	}

	signal.at<float>(0,0) = signal.at<float>(0,1);
	if(SPACE==1){
		signal.at<float>(0,cols-1) = signal.at<float>(0,cols-2);
	}

	Moments mo = moments(signal);
	return cvRound(mo.m10/mo.m00);//centroid!!!
}

void locate_max_PSF(Mat& signal,int center){
	//shift signal to center
	int len = signal.cols;
	if(center==len/2){
		return;
	}
	Mat tmp;
	signal.copyTo(tmp);	
	signal = signal * 0.;//reset it!!!
	for(int i=0; i<len; i++){
		int j = (i-center)+len/2;
		if(j<0 || len<=j){
			continue;
		}else{
			signal.at<float>(0,j) = tmp.at<float>(0,i);
		}
	}
	return ;
}

void apply_hamming_window(Mat& signal){
	Mat hamm = Mat::zeros(signal.size(),CV_32FC1);
	for(int i=0, j=-hamm.cols/2; i<hamm.cols; i++, j++){
		double idx = (2.*M_PI*(double)j)/hamm.cols;
		hamm.at<float>(0,i) = 0.53836 + 0.46164 * cos(idx);
	}
	multiply(hamm, signal, signal);
}

void ftwos_v1(Mat& signal, Mat& freq){
	int len = signal.cols;
	freq = Mat::zeros(1,signal.cols/2,CV_32FC1);
	Mat plan[]={
		Mat::zeros(1,len,CV_32FC1),
		Mat::zeros(1,len,CV_32FC1)
	};
	signal.copyTo(plan[0]);
	//"edge"(black or white) will be split into the complex of real and image part
	Mat node,complx;
	merge(plan,2,node);   
	dft(node,complx,DFT_ROWS);
	split(complx,plan);
	magnitude(plan[0], plan[1], plan[0]);
	plan[1] = plan[0] + Scalar::all(1); 
	log(plan[1],plan[0]);

	plan[0].colRange(0,len/2).copyTo(freq);

	float dc = freq.at<float>(0,0);
	freq = freq / dc;//normalize!!!
}

Mat ftwos_v2(Mat& signal,float PixPerMM){
	float freqStp = ALPHA/(PixPerMM*signal.cols);
	float freqPos = 0.f;
	float freqDCV = 1.f;
	float ds = 1./signal.cols;

	Mat freq = Mat::zeros(1,signal.cols/ALPHA,CV_32FC2);

	for(int j=0; j<freq.cols; j++){
		double g = 2.* M_PI * ds * (double)j;
		double a=0.,b=0.;
		for (int i=0; i<signal.cols; i++) { 
			double idx = g * (double)(i);
			a = a + signal.at<float>(0,i) * cos(idx);
			b = b + signal.at<float>(0,i) * sin(idx);
		}
		double freqVal = sqrt(a * a + b * b);
		if(j==0){
			freqDCV = freqVal;
		}
		freq.at<Vec2f>(0,j) = Vec2f(
			freqPos,
			freqVal/freqDCV
		);
		//printf("%6.3f, %.3f\n", freqPos,freqVal);
		freqPos = freqPos + freqStp;
	}

	return freq;
}

Mat sfrProc(Mat& src,float PixPerMM){

	vector<Point2f> edge;

	locate_centroids(src,edge);

	Vec4f ln;
	fitLine(edge,ln,CV_DIST_L2,0,1,0.01);
	float slope=ln[1]/ln[0];
	float bias=ln[3]-ln[2]*slope;
	//TODO: check slope is valid~~

	int off,rng = src.rows;
	//trick, just get a integer multiple of 'slope'
	rng = ((int32_t)(rng*slope)) * (1.f/slope);
	off = (src.rows - rng)/2;
	//re-composite edge line~~
	edge.clear();
	for(int i=off; i<(off+rng); i++){
		//swap the coordinate~~~
		edge.push_back(Point2f(i*slope+bias,i-off));
	}

	//prepare the map of signal and edge
	Mat node = src(Rect(
		0, off,
		src.cols, rng
	));
	
	Mat bins = Mat::zeros(1,node.cols*ALPHA,CV_32FC1);

	bin_to_regular_xgrid(edge,node,bins);
	
	int center = calculate_derivative(bins);

	locate_max_PSF(bins,center);

	apply_hamming_window(bins);

	return ftwos_v2(bins,PixPerMM);

	//dump32FC1("_sfr2",freq);

	//1 cycle = 1 pixel, freq=cycle/mm for image width

	//check.point
	/*imwrite("cc.0.png",node);
	Mat map;
	cvtColor(node,map,COLOR_GRAY2BGR);
	for(int i=0; i<edge.size(); i++){
		map.at<Vec3b>(i,edge[i].x) = Vec3b(255,0,0);
	}
	imwrite("cc.1.png",map);*/
}
//---------------------------//

extern "C" JNIEXPORT jfloatArray JNICALL Java_prj_daemon_FltrSlangEdge_procSFR(
	JNIEnv* env,
	jobject thiz,
	jobject camBundle,
	jlong ptrImg,
	jlong ptrOva
){
	Mat& img = *((Mat*)ptrImg);
	Mat& ova = *((Mat*)ptrOva);
	if(img.type()==CV_8UC1){
		return NULL;
	}

	jfloatArray res = env->NewFloatArray(100);
	jfloat* buf = env->GetFloatArrayElements(res,0);
	env->ReleaseFloatArrayElements(res,buf,0);
	return res;
}






