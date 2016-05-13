#include <global.hpp>

void setJdouble(JNIEnv *env,jobject thiz,const char* name,double val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"D");
	env->SetDoubleField(thiz,id,val);
}
double getJdouble(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"D");
	return env->GetDoubleField(thiz,id);
}

void setFloat(JNIEnv *env,jobject thiz,const char* name,float val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"F");
	env->SetFloatField(thiz,id,val);
}
float getFloat(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"F");
	return env->GetFloatField(thiz,id);
}

void setJlong(JNIEnv *env,jobject thiz,const char* name,long val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"J");
	env->SetLongField(thiz,id,val);
}
long getJlong(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"J");
	return env->GetLongField(thiz,id);
}

void setJint(JNIEnv *env,jobject thiz,const char* name,int val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"I");
	env->SetIntField(thiz,id,val);
}
int getJint(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"I");
	return env->GetIntField(thiz,id);
}

/*void setJint16_t(JNIEnv *env,jobject thiz,const char* name,int16_t val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"S");
	env->SetShortField(thiz,id,val);
}
int16_t getJint16_t(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"S");
	return env->GetShortField(thiz,id);
}

void setJuint8_t(JNIEnv *env,jobject thiz,const char* name,uint8_t val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"B");
	env->SetByteField(thiz,id,val);
}
uint8_t getJuint8_t(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"B");
	return env->GetByteField(thiz,id);
}*/

void setJchar(JNIEnv *env,jobject thiz,const char* name,char val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"C");
	env->SetCharField(thiz,id,val);
}
char getJchar(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"C");
	return env->GetCharField(thiz,id);
}

void setJbool(JNIEnv *env,jobject thiz,const char* name,bool val){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"Z");
	env->SetBooleanField(thiz,id,val);
}
bool getJbool(JNIEnv *env,jobject thiz,const char* name){
	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id = env->GetFieldID(_clazz,name,"Z");
	return env->GetBooleanField(thiz,id);
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

jsize jstrcpy(JNIEnv* env, jstring src,const char* dst){
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

NAT_EXPORT void dummy(){
	//stupid Virtual C++ need this to generate a library!!!!
}


