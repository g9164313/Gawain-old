#include <global.hpp>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/shm.h>

static char nameInp[500];
static char nameOut[500];

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeWrite(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jbuf
){
	jbyte* bufPtr = env->GetByteArrayElements(jbuf,NULL);
	jint jcnt = env->GetArrayLength(jbuf);
	int fd = open(nameOut,O_WRONLY);//O_NONBLOCK???
	if(fd>0){
		write(fd,(void*)bufPtr,jcnt);
		close(fd);
	}
	env->ReleaseByteArrayElements(jbuf,bufPtr,0);
	return jcnt;
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeFeed(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jbuf
){
	//this will be invoked by 'main' thread
	jbyte* bufPtr = env->GetByteArrayElements(jbuf,NULL);
	jint jcnt = env->GetArrayLength(jbuf);
	int fd = open(nameInp,O_WRONLY);
	if(fd>0){
		write(fd,(void*)bufPtr,jcnt);
		close(fd);
	}else{
		jcnt=0;
	}
	env->ReleaseByteArrayElements(jbuf,bufPtr,0);
	return jcnt;
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeRead(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jbuf
){
	jbyte* bufPtr = env->GetByteArrayElements(jbuf,NULL);
	jint jcnt = env->GetArrayLength(jbuf);
	int fd = open(nameInp,O_RDONLY);
	if(fd>0){
		bufPtr[0] = 0;
		jcnt = read(fd,(void*)bufPtr,jcnt);
		close(fd);
	}else{
		jcnt = 0;//reset it~~
	}
	env->ReleaseByteArrayElements(jbuf,bufPtr,0);
	return jcnt;
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipeCheck(
	JNIEnv* env,
	jobject thiz
){
	return 1;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_ModDaemon_pipeClosure(
	JNIEnv* env,
	jobject thiz
){
	unlink(nameInp);
	unlink(nameOut);
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_ModDaemon_pipePrepare(
	JNIEnv* env,
	jobject thiz,
	jstring jName,
	jint bufSize
){
	char name[500];
	jstrcpy(env,jName,name);
	sprintf(nameInp,"/tmp/%s1",name);
	sprintf(nameOut,"/tmp/%s0",name);

	Java_narl_itrc_ModDaemon_pipeClosure(env,thiz);

	if(mkfifo(nameInp,0666)<0){
		return -1;
	}
	if(mkfifo(nameOut,0666)<0){
		return -2;
	}
	return 0;
}
//-----------------------------------------------//

void memFactory(jlong* hand,bool cls){

	if(hand[0]!=0){
		if(hand[1]!=0){
			shmdt((void*)hand[1]);
		}
		hand[1] = 0;
		shmctl((int)hand[0],IPC_RMID,NULL);
	}
	hand[0] = 0;
	if(cls==true){
		return;
	}

	key_t key = hand[5];
	size_t len = hand[2]*hand[3]*hand[4];
	int id = shmget(key,len,IPC_CREAT|0666);
	if(id>0){
		hand[0] = id;
		void* ptr = shmat(id,NULL,0);
		if(ptr!=(void*)-1){
			hand[1] = (jlong)ptr;
		}else{
			hand[1] = 0;
			cerr<<"[memFactory]: fail to attach memory"<<endl;
		}
	}else{
		hand[0] = 0;
		cerr<<"[memFactory]: fail to create shared memory"<<endl;
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_PanDaemon_memPrepare(
	JNIEnv* env,
	jobject thiz,
	jlongArray jHand
){
	jlong* hand = env->GetLongArrayElements(jHand,NULL);
	memFactory(hand,false);
	env->ReleaseLongArrayElements(jHand,hand,0);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_PanDaemon_memClosure(
	JNIEnv* env,
	jobject thiz,
	jlongArray jHand
){
	jlong* hand = env->GetLongArrayElements(jHand,NULL);
	memFactory(hand,true);
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
	Mat src = imread(name,IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
	if(hand[2]!=src.cols || hand[3]!=src.rows){
		hand[2] = src.cols;
		hand[3] = src.rows;
		hand[4] = 1;
		memFactory(hand,false);
	}
	if(hand[1]!=0){
		Mat dst(src.size(),CV_8UC1,(void*)hand[1]);
		src.copyTo(dst);
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








