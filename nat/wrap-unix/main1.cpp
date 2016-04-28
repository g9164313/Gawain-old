#include <global.hpp>
#include <algorithm>

//some experiments
int main(int argc, char* argv[]) {
	long ptr;

	{
		ptr =(long)(new Mat());
		(*(Mat*)ptr) = imread("qq0.png");
		imwrite("test1.png",(*(Mat*)ptr));
	}

	(*(Mat*)ptr) = imread("qq1.png");
	imwrite("test2.png",(*(Mat*)ptr));

	(*(Mat*)ptr).release();
	return 0;
}
