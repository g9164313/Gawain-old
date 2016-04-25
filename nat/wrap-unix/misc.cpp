#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <global.hpp>
#include <grabber.hpp>
#include <utils_ipc.hpp>
#include <algorithm>

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

