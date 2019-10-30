/*
 * wrap_sfr_iso.cpp
 *
 *  Created on: 2016年11月29日
 *      Author: qq
 */
#include <global.hpp>
#include "../vision/CamBundle.hpp"

/**
* Data passed to this function is assumed to be radiometrically corrected,
* and oriented vertically, with black on left, white on right. The black to
* white orientation doesn't appear to be terribly important in this code.
* Radiometric correct data (or at least 0-1 scaled) is important for the
* contrast check.
*     Parameters:
* Input:
*   farea  = radiometric image data in ROI
*   size_x = number of columns in ROI
*   nrows  = number of rows in ROI
* Output:
*   freq   = new array of relative spatial frequencies
*   sfr    = new array of computed SFR at each freq
*   len    = length of freq & sfr arrays
*   slope  = estimated slope value
*   numcycles = number of full periods included in SFR
*   pcnt2  = location of edge mid-point in oversample space
*   off    = shift to center edge in original rows
*   R2     = linear edge fit
*   version = 0 = default ([-1 1] deriv, no rounding, no peak)
*             1 = add rounding
*             2 = add peak
*             4 = [-1 0 1] derivative, rounding, no peak
*   iterate = 0 do just a single run
*             1 means more runs after this, don't change 'farea'
*             and let numcycles go as low as 1.0
*   user_angle = flag to indicate if the line fit has been precomputed
*
* farea  = size_x*4 of ESF and len*2 of LSF values
* (if iterate = 0)
 */
extern short sfrProc(
	double *farea, unsigned short size_x, int *nrows,
	double **freq,
	double **sfr,
	int *len,
	double *slope, int *numcycles,
	int *pcnt2,
	double *off,
	double *R2,
	int version,
	int iterate,
	int user_angle
);

FILE *g_mtfout = NULL;//just for logger file~~

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_FltrSlangEdge_implSfrProc(
	JNIEnv* env,
	jobject thiz,
	jobject bundle,
	jintArray mark
){
	MACRO_PREPARE

	Mat src(height,width,type,buff);
	Mat img = checkMono(env,mark,src);

	Mat dat;
	img.convertTo(dat,CV_64FC1);

	double *gray_v;
	int size_x, size_y;

	double *Freq = NULL;
	double *Disp = NULL;
	int bin_len;
	double slope;
	int numcycles = 0;
	int center;
	double off, R2;
	int g_version = 0;

	gray_v = (double*)dat.ptr();
	size_x = dat.cols;
	size_y = dat.rows;

	short err = sfrProc(
		gray_v, size_x, &size_y,
		&Freq,
		&Disp,
		&bin_len,
		&slope, &numcycles,
		&center,/* location of edge mid-point */
		&off,   /* shift to center edge */
		&R2 /* linear edge fit */,
		g_version, /* version */
		0, /* iterate */
		0  /* provide user angle */
	);
	if(err!=0){
		//we have problem~~~~
		return;
	}

	double scale = getJDouble(env,thiz,"pix_mm");// pixel per millimeter

	setJDouble(env,thiz,"slopeDegree",(atan(slope)*(double)(180.0/M_PI)));
	setJInt(env,thiz,"numOfCycles"   ,numcycles);
	setJInt(env,thiz,"numOfLeftSide" ,size_x/2+off);
	setJInt(env,thiz,"numOfRightSide",size_x/2-off);
	setJDouble(env,thiz,"fitRatio",R2);

	bool chkFrq = false, chkSfr = false;

	vector<double> vecFrq,vecSfr;
	for(int i = 0; i<bin_len/2; i++) {
		double frq, sfr;
		double freq, fd_scale;

		freq = M_PI * Freq[i];

		if (g_version & 4){
			freq /= 2.0; /* [-1 0 1] */
		}else{
			freq /= 4.0; /* [-1 1] */
		}

		if (freq == 0.0){
			fd_scale = 1.0;
		}else{
			fd_scale = freq / sin(freq);
		}

		frq = Freq[i] * scale;
		sfr = Disp[i] * fd_scale;

		vecFrq.push_back(frq);
		vecSfr.push_back(sfr);

		if(Freq[i]>=0.5 && chkFrq==false){
			setJInt(env,thiz,"idxFrqOver",i);
			chkFrq = true;
		}
		if(sfr<=0.5 && chkSfr==false){
			setJInt(env,thiz,"idxSfrLess",i);
			chkSfr = true;
		}
	}

	//stuff all information~~~
	setDoubleArray(
		env,thiz,
		"frq",
		&vecFrq[0], vecFrq.size()
	);
	setDoubleArray(
		env,thiz,
		"sfr",
		&vecSfr[0], vecSfr.size()
	);
	free(Freq);
	free(Disp);
}

extern int test_sfr(
	Mat& img,
	double scale,
	vector<double>& frq,
	vector<double>& sfr
){

	Mat dat;
	img.convertTo(dat,CV_64FC1);

	double *gray_v;
	int size_x, size_y;

	double *Freq = NULL;
	double *Disp = NULL;
	int bin_len;
	double slope;
	int numcycles = 0;
	int center;
	double off, R2;
	int g_version = 0;

	gray_v = (double*)dat.ptr();
	size_x = dat.cols;
	size_y = dat.rows;

	short err = sfrProc(
		gray_v, size_x, &size_y,
		&Freq,
		&Disp,
		&bin_len,
		&slope, &numcycles,
		&center,/* location of edge mid-point */
		&off,   /* shift to center edge */
		&R2 /* linear edge fit */,
		g_version, /* version */
		0, /* iterate */
		0  /* provide user angle */
	);

	//double angle = atan(slope) * 180 / M_PI;
	//cout<<"angle="<<angle<<", cycle="<<numcycles<<endl;

	frq.clear();
	sfr.clear();

	int idxMTF50 = 0;
	for(int i = 0; i<bin_len/2; i++) {
		double v_frq, v_sfr;
		double freq, fd_scale=1.;

		freq = M_PI * Freq[i];

		if (g_version & 4){
			freq /= 2.0; /* [-1 0 1] */
		}else{
			freq /= 4.0; /* [-1 1] */
		}

		if (freq == 0.0){
			fd_scale = 1.0;
		}else{
			fd_scale = freq / sin(freq);
		}

		v_frq = Freq[i] * scale;
		v_sfr = Disp[i] * fd_scale;

		frq.push_back(v_frq);
		sfr.push_back(v_sfr);

		if(v_sfr<=0.5 && idxMTF50==0){
			idxMTF50 = i-1;
		}
	}
	return idxMTF50;
}



