#include <global.hpp>

extern void filterNMText(Mat& src,vector<Rect>& boxes);

int main(int argc, char* argv[]) {

	//Mat src = imread("img1_001.png");
	Mat src = imread("gg5.jpg");
	vector<Rect> boxes;
	filterNMText(src,boxes);

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
