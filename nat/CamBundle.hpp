#ifndef CAMBUNDLE_HPP
#define CAMBUNDLE_HPP

#include <jni.h>
#include <string>
#include <iostream>

extern jint* intArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jintArray& arr);
extern jlong* longArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jlongArray& arr);
extern jfloat* floatArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jfloatArray& arr);

using namespace std;

//it should be same as declaration in java file!!!
#define PR_SIZE 4
#define ROI_COLS 6
#define PIN_COLS 4

#define MACRO_FETCH_CHECK \
	if(cam==NULL){ return; } \
	if(cam->ctxt==NULL){ return; }

#define MACRO_CLOSE_CHECK \
	CamBundle* cam = getContext(env,bundle); \
	if(cam==NULL){ return; }

#define MACRO_GATHER_CHECK_RES0 \
	if(cam==NULL){ return 0; } \
	if(cam->ctxt==NULL){ return 0; }

#define MACRO_UPDATE_MAT_START(i) \
	jlongArray arr; \
	jlong* buf = longArray2Ptr(env,clzz,thiz,"ptrMatx",arr); \
	Mat* src = (Mat*)(buf[i]); \
	if(src!=NULL){ delete src; }

#define MACRO_UPDATE_MAT_END(i) \
	buf[i] = (jlong)src; \
	env->ReleaseLongArrayElements(arr,buf,0);

class CamBundle{
public:
	JNIEnv* env;
	jclass clzz;
	jobject thiz;//this is object for interface(CamBase)
	void* ctxt;//it is a variable pointer for storage~~~

	CamBundle(JNIEnv* _env,jobject _obj):
		env(_env),thiz(_obj),ctxt(NULL),matImage(NULL),matSize(0)
	{
		createMat();

		if(env==NULL){
			return;
		}

		clzz=env->GetObjectClass(thiz);

		idPtrCntx = env->GetFieldID(clzz,"ptrCntx","J");

		midOptEnbl = env->GetMethodID(clzz,"updateOptEnbl","(Z)V");

		midMsgLast = env->GetMethodID(clzz,"updateMsgLast","(Ljava/lang/String;)V");
	}

	~CamBundle(){

		delete[] matImage;

		if(env==NULL){
			return;
		}

		env->SetLongField(thiz,idPtrCntx,0);
	}

	void updateOptEnbl(bool flag){
		if(env==NULL){
			cout<<"optEnbl="<<flag<<endl;
		}else{
			env->CallVoidMethod(thiz,midOptEnbl,(jboolean)flag);
		}
	}

	void updateMsgLast(const char* txt){
		if(env==NULL){
			cout<<"msgLast="<<txt<<endl;
		}else{
			env->CallVoidMethod(thiz,midMsgLast,env->NewStringUTF(txt));
		}
	}

	void updateEnableState(bool flag, const char* txt){
		if(env==NULL){
			cout<<"optEnbl="<<flag<<",msgLast="<<txt<<endl;
		}else{
			env->CallVoidMethod(thiz,midOptEnbl,(jboolean)flag);
			if(txt!=NULL){
				env->CallVoidMethod(thiz,midMsgLast,env->NewStringUTF(txt));
			}
		}
	}

	void updateInfo(int type,int width,int height){
		if(env==NULL){
			cout<<"[INFO] type="<<type<<", size="<<width<<"x"<<"height"<<endl;
			return;
		}
		env->SetIntField(
			thiz,
			env->GetFieldID(clzz,"infoType","I"),
			type
		);
		env->SetIntField(
			thiz,
			env->GetFieldID(clzz,"infoWidth","I"),
			width
		);
		env->SetIntField(
			thiz,
			env->GetFieldID(clzz,"infoHeight","I"),
			height
		);
	}

	Mat* getMat(int idx){
		if(idx>=matSize){ return NULL; }
		return &(matImage[idx]);
	}

	void getRoi(int idx,int* typ,Rect* roi){
		if(idx>=PR_SIZE){
			return;
		}
		jintArray jarr;
		jint* roival = intArray2Ptr(env,clzz,thiz,"roiVal",jarr);
		if(typ!=NULL){
			*typ = roival[idx*ROI_COLS+0];
		}
		if(roi!=NULL){
			roi->x = roival[idx*ROI_COLS+1];
			roi->y = roival[idx*ROI_COLS+2];
			roi->width = roival[idx*ROI_COLS+3];
			roi->height= roival[idx*ROI_COLS+4];
		}
		env->ReleaseIntArrayElements(jarr,roival,0);
	}

	Mat& updateSource(){
		return matImage[0];
	}

	Mat& updateSource(
		uint32_t height,uint32_t width,
		uint32_t type,
		uint8_t* buff
	){
		Mat& src = matImage[0];
		Mat tmp(height,width,type,buff);
		src = tmp;
		return src;
	}

	Mat& updateOverlay(){
		Mat& src = matImage[0];
		Mat& dst = matImage[1];
		if(dst.empty()==true){
			dst.create(src.size(),CV_8UC3);
		}
		switch(src.type()){
		case CV_8UC1:
			cvtColor(src,dst,COLOR_GRAY2BGR);
			break;
		case CV_8UC3:
			src.copyTo(dst);
			break;
		}
		return matImage[1];
	}

private:

	Mat* matImage;
	int matSize;
	jfieldID idPtrCntx;
	jmethodID midOptEnbl,midMsgLast;

	void createMat(){
		jlongArray arr;
		jlong* buf = longArray2Ptr(env,clzz,thiz,"ptrMatx",arr);
		matSize= env->GetArrayLength(arr);
		matImage = new Mat[matSize];
		for(int i=0; i<matSize; i++){
			buf[i] = (jlong)(&(matImage[i]));
		}
		env->ReleaseLongArrayElements(arr,buf,0);
	}
};

inline CamBundle* getContext(JNIEnv* env,jobject thiz){
	jfieldID fid = env->GetFieldID(
		env->GetObjectClass(thiz),
		"ptrCntx",
		"J"
	);
	CamBundle* bnd = (CamBundle*)(env->GetLongField(thiz,fid));
	if(bnd!=NULL){
		bnd->env = env;
		bnd->thiz= thiz;
		bnd->clzz= env->GetObjectClass(thiz);
	}
	return bnd;
}

inline CamBundle* initContext(JNIEnv* env,jobject thiz){
	CamBundle* inf = new CamBundle(env,thiz);
	jfieldID fid = env->GetFieldID(
		env->GetObjectClass(thiz),
		"ptrCntx",
		"J"
	);
	env->SetLongField(thiz,fid,(jlong)inf);
	return inf;
}

inline void finalContext(JNIEnv* env,jobject thiz,CamBundle* cam,const char* msg){
	cam->updateEnableState(false,msg);
	delete cam;
	jfieldID fid = env->GetFieldID(
		env->GetObjectClass(thiz),
		"ptrCntx",
		"J"
	);
	env->SetLongField(thiz,fid,0L);
}

#endif
