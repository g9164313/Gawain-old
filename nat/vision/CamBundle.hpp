#ifndef CAMBUNDLE_HPP
#define CAMBUNDLE_HPP

#include <global.hpp>

#define MACRO_SET_IMG_INFO(ova) set_img_array(env,bundle,idImgInfo,ova,"imgInfo",".png");

//-----------------------------------------------------------------------//

#define MACRO_FIELD_GET \
	jclass b_clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(b_clzz,"ptrCntx" ,"J"); \
	jfieldID idSizeW= env->GetFieldID(b_clzz,"bufSizeW","I"); \
	jfieldID idSizeH= env->GetFieldID(b_clzz,"bufSizeH","I"); \
	jfieldID idCvFmt= env->GetFieldID(b_clzz,"bufCvFmt","I"); \

#define MACRO_FIELD_PREP \
	void* cntx  = (void*)(env->GetLongField(bundle,idCntx)); \
	int   width = env->GetIntField (bundle,idSizeW); \
	int   height= env->GetIntField (bundle,idSizeH); \
	int   format= env->GetIntField (bundle,idCvFmt); \

#define MACRO_FIELD_SET(context,width,height,format) \
	env->SetLongField(bundle,idCntx ,(jlong)(context)); \
	env->SetIntField (bundle,idSizeW,(jint )(width  )); \
	env->SetIntField (bundle,idSizeH,(jint )(height )); \
	env->SetIntField (bundle,idCvFmt,(jint )(format )); \

//-----------------------------------------------------------------------//

#define MACRO_SETUP_BEG MACRO_FIELD_GET

#define MACRO_SETUP_END(cntx,width,height,format) MACRO_FIELD_SET(cntx,width,height,format)

extern "C" void setupCallback(
	JNIEnv * env,
	jobject bundle,
	void* context,
	int width,
	int height,
	int format
);

//-----------------------------------------------------------------------//

#define MACRO_FETCH_BEG \
	MACRO_FIELD_GET \
	MACRO_FIELD_PREP \
	if(cntx==NULL){ return; } \

extern "C" void fetchCallback(
	JNIEnv * env,
	jobject thiz,
	jobject bundle,
	const Mat& data
);

//-----------------------------------------------------------------------//

#define MACRO_CLOSE_BEG \
	MACRO_FIELD_GET \
	MACRO_FIELD_PREP

#define MACRO_CLOSE_END \
	MACRO_FIELD_SET(0,0,0,0)

//-----------------------------------------------------------------------//


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

#define MACRO_READY \
	jclass b_clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(b_clzz,"ptrCntx" ,"J"); \
	jfieldID idBuff = env->GetFieldID(b_clzz,"ptrBuff" ,"J"); \
	jfieldID idType = env->GetFieldID(b_clzz,"bufType" ,"I"); \
	jfieldID idSizeW= env->GetFieldID(b_clzz,"bufSizeW","I"); \
	jfieldID idSizeH= env->GetFieldID(b_clzz,"bufSizeH","I"); \
	jfieldID idImgBuff = env->GetFieldID(b_clzz,"imgBuff","[B"); \
	jfieldID idImgInfo = env->GetFieldID(b_clzz,"imgInfo","[B");

#define MACRO_PREPARE \
	MACRO_READY \
	void* cntx= (void*)(env->GetLongField(bundle,idCntx)); \
	void* buff= (void*)(env->GetLongField(bundle,idBuff)); \
	int type  = env->GetIntField (bundle,idType); \
	int width = env->GetIntField (bundle,idSizeW); \
	int height= env->GetIntField (bundle,idSizeH);

#define MACRO_RESET_FIELD(buff,width,height,type) \
	env->SetLongField(bundle,idBuff ,(jlong)(buff)); \
	env->SetIntField (bundle,idSizeH,(jint )(height)); \
	env->SetIntField (bundle,idSizeW,(jint )(width)); \
	env->SetIntField (bundle,idType ,(jint )(type));

#define MACRO_PREPARE_CNTX \
	jclass b_clzz=env->GetObjectClass(bundle); \
	jfieldID idCntx = env->GetFieldID(b_clzz,"ptrCntx" ,"J"); \
	void* cntx = (void*)(env->GetLongField(bundle,idCntx));

#define MACRO_FETCH_REMAP(_src) \
	buff  = realloc(buff,(size_t)(_src.total()*_src.elemSize())); \
	type  =_src.type(); \
	width =_src.cols; \
	height=_src.rows; \
	Mat _dst(height,width,type,buff); \
	_src.copyTo(_dst);

#define MACRO_FETCH_COPY(_src) \
	if(buff==NULL || width!=_src.cols || height!=_src.rows){ \
		MACRO_FETCH_REMAP(_src) \
		MACRO_RESET_FIELD(buff,width,height,type) \
		set_img_array(env,bundle,idImgBuff,_dst,"imgBuff",".png");\
	}else{ \
		Mat _dst(height,width,type,buff); \
		_src.copyTo(_dst); \
		set_img_array(env,bundle,idImgBuff,_dst,"imgBuff",".png");\
	}

extern void setDataInfo(
	JNIEnv * env,
	jobject bundle,
	const Mat& info
);
*/

#endif
