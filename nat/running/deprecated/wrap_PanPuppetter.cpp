/*
 * wrap_PanPuppetter.cpp
 *
 *  Created on: 2018年1月2日
 *      Author: qq
 */
#include <global.hpp>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
using namespace cv;
using namespace std;

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_PanPuppeteer_recognizeText(
	JNIEnv * env,
	jobject thiz,
	jbyteArray jdata,
	int width, int height
){
	jbyte* data = env->GetByteArrayElements(jdata, 0);

	Mat img(height, width, CV_8UC4, (uint8_t*)data);

	imwrite("test.png",img);

	env->ReleaseByteArrayElements(jdata, data, 0);
}

