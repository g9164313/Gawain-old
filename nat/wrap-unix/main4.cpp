#include <global.hpp>
#include <zlib.h>

int main(int argc, char* argv[]) {

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


int main2(int argc, char* argv[]) {

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


