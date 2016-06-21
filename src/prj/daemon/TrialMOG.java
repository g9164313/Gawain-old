package prj.daemon;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.SimpleStringProperty;
import narl.itrc.DevGrabber;
import narl.itrc.Misc;
import narl.itrc.DirSN;

public class TrialMOG extends DevGrabber {
	
	private final String DIR_BASE=Misc.pathTemp+wndName;
	//private final String DIR_BACK=DIR_BASE+File.separator+"back"+File.separator;
	//private final String DIR_FORE=DIR_BASE+File.separator+"fore"+File.separator;
	//private final String DIR_MEAS=DIR_BASE+File.separator+"meas"+File.separator;
	private final DirSN dirBack = new DirSN(DIR_BASE+File.separator+"back","img%.png");
	private final DirSN dirFore = new DirSN(DIR_BASE+File.separator+"fore","img%.png");
	private final DirSN dirMeas = new DirSN(DIR_BASE+File.separator+"meas","img%.png");
	
	private SimpleStringProperty msgCount = new SimpleStringProperty("???/???");
	private SimpleStringProperty msgStatus= new SimpleStringProperty("--------");	

	private long ptrModBack= 0L;//influenced by native code
	private long ptrModFore= 0L;//influenced by native code
	private int[] objInfo ={-1,-1,0,0,0};//influenced by native code,
	private native void modDump(long ptrMod,String name);
	private native void modUpdateSize(String name,int[] info);
	private native void modAdminPtr(char tkn,long hist,double varThres,boolean detShadow);	
	private native void modApplyFile(long ptrMod,String imgName,String mskName,double learn);
	private native void cutOutBounding(		
		long srcImage,String srcName,
		long dstImage,String dstName
	);
	private native boolean modMeasurement(
		long ptrModBack,long ptrModFore,
		int objWidth,int objHeight,
		long srcImage,String srcName,
		long dstImage,String dstObjName
	);

	public TrialMOG(String name){
		super(name);
		init();
	}
	public TrialMOG(String name,int type){
		super(name,type);
		init();
	}
	
	private void init(){
		checkInfo1(true);
		checkInfo1(false);
	}
	
	@Override
	protected void eventShutdown(){
		modAdminPtr('x',0,0,false);//release model~~~
	}
	
	private final int STA_IDLE=0;
	private final int STA_TRAN=1;
	private final int STA_MEAS=2;
	private AtomicInteger state = new AtomicInteger(STA_IDLE);
	
	@Override
	protected void graberStart(){
		tskTrain();
	}
		
	@Override
	protected void graberImage(long pDat, long pLay) {
		switch(state.get()){
		case STA_IDLE:
			tskDispatch(pDat,pLay);
			info1Property();
			break;
		case STA_TRAN:			
			tskTrain();
			state.set(STA_IDLE);//idle again~~~
			break;
		case STA_MEAS:
			tskMeasure(pDat,pLay);
			state.set(STA_IDLE);//idle again~~~
			break;
		}
	}
	
	private void tskDispatch(long pDat,long pLay){
		if(pipeMsg.length()==0){
			return;
		}
		String[] tkn = pipeMsg.toUpperCase().split(",");
		if(tkn[0].startsWith("CLR")==true){
			if(tkn.length>=2){
				if(tkn[1].equals("BACK")==true){
					clearDir(true);
				}else if(tkn[1].equals("FORE")==true){
					clearDir(false);
				}
			}else{
				//clear all data~~~
				clearDir(true);
				clearDir(false);
			}
			pipeMsg="DONE";
		}else if(tkn[0].startsWith("SAVE")==true){
			if(tkn.length>=2){
				if(tkn[1].equals("BACK")==true){
					Misc.imWrite(dirBack.genSNTxt(), pDat);
					checkInfo1(true);					
				}else if(tkn[1].equals("FORE")==true){
					Misc.imWrite(dirFore.genSNTxt(), pDat);
					checkInfo1(false);
				}
			}
			pipeMsg="DONE";
		}else if(tkn[0].startsWith("TRAN")==true){
			tskTrain();
			pipeMsg="DONE";
		}else if(tkn[0].startsWith("MEAS")==true){
			tskMeasure(pDat,pLay);
			pipeMsg="DONE";
		}
	}
	
	private void tskMeasure(long pDat,long pLay){
		if(
			objInfo[0]<=0 || objInfo[1]<=0 || 
			ptrModBack==0 || ptrModFore==0
		){
			msgStatus.setValue("內部錯誤");
			return;
		}
		String img_name = dirMeas.genSNTxt();
		String obj_name = dirMeas.getSNTxt("obj");
		boolean isOK = modMeasurement(
			ptrModBack,ptrModFore,
			objInfo[0],objInfo[1],
			pDat,img_name,
			pLay,obj_name
		);
		if(isOK==true){
			msgStatus.setValue("OK");
		}else{
			msgStatus.setValue("NG");
		}
	}
	
	private void tskTrain(){
		String[] lst=null;
		////first step - train background image////
    	lst = dirBack.getListName(null);
    	if(lst.length==0){
    		msgStatus.setValue("無背景資料!!!");
    		return;
    	}
    	msgStatus.setValue("訓練背景");
    	modAdminPtr('b',lst.length,16,true);
		for(String name:lst){
			modApplyFile(ptrModBack,dirBack.getFullName(name),"",-1.);
		}
		
		////next step - train foreground & target////
		lst = dirFore.getListName("obj%.png");//check whether we have target to train model~~~
		if(lst.length==0){			
			lst = tskCutOutObj();			
			if(lst==null){
				return;
			}
		}
		
		msgStatus.setValue("訓練目標");
		modAdminPtr('f',lst.length,200,true);
		for(String name:lst){
			modApplyFile(ptrModFore,dirFore.getFullName(name),"",-1.);
		}
		modUpdateSize(dirFore.getFullName(lst[0]),objInfo);
		
		msgStatus.setValue("訓練結束");
		modDump(ptrModBack,DIR_BASE+File.separator+"background.png");
		modDump(ptrModFore,DIR_BASE+File.separator+"foreground.png");
	}
	
	private String[] tskCutOutObj(){
		String[] lst = dirFore.getListName("img%.png");
		if(lst.length==0){
    		msgStatus.setValue("無前景資料!!!");
    		return null;
    	}
		
		msgStatus.setValue("修剪目標");
		objInfo[0] = objInfo[1] = -1;//remember to reset this!!!
		objInfo[2] = objInfo[3] = objInfo[4] = 0;
		for(String name:lst){
			cutOutBounding(
				0,dirFore.getFullName(name),
				0,null
			);
		}
		objInfo[0] = objInfo[2] / objInfo[4];//average width~~~
		objInfo[1] = objInfo[3] / objInfo[4];//average height~~~
		msgStatus.setValue("裁切目標-"+objInfo[0]+"x"+objInfo[1]);
		for(String name:lst){
			cutOutBounding(
				0,dirFore.getFullName(name),
				0,dirFore.getFullName("obj"+name.substring(3))
			);
		}
		
		lst = dirFore.getListName("obj%.png");//get list again~~~~
		if(lst.length==0){
    		msgStatus.setValue("無目標資料!!!");
    		return null;
    	}
		return lst;
	}
		
	public void cmdTrain(){
		nextState(STA_TRAN);
	}
	
	public void cmdMeasure(){
		nextState(STA_MEAS);
	}
	
	private void nextState(int sta){
		if(state.get()!=STA_IDLE){
			return;
		}
		state.set(sta);
	}
	
    public String getSrcName() {
        return wndName;
    }
    
    public SimpleStringProperty info1Property(){
    	if(isTaping()==false){
    		return msgCount;
    	}
    	checkInfo1(tapeBackground);
    	return msgCount;
    }

    public SimpleStringProperty info2Property(){
    	return msgStatus;
    }
    
    private boolean tapeBackground = true;
    public void recordStart(boolean isBack){
    	tapeBackground = isBack;//keep this flag~~~
    	if(isBack==true){
    		tapeFile(dirBack.getFullName("img.png"),-1);
    	}else{    		
    		tapeFile(dirFore.getFullName("img.png"),-1);
    	}
    }
    public void recordStop(){
    	tapeFile("");
    	checkInfo1(tapeBackground);
    }

    private void checkInfo1(boolean isBack){
		String txt = msgCount.get();
		int pos = txt.indexOf("/");
		if(isBack==true){
			txt = txt.substring(pos+1);
			msgCount.set(String.format("%d/%s",dirBack.countFile(null),txt));
		}else{
			txt = txt.substring(0,pos);
			msgCount.set(String.format("%s/%d",txt,dirFore.countFile(null)));
		}
	}
    public void clearDir(boolean isBack){
    	if(isBack){
    		dirBack.clearData();
    	}else{
    		dirFore.clearData();
    	}
    	checkInfo1(isBack);
    }
};
