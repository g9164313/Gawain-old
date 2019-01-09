#if defined _MSC_VER
//support for windows
#include <Windows.h>
#include <string>
#include <vector>
#define TEXT_SIZE 512

static CHAR szText[TEXT_SIZE];
static HANDLE hPipe=INVALID_HANDLE_VALUE;

const CHAR* ipcCmd(const CHAR* szCommand){	
	if(szCommand==NULL){
		//terminate service
		CloseHandle(hPipe);
		hPipe=INVALID_HANDLE_VALUE;//reset it~~~
		return NULL;
	}

	szText[0] = 0;//clear~~~
	if(hPipe==INVALID_HANDLE_VALUE){
		hPipe = CreateFile(
			TEXT("\\\\.\\pipe\\daemon"),// pipe name 
			GENERIC_READ |GENERIC_WRITE,// read and write access
			0,            // no sharing 
			NULL,         // default security attributes
			OPEN_EXISTING,// opens existing pipe 
			0,            // default attributes 
			NULL);        // no template file
		if(hPipe==INVALID_HANDLE_VALUE){
			return szText;
		}
	}

	BOOL fSuccess;
	DWORD cbBack,cbSize;

	cbSize = strlen(szCommand);
	fSuccess = WriteFile( 
		hPipe,// pipe handle 
		szCommand,//message 
		cbSize,// message length 
		&cbBack,// bytes written 
		NULL);  // not overlapped
	if(!fSuccess){
		//show error messages!!!
		GetLastError();
		return szText;
	}

	cbSize = sizeof(szText);
	fSuccess = ReadFile( 
		hPipe,  // pipe handle 
		szText, // buffer to receive reply 
		cbSize, // size of buffer 
		&cbBack,// number of bytes read 
		NULL);  // not overlapped 
	if(!fSuccess){
		//show error messages!!!
		GetLastError();
	}
	return szText;
}

typedef struct MEM_NODE {
	HANDLE  hndFile;
	LPCTSTR ptrBuff;
} MemNode;
static std::vector<MemNode> MemList;

void* ipcMem(size_t idx,size_t size){
	idx = idx - 1;
	if(MemList.size()==idx){
		#define PATH_SIZE 100
		CHAR path[100];
		sprintf_s(path,PATH_SIZE,"Global\\M%03d",idx);//just query~~~
		MemNode node;
		node.hndFile=CreateFileMapping(
			INVALID_HANDLE_VALUE,
			NULL,
			PAGE_READWRITE,0,
			size,
			path
		);
		node.ptrBuff=(LPTSTR)MapViewOfFile(
			node.hndFile,
			FILE_MAP_ALL_ACCESS,
			0,0,
			size
		);
		MemList.push_back(node);
		return (void*)(node.ptrBuff);
	}else if(idx<MemList.size()){
		MemNode& node = MemList[idx];
		UnmapViewOfFile(node.ptrBuff);
		CloseHandle(node.hndFile);
	}
	return NULL;
}

#else
//support for Unix-like 
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/shm.h>
#include <string>

using namespace std;

string ipcCmd(string txt){
	int fd = open("/tmp/daemon1",O_WRONLY);
	if(fd>0){
		write(fd,txt.c_str(),txt.length());
		close(fd);
		fd = open("/tmp/daemon0",O_RDONLY);
		txt.clear();
		if(fd>0){
			char buf[500];
			int cnt = read(fd,buf,1000);
			close(fd);
			txt.assign(buf,cnt);
		}
	}
	return txt;
}

void* ipcMem(int idx,int len){
	/*char buf[100];
	sprintf(buf,"%d@mem?%d\n",idx,len);
	string res = ipcCmd(string(buf));
	if(res=="fail\n"){
		return NULL;
	}
	sprintf(buf,"MEM%d",idx-1);
	key_t key=0;
	for(int i=0; i<strlen(buf); i++){
		key = key + buf[i];
	}
	//cout<<"name="<<buf<<endl;
	//cout<<"key ="<<key<<endl;

	int id = shmget(key,1000,IPC_CREAT|0666);
	if(id<0){
		cerr<<"fail to create shared memory("<<buf<<")"<<endl;
		return NULL;
	}
	void* ptr = shmat(id,NULL,0);
	if(ptr==(void*)-1){
		cerr<<"fail to attach memory("<<buf<<")"<<endl;
		return NULL;
	}
	//shmctl(id,IPC_RMID,NULL);
	return ptr;*/
}

#endif
