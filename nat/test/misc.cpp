#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <global.hpp>
#include <algorithm>
#include "../util_ipc.hpp"

/*void list_dir(string path,vector<string>& lst,string prex);

extern Mat cutOutBounding(Mat& img,Mat& msk,int width,int height);
extern void removeNoise(Mat& msk,int* board);

int main5(int argc, char* argv[]) {

	string pathBack="./cam0/back";
	vector<string> lstBack;
	list_dir(pathBack,lstBack,"");

	string pathFore="./cam0/fore";
	vector<string> lstFore;
	list_dir(pathFore,lstFore,"Chip1");

	string pathMeas="./cam0/meas";
	vector<string> lstMeas;
	list_dir(pathMeas,lstMeas,"Sp01L120");

	IpcToken tkn("cam1");

	tkn.exec("IDFY");
	cout<<"CMD=idfy @ RESP="<<tkn.getMsg()<<endl;

	tkn.exec("MEM0,6576,4384,mono");
	cout<<"RESP="<<tkn.getMsg()<<endl;
	void* buff = tkn.getMem(0);
	if(buff==NULL){
		cout<<"something is wrong!!!"<<endl;
		return -1;
	}

	/*tkn.exec("CLR,back");
	cout<<"RESP="<<tkn.getMsg()<<endl;
	tkn.exec("CLR,fore");
	cout<<"RESP="<<tkn.getMsg()<<endl;

	for(size_t i=0; i<lstBack.size(); i++){
		string name = pathBack+"/"+lstBack[i];
		Mat img = imread(name,IMREAD_GRAYSCALE);
		Mat nod(img.rows,img.cols,CV_8UC1,buff);
		img.copyTo(nod);
		tkn.exec("SAVE,BACK");
		cout<<"CMD=save,back @ RESP="<<tkn.getMsg()<<endl;
	}
	for(size_t i=0; i<lstFore.size(); i++){
		string name = pathFore+"/"+lstFore[i];
		Mat img = imread(name,IMREAD_GRAYSCALE);
		Mat nod(img.rows,img.cols,CV_8UC1,buff);
		img.copyTo(nod);
		tkn.exec("SAVE,FORE");
		cout<<"CMD=save,fore @ RESP="<<tkn.getMsg()<<endl;
	}
	tkn.exec("TRAN");
	cout<<"CMD=tran @ RESP="<<tkn.getMsg()<<endl;*/

	/*for(size_t i=0; i<lstMeas.size(); i++){
		string name = pathMeas+"/"+lstMeas[i];
		Mat img = imread(name,IMREAD_GRAYSCALE);
		Mat nod(img.rows,img.cols,CV_8UC1,buff);
		img.copyTo(nod);
		tkn.exec("MEAS");
		cout<<"CMD=MEAS @ RESP="<<tkn.getMsg()<<endl;
	}

	//tkn.pipeSend("gfgfggf");
	//tkn.pipeRecv(NULL);
	//cout<<"RESP>>"<<tkn.getMsg()<<endl;
	return 0;
}*/

char** listFileName(const char* path, const char* part){

	static char** lst = NULL;

	free(lst);

	DIR* dir = opendir(path);
	if(!dir){
		cout<<"fail to open directory ("<<path<<")..."<<endl;
		exit(EXIT_FAILURE);
	}

	//first, count all files~~~
	int cnt = 0;
	while(1){
		struct dirent * e = readdir(dir);
		if(!e){
			break;
		}
		if(e->d_type!=DT_REG){
			continue;
		}
		if(part!=NULL){
			if(strstr(e->d_name,part)==NULL){
				continue;
			}
		}
		cnt++;
	}

	rewinddir(dir);

	//second, prepare memory and copy string data....

	lst = (char**) malloc(sizeof(char*) * (cnt+1));

	lst[cnt] = NULL;

	for(int i=0; i<cnt; i++){
		struct dirent * e = readdir(dir);
		if(!e){
			break;
		}
		if(e->d_type!=DT_REG){
			continue;
		}
		if(part!=NULL){
			if(strstr(e->d_name,part)==NULL){
				continue;
			}
		}
		lst[i] = e->d_name;
	}

	return lst;
}

void list_dir(string path,vector<string>& lst,string prex){
	lst.clear();
	DIR* dir;
	dir = opendir(path.c_str());
	if (!dir) {
		cout<<"fail to open...."<<endl;
		exit(EXIT_FAILURE);
	}
	while(1){
		struct dirent * entry;
		entry = readdir(dir);
		if(!entry){
			break;
		}
		if(entry->d_type!=DT_REG){
			continue;
		}
		//check prefix name
		int len1=prex.length();
		if(len1!=0){
			string name(entry->d_name);
			size_t pos = name.find(prex);
			//if(pos==string::npos){
			if(pos!=0){
				continue;
			}
		}
		//check appendix name
		if(
			/*strstr(entry->d_name,".tiff")==NULL &&*/
			strstr(entry->d_name,".png")==NULL &&
			strstr(entry->d_name,".jpg")==NULL &&
			strstr(entry->d_name,".bmp")==NULL
		){
			continue;
		}
		string dst(entry->d_name);
/*#if defined _MSC_VER
		dst=path+"\\"+dst;
#else
		dst=path+"/"+dst;
#endif*/
		lst.push_back(dst);
	}
	std::sort(lst.begin(),lst.end());
}

string strip_appendix(string name){
	size_t pos = name.find_last_of('.');
	name = name.substr(0,pos);
	name = name + ".org.png";
	return name;
}

