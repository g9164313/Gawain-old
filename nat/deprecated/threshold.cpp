/*
 * threshold.cpp
 *
 *  Created on: 2016年4月28日
 *      Author: qq
 */
#include <global.hpp>


/*int IJIsoData(int[] data) {
	// This is the original ImageJ IsoData implementation, here for backward compatibility.
	int level;
	int maxValue = data.length - 1;
	double result, sum1, sum2, sum3, sum4;
	int count0 = data[0];
	data[0] = 0;//set to zero so erased areas aren't included
	int countMax = data[maxValue];
	data[maxValue] = 0;
	int min = 0;
	while ((data[min]==0) && (min<maxValue))
	min++;
	int max = maxValue;
	while ((data[max]==0) && (max>0))
	max--;
	if (min>=max) {
		data[0]= count0; data[maxValue]=countMax;
		level = data.length/2;
		return level;
	}
	int movingIndex = min;
	int inc = Math.max(max/40, 1);
	do {
		sum1=sum2=sum3=sum4=0.0;
		for (int i=min; i<=movingIndex; i++) {
			sum1 += (double)i*data[i];
			sum2 += data[i];
		}
		for (int i=(movingIndex+1); i<=max; i++) {
			sum3 += (double)i*data[i];
			sum4 += data[i];
		}
		result = (sum1/sum2 + sum3/sum4)/2.0;
		movingIndex++;
	}while ((movingIndex+1)<=result && movingIndex<max-1);
	data[0]= count0; data[maxValue]=countMax;
	level = (int)Math.round(result);
	return level;
}

int defaultIsoData(int[] data) {
	// This is the modified IsoData method used by the "Threshold" widget in "Default" mode
	int n = data.length;
	int[] data2 = new int[n];
	int mode=0, maxCount=0;
	for (int i=0; i<n; i++) {
		int count = data[i];
		data2[i] = data[i];
		if (data2[i]>maxCount) {
			maxCount = data2[i];
			mode = i;
		}
	}
	int maxCount2 = 0;
	for (int i = 0; i<n; i++) {
		if ((data2[i]>maxCount2) && (i!=mode))
		maxCount2 = data2[i];
	}
	int hmax = maxCount;
	if ((hmax>(maxCount2*2)) && (maxCount2!=0)) {
		hmax = (int)(maxCount2 * 1.5);
		data2[mode] = hmax;
	}
	return IJIsoData(data2);
}*/

int Yen(Mat& data,double* valCrit) {
	/* normalized histogram */
	int histSize = 256;
	Mat norm_histo;
	calcHist(&data, 1, NULL, Mat(), norm_histo, 1, &histSize, 0);
	float total = norm(norm_histo, NORM_L1);
	norm_histo = norm_histo / total;
	double P1[256];/* cumulative normalized histogram */
	double P1_sq[256];
	double P2_sq[256];
	P1[0] = norm_histo.at<float>(0, 0);
	for (int ih = 1; ih < 256; ih++) {
		P1[ih] = P1[ih - 1] + norm_histo.at<float>(ih, 0);
	}
	P1_sq[0] = norm_histo.at<float>(0, 0) * norm_histo.at<float>(0, 0);
	for (int ih = 1; ih < 256; ih++) {
		P1_sq[ih] = P1_sq[ih - 1] + norm_histo.at<float>(ih, 0) * norm_histo.at<float>(ih, 0);
	}
	P2_sq[255] = 0.0;
	for (int ih = 254; ih >= 0; ih--) {
		P2_sq[ih] = P2_sq[ih + 1] + norm_histo.at<float>(ih + 1, 0) * norm_histo.at<float>(ih + 1, 0);
	}
	/* Find the threshold that maximizes the criterion */
	int threshold = -1;
	double crit;
	double max_crit = -DBL_MAX; //Double.MIN_VALUE;
	for (int it = 0; it < 256; it++) {
		double e1 =((P1_sq[it]*P2_sq[it])>0.0 ? log(P1_sq[it] * P2_sq[it]) : 0.0);
		double e2 =((P1[it]*(1.0-P1[it]))>0.0 ?	log(P1[it] * (1.0 - P1[it])) : 0.0);
		crit = -1.0*e1 + 2*e2;
		if (crit > max_crit) {
			max_crit = crit;
			threshold = it;
		}
	}
	if(valCrit!=NULL){
		(*valCrit) = max_crit;
	}
	return threshold;
}
//-----------------------//

class ThresStdDev : public ParallelLoopBody {
private:
	int rem,lv1,lv2;
	double thrs;
	Mat node;
	Mat* edg;
public:
	ThresStdDev(
		Mat& _node,Mat* _edg,
		double val,
		int _lv1, int _lv2,
		int _rem
	):node(_node),edg(_edg),
		thrs(val),
		lv1(_lv1),lv2(_lv2),
		rem(_rem)
	{
	}
	virtual void operator()(const Range &r) const {
		int ww =(edg->cols);
		for(int i=r.start; i<r.end; i++){
			int px = i%ww;
			int py = i/ww;
			Rect roi(px,py,rem*2+1,rem*2+1);
			Scalar avg,dev;
			meanStdDev(node(roi),avg,dev);
			if(avg[0]<lv1 || lv2<avg[0]){
				continue;
			}
			if(dev[0]<=thrs){
				edg->at<uint8_t>(py,px) = 255;
			}else{
				edg->at<uint8_t>(py,px) = 0;
			}
			//if(px%2==py%2){ edg->at<uint8_t>(py,px)=255; }
		}
	}
};

void thresholdSmooth(
	Mat& img,
	Mat& edg,
	double stdDev,
	int level1, int level2,
	int blockSize
){
	if(blockSize%2==0){
		blockSize+=1;
	}
	int rem = blockSize/2;
	Mat node(
		img.rows+rem*2+1,
		img.cols+rem*2+1,
		img.type()
	);
	copyMakeBorder(
		img,node,
		rem,rem+1,
		rem,rem+1,
		BORDER_REPLICATE
	);
	parallel_for_(
		Range(0,edg.cols*edg.rows),
		ThresStdDev(node,&edg,stdDev,level1,level2,rem)
	);
}

void thresholdHist(
	Mat& img,
	Mat& edg,
	Mat& hist,
	int width,int height
){
	Mat res(img.size(),CV_32FC1);
	Mat histImg;
	int histSize = hist.rows;
	for(int j=0; j<(img.rows-height); j++){
		for(int i=0; i<(img.cols-width); i++){
			Rect roi(i,j,width,height);
			Mat _img = img(roi);
			calcHist(&_img,1,NULL,Mat(),histImg,1,&histSize,NULL);
			double diff = compareHist(hist,histImg,CV_COMP_CORREL);
			res.at<float>(j,i) = diff;
		}
	}
	normalize(res,res,255,0,NORM_INF,CV_32F);
	imwrite("cc.0.png",res);
}
//-----------------------------------//

struct PointST {
	int x;
	int y;
	float SWT;
};

struct Ray {
	PointST p;
	PointST q;
	std::vector<PointST> points;
};

/*void _stroke_width_transform(Mat& edgeImage, Mat& gradientX, Mat& gradientY,
		bool dark_on_light, Mat& SWTImage, std::vector<Ray> & rays) {
	// First pass
	float prec = .05;
	for (int row = 0; row < edgeImage.rows; row++) {
		for (int col = 0; col < edgeImage.cols; col++) {
			if (edgeImage.at<uchar>(row, col) == 0) {
				continue;
			}
			Ray r;

			PointST p;
			p.x = col;
			p.y = row;
			r.p = p;
			vector<PointST> points;
			points.push_back(p);

			float curX = (float) col + 0.5;
			float curY = (float) row + 0.5;
			int curPixX = col;
			int curPixY = row;
			float G_x = gradientX.at<float>(row, col);
			float G_y = gradientY.at<float>(row, col);
			// normalize gradient
			float mag = sqrt((G_x * G_x) + (G_y * G_y));
			if (dark_on_light) {
				G_x = -G_x / mag;
				G_y = -G_y / mag;
			} else {
				G_x = G_x / mag;
				G_y = G_y / mag;
			}

			while (true) {
				curX += G_x * prec;
				curY += G_y * prec;
				if ((int) (floor(curX)) != curPixX
						|| (int) (floor(curY)) != curPixY) {
					curPixX = (int) (floor(curX));
					curPixY = (int) (floor(curY));
					// check if pixel is outside boundary of image
					if ((curPixX < 0) || (curPixX >= SWTImage.cols)
							|| (curPixY < 0) || (curPixY >= SWTImage.rows)) {
						break;
					}
					PointST pnew;
					pnew.x = curPixX;
					pnew.y = curPixY;
					points.push_back(pnew);

					if (edgeImage.at<uchar>(curPixY, curPixX) > 0) {
						r.q = pnew;
						// dot product
						float G_xt = gradientX.at<float>(curPixY, curPixX);
						float G_yt = gradientY.at<float>(curPixY, curPixX);
						mag = sqrt((G_xt * G_xt) + (G_yt * G_yt));
						if (dark_on_light) {
							G_xt = -G_xt / mag;
							G_yt = -G_yt / mag;
						} else {
							G_xt = G_xt / mag;
							G_yt = G_yt / mag;
						}

						if (acos(G_x * -G_xt + G_y * -G_yt) < M_PI / 2.0) {
							float length = hypot((float) r.q.x - (float) r.p.x,
									(float) r.q.y - (float) r.p.y);
							for (vector<PointST>::iterator pit = points.begin();
									pit != points.end(); pit++) {
								if (SWTImage.at<float>(pit->y, pit->x) < 0) {
									SWTImage.at<float>(pit->y, pit->x) = length;
								} else {
									SWTImage.at<float>(pit->y, pit->x) =
											std::min(length,
													SWTImage.at<float>(pit->y,
															pit->x));
								}
							}
							r.points = points;
							rays.push_back(r);
						}
						break;
					}
				}
			}
		}
	}
}*/

bool _stroke_width_sort(const PointST &lhs, const PointST &rhs) {
	return lhs.SWT < rhs.SWT;
}

/*void _stroke_width_filter(Mat& SWTImage, std::vector<Ray>& rays) {
	for (vector<Ray>::iterator rit = rays.begin(); rit != rays.end(); rit++) {
		for (vector<PointST>::iterator pit = rit->points.begin();
				pit != rit->points.end(); pit++) {
			pit->SWT = SWTImage.at<float>(pit->y, pit->x);
		}
		sort(rit->points.begin(), rit->points.end(), &_stroke_width_sort);
		float median = (rit->points[rit->points.size() / 2]).SWT;
		for (vector<PointST>::iterator pit = rit->points.begin();
				pit != rit->points.end(); pit++) {
			SWTImage.at<float>(pit->y, pit->x) = std::min(pit->SWT, median);
		}
	}
}*/

Mat strokeWidth(Mat& edge, bool dark_on_light, vector<Ray>& rays) {
	Mat img, gradx, grady;
	edge.convertTo(img, CV_32FC1, 1. / 255., 0);

	GaussianBlur(img, img, Size(5, 5), 0.);
	Scharr(img, gradx, -1, 1, 0);
	Scharr(img, grady, -1, 0, 1);
	medianBlur(gradx, gradx, 3);
	medianBlur(grady, grady, 3);

	Mat swt = Mat::ones(img.size(), CV_32FC1);
	swt = swt * -1.;
	//_stroke_width_transform(edge, gradx, grady, dark_on_light, swt, rays);
	//_stroke_width_filter(swt,rays);
	//dumpVal("cc.0.png",cc);
	return swt;
}




