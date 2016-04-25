#include <global.hpp>
#include <windows.h>

static OVERLAPPED over;
static HANDLE hand = INVALID_HANDLE_VALUE;

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeWrite(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jbuf
){
	jbyte* bufPtr = env->GetByteArrayElements(jbuf,NULL);
	DWORD bufSize = env->GetArrayLength(jbuf);
	DWORD cbBack;
	BOOL fSuccess;
	fSuccess = WriteFile(
		hand,
		(LPVOID)bufPtr,
		bufSize, 
		&cbBack, 
		NULL
	);
	if(!fSuccess){
		cout<<"WRITE GLE="<<GetLastError()<<endl;
	}
	//cout<<"write:"<<bufPtr<<"("<<cbBack<<"<--"<<bufSize<<")"<<endl;
	env->ReleaseByteArrayElements(jbuf,bufPtr,0);
	return cbBack;
}

#define FEED_SIZE 512
static bool feedState = false;
static jsize feedSize = 0L;
static unsigned char feedBuff[FEED_SIZE];
extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeFeed(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jbuf
){
	//this will be invoked by 'main' thread
	feedState = true;
	jbyte* ptr = env->GetByteArrayElements(jbuf,NULL);
	feedSize = env->GetArrayLength(jbuf);
	if(feedSize>=FEED_SIZE){
		feedSize = FEED_SIZE - 1;
	}
	CopyMemory(feedBuff,(void*)ptr,feedSize);
	feedBuff[feedSize+1]=0;
	env->ReleaseByteArrayElements(jbuf,ptr,0);
	return feedSize;
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeRead(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jbuf
){
	jbyte* bufPtr = env->GetByteArrayElements(jbuf,NULL);
	DWORD bufSize = env->GetArrayLength(jbuf);
	if(feedState==true){
		CopyMemory(bufPtr,(void*)feedBuff,feedSize);
		feedState = false;
		env->ReleaseByteArrayElements(jbuf,bufPtr,0);		
		return feedSize;
	}
	DWORD cbRead;
	BOOL fSuccess;	
	fSuccess = ReadFile(
		hand,
		(LPVOID)bufPtr,
		bufSize, 
		&cbRead, 
		NULL
	);
	bufPtr[cbRead] = 0;
	env->ReleaseByteArrayElements(jbuf,bufPtr,0);
	if(fSuccess && cbRead!=0){
		return (jint)cbRead;
	}
	return 0;
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeCheck(
	JNIEnv* env,
	jobject thiz
){	
	if(feedState==true){
		return 1;
	}

	DWORD cbVal = WaitForSingleObject(over.hEvent,25);
	switch(cbVal){
	case STATUS_TIMEOUT:
		return 0;
	case WAIT_OBJECT_0:
		return 1;
	}
	//cout<<"check="<<cbVal<<endl;
	if(GetLastError()==ERROR_BROKEN_PIPE){
		return -1;
	}
	return 0;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_ModDaemon_pipeClosure(
	JNIEnv* env,
	jobject thiz
){
	FlushFileBuffers(hand);
	DisconnectNamedPipe(hand);
	CloseHandle(hand);
	hand = INVALID_HANDLE_VALUE;
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipePrepare(
	JNIEnv* env,
	jobject thiz,
	jstring jName,
	jint bufSize
){	
	over.hEvent = CreateEvent(NULL,TRUE,TRUE,NULL);
	char name[500];
	char path[500];
	jstrcpy(env,jName,name);
	sprintf(path,"\\\\.\\pipe\\%s",name);
	cout<<"pipe_name="<<path<<endl;
	hand = CreateNamedPipe( 
		path,             
		PIPE_ACCESS_DUPLEX| FILE_FLAG_OVERLAPPED,       
		PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT,                
		PIPE_UNLIMITED_INSTANCES,
		bufSize,                  
		bufSize,                  
		1000,                        
		NULL
	); 
	if(hand==INVALID_HANDLE_VALUE){
		return -1;
	}
	BOOL fFail = ConnectNamedPipe(hand,&over);
	if(fFail){
		cout<<"fail to connect"<<endl;
		return -2;
	}
	DWORD dwErr = GetLastError();
	if(dwErr==ERROR_PIPE_CONNECTED){
		SetEvent(over.hEvent);
	}
	return 0;
}
//-----------------------------------------------//

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_PanDaemon_memPrepare(
	JNIEnv* env,
	jobject thiz,
	jlongArray jHand
){	
	jlong* hand = env->GetLongArrayElements(jHand,NULL);

	char path[500];
	//sprintf(path,"Global\\m000")
	sprintf(path,"Global\\%c%c%c%c",
		(char)((hand[5]&0xFF000000)>>24),
		(char)((hand[5]&0x00FF0000)>>16),
		(char)((hand[5]&0x0000FF00)>>8),
		(char)((hand[5]&0x000000FF))
	);
	cout<<"mem_name="<<path<<endl;

	DWORD len = (DWORD)(hand[2]*hand[3]*hand[4]);
	HANDLE  hndFile;
	LPCTSTR ptrBuff;
	hndFile=CreateFileMapping(
		INVALID_HANDLE_VALUE,
		NULL,
		PAGE_READWRITE,0,
		len,
		path
	);
	ptrBuff=(LPTSTR)MapViewOfFile(
		hndFile,
		FILE_MAP_ALL_ACCESS,
		0,0,
		len
	);
	hand[0] = (jlong)hndFile;
	hand[1] = (jlong)ptrBuff;

	env->ReleaseLongArrayElements(jHand,hand,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_PanDaemon_memClosure(
	JNIEnv* env,
	jobject thiz,
	jlongArray jHand
){
	jlong* hand = env->GetLongArrayElements(jHand,NULL);

	HANDLE hndFile;
	LPCTSTR ptrBuff;
	hndFile = (HANDLE )hand[0];
	ptrBuff = (LPCTSTR)hand[1];
	UnmapViewOfFile(ptrBuff);
	CloseHandle(hndFile);

	env->ReleaseLongArrayElements(jHand,hand,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_PanDaemon_memLoad(
	JNIEnv* env,
	jobject thiz,
	jlongArray jHand,
	jstring jName
){
	char name[500];
	jstrcpy(env,jName,name);

	jlong* hand = env->GetLongArrayElements(jHand,NULL);
	if(hand[0]!=0 && hand[1]!=0){
		Mat src = imread(name,IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
		void* ptr = (void*)src.ptr();
		DWORD len = hand[2]*hand[3]*hand[4];
		CopyMemory((void*)hand[1],(void*)ptr,len);
	}else{
		cerr<<"[memLoad]: no pointer!!"<<endl;
	}
	env->ReleaseLongArrayElements(jHand,hand,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_PanDaemon_memDump(
	JNIEnv* env,
	jobject thiz,
	jlongArray jHand,
	jstring jName
){
	char name[500];
	jstrcpy(env,jName,name);

	jlong* hand = env->GetLongArrayElements(jHand,NULL);
	if(hand[0]!=0 && hand[1]!=0){
		int scale=hand[4];
		switch(scale){
		case 3:
			scale = CV_8UC3;
			break;
		default:
		case 1:
			scale = CV_8UC1;
			break;
		}
		Mat img(hand[3],hand[2],scale,(void*)hand[1]);
		imwrite(name,img);
	}
	env->ReleaseLongArrayElements(jHand,hand,0);
}



