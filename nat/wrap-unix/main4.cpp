#include <global.hpp>
#include <grabber.hpp>
#include <utils_ipc.hpp>

extern void registration(
	Mat& _ref,
	Mat& _src,
	double* angle=NULL,
	double* response=NULL
);
extern void imposition(const char* name,Mat& ref,Mat& src);

int main(int argc, char* argv[]) {

	#define PATH "./cam0/6S正面LSP350_20160225_重做20160202/"
	#define PATH1 "./cam0/6S正面LSP350_20160225_重做20160202/sample0226/"
	//Mat ref = imread(PATH"Chip1Light120t15ms_locate01.bmp",IMREAD_GRAYSCALE);
	//Mat src = imread(PATH"Chip1Light140t15ms_locate10.bmp",IMREAD_GRAYSCALE);
	//Mat src = imread(PATH1"Sp01L120Located02.bmp",IMREAD_GRAYSCALE);

	Mat ref = imread("qq0.png",IMREAD_GRAYSCALE);
	Mat src = imread("qq3.png",IMREAD_GRAYSCALE);

	TICK_BEG
	registration(ref,src);
	TICK_END("regist")

	imwrite("aa1.png",ref);
	imwrite("aa2.png",src);

	imposition("aa3.png",ref,src);
	return 0;
}
