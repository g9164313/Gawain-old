#include <global.hpp>

static const int VIEW_WIDTH=800;
static const int VIEW_HEIGHT=800;
static const char* Title="viewer";
static const char* TitleScrollH="hori.";
static const char* TitleScrollV="veri.";

static vector<Mat*> grpImage;

static void image2view(){
	//project image to view~~~
	Mat& img = *(grpImage[0]);

	Rect roi,dst;
	roi.x = getTrackbarPos(TitleScrollH,Title);//relative to image~~
	roi.y = getTrackbarPos(TitleScrollV,Title);//relative to image~~
	roi.width = VIEW_WIDTH;
	roi.height= VIEW_HEIGHT;
	valid_roi(roi,img);

	imshow(Title,img(roi));
}

static void eventScroll(int pos, void* userdata){
	image2view();
}

static MouseCallback eventUser;

static void eventMouse(int e,int x,int y,int flags,void* userdata){
	x = x + getTrackbarPos(TitleScrollH,Title);
	y = y + getTrackbarPos(TitleScrollV,Title);
	if(eventUser!=NULL){
		eventUser(e,x,y,flags,userdata);
	}
}

void showViewer(Mat& image,MouseCallback event,void* userdata){
	int ww = image.cols;
	int hh = image.rows;

	namedWindow(Title);
	createTrackbar(TitleScrollH,Title,NULL,(ww>VIEW_WIDTH )?(ww-VIEW_WIDTH ):(1),eventScroll);
	createTrackbar(TitleScrollV,Title,NULL,(hh>VIEW_HEIGHT)?(hh-VIEW_HEIGHT):(1),eventScroll);
	setMouseCallback(Title,eventMouse,userdata);
	eventUser = event;

	grpImage.clear();
	grpImage.push_back(&image);

	image2view();
	while((char)waitKey(10)!=27){
	}

	destroyAllWindows();
}
