/*
 * FilterLetterpress.cpp
 *
 *  Created on: 2016年10月3日
 *      Author: qq
 */
#include <vision.hpp>
#include <CamBundle.hpp>

extern "C" JNIEXPORT void JNICALL Java_prj_letterpress_WidAoiViews_implCookData(
	JNIEnv* env,
	jobject thiz /* refer to 'prj.letterpress.WidAoiViews' */,
	jobject bundle /* refer to 'CamBundle' */,
	jlong ptrMatx
){
	MACRO_PREPARE
	if(buff==NULL){
		return;
	}
	Mat src(height,width,type,buff);
	Mat img = checkMono(src);
	Mat ova = Mat::zeros(img.size(),CV_8UC4);

	Mat edg;
	Canny(img,edg,5000,7000,7,true);
	drawEdge(ova,edg);

	MACRO_SET_IMG_INFO(ova);
}

