#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <cmath>
#include <list>
#include <string>
#include <vector>
#include <algorithm>
#include <set>

#include <opencv/cv.h>

using namespace std;
using namespace cv;

Rect txt2rect(const char* txt){
	uint32_t v[4];
	int cnt = sscanf(txt,"%d,%d,%dx%d",v,v+1,v+2,v+3);
	if(cnt<4){
		return Rect(0,0,800,600);
	}
	return Rect(v[0],v[1],v[2],v[3]);
}


string type2str(int type) {
  string r;

  uchar depth = type & CV_MAT_DEPTH_MASK;
  uchar chans = 1 + (type >> CV_CN_SHIFT);

  switch ( depth ) {
    case CV_8U:  r = "CV_8U"; break;
    case CV_8S:  r = "CV_8S"; break;
    case CV_16U: r = "CV_16U"; break;
    case CV_16S: r = "CV_16S"; break;
    case CV_32S: r = "CV_32S"; break;
    case CV_32F: r = "CV_32F"; break;
    case CV_64F: r = "CV_64F"; break;
    default:     r = "CV_User"; break;
  }

  r += "C";
  r += (chans+'0');

  return r;
}

set<string> listName(string path, string appx){
	set<string> names;
	DIR* dir = opendir(path.c_str());
	for(;;){
		struct dirent * e = readdir(dir);
		if(!e){
			break;
		}
		if(e->d_type!=DT_REG){
			continue;
		}
		if(appx.empty()==false){
			if(strstr(e->d_name, appx.c_str())==NULL){
				continue;
			}
		}
		names.insert(path+"/"+e->d_name);
	}
	closedir(dir);
	return names;
}



char** listFileName(const char* path, const char* part, int* size){

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

	if(size!=NULL){
		*size = cnt;
	}

	rewinddir(dir);

	//second, prepare memory and copy string data....

	lst = new char*[cnt];

	int i=0;
	while(i<cnt){
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
		lst[i] = new char[200];
		/*sprintf(
			lst[i],
			"%s/%s",
			path, e->d_name
		);*/
		strcpy(lst[i],e->d_name);
		i++;
	}
	closedir(dir);
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

//--------------------------------------

extern const Scalar colorJet[] = {
	Scalar(0x00,0x00,0x84),
	Scalar(0x00,0x00,0x88),
	Scalar(0x00,0x00,0x8C),
	Scalar(0x00,0x00,0x90),
	Scalar(0x00,0x00,0x94),
	Scalar(0x00,0x00,0x98),
	Scalar(0x00,0x00,0x9C),
	Scalar(0x00,0x00,0xA0),
	Scalar(0x00,0x00,0xA4),
	Scalar(0x00,0x00,0xA8),
	Scalar(0x00,0x00,0xAC),
	Scalar(0x00,0x00,0xB0),
	Scalar(0x00,0x00,0xB4),
	Scalar(0x00,0x00,0xB8),
	Scalar(0x00,0x00,0xBC),
	Scalar(0x00,0x00,0xC0),
	Scalar(0x00,0x00,0xC4),
	Scalar(0x00,0x00,0xC8),
	Scalar(0x00,0x00,0xCC),
	Scalar(0x00,0x00,0xD0),
	Scalar(0x00,0x00,0xD4),
	Scalar(0x00,0x00,0xD8),
	Scalar(0x00,0x00,0xDC),
	Scalar(0x00,0x00,0xE0),
	Scalar(0x00,0x00,0xE4),
	Scalar(0x00,0x00,0xE8),
	Scalar(0x00,0x00,0xEC),
	Scalar(0x00,0x00,0xF0),
	Scalar(0x00,0x00,0xF4),
	Scalar(0x00,0x00,0xF8),
	Scalar(0x00,0x00,0xFC),
	Scalar(0x00,0x00,0xFF),
	Scalar(0x00,0x04,0xFF),
	Scalar(0x00,0x08,0xFF),
	Scalar(0x00,0x0C,0xFF),
	Scalar(0x00,0x10,0xFF),
	Scalar(0x00,0x14,0xFF),
	Scalar(0x00,0x18,0xFF),
	Scalar(0x00,0x1C,0xFF),
	Scalar(0x00,0x20,0xFF),
	Scalar(0x00,0x24,0xFF),
	Scalar(0x00,0x28,0xFF),
	Scalar(0x00,0x2C,0xFF),
	Scalar(0x00,0x30,0xFF),
	Scalar(0x00,0x34,0xFF),
	Scalar(0x00,0x38,0xFF),
	Scalar(0x00,0x3C,0xFF),
	Scalar(0x00,0x40,0xFF),
	Scalar(0x00,0x44,0xFF),
	Scalar(0x00,0x48,0xFF),
	Scalar(0x00,0x4C,0xFF),
	Scalar(0x00,0x50,0xFF),
	Scalar(0x00,0x54,0xFF),
	Scalar(0x00,0x58,0xFF),
	Scalar(0x00,0x5C,0xFF),
	Scalar(0x00,0x60,0xFF),
	Scalar(0x00,0x64,0xFF),
	Scalar(0x00,0x68,0xFF),
	Scalar(0x00,0x6C,0xFF),
	Scalar(0x00,0x70,0xFF),
	Scalar(0x00,0x74,0xFF),
	Scalar(0x00,0x78,0xFF),
	Scalar(0x00,0x7C,0xFF),
	Scalar(0x00,0x80,0xFF),
	Scalar(0x00,0x84,0xFF),
	Scalar(0x00,0x88,0xFF),
	Scalar(0x00,0x8C,0xFF),
	Scalar(0x00,0x90,0xFF),
	Scalar(0x00,0x94,0xFF),
	Scalar(0x00,0x98,0xFF),
	Scalar(0x00,0x9C,0xFF),
	Scalar(0x00,0xA0,0xFF),
	Scalar(0x00,0xA4,0xFF),
	Scalar(0x00,0xA8,0xFF),
	Scalar(0x00,0xAC,0xFF),
	Scalar(0x00,0xB0,0xFF),
	Scalar(0x00,0xB4,0xFF),
	Scalar(0x00,0xB8,0xFF),
	Scalar(0x00,0xBC,0xFF),
	Scalar(0x00,0xC0,0xFF),
	Scalar(0x00,0xC4,0xFF),
	Scalar(0x00,0xC8,0xFF),
	Scalar(0x00,0xCC,0xFF),
	Scalar(0x00,0xD0,0xFF),
	Scalar(0x00,0xD4,0xFF),
	Scalar(0x00,0xD8,0xFF),
	Scalar(0x00,0xDC,0xFF),
	Scalar(0x00,0xE0,0xFF),
	Scalar(0x00,0xE4,0xFF),
	Scalar(0x00,0xE8,0xFF),
	Scalar(0x00,0xEC,0xFF),
	Scalar(0x00,0xF0,0xFF),
	Scalar(0x00,0xF4,0xFF),
	Scalar(0x00,0xF8,0xFF),
	Scalar(0x00,0xFC,0xFF),
	Scalar(0x02,0xFF,0xFE),
	Scalar(0x06,0xFF,0xFA),
	Scalar(0x0A,0xFF,0xF6),
	Scalar(0x0E,0xFF,0xF2),
	Scalar(0x12,0xFF,0xEE),
	Scalar(0x16,0xFF,0xEA),
	Scalar(0x1A,0xFF,0xE6),
	Scalar(0x1E,0xFF,0xE2),
	Scalar(0x22,0xFF,0xDE),
	Scalar(0x26,0xFF,0xDA),
	Scalar(0x2A,0xFF,0xD6),
	Scalar(0x2E,0xFF,0xD2),
	Scalar(0x32,0xFF,0xCE),
	Scalar(0x36,0xFF,0xCA),
	Scalar(0x3A,0xFF,0xC6),
	Scalar(0x3E,0xFF,0xC2),
	Scalar(0x42,0xFF,0xBE),
	Scalar(0x46,0xFF,0xBA),
	Scalar(0x4A,0xFF,0xB6),
	Scalar(0x4E,0xFF,0xB2),
	Scalar(0x52,0xFF,0xAE),
	Scalar(0x56,0xFF,0xAA),
	Scalar(0x5A,0xFF,0xA6),
	Scalar(0x5E,0xFF,0xA2),
	Scalar(0x62,0xFF,0x9E),
	Scalar(0x66,0xFF,0x9A),
	Scalar(0x6A,0xFF,0x96),
	Scalar(0x6E,0xFF,0x92),
	Scalar(0x72,0xFF,0x8E),
	Scalar(0x76,0xFF,0x8A),
	Scalar(0x7A,0xFF,0x86),
	Scalar(0x7E,0xFF,0x82),
	Scalar(0x82,0xFF,0x7E),
	Scalar(0x86,0xFF,0x7A),
	Scalar(0x8A,0xFF,0x76),
	Scalar(0x8E,0xFF,0x72),
	Scalar(0x92,0xFF,0x6E),
	Scalar(0x96,0xFF,0x6A),
	Scalar(0x9A,0xFF,0x66),
	Scalar(0x9E,0xFF,0x62),
	Scalar(0xA2,0xFF,0x5E),
	Scalar(0xA6,0xFF,0x5A),
	Scalar(0xAA,0xFF,0x56),
	Scalar(0xAE,0xFF,0x52),
	Scalar(0xB2,0xFF,0x4E),
	Scalar(0xB6,0xFF,0x4A),
	Scalar(0xBA,0xFF,0x46),
	Scalar(0xBE,0xFF,0x42),
	Scalar(0xC2,0xFF,0x3E),
	Scalar(0xC6,0xFF,0x3A),
	Scalar(0xCA,0xFF,0x36),
	Scalar(0xCE,0xFF,0x32),
	Scalar(0xD2,0xFF,0x2E),
	Scalar(0xD6,0xFF,0x2A),
	Scalar(0xDA,0xFF,0x26),
	Scalar(0xDE,0xFF,0x22),
	Scalar(0xE2,0xFF,0x1E),
	Scalar(0xE6,0xFF,0x1A),
	Scalar(0xEA,0xFF,0x16),
	Scalar(0xEE,0xFF,0x12),
	Scalar(0xF2,0xFF,0x0E),
	Scalar(0xF6,0xFF,0x0A),
	Scalar(0xFA,0xFF,0x06),
	Scalar(0xFE,0xFF,0x01),
	Scalar(0xFF,0xFC,0x00),
	Scalar(0xFF,0xF8,0x00),
	Scalar(0xFF,0xF4,0x00),
	Scalar(0xFF,0xF0,0x00),
	Scalar(0xFF,0xEC,0x00),
	Scalar(0xFF,0xE8,0x00),
	Scalar(0xFF,0xE4,0x00),
	Scalar(0xFF,0xE0,0x00),
	Scalar(0xFF,0xDC,0x00),
	Scalar(0xFF,0xD8,0x00),
	Scalar(0xFF,0xD4,0x00),
	Scalar(0xFF,0xD0,0x00),
	Scalar(0xFF,0xCC,0x00),
	Scalar(0xFF,0xC8,0x00),
	Scalar(0xFF,0xC4,0x00),
	Scalar(0xFF,0xC0,0x00),
	Scalar(0xFF,0xBC,0x00),
	Scalar(0xFF,0xB8,0x00),
	Scalar(0xFF,0xB4,0x00),
	Scalar(0xFF,0xB0,0x00),
	Scalar(0xFF,0xAC,0x00),
	Scalar(0xFF,0xA8,0x00),
	Scalar(0xFF,0xA4,0x00),
	Scalar(0xFF,0xA0,0x00),
	Scalar(0xFF,0x9C,0x00),
	Scalar(0xFF,0x98,0x00),
	Scalar(0xFF,0x94,0x00),
	Scalar(0xFF,0x90,0x00),
	Scalar(0xFF,0x8C,0x00),
	Scalar(0xFF,0x88,0x00),
	Scalar(0xFF,0x84,0x00),
	Scalar(0xFF,0x80,0x00),
	Scalar(0xFF,0x7C,0x00),
	Scalar(0xFF,0x78,0x00),
	Scalar(0xFF,0x74,0x00),
	Scalar(0xFF,0x70,0x00),
	Scalar(0xFF,0x6C,0x00),
	Scalar(0xFF,0x68,0x00),
	Scalar(0xFF,0x64,0x00),
	Scalar(0xFF,0x60,0x00),
	Scalar(0xFF,0x5C,0x00),
	Scalar(0xFF,0x58,0x00),
	Scalar(0xFF,0x54,0x00),
	Scalar(0xFF,0x50,0x00),
	Scalar(0xFF,0x4C,0x00),
	Scalar(0xFF,0x48,0x00),
	Scalar(0xFF,0x44,0x00),
	Scalar(0xFF,0x40,0x00),
	Scalar(0xFF,0x3C,0x00),
	Scalar(0xFF,0x38,0x00),
	Scalar(0xFF,0x34,0x00),
	Scalar(0xFF,0x30,0x00),
	Scalar(0xFF,0x2C,0x00),
	Scalar(0xFF,0x28,0x00),
	Scalar(0xFF,0x24,0x00),
	Scalar(0xFF,0x20,0x00),
	Scalar(0xFF,0x1C,0x00),
	Scalar(0xFF,0x18,0x00),
	Scalar(0xFF,0x14,0x00),
	Scalar(0xFF,0x10,0x00),
	Scalar(0xFF,0x0C,0x00),
	Scalar(0xFF,0x08,0x00),
	Scalar(0xFF,0x04,0x00),
	Scalar(0xFF,0x00,0x00),
	Scalar(0xFC,0x00,0x00),
	Scalar(0xF8,0x00,0x00),
	Scalar(0xF4,0x00,0x00),
	Scalar(0xF0,0x00,0x00),
	Scalar(0xEC,0x00,0x00),
	Scalar(0xE8,0x00,0x00),
	Scalar(0xE4,0x00,0x00),
	Scalar(0xE0,0x00,0x00),
	Scalar(0xDC,0x00,0x00),
	Scalar(0xD8,0x00,0x00),
	Scalar(0xD4,0x00,0x00),
	Scalar(0xD0,0x00,0x00),
	Scalar(0xCC,0x00,0x00),
	Scalar(0xC8,0x00,0x00),
	Scalar(0xC4,0x00,0x00),
	Scalar(0xC0,0x00,0x00),
	Scalar(0xBC,0x00,0x00),
	Scalar(0xB8,0x00,0x00),
	Scalar(0xB4,0x00,0x00),
	Scalar(0xB0,0x00,0x00),
	Scalar(0xAC,0x00,0x00),
	Scalar(0xA8,0x00,0x00),
	Scalar(0xA4,0x00,0x00),
	Scalar(0xA0,0x00,0x00),
	Scalar(0x9C,0x00,0x00),
	Scalar(0x98,0x00,0x00),
	Scalar(0x94,0x00,0x00),
	Scalar(0x90,0x00,0x00),
	Scalar(0x9C,0x00,0x00),
	Scalar(0x88,0x00,0x00),
	Scalar(0x84,0x00,0x00),
	Scalar(0x80,0x00,0x00),
	Scalar(0x7C,0x00,0x00),
	Scalar(0x78,0x00,0x00),
	Scalar(0x74,0x00,0x00),
	Scalar(0x70,0x00,0x00),
};

static int jet_idx = 0;

Scalar takeColor(int idx){
	if(idx<0 || idx>258){
		jet_idx = 0;
	}else{
		jet_idx+=1;
	}
	return colorJet[jet_idx];
}
