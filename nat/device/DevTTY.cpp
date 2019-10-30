#include <global.hpp>
#ifndef _MSC_VER
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <signal.h>
#endif//_MSC_VER

#define NAME_HANDLE  "handle"

extern "C" JNIEXPORT void Java_narl_itrc_DevTTY_implOpen(
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
	char desc[500];
	sprintf_s(desc,"\\\\.\\%s",name);

	HANDLE hand = CreateFileA(
		desc,
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
	
	DCB dcb;
	GetCommState(hand, &dcb);
	dcb.BaudRate = baud_rate;
	dcb.ByteSize = ((int)data_bit)-0x30;
	switch(parity){
	case 'n'://none
	case 'N':
		dcb.Parity = NOPARITY;
		break;
	case 'e'://even
	case 'E':
		dcb.Parity = EVENPARITY;
		break;
	case 'o'://odd
	case 'O':
		dcb.Parity = ODDPARITY;
		break;
	case 'm'://mark
	case 'M':
		dcb.Parity = MARKPARITY;
		break;
	case 's'://space
	case 'S':
		dcb.Parity = SPACEPARITY;
		break;
	default:
		cout<<"no support parity:"<<parity<<endl;
		break;
	}
	switch(stop_bit){
	case '1': dcb.StopBits = ONESTOPBIT  ; break;
	case 'q': dcb.StopBits = ONE5STOPBITS; break;
	case '2': dcb.StopBits = TWOSTOPBITS ; break;
	default:
		cout<<"no support stop-bit:"<<stop_bit<<endl;
		break;
	}
	/* No software handshaking */
	//dcb.fTXContinueOnXoff = TRUE;
	//dcb.fOutX = FALSE;
	//dcb.fInX = FALSE;
	/* Binary mode (it's the only supported on Windows anyway) */
	dcb.fBinary = TRUE;
	/* Want errors to be blocking */
	dcb.fAbortOnError = TRUE;
	if(!SetCommState(hand, &dcb)){
		cout<<"Fail to set DBCA"<<endl;
	}
	/*COMMTIMEOUTS cpt;
	cpt.ReadIntervalTimeout         = MAXDWORD;
	cpt.ReadTotalTimeoutMultiplier  = 0;
	cpt.ReadTotalTimeoutConstant    = 3000;
	cpt.WriteTotalTimeoutMultiplier = 0;
	cpt.WriteTotalTimeoutConstant   = 3000;
	if(!SetCommTimeouts(hand, &cpt)){
		cout<<"Fail to set Timeout"<<endl;
	}*/
#else
	int fd = open(name, O_RDWR);
	if(fd<0){
		setJLong(env,thiz,NAME_HANDLE,0);
		return;
	}
	setJLong(env,thiz,NAME_HANDLE,fd);

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
	//none blocking
	//attr.c_cc[VMIN] = 0;//block until n bytes are received
	//attr.c_cc[VTIME]= 1;//block until a timer expires (n * 100 mSec.)
	//tcsetattr(fd, TCSANOW, &attr);
	//block thread
	//fcntl(fd,F_SETFL,0);//Synchronized mode - This will block thread!!!
	//fcntl(fd,F_SETFL,FNDELAY);//Asynchronized mode
#endif//_MSC_VER
}

extern "C" JNIEXPORT jint Java_narl_itrc_DevTTY_implRead(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jbuf,
	jint offset,
	jint length
) {
	int fd = getJLong(env,thiz,NAME_HANDLE);
	if(fd<=0){
		return -1;
	}
	size_t len = env->GetArrayLength(jbuf);
	if (length <= 0) {
		length = len;
	}
	if((offset+length)>len){
		return -2;
	}
	jbyte* buf = env->GetByteArrayElements(jbuf,NULL);
	size_t count;
	jbyte* ptr = buf + offset;	
#if defined _MSC_VER
	DWORD res;
	//SetCommMask((HANDLE)fd, EV_RXCHAR);
	//WaitCommEvent((HANDLE)fd, (LPDWORD)(&res), NULL);
	ReadFile(
		(HANDLE)fd,
		ptr, length,
		(LPDWORD)(&res),
		NULL
	);
	count = res;
#else
	count = read(fd,ptr,length);
#endif
	env->ReleaseByteArrayElements(jbuf,buf,0);
	//cout<<"read-len="<<len<<","<<length<<endl;
	return count;
}


extern "C" JNIEXPORT jint Java_narl_itrc_DevTTY_implWrite(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jbuf,
	jint offset,
	jint length
) {
	int fd = getJLong(env,thiz,NAME_HANDLE);
	if(fd<=0){
		return -1;
	}	
	size_t len = env->GetArrayLength(jbuf);
	if (length <= 0) {
		length = len;
	}
	if((offset+length)>len){
		return -2;
	}
	jbyte* buf = env->GetByteArrayElements(jbuf,NULL);
	size_t count;
	jbyte* ptr = buf + offset;
#if defined _MSC_VER
	DWORD res;
	WriteFile(
		(HANDLE)fd,
		ptr, length,
		(LPDWORD)(&res),
		NULL
	);
	count = res;
#else	
	count = write(fd,(void*)ptr,length);
#endif
	env->ReleaseByteArrayElements(jbuf,buf,0);
	return count;
}

extern "C" JNIEXPORT void Java_narl_itrc_DevTTY_implClose(
	JNIEnv * env,
	jobject thiz
) {
	jlong fd = getJLong(env,thiz,NAME_HANDLE);
#if defined _MSC_VER
	CloseHandle((HANDLE)fd);
#else
	//fcntl(fd, F_SETSIG, SIGQUIT);
	//tcsendbreak(fd,0);
	close(fd);
#endif//_MSC_VER
	setJLong(env,thiz,NAME_HANDLE,0);
}


