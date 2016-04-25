#include <global.hpp>
#include <grabber.hpp>

void GBundle::init(){

	clzz=env->GetObjectClass(thiz);

	idCallback = env->GetMethodID(clzz,"looperCallback","(JJ)V");

	//optTapeType= env->GetFieldID(clzz,"optTapeType","B");
	optTapeIndx= env->GetFieldID(clzz,"optTapeIndx","J");
	optTapeSize= env->GetFieldID(clzz,"optTapeSize","J");
	//bnd->idImgHeight= env->GetFieldID(bnd->clzz,"imgHeight","I");
	optFPS = env->GetFieldID(clzz,"optFPS" ,"D");
	optExit= env->GetFieldID(clzz,"optExit","Z");

	preTick = -1.;//reset this~~~

	tapeBIN1=tapeInfo=NULL;

	getTxt("wndName",wndName);

	cfgName[0] = 0;//default name is empty
	getTxt("cfgName",cfgName);

	infText[0] = 0;//device will write this information~~~
}

void GBundle::callback(Mat& dat){
	if(env==NULL){
		updateTick();
		imshow(wndName,dat);
		waitKey(10);//!!WIN7/8 WORKAROUND!!
		return;
	}
	if(dat.empty()==true){
		//let user can execute commands~~~
		env->CallIntMethod(thiz,idCallback,0,0);
		return;
	}
	if(overlay.empty()==true){
		overlay = Mat::zeros(dat.size(),CV_8UC3);
	}else if(overlay.size()!=dat.size()){
		//recreate the overlay again~~~
		overlay = Mat::zeros(dat.size(),CV_8UC3);
	}

	////update data & overlay////
	switch(dat.type()){
	case CV_8UC1:
		cvtColor(dat,overlay,COLOR_GRAY2BGR);
		break;
	case CV_8UC3:
		dat.copyTo(overlay);
		break;
	}
	env->CallIntMethod(thiz,idCallback,(jlong)(&dat),(jlong)(&overlay));

	updateTape(dat);
	updateTick();
}

void GBundle::callback(void* ptr,int width,int height,int type){
	Mat dat(height,width,type,ptr);
	callback(dat);
}

void experiment1(Mat& dat){

	/*Mat nod1,nod2,nod3;
	cvtColor(dat,nod1,COLOR_BGR2Lab);
	Mat lba[] = {
		Mat::zeros(dat.size(),CV_8UC1),
		Mat::zeros(dat.size(),CV_8UC1),
		Mat::zeros(dat.size(),CV_8UC1)
	};
	split(nod1,lba);
	Ptr<CLAHE> ago = createCLAHE();
	ago->setClipLimit(4);
	ago->apply(lba[0],nod2);
	nod2.copyTo(lba[0]);
	merge(lba,3,nod3);
	cvtColor(nod3,dat,COLOR_Lab2BGR);*/
	Mat rgb[] = {
		Mat::zeros(dat.size(),CV_8UC1),
		Mat::zeros(dat.size(),CV_8UC1),
		Mat::zeros(dat.size(),CV_8UC1)
	};
	split(dat,rgb);
	Ptr<CLAHE> ago = createCLAHE();
	ago->setClipLimit(5);
	ago->apply(rgb[0],rgb[0]);
	ago->apply(rgb[1],rgb[1]);
	ago->apply(rgb[2],rgb[2]);
	merge(rgb,3,dat);
}

void GBundle::updateTick(){
	if(preTick<0){
		preTick = getTickCount();
		curFreq = 0.;
	}else{
		int64 curTick = getTickCount();
		curFreq = getTickFrequency()/(double)(curTick-preTick);
		if(env!=NULL){
			env->SetDoubleField(thiz,optFPS,curFreq);
		}
		preTick = curTick;
	}
}

void GBundle::updateTape(Mat& dat){

	jlong idx = env->GetLongField(thiz,optTapeIndx);
	jlong cnt = env->GetLongField(thiz,optTapeSize);

	if(cnt==0 || idx==cnt){
		if(tapeBIN1!=NULL){
			fclose(tapeBIN1);
			tapeBIN1 = NULL;
		}
		if(tapeInfo!=NULL){
			fclose(tapeInfo);
			tapeInfo = NULL;
		}
		env->SetLongField(thiz,optTapeIndx,0);
		return;
	}

	char name[500],appx[10],fname[510];
	getTxt("optTapeName",name);
	getTxt("optTapeAppx",appx);

	if(strcmp(appx,".bin")==0){
		if(tapeBIN1==NULL){
			sprintf(fname,"%s%s",name,appx);
			tapeBIN1 = fopen(fname,"wb");
			cout<<"create bin file:"<<fname<<endl;
			getTxt("optTapeInfo",fname);
			if(strlen(fname)!=0){
				tapeInfo = fopen(fname,"w");
				fprintf(
					tapeInfo,
					"size=%dx%d,type=%d,CPU_FREQ=%E\n########\n",
					dat.cols,dat.rows,dat.type(),
					getTickFrequency()
				);
			}
		}else{
			fwrite(dat.ptr(),dat.total(),dat.elemSize(),tapeBIN1);
			if(tapeInfo!=NULL){
				fprintf(tapeInfo,"%ld, %s\n",preTick,infText);
			}
		}
	}else{
		sprintf(fname,"%s%08ld%s",name,idx,appx);
		imwrite(fname,dat);
	}

	idx++;
	env->SetLongField(thiz,optTapeIndx,idx);
	if(cnt>0){
		cnt--;
		env->SetLongField(thiz,optTapeSize,cnt);
	}
}

void GBundle::getTxt(const char* name,const char* txt){
	jfieldID mid;
	jstring msg;
	mid = env->GetFieldID(clzz,name,"Ljava/lang/String;");
	msg = (jstring)env->GetObjectField(thiz,mid);
	jstrcpy(env,msg,(char*)txt);
}

void GBundle::setTxt(const char* name,const char* txt){
	jfieldID mid;
	jstring msg;
	mid = env->GetFieldID(clzz,name,"Ljava/lang/String;");
	msg = env->NewStringUTF(txt);
	env->SetObjectField(thiz,mid,msg);
}

bool GBundle::checkExit(){
	if(env==NULL){
		char k = (char)cvWaitKey(5);
		if(k!=27){
			return false;//user press ESC key~~~
		}
		return true;
	}
	if(env->GetBooleanField(thiz,optExit)==JNI_FALSE){
		return false;
	}
	setLastMsg("exit");
	return true;
}
//---------------------------------//

#define IPC_SERVER
#include <utils_ipc.hpp>
extern "C" JNIEXPORT void JNICALL Java_narl_itrc_DevGrabber_looperNulMem(
	JNIEnv* env,
	jobject thiz,
	jstring jname
){
	GBundle bnd(env,thiz);

	char name[50];
	jstrcpy(env,jname,name);

	IpcToken tkn(name);

	char* bufTxt = tkn.getMsg();

	Mat imgDat;
	do{
		if(tkn.pipeWait()<0){
			continue;
		}
		//cout<<"CMD="<<bufTxt<<endl;
		if(CmdIsIdfy==true){
			sprintf(bufTxt,"DONE,null-memory-device=%s",bnd.getName());
			bnd.setPipeMsg(bufTxt);
		}else if(CmdIsMems==true){
			//parse parameters,[width],[height],[scale],[type]
			int idx = bufTxt[3]-'0';
			int param[4];
			if(tkn.parseInitParam(bufTxt,param)==true){
				int len = param[0]*param[1]*param[2];
				void* ptr_mem = tkn.setMem(idx,len);
				//cout<<"MEM @ "<<len<<endl;
				if(ptr_mem!=NULL){
					if(idx==0){
						//the first memory is shared for Image~~~
						imgDat = Mat(
							param[1],param[0],
							param[3],
							ptr_mem
						);
					}
					sprintf(bufTxt,"DONE,%p",ptr_mem);
				}else{
					sprintf(bufTxt,"FAIL,internal API");
				}
			}else{
				sprintf(bufTxt,"FAIL,invalid parameter");
			}
		}else{
			//just callback and get result~~
			bnd.setPipeMsg(bufTxt);
			bnd.callback(imgDat);
			bnd.getPipeMsg(bufTxt);
			//cout<<"pipeMsg="<<bufTxt<<endl;
			if(TextIsFail==false && TextIsDone==false){
				sprintf(bufTxt,"FAIL,unknown command");
			}
		}
		tkn.pipeSend(NULL);
	}while(!bnd.checkExit());
}
//---------------------------------//

extern "C" JNIEXPORT jlong JNICALL Java_narl_itrc_DevGrabber_showFileFrame(
	JNIEnv* env,
	jobject thiz,
	jstring jname,
	jlong idx,jlong off,
	jint width,jint height,
	jint type
){
	char name[500];
	jstrcpy(env,jname,name);

	GBundle bnd(env,thiz);

	Mat dat(height,width,type);
	FILE *fs = fopen(name,"rb");
	fseek(fs,0,SEEK_END);
	jlong max=ftell(fs);
	jlong blk=dat.total()*dat.elemSize();
	max = max/blk;
	//cout<<"total="<<max<<",blk="<<blk<<endl;
	if(idx>=max){
		idx = max-1;
	}
	fseek(fs,idx*blk,SEEK_SET);
	fread(dat.ptr(),dat.total(),dat.elemSize(),fs);
	fclose(fs);

	Mat lay;
	//bnd.updateData(dat,lay);//TODO:do we need this???

	Point pp;
	const int font_h = 20;
	char txt[200];
	pp.x = 3;
	pp.y = font_h;
	sprintf(txt,"frame=%ld",idx);
	putText(lay,txt,pp,FONT_HERSHEY_PLAIN,1.,Scalar(250,250,0));

	bnd.updateTape(dat);
	return idx;
}




