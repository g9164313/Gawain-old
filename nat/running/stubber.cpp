/*
 * stubber.cpp
 *
 *  Created on: 2019年7月11日
 *      Author: qq
 */
#include <global.hpp>
#include <vision.hpp>

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanDropper_stub1(
	JNIEnv * env,
	jobject thiz,
	jobject objCamera,
	jobject objFilm
){
	jclass cam_clzz  = env->GetObjectClass(objCamera);
	jmethodID is_proc_done_mid = env->GetMethodID(
		cam_clzz,
		"isProcessDone",
		"()Z"
	);
	jmethodID sync_proc_mid = env->GetMethodID(
		cam_clzz,
		"syncProcess",
		"(Lnarl/itrc/vision/ImgFilm;)Lnarl/itrc/vision/ImgFilm;"
	);
	jclass fim_clzz  = env->GetObjectClass(objFilm);
	jfieldID f_width = env->GetFieldID(fim_clzz,"cvWidth","I");
	jfieldID f_height= env->GetFieldID(fim_clzz,"cvHeight","I");
	jfieldID f_cvtype= env->GetFieldID(fim_clzz,"cvType","I");
	jfieldID f_c_snap= env->GetFieldID(fim_clzz,"snap" ,"I");
	int width = env->GetIntField(objFilm, f_width);
	int height= env->GetIntField(objFilm, f_height);
	int cvtype= env->GetIntField(objFilm, f_cvtype);
	int c_snap= env->GetIntField(objFilm, f_c_snap);
	jobject o_pool = env->GetObjectField(
		objFilm,
		env->GetFieldID(fim_clzz,"pool","[B")
	);
	jobject o_over = env->GetObjectField(
		objFilm,
		env->GetFieldID(fim_clzz,"over","[B")
	);
	jbyteArray* j_pool = reinterpret_cast<jbyteArray*>(&o_pool);
	jbyteArray* j_over = reinterpret_cast<jbyteArray*>(&o_over);
	size_t poolSize = env->GetArrayLength(*j_pool);
	size_t overSize = env->GetArrayLength(*j_over);

	//while(env->CallBooleanMethod(objCamera, is_proc_done_mid)==JNI_FALSE){
		jbyte* ptrPool = env->GetByteArrayElements(*j_pool,0);
		jbyte* ptrOver = env->GetByteArrayElements(*j_over,0);
		Mat pool(height, width, cvtype, ptrPool);
		Mat over(height, width, CV_8UC4, ptrOver);
		over = over * 0;//clear
		rectangle(over,Rect(
			100,100,
			100,100
		),Scalar(0,0,255,70),-1);
		cout<<"proces..."<<width<<"x"<<height<<endl;
		//sleep(1);

		env->ReleaseByteArrayElements(*j_pool, ptrPool, 0);
		env->ReleaseByteArrayElements(*j_over, ptrOver, 0);
		objFilm = env->CallObjectMethod(objCamera, sync_proc_mid, objFilm);
	//}
	cout<<"stubber is done!!!"<<endl;
}




