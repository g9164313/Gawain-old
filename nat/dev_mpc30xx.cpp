#include <global.hpp>
#if defined _MSC_VER
#include <MPC3024A.h>

#define MAX_AXIS 4
#define ADDR_RELATIVE 0
#define ADDR_ABSOLUTE 1

static u32 paramWait=0;
static bool useTrapezoidal = true;

u8 char2axis(jchar cc){
	switch(cc){
	case 'x':
	case 'X':
		return AXIS_X;
	case 'y':
	case 'Y':
		return AXIS_Y;
	case 'z':
	case 'Z':
		return AXIS_Z;
	case 'a':
	case 'A':
		return AXIS_A;
	}
	return 0xFF;
}

NAT_EXPORT void set_position(int id,jint* pos,jint* cnt){
	for(u8 aid=0; aid<MAX_AXIS; aid++){
		MPC3024A_current_position_set((u8)id,aid,(i32)(pos[aid]));
		MPC3024A_FB_counter_set((u8)id,aid,(i32)(cnt[aid]));
	}
}

NAT_EXPORT void get_info(int id,jint* pos,jint* cnt,jint* sta){
	for(u8 aid=0; aid<MAX_AXIS; aid++){
		i32 tmp;

		MPC3024A_current_position_read((u8)id,aid,&tmp);
		if(pos!=NULL){ pos[aid] = (jint)tmp; }

		MPC3024A_FB_counter_read((u8)id,aid,&tmp);
		if(cnt!=NULL){ cnt[aid] = (jint)tmp; }

		if(sta!=NULL){			
			sta[aid]=0;//reset it~~~
			for(int chk=0; chk<=16; chk++){
				u8 flag;
				MPC3024A_point_read((u8)id,aid,chk,&flag);
				tmp = flag;
				tmp = tmp << chk;
				sta[aid]=sta[aid]|tmp;
			}
		}
	}
}

NAT_EXPORT bool is_dull(int id){
	i32 pre[4],cnt[4];
	for(u8 aid=0; aid<MAX_AXIS; aid++){
		MPC3024A_FB_counter_read((u8)id,aid,cnt+aid);
		pre[aid] = cnt[aid];
	}
	msleep(200);
	for(u8 aid=0; aid<MAX_AXIS; aid++){
		MPC3024A_FB_counter_read((u8)id,aid,cnt+aid);
		if( pre[aid]!=cnt[aid] ){
			return false;
		}
	}
	return true;
}

NAT_EXPORT void set_jogger(
	int id,
	jint* pam,
	jchar axs,jchar op
){
	u8 aid = char2axis(axs);		
	if(aid==0xFF){
		return;
	}
	u8 tkn[4]={1,1,1,1};//stop all axis
	switch(op){
	case '>': 
	case '+':
	case 'f':
	case 'F':
		MPC3024A_T_velocity_move(
			id,aid,
			(i32)(pam[0]),
			(i32)(pam[1]),
			(i32)(pam[2])
		);
		break;
	case '0':
	case 'o':
	case 'O':
	case '.':		
		MPC3024A_stop(id,tkn,100);
		break;
	case '<': 
	case '-':
	case 'r':
	case 'R':
		MPC3024A_T_velocity_move(
			id,aid,
			(i32)(-pam[0]),
			(i32)(-pam[1]),
			(i32)(pam[2])
		);
		break;
	}
}

NAT_EXPORT void set_motion(
	jint id,jint code,	
	jint* val,jint* pam,
	u8 addr
){
	u8 mid=(code&0xFF00)>>8;
	u8 aid=(code&0x00FF);
	u8 adr[]={addr,addr,addr,addr};
	if(useTrapezoidal==true){
		switch(mid){
		case 1:			
			MPC3024A_T_position_move(
				(u8)id,aid,(i32)(val[0]),addr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				paramWait
			);
			break;
		case 2:
			MPC3024A_T_LINE2_move(
				(u8)id,aid,(i32)(val[0]),(i32)(val[1]),adr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				paramWait
			);
			break;
		case 3:
			MPC3024A_T_LINE3_move(
				(u8)id,aid,(i32)(val[0]),(i32)(val[1]),(i32)(val[2]),adr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				paramWait
			);
			break;
		case 4:
			MPC3024A_T_LINE4_move(
				(u8)id,(i32)(val[0]),(i32)(val[1]),(i32)(val[2]),(i32)(val[3]),adr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				paramWait
			);
			break;
		}
	}else{
		switch(mid){
		case 1:			
			MPC3024A_S_position_move(
				(u8)id,aid,(i32)(val[0]),addr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				(u32)(pam[4]),(u32)(pam[5]),
				paramWait
			);
			break;
		case 2:
			MPC3024A_S_LINE2_move(
				(u8)id,aid,(i32)(val[0]),(i32)(val[1]),adr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				(u32)(pam[4]),(u32)(pam[5]),
				paramWait
			);
			break;
		case 3:
			MPC3024A_S_LINE3_move(
				(u8)id,aid,(i32)(val[0]),(i32)(val[1]),(i32)(val[2]),adr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				(u32)(pam[4]),(u32)(pam[5]),
				paramWait
			);
			break;
		case 4:
			MPC3024A_S_LINE4_move(
				(u8)id,(i32)(val[0]),(i32)(val[1]),(i32)(val[2]),(i32)(val[3]),adr,
				(u32)(pam[0]),(u32)(pam[1]),
				(u32)(pam[2]),(u32)(pam[3]),
				(u32)(pam[4]),(u32)(pam[5]),
				paramWait
			);
			break;
		}
	}
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_DevMPC30xx_setPosition(
	JNIEnv * env, 
	jobject thiz,
	jint id
){
	jintArray jpos,jcnt;	
	jclass _clazz = env->GetObjectClass(thiz);	
	jint* pos = ArrayInt2Ptr(env,_clazz,thiz,"pos",jpos);
	jint* cnt = ArrayInt2Ptr(env,_clazz,thiz,"cnt",jcnt);
	set_position(id,pos,cnt);
	env->ReleaseIntArrayElements(jpos,pos,0);
	env->ReleaseIntArrayElements(jcnt,cnt,0);
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_DevMPC30xx_getInfo(
	JNIEnv * env, 
	jobject thiz,
	jint id
){
	jintArray jpos,jcnt,jsta;	
	jclass _clazz = env->GetObjectClass(thiz);
	jint* pos = ArrayInt2Ptr(env,_clazz,thiz,"pos",jpos);
	jint* cnt = ArrayInt2Ptr(env,_clazz,thiz,"cnt",jcnt);
	jint* sta = ArrayInt2Ptr(env,_clazz,thiz,"sta",jsta);

	get_info(id,pos,cnt,sta);

	env->ReleaseIntArrayElements(jpos,pos,0);
	env->ReleaseIntArrayElements(jcnt,cnt,0);
	env->ReleaseIntArrayElements(jsta,sta,0);
}

extern "C" JNIEXPORT jboolean JNICALL Java_prj_epistar_DevMPC30xx_isDull(
	JNIEnv * env, 
	jobject thiz,
	jint id
){
	jintArray jpos,jcnt,jsta;	
	jclass _clazz = env->GetObjectClass(thiz);
	jint* pos = ArrayInt2Ptr(env,_clazz,thiz,"pos",jpos);
	jint* cnt = ArrayInt2Ptr(env,_clazz,thiz,"cnt",jcnt);
	jint* sta = ArrayInt2Ptr(env,_clazz,thiz,"sta",jsta);

	bool flag = is_dull(id);
	get_info(id,pos,cnt,sta);

	env->ReleaseIntArrayElements(jpos,pos,0);
	env->ReleaseIntArrayElements(jcnt,cnt,0);
	env->ReleaseIntArrayElements(jsta,sta,0);

	if(flag==true){
		return JNI_TRUE;
	}
	return JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_DevMPC30xx_setMotion(
	JNIEnv * env, 
	jobject thiz,
	jint id,
	jint code,
	jboolean block
){
	jclass _clazz = env->GetObjectClass(thiz);
	jintArray jpos,jcnt,jpam,jsta;	
	jint* pos = ArrayInt2Ptr(env,_clazz,thiz,"pos",jpos);
	jint* cnt = ArrayInt2Ptr(env,_clazz,thiz,"cnt",jcnt);
	jint* sta = ArrayInt2Ptr(env,_clazz,thiz,"sta",jsta);	
	jint* pam = ArrayInt2Ptr(env,_clazz,thiz,"pam",jpam);

	jfieldID jaddr = env->GetFieldID(_clazz,"addr","I");
	u8 addr = (u8)(env->GetIntField(thiz,jaddr));//reload it always~~~~

	set_motion(id,code,pos,pam,addr);
	if(block==JNI_TRUE){
		while(is_dull(id)==false);
	}
	get_info(id,pos,cnt,sta);//update again~~~~

	env->ReleaseIntArrayElements(jpos,pos,0);
	env->ReleaseIntArrayElements(jcnt,cnt,0);
	env->ReleaseIntArrayElements(jsta,sta,0);
	env->ReleaseIntArrayElements(jpam,pam,0);	
}

extern "C" JNIEXPORT void JNICALL Java_prj_epistar_DevMPC30xx_setJogger(
	JNIEnv * env, 
	jobject thiz,
	jint id,
	jchar axs,
	jchar op
){
	jclass _clazz = env->GetObjectClass(thiz);
	jintArray jpam,jsta;
	jint* sta = ArrayInt2Ptr(env,_clazz,thiz,"sta",jsta);
	jint* pam = ArrayInt2Ptr(env,_clazz,thiz,"pam",jpam);
	
	set_jogger(id,pam,axs,op);

	env->ReleaseIntArrayElements(jsta,sta,0);
	env->ReleaseIntArrayElements(jpam,pam,0);	
}

extern "C" JNIEXPORT jint JNICALL Java_prj_epistar_DevMPC30xx_close(
	JNIEnv * env, 
	jobject thiz
){ 	
	return MPC3024A_close();
}

extern "C" JNIEXPORT jint JNICALL Java_prj_epistar_DevMPC30xx_open(
	JNIEnv * env, 
	jobject thiz,
	jint id
){
	u32 res = MPC3024A_initial();
	if(res!=JSDRV_NO_ERROR){		
		return res;
	}
	res = MPC3024A_init_card((u8)id);
	if(res!=JSDRV_NO_ERROR){
		return res;
	}
	
	res = MPC3024A_ALM_PIN_set((u8)id,AXIS_X,1,0);
	res = MPC3024A_ALM_PIN_set((u8)id,AXIS_Y,1,0);
	res = MPC3024A_ALM_PIN_set((u8)id,AXIS_Z,1,0);
	res = MPC3024A_ALM_PIN_set((u8)id,AXIS_A,1,0);
	
	res = MPC3024A_pulse_outmode_set((u8)id,AXIS_X,6);
	res = MPC3024A_pulse_outmode_set((u8)id,AXIS_Y,6);
	res = MPC3024A_pulse_outmode_set((u8)id,AXIS_Z,6);
	res = MPC3024A_pulse_outmode_set((u8)id,AXIS_A,6);

	//servo-on
	res = MPC3024A_point_set((u8)id,AXIS_X,1,1);
	res = MPC3024A_point_set((u8)id,AXIS_Y,1,1);
	res = MPC3024A_point_set((u8)id,AXIS_Z,1,1);
	res = MPC3024A_point_set((u8)id,AXIS_A,1,1);
	//MPC3024A_4Axis_restart((u8)id);
	MPC3024A_current_position_set((u8)id,AXIS_X,0);
	MPC3024A_current_position_set((u8)id,AXIS_Y,0);
	MPC3024A_current_position_set((u8)id,AXIS_Z,0);
	MPC3024A_current_position_set((u8)id,AXIS_A,0);

	MPC3024A_FB_counter_set((u8)id,AXIS_X,0);
	MPC3024A_FB_counter_set((u8)id,AXIS_Y,0);
	MPC3024A_FB_counter_set((u8)id,AXIS_Z,0);
	MPC3024A_FB_counter_set((u8)id,AXIS_A,0);

	u8 tkn[4]={1,1,1,1};//this is a patch for velocity motion!!!!
	MPC3024A_stop((u8)id,tkn,0);
	
	Java_prj_epistar_DevMPC30xx_getInfo(env,thiz,id);
	return 0;
}
#endif
