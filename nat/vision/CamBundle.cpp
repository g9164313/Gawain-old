/*
 * CamBundle.cpp
 *
 *  Created on: 2016年5月2日
 *      Author: qq
 */
#include "vision/CamBundle.hpp"
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <global.hpp>
#ifndef VISION
#define VISION
#endif

extern "C" void setupCallback(
	JNIEnv * env,
	jobject bundle,
	void* context,
	int width,
	int height,
	int format
){
	jclass b_clzz = env->GetObjectClass(bundle);
	env->SetLongField(bundle,env->GetFieldID(b_clzz,"ptrCntx" ,"J"),(jlong)(context));
	env->SetIntField (bundle,env->GetFieldID(b_clzz,"bufSizeW","I"),(jint )(width  ));
	env->SetIntField (bundle,env->GetFieldID(b_clzz,"bufSizeH","I"),(jint )(height ));
	env->SetIntField (bundle,env->GetFieldID(b_clzz,"bufCvFmt","I"),(jint )(format ));
	env->CallVoidMethod(
		bundle,
		env->GetMethodID(b_clzz,"setupCallback","()V")
	);
}

extern "C" void fetchCallback(
	JNIEnv * env,
	jobject thiz,
	jobject bundle,
	const Mat& src
){
	jclass b_clzz = env->GetObjectClass(bundle);
	/*int zoomLocaX = env->GetIntField(bundle, env->GetFieldID(b_clzz,"zoomLocaX","I"));
	int zoomLocaY = env->GetIntField(bundle, env->GetFieldID(b_clzz,"zoomLocaY","I"));
	int zoomSizeW = env->GetIntField(bundle, env->GetFieldID(b_clzz,"zoomSizeW","I"));
	int zoomSizeH = env->GetIntField(bundle, env->GetFieldID(b_clzz,"zoomSizeH","I"));
	int zoomScale = env->GetIntField(bundle, env->GetFieldID(b_clzz,"zoomScale","I"));*/

	//project image data to another coordinate space~~
	Mat dst;
	switch(src.channels()){
	case 3:
		cvtColor(src,dst,COLOR_BGR2RGB);
		break;
	case 1:
		cvtColor(src,dst,COLOR_GRAY2RGB);
		break;
	default:
		return;
	}

	jsize buff_size = dst.cols * dst.rows * 3;
	jbyteArray outBuf = env->NewByteArray(buff_size);
	env->SetByteArrayRegion(
		outBuf,
		0, buff_size,
		(jbyte*) dst.data
	);
	env->CallVoidMethod(
		bundle,
		env->GetMethodID(b_clzz,"fetchCallback","(J[BII)V"),
		outBuf,
		(jlong)&src,
		dst.cols,
		dst.rows
	);
	env->DeleteLocalRef(outBuf);
}
//----------------------------//

#define MODE_MEM_DISK 0x001
#define MODE_MEM_ZIP  0x002
#define MODE_MEM_PNG  0x010
#define MODE_MEM_TIF  0x020
#define MODE_MEM_JPG  0x030
#define MODE_MAPPING  0x100

static int prepare_file(
	JNIEnv* env,
	jobject thiz,
	jclass o_clzz,
	jstring jname,
	size_t len
){
	char tmp_buff[4096];
	jstrcpy(env,jname,tmp_buff);

	int fd = open(
		tmp_buff,
		O_RDWR | O_CREAT | O_TRUNC,
		(mode_t)0666
	);

	if(fd!=-1){
		//how to keep the previous file size???
		size_t cnt = len / sizeof(tmp_buff);
		for(;cnt!=0;){
			write(fd,tmp_buff,sizeof(tmp_buff));//Just put dummy data
			cnt--;
		}
		write(fd,tmp_buff,len % sizeof(tmp_buff));
	}

	env->SetIntField(
		thiz,
		env->GetFieldID(o_clzz,"blkFileDesc","I"),
		(jint)fd
	);
	return fd;
}

extern "C" JNIEXPORT jlong JNICALL Java_narl_itrc_vision_BlkRender_blkAllocate(
	JNIEnv* env,
	jobject thiz,
	jstring jname,
	jobject bundle
){
	jclass b_clzz = env->GetObjectClass(bundle);
	jint format = env->GetIntField(
		bundle,
		env->GetFieldID(b_clzz,"bufType" ,"I")
	);
	uint32_t width = env->GetIntField(
		bundle,
		env->GetFieldID(b_clzz,"bufSizeW","I")
	);
	uint32_t height = env->GetIntField(
		bundle,
		env->GetFieldID(b_clzz,"bufSizeH","I")
	);

	uint32_t chan = CV_MAT_CN(format);
	uint32_t bits;
	switch(format){
	default:
	case CV_8U:
	case CV_8S:
		bits = 1;//byte
		break;
	case CV_16U:
	case CV_16S:
		bits = 2;//byte
		break;
	case CV_32S:
	case CV_32F:
		bits = 4;//byte
		break;
	case CV_64F:
		bits = 8;//byte
		break;
	}

	jclass o_clzz = env->GetObjectClass(thiz);
	jfieldID idAddress = env->GetFieldID(o_clzz,"blkAddress","J");
	jfieldID idAllSize = env->GetFieldID(o_clzz,"blkAllSize","J");

	jint mod = env->GetIntField(
		thiz,
		env->GetFieldID(o_clzz,"blkMode","I")
	);//Mode type can be referenced in Java File

	void* buf = (void*) env->GetLongField(thiz,idAddress);

	size_t count = (size_t) env->GetLongField(
		thiz,
		env->GetFieldID(o_clzz,"blkCounter","J")
	);
	size_t len = 128 + count * width * height * chan * bits;

	switch(mod){
	default:
	case MODE_MEM_DISK:
	case MODE_MEM_ZIP:
	case MODE_MEM_PNG:
	case MODE_MEM_TIF:
	case MODE_MEM_JPG:
		buf = realloc(buf,len);
		if(buf==NULL){
			return 0;
		}
		break;
	case MODE_MAPPING:
		//this is special case, we must create a file then mapping it to memory....
		int fd = prepare_file(env,thiz,o_clzz,jname,len);
		buf = mmap(
			NULL,len,
			PROT_READ|PROT_WRITE,
			MAP_SHARED,
			fd,	0
		);
		if(buf==MAP_FAILED){
			close(fd);
			return 0L;
		}
		break;
	}
	env->SetLongField(thiz,idAddress,(jlong)buf);
	env->SetLongField(thiz,idAllSize,(jlong)len);

	/* prepare header, the size is 128byte and this will be 32 slots, each slot is 32-bit */
	memset(buf,0,128);
	uint32_t* head = (uint32_t*)buf;
	head[0] = 0x4B4C5542;//signature
	head[1] = count; //the number of images
	head[2] = width; //image width
	head[3] = height;//image height
	head[4] = format;//image format, it is just CV_TYPE
	//printf("realloc=%p @ %ld # cnt=%d, %dx%d, chan=%d, %dbyte",buf,len,cnt,width,height,chan,type);
	//cout<<endl;
	return (jlong)buf;
}

static void flush2image(void* buf,size_t len,char* target,const char* extension){

	uint32_t* head = (uint32_t*)buf;
	uint32_t cnt = head[1];//the number of images
	uint32_t ww  = head[2];//image width
	uint32_t hh  = head[3];//image height
	uint32_t fmt = head[4];//image format, it is just CV_TYPE

	uint8_t* ptr = (uint8_t*)buf;

	ptr+=128;//skip header~~~

	char name[100]={0};
	strcat(name,target);
	strcat(name,extension);

	Mat img(hh*cnt,ww,fmt,ptr);
	imwrite(name,img);
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_BlkRender_blkFlush(
	JNIEnv* env,
	jobject thiz,
	jstring jname
){
	jclass o_clzz = env->GetObjectClass(thiz);

	jint mod = env->GetIntField(
		thiz,
		env->GetFieldID(o_clzz,"blkMode","I")
	);//Mode type can be referenced in Java File

	void* buf = (void*)env->GetLongField(
		thiz,
		env->GetFieldID(o_clzz,"blkAddress","J")
	);

	size_t len = env->GetLongField(
		thiz,
		env->GetFieldID(o_clzz,"blkAllSize","J")
	);

	FILE* fs;
	char name[500];
	jstrcpy(env,jname,name);

	switch(mod){
	case MODE_MEM_DISK:
		fs = fopen(name,"wb");
		fwrite(buf, sizeof(uint8_t), len, fs);
		fclose(fs);
		break;
	case MODE_MEM_ZIP:
		break;
	case MODE_MEM_PNG:
		flush2image(buf,len,name,".png");
		break;
	case MODE_MEM_TIF:
		flush2image(buf,len,name,".tif");
		break;
	case MODE_MEM_JPG:
		flush2image(buf,len,name,".jpg");
		break;
	case MODE_MAPPING:
		if(msync(buf,len,MS_SYNC)==-1){
			cerr<<"fail to sync memory!!!"<<endl;
		}
		if(munmap(buf,len)==-1){
			cerr<<"fail to unmap memory!!!"<<endl;
		}
		close(env->GetIntField(
			thiz,
			env->GetFieldID(o_clzz,"blkFileDesc","I")
		));
		break;
	}
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_vision_BlkRender_blkFree(
	JNIEnv* env,
	jobject thiz
){
	jclass o_clzz = env->GetObjectClass(thiz);
	jfieldID idAddress = env->GetFieldID(o_clzz,"blkAddress" ,"J");

	jint mod = env->GetIntField(
		thiz,
		env->GetFieldID(o_clzz,"blkMode","I")
	);//Mode type can be referenced in Java File

	void* buf = (void*)env->GetLongField(thiz,idAddress);

	switch(mod){
	case MODE_MEM_DISK:
	case MODE_MEM_ZIP:
	case MODE_MEM_PNG:
		free(buf);
		break;
	case MODE_MAPPING:
		//memory is mapping to disk, so we don't release any thing~~~
		break;
	}

	env->SetLongField(thiz,idAddress,0);
}

/**
 * set a overlay picture to indicate information
 * @param bnd - just 'bundle'
 * @param ova - 4-channel image (PNG format)
 */
void set_img_array(
	JNIEnv * env,
	jobject bnd,
	jfieldID fid,
	const Mat& img,
	const char* name,
	const char* ext
){
	vector<uchar> buf;
	imencode(ext,img,buf);
	jbyteArray arr = env->NewByteArray(buf.size());
	env->SetByteArrayRegion(
		arr,
		0,buf.size(),
		(jbyte*)&buf[0]
	);
	env->SetObjectField(bnd,fid,arr);
}

void drawImage(Mat& overlay,const Mat& src){
	Mat chan[4];//Blue,Green,Red,Alpha
	split(overlay,chan);
	if(src.type()==CV_8UC1){
		chan[0] = src;
		chan[1] = src;
		chan[2] = src;
	}
	chan[3] = 255;
	merge(chan,4,overlay);
}

void drawEdgeMap(Mat& overlay,const Mat& edge){
	if(edge.type()!=CV_8UC1){
		cerr<<"[drawEdge] only support mono picture"<<endl;
		return;
	}
	Mat chan[4];//Blue,Green,Red,Alpha
	split(overlay,chan);
	chan[1] += (edge);
	chan[3] += (edge);
	merge(chan,4,overlay);
}

void drawRectangle(
	Mat& overlay,
	Rect rec,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	rectangle(overlay,rec,clr,thickness,lineType);
}

void drawRectangle(
	Mat& overlay,
	Point center,
	int width,
	int height,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	width = width/2;
	height= height/2;
	Point pt1 = center;
	Point pt2 = center;
	pt1.x = pt1.x - width;
	pt1.y = pt1.y - height;
	pt2.x = pt2.x + width;
	pt2.y = pt2.y + height;
	rectangle(overlay,pt1,pt2,clr,thickness,lineType);
}

void drawCrossT(
	Mat& overlay,
	Point pts,
	int lineSize,
	const Scalar& color,
	int thickness
){
	Scalar clr = color;
	clr[3] = 255;
	lineSize = lineSize/2;
	Point p1,p2;
	p1 = p2 = pts;
	p1.x = p1.x - lineSize;
	p2.x = p2.x + lineSize;
	line(overlay,p1,p2,clr,thickness);
	p1 = p2 = pts;
	p1.y = p1.y - lineSize;
	p2.y = p2.y + lineSize;
	line(overlay,p1,p2,clr,thickness);
}

void drawCrossX(
	Mat& overlay,
	Point pts,
	int lineSize,
	const Scalar& color,
	int thickness
){
	Scalar clr = color;
	clr[3] = 255;
	lineSize = ceil((lineSize*1.414213562f)/4.f);
	Point p1,p2;
	p1 = p2 = pts;
	p1.x = p1.x - lineSize;
	p1.y = p1.y - lineSize;
	p2.x = p2.x + lineSize;
	p2.y = p2.y + lineSize;
	line(overlay,p1,p2,clr,thickness);
	p1 = p2 = pts;
	p1.x = p1.x + lineSize;
	p1.y = p1.y - lineSize;
	p2.x = p2.x - lineSize;
	p2.y = p2.y + lineSize;
	line(overlay,p1,p2,clr,thickness);
}

static Scalar mapJetColor[]={
	Scalar(  0,  0,128,255),
	Scalar(  0,  0,255,255),
	Scalar(  0,128,255,255),
	Scalar(  2,255,254,255),
	Scalar(130,255,126,255),
	Scalar(255,252,  0,255),
	Scalar(255,124,  0,255),
	Scalar(252,  0,  0,255)
};
void drawContour(
	Mat& overlay,
	vector<vector<Point> >& cts
){
	for(int i=0; i<cts.size(); i++){
		polylines(
			overlay, cts[i],
			false,
			mapJetColor[i%8],
			1, LINE_8
		);
	}
}

static unsigned int mapIndex = 0;
void drawPolyline(
	Mat& overlay,
	vector<Point>& cts,
	bool closed,
	int thickness,
	int lineType
){
	polylines(
		overlay, cts, closed,
		mapJetColor[mapIndex%8],
		thickness, lineType
	);
	mapIndex++;
}

void drawPolyline(
	Mat& overlay,
	vector<Point>& cts,
	bool closed,
	const Scalar& color,
	int thickness,
	int lineType
){
	Scalar clr = color;
	clr[3] = 255;
	polylines(
		overlay, cts, closed,
		clr,
		thickness, lineType
	);
}


