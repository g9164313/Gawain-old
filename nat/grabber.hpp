#include <global.hpp>

#ifndef GRABBER_HPP
#define GRABBER_HPP

class GBundle{
public:
	GBundle(JNIEnv* _env,jobject _thiz){
		strcpy(wndName,"::grabber::");//default name~~~
		env = _env;
		thiz=_thiz;
		if(env==NULL){
			return;
		}
		init();
	}

	Mat overlay;

	char cfgName[500];
	char infText[500];

	void setLastMsg(const char* txt){
		if(env==NULL){
			cout<<txt<<endl;
			return;
		}
		setTxt("lastMsg",txt);
	}
	void setPipeMsg(const char* txt){
		setTxt("pipeMsg",txt);
	}
	void getPipeMsg(const char* txt){
		getTxt("pipeMsg",txt);
	}

	void callback(Mat& dat);
	void callback(void* ptr,int width,int height,int type);

	void updateTape(Mat& dat);
	void updateFPS(double fps);

	bool checkExit();

	void getTxt(const char* name,const char* txt);
	void setTxt(const char* name,const char* txt);

	bool isMock(){
		return (env==NULL)?(true):(false);
	}

	const char* getName(){
		return wndName;
	}
	double getFPSVal(){
		return curFreq;
	}

private:
	JNIEnv* env;
	jclass  clzz;
	jobject thiz;

	jmethodID idCallback;

	jfieldID optTapeIndx;
	jfieldID optTapeSize;

	jfieldID optFPS;
	jfieldID optExit;

	int64 preTick;
	double curFreq;
	FILE *tapeBIN1, *tapeInfo;

	char wndName[500];

	void init();
	void updateTick();
};
//----------------------------------------//


struct RAW_HEAD {
	uint32_t type;
	uint32_t rows;
	uint32_t cols;
	uint32_t tileFlag;
	uint32_t tileRows;
	uint32_t tileCols;
	uint32_t rev6;
	uint32_t rev7;
};
typedef struct RAW_HEAD RawHead;

NAT_EXPORT void tear_tile_any(
	FILE* fd,RawHead& hd,
	Mat& img,
	int px,int py,
	int scale
);
NAT_EXPORT int tear_tile_nxn(
	FILE* fd,
	Mat& img,
	long tid,
	long cntX,long cntY,
	int scale
);


#define TILE_SEQUENCE   0x0
#define TILE_INTERLEAVE 0x1

inline long tileTop(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	long gy=tid/hd.tileCols;
	if(hd.tileFlag==TILE_INTERLEAVE){
		gx = hd.tileCols - gx - 1;//odd line modification
	}
	gy=gy-1;
	if(gy<0){
		return -1;
	}
	return gx+gy*hd.tileCols;
}

inline long tileBottom(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	long gy=tid/hd.tileCols;
	if(hd.tileFlag==TILE_INTERLEAVE){
		gx = hd.tileCols - gx - 1;//odd line modification
	}
	gy=gy+1;
	if(gy>=(long)hd.tileRows){
		return -1;
	}
	return gx+gy*hd.tileCols;
}

inline long tileLeft(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	long gy=tid/hd.tileCols;
	if(hd.tileFlag==TILE_INTERLEAVE){
		if(gy%2!=0){
			gx = gx + 2;//odd line modification
		}
	}
	gx=gx-1;
	if(gx<0){
		return -1;
	}
	return gx+gy*hd.tileCols;
}

inline long tileRight(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	long gy=tid/hd.tileCols;
	if(hd.tileFlag==TILE_INTERLEAVE){
		if(gy%2!=0){
			gx = gx - 2;//odd line modification
		}
	}
	gx=gx+1;
	if((long)hd.tileCols<=gx){
		return -1;
	}
	return gx+gy*hd.tileCols;
}

inline long tidByDir(RawHead& hd,long tid,char dir){
	switch(dir){
	case '\211'://~'v'
	case '^'://top
		tid = tileTop(tid,hd);
		break;
	case '\241'://~'^'
	case 'v'://bottom
		tid = tileBottom(tid,hd);
		break;
	case '\301'://~'>'
	case '<'://left
		tid = tileLeft(tid,hd);
		break;
	case '\303'://~'<'
	case '>'://right
		tid = tileRight(tid,hd);
		break;
	}
	return tid;
}


inline bool IsEndOfRow(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	if((gx+1)>=(long)hd.tileCols){
		return true;
	}
	return false;
}

inline bool IsBegOfRow(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	if(gx==0){
		return true;
	}
	return false;
}

inline bool IsEventRow(uint32_t tid,RawHead& hd){
	long gx=tid%hd.tileCols;
	long gy=tid/hd.tileCols;
	if(gy%2==0){
		return true;
	}
	return false;
}

inline void tid2gid(RawHead& hd,long tid,int* gx,int* gy){
	if(tid<0){
		return;
	}
	(*gx)=tid%hd.tileCols;
	(*gy)=tid/hd.tileCols;
	if(hd.tileFlag==TILE_INTERLEAVE){
		if((*gy)%2!=0){
			(*gx) = hd.tileCols - 1 - (*gx);//odd line modification
		}
	}
}

inline long gid2tid(RawHead& hd,long gx,long gy){
	if(gx<0||(long)hd.tileCols<=gx){
		return -1;
	}
	if(gy<0||(long)hd.tileRows<=gy){
		return -1;
	}
	if(hd.tileFlag==TILE_INTERLEAVE){
		if(gy%2!=0){
			gx = hd.tileCols - 1 - gx;//odd line modification
		}
	}
	return gx+gy*hd.tileCols;
}

inline void getTile(FILE* fd,RawHead& hd,long tid,Mat& img){
	if(tid<0){
		return;
	}
	if(img.empty()==true){
		img = Mat::zeros(hd.rows,hd.cols,hd.type);
	}
	size_t len = img.total()*img.elemSize();
	fseek(fd,sizeof(hd)+len*tid,SEEK_SET);
	fread(img.ptr(),img.total(),img.elemSize(),fd);
}

inline void getTile(FILE* fd,RawHead& hd,int gx,int gy,Mat& img){
	long tid = gid2tid(hd,gx,gy);
	if(tid<0){
		return;
	}
	getTile(fd,hd,tid,img);
}

inline void getTile(FILE* fd,RawHead& hd,long tid,char dir,int size,Mat& img){
	Mat tmp(hd.rows,hd.cols,hd.type);
	getTile(fd,hd,tid,tmp);
	Rect roi;
	switch(dir){
	case '\211'://~'v'
	case '^'://top
		roi.x=0; roi.width =hd.cols;
		roi.y=0; roi.height=size;
		break;
	case '\241'://~'^'
	case 'v'://bottom
		roi.x=0; roi.width =hd.cols;
		roi.y=hd.rows-size; roi.height=size;
		break;
	case '\301'://~'>'
	case '<'://left
		roi.x=0; roi.width =size;
		roi.y=0; roi.height=hd.rows;
		break;
	case '\303'://~'<'
	case '>'://right
		roi.x=hd.cols-size; roi.width=size;
		roi.y=0; roi.height=hd.rows;
		break;
	}
	tmp(roi).copyTo(img);
}

inline void putTile(FILE* fd,RawHead& hd,size_t tid,Mat& img){
	size_t len = img.total()*img.elemSize();
	fseek(fd,sizeof(hd)+len*tid,SEEK_SET);
	fwrite(img.ptr(),img.total(),img.elemSize(),fd);
}

inline void putTile(FILE* fd,RawHead& hd,int gx,int gy,Mat& img){
	long tid = gid2tid(hd,gx,gy);
	if(tid<0){
		return;
	}
	putTile(fd,hd,tid,img);
}


#endif
