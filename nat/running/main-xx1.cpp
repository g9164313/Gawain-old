#include <global.hpp>
#include <zlib.h>
#include <vector>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include "opencv2/stitching/detail/blenders.hpp"
#include <opencv2/ximgproc.hpp>
#include <opencv2/xobjdetect.hpp>
#include <opencv2/xphoto.hpp>
#include <opencv2/xfeatures2d.hpp>

using namespace xobjdetect;
using namespace ximgproc;
using namespace xfeatures2d;

vector<Point2f> Points(vector<KeyPoint> keypoints)
{
    vector<Point2f> res;
    for(unsigned i = 0; i < keypoints.size(); i++) {
        res.push_back(keypoints[i].pt);
    }
    return res;
}

int main(int argc, char** argv){

	Mat img1 = imread("./cv_sample1/box.png", IMREAD_GRAYSCALE);
	//Mat img1 = imread("./ww1.jpg", IMREAD_GRAYSCALE);
	Mat img2 = imread("./cv_sample1/box_in_scene.png", IMREAD_GRAYSCALE);

	vector<KeyPoint> keypoints1,keypoints2;
	Mat descriptors1, descriptors2;

	Ptr<Feature2D> surf = SIFT::create();
	surf->detectAndCompute(img1, Mat(), keypoints1, descriptors1);
	surf->detectAndCompute(img2, Mat(), keypoints2, descriptors2);

	vector<DMatch> matches;
	Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create( "BruteForce" );
	matcher->match(descriptors1,descriptors2,matches);
	//FlannBasedMatcher matcher;
	//matcher.match(keypoints1,keypoints2,matches);


	vector<DMatch> mm;
	vector<Point2f> mp1, mp2;
	for(vector<DMatch>::iterator iter=matches.begin(); iter<matches.end(); ++iter) {
		DMatch& obj = (*iter);
		if(obj.distance<100){
			//cout<<"IDX="<<obj.queryIdx<<","<<obj.trainIdx<<" @ "<<obj.distance<<endl;
			mm.push_back(obj);
			mp1.push_back(keypoints1[obj.queryIdx].pt);
			mp2.push_back(keypoints2[obj.trainIdx].pt);
		}
	}

	Mat img_matches;
	drawMatches(img1,keypoints1, img2,keypoints2, mm, img_matches);
	imwrite("qq1.png",img_matches);

	Mat homography = findHomography(mp1,mp2,CV_RANSAC);
	cout<<homography<<endl;

	Mat t_img1;
	warpPerspective(img1,t_img1,homography,img2.size());
	//imwrite("qq2.png",t_img1);

	Mat dst;
	addWeighted(t_img1,0.7, img2,0.3, 0., dst);
	imwrite("qq3.png",dst);
	//Mat qq1;
	//drawKeypoints(img1,keypoints1,qq1);
	return 0;
}
//-------------------------------//

int main2(int argc, char* argv[]) {

	//test writing speed
	for(int i=1;i<=5; i++){

		long width = (1<<(i+10));

		void* buf = malloc(width*width);

		Mat img(width,width,CV_8UC1,buf);
		img = img * 0;
		//randu(img,Scalar(0),Scalar(255));

		char name[60];
		sprintf(name,"volbin-%d.tif",i);

		cout<<"write "<<name<<", size="<<(width*width)<<"bytes."<<endl;

		TICK_BEG

		imwrite(name,img);

		TICK_END("I/O")

		free(buf);

		cout<<endl;
	}
	return 0;
}
//-------------------------------//

int main3(int argc, char* argv[]) {

	long width = (1<<15);
	long height= (1<<15);
	void* buf = malloc(width*height);

	double accumTick = 0.;

	const int TEST_ROUND=5;

	for(int i=0; i<TEST_ROUND; i++){

		Mat img(height,width,CV_8UC1,buf);

		randu(img,Scalar(0),Scalar(255));

		/*long cnt;
		TICK_BEG
		threshold(img,img,200.,0.,THRESH_TOZERO);
		cnt = countNonZero(img);
		long size = (width*height)/1000000;
		cout<<"size="<<width<<"x"<<height<<"="<<size<<"MByte"<<endl;
		cout<<"result="<<cnt<<endl;
		TICK_END2("count",accumTick)*/

		//TICK_BEG
		//vector<uchar> buf(512*1024*1024);
		/*vector<uchar> buf;
		buf.reserve(512*1024*1024);
		imencode(".tiff",img,buf);
		long size = buf.size()/1000000;
		cout<<"compressed size:"<<size<<"MByte"<<endl;
		buf.clear();*/
		//TICK_END("encode")

		cout<<endl;
	}

	accumTick = accumTick/TEST_ROUND;

	cout<<endl<<"average time="<<accumTick<<"sec"<<endl;

	free(buf);
	//Mat ref = imread("qq0.png",IMREAD_GRAYSCALE);
	//Mat src = imread("qq3.png",IMREAD_GRAYSCALE);
	//TICK_BEG
	//registration(ref,src);
	//TICK_END("regist")
	//imwrite("aa1.png",ref);
	//imwrite("aa2.png",src);
	//imposition("aa3.png",ref,src);
	return 0;
}

//--------------------------------------------//

struct RAW_HEAD {
	uint32_t type;
	uint32_t rows;
	uint32_t cols;
	uint32_t tileFlag;
	uint32_t tileRows;
	uint32_t tileCols;
};

int main5(int argc, char** argv){
	//try to read some old file

	FILE* fd = fopen("pano.1.raw","r");

	RAW_HEAD hd;

	fread(&hd,sizeof(hd),1,fd);

	size_t total = hd.cols*hd.rows;

	uint8_t* buf = new uint8_t[total];

	for(int j=0; j<hd.tileRows; j++){

		for(int i=0; i<hd.tileCols; i++){

			fread(buf,total,1,fd);

			Mat img(hd.rows,hd.cols,hd.type,buf);

			char name[100];
			if(j%2==0){
				sprintf(name,"./pano/img+%02d+%02d.tiff",j,i);
			}else{
				sprintf(name,"./pano/img+%02d+%02d.tiff",j,hd.tileCols-1-i);
			}
			imwrite(name,img);

			cout<<"dump "<<name<<endl;
		}
	}
	delete buf;
	fclose(fd);
	cout<<"done"<<endl;
	return 0;
}
//------------------------------//

#include "opencv2/stitching.hpp"

using namespace cv::detail;

static double rateFrame(Mat& frame){

    unsigned long int sum = 0;
    unsigned long int size = frame.cols * frame.rows;
    Mat edges;
    cvtColor(frame, edges, CV_BGR2GRAY);
    GaussianBlur(edges, edges, Size(7, 7), 1.5, 1.5);
    imwrite("cc1.png",edges);

    Canny(edges, edges, 0, 30, 3);
    imwrite("cc2.png",edges);

    MatIterator_<uchar> it, end;
    for (
    	it = edges.begin<uchar>(), end = edges.end<uchar>();
    	it != end;
    	++it
	){
        sum += (*it != 0);
    }
    return (double) sum / (double) size;
}

int main6(int argc, char** argv){

	//Mat aa1 = imread("./aa1.jpg");
	Mat aa1 = imread("./artificial-1.png");
	Mat aa2 = imread("./aa2.jpg");
	Mat aa3 = imread("./aa2.jpg");

	double rate = rateFrame(aa1);
	cout<<"%%%"<<rate<<endl;

	Mat pano;
	vector<Mat> imgs;
	imgs.push_back(aa1);
	imgs.push_back(aa2);
	imgs.push_back(aa3);
	Stitcher stitcher = Stitcher::createDefault(false);
	Stitcher::Status status = stitcher.stitch(imgs, pano);

	imwrite("cc3.png",pano);
	//aa1.convertTo(aa1_s, CV_16SC3);
	//aa2.convertTo(aa2_s, CV_16SC3);

	Mat aa1_s, aa2_s, dst, dst_m;
	Mat msk1 = Mat::ones(aa1.size(), CV_8UC1);
	Mat msk2 = Mat::ones(aa2.size(), CV_8UC1);

	//FeatherBlender qq;
	MultiBandBlender qq;

	Mat edg;
	Canny(aa1,edg,500,1000);

	qq.prepare(Rect(0,0,aa1.cols,aa1.rows*2));
	//qq.feed(aa1_s,msk1,Point(0,0));
	//qq.feed(aa2_s,msk2,Point(0,1213));
	qq.feed(aa1,msk1,Point(0,0));
	qq.feed(aa2,msk2,Point(0,0));
	qq.blend(dst,dst_m);

	imwrite("cc3.png",dst);

	return 0;
}
//------------------------------//

int main7(int argc, char** argv){
	const int ROW = 256;

	//製作 color map 的指示
	Mat src(1,ROW,CV_8UC1);
	for(int i=0; i<256; i++){
		src.at<uint8_t>(0,i) = 255 - i;
	}
	cout<<"--sample--"<<endl;
	cout<<"src="<<src<<endl<<endl;

	Mat dst;
	applyColorMap(src, dst, COLORMAP_RAINBOW);
	cout<<"--mapping--"<<endl;
	cout<<"ch="<<dst.channels()<<endl;
	cout<<"private int[] rainbow = {"<<endl;

	const int COLS = 8;

	for(int i=0; i<ROW; i++){
		Vec3b pix = dst.at<Vec3b>(0,i);
		int val =0;
		val = val | (((uint32_t)pix[2])<<16);
		val = val | (((uint32_t)pix[1])<<8);
		val = val | (((uint32_t)pix[0]));
		val = val | 0x80000000;

		int col = i%COLS;
		//if(col==0){ cout<<"    "; }
		printf("0x%08X, ",val);
		if(col==(COLS-1)){
			cout<<endl;
		}
	}
	cout<<"};"<<endl;

	return 0;
}
//------------------------------//

//below lines comes from https://stackoverflow.com/questions/40713929/weiner-deconvolution-using-opencv
//thanks, Andrey Smorodov

void Recomb(Mat &src, Mat &dst){
    int cx = src.cols >> 1;
    int cy = src.rows >> 1;
    Mat tmp;
    tmp.create(src.size(), src.type());
    src(Rect( 0,  0, cx, cy)).copyTo(tmp(Rect(cx, cy, cx, cy)));
    src(Rect(cx, cy, cx, cy)).copyTo(tmp(Rect(0, 0, cx, cy)));
    src(Rect(cx,  0, cx, cy)).copyTo(tmp(Rect(0, cy, cx, cy)));
    src(Rect( 0, cy, cx, cy)).copyTo(tmp(Rect(cx, 0, cx, cy)));
    dst = tmp;
}

void convolveDFT(Mat& A, Mat& B, Mat& C)
{
    // reallocate the output array if needed
    C.create(abs(A.rows - B.rows) + 1, abs(A.cols - B.cols) + 1, A.type());

    Size dftSize;
    // compute the size of DFT transform
    dftSize.width = getOptimalDFTSize(A.cols + B.cols - 1);
    dftSize.height = getOptimalDFTSize(A.rows + B.rows - 1);

    // allocate temporary buffers and initialize them with 0's
    Mat tempA(dftSize, A.type(), Scalar::all(0));
    Mat tempB(dftSize, B.type(), Scalar::all(0));

    // copy A and B to the top-left corners of tempA and tempB, respectively
    Mat roiA(tempA, Rect(0, 0, A.cols, A.rows));
    A.copyTo(roiA);
    Mat roiB(tempB, Rect(0, 0, B.cols, B.rows));
    B.copyTo(roiB);

    // now transform the padded A & B in-place;
    // use "nonzeroRows" hint for faster processing
    dft(tempA, tempA, 0, A.rows);
    dft(tempB, tempB, 0, A.rows);

    // multiply the spectrums;
    // the function handles packed spectrum representations well
    mulSpectrums(tempA, tempB, tempA, 0);
    // transform the product back from the frequency domain.
    // Even though all the result rows will be non-zero,
    // you need only the first C.rows of them, and thus you
    // pass nonzeroRows == C.rows

    dft(tempA, tempA, DFT_INVERSE + DFT_SCALE);
    // now copy the result back to C.
    C = tempA(Rect((dftSize.width - A.cols) / 2, (dftSize.height - A.rows) / 2, A.cols, A.rows)).clone();
    // all the temporary buffers will be deallocated automatically
}

//----------------------------------------------------------
// Compute Re and Im planes of FFT from Image
//----------------------------------------------------------
void ForwardFFT(Mat &Src, Mat *FImg)
{
    int M = getOptimalDFTSize(Src.rows);
    int N = getOptimalDFTSize(Src.cols);
    Mat padded;
    copyMakeBorder(Src, padded, 0, M - Src.rows, 0, N - Src.cols, BORDER_CONSTANT, Scalar::all(0));
    Mat planes[] = { Mat_<double>(padded), Mat::zeros(padded.size(), CV_64FC1) };
    Mat complexImg;
    merge(planes, 2, complexImg);
    dft(complexImg, complexImg);
    split(complexImg, planes);
    // crop result
    planes[0] = planes[0](Rect(0, 0, Src.cols, Src.rows));
    planes[1] = planes[1](Rect(0, 0, Src.cols, Src.rows));
    FImg[0] = planes[0].clone();
    FImg[1] = planes[1].clone();
}
//----------------------------------------------------------
// Compute image from Re and Im parts of FFT
//----------------------------------------------------------
void InverseFFT(Mat *FImg, Mat &Dst)
{
    Mat complexImg;
    merge(FImg, 2, complexImg);
    dft(complexImg, complexImg, DFT_INVERSE + DFT_SCALE);
    split(complexImg, FImg);
    Dst = FImg[0];
}
//----------------------------------------------------------
// wiener Filter
//----------------------------------------------------------
void wienerFilter(Mat &src, Mat &dst, Mat &_h, double k)
{
    //---------------------------------------------------
    // Small epsilon to avoid division by zero
    //---------------------------------------------------
    const double eps = 1E-8;
    //---------------------------------------------------
    int ImgW = src.size().width;
    int ImgH = src.size().height;
    //--------------------------------------------------
    Mat Yf[2];
    ForwardFFT(src, Yf);
    //--------------------------------------------------
    Mat h = Mat::zeros(ImgH, ImgW, CV_64FC1);

    int padx = h.cols - _h.cols;
    int pady = h.rows - _h.rows;

    copyMakeBorder(_h, h,
    	pady / 2, pady - pady / 2,
		padx / 2, padx - padx / 2,
		BORDER_CONSTANT,
		Scalar::all(0)
    );

    Mat Hf[2];
    ForwardFFT(h, Hf);


    //--------------------------------------------------
    Mat Fu[2];
    Fu[0] = Mat::zeros(ImgH, ImgW, CV_64FC1);
    Fu[1] = Mat::zeros(ImgH, ImgW, CV_64FC1);

    complex<double> a;
    complex<double> b;
    complex<double> c;

    double Hf_Re;
    double Hf_Im;
    double Phf;
    double hfz;
    double hz;
    double A;

    for (int i = 0; i < h.rows; i++)
    {
        for (int j = 0; j < h.cols; j++)
        {
            Hf_Re = Hf[0].at<double>(i, j);
            Hf_Im = Hf[1].at<double>(i, j);
            Phf = Hf_Re*Hf_Re + Hf_Im*Hf_Im;
            hfz = (Phf < eps)*eps;
            hz = (h.at<double>(i, j) > 0);
            A = Phf / (Phf + hz + k);
            a = complex<double>(Yf[0].at<double>(i, j), Yf[1].at<double>(i, j));
            b = complex<double>(Hf_Re + hfz, Hf_Im + hfz);
            c = a / b; // Deconvolution :) other work to avoid division by zero
            Fu[0].at<double>(i, j) = (c.real()*A);
            Fu[1].at<double>(i, j) = (c.imag()*A);
        }
    }
    InverseFFT(Fu, dst);
    Recomb(dst, dst);
}

void shape_gain(Mat& dat){

	Mat plan[] ={
		Mat_<double>(dat),
		Mat::zeros(dat.size(), CV_64FC1)
	};
	//cout<<"cc1="<<plan[0]<<endl;

	Mat comp;
	merge(plan, 2, comp);
	dft(comp, comp);
	split(comp, plan);

	//cout<<"cc3="<<plan[0]<<endl;
	//cout<<"cc4="<<plan[1]<<endl;
	//plan[0] = plan[0].mul(shape);
	//plan[1] = plan[1].mul(shape);

	int nn = plan[0].cols;
	int n  = plan[0].cols/2;

	//double DC1 = plan[0].at<double>(0,0);//keep this~~~
	//double AC1 = plan[0].at<double>(0,n);

	//cout<<"val=[";
	for(int i=1; i<n; i++){
		//double scale = 1 - abs( (i-(n-1)/2.) / (n/2.) );
		double idx = (i)/((double)n);
		double val = sin(M_PI*idx)/(M_PI*idx);
		//cout<<val<<",";
		//plan[0].at<double>(0,i) = plan[0].at<double>(0,i)*2.3;
		plan[0].at<double>(0,i) = val;
	}
	//cout<<"]"<<endl;

	//plan[0].at<double>(0,0) = DC1;
	//plan[0].at<double>(0,n) = AC1;


	//Mat chk;
	//plan[0].convertTo(chk,CV_16S);
	//cout<<"cc2="<<chk<<endl;

	merge(plan, 2, comp);
	idft(comp, comp, DFT_SCALE);
	split(comp, plan);
	plan[0].convertTo(dat,CV_8UC1);
	//cout<<"cc3="<<dat<<endl;

	return;
}

int main4(int argc, char** argv){

    Mat Img = imread("F:\\ImagesForTest\\lena.jpg", 0); // Source image
    Img.convertTo(Img, CV_32FC1, 1.0 / 255.0);

    Mat kernel = imread("F:\\ImagesForTest\\Point.jpg", 0); // PSF
    //resize(kernel, kernel, Size(), 0.5, 0.5);

    kernel.convertTo(kernel, CV_32FC1, 1./255.);

    float kernel_sum = cv::sum(kernel)[0];
    kernel /= kernel_sum;

    int width = Img.cols;
    int height= Img.rows;
    Mat resim;
    //convolveDFT(Img, kernel, resim);
    //Mat resim2;
    //kernel.convertTo(kernel, CV_64FC1);
    //wienerFilter(resim, resim2, kernel, 0.01); // Apply filter
    //imshow("Kernel", kernel * 255);
    //imshow("Image", Img);
    //imshow("Result", resim);
    //cvWaitKey(0);

    return 0;
}
//--------------------------//

int main1(int argc, char* argv[]) {


	Mat src = imread("reg-xxr.bmp");
	Mat chn[3];
	split(src,chn);

	for(int c=0; c<3; c++){
		Mat img = chn[c];
		for(int i=0; i<img.rows; i++){
			Mat line = img.row(i);
			/*for(int j=0; j<line.cols; j+=16){
				Mat sect = line.colRange(j,j+16);
				shape_gain(sect,tri);
			}*/
			shape_gain(line);
		}
	}

	merge(chn, 3, src);
	//imwrite("reg-xxx.png",src(Rect(1617,73,103,63)));
	imwrite("reg-xxy.bmp",src);
	return 0;
}
//-------------------------------//
