#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <iostream>

#ifdef _MSC_VER
#include <Windows.h>
#include <atlimage.h>
#endif

using namespace std;

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_writeRGBA(
	JNIEnv* env,
	jobject thiz,
	jstring name,
	jbyteArray buff,
	jint width,
	jint height,
	jintArray roi
) {

	jint* info = env->GetIntArrayElements(j_info, NULL);
	env->ReleaseIntArrayElements(j_info, info, 0);
}

#ifdef _MSC_VER

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_sendMouseClick(
	JNIEnv* env,
	jobject thiz,
	jint pos_x,
	jint pos_y
) {
	INPUT input;
	input.type = INPUT_MOUSE;	
	input.mi.dwFlags = (
		MOUSEEVENTF_ABSOLUTE | 
		MOUSEEVENTF_MOVE | 
		MOUSEEVENTF_LEFTDOWN | 
		MOUSEEVENTF_LEFTUP
	);
	input.mi.dx = (65536 / GetSystemMetrics(SM_CXSCREEN)) * pos_x;
	input.mi.dy = (65536 / GetSystemMetrics(SM_CYSCREEN)) * pos_y;
	input.mi.mouseData = 0;
	input.mi.dwExtraInfo = NULL;
	input.mi.time = 0;
	SendInput(1, &input, sizeof(INPUT));
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_sendKeyboardText(
	JNIEnv* env,
	jobject thiz,
	jstring text
) {
	const jchar* txt = env->GetStringChars(text, NULL);

	jsize len = env->GetStringLength(text);	

	INPUT* lstInput = new INPUT[len*2];
	
	ZeroMemory(lstInput, sizeof(INPUT) * len*2);

	for (jsize idx=0; idx<len; idx++) {
		//WORD cc = MapVirtualKey(dat[idx], MAPVK_VK_TO_VSC);
		//WORD cc = VkKeyScanEx(txt[idx], 0);

		lstInput[idx * 2 + 0].type = INPUT_KEYBOARD;
		lstInput[idx * 2 + 0].ki.dwFlags = KEYEVENTF_UNICODE;
		lstInput[idx * 2 + 0].ki.wScan = txt[idx];

		lstInput[idx * 2 + 1].type = INPUT_KEYBOARD;
		lstInput[idx * 2 + 1].ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP;
		lstInput[idx * 2 + 1].ki.wScan = txt[idx];
	}
	
	env->ReleaseStringChars(text, txt);

	SendInput(len*2, lstInput, sizeof(INPUT));

	delete[] lstInput;
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
