#include <global.hpp>

extern void filterNMText(Mat& src,vector<Rect>& boxes);

int main(int argc, char* argv[]) {

	long width = (1<<15);
	long height= (1<<15);
	void* buf = malloc(width*height);

	Mat img(height,width,CV_8UC1,buf);
	//Mat img(height,width,CV_8UC1);

	randu(img,Scalar(0),Scalar(255));

	long cnt;
	TICK_BEG
	threshold(img,img,100.,0.,THRESH_TOZERO);
	cnt = countNonZero(img);
	TICK_END("test")

	long area = width*height;
	cout<<"size="<<width<<"x"<<height<<"="<<area<<endl;
	cout<<"result="<<cnt<<endl;

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
