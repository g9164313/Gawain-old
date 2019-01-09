#include <global.hpp>

#ifndef PEEK_H
#define PEEK_H

#define DESC_BOUNDING 1
#define DESC_SPECKLE 2

struct _MarkDefect {
	int desc;
	Vec4i arg;
	Point pts1;
	Point pts2;
};

typedef struct _MarkDefect MarkDefect;

inline void printMark(MarkDefect& mk){
	int val = 0;
	switch(mk.desc){
	case DESC_BOUNDING:
		val = hypot(
			mk.pts2.x-mk.pts1.x,
			mk.pts2.y-mk.pts1.y
		);
		printf(
			"[bound] (%d,%d)-(%d,%d)@%d - Far:%d",
			mk.pts1.x,mk.pts1.y,
			mk.pts2.x,mk.pts2.y,
			val, mk.arg[2]
		);
		break;
	case DESC_SPECKLE:
		printf(
			"[speckle] (%d,%d)@%d - Area:%d",
			mk.arg[0],mk.arg[1],
			mk.arg[2],mk.arg[3]
		);
		break;
	}
	cout<<endl;
}

#define PARM_INFO_WIDTH    101 //基本資料
#define PARM_INFO_HEIGHT   102
#define PARM_INFO_CENTER_X 103
#define PARM_INFO_CENTER_Y 104
#define PARM_DUMP_DEFECT   201
#define PARM_DUMP_ORIGIN   202
#define PARM_DUMP_SPECKLE  203
#define PARM_SCALE_X   1 //尋找目標時縮放原始圖的比例
#define PARM_SCALE_Y   2
#define PARM_SPACE_X   3 //決定目標周圍要留多少空白
#define PARM_SPACE_Y   4
#define PARM_TRANPOSE  5 //影像旋轉90度
#define PARM_BOUND     6 //邊界檢測 - 是否執行
#define PARM_BOUND_FAR 8 //邊界檢測 - 允許離理想矩形的最小值(pixel)
#define PARM_BOUND_LEN 9 //邊界檢測 - 允許最大的破碎長度
#define PARM_SPECKLE   7 //暗點檢測 - 是否執行
#define PARM_SPECKLE_INT 10 //暗點檢測 - 暗點的強度
#define PARM_SPECKLE_RAD 11 //暗點檢測 - 允許speckle最小的半徑
#define PARM_SPECKLE_ARE 12 //暗點檢測 - 允許speckle最小的面積，注意，這個數值是精確到小數點以下一位，也就是5代表0.5

NAT_EXPORT int squareIdentify(void* data,int width,int height);
NAT_EXPORT void squareRelease();
NAT_EXPORT int squareGetMark(MarkDefect* buf,int off,int size);
NAT_EXPORT long squareGet(int id);
NAT_EXPORT void squareSet(int id,long val);

#endif


