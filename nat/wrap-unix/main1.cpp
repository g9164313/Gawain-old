#include <global.hpp>
#include <algorithm>

extern Mat sfrProc(Mat& src,float PixPerMM);

//some experiments
int main(int argc, char* argv[]) {

	Mat src = imread("aaa.png",IMREAD_GRAYSCALE);
	Mat freq= sfrProc(src,0.0508);
	return 0;
}
