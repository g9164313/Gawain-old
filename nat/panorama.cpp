#include <global.hpp>
#include <grabber.hpp>

static int Num[2]={15,15},Denom[2]={100,100};

inline int get_overlay(int val,int i){
	int tmp = (val*Num[i])/Denom[i];
	if(tmp<=0){
		return 1;
	}
	return tmp;
}

inline int getOverlapW(int val){
	return get_overlay(val,0);
}

inline int getOverlapH(int val){
	return get_overlay(val,1);
}

void match_tile(Mat& pre,char dir,Mat& cur,Point& off){

	int over_w = getOverlapW(cur.cols);
	int over_h = getOverlapH(cur.rows);
	int strip_w = over_w/10+1;
	int strip_h = over_h/10+1;
	Rect aa,bb;

	switch(dir){
	case '\211'://~'v'
	case '^'://top
		aa = Rect(0,0,cur.cols,strip_h);
		bb = Rect(0,pre.rows-over_h,pre.cols,over_h);
		break;
	case '\241'://~'^'
	case 'v'://bottom
		aa = Rect(0,cur.rows-strip_h,cur.cols,strip_h);
		bb = Rect(0,0,pre.cols,over_h);
		break;
	case '\301'://~'>'
	case '<'://left
		aa = Rect(0,0,strip_w,cur.rows);
		bb = Rect(pre.cols-over_w,0,over_w,pre.rows);
		break;
	case '\303'://~'<'
	case '>'://right
		aa = Rect(cur.cols-strip_w,0,strip_w,cur.rows);
		bb = Rect(0,0,over_w,pre.rows);
		break;
	}

	Mat corr(
		bb.height-aa.height+1,
		bb.width -aa.width +1,
		CV_32FC1
	);
	matchTemplate(pre(bb),cur(aa),corr,CV_TM_CCORR_NORMED);
	minMaxLoc(corr,NULL,NULL,NULL,&off);

	//modify offset~~~
	switch(dir){
	case '\211'://~'v'
	case '^'://top
		off.y = -over_h+off.y;
		break;
	case '\241'://~'^'
	case 'v'://bottom
		off.y = strip_h+off.y;
		break;
	case '\301'://~'>'
	case '<'://left
		off.x = -over_w+off.x;
		break;
	case '\303'://~'<'
	case '>'://right
		off.x = off.x+strip_w;
		break;
	}
}

void regist_tile(Mat& pre,char dir,Mat& cur,Point& off){
	int dw = abs(off.x);
	int dh = abs(off.y);
	Rect aa,bb;

	switch(dir){
	case '\211'://~'v'
	case '^'://top
		aa = Rect(0,0,cur.cols,dh);
		bb = Rect(0,pre.rows-dh,pre.cols,dh);
		break;
	case '\241'://~'^'
	case 'v'://bottom
		aa = Rect(0,cur.rows-dh,cur.cols,dh);
		bb = Rect(0,0,pre.cols,dh);
		break;
	case '\301'://~'>'
	case '<'://left
		aa = Rect(0,0,dw,cur.rows);
		bb = Rect(pre.cols-dw,0,dw,pre.rows);
		break;
	case '\303'://~'<'
	case '>'://right
		aa = Rect(cur.cols-dw,0,dw,cur.rows);
		bb = Rect(0,0,dw,pre.rows);
		break;
	}

	Mat f_cur = Mat_<float>(cur(aa));
	Mat f_pre = Mat_<float>(pre(bb));
	Mat hann;
	createHanningWindow(
		hann,
		f_cur.size(),
		CV_32F
	);
	Point2d dff = phaseCorrelate(f_cur,f_pre,hann);
	//cout<<"relocate=("<<dff.x<<","<<dff.y<<")@"<<resp<<endl;
	//really???
	off.x = off.x + cvRound(dff.x);
	off.y = off.y + cvRound(dff.y);
	return;
}

char walk_through(RawHead& hd,long tid,Point& pos,Point& over){
	char dir='<';
	if(IsBegOfRow(tid,hd)==true){
		dir='^';
		pos.y = pos.y + hd.rows;
	}else{
		switch(hd.tileFlag){
		case TILE_INTERLEAVE:
			if(IsEventRow(tid,hd)==true){
				dir='<';
				pos.x = pos.x + hd.cols;
			}else{
				dir='>';
				pos.x = pos.x - hd.cols;
			}
			break;
		default:
		case TILE_SEQUENCE:
			dir='<';
			pos.x = pos.x + hd.cols;
			break;
		}
	}
	return dir;
}

/*if(tid>=(407)){
	imwrite("cc.0.png",cur);
	imwrite("cc.1.png",pre);
	Mat _cur,_pre;
	//Mat sum(2*hd.rows+off.y,2*hd.cols,hd.type);
	//copyMakeBorder(cur,_cur,hd.rows+off.y,0,0,0,BORDER_CONSTANT);
	//copyMakeBorder(pre,_pre,0,hd.rows+off.y,0,0,BORDER_CONSTANT);
	Mat sum(hd.rows,hd.cols*2+off.x,hd.type);
	copyMakeBorder(cur,_cur,0,0,hd.cols+off.x,0,BORDER_CONSTANT);
	copyMakeBorder(pre,_pre,0,0,0,hd.cols+off.x,BORDER_CONSTANT);
	sum = (_cur+_pre)/2;
	imwrite("cc.3.png",sum);
}*/

void adjust_loca(FILE* fdDst,FILE* fdSrc,RawHead& hd,vector<Point>& loca){
	Mat cur(hd.rows,hd.cols,hd.type);
	Mat pre(cur.size(),hd.type);

	Point pos(0,0),off(0,0);
	Point over(
		getOverlapW(hd.cols),
		getOverlapH(hd.rows)
	);

	long tid=0;
	long cnt=hd.tileCols*hd.tileRows;
	char dir='<';

	cur = cur*0;//clear~~
	fwrite(cur.ptr(),cur.total(),cur.elemSize(),fdDst);//just for reduce disk I/O

	getTile(fdSrc,hd,tid,pre);
	loca.push_back(pos);//it is the first and special one~~~
	tid++;
	dir=walk_through(hd,tid,pos,over);
	do{
		getTile(fdSrc,hd,tid,cur);
		//registration
		match_tile(pre,dir,cur,off);
		//regist_tile(pre,dir,cur,off);
		pos.x = pos.x + off.x;
		pos.y = pos.y + off.y;
		loca.push_back(pos);
		//prepare the nest turn~~~
		cur.copyTo(pre);
		tid++;
		dir=walk_through(hd,tid,pos,over);

		cur = cur*0;//clear~~
		fwrite(cur.ptr(),cur.total(),cur.elemSize(),fdDst);//just for reduce disk I/O
	}while(tid<cnt);
}
//----------------------------//

void fusion_tile(FILE* fd,RawHead& hd,Mat src,Point pos){
	if(pos.x<0||pos.y<0){
		return;
	}
	int gx=pos.x/hd.cols;
	int gy=pos.y/hd.rows;
	Mat dst(hd.rows,hd.cols,hd.type);
	getTile(fd,hd,gx,gy,dst);

	Rect roi;
	roi.x=pos.x%hd.cols;
	roi.y=pos.y%hd.rows;
	roi.width =src.cols;
	roi.height=src.rows;

	src.copyTo(dst(roi));

	putTile(fd,hd,gx,gy,dst);
}

void revive_tile(FILE* fdSrc,FILE* fdDst,RawHead& hd,vector<Point>& loca){
	Mat img(hd.rows,hd.cols,hd.type);
	long tid=0;
	long cnt=hd.tileCols*hd.tileRows;

	int tw = hd.cols;//change to sign~~~
	int th = hd.rows;//change to sign~~~

	getTile(fdSrc,hd,tid,img);
	putTile(fdDst,hd,tid,img);//it is the first and special tile~~
	tid++;
	do{
		getTile(fdSrc,hd,tid,img);
		Point& aa = loca[tid];
		Point bb;
		if(aa.x>0){
			bb.x = aa.x + tw - aa.x%tw;
		}else{
			bb.x = aa.x -aa.x%tw;
		}
		if(aa.y>0){
			bb.y = aa.y + th - aa.y%th;
		}else{
			bb.y = aa.y - aa.y%th;
		}
		if(aa.x>=0&&aa.y>=0){
			Rect roi(
				0        ,0,
				bb.x-aa.x,bb.y-aa.y
			);
			if(roi.width>0 && roi.height>0){
				fusion_tile(fdDst,hd,img(roi),aa);
			}
		}
		if(bb.x>=0 && aa.y>=0){
			Rect roi(
				   bb.x-aa.x,        0,
				tw-bb.x+aa.x,bb.y-aa.y
			);
			if(roi.width>0 && roi.height>0){
				fusion_tile(fdDst,hd,img(roi),Point(bb.x,aa.y));
			}
		}
		if(aa.x>=0 && bb.y>=0){
			Rect roi(
			            0,   bb.y-aa.y,
				bb.x-aa.x,th-bb.y+aa.y
			);
			if(roi.width>0 && roi.height>0){
				fusion_tile(fdDst,hd,img(roi),Point(aa.x,bb.y));
			}
		}
		if(bb.x>=0 && bb.y>=0){
			Rect roi(
				   bb.x-aa.x,   bb.y-aa.y,
				tw-bb.x+aa.x,th-bb.y+aa.y
			);
			if(roi.width>0 && roi.height>0){
				fusion_tile(fdDst,hd,img(roi),bb);
			}
		}
		tid++;
	}while(tid<cnt);
}

void save_var(vector<Point>& dat){
	FileStorage fs("data.yml", FileStorage::WRITE);
	fs<<"loca"<<dat;
	fs.release();
}

void load_var(vector<Point>& dat){
	dat.clear();
	FileStorage fs("data.yml", FileStorage::READ);
	fs["loca"]>>dat;
	fs.release();
	//for(int i=0; i<dat.size(); i++){
	//	printf("%d=(%d,%d)\n",i,dat[i].x,dat[i].y);
	//}
}

NAT_EXPORT void panoramaTile(FILE* fdSrc,FILE* fdDst){
	RawHead hd;
	fseek(fdSrc,0L,SEEK_SET);
	fread(&hd,sizeof(hd),1,fdSrc);
	fwrite(&hd,sizeof(hd),1,fdDst);
	vector<Point> loca;
	adjust_loca(fdDst,fdSrc,hd,loca);//start to registration and matching
	//save_var(loca);
	//blank_raw(fdDst,hd);
	//load_var(loca);
	revive_tile(fdSrc,fdDst,hd,loca);//restore data and fusion
}
//---------------------------------------------//

NAT_EXPORT void tear_tile_any(
	FILE* fd,RawHead& hd,
	Mat& img,
	int px,int py,
	int scale
){
	//scale is no zero,
	//positive-->shrink buffer
	//negative-->expand buffer
	if(scale==0 || scale==-1){ scale=1; }
	img = img * 0;//clear old data~~~~

	Mat buf(hd.rows,hd.cols,hd.type);
	Rect bb,aa;
	int width = (scale>1)?(img.cols*scale):(img.cols/scale);
	int height= (scale>1)?(img.rows*scale):(img.rows/scale);
	int imgRH=px+width;
	int imgBM=py+height;
	int imgTP=py;
	do{
		int imgLF=px;
		do{
			int gx=imgLF/hd.cols;
			int gy=imgTP/hd.rows;
			long tid = gid2tid(hd,gx,gy);
			if(tid<0){
				break;
			}
			getTile(fd,hd,tid,buf);
			bb.x=imgLF-gx*hd.cols;
			bb.y=imgTP-gy*hd.rows;
			int end=(gx+1)*hd.cols;
			if(end>imgRH){
				bb.width=imgRH-imgLF;
			}else{
				bb.width=end-imgLF;
			}
			end=(gy+1)*hd.rows;
			if(end>imgBM){
				bb.height=imgBM-imgTP;
			}else{
				bb.height=end-imgTP;
			}
			aa.x=imgLF-px;
			aa.y=imgTP-py;
			aa.width =bb.width;
			aa.height=bb.height;

			if(scale==1){
				buf(bb).copyTo(img(aa));
			}else if(
				scale>1 &&
				(bb.height/scale)>=1 &&
				(bb.width /scale)>=1
			){
				Mat tmp(
					bb.height/scale,
					bb.width/scale,
					hd.type
				);
				resize(buf(bb),tmp,tmp.size());
				Rect _aa(
					aa.x/scale,
					aa.y/scale,
					tmp.cols,
					tmp.rows
				);
				//valid_roi(_aa,img);
				tmp.copyTo(img(_aa));
			}else{
				//TODO: what~~~
			}

			imgLF+=bb.width;
		}while(imgLF<imgRH);
		imgTP+=bb.height;
	}while(imgTP<imgBM && bb.height!=0);
}

NAT_EXPORT int tear_tile_nxn(
	FILE* fd,
	Mat& img,
	long tid,
	long cntX,long cntY,
	int scale
){
	RawHead hd;
	fseek(fd,0L,SEEK_SET);
	fread(&hd,sizeof(hd),1,fd);
	if(hd.tileCols==0||hd.tileRows==0){
		cerr<<"header has problem..."<<endl;
		return 1;
	}
	if(cntX<0){ cntX=hd.tileCols; }
	if(cntY<0){ cntY=hd.tileRows; }
	if(scale==0){
		scale = 1;
	}else if(scale<0){
		//auto calculate the size of thumbnail~~~
		//use 100MB as limit...
		size_t total=0;
		size_t count=hd.tileCols*hd.tileRows;
		scale = 0;//reset it~~~
		do{
			scale++;
			total = (hd.cols/scale)*(hd.rows/scale);
			total = total*count;
		}while(total>(1024*1024*100));
	}

	Mat buf(hd.rows,hd.cols,hd.type);
	Mat tmp((hd.rows)/scale,(hd.cols)/scale,hd.type);
	img.create(tmp.rows*cntY,tmp.cols*cntX,hd.type);
	Size tsz = tmp.size();

	long gsx=tid%hd.tileCols;
	long gsy=tid/hd.tileCols;

	Rect roi(0,0,tsz.width,tsz.height);
	for(long gy=gsy; gy<(cntY+gsy); gy++){
		for(long gx=gsx; gx<(cntX+gsx); gx++){
			long idx = gid2tid(hd,gx,gy);
			getTile(fd,hd,idx,buf);
			resize(buf,tmp,tsz);
			roi.x = (gx-gsx)*tsz.width;
			roi.y = (gy-gsy)*tsz.height;
			tmp.copyTo(img(roi));
		}
	}
	return scale;
}

//static Scalar randomColor( RNG& rng )
//{
//  int icolor = (unsigned) rng;
//  return Scalar( icolor&255, (icolor>>8)&255, (icolor>>16)&255 );
//}

NAT_EXPORT int tearTileNxN(
	const char* nameDst,
	const char* nameSrc,
	long tid,
	long cntX,long cntY,
	int scale,
	int tile
){
	RawHead hd;
	FILE* fd = fopen(nameSrc,"rb");
	fseek(fd,0L,SEEK_SET);
	fread(&hd,sizeof(hd),1,fd);
	Mat img;
	scale = tear_tile_nxn(fd,img,tid,cntX,cntY,scale);
	imwrite(nameDst,img);
	img.release();
	fclose(fd);
	if(tile<=0){
		return scale;
	}

	if(cntX<0){ cntX=hd.tileCols; }
	if(cntY<0){ cntY=hd.tileRows; }
	img = imread(nameDst,IMREAD_COLOR);
	/*RNG rng(0xFFFFFFFF);
	for(size_t i=0; i<5; i++){
		Scalar cc = randomColor(rng);
		Point& vt = dispData[i];
		rectangle(
			img,
			Rect(vt.x,vt.y,hd.cols,hd.rows),
			cc,
			grid
		);
		char txt[50];
		sprintf(txt,"%zu",i);
		Point to = vt;
		to.y += 40;
		putText(img,txt,to,FONT_HERSHEY_SIMPLEX,1.,cc);
	}*/
	int ww = img.cols;
	int hh = img.rows;
	Point p1,p2;
	Scalar cc(0,200,0);
	for(size_t i=1; i<cntX; i++){
		p1.x=i*(ww/cntX);
		p1.y=0;
		p2.x=p1.x;
		p2.y=hh;
		line(img,p1,p2,cc,tile);
	}
	for(size_t i=1; i<cntY; i++){
		p1.x=0;
		p1.y=i*(hh/cntY);
		p2.x=ww;
		p2.y=p1.y;
		line(img,p1,p2,cc,tile);
	}
	imwrite(nameDst,img);
	return scale;
}

NAT_EXPORT void tearTile1(const char* nameSrc,size_t tid,const char* nameDst){
	RawHead hd;
	FILE* fd = fopen(nameSrc,"rb");
	if(fd==NULL){
		return;
	}
	fread(&hd,sizeof(hd),1,fd);
	Mat buf(hd.rows,hd.cols,hd.type);
	getTile(fd,hd,tid,buf);
	fclose(fd);
	imwrite(nameDst,buf);
}

NAT_EXPORT void tearTileByGid(const char* nameSrc,int gx,int gy,const char* nameDst){
	RawHead hd;
	FILE* fd = fopen(nameSrc,"rb");
	if(fd==NULL){
		return;
	}
	fread(&hd,sizeof(hd),1,fd);
	Mat buf(hd.rows,hd.cols,hd.type);
	getTile(fd,hd,gid2tid(hd,gx,gy),buf);
	fclose(fd);
	imwrite(nameDst,buf);
}

NAT_EXPORT void tearTileAll(const char* name,const char* dir){
	RawHead hd;
	FILE* fd = fopen(name,"rb");
	if(fd==NULL){
		return;
	}
	fread(&hd,sizeof(hd),1,fd);
	char nameDst[500];
	int ww = hd.cols;
	int hh = hd.rows;
	Mat buf(hh,ww,hd.type);
	for(size_t gy=0; gy<hd.tileRows; gy++){
		for(size_t gx=0; gx<hd.tileCols; gx++){
			long tid = gid2tid(hd,gx,gy);
			if(tid<0){
				break;
			}
			getTile(fd,hd,tid,buf);
			sprintf(nameDst,"%sC%zu_%zu.tiff",dir,gy,gx);
			imwrite(nameDst,buf);
		}
	}
	fclose(fd);
}
//---------------------------------------------//









