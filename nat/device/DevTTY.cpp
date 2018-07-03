#include <global.hpp>
#if defined _MSC_VER

#else
#include <stdio.h>   /* Standard input/output definitions */
#include <string.h>  /* String function definitions */
#include <unistd.h>  /* UNIX standard function definitions */
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
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

	char modeText[500];
	char modeData = (char)((int)data_bit);
	char modePart = (char)((int)parity);
	char modeStop = (char)((int)stop_bit);
	sprintf_s(
		modeText,
		"baud=%d data=%c parity=%c stop=%c",
		baud_rate, modeData, modePart, modeStop
	);

	cout<<"SETTING: "<< modeText <<endl;

	BuildCommDCBA(modeText, &port_settings);

	if(!SetCommState(hand, &port_settings)){
		cout<<"Fail to set ["<<name<<"] state"<<endl;
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

#else
	int fd = open(name, O_RDWR|O_NOCTTY);
	if(fd<0){
		setJLong(env,thiz,NAME_HANDLE,0);
		return;
	}
	setJLong(env,thiz,NAME_HANDLE,fd);

	//cout<<"open file:"<<name<<" fid="<<fd<<endl;

	struct termios options;
	tcgetattr(fd,&options);
	cfmakeraw(&options);

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
	cfsetispeed(&options,spd);
	cfsetospeed(&options,spd);

	options.c_cflag &= ~CSIZE;
	switch(data_bit){
	case '5': options.c_cflag |= CS5; break;
	case '6': options.c_cflag |= CS6; break;
	case '7': options.c_cflag |= CS7; break;
	case '8': options.c_cflag |= CS8; break;
	default:
		cout<<"[WARN] no support data-bit:"<<data_bit<<endl;
		break;
	}

	switch(parity){
	case 'n'://none
	case 'N':
		options.c_cflag &= ~PARENB;
		options.c_iflag |=  IGNPAR;
		break;
	case 'e'://even
	case 'E':
		options.c_cflag |=  PARENB;
		options.c_cflag &= ~PARODD;
		options.c_iflag |=  INPCK;
		break;
	case 'o'://odd
	case 'O':
		options.c_cflag |=  PARENB;
		options.c_cflag |=  PARODD;
		options.c_iflag |=  INPCK;
		break;
	case 'm'://mark
	case 'M':
		options.c_cflag |=  PARENB|CMSPAR;
		options.c_cflag |=  PARODD;
		break;
	case 's'://space
	case 'S':
		options.c_cflag |=  PARENB|CMSPAR;
		options.c_cflag &= ~PARODD;
		break;
	default:
		cout<<"[WARN] no support parity:"<<parity<<endl;
		break;
	}

	switch(stop_bit){
	case '1': options.c_cflag &= ~CSTOPB; break;
	case '2': options.c_cflag |=  CSTOPB; break;
	default:
		cout<<"[WARN] no support stop-bit:"<<stop_bit<<endl;
		break;
	}

	options.c_cflag |= (CLOCAL | CREAD);
	options.c_cc[VMIN] =  0;//block until n bytes are received
	options.c_cc[VTIME]= 30;//block until a timer expires (n * 100 mSec.)

	//TODO:how to set flow-control
	//block or not~~~
	//fcntl(fd,F_SETFL,0);//Synchronized mode - This will block thread!!!
	//fcntl(fd,F_SETFL,FNDELAY);//Asynchronized mode

	tcsetattr(fd,TCSANOW,&options);
#endif//_MSC_VER
}

extern "C" JNIEXPORT jlong JNICALL Java_narl_itrc_DevTTY_implRead(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jbuf
) {
	jbyte* buf = env->GetByteArrayElements(jbuf,NULL);
	size_t len = env->GetArrayLength(jbuf);

#if defined _MSC_VER
	jlong hand = getJLong(env,thiz,NAME_HANDLE);
	if(hand==0L){
		return 0;
	}
	jlong cnt = 0;
	ReadFile(
		(HANDLE)hand,
		buf, len,
		(LPDWORD)((void *)&cnt),
		NULL
	);
	env->ReleaseByteArrayElements(jbuf,buf,0);
	return (jlong)cnt;
#else
	//Do we need block mode?,This is tri_state variable
	//fcntl(fd,F_SETFL,0);
	//fcntl(fd,F_SETFL,FNDELAY);
	int fd = getJLong(env,thiz,NAME_HANDLE);
	if(fd<=0){
		return 0;
	}
	jlong cnt = (jlong)read(fd, buf, len);
	env->ReleaseByteArrayElements(jbuf,buf,0);
	return cnt;
#endif
}


extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevTTY_implWrite(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jbuf
) {
	jbyte* buf = env->GetByteArrayElements(jbuf,NULL);
	size_t len = env->GetArrayLength(jbuf);
#if defined _MSC_VER
	jlong hand = getJLong(env,thiz,NAME_HANDLE);
	if(hand!=0L){
		int n;
		WriteFile(
			(HANDLE)hand,
			buf, len,
			(LPDWORD)((void *)&n),
			NULL
		);
		if(n<0){ cout<<"fail to write"<<endl; }
	}
#else
	int fd = getJLong(env,thiz,NAME_HANDLE);
	if(fd!=0){
		ssize_t res;
		jbyte* ptr = buf;
		do{
			res = write(fd,buf,len);
			if(res<0){
				cout<<"tty-write-error:"<<errno<<endl;
				break;
			}else if(res==0){
				cout<<"tty-no-writing:"<<endl;//??What is going on??
				break;
			}
			len = len - res;
		}while(len>0);
	}
#endif
	env->ReleaseByteArrayElements(jbuf,buf,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevTTY_implClose(
	JNIEnv * env,
	jobject thiz
) {
	long val = getJLong(env,thiz,NAME_HANDLE);
#if defined _MSC_VER
	HANDLE hand = (HANDLE)val;
	CloseHandle(hand);
#else
	close((int)val);
#endif//_MSC_VER
	setJLong(env,thiz,NAME_HANDLE,0);//reset handle!!!!
}


