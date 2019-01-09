#include <global.hpp>

void setJDouble(JNIEnv *env, jobject thiz, const char* name, double val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"D");
	env->SetDoubleField(thiz,id,val);
}
double getJDouble(JNIEnv *env, jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"D");
	return env->GetDoubleField(thiz,id);
}

void setFloat(JNIEnv *env, jobject thiz, const char* name, float val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"F");
	env->SetFloatField(thiz,id,val);
}
float getFloat(JNIEnv *env, jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"F");
	return env->GetFloatField(thiz,id);
}

void setJLong(JNIEnv *env, jobject thiz, const char* name, long val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"J");
	env->SetLongField(thiz,id,val);
}
long getJLong(JNIEnv *env, jobject thiz, const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"J");
	return env->GetLongField(thiz,id);
}

void setJInt(JNIEnv *env, jobject thiz, const char* name, int val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"I");
	env->SetIntField(thiz,id,val);
}
int getJInt(JNIEnv *env, jobject thiz, const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"I");
	return env->GetIntField(thiz,id);
}

void setJShort(JNIEnv *env, jobject thiz, const char* name, int16_t val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"S");
	env->SetShortField(thiz,id,val);
}
jshort getJShort(JNIEnv *env, jobject thiz, const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"S");
	return env->GetShortField(thiz,id);
}

void setJByte(JNIEnv *env, jobject thiz, const char* name, uint8_t val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"B");
	env->SetByteField(thiz,id,val);
}
jbyte getJByte(JNIEnv *env,jobject thiz, const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"B");
	return env->GetByteField(thiz,id);
}

void setJChar(JNIEnv *env, jobject thiz, const char* name, char val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"C");
	env->SetCharField(thiz,id,val);
}
char getJChar(JNIEnv *env, jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"C");
	return env->GetCharField(thiz,id);
}

void setJBool(JNIEnv *env, jobject thiz, const char* name, bool val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"Z");
	env->SetBooleanField(thiz,id,val);
}
bool getJBool(JNIEnv *env,jobject thiz, const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"Z");
	return env->GetBooleanField(thiz,id);
}

jsize getJString(JNIEnv *env, jobject thiz, const char* name, const char* dst){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID _id = env->GetFieldID(_clazz,name,"Ljava/lang/String;");
	jstring jsrc = (jstring)env->GetObjectField(thiz,_id);
	return jstrcpy(env,jsrc,dst);
}
//--------------------------------//


jobjectArray create2DArray(JNIEnv * env, size_t cols, size_t rows, jint* value[]){
	jobjectArray _col = env->NewObjectArray(
		cols,
		env->FindClass("[I"),
		NULL
	);
	for(jsize i=0; i<cols; i++){
		jintArray _row = env->NewIntArray(rows);
		env->SetIntArrayRegion(_row, 0, rows, value[i]);
		env->SetObjectArrayElement(_col, i, _row);
		env->DeleteLocalRef(_row);
	}
	return _col;
}

jobjectArray create2DArray(JNIEnv * env, size_t cols, size_t rows, jlong* value[]){
	jobjectArray _col = env->NewObjectArray(
		cols,
		env->FindClass("[J"),
		NULL
	);
	for(jsize i=0; i<cols; i++){
		jlongArray _row = env->NewLongArray(rows);
		env->SetLongArrayRegion(_row, 0, rows, value[i]);
		env->SetObjectArrayElement(_col, i, _row);
		env->DeleteLocalRef(_row);
	}
	return _col;
}
//--------------------------------//

jsize jstrcpy(JNIEnv* env, jstring src, string& dst){
	//TODO: how to use "string"???
	if(src==NULL){ return 0; }
	const char *_src = env->GetStringUTFChars(src,0);
	dst.assign(_src);
	env->ReleaseStringUTFChars(src, _src);
	return env->GetStringLength(src);
}

jsize jstrcpy(JNIEnv* env, jstring src, const char* dst){
	if(src==NULL){ return 0; }
	jboolean is_copy = JNI_FALSE;
	const char *_src = env->GetStringUTFChars(src, &is_copy);
#ifdef _MSC_VER
	strncpy_s((char*)dst,500,_src,500);
#else
	strcpy((char*)dst, _src);
#endif
	env->ReleaseStringUTFChars(src, _src);
	return env->GetStringLength(src);
}
//--------------------------------//

jbyte* byteArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jbyteArray& arr){
	jfieldID id;
	jobject obj;
	id = env->GetFieldID(_clazz,name,"[B");
	obj= env->GetObjectField(thiz,id);
	arr= *(reinterpret_cast<jbyteArray*>(&obj));
	return env->GetByteArrayElements(arr,NULL);
}

jchar* charArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jcharArray& arr){
	jfieldID id;
	jobject obj;
	id = env->GetFieldID(_clazz,name,"[C");
	obj= env->GetObjectField(thiz,id);
	arr= *(reinterpret_cast<jcharArray*>(&obj));
	return env->GetCharArrayElements(arr,NULL);
}

jint* intArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jintArray& arr){
	jfieldID id;
	jobject obj;
	id =env->GetFieldID(_clazz,name,"[I");
	obj=env->GetObjectField(thiz,id);
	arr=*(reinterpret_cast<jintArray*>(&obj));
	return env->GetIntArrayElements(arr,NULL);
}

jlong* longArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jlongArray& arr){
	jfieldID id;
	jobject obj;
	id =env->GetFieldID(_clazz,name,"[J");
	obj=env->GetObjectField(thiz,id);
	arr=*(reinterpret_cast<jlongArray*>(&obj));
	return env->GetLongArrayElements(arr,NULL);
}

jfloat* floatArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jfloatArray& arr){
	jfieldID id;
	jobject obj;
	id =env->GetFieldID(_clazz,name,"[F");
	obj=env->GetObjectField(thiz,id);
	arr=*(reinterpret_cast<jfloatArray*>(&obj));
	return env->GetFloatArrayElements(arr,NULL);
}

jdouble* doubleArray2Ptr(JNIEnv* env,jclass _clazz,jobject thiz,const char* name,jdoubleArray& arr){
	jfieldID id;
	jobject obj;
	id =env->GetFieldID(_clazz,name,"[D");
	obj=env->GetObjectField(thiz,id);
	arr=*(reinterpret_cast<jdoubleArray*>(&obj));
	return env->GetDoubleArrayElements(arr,NULL);
}
//--------------------------------//

void setIntArray(
	JNIEnv* env,
	jobject thiz,
	const char* name,
	int* val, size_t len
){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"[I");
	jintArray arry = env->NewIntArray(len);
	env->SetIntArrayRegion(arry, 0, len, (jint*)val);
	env->SetObjectField(thiz,id,arry);
}

void setDoubleArray(
	JNIEnv* env,
	jobject thiz,
	const char* name,
	double* val, size_t len
){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"[D");
	jdoubleArray arry = env->NewDoubleArray(len);
	env->SetDoubleArrayRegion(arry, 0, len, val);
	env->SetObjectField(thiz,id,arry);
}
//--------------------------------//

void callThreadJoin(JNIEnv* env,jobject thiz,const char* name){
	jclass  clz;
	jfieldID oid;
	jmethodID mid;
	jobject obj;
	clz= env->GetObjectClass(thiz);
	oid= env->GetFieldID(clz,name,"Ljava/lang/Thread;");
	obj= env->GetObjectField(thiz,oid);
	clz= env->FindClass("java/lang/Thread");
	mid= env->GetMethodID(clz,"join","()V");
	env->CallVoidMethod(obj,mid);
}
//--------------------------------//

#if defined _MSC_VER
#define MACRO_LOG_BEG \
	char txt[2048]; \
	va_list args; \
	va_start(args, fmt); \
	vsprintf_s(txt,fmt,args); \
	va_end(args);
#else
#define MACRO_LOG_BEG \
	char txt[2048]; \
	va_list args; \
	va_start(args, fmt); \
	vsprintf(txt,fmt,args); \
	va_end(args);
#endif


void log_msg(JNIEnv* env,const char* name,const char* msg){
	jclass clzz = env->FindClass("narl/itrc/Misc");
	jmethodID mid = env->GetStaticMethodID(
		clzz,
		name,
		"(Ljava/lang/String;[Ljava/lang/Object;)V"
	);
	jstring j_msg = env->NewStringUTF(msg);
	env->CallStaticVoidMethod(
		clzz,
		mid,
		j_msg
	);
	env->DeleteLocalRef(j_msg);
}

void logv(JNIEnv* env,const char* fmt,...){
	MACRO_LOG_BEG
	log_msg(env,"logv",txt);
}

void logw(JNIEnv* env,const char* fmt,...){
	MACRO_LOG_BEG
	log_msg(env,"logw",txt);
}

void loge(JNIEnv* env,const char* fmt,...){
	MACRO_LOG_BEG
	log_msg(env,"loge",txt);
}
//--------------------------------//

extern "C" JNIEXPORT jlong Java_narl_itrc_Misc_realloc__JJ(
	JNIEnv* env,
	jclass  clzz,
	jlong   ptr,
	jlong   len
){
	return (jlong)realloc((void*)ptr,(size_t)len);
}

extern "C" JNIEXPORT jlong Java_narl_itrc_Misc_free__J(
	JNIEnv* env,
	jclass  clzz,
	jlong   ptr
){
	free((void*)ptr);
	return 0;
}

NAT_EXPORT void dummy(){
	//stupid Virtual C++ need this to generate a library!!!!
}


