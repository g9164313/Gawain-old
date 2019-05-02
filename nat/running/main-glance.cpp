#include <stdio.h>
#include <stdarg.h>
#include <math.h>
#include <fcntl.h>
#include <termios.h> /* POSIX terminal control definitions */
#include <unistd.h>
#include <string.h>
#include <iostream>
#include <fstream>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include <m3api/xiApi.h>

using namespace cv;
using namespace std;

#define HandleResult(res,place) if (res!=XI_OK) {printf("Error after %s (%d)\n",place,res);goto finish;}


static int ttyID = -1;

void send(const char* txt){
	size_t cnt = strlen(txt);
	write(ttyID, (const void*)txt, cnt);
	usleep(100);
}


char* recv(){	
	int idx = 0;
	static char buf[1024];
	memset(buf,0,sizeof(buf));
	do {		
		int cnt = read(ttyID, buf+idx, 1024);
		if (cnt<=0) {
			continue;
		}		
		idx+=cnt;		
	} while(strchr(buf,'\n')==NULL);
	tcflush(ttyID, TCIFLUSH);
	return buf;
}


void initNanoPZ(){

	ttyID = open("/dev/ttyUSB0", O_RDWR | O_NOCTTY | O_NDELAY);
	if(ttyID<0){
		printf("fail to open TTY\n");
		return;
	}

	struct termios opts;
	tcgetattr(ttyID, &opts);
	
	opts.c_cflag &= ~PARENB;
	opts.c_cflag &= ~CSTOPB;
	opts.c_cflag &= ~CSIZE;
	opts.c_cflag |= (CS8 | CLOCAL | CREAD);
	
	opts.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
	opts.c_iflag |= (INPCK | ISTRIP);
	opts.c_iflag &= ~(IXON | IXOFF | IXANY);
	
	opts.c_oflag = 0;
	
	opts.c_cc[VMIN] = 1; /* block until n bytes are received */
	opts.c_cc[VTIME] = 40; /* block until a timer expires (n * 100 mSec.) */
	cfsetispeed(&opts, B19200);
	cfsetospeed(&opts, B19200);	
		
	tcsetattr(ttyID, TCSANOW, &opts);
	
	fcntl(ttyID, F_SETFL, 0);

	//send("1MO?\r");
	//send("1ID?\r");	
	//printf("nanoPZ: %s\n",recv());
}

void moveNanoPZ(int ustep){

	send("1TP?\r");
	printf("@=>nanoPZ:%s\r\n",recv());
	
	//char txt[100];	
	//sprintf(txt,"1PR%d\r\n",ustep);
	//send(txt);
	
	char* msg = NULL;
	do{
		send("1TS?\r");
		msg = recv();
		printf("--> %s",msg);
	}while(strchr(msg,'Q')==NULL);
	
	send("1TP?\r");
	printf("#=>nanoPZ:%s\r\n",recv());
}

void stopNanoPZ(){
	close(ttyID);
}
//------------------------------------//

void phase_calculate(const char* dir,int num, ...){

	char temp[200];

	Mat img[10];

	Rect roi(417,211,840,840);

	va_list valist;
	va_start(valist, num);
	for(int i=0; i<num; i++){
		const char* name = va_arg(valist, const char*);
		if(name==NULL){
			continue;
		}
		sprintf(temp,"%s/%s",dir,name);
		Mat src = imread(temp,IMREAD_GRAYSCALE);
		img[i] = src(roi);

	}
	va_end(valist);

	Mat phi(roi.height, roi.width, CV_32FC1);

	for(int y=0; y<roi.height; y++){
		for(int x=0; x<roi.width; x++){
			float I1 = img[0].at<uint8_t>(y,x);
			float I2 = img[1].at<uint8_t>(y,x);
			float I3 = img[2].at<uint8_t>(y,x);
			float I4 = img[3].at<uint8_t>(y,x);
			float rad = (I4 - I2) / (I1 - I3);
			phi.at<float>(y,x) = atan(rad);
		}
	}

	//show mapping~~~

	//Mat node = (phi + M_PI/2.) * (255./M_PI);
	//Mat gray;
	Mat nod, map;
	phi.convertTo(nod,CV_8UC1, (255./M_PI), (255./2.));

	applyColorMap(nod, map, COLORMAP_JET);

	sprintf(temp,"%s/%s",dir,"result-deg.png");
	imwrite(temp,map);
	return;
}
//------------------------------------//

int main(int argc, char* argv[]) {

	phase_calculate(
		"./grille/michelson",
		4,
		"ss1.jpg",
		"ss2.jpg",
		"ss3.jpg",
		"ss4.jpg"
	);
	cout<<"process...."<<endl;

	return 0;
}

int main_2(int argc, char* argv[]) {
	initNanoPZ();
	//moveNanoPZ(1000);	
	stopNanoPZ();
	return 0;
}

int main_1(int argc, char* argv[]) {

	int ustep = 1000;
	char txt[100];

	if(argc>=2){
		ustep = atoi(argv[1]);
	}
	
	XI_IMG xImg;

	memset(&xImg,0,sizeof(xImg));
	xImg.size = sizeof(XI_IMG);

	// Sample for XIMEA API V4.05
	HANDLE xiH = NULL;
	XI_RETURN stat = XI_OK;

	// Retrieving a handle to the camera device
	printf("Opening first camera...\n");
	stat = xiOpenDevice(0, &xiH);
	HandleResult(stat,"xiOpenDevice");

	// Setting "exposure" parameter (10ms=10000us)
	stat = xiSetParamInt(xiH, XI_PRM_EXPOSURE, 43000);
	HandleResult(stat,"set exposure");

	stat = xiSetParamFloat(xiH, XI_PRM_GAIN, 37);
	HandleResult(stat,"set gain");

	//stat = xiSetParamInt(xiH, XI_PRM_AEAG, XI_ON);
	//HandleResult(stat,"set AEGA");

	printf("Acquisition...\n");
	stat = xiStartAcquisition(xiH);

	initNanoPZ();

	#define EXPECTED_IMAGES 6
	for (int idx=0; idx<EXPECTED_IMAGES; idx++){

		// getting image from camera
		stat = xiGetImage(xiH, 500, &xImg);

		Mat img(
			(int)xImg.height, (int)xImg.width,
			CV_8UC1,
			(void*)xImg.bp
		);

		char name[50];
		sprintf(name,"./pic-%02d.png",idx);
		imwrite(name,img);

		printf("Got picture - %s\n", name);
		
		sprintf(txt,"1PR%d\r\n",ustep);
		send(txt);
		usleep(1000*1000);
	}

	sprintf(txt,"1PR%d\r\n",ustep*-1*EXPECTED_IMAGES);
	send(txt);

	xiStopAcquisition(xiH);
	xiCloseDevice(xiH);

finish:
	stopNanoPZ();
	printf("Done\n");
	return 0;
}

