#include <global.hpp>

void shift_vertex(Point2f vtx[4]){
	int idx=0;
	double len = HUGE_VAL;
	for(int i=0; i<4; i++){
		double v = hypot(vtx[i].x,vtx[i].y);
		if(v<len){
			idx = i;
			len = v;
		}
	}
	Point2f tmp[4];
	int pos=0;
	while(pos<4){
		tmp[pos] = vtx[idx];
		pos++;
		idx++;
		idx = idx % 4;
	}
	vtx[0] = tmp[0];
	vtx[2] = tmp[2];
	//test clock-wise
	if(tmp[1].x<tmp[3].x){
		vtx[1] = tmp[3];
		vtx[3] = tmp[1];
	}else{
		vtx[1] = tmp[1];
		vtx[3] = tmp[3];
	}
}

vector<Point>& find_biggest(vector<vector<Point> >& cts){
	size_t indx=0;
	float area = 0.f;
	for(size_t i=0; i<cts.size(); i++){
		/*RotatedRect obj = minAreaRect(cts[i]);
		float v = obj.size.width * obj.size.height;
		if(v>=area){
			indx=i;
			area=v;
		}*/
		vector<Point>& cc = cts[i];
		float v = contourArea(cc);
		if(v>=area){
			indx=i;
			area=v;
		}
	}
	return cts[indx];
}

void removeNoise(Mat& msk,int* board){
	msk=msk-128;//remove shadow~~~~
	const int kern_size = 3;
	Mat kern = getStructuringElement(
		MORPH_ELLIPSE,
		Size(2*kern_size+1,2*kern_size+1),
		Point(kern_size,kern_size)
	);
	erode(msk,msk,kern);//remove salt-and-pepper noise
	dilate(msk,msk,kern);
	if(board==NULL){
		return;
	}
	//skip data along boards - top, bottom, left, right
	if(board[0]>0){
		msk(Rect(0,0,msk.cols,board[0]))=0;
	}
	if(board[1]>0){
		msk(Rect(0,msk.rows-board[1],msk.cols,board[1]))=0;
	}
	if(board[2]>0){
		msk(Rect(0,0,board[2],msk.rows))=0;
	}
	if(board[3]>0){
		msk(Rect(msk.cols-board[3],0,board[3],msk.rows))=0;
	}
}

Mat cutOutBounding(Mat& img,Mat& msk,int width,int height){

	removeNoise(msk,NULL);

	//step.1 find a possible zone
	vector<vector<Point> > cts;
	Mat tmp;
	msk.copyTo(tmp);
	findContours(tmp,cts,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
	if(cts.size()==0){
		return Mat();//WTF???
	}
	tmp.release();


	vector<Point>& _cts = find_biggest(cts);
	vector<Point> cts0;
	RotatedRect obj = minAreaRect(_cts);
	float mod1 = std::min(obj.size.width,obj.size.height);
	float mod2 = std::max(obj.size.width,obj.size.height);
	if((mod1/mod2)>0.3){
		approxPolyDP(_cts, cts0, arcLength(_cts,true)*0.02, true);
	}else{
		approxPolyDP(_cts, cts0, mod1/3., true);
	}
	//step.2 wrap rectangle~~~
	obj = minAreaRect(cts0);
	Point2f vtx[4];
	obj.points(vtx);
	shift_vertex(vtx);

	float ow = hypot2f(vtx[0],vtx[1]);
	float oh = hypot2f(vtx[0],vtx[3]);
	Point2f dir1[3] = {
		vtx[0],
		vtx[1],
		vtx[3]
	};
	Point2f dir2[3] = {
		Point2f(0,0),
		Point2f(ow,0),
		Point2f(0,oh)
	};
	warpAffine(
		img,tmp,
		getAffineTransform(dir1,dir2),
		Size(
			(width >0)?(width ):(ow),
			(height>0)?(height):(oh)
		)
	);
	return tmp;
}

void releasePtrMod(
	JNIEnv * env,
	jobject thiz,
	jfieldID fid
){
	jclass clzz=env->GetObjectClass(thiz);
	jlong ptr;
	ptr=env->GetLongField(thiz,fid);
	if(ptr==0L){
		return;
	}
	delete ((Ptr<BackgroundSubtractorMOG2>*)ptr);
	env->SetLongField(thiz,fid,0L);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_TrialMOG_modDump(
	JNIEnv * env,
	jobject thiz,
	jlong ptrMod,
	jstring jDstName
){
	if(ptrMod==0L){
		return;
	}
	Ptr<BackgroundSubtractorMOG2>& mod = *((Ptr<BackgroundSubtractorMOG2>*)ptrMod);
	Mat img;
	mod->getBackgroundImage(img);
	char name[500];
	jstrcpy(env,jDstName,name);
	imwrite(name,img);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_TrialMOG_modUpdateSize(
	JNIEnv * env,
	jobject thiz,
	jstring jname,
	jintArray jObjInfo
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat obj = imread(name);
	jint* info = env->GetIntArrayElements(jObjInfo,NULL);
	info[0] = obj.cols;
	info[1] = obj.rows;
	env->ReleaseIntArrayElements(jObjInfo,info,0);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_TrialMOG_modAdminPtr(
	JNIEnv * env,
	jobject thiz,
	jchar tkn,
	jlong hist,
	jdouble varThres,
	jboolean detShadow
){
	jclass clzz=env->GetObjectClass(thiz);
	jfieldID fid;
	jlong ptr;
	if(tkn=='b'){
		fid=env->GetFieldID(clzz,"ptrModBack","J");
	}else if(tkn=='f'){
		fid=env->GetFieldID(clzz,"ptrModFore","J");
	}else{
		releasePtrMod(env,thiz,env->GetFieldID(clzz,"ptrModBack","J"));
		releasePtrMod(env,thiz,env->GetFieldID(clzz,"ptrModFore","J"));
		return;
	}
	ptr=env->GetLongField(thiz,fid);
	if(ptr!=0L){
		releasePtrMod(env,thiz,fid);
	}
	Ptr<BackgroundSubtractorMOG2>* tmp = new Ptr<BackgroundSubtractorMOG2>();
	*tmp = createBackgroundSubtractorMOG2(hist,varThres,detShadow);
	ptr = (jlong)(tmp);
	env->SetLongField(thiz,fid,ptr);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_TrialMOG_modApplyFile(
	JNIEnv * env,
	jobject thiz,
	jlong ptrMod,
	jstring jImgName,
	jstring jMskName,
	jdouble learn
){
	if(ptrMod==0L){
		return;
	}
	char imgName[500],mskName[500];
	jstrcpy(env,jImgName,imgName);
	jstrcpy(env,jMskName,mskName);
	Ptr<BackgroundSubtractorMOG2>& mod = *((Ptr<BackgroundSubtractorMOG2>*)ptrMod);
	//Mat tmp = imread(name,IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
	Mat img = imread(imgName,IMREAD_GRAYSCALE);//?? why we can't use IMREAD_ANYDEPTH ??
	Mat msk(img.size(),CV_8UC1);
	mod->apply(img,msk,learn);
	if(mskName[0]!=0){
		imwrite(mskName,msk);
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_TrialMOG_cutOutBounding(
	JNIEnv * env,
	jobject thiz,
	jlong ptrSrcImage,
	jstring jSrcName,
	jlong ptrDstImage,
	jstring jDstName
){
	jclass clzz=env->GetObjectClass(thiz);
	jfieldID fid=env->GetFieldID(clzz,"ptrModBack","J");

	jlong ptr=env->GetLongField(thiz,fid);
	if(ptr==0L){
		return;
	}

	Mat img;
	if(ptrSrcImage==0L){
		//take image from file~~~
		char name[500];
		jstrcpy(env,jSrcName,name);
		img = imread(name,IMREAD_GRAYSCALE);
		if(img.empty()==true){
			return;
		}
	}else{
		//take image from memory~~~
		img = *((Mat*)ptrSrcImage);
	}

	Ptr<BackgroundSubtractorMOG2>& mod = *((Ptr<BackgroundSubtractorMOG2>*)ptr);
	Mat msk(img.size(),CV_8UC1);
	mod->apply(img,msk,0.);

	fid=env->GetFieldID(clzz,"objInfo","[I");
	jobject objInfo = env->GetObjectField(thiz,fid);
	jintArray* arrInfo = reinterpret_cast<jintArray*>(&objInfo);

	jint* info = env->GetIntArrayElements(*arrInfo,NULL);

	Mat obj = cutOutBounding(img,msk,info[0],info[1]);

	if(info[0]<0 && info[1]<0 && obj.empty()==false){
		info[2] = info[2] + obj.cols;
		info[3] = info[3] + obj.rows;
		info[4]++;
	}

	env->ReleaseIntArrayElements(*arrInfo,info,0);

	if(ptrDstImage!=0L){
		//dump to memory
		img = *((Mat*)ptrDstImage);
		if(obj.empty()==true){
			//TODO: show something~~~
			return;
		}else{
			obj.copyTo(img(Rect(0,0,obj.cols,obj.rows)));
		}
	}
	if(jDstName!=NULL){
		char name[500];
		jstrcpy(env,jDstName,name);
		imwrite(name,obj);
	}
}

extern "C" JNIEXPORT jboolean JNICALL Java_prj_daemon_TrialMOG_modMeasurement(
	JNIEnv * env,
	jobject thiz,
	jlong ptrModBack,
	jlong ptrModFore,
	jint objWidth,
	jint objHeight,
	jlong ptrSrcImage,jstring jSrcName,
	jlong ptrDstImage,jstring jObjName
){
	Ptr<BackgroundSubtractorMOG2>& modBack = *((Ptr<BackgroundSubtractorMOG2>*)ptrModBack);
	Ptr<BackgroundSubtractorMOG2>& modFore = *((Ptr<BackgroundSubtractorMOG2>*)ptrModFore);

	Mat img = *((Mat*)ptrSrcImage);

	int64 t1 = getTickCount();

	Mat msk1(img.size(),CV_8UC1);
	modBack->apply(img,msk1,0);

	Mat obj = cutOutBounding(img,msk1,objWidth,objHeight);
	if(obj.empty()==true){
		return JNI_FALSE;
	}

	Mat msk2(obj.size(),CV_8UC1);
	modFore->apply(obj,msk2,0);

	double dff = getTickFrequency()/(double)(getTickCount()-t1);
	cout<<"time="<<dff<<endl;

	int skip[]={10,10,10,10};
	removeNoise(msk2,skip);

	if(jSrcName!=NULL){
		char name[500];
		jstrcpy(env,jSrcName,name);
		imwrite(name,img);
	}
	jboolean flag = JNI_TRUE;
	int cnt=countNonZero(msk2);
	Mat nod;
	if(obj.type()==CV_8UC1){
		cvtColor(obj,nod,COLOR_GRAY2BGR);
	}else{
		nod = obj;
	}
	if(cnt!=0){
		flag = JNI_FALSE;
		vector<vector<Point> > cts;
		findContours(msk2,cts,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
		drawContours(nod,cts,-1,Scalar(0,0,255));
	}
	if(jObjName!=NULL){
		char name[500];
		jstrcpy(env,jObjName,name);
		imwrite(name,nod);
	}
	if(ptrDstImage!=0L){
		//display result~~~
		img = *((Mat*)ptrDstImage);
		obj.copyTo(img(Rect(0,0,objWidth,objHeight)));
	}
	return flag;
}




