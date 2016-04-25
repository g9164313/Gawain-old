#ifndef UTILS_IPC
#define UTILS_IPC

#define MEM_SLOT 10
#define MSG_BUFF 100

#if defined _MSC_VER
//this direction for Visual Studio
#include <windows.h>
#include <stdlib.h>
#include <conio.h>
#include <iostream>
#else
//this direction for System V IPC
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <sys/shm.h>
#endif//_MSC_VER

//Basic Command
//MEMx,[width],[height],[type]
//DONE,[OK:NG]
//FAIL,[reason]

#define CmdIsIdfy (bufTxt[0]=='*' && bufTxt[1]=='I' && bufTxt[2]=='D' && bufTxt[3]=='N')
#define CmdIsMems (bufTxt[0]=='M' && bufTxt[1]=='E' && bufTxt[2]=='M' && ('0'<=bufTxt[3] || bufTxt[3]<='9'))
#define TextIsFail (bufTxt[0]=='F' && bufTxt[1]=='A' && bufTxt[2]=='I' && bufTxt[3]=='L')
#define TextIsDone (bufTxt[0]=='D' && bufTxt[1]=='O' && bufTxt[2]=='N' && bufTxt[3]=='E')

class IpcToken {
public:
	IpcToken(const char* name){
		strncpy(t_name,name,sizeof(t_name));
		pipeInit();
		memsInit();
	}
	~IpcToken(){
		pipeDone();
		memsDone();
	}

	void* setMem(int idx,size_t len){
		memsMake(idx,len);//just a wrap~~~~
		return getMem(idx);
	}
	void* getMem(int idx){
		if(idx>=MEM_SLOT){
			return NULL;
		}
		return memPtr[idx];
	}

	char* getMsg(){
		#if defined _MSC_VER
		return msgBuf;
		#else
		return msgBuf+sizeof(long);
		#endif//_MSC_VER
	}

	char* exec(const char* bufTxt){
		int memLen = 0;
		if(CmdIsMems==true){
			int param[4];
			if(parseInitParam(bufTxt,param)==true){
				memLen = param[0]*param[1]*param[2];
			}
		}
		pipeSend(bufTxt);
		pipeRecv(NULL);
		if(memLen>0){
			setMem(bufTxt[3]-'0',memLen);//mapping~~~
		}
		return getMsg();
	}

	bool parseInitParam(const char* cmd,int* param){
		//parse parameters,[width],[height],[scale],[type]
		strncpy(msgBuf,cmd,MSG_BUFF);
		char* tkn1 = (char*)strchr(msgBuf,',');//the first dot~~~
		if((tkn1)==NULL){
			return false;
		}
		*tkn1 = 0;
		tkn1++;
		char* tkn2 = strchr(tkn1,',');//the second dot~~~
		if((tkn2)==NULL){
			return false;
		}
		*tkn2 = 0;
		tkn2++;
		param[0] = atoi(tkn1);
		param[1] = atoi(tkn2);
		param[2] = 1;
		param[3] = 0;//hard code for CV_8UC1;
		char* tkn3 = strchr(tkn2,',');//the third dot~~~
		if((tkn3)!=NULL){
			*tkn3 = 0;
			tkn3++;
			if(strcmp(tkn3,"mono")==0){
				param[2] = 1;
				param[3] = 0;//hard code for CV_8UC1;
			}else if(strcmp(tkn3,"color")==0){
				param[2] = 3;
				param[3] = 16;//hard code for CV_8UC3;
			}
		}
		return true;
	}

#if defined _MSC_VER
	int pipeSend(const char* txt){
		if(hand==INVALID_HANDLE_VALUE){
			return 0;
		}
		if(txt==NULL){
			txt = msgBuf;
		}
		DWORD bufSize = strlen(txt);
		DWORD cbBack;
		BOOL fSuccess;
		fSuccess = WriteFile(
			hand,
			(LPVOID)txt,
			bufSize, 
			&cbBack, 
			NULL
		);
		DWORD dwErr = GetLastError();
		if(!fSuccess){
			//ERROR_NO_DATA 232L
			cout<<"[pipeSend]Fail!!! "<<dwErr<<endl;
		}
		return fSuccess;
	}
	int pipeRecv(char* txt){
		if(hand==INVALID_HANDLE_VALUE){
			return 0;
		}
		DWORD cbRead;
		BOOL fSuccess;	
		fSuccess = ReadFile(
			hand,
			(LPVOID)msgBuf,
			sizeof(msgBuf), 
			&cbRead, 
			NULL
		);
		DWORD dwErr = GetLastError();
		if(!fSuccess){						
			if(dwErr==ERROR_BROKEN_PIPE){
				DisconnectNamedPipe(hand);
				ConnectNamedPipe(hand,&over);//pipeInit();
			}else{
				cout<<"[pipeRecv]Fail!!! "<<dwErr<<endl;
			}
		}
		msgBuf[cbRead] = 0;
		if(txt!=NULL){
			strncpy(txt,msgBuf,cbRead);
		}
		return fSuccess;
	}
	int pipeWait(){
		if(hand==INVALID_HANDLE_VALUE){
			return -1;
		}		
		DWORD dwVal = WaitForSingleObject(over.hEvent,25);
		//DWORD dwErr = GetLastError();
		//cout<<"check="<<dwVal<<"@"<<dwErr<<endl;
		if(dwVal==WAIT_OBJECT_0){
			//cout<<"check="<<dwVal<<"@"<<dwErr<<endl;
			if(pipeRecv(NULL)==0){
				//cout<<"broken!!"<<endl;
				return -2;
			}
			return 0;
		}
		//WAIT_TIMEOUT 258L		
		return -3;
	}
#else
	int pipeSend(const char* txt){
		if(txt!=NULL){
			strncpy(getMsg(),txt,lenMsgBuf);
		}
		#if defined IPC_SERVER
		return msgsnd(msgTkn[1],msgBuf,lenMsgBuf,0);
		#else
		return msgsnd(msgTkn[0],msgBuf,lenMsgBuf,0);
		#endif
	}
	int pipeRecv(char* txt){
		int res = msgrcv(msgTkn[1],msgBuf,lenMsgBuf,0,0);
		if(txt!=NULL){
			strncpy(txt,getMsg(),lenMsgBuf);
		}
		return res;
	}
	int pipeWait(){
		return msgrcv(msgTkn[0],msgBuf,lenMsgBuf,0,IPC_NOWAIT);
	}
#endif
	
//private:
	char t_name[50];
	char msgBuf[MSG_BUFF+sizeof(long)];
	void* memPtr[MEM_SLOT];

//message pipe routine~~~
#if defined _MSC_VER
	OVERLAPPED over;
	HANDLE hand;
	void pipeInit(){
		DWORD bufSize = sizeof(msgBuf)*2;
		char path[500];
		sprintf(path,"\\\\.\\pipe\\%s",t_name);
		#if defined IPC_SERVER
		hand = CreateNamedPipe( 
			path,             
			PIPE_ACCESS_DUPLEX| FILE_FLAG_OVERLAPPED,       
			PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT,                
			PIPE_UNLIMITED_INSTANCES,
			bufSize,                  
			bufSize,                  
			1000,                        
			NULL);
		#else
		hand = CreateFile(
			path,// pipe name 
			GENERIC_READ |GENERIC_WRITE,// read and write access
			0,            // no sharing 
			NULL,         // default security attributes
			OPEN_EXISTING,// opens existing pipe 
			0,            // default attributes 
			NULL);        // no template file
		#endif//IPC_SERVER
		if(hand==INVALID_HANDLE_VALUE){
			cout<<"faile to create pipe:"<<path<<endl;
			return;
		}
		over.hEvent = CreateEvent(NULL,TRUE,TRUE,NULL);
		ConnectNamedPipe(hand,&over);//Does it succeed???
		if(GetLastError()==ERROR_PIPE_CONNECTED){
			SetEvent(over.hEvent);
		}
	}
	void pipeDone(){
		DisconnectNamedPipe(hand);
		CloseHandle(hand);
		hand=INVALID_HANDLE_VALUE;//reset it~~~
	}
#else
	int msgTkn[2];
	int lenMsgBuf;
	void pipeInit(){
		lenMsgBuf = sizeof(msgBuf) - sizeof(long);//initial size~~~

		int len = strlen(t_name);
		int res = 0;
		for(int i=0; i<len; i++){
			res = res + t_name[i];
		}
		res = res << 1;//shift a bit for identification~~~
		msgTkn[0] = msgTkn[1] = -1;//reset~~~

		for(int i=0; i<2; i++){//just two pipe-line
			key_t kid = ftok("/tmp",(res<<1)+i);
			if(kid!=-1){
				msgTkn[i] = msgget(kid,IPC_CREAT|0666);
			}
		}
	}
	void pipeDone(){
		#if defined IPC_SERVER
		msgctl(msgTkn[0],IPC_RMID,0);
		msgctl(msgTkn[1],IPC_RMID,0);
		#else
		#endif//IPC_SERVER
	}
#endif//_MSC_VER

//share memory routine~~~
#if defined _MSC_VER
	HANDLE memTkn[MEM_SLOT];
	void memsInit(){
		for(int i=0; i<MEM_SLOT; i++){
			memTkn[i]=INVALID_HANDLE_VALUE;
			memPtr[i]=NULL;
		}
	}
	void memsMake(int idx,size_t len){
		char path[500];
		sprintf(path,"Global\\%s",t_name);
		memTkn[idx]=CreateFileMapping(
			INVALID_HANDLE_VALUE,
			NULL,
			PAGE_READWRITE,0,
			(DWORD)len,
			path
		);
		if(memTkn[idx]==NULL){
			memTkn[idx] = INVALID_HANDLE_VALUE;
			memPtr[idx] = NULL;			
		}else{
			memPtr[idx]=(void*)MapViewOfFile(
				memTkn[idx],
				FILE_MAP_ALL_ACCESS,
				0,0,
				(DWORD)len
			);
		}
	}
	void memsDone(){
		for(int i=0; i<MEM_SLOT; i++){
			UnmapViewOfFile((LPCTSTR)memPtr[i]);
			CloseHandle(memTkn[i]);
			memTkn[i]=INVALID_HANDLE_VALUE;
			memPtr[i]=NULL;
		}
	}
#else
	int memTkn[MEM_SLOT];

	void memsInit(){
		for(int i=0; i<MEM_SLOT; i++){
			memTkn[i]=-1;
			memPtr[i]=NULL;
		}
	}
	void memsMake(int idx,size_t len){
		key_t kid = 0;
		int cnt = strlen(t_name);
		for(int i=0; i<cnt; i++){
			kid = kid + t_name[i];
		}
		#if defined IPC_SERVER
		memTkn[idx] = shmget(kid,len,IPC_CREAT|0666);
		#else
		memTkn[idx] = shmget(kid,len,0666);
		#endif
		if(memTkn[idx]==-1){
			memPtr[idx] = NULL;
		}else{
			memPtr[idx] = shmat(memTkn[idx],NULL,0);
		}
	}
	void memsDone(){
		for(int i=0; i<MEM_SLOT; i++){
			shmdt(memPtr[i]);
			#if defined IPC_SERVER
			shmctl(memTkn[i],IPC_RMID,NULL);
			#endif//IPC_SERVER
			memTkn[i]=-1;
			memPtr[i]=NULL;
		}
	}
#endif//_MSC_VER
};

#endif//UTILS_IPC
