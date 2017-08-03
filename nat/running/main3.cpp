#include <global.hpp>
#include <zlib.h>

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


int main(int argc, char* argv[]) {


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

int main1(int argc, char* argv[]) {

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

int example_main(int argc, char** argv){

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
    convolveDFT(Img, kernel, resim);

    //Mat resim2;
    //kernel.convertTo(kernel, CV_64FC1);
    //wienerFilter(resim, resim2, kernel, 0.01); // Apply filter

    //imshow("Kernel", kernel * 255);
    //imshow("Image", Img);
    //imshow("Result", resim);
    //cvWaitKey(0);

    return 0;
}



