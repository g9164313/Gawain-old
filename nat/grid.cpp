#include <global.hpp>
#include <grabber.hpp>

static int imgWidth=1,imgHeight=1;
static int joinL1=20 ,joinL2=40;
static int gridWD=700,gridHH=700;
static int _grid_wd=700,_grid_hh=700;
static int _grid_bd[4];

static Mat* _img;//debug!!!!

void grid_draw(
	const char* name,
	Mat& _img,
	vector<Point> *_node = NULL,
	const Point* _tri = NULL,
	Point* _cc = NULL,
	Mat* _grid = NULL
);

static Point cmpValue;

bool cmpDistance(Point a,Point b) {
	float aa = hypot2f(a,cmpValue);
	float bb = hypot2f(b,cmpValue);
	return (aa<bb);
}

Point norm_vec(Point& p0,Point& p1){
	Vec2f v(p1.x-p0.x,p1.y-p0.y);
	float len = hypot(v[0],v[1]);
	v[0] = v[0] / len;
	v[1] = v[1] / len;
	float scale = 1;
	if(abs(v[0])>=abs(v[1])){
		//it is horizontal~~
		if(v[0]<0){ v[0] = v[0] * -1; }
		scale = _grid_wd;
	}else{
		//it is vertical~~
		if(v[1]<0){ v[1] = v[1] * -1; }
		scale = _grid_hh;
	}
	v[0] = v[0] * scale;
	v[1] = v[1] * scale;
	return Point(v[0],v[1]);
}

void analysis_junction(Mat& img,vector<Point>& junc){
	//const int scale=4;
	//Mat img(_img.rows/scale+1,_img.cols/scale+1,_img.type());
	//pyrDown(_img,img);
	//imwrite("cc.0.tif",_img);
	vector<Point> node;

	Mat edg0(img.size(),CV_8UC1);
	Mat edg1(edg0.size(),CV_8UC1);
	Mat edg2(edg0.size(),CV_8UC1);
	thresholdRange(img,edg0,joinL1,joinL2);
	//imwrite("cc.0.tif",edg);

	int dta[4] = {
		edg0.cols/100, edg0.cols-edg0.cols/100,
		edg0.rows/100, edg0.rows-edg0.rows/100
	};
	int allow_min_size[2] = {
		_grid_wd/10,
		_grid_hh/10
	};
	int allow_max_area = (_grid_wd/5) * (_grid_hh/5);

	Point arch(-1,-1);
	Mat kern_o = getStructuringElement(MORPH_ELLIPSE,Size(6,6),arch);
	Mat kern_w = getStructuringElement(MORPH_RECT,Size(_grid_wd/2,1),arch);
	Mat kern_h = getStructuringElement(MORPH_RECT,Size(1,_grid_hh/2),arch);

	vector<vector<Point> > blob;
	morphologyEx(edg0,edg0,MORPH_ERODE ,kern_o,arch,1);
	morphologyEx(edg0,edg0,MORPH_DILATE,kern_o,arch,2);
	morphologyEx(edg0,edg1,MORPH_OPEN,kern_w);
	morphologyEx(edg0,edg2,MORPH_OPEN,kern_h);
	edg0 = edg1 & edg2;
	//imwrite("cc.5.tif",edg);
	if(countNonZero(edg0)==0){
		cerr<<"no junction!!!"<<endl;
		return;//WTF!!!
	}
	//check_overlay(img,edg,"cc.6.tif");
	//edg1 = edg1 * 0;

	//Step.1: find the junction blobs~~~
	findContours(edg0,blob,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);
	for(size_t i=0; i<blob.size(); i++){
		if(contourArea(blob[i])>=allow_max_area){
			continue;
		}
		vector<Point> approx;
		approxPolyDP(
			blob[i],
			approx,
			arcLength(blob[i],true)*0.1,
			true
		);
		if(approx.size()!=4){
			continue;
		}
		Rect rr = boundingRect(approx);
		if(
			rr.width <allow_min_size[0]||
			rr.height<allow_min_size[1]
		){
			continue;
		}
		//drawContours(edg1,blob,i,Scalar::all(255),3);//debug!!!
		node.push_back(Point(
			rr.x+rr.width/2,
			rr.y+rr.height/2
		));
	}
	//check_overlay(img,edg1,"cc.1.tif");

	//Step.2: check the distance between the junction~~~~
	cmpValue.x = img.cols/2;
	cmpValue.y = img.rows/2;
	std::sort(node.begin(),node.end(),cmpDistance);
	int bnd = std::min(_grid_wd/2,_grid_hh/2)-3;
	edg0 = edg0 * 0;
	edg1 = edg1 * 0;
	edg2 = edg2 * 0;
	for(size_t i=0; i<node.size(); i++){
		Rect roi(
			node[i].x-bnd,
			node[i].y-bnd,
			bnd*2,
			bnd*2
		);
		if(check_roi(roi,edg0)==false){
			continue;
		}
		Mat tmp0 = edg0(roi);
		Mat tmp1 = edg1(roi);
		Mat tmp2 = edg2(roi);
		Point cc(bnd,bnd);
		circle(tmp2,cc,bnd,Scalar::all(255),-1);
		tmp0 = tmp1 & tmp2;
		if(countNonZero(tmp0)!=0){
			continue;
		}
		circle(tmp1,cc,bnd,Scalar::all(255),-1);
		junc.push_back(node[i]);
	}
	//check_overlay(img,edg1,"cc.2.tif");//debug~~~
	//grid_draw("cc.0.tif",(*_img),&junc,NULL,NULL);//debug~~~
	return;
}

bool find_triangle(
	vector<Point>& node,
	Point* tri,
	int limit=-1
){
	if(node.size()<=3){
		return true;
	}
	Point& p0 = tri[0];
	cmpValue = p0;//fix the first point~~~
	std::sort(node.begin(),node.end(),cmpDistance);
	int len;
	for(size_t i1=1; i1<(node.size()-1); i1++){
		Point& p1 = node[1];
		len = hypot2i(p0,p1);
		if(len>=limit && limit>0){
			return false;
		}
		for(size_t i2=i1+1; i2<node.size(); i2++){
			Point& p2 = node[i2];
			len = hypot2i(p0,p2);
			if(len>=limit && limit>0){
				return false;
			}
			double deg0 = abs(find_deg(p0,p1,p2));
			double deg1 = abs(find_deg(p1,p2,p0));
			double deg2 = abs(find_deg(p2,p0,p1));
			if(
				IsInRange(deg0,90,1)==true||
				IsInRange(deg1,90,1)==true||
				IsInRange(deg2,90,1)==true
			){
				//check direction and normalize~~
				Point v1 = norm_vec(p0,p1);
				Point v2 = norm_vec(p0,p2);
				if(abs(v1.x)>=abs(v1.y)){
					tri[1] = p0 + v1;
					tri[2] = p0 + v2;
				}else{
					tri[1] = p0 + v2;
					tri[2] = p0 + v1;
				}
				return true;
			}
		}
	}
	return false;
}

void mesh_junction(vector<Point>& node){
	Point tri[3];
	vector<Point> buff,junc;
	buff = node;
	int bnd = std::max(_grid_wd,_grid_hh)*2;
	for(size_t i=0; i<node.size(); i++){
		tri[0] = node[i];
		if(find_triangle(buff,tri,bnd)==true){
			junc.push_back(node[i]);
		}
	}
	node = junc;
}

void find_triangle(vector<Point>& node,Point* tri,char tkn){
	int i1=-1, i2=-1;
	cmpValue = tri[0];
	std::sort(node.begin(),node.end(),cmpDistance);
	//check valid~~
	int dff,cnt=4;

	dff = hypot2i(tri[0],node[1]);
	if(dff<_grid_bd[0]||_grid_bd[1]<dff){ cnt--; }

	dff = hypot2i(tri[0],node[2]);
	if(dff<_grid_bd[0]||_grid_bd[1]<dff){ cnt--; }

	dff = hypot2i(tri[0],node[3]);
	if(dff<_grid_bd[2]||_grid_bd[3]<dff){ cnt--; }

	dff = hypot2i(tri[0],node[4]);
	if(dff<_grid_bd[2]||_grid_bd[3]<dff){ cnt--; }

	if(cnt<2){
		cerr<<"not enough vertex for triangle..."<<endl;
		return;
	}
	//find a triangle~~
	for(size_t i=1; i<5; i++){
		for(size_t j=i+1; j<5; j++){
			Point& p1 = node[i];
			Point& p2 = node[j];
			double deg = abs(find_deg(tri[0],p1,p2));
			if(IsInRange(deg,90,1)==true){
				i1 = i;
				i2 = j;
				break;
			}
		}
	}
	//check directory~~~
	tri[1] = tri[0];
	tri[2] = tri[0];
	Point p1 = node[i1] - tri[0];
	Point p2 = node[i2] - tri[0];
	if(tkn=='h'){
		if(abs(p1.y)>abs(p2.y)){
			std::iter_swap(&i1,&i2);
		}
	}else if(tkn=='v'){
		if(abs(p1.x)>abs(p2.x)){
			std::iter_swap(&i1,&i2);
		}
	}
	if(i1>=0){
		tri[1] = node[i1];
	}
	if(i2>=0){
		tri[2] = node[i2];
	}
}

void update_vec(Point& vec,char tkn,char dir){
	if(tkn=='h'){
		if(dir=='+'){
			vec.x = abs(vec.x);
		}else{
			vec.x = -abs(vec.x);
		}
	}else{
		if(dir=='+'){
			vec.y = abs(vec.y);
		}else{
			vec.y = -abs(vec.y);
		}
	}
}

inline bool IsInImg(Point& nxt){
	if(nxt.x<0 ||
		nxt.y<0 ||
		nxt.x>=imgWidth ||
		nxt.y>=imgHeight
	){
		return false;
	}
	return true;
}

int find_least(vector<Point>& node,Point& vtx,int limit=INT_MAX){
	int dff,idx=-1,min=limit;
	for(size_t i=0; i<node.size(); i++){
		dff = hypot2i(node[i],vtx);
		//cout<<"len="<<dff<<","<<min<<endl;
		if(min>dff){
			idx = i;
			min = dff;
		}
	}
	if(idx>=0){
		vtx = node[idx];
	}
	return idx;
}

void track_junction(
	vector<Point>& lane,
	vector<Point>& node,
	Point p0,Point vec,
	char tkn, char dir,
	int cnt=-1
){
	int bnd = std::max(_grid_wd/2,_grid_hh/2);

	vector<Point> buf;
	Point tmp=p0,nxt=p0;
	while(IsInImg(nxt)==true){
		update_vec(vec,tkn,dir);
		nxt = p0 + vec;
		int gap=0;
		while(find_least(node,nxt,bnd)<0){
			if(IsInImg(nxt)==false){
				break;
			}
			gap++;
			nxt = nxt + vec;
		}
		if(IsInImg(nxt)==false){
			break;
		}
		if(gap!=0){
			vec = nxt - p0;
			vec.x = vec.x / (gap+1);
			vec.y = vec.y / (gap+1);
			for(int i=0; i<gap; i++){
				p0 = p0 + vec;
				buf.push_back(p0);
			}
		}
		buf.push_back(nxt);
		vec = nxt - p0;
		if(vec.x==0&&vec.y==0){
			cerr<<"no vector!!!"<<endl;
			break;
		}
		p0 = nxt;//the next turn~~~
	}

	if(cnt<=0){
		lane = buf;
		return;
	}
	tmp = p0;
	for(int i=0; i<cnt; i++){
		if(i>=buf.size()){
			tmp = tmp + vec;
		}else{
			tmp = buf[i];
		}
		lane.push_back(tmp);
	}
}

Vec4i count_occupy(
	vector<Point>& lane,
	vector<Point>& node,
	Point& p0,
	Point& p1,Point& p2
){
	vector<Point> ext;
	Vec4i cnt;
	Point vec,tail_h,tail_v;

	ext.clear();
	vec = p1 - p0;
	track_junction(ext,node,p0,vec,'h','-');
	cnt[1] = ext.size();
	tail_h = ext.back();

	ext.clear();
	vec = p1 - p0;
	track_junction(ext,node,p0,vec,'h','+');
	cnt[0] = ext.size();
	tail_h = tail_h - ext.back();

	ext.clear();
	vec = p2 - p0;
	track_junction(ext,node,p0,vec,'v','-');
	cnt[3] = ext.size();
	tail_v = ext.back();
	//direction is inverse, so we reversed it~~
	std::reverse(ext.begin(),ext.end());
	lane = ext;
	lane.push_back(p0);

	ext.clear();
	vec = p2 - p0;
	track_junction(ext,node,p0,vec,'v','+');
	cnt[2] = ext.size();
	tail_v = tail_v - ext.back();
	//append again~~
	for(size_t i=0; i<ext.size(); i++){
		lane.push_back(ext[i]);
	}

	//clear points outside the circle~~~
	int rad = hypot(
		hypot(tail_h.x,tail_h.y),
		hypot(tail_v.x,tail_v.y)
	);
	rad=rad/2;
	vector<Point> buff;
	for(size_t i=0; i<node.size(); i++){
		int dff = hypot2i(p0,node[i]);
		if(dff<=rad){
			buff.push_back(node[i]);
		}
	}
	node = buff;//re-assign it~~~~
	return cnt;
}

Mat list_junction(
	vector<Point>& node,
	Point p0,
	Point p1,Point p2
){
	Mat grid;
	vector<Point> lane;
	Point tri[3],tv=p1-p0;
	int id,rem;
	Vec4i cnt = count_occupy(lane,node,p0,p1,p2);

	grid.create(cnt[2]+cnt[3]+1,cnt[0]+cnt[1]+1,CV_32SC2);
	//grid_draw("cc.1.tif",(*_img),NULL,&lane);
	cout<<"lane=";
	for(size_t j=0; j<lane.size(); j++){
		tri[0] = lane[j];
		cout<<j<<",";
		find_triangle(node,tri,'h');
		if(id>=0){
			//check vector is valid~~~
			Point tmp = tri[1] - tri[0];
			if(abs(find_deg(tmp,tv))<=5.){
				tv = tmp;
			}
		}

		vector<Point> scan;
		track_junction(scan,node,tri[0],tv,'h','-',cnt[1]);
		//grid_draw("cc.2.tif",(*_img),NULL,&scan);

		std::reverse(scan.begin(),scan.end());
		scan.push_back(tri[0]);

		track_junction(scan,node,tri[0],tv,'h','+',cnt[0]);
		//grid_draw("cc.3.tif",(*_img),NULL,&scan);

		for(size_t i=0; i<scan.size(); i++){
			grid.at<Vec2i>(j,i) = Vec2i(scan[i]);
		}
	}
	cout<<endl;
	return grid;
}

void gridPick(
	FILE* fdDst,
	FILE* fdSrc,
	RawHead& hdSrc,
	Mat& grid
);
void gridMake(
	FILE* fdSrc,
	const char* imgPano,
	int scale,
	FILE* fdDst,
	const char* imgGrid
){
	RawHead hdSrc;
	fseek(fdSrc,0L,SEEK_SET);
	fread(&hdSrc,sizeof(hdSrc),1,fdSrc);

	Mat img;
	if(imgPano==NULL || scale<=0){
		scale = tear_tile_nxn(fdSrc,img,0,-1,-1,-1);
	}else{
		img = imread(imgPano,IMREAD_GRAYSCALE);//debug!!!
		if(img.empty()==true){
			//why???
			scale = tear_tile_nxn(fdSrc,img,0,-1,-1,-1);
			imwrite(imgPano,img);
		}
	}
	//img = imread("pano.4.jpg",IMREAD_GRAYSCALE); scale=2;//debug!!!
	//img = imread("pano.5.jpg",IMREAD_GRAYSCALE); scale=7;//debug!!!
	_img = &img;//debug~~~

	imgWidth = img.cols;
	imgHeight= img.rows;
	_grid_wd = gridWD/scale;
	_grid_hh = gridHH/scale;
	float g_val = std::min(_grid_wd,_grid_hh);
	_grid_bd[0] = (g_val*0.6);
	_grid_bd[1] = (g_val*1.4);
	g_val = std::max(_grid_wd,_grid_hh);
	_grid_bd[2] = (g_val*0.6);
	_grid_bd[3] = (g_val*1.4);

	Mat grd;
//#define USE_YML
#ifdef USE_YML
	FileStorage yml("grid.yml",FileStorage::READ);
	yml["grid"]>>grd;
	yml.release();
	//grd = grd / scale;
	//gridDraw("cc.0.jpg",img,&grd);
	//grd = grd * scale;
#else
	Point tri[3];
	vector<Point> node;
	analysis_junction(img,node);//the first node is in the center~~~

	tri[0] = node[0];
	cmpValue = node[0];
	std::sort(node.begin(),node.end(),cmpDistance);
	find_triangle(node,tri);

	mesh_junction(node);
	//mesh_junction(node);
	//grid_draw("cc.2.tif",(*_img),&node,NULL,NULL);

	grd = list_junction(
		node,
		tri[0],
		tri[1],tri[2]
	);
	if(imgGrid!=NULL){
		grid_draw("cc.3.tif",img,&node,tri,tri,&grd);
		grid_draw(imgGrid,img,&node,tri,tri,&grd);
	}
	grd = grd * scale;
	//FileStorage yml("grid.yml",FileStorage::WRITE);
	//yml<<"grid"<<grd;
	//yml.release();
#endif
	gridPick(fdDst,fdSrc,hdSrc,grd);

	grd.release();
	img.release();
}
//-----------------------//

#define FOR_EACH_GRID(grd,rows,cols) \
 for(size_t j=0; j<rows; j++){ \
 	for(size_t i=0; i<cols; i++){ \
		Point gtx[4]={ \
			Point(grd.at<Vec2i>(j  ,i  )), \
			Point(grd.at<Vec2i>(j  ,i+1)), \
			Point(grd.at<Vec2i>(j+1,i  )), \
			Point(grd.at<Vec2i>(j+1,i+1))}
#define FOR_EACH_GRID_END() }}

Mat get_affine(Point* tri,int ox,int oy){
	float w = hypot2f(tri[0],tri[1]);
	//float h = hypot2f(tri[0],tri[2]);
	Point cc(ox,oy);
	Point2f p0(tri[0].x,tri[0].y);
	Point2f p1(tri[1].x,tri[1].y);
	Point2f _p1(p0.x+w,p0.y);
	double deg = find_deg(p0,p1,_p1);
	return getRotationMatrix2D(cc,-deg,1);
	/*Point2f src[3] = {
		Point2f(tri[0].x-ox,tri[0].y-oy),
		Point2f(tri[1].x-ox,tri[1].y-oy),
		Point2f(tri[2].x-ox,tri[2].y-oy)
	};
	Point2f dst[3] = {
		Point2f(src[0]),
		Point2f(src[0].x+w,src[0].y),
		Point2f(src[0])
	};
	double deg1 = find_deg(dst[0],dst[1],src[1]);
	double deg2 = find_deg(src[0],src[1],src[2]);
	if(src[0].y<=src[1].y){
		deg1 = deg1 + deg2;
		deg1 = 90.-deg1;
		deg1 = (deg1*M_PI)/180.;
		dst[2].x = dst[2].x - h*sin(deg1);
	}else{
		deg1 = -deg1 + deg2;
	}
	dst[2].y = dst[2].y + h*cos(deg1);
	return getAffineTransform(src,dst);*/
}

Rect get_expand(Point* vtx,int ox,int oy){
	Rect roi;
	int lf = std::min(vtx[0].x,vtx[2].x);
	int rh = std::max(vtx[1].x,vtx[3].x);
	int tp = std::min(vtx[0].y,vtx[1].y);
	int bm = std::max(vtx[2].y,vtx[3].y);
	roi.x = lf - ox;
	roi.y = tp - oy;
	roi.width = rh - lf;
	roi.height= bm - tp;
	return roi;
}

bool valid_grid(Point* vtx){
	if(
		vtx[0].x<0 || vtx[0].y<0 ||
		vtx[1].x<0 || vtx[1].y<0 ||
		vtx[2].x<0 || vtx[2].y<0 ||
		vtx[3].x<0 || vtx[3].y<0
	){
		return false;
	}
	return true;
}

bool cover_grid(Point* vtx,Rect& roi){
	return roi.contains(vtx[0]) &&
		roi.contains(vtx[1]) &&
		roi.contains(vtx[2]) &&
		roi.contains(vtx[3]);
}

void gridPick(FILE* fdDst,FILE* fdSrc,RawHead& hdSrc,Mat& grid){
	if(fdDst==NULL){
		return;
	}
	RawHead hdDst;
	hdDst.type = hdSrc.type;
	hdDst.tileCols=grid.cols-1;
	hdDst.tileRows=grid.rows-1;
	hdDst.tileFlag=TILE_SEQUENCE;
	//check size~~
	float sum_gw=0.,sum_gh=0.,cnt_grd=0.;
	FOR_EACH_GRID(grid,hdDst.tileRows,hdDst.tileCols);
		if(valid_grid(gtx)==false){
			continue;
		}
		sum_gw = sum_gw + hypot2f(gtx[0],gtx[1]);
		sum_gh = sum_gh + hypot2f(gtx[0],gtx[2]);
		cnt_grd++;
	FOR_EACH_GRID_END()
	if(cnt_grd==0){
		cerr<<"no grid!!!"<<endl;
		return;
	}
	hdDst.cols=cvRound(sum_gw/cnt_grd);
	hdDst.rows=cvRound(sum_gh/cnt_grd);
	//prepare raw file~~~
	fwrite(&hdDst,sizeof(RawHead),1,fdDst);

	Rect zone(0,0,hdSrc.cols,hdSrc.rows);
	Mat buff(zone.size(),hdSrc.type);
	Mat cell(hdDst.rows,hdDst.cols,hdSrc.type);
	tear_tile_any(
		fdSrc,hdSrc,
		buff,
		zone.x,zone.y,
		1
	);//fill data, first~~
	//imwrite("cc.1.jpg",buff);

	FOR_EACH_GRID(grid,hdDst.tileRows,hdDst.tileCols);
		cell = cell * 0 ;//clear~~~
		if(valid_grid(gtx)==false){
			fwrite(cell.ptr(),cell.total(),cell.elemSize(),fdDst);
			continue;
		}
		if(cover_grid(gtx,zone)==false){
			//re-fill data!!!
			zone.x = gtx[0].x-hdDst.cols/2;
			if(zone.x<0){
				zone.x=0;
			}
			zone.y = gtx[0].y-hdDst.rows/2;
			if(zone.y<0){
				zone.y=0;
			}
			tear_tile_any(
				fdSrc,hdSrc,
				buff,
				zone.x,zone.y,
				1
			);//fill data, again~~
			//imwrite("cc.1.jpg",buff);
		}

		Rect roi = get_expand(gtx,zone.x,zone.y);
		if(check_roi(roi,buff)==false){
			fwrite(cell.ptr(),cell.total(),cell.elemSize(),fdDst);
			continue;
		}
		Mat aff = get_affine(
			gtx,
			roi.x+roi.width /2,
			roi.y+roi.height/2
		);
		warpAffine(
			buff(roi),
			cell,aff,
			cell.size(),
			INTER_LINEAR
		);
		aff.release();
		fwrite(cell.ptr(),cell.total(),cell.elemSize(),fdDst);

		//char name[500];
		//sprintf(name,"./cell/c[%zu_%zu].tiff",j,i);
		//imwrite(name,cell);
	FOR_EACH_GRID_END();
}
//-----------------------//

extern int findMaxBlob(vector<vector<Point> >& blob);
extern void drawBlob(const char* name,Mat& _img,vector<Point>& blob);

int probe_unity(Mat& ova,Mat& img,vector<Point>& cont){

	Mat edg(img.size(),CV_8UC1);
	threshold(img,edg,0,255,THRESH_OTSU|THRESH_BINARY);

	vector<vector<Point> > blob;
	findContours(edg,blob,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);

	Scalar red(0,0,255);
	Scalar grn(0,255,0);
	Scalar yow(0,200,200);

	size_t total = blob.size();
	if(total==0){
		line(ova,Point(0,0),Point(ova.cols,ova.rows),red,1);
		line(ova,Point(ova.cols,0),Point(0,ova.rows),red,1);
		return 1;
	}else if(total>1){
		for(size_t i=0; i<blob.size(); i++){
			drawContours(ova,blob,i,red,1);
		}
		return 2;
	}
	approxPolyDP(
		blob[0],
		cont,
		arcLength(blob[0],true)*0.1,
		true
	);
	if(cont.size()!=4){
		drawContours(ova,blob,0,red,1);
		blob.clear();
		blob.push_back(cont);
		drawContours(ova,blob,0,yow,1);
		return 3;
	}
	drawContours(ova,blob,0,grn,1);
	return 0;
}

void gridMeas(FILE* fdDst,const char* mapName,FILE* fdSrc){
	const char* blobName="blob.yml";
	const char* blobIdfy="blob-%zu";
	char name[500];

	RawHead hdSrc,hdDst;
	fseek(fdSrc,0L,SEEK_SET);
	fread(&hdSrc,sizeof(hdSrc),1,fdSrc);

	hdDst = hdSrc;
	hdDst.type = CV_8UC3;//it must be color!!!
	if(fdDst!=NULL){
		fwrite(&hdDst,sizeof(RawHead),1,fdDst);
	}

	Mat map(hdSrc.tileRows,hdSrc.tileCols,CV_8UC1);

	Mat src(hdSrc.rows,hdSrc.cols,hdSrc.type);
	Mat dst(hdSrc.rows,hdSrc.cols,hdDst.type);

	for(size_t j=0; j<hdSrc.tileRows; j++){
		for(size_t i=0; i<hdSrc.tileCols; i++){
			getTile(fdSrc,hdSrc,i,j,src);
			threshold(src,src,joinL2,255,THRESH_TOZERO);
			if(hdSrc.type==CV_8UC3){
				src.copyTo(dst);
			}else if(hdSrc.type==CV_8UC1){
				cvtColor(src,dst,COLOR_GRAY2BGR);
			}else{
				dst = dst*0;
			}
			if(countNonZero(src)==0){
				map.at<uint8_t>(j,i) = '-';//mark it!!!
				if(fdDst!=NULL){
					fwrite(dst.ptr(),dst.total(),dst.elemSize(),fdDst);
				}
				continue;
			}
			vector<Point> cont;
			char tkn = 'A';
			tkn = tkn + probe_unity(dst,src,cont);
			map.at<uint8_t>(j,i) = tkn;
			if(fdDst!=NULL){
				fwrite(dst.ptr(),dst.total(),dst.elemSize(),fdDst);
			}
		}
	}

	FileStorage ymlMap(mapName,FileStorage::WRITE);
	ymlMap<<"map"<<map;
	ymlMap.release();
}
//-----------------------//

void grid_draw(
	const char* name,
	Mat& _img,
	vector<Point> *_node,
	const Point* _tri,
	Point* _cc,
	Mat* _grid
){
	bool flag=false;
	Mat img;
	if(img.channels()==1){
		img.create(_img.size(),CV_8UC3);
		cvtColor(_img,img,COLOR_GRAY2BGR);
		flag = true;
	}else if(img.channels()==3){
		img=_img;
	}
	Scalar red(0,0,200);
	Scalar cyn(200,0,200);
	Scalar yow(0,200,200);
	Scalar grn(0,200,0);
	if(_node!=NULL){
		vector<Point>& node = *_node;
		for(size_t i=0; i<node.size(); i++){
			circle(img,node[i],10,yow,3);
		}
	}
	if(_tri!=NULL){
		Point tmp;;
		circle(img,_tri[0],7,cyn,2);
		tmp=_tri[0];
		tmp.y+=30;
		putText(img,"0",tmp,FONT_HERSHEY_PLAIN,1.,cyn);
		circle(img,_tri[1],7,cyn,2);
		tmp=_tri[1];
		tmp.y+=30;
		putText(img,"1",_tri[1],FONT_HERSHEY_PLAIN,1.,cyn);
		circle(img,_tri[2],7,cyn,2);
		tmp=_tri[2];
		tmp.y+=30;
		putText(img,"2",_tri[2],FONT_HERSHEY_PLAIN,1.,cyn);
	}
	if(_cc!=NULL){
		circle(img,(*_cc),11,red,3);
	}
	if(_grid!=NULL){
		Mat& grid=*_grid;
		FOR_EACH_GRID(grid,grid.rows-1,grid.cols-1);
			if(valid_grid(gtx)==false){
				continue;
			}
			circle(img,gtx[0],10,grn,3);
			//rectangle(img,gtx[0],gtx[3],grn,1);
			char txt[200];
			sprintf(txt,"%zu_%zu",j,i);
			gtx[0].y = gtx[0].y + 40;
			putText(img,txt,gtx[0],FONT_HERSHEY_PLAIN,1.,grn);
		FOR_EACH_GRID_END()
	}
	imwrite(name,img);
	if(flag==true){	img.release(); }
}


