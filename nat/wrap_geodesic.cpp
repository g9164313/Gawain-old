/*
 * wrap_kinship.cpp
 *
 *  Created on: 2015/11/18
 *      Author: qq
 */
#include <global.hpp>

#define MAX_MOD 10
#define NAME_PIC "model.png"
#define NAME_XML "model.xml"

extern void cutter(Mat& src,Mat& dst,int dw,int dh);

static Ptr<BackgroundSubtractorMOG2> mod[MAX_MOD];
static Mat msk[MAX_MOD];

void check_size(Mat& aa,Mat& bb){
	if(
		aa.cols==bb.cols ||
		aa.rows==bb.rows
	){
		return;
	}
	bb.release();
	bb = Mat(aa.size(),aa.type());
}

extern void toColor(Mat& src,Mat& dst);

void side_by_side(Mat& dst,Mat& src1,Mat& src2){
	Mat s1,s2;
	toColor(src1,s1);
	toColor(src2,s2);
	if(s1.empty()==true || s2.empty()==true){
		return;
	}
	int hh = std::max(s1.rows,s2.rows);
	dst = Mat(
		hh,
		s1.cols+s2.cols,
		CV_8UC3
	);
	s1.copyTo(dst(Rect(0,0,s1.cols,s1.rows)));
	s2.copyTo(dst(Rect(s1.cols,0,s2.cols,s2.rows)));
}

void append_path(char* dst,const char* path,const char* name){
	if(path[0]!=0){
		sprintf(dst,"%s%s",path,name);
	}else{
		strcpy(dst,name);
	}
}

void save_model(const char* path,Ptr<BackgroundSubtractorMOG2>& mm){
	char namePic[500]={0};
	char nameXml[500]={0};
	append_path(namePic,path,NAME_PIC);
	append_path(nameXml,path,NAME_XML);

	Mat obj;
	mm->getBackgroundImage(obj);
	imwrite(namePic,obj);

	FileStorage fs(nameXml,FileStorage::WRITE);
	fs<<"type"<<obj.type();
	fs<<"width"<<obj.cols;
	fs<<"height"<<obj.rows;
	fs<<"BackgroundRatio"<< mm->getBackgroundRatio();
	fs<<"ComplexityReductionThreshold"<< mm->getComplexityReductionThreshold();
	fs<<"DetectShadows"<< mm->getDetectShadows();
	fs<<"History"<< mm->getHistory();
	fs<<"NMixtures"<< mm->getNMixtures();
	fs<<"ShadowThreshold"<< mm->getShadowThreshold();
	fs<<"ShadowValue"<< mm->getShadowValue();
	fs<<"VarInit"<< mm->getVarInit();
	fs<<"VarMax"<< mm->getVarMax();
	fs<<"VarMin"<< mm->getVarMin();
	fs<<"VarThreshold"<< mm->getVarThreshold();
	fs<<"VarThresholdGen"<< mm->getVarThresholdGen();
	fs.release();
}

void load_model(const char* path,Ptr<BackgroundSubtractorMOG2>& mm,Mat& mk){
	char nameXml[500]={0};
	append_path(nameXml,path,NAME_XML);
	mk.release();

	int type,width,height;
	double BackgroundRatio;
	double ComplexityReductionThreshold;
	bool DetectShadows;
	int History;
	int NMixtures;
	double ShadowThreshold;
	int ShadowValue;
	double VarInit,VarMax,VarMin;
	double VarThreshold;
	double VarThresholdGen;

	FileStorage fs(nameXml,FileStorage::READ);
	fs["type"]>>type;
	fs["width"]>>width;
	fs["height"]>>height;
	fs["BackgroundRatio"]>> BackgroundRatio;
	fs["ComplexityReductionThreshold"]>> ComplexityReductionThreshold;
	fs["DetectShadows"]>> DetectShadows;
	fs["History"]>> History;
	fs["NMixtures"]>> NMixtures;
	fs["ShadowThreshold"]>> ShadowThreshold;
	fs["ShadowValue"]>> ShadowValue;
	fs["VarInit"]>> VarInit;
	fs["VarMax"]>> VarMax;
	fs["VarMin"]>> VarMin;
	fs["VarThreshold"]>> VarThreshold;
	fs["VarThresholdGen"]>> VarThresholdGen;
	fs.release();

	mk = Mat(height,width,type);
	mm->setBackgroundRatio(BackgroundRatio);
	mm->setComplexityReductionThreshold(ComplexityReductionThreshold);
	mm->setDetectShadows(DetectShadows);
	mm->setHistory(History);
	mm->setNMixtures(NMixtures);
	mm->setShadowThreshold(ShadowThreshold);
	mm->setShadowValue(ShadowValue);
	mm->setVarInit(VarInit);
	mm->setVarMax(VarMax);
	mm->setVarMin(VarMin);
	mm->setVarThreshold(VarThreshold);
	mm->setVarThresholdGen(VarThresholdGen);
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_panGeodesic_modApply(
	JNIEnv * env,
	jobject thiz,
	jint jid,
	jlongArray jhand,
	jstring jname,
	jint jstep
){
	if(jid>=MAX_MOD){
		return;
	}

	Mat img,tmp;
	jlong hand[6];
	char name[500]={0};

	if(jhand!=NULL){
		env->GetLongArrayRegion(jhand,0,6,hand);
		if(hand[0]==0||hand[1]==0){
			return;
		}
		switch(hand[4]){
		case 3:
			hand[4] = CV_8UC3;
			break;
		default:
		case 1:
			hand[4] = CV_8UC1;
			break;
		}
		img = Mat(hand[3],hand[2],hand[4],(void*)hand[1]);
	}

	switch(jstep){
	case 1://cut data & find size
		if(img.empty()==true){
			cerr<<"no source image!!"<<endl;
			return;
		}
		msk[jid].release();
		cutter(
			img,msk[jid],
			-1,-1
		);
		if(jname!=NULL){
			jstrcpy(env,jname,name);
			imwrite(name,msk[jid]);
		}
		break;

	case 20://train file
		if(jname==NULL){
			cerr<<"no file to train"<<endl;
			break;
		}
		jstrcpy(env,jname,name);
		tmp = imread(name,IMREAD_ANYDEPTH|IMREAD_GRAYSCALE);
		mod[jid]->apply(tmp,msk[jid]);
		break;
	case 21://cut data, then train it
		if(img.empty()==true){
			cerr<<"no source image!!"<<endl;
			return;
		}
		cutter(
			img,tmp,
			msk[jid].cols,
			msk[jid].rows
		);
		if(jname!=NULL){
			jstrcpy(env,jname,name);
			imwrite(name,tmp);
		}
		mod[jid]->apply(tmp,msk[jid]);
		break;

	case 3://test sample
		if(img.empty()==true){
			cerr<<"no source image!!"<<endl;
			return;
		}
		cutter(
			img,tmp,
			msk[jid].cols,
			msk[jid].rows
		);
		mod[jid]->apply(tmp,msk[jid],0);
		if(jname!=NULL){
			jstrcpy(env,jname,name);
			Mat pic;
			side_by_side(pic,tmp,msk[jid]);
			if(pic.empty()==true){
				return;
			}
			if(strcmp(name,":show:")==0){

			}else{
				imwrite(name,pic);
			}
		}
		break;
	}
	return;
}

#define PARM_SWITCH(type,attr) \
if(jflag==JNI_TRUE){ \
	mod[jid]->set##attr(getJ##type(env,thiz,name)); \
}else{ \
	setJ##type(env,thiz,name,mod[jid]->get##attr());\
}

#define PARM_SWITCH2(type) \
	type val = 3.3;

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_panGeodesic_modParm(
	JNIEnv * env,
	jobject thiz,
	jint jid,
	jstring jname,
	jstring jattr,
	jboolean jflag
){
	if(jid>=MAX_MOD){
		return;
	}
	char name[500]={0};
	char attr[500]={0};

	jstrcpy(env,jname,name);

	if(strcmp(name,"parmBackgroundRatio")==0){
		PARM_SWITCH(double,BackgroundRatio)

	}else if(strcmp(name,"parmComplexityReductionThreshold")==0){
		PARM_SWITCH(double,ComplexityReductionThreshold)

	}else if(strcmp(name,"parmDetectShadows")==0){
		PARM_SWITCH(bool,DetectShadows)

	}else if(strcmp(name,"parmHistory")==0){
		PARM_SWITCH(int,History)

	}else if(strcmp(name,"parmNMixtures")==0){
		PARM_SWITCH(int,NMixtures)

	}else if(strcmp(name,"parmShadowThreshold")==0){
		PARM_SWITCH(double,ShadowThreshold)

	}else if(strcmp(name,"parmShadowValue")==0){
		PARM_SWITCH(int,ShadowValue)

	}else if(strcmp(name,"parmVarInit")==0){
		PARM_SWITCH(double,VarInit)

	}else if(strcmp(name,"parmVarMax")==0){
		PARM_SWITCH(double,VarMax)

	}else if(strcmp(name,"parmVarMin")==0){
		PARM_SWITCH(double,VarMin)

	}else if(strcmp(name,"parmVarThreshold")==0){
		PARM_SWITCH(double,VarThreshold)

	}else if(strcmp(name,"parmVarThresholdGen")==0){
		PARM_SWITCH(double,VarThresholdGen)

	}else if(strcmp(name,"initModel")==0){
		if(mod[jid]==NULL){
			mod[jid] = createBackgroundSubtractorMOG2(100,20,false);
		}else{
			mod[jid]->clear();
		}
		if(jattr!=NULL){
			jstrcpy(env,jattr,attr);
			load_model(attr,mod[jid],msk[jid]);
		}

	}else if(strcmp(name,"saveModel")==0){
		jstrcpy(env,jattr,attr);
		save_model(attr,mod[jid]);
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_panGeodesic_modFree(
	JNIEnv * env,
	jobject thiz
){
	for(int i=0; i<MAX_MOD; i++){
		msk[i].release();
		mod[i].release();
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_daemon_panGeodesic_setMskSize(
	JNIEnv * env,
	jobject thiz,
	jint jid,
	jint width,
	jint height
){
	if(jid>=MAX_MOD){
		return;
	}
	msk[jid].release();
	msk[jid] = Mat::zeros(height,width,CV_8UC1);
}

extern "C" JNIEXPORT jint JNICALL Java_prj_daemon_panGeodesic_getMskWidth(
	JNIEnv * env,
	jobject thiz,
	jint jid
){
	if(jid>=MAX_MOD){
		return -1;
	}
	return msk[jid].cols;
}

extern "C" JNIEXPORT jint JNICALL Java_prj_daemon_panGeodesic_getMskHeight(
	JNIEnv * env,
	jobject thiz,
	jint jid
){
	if(jid>=MAX_MOD){
		return -1;
	}
	return msk[jid].rows;
}





