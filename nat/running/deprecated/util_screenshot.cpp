#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <iostream>

#ifdef _MSC_VER

#include <Windows.h>
#include <atlimage.h>

using namespace std;
using namespace Gdiplus;

static void*  buffPtr = NULL;
static UINT32 buffLen = 0;

void cook2dib(HDC hMemDC, HBITMAP hBitmap, int width, int height) {

	BITMAPINFOHEADER   bi;
	bi.biSize = sizeof(BITMAPINFOHEADER);
	bi.biWidth = width;
	bi.biHeight= height;
	bi.biPlanes= 1;
	bi.biBitCount = 24;
	bi.biCompression = BI_RGB;
	bi.biSizeImage = 0;
	bi.biXPelsPerMeter = 0;
	bi.biYPelsPerMeter = 0;
	bi.biClrUsed = 0;
	bi.biClrImportant = 0;

	buffLen = ((width * bi.biBitCount + 31) / 32) * 4 * height;
	buffPtr = realloc(buffPtr, buffLen);

	// Gets the "bits" from the bitmap and copies them into a buffer 
	// which is pointed to by lpbitmap.
	GetDIBits(
		hMemDC,
		hBitmap,
		0, (UINT)height,
		buffPtr,
		(BITMAPINFO *)&bi,
		DIB_RGB_COLORS
	);//why not use 'StretchDIBits' ???
}

/*

//example from https://www.codeproject.com/Tips/738533/save-load-image-between-buffer

bool save_img(const CImage &image, vecByte &buf)
{
	IStream *stream = NULL;
	HRESULT hr = CreateStreamOnHGlobal(0, TRUE, &stream);
	if (!SUCCEEDED(hr))
		return false;
	image.Save(stream, Gdiplus::ImageFormatBMP);
	ULARGE_INTEGER liSize;
	IStream_Size(stream, &liSize);
	DWORD len = liSize.LowPart;
	IStream_Reset(stream);
	buf.resize(len);
	IStream_Read(stream, &buf[0], len);
	stream->Release();
	return true;
}

bool load_img(const vecByte &buf, CImage &image)
{
	UINT len = buf.size();
	HGLOBAL hMem = GlobalAlloc(GMEM_FIXED, len);
	BYTE *pmem = (BYTE*)GlobalLock(hMem);
	memcpy(pmem, &buf[0], len);
	IStream    *stream = NULL;
	CreateStreamOnHGlobal(hMem, FALSE, &stream);
	image.Load(stream);
	stream->Release();
	GlobalUnlock(hMem);
	GlobalFree(hMem);
	return true;
}
*/

void cook2png(HBITMAP hBitmap) {
	IStream* stm;
	HRESULT hr = CreateStreamOnHGlobal(0, TRUE, &stm);
	if (!SUCCEEDED(hr)){
		return;
	}
	CImage img;
	img.Attach(hBitmap);
	img.Save(stm, ImageFormatPNG);

	ULARGE_INTEGER liSize;
	IStream_Size(stm, &liSize);
	IStream_Reset(stm);
	
	buffLen = liSize.LowPart;
	buffPtr = realloc(buffPtr, buffLen);

	IStream_Read(stm, buffPtr, buffLen);
	stm->Release();
}

__declspec(dllexport) void* screenshot(
	INT32* sw, INT32* sh,
	INT32* len,
	const char mode
){
	HDC hScreenDC = CreateDC(
		L"DISPLAY", 
		NULL, 
		NULL, 
		NULL
	);

	// and a device context to put it in
	HDC hMemoryDC = CreateCompatibleDC(hScreenDC);
	int width = GetDeviceCaps(hScreenDC, HORZRES);
	int height= GetDeviceCaps(hScreenDC, VERTRES);
	if (sw != NULL) { *sw = width; }
	if (sh != NULL) { *sh = height; }

	// maybe worth checking these are positive values
	HBITMAP hBitmap = CreateCompatibleBitmap(
		hScreenDC, 
		width, height
	);

	// get a new bitmap
	HBITMAP hOldBitmap = (HBITMAP)SelectObject(hMemoryDC, hBitmap);

	switch (mode) {
	case 'd':
	case 'D':
		StretchBlt(
			hMemoryDC,
			0, height,
			width, -height,
			hScreenDC,
			0, 0,
			width, height,
			SRCCOPY
		);
		cook2dib(hMemoryDC,hBitmap,width,height);
		break;
	case 'p':
	case 'P':
		BitBlt(
			hMemoryDC,
			0, 0,
			width, height,
			hScreenDC,
			0, 0,
			SRCCOPY
		);
		cook2png(hBitmap);
		break;
	}

	//redraw the original bitmap
	hBitmap = (HBITMAP)SelectObject(hMemoryDC, hOldBitmap);

	// clean up
	DeleteDC(hMemoryDC);
	DeleteDC(hScreenDC);
	DeleteObject(hBitmap);
	if (len != NULL) {
		*len = buffLen;
	}
	return buffPtr;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_narl_itrc_Misc_screenshot2png(
	JNIEnv* env,
	jobject thiz,
	jintArray j_info
) {
	//old method, it may cause GC frequently
	/*vector<uchar> buf;
	imencode(ext, img, buf);*/

	jint* info = env->GetIntArrayElements(j_info, NULL);

	INT32 sw, sh;
	screenshot(&sw,&sh,NULL,'p');
	
	info[0] = sw;
	info[1] = sh;
	env->ReleaseIntArrayElements(j_info, info, 0);

	jbyteArray arr = env->NewByteArray(buffLen);
	env->SetByteArrayRegion(
		arr,
		0, buffLen,
		(jbyte*)buffPtr
	);
	return arr;
}

extern "C" JNIEXPORT void JNICALL Java_narl_itrc_Misc_deleteScreenshot(
	JNIEnv* env,
	jobject thiz,
	jbyteArray j_buf
){
	if (j_buf == NULL) {
		free(buffPtr);
		buffPtr = NULL;
		buffLen = 0;
		//cout << "release Screen buffer" << endl;
	}else {
		env->DeleteLocalRef(j_buf);
	}	
}

#endif


