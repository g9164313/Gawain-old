#include <global.hpp>
#include <grabber.hpp>

//--------junction for prj.epistar--------//

extern "C" JNIEXPORT jlong JNICALL Java_prj_epistar_Entry_rawOpen(
	JNIEnv * env,
	jobject thiz,
	jstring fdName,
	jstring fdAttr
){
	char name[500];
	jstrcpy(env,fdName,name);
	char attr[500];
	jstrcpy(env,fdAttr,attr);
	FILE* fd = fopen(name,attr);
	if(fd==NULL){
		return 0L;
	}
	return (long)fd;
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_Entry_rawClose(
	JNIEnv * env,
	jobject thiz,
	jlong ptrFile
){
	if(ptrFile==0L){
		return;
	}
	fclose((FILE*)ptrFile);
}

inline void mapArray2RawHead(JNIEnv * env,jintArray jHead,RawHead& hd){
	if(jHead==NULL){
		memset(&hd,0,sizeof(RawHead));
		return;
	}
	jint* head = env->GetIntArrayElements(jHead,NULL);
	hd.type = head[0];
	hd.rows = head[1];
	hd.cols = head[2];
	hd.tileFlag = head[3];
	hd.tileRows = head[4];
	hd.tileCols = head[5];	
	env->ReleaseIntArrayElements(jHead,head,0);
}

inline void mapRawHead2Array(JNIEnv * env,RawHead& hd,jintArray jHead){
	if(jHead==NULL){
		memset(&hd,0,sizeof(RawHead));
		return;
	}
	jint* head = env->GetIntArrayElements(jHead,NULL);
	head[0] = hd.type;
	head[1] = hd.rows;
	head[2] = hd.cols;
	head[3] = hd.tileFlag;
	head[4] = hd.tileRows;
	head[5] = hd.tileCols;	
	env->ReleaseIntArrayElements(jHead,head,0);
}

extern "C" JNIEXPORT int JNICALL Java_prj_epistar_Entry_rawSetHeader(
	JNIEnv * env,
	jobject thiz,
	jstring nameSrc,
	jintArray jHead
){
	char name[500];
	jstrcpy(env,nameSrc,name);
	FILE* fd = fopen(name,"rb+");
	if(fd==NULL){
		cout<<"fail to open "<<name<<endl;
		return -1;
	}
	RawHead hd;
	fread(&hd,sizeof(hd),1,fd);
	mapArray2RawHead(env,jHead,hd);
	fseek(fd,0,SEEK_SET);
	fwrite(&hd,sizeof(hd),1,fd);
	fclose(fd);
	return 0;
}

extern "C" JNIEXPORT int JNICALL Java_prj_epistar_Entry_rawGetHeader(
	JNIEnv * env,
	jobject thiz,
	jstring nameSrc,
	jintArray jHead
){
	char name[500];
	jstrcpy(env,nameSrc,name);
	FILE* fd = fopen(name,"rb");
	if(fd==NULL){
		cout<<"fail to open "<<name<<endl;
		return -1;
	}
	RawHead hd;
	fread(&hd,sizeof(hd),1,fd);
	mapRawHead2Array(env,hd,jHead);
	fclose(fd);
	return 0;
}


extern "C" JNIEXPORT void JNICALL Java_prj_epistar_Entry_rawSetHeadFd(
	JNIEnv * env,
	jobject thiz,
	jlong jFd,
	jintArray jHead
){
	if(jFd==0L){ return; }
	FILE* fd = (FILE*)jFd;
	RawHead hd;
	fread(&hd,sizeof(hd),1,fd);
	mapArray2RawHead(env,jHead,hd);
	fseek(fd,0,SEEK_SET);
	fwrite(&hd,sizeof(hd),1,fd);
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_Entry_rawGetHeadFd(
	JNIEnv * env,
	jobject thiz,
	jlong jFd,
	jintArray jHead
){
	if(jFd==0L){ return; }
	FILE* fd = (FILE*)jFd;
	RawHead hd;
	fread(&hd,sizeof(hd),1,fd);
	mapRawHead2Array(env,hd,jHead);
}

NAT_EXPORT void panoramaTile(FILE* fdSrc,FILE* fdDst);
NAT_EXPORT int tear_tile_nxn(
	FILE* fd,
	Mat& img,
	long tid,
	long cntX,long cntY,
	int scale
);
extern "C" JNIEXPORT jint JNICALL Java_prj_epistar_TskMake_makePanorama(
	JNIEnv * env,
	jobject thiz,
	jstring jnameGrab,
	jstring jnamePano,
	jstring jnameNail
){
	char nameGrab[500],namePano[500],nameNail[500];
	jstrcpy(env,jnameGrab,nameGrab);
	jstrcpy(env,jnamePano,namePano);
	jstrcpy(env,jnameNail,nameNail);
	int scale=1;

	FILE* fdSrc = fopen(nameGrab,"rb");
	FILE* fdDst = fopen(namePano,"wb+");
	panoramaTile(fdSrc,fdDst);
	Mat img;
	scale = tear_tile_nxn(fdDst,img,0,-1,-1,-1);
	fclose(fdSrc);
	fclose(fdDst);
	imwrite(nameNail,img);
	img.release();
	return scale;
}

extern void gridMake(
	FILE* fdSrc,
	const char* imgPano,
	int scale,
	FILE* fdDst,
	const char* imgGrid
);
extern "C" JNIEXPORT void JNICALL Java_prj_epistar_TskMake_makeGridding(
	JNIEnv * env,
	jobject thiz,
	jstring jnamePano,
	jstring jnameNail1,
	jint scale,
	jstring jnameGrid,
	jstring jnameNail2
){
	char namePano[500],nameNail1[500];
	char nameGrid[500],nameNail2[500];
	jstrcpy(env,jnamePano ,namePano );
	jstrcpy(env,jnameNail1,nameNail1);
	jstrcpy(env,jnameGrid ,nameGrid );
	jstrcpy(env,jnameNail2,nameNail2);

	FILE* fdSrc = fopen(namePano,"rb");
	FILE* fdDst = fopen(nameGrid,"wb+");
	gridMake(
		fdSrc,nameNail1,scale,
		fdDst,nameNail2
	);
	fclose(fdSrc);
	fclose(fdDst);
}

extern void gridMeas(
	FILE* fdDst,
	const char* mapName,
	FILE* fdSrc
);
extern "C" JNIEXPORT void JNICALL Java_prj_epistar_TskMake_makeMeasure(
	JNIEnv * env,
	jobject thiz,
	jstring jnameMeas,
	jstring jnameMap,
	jstring jnameGrid
){
	char nameMeas[500],nameMap[500],nameGrid[500];
	jstrcpy(env,jnameMeas,nameMeas);
	jstrcpy(env,jnameMap ,nameMap );
	jstrcpy(env,jnameGrid,nameGrid);

	FILE* fdSrc = fopen(nameGrid,"rb");
	FILE* fdDst = fopen(nameMeas,"wb+");
	gridMeas(
		fdDst,nameMap,
		fdSrc
	);
	fclose(fdSrc);
	fclose(fdDst);
}
//----------------//

extern "C" JNIEXPORT jobjectArray JNICALL Java_prj_epistar_PanViewer_getMap(
	JNIEnv* env,
	jobject thiz,
	jstring jname
){
	char name[500];
	jstrcpy(env,jname,name);
	Mat map;
	FileStorage yml(name,FileStorage::READ);
	if(yml.isOpened()==false){
		return NULL;
	}
	yml["map"]>>map;
	yml.release();
	//cout<<map;

	jclass clazz = env->FindClass("[C");

	jobjectArray jmap = env->NewObjectArray(map.rows,clazz,NULL);

	for(size_t j=0; j<map.rows; j++){

		jcharArray row = env->NewCharArray(map.cols);

		jchar* ptr = new jchar[map.cols];
		for(size_t i=0; i<map.cols; i++){
			ptr[i] = map.at<uint8_t>(j,i);
		}
		env->SetCharArrayRegion(row,0,map.cols,ptr);
		env->SetObjectArrayElement(jmap,j,row);

		delete ptr;
	}
	return jmap;
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_PanViewer_projMap(
	JNIEnv* env,
	jobject thiz,
	jstring jname,
	jlong jfile,
	jint adj,
	jint px,jint py,
	jint ww,jint hh
){
	if(jfile==0){
		return;
	}
	char name[500];
	jstrcpy(env,jname,name);
	FILE* fd=(FILE*)jfile;
	RawHead hd;
	fseek(fd,0,SEEK_SET);
	fread(&hd,sizeof(RawHead),1,fd);
	Mat node(
		hd.rows/adj,
		hd.cols/adj,
		hd.type
	);
	int scale = 1+std::min(
		hh/node.rows,
		ww/node.cols
	);
	tear_tile_any(fd,hd,node,px,py,scale);
	imshow(name,node);
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_PanViewer_proj2Grid(
	JNIEnv* env,
	jobject thiz,
	jstring jname,
	jlong jfile,
	jint gx,jint gy
){
	if(jfile==0){
		return;
	}
	char name[500];
	jstrcpy(env,jname,name);
	FILE* fd=(FILE*)jfile;
	RawHead hd;
	fseek(fd,0,SEEK_SET);
	fread(&hd,sizeof(RawHead),1,fd);

	Mat buf(hd.rows,hd.cols,hd.type);
	getTile(fd,hd,gid2tid(hd,gx,gy),buf);
	imshow(name,buf);
}
//----------------//
