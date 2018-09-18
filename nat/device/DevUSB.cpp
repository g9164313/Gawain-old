/*
 * DevUSB.cpp
 *  In Unix, the implement is 'libusb-1.0' ().<p>
 *  In Windows, the implement will be UMDF or WinUSB (not sure).<p>
 *  Created on: 2018年9月6日
 *      Author: qq
 */
#include <global.hpp>
#include <libusb.h>

#define DEVICE_FIELD "handle"
#define FACE_FIELD   "face"
#define NUM_F_FIELD  "numFace"

extern "C" JNIEXPORT jboolean JNICALL Java_narl_itrc_DevUSB_configure(
	JNIEnv * env,
	jobject thiz,
	jshort vid,
	jshort pid
) {
	libusb_init(NULL);

	libusb_device_handle* handle = libusb_open_device_with_vid_pid(NULL, vid, pid);
	if(handle==NULL){
		return JNI_FALSE;
	}

	int res = libusb_set_auto_detach_kernel_driver(handle,1);

	libusb_device* dev = libusb_get_device(handle);

	libusb_config_descriptor* conf;

	res = libusb_get_active_config_descriptor(dev, &conf);
	if(res!=0){
		printf("fail to get configure");
		return JNI_FALSE;
	}

	const libusb_interface* p_face = conf->interface;
	int numFace = conf->bNumInterfaces;
	/*
	for(int i=0; i<numFace; i++){

		const libusb_interface_descriptor* face = p_face[i].altsetting;

		res = libusb_claim_interface(handle, face->bInterfaceNumber);
		if(res!=0){
			printf("fail to claim interface[%d]",i);
		}
	}*/
	libusb_claim_interface(handle, 0);

	setJLong(env,thiz, DEVICE_FIELD, (long)handle);
	setJLong(env,thiz, FACE_FIELD  , (long)(p_face));
	setJInt (env,thiz, NUM_F_FIELD , (int )(numFace));
	return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevUSB_listEndpoint(
	JNIEnv * env,
	jobject thiz
) {

	int res = 0;

	libusb_device_handle* handle = (libusb_device_handle*)getJLong(env,thiz, DEVICE_FIELD);

	libusb_interface* p_face = (libusb_interface*)getJLong(env,thiz, FACE_FIELD);

	int numFace = getJInt(env,thiz, NUM_F_FIELD);

	jobjectArray j_array0 = env->NewObjectArray(
		numFace,
		env->FindClass("[J"),
		NULL
	);
	jobjectArray j_array1 = env->NewObjectArray(
		numFace,
		env->FindClass("[J"),
		NULL
	);

	for(int i=0; i<numFace; i++){

		const libusb_interface_descriptor* face = p_face[i].altsetting;

		int numPoint = face->bNumEndpoints;

		int numOut=0, numInt=0;
		jlong valOut[100]={0}, valInt[100]={0};

		for(int j=0; j<numPoint; j++){

			const libusb_endpoint_descriptor* end = (face->endpoint)+j;

			if( (end->bEndpointAddress & 0x80)==0 ){
				valOut[numOut] = (jlong)end;
				numOut+=1;
			}else{
				valInt[numInt] = (jlong)end;
				numInt+=1;
			}
		}

		jlongArray j_out = env->NewLongArray(numOut);
		jlongArray j_int = env->NewLongArray(numInt);
		env->SetLongArrayRegion(j_out, 0, numOut, valOut);
		env->SetLongArrayRegion(j_int, 0, numInt, valInt);
		env->SetObjectArrayElement(j_array0, i, j_out);
		env->SetObjectArrayElement(j_array1, i, j_int);
		env->DeleteLocalRef(j_out);
		env->DeleteLocalRef(j_int);
	}

	jclass _clazz = env->GetObjectClass(thiz);
	env->SetObjectField(
		thiz,
		env->GetFieldID(_clazz,"end0","[[J"),
		j_array0
	);
	env->SetObjectField(
		thiz,
		env->GetFieldID(_clazz,"end1","[[J"),
		j_array1
	);
}

extern "C" JNIEXPORT jint JNICALL Java_narl_itrc_DevUSB_bulkTransfer(
	JNIEnv * env,
	jobject thiz,
	jlong ptr,
	jbyteArray buffer,
	jint length,
	jint timeout
) {
	libusb_device_handle* handle = (libusb_device_handle*)getJLong(env,thiz, DEVICE_FIELD);

	libusb_endpoint_descriptor* end = (libusb_endpoint_descriptor*)ptr;

	jbyte* buf = env->GetByteArrayElements(buffer,NULL);

	size_t len = (length<=0)?(env->GetArrayLength(buffer)):(length);

	int transferred=0;

	int res = libusb_bulk_transfer(
		handle,
		end->bEndpointAddress,
		(uint8_t*)buf, len,
		&transferred,
		timeout
	);

	setJInt(env,thiz, "lastResult", res);
	if(transferred<len){
		buf[transferred] = 0;
	}
	env->ReleaseByteArrayElements(buffer, buf, 0);
	return transferred;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevUSB_release(
	JNIEnv * env,
	jobject thiz
) {
	libusb_device_handle* handle = (libusb_device_handle*)getJLong(env, thiz, DEVICE_FIELD);

	if(handle!=NULL){

		libusb_interface* p_face = (libusb_interface*)getJLong(env,thiz, FACE_FIELD);

		int numFace = getJInt(env, thiz, NUM_F_FIELD);
		/*for(int i=0; i<numFace; i++){
			const libusb_interface_descriptor* face = p_face[i].altsetting;
			libusb_release_interface(handle, face->bInterfaceNumber);
		}*/
		libusb_release_interface(handle, 0);
		libusb_close(handle);
		libusb_exit(NULL);
	}

	setJLong(env,thiz, DEVICE_FIELD, 0);
	setJLong(env,thiz, FACE_FIELD  , 0);
	setJInt (env,thiz, NUM_F_FIELD , 0);

	jclass _clazz = env->GetObjectClass(thiz);
	jfieldID id_end0 = env->GetFieldID(_clazz, "end0", "[[J");
	jfieldID id_end1 = env->GetFieldID(_clazz, "end1", "[[J");
	env->DeleteGlobalRef(env->GetObjectField(thiz, id_end0));
	env->DeleteGlobalRef(env->GetObjectField(thiz, id_end1));
	env->SetObjectField(thiz, id_end0, NULL);
}

