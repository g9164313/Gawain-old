#include <global.hpp>
#include <zlib.h>

int main(int argc, char* argv[]) {

	long width = (1<<15);
	long height= (1<<15);
	void* buf = malloc(width*height);

	const int TEST_ROUND=2;
	double accumTick = 0.;
	for(int i=0; i<TEST_ROUND; i++){
		Mat img(height,width,CV_8UC1,buf);
		//Mat img(height,width,CV_8UC1);

		randu(img,Scalar(0),Scalar(255));

		long cnt;
		TICK_BEG
		threshold(img,img,200.,0.,THRESH_TOZERO);
		cnt = countNonZero(img);
		long size = (width*height)/1000000;
		cout<<"size="<<width<<"x"<<height<<"="<<size<<"MByte"<<endl;
		cout<<"result="<<cnt<<endl;
		TICK_END2("count",accumTick)

		TICK_BEG
		//vector<uchar> buf(512*1024*1024);
		/*vector<uchar> buf;
		buf.reserve(512*1024*1024);
		imencode(".tiff",img,buf);
		long size = buf.size()/1000000;
		cout<<"compressed size:"<<size<<"MByte"<<endl;
		buf.clear();*/
		TICK_END("encode")

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
