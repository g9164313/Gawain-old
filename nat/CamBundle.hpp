#ifndef CAMBUNDLE_HPP
#define CAMBUNDLE_HPP

#include <global.hpp>

extern void set_img_array(
	JNIEnv * env,
	jobject bnd,
	jfieldID fid,
	const Mat& img,
	const char* name,
	const char* ext
);

extern void drawEdge(Mat& overlay,const Mat& edge);

#define MACRO_SET_IMG_INFO(ova) set_img_array(env,bundle,idImgInfo,ova,"imgInfo",".png");

//--------------------------------------------//

#define MACRO_READY \
	jclass b_clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(b_clzz,"ptrCntx" ,"J"); \
	jfieldID idBuff = env->GetFieldID(b_clzz,"ptrBuff" ,"J"); \
	jfieldID idType = env->GetFieldID(b_clzz,"bufType" ,"I"); \
	jfieldID idSizeW= env->GetFieldID(b_clzz,"bufSizeW","I"); \
	jfieldID idSizeH= env->GetFieldID(b_clzz,"bufSizeH","I"); \
	jfieldID idImgBuff = env->GetFieldID(b_clzz,"imgBuff","[B"); \
	jfieldID idImgInfo = env->GetFieldID(b_clzz,"imgInfo","[B"); \

#define MACRO_PREPARE \
	MACRO_READY \
	void* cntx = (void*)(env->GetLongField(bundle,idCntx)); \
	void* buff = (void*)(env->GetLongField(bundle,idBuff)); \
	int type  = env->GetIntField (bundle,idType); \
	int width = env->GetIntField (bundle,idSizeW); \
	int height= env->GetIntField (bundle,idSizeH);

#define MACRO_RESET_FIELD(buff,width,height,type) \
	env->SetLongField(bundle,idBuff ,(jlong)(buff)); \
	env->SetIntField (bundle,idSizeH,(jint )(height)); \
	env->SetIntField (bundle,idSizeW,(jint )(width)); \
	env->SetIntField (bundle,idType ,(jint )(type));

//--------------------------------------------//

#define MACRO_SETUP_BEG MACRO_READY

#define MACRO_SETUP_END(cntx,buff,width,height,type) \
	env->SetLongField(bundle, idCntx ,(jlong)(cntx)); \
	MACRO_RESET_FIELD(buff,width,height,type)

#define MACRO_SETUP_END1(cntx) MACRO_SETUP_END(cntx,0,0,0,0)

#define MACRO_SETUP_END2(cntx,type) MACRO_SETUP_END(cntx,0,0,0,type)

//--------------------------------------------//


#define MACRO_FETCH_BEG MACRO_PREPARE \
	if(cntx==NULL){ return; }

#define MACRO_FETCH_REMAP(_src) \
	buff  = realloc(buff,(size_t)(_src.total()*_src.elemSize())); \
	type  =_src.type(); \
	width =_src.cols; \
	height=_src.rows; \
	Mat _dst(height,width,type,buff); \
	_src.copyTo(_dst);

#define MACRO_FETCH_COPY(_src) \
	if(buff==NULL || width!=tmp.cols || height!=tmp.rows){ \
		MACRO_FETCH_REMAP(_src) \
		MACRO_RESET_FIELD(buff,width,height,type) \
		set_img_array(env,bundle,idImgBuff,_dst,"imgBuff",".jpg");\
	}else{ \
		Mat _dst(height,width,type,buff); \
		_src.copyTo(_dst); \
		set_img_array(env,bundle,idImgBuff,_dst,"imgBuff",".jpg");\
	}
//--------------------------------------------//

#define MACRO_CLOSE_BEG MACRO_PREPARE

#define MACRO_CLOSE_END \
	free(buff);\
	MACRO_SETUP_END(0,0,0,0,0)

//--------------------------------------------//


/*#define MACRO_CHECK_CNTX \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(clzz,"ptrCntx","J"); \
	void* cntx = (void*)(env->GetLongField(bundle,idCntx));

#define MACRO_BUNDLE_CHECK_CNTX_VOID \
	MACRO_CHECK_CNTX \
	if(matx==NULL){ return; }

#define MACRO_CHECK_MATX \
	jclass clzz=env->GetObjectClass(bundle); \
	jfieldID idMatx = env->GetFieldID(clzz,"ptrMatx","J"); \
	Mat* matx = (Mat*)(env->GetLongField(bundle,idMatx));

#define MACRO_BUNDLE_CHECK_MATX_VOID \
	MACRO_CHECK_MATX \
	if(matx==NULL){ return; }

#define MACRO_BUNDLE_CHECK_MATX_NULL \
	MACRO_CHECK_MATX \
	if(matx==NULL){ return NULL; }
*/

extern void setDataInfo(
	JNIEnv * env,
	jobject bundle,
	const Mat& info
);

#endif
