#include <stdio.h>   /* Standard input/output definitions */
#include <string.h>  /* String function definitions */
#include <unistd.h>  /* UNIX standard function definitions */
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <iostream>
#include <fstream>
#include <global.hpp>

using namespace std;

//some experiments
int main2(int argc, char* argv[]) {

	size_t ww = 640;
	size_t hh = 480*5000;

	void* buf = malloc(hh*ww*3);

	Mat img(hh,ww,CV_8UC3,buf);

	//randu(img,Scalar(0),Scalar(255));

	imwrite("test.tif",img);

	free(buf);

	return 0;
}

int main_1(int argc, char* argv[]) {

	int fd = open("/dev/ttyS0", O_RDWR | O_NOCTTY);

	struct termios options;
	tcgetattr(fd,&options);
	cfmakeraw(&options);

	options.c_cflag = CS8 | 0 | 0 | CLOCAL | CREAD;
	options.c_iflag = IGNPAR;
	options.c_oflag = 0;
	options.c_lflag = 0;
	options.c_cc[VMIN] = 0;      /* block until n bytes are received */
	options.c_cc[VTIME] = 10;     /* block until a timer expires (n * 100 mSec.) */
	cfsetispeed(&options,B115200);
	cfsetospeed(&options,B115200);
	fcntl(fd,F_SETFL,0);
	tcsetattr(fd,TCSANOW,&options);


	write(fd,"MG TIME\r",strlen("MG TIME\r"));
	int cnt = 0;
	char buf[1024];
	do{
		cout<<"waiting..."<<cnt<<endl;
		cnt = read(fd,buf,1024);
		if(cnt==-1){
			continue;
		}
		buf[cnt] = 0;
	}while(cnt<=0);
	cout<<"we got "<<buf<<endl;
	cout<<"we done!!!";
	close(fd);
	return 0;
}
