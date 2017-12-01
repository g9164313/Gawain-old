#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <iostream>

#ifdef _MSC_VER
#include <Windows.h>
#include <atlimage.h>
#endif

using namespace std;

#ifdef _MSC_VER

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_sendMouseClick(
	JNIEnv* env,
	jobject thiz,
	jint pos_x,
	jint pos_y
) {
	INPUT in;
	in.type = INPUT_MOUSE;
	in.mi.dx = (65536 / GetSystemMetrics(SM_CXSCREEN)) * pos_x;
	in.mi.dy = (65536 / GetSystemMetrics(SM_CYSCREEN)) * pos_y;
	in.mi.dwFlags = (
		MOUSEEVENTF_ABSOLUTE | 
		MOUSEEVENTF_MOVE | 
		MOUSEEVENTF_LEFTDOWN | 
		MOUSEEVENTF_LEFTUP
	);
	in.mi.mouseData = 0;
	in.mi.dwExtraInfo = NULL;
	in.mi.time = 0;
	SendInput(1, &in, sizeof(INPUT));
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_getCursorPos(
	JNIEnv* env,
	jobject thiz,
	jintArray j_info
) {
	jint* info = env->GetIntArrayElements(j_info, NULL);
	POINT pos;
	GetCursorPos(&pos);
	info[0] = pos.x;
	info[1] = pos.y;
	env->ReleaseIntArrayElements(j_info, info, 0);
}

#endif