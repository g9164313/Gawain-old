#include <global.hpp>
#if defined _MSC_VER

#else
#include <stdio.h>   /* Standard input/output definitions */
#include <string.h>  /* String function definitions */
#include <unistd.h>  /* UNIX standard function definitions */
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <signal.h>
#endif//_MSC_VER

#define NAME_HANDLE  "handle"

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevTTY_implOpen(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jint  baud_rate,
	jchar data_bit,
	jchar parity,
	jchar stop_bit,
	jchar flow_mode
) {
	char name[500];
	jstrcpy(env,jname,name);
#if defined _MSC_VER
	HANDLE hand = CreateFileA(
		name,
		GENERIC_READ|GENERIC_WRITE,
		0,   /* no share  */
		NULL,/* no security */
		OPEN_EXISTING,
		0,   /* no threads */
		NULL /* no templates */
	);
	if(hand==INVALID_HANDLE_VALUE){
		setJLong(env,thiz,NAME_HANDLE,0);//reset handle!!!!
		return;
	}
	setJLong(env,thiz,NAME_HANDLE,(long)hand);

	DCB port_settings;
	memset(&port_settings, 0, sizeof(port_settings));  /* clear the new struct  */
	port_settings.DCBlength = sizeof(port_settings);
	port_settings.XonChar = 0x11;
	port_settings.XonLim = 200;
	port_settings.XoffChar = 0x13;
	port_settings.XoffLim = 200;
	port_settings.fRtsControl = 0x01;
	port_settings.EofChar = 0x1A;

	char modeText[500];
	char modeData = (char)((int)data_bit);
	char modePart = (char)((int)parity);
	char modeStop = (char)((int)stop_bit);
	sprintf_s(
		modeText,
		"baud=%d data=%c parity=%c stop=%c",
		baud_rate, modeData, modePart, modeStop
	);
	BuildCommDCBA(modeText, &port_settings);
	
	if(!SetCommState(hand, &port_settings)){
		cout << "Fail to set TTY"<< endl;
		return;
	} else {
		cout << "TTY SETTING: " << modeText << endl;
	}

	COMMTIMEOUTS cpt;
	cpt.ReadIntervalTimeout         = MAXDWORD;
	cpt.ReadTotalTimeoutMultiplier  = 0;
	cpt.ReadTotalTimeoutConstant    = 2000;
	cpt.WriteTotalTimeoutMultiplier = 0;
	cpt.WriteTotalTimeoutConstant   = 2000;
	if(!SetCommTimeouts(hand, &cpt)){
		cout<<"Fail to set Timeout"<<endl;
	}

	DWORD dwStoredFlags = EV_BREAK | EV_CTS | EV_DSR | EV_ERR | EV_RING | EV_RLSD | EV_RXCHAR | EV_TXEMPTY;
	if(!SetCommMask(hand, dwStoredFlags)){
		cout << "TTY: Fail to set mask" << endl;
	}
#else
	int fd = open(name, O_RDWR);
	if(fd<0){
		setJLong(env,thiz,NAME_HANDLE,0);
		return;
	}
	setJLong(env,thiz,NAME_HANDLE,fd);

	//cout<<"open file:"<<name<<" fid="<<fd<<endl;

	struct termios attr;
	tcgetattr(fd, &attr);
	cfmakeraw(&attr);

	speed_t spd = B9600;//default
	switch(baud_rate){
	case    300: spd=B300;   break;
	case    600: spd=B600;   break;
	case   1200: spd=B1200;  break;
	case   1800: spd=B1800;  break;
	case   2400: spd=B2400;  break;
	case   4800: spd=B4800;  break;
	case   9600: spd=B9600;  break;
	case  19200: spd=B19200; break;
	case  38400: spd=B38400; break;
	case  57600: spd=B57600; break;
	case 115200: spd=B115200;break;
	case 230400: spd=B230400;break;
	case 460800: spd=B460800;break;
	case 500000: spd=B500000;break;
	default:
		cout<<"[WARN] no support baud-rate:"<<baud_rate<<endl;
		break;
	}
	cfsetispeed(&attr,spd);
	cfsetospeed(&attr,spd);

	attr.c_cflag &= ~CSIZE;
	switch(data_bit){
	case '5': attr.c_cflag |= CS5; break;
	case '6': attr.c_cflag |= CS6; break;
	case '7': attr.c_cflag |= CS7; break;
	case '8': attr.c_cflag |= CS8; break;
	default:
		cout<<"[WARN] no support data-bit:"<<data_bit<<endl;
		break;
	}

	switch(parity){
	case 'n'://none
	case 'N':
		attr.c_cflag &= ~PARENB;
		attr.c_iflag |=  IGNPAR;
		break;
	case 'e'://even
	case 'E':
		attr.c_cflag |=  PARENB;
		attr.c_cflag &= ~PARODD;
		attr.c_iflag |=  INPCK;
		break;
	case 'o'://odd
	case 'O':
		attr.c_cflag |=  PARENB;
		attr.c_cflag |=  PARODD;
		attr.c_iflag |=  INPCK;
		break;
	case 'm'://mark
	case 'M':
		attr.c_cflag |=  PARENB|CMSPAR;
		attr.c_cflag |=  PARODD;
		break;
	case 's'://space
	case 'S':
		attr.c_cflag |=  PARENB|CMSPAR;
		attr.c_cflag &= ~PARODD;
		break;
	default:
		cout<<"[WARN] no support parity:"<<parity<<endl;
		break;
	}

	switch(stop_bit){
	case '1': attr.c_cflag &= ~CSTOPB; break;
	case '2': attr.c_cflag |=  CSTOPB; break;
	default:
		cout<<"[WARN] no support stop-bit:"<<stop_bit<<endl;
		break;
	}

	attr.c_cc[VMIN] = 0;//block until n bytes are received
	attr.c_cc[VTIME]= 2;//block until a timer expires (n * 100 mSec.)
	tcsetattr(fd, TCSANOW, &attr);
	//block or not~~~
	//fcntl(fd,F_SETFL,0);//Synchronized mode - This will block thread!!!
	//fcntl(fd,F_SETFL,FNDELAY);//Asynchronized mode
	//fcntl(fd,F_SETFL,O_NONBLOCK);
#endif//_MSC_VER
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_DevTTY_implRead(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jbuf,
	jint offset
) {
	int fd = getJLong(env,thiz,NAME_HANDLE);
	if(fd<=0){
		return 0;
	}
	jbyte* buf = env->GetByteArrayElements(jbuf,NULL);
	size_t len = env->GetArrayLength(jbuf);
#if defined _MSC_VER
	jlong cnt = 0;
	ReadFile(
		(HANDLE)hand,
		buf, len,
		(LPDWORD)((void *)&cnt),
		NULL
	);
#else
	jint cnt = (jint)read(fd, buf+offset, len);
#endif
	//cout << "read " << cnt <<" byte."<< endl;
	env->ReleaseByteArrayElements(jbuf,buf,0);
	return cnt;
}


extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevTTY_implWrite(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jbuf
) {
	int fd = getJLong(env,thiz,NAME_HANDLE);
	if(fd<=0){
		return;
	}
	jbyte* buf = env->GetByteArrayElements(jbuf,NULL);
	size_t len = env->GetArrayLength(jbuf);
#if defined _MSC_VER
	int count=0;
	WriteFile(
		(HANDLE)hand,
		buf, len,
		(LPDWORD)((void *)&count),
		NULL
	);
	if(count<0){
		cout<<"fail to write"<<endl;
	}
#else
	ssize_t count;
	jbyte* ptr = buf;
	do{
		count = write(fd,(void*)ptr,len);
		if(count<0){
			cout<<"tty-write-error:"<<errno<<endl;
			break;
		}
		len = len - count;
		ptr = ptr + count;
	}while(len>0);
#endif
	env->ReleaseByteArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevTTY_implClose(
	JNIEnv * env,
	jobject thiz
) {
	jlong fd = getJLong(env,thiz,NAME_HANDLE);
#if defined _MSC_VER
	HANDLE hand = (HANDLE)val;
	CloseHandle(hand);
#else
	//fcntl(fd, F_SETSIG, SIGQUIT);
	//tcsendbreak(fd,0);
	close(fd);
#endif//_MSC_VER
	setJLong(env,thiz,NAME_HANDLE,0);
}


