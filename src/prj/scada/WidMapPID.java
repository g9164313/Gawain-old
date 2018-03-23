package prj.scada;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import eu.hansolo.medusa.Gauge.SkinType;
import narl.itrc.Misc;
import narl.itrc.WidDiagram;
import narl.itrc.WidValueNote;

public class WidMapPID extends WidDiagram {

	public WidMapPID(){
		super(18,16);
		default_layout();		
		redrawAll();
	}
	
	//rotator轉盤 <---> M121, Y18 
	//baffle 擋板 <---> M(Y)38
	//m-valve1(MV1) <---> M(Y)4
	//m-valve2(MV2) <---> M(Y)14
	//r-valve(RV) <---> M(Y)3
	//f-valve1(FV1) <---> M(Y)2
	//f-valve2(FV2) <---> M(Y)13
	//VV  <---> M(Y)5
	//MP  <---> M(Y)20
	//CP1 <---> M(Y)33
	//CP2 <---> M(Y)37
	//SPI-2K <---> M(Y)128
	//RF1    <---> M(Y)32
	//DC1    <---> M(Y)34
	//DC2    <---> M(Y)35
		
	//R????(R1016) --> 轉盤速度
	//R1131 --> 設定轉盤速度 (0~60 RPM)
	//R????(R1007) --> 腔體溫度
	//R????(R1032) --> 基板溫度
	//R####(R1038) --> 讀取 chiller 溫度, 固定點小數一位
	//R####(R1138) --> 設定 chiller 溫度, 固定點小數一位
	//R2000(R1004) --> chamber pressure(right) [MKS626B]
	//R3849(R1024) --> chamber pressure(left，陰極管) [PKR251]
	//R3848(R1028) --> CP1/TEMP
	//R3853(R1025) --> CP1/pressure [TPR 280]
	//R3856(R1034) --> CP2/TEMP
	//R3857(R1035) --> CP2/pressure [TPR 280]
	//R3854(R1000) --> Ar pressure
	//R3858(R1001) --> O2 pressure
	//R????(R1002) --> N2 pressure
	//R1026 --> 加熱器電流
	//R1027 --> 離心機電流
	//SCCM 設定 R1100-Ar, R1101-O2, R1102-N2,
	//APC 設定 M(Y)122 --> on:氣壓固定, off:位置固定
	//APC 設定 R1104(R1004):氣壓數值(固定點一位), R1105(R1005):蝴蝶閥位置
	
	//CRV1(氮氣閥門跟加熱),再生用(purge,re-purge)<---> M(Y)23 
	//CRV2(氮氣閥門跟加熱),再生用(purge,re-purge)<---> M(Y)21	
	//01 41 1 <-- PLC RUN
	//01 40 <-- PLC 讀取
	
	public void hookPart(DevFatek dev){
		//trick, we will manually initial all parts again.....
		
		Arb.setRegist(dev, "R01100");
		O2b.setRegist(dev, "R01101");
		N2b.setRegist(dev, "R01102");
		
		Arv.hookRelay(dev, 9);		
		O2v.hookRelay(dev, 10);		
		N2v.hookRelay(dev, 11);
		
		gv1.hookRelay(dev, 12);
		gv2.hookRelay(dev, 15);
		gv3.hookRelay(dev, 36);	
		
		mv1.hookRelay(dev, 4);
		apc.hookRelay(dev, 122, 1104, 1105);
		fv1.hookRelay(dev, 1);
		
		mv2.hookRelay(dev, 14);		
		fv2.hookRelay(dev, 13);
		
		rv.hookRelay(dev, 3);
		vv.hookRelay(dev, 5);
		
		mp1.hookRelay(dev, 20);
		cp1.hookRelay(dev, 33);
		cp2.hookRelay(dev, 37);
		
		towerLamp.hookRelay(dev);
		
		chuck.hookRelay(dev, 121, 18);
		chuck.temp.bind(prop_Temp_Chuck(dev));
		chuck.rota.bind(dev.getMarker("R1016").multiply(1));
		
		baffle.hookRelay(dev, 38);
		
		burner_dc1.hookRelay(dev, 128, 34, 1112);
		burner_dc1.attr[0].bind(dev.getMarker("R1010").multiply(1));
		burner_dc1.attr[1].bind(dev.getMarker("R1011").multiply(1));
		burner_dc1.attr[2].bind(dev.getMarker("R1012").multiply(1));
		
		chiller.setRegist(dev, "R01138", "R01038");
		
		ampHeater.bind(dev.getMarker("R1026").multiply(1));
		ampMP1.bind(dev.getMarker("R1027").multiply(1));
		
		presAr.bind(prop_Press_Ar(dev));
		presO2.bind(prop_Press_O2(dev));
		presN2.bind(prop_Press_N2(dev));
		
		tempChiller.bind(prop_Temp_Chiller(dev));
		tempChamber2.bind(prop_Temp_Chamber(dev));
		
		presChamber1.bind(prop_Press_CH1(dev));
		presChamber2.bind(prop_Press_CH2(dev),ptxt_Press_CH2(dev));
		
		tempCP1.bind(prop_Temp_CP1(dev));
		tempCP2.bind(prop_Temp_CP2(dev));
		presCP1.bind(prop_Press_CP1(dev), ptxt_Precc_CP1(dev));
		presCP2.bind(prop_Press_CP2(dev), ptxt_Precc_CP2(dev));
	}
			
	private NumberBinding prop_Temp_CP1(DevFatek dev){
		return prop_cp_temp(dev,"R1028");
	}
	private NumberBinding prop_Temp_CP2(DevFatek dev){
		return prop_cp_temp(dev,"R1034");
	}
	private NumberBinding prop_cp_temp(DevFatek dev,String name){
		return dev.getMarker(name).multiply(1);
	}
	
	private NumberBinding prop_Temp_Chamber(DevFatek dev){
		return dev.getMarker("R1032").multiply(0.1f);
	}
	private NumberBinding prop_Temp_Chuck(DevFatek dev){
		return dev.getMarker("R1007").multiply(0.1f);
	}	
	private NumberBinding prop_Temp_Chiller(DevFatek dev){
		return dev.getMarker("R1038").multiply(0.1f);
	}
	
	private NumberBinding prop_Press_Ar(DevFatek dev){
		return dev.getMarker("R1000").divide(8192.f).multiply(100.f);
	}
	private NumberBinding prop_Press_O2(DevFatek dev){
		return dev.getMarker("R1001").divide(8192.f).multiply(40.f);
	}
	private NumberBinding prop_Press_N2(DevFatek dev){
		return dev.getMarker("R1002").divide(8192.f).multiply(4.f);
	}
	
	private NumberBinding prop_Press_CH1(DevFatek dev){
		return dev.getMarker("R1004").multiply(0.1f);//MKS626B 電容絕對壓力計
	}
	private NumberBinding prop_Press_CH2(DevFatek dev){
		FloatProperty val = new SimpleFloatProperty();		
		dev.getMarker("R1024").addListener((observable,preVal,curVal)->{
			val.set((float)press_ch2(curVal));			
		});
		return val.multiply(1.f);
	}
	private StringProperty ptxt_Press_CH2(DevFatek dev){
		StringProperty txt = new SimpleStringProperty("???Torr");		
		dev.getMarker("R1024").addListener((observable,preVal,curVal)->{
			txt.set(Misc.num2prefix(press_ch2(curVal),2)+"Torr");			
		});
		return txt;
	}
	private double press_ch2(Number curVal){
		double volt = (curVal.floatValue() / 8192.f) * 10.f;
		double pres = Math.pow(10., (1.667*volt-11.46));
		if(pres<3.8E-9){
			pres = 3.8E-9;
		}
		if(750<pres){
			pres = 750.;
		}
		return pres;//unit is 'Torr'
	}
	
	private NumberBinding prop_Press_CP1(DevFatek dev){
		return prop_press_calc(dev,"R1025");
	}
	private NumberBinding prop_Press_CP2(DevFatek dev){
		return prop_press_calc(dev,"R1035");
	}
	private NumberBinding prop_press_calc(DevFatek dev,String name){
		FloatProperty val = new SimpleFloatProperty();		
		dev.getMarker(name).addListener((observable,preVal,curVal)->{			
			val.set((float)press_cp(curVal));			
		});
		return val.multiply(1.f);
	}
	private StringProperty ptxt_Precc_CP1(DevFatek dev){
		return ptxt_press_cp(dev,"R1025");
	}
	private StringProperty ptxt_Precc_CP2(DevFatek dev){
		return ptxt_press_cp(dev,"R1035");
	}
	private StringProperty ptxt_press_cp(DevFatek dev,String name){
		StringProperty txt = new SimpleStringProperty("???Torr");		
		dev.getMarker(name).addListener((observable,preVal,curVal)->{
			txt.set(Misc.num2prefix(press_cp(curVal))+"Torr");			
		});
		return txt;
	}
	private double press_cp(Number curVal){
		double volt = (curVal.floatValue() / 8192.f) * 10.f;
		//double pres = Math.pow(10., (volt-5.625f));//TPR280 手冊裡提供的公式，感恩咕狗，讚嘆咕狗
		double pres = Math.pow(10., ((volt-6.304f)/1.286f));//高敦操作程式裡的公式
		if(pres<3.75E-4){
			pres = 3.75E-4;
		}
		if(750<pres){
			pres = 750.;
		}
		return pres;//unit is 'Torr'
	}	
	//-----------------------------------------//
	
	protected class ItmTowerLamp extends ItemBrick {
		public ItmTowerLamp(double gx, double gy) {
			super(CATE_TOWER, gx, gy);
		}
		private IntegerProperty Y29, Y30, Y31;
		private ChangeListener<Number> event = new ChangeListener<Number>(){
			@Override
			public void changed(
				ObservableValue<? extends Number> observable, 
				Number oldValue,
				Number newValue
			) {
				clear();
				draw(0);
				if(Y29.get()==1){
					draw(3);//red light
				}
				if(Y30.get()==1){
					draw(2);//yellow light
				}
				if(Y31.get()==1){
					draw(1);//green light
				}
			}
		};
		private DevFatek dev;
		public void hookRelay(DevFatek dev){
			this.dev = dev;
			Y29 = dev.getMarker("Y0029");
			Y30 = dev.getMarker("Y0030");
			Y31 = dev.getMarker("Y0031");
			Y29.addListener(event);
			Y30.addListener(event);
			Y31.addListener(event);
		}
		public void setRed(){
			set(false,false,true);
		}
		public void setYellow(){
			set(false,true,false);
		}
		public void setGreen(){
			set(true,false,false);
		}
		public void set(boolean green, boolean yellow, boolean red){
			int gg = (green==true)?(1):(0);
			int yy = (yellow==true)?(1):(0);
			int rr = (red==true)?(1):(0);
			dev.setNode(1, "M0029", rr, yy, gg);
		}
	};
	
	protected class Indicator {
		public Label txt;
		public Gauge gag;
		public WidValueNote cht;
		public Indicator(){
			this("","",0.,0.);
		}
		public Indicator(
			String name,
			String unit,
			double min, double max
		){
			txt = new Label(name);
			cht = new WidValueNote();
			cht.setRange(min, max);
			gag = GaugeBuilder.create()
				.skinType(SkinType.HORIZONTAL)
				//.skinType(SkinType.VERTICAL)
				//.skinType(SkinType.DASHBOARD)
				.title(name)
				.unit(unit)
				.minValue(min)
				.maxValue(max)				
				.build();			
		}
		public Indicator set(String name){
			txt.setText(name);
			gag.setTitle(name);
			return this;
		}
		public Indicator set(String unit,double min, double max){
			gag.setUnit(unit);
			gag.setMinValue(min);
			gag.setMaxValue(max);
			cht.setRange(min, max);
			return this;
		}
		public Indicator set(String name,String unit,double min, double max){
			set(name);
			set(unit,min,max);
			return this;
		}
		
		public HBox genPanel(){
			HBox lay = new HBox();
			cht.setPrefHeight(60);
			//cht.setPrefWidth(650);
			cht.setLegendVisible(false);
			lay.getChildren().addAll(gag,cht);
			lay.managedProperty().bind(lay.visibleProperty());
			return lay;
		}
		
		public NumberBinding prop;
		public Indicator bind(String prefix,NumberBinding propValue){
			prop = propValue;
			gag.valueProperty().bind(prop);
			cht.bind(prop);
			txt.textProperty().bind(prop.multiply(1.).asString(prefix+"%.1f "+gag.getUnit()));			
			return this;
		}
		public Indicator bind(NumberBinding propValue){
			return bind("",propValue);
		}
		public Indicator bind(NumberBinding propValue, StringProperty propText){
			prop = propValue;
			gag.valueProperty().bind(prop);
			cht.bind(prop);
			txt.textProperty().bind(propText);
			return this;
		}
	};
	
	public class ItmSwitch extends ItemToggle {
		public ItmSwitch(){
		}		
		public ItmSwitch(String name,double gx, double gy){
			locate(name,gx,gy);
			prepare_cursor();
		}
		private String nameRelay;
		private IntegerProperty nodeValue;		
		protected void eventChanged(boolean flag){			
		}
		private final ChangeListener<Number> nodeEvent = new ChangeListener<Number>(){
			@Override
			public void changed(
				ObservableValue<? extends Number> observable, 
				Number oldValue, 
				Number newValue
			) {
				boolean flag = true;
				if(nodeValue.get()==0){
					flag = false;
				}else{
					flag = true;
				}
				applyMakeup(flag);
				eventChanged(flag);
			}
		};
		public void hookRelay(DevFatek dev,int idx){
			hookRelay(dev,idx,idx);
		}		
		public void hookRelay(DevFatek dev, int m_idx, int y_idx){
			this.dev = dev;
			nameRelay = String.format("M%04d",m_idx);
			nodeValue = dev.getMarker(String.format("Y%04d",y_idx));
			nodeValue.addListener(nodeEvent);
			nodeEvent.changed(null, null, null);
		}
		protected DevFatek dev;
		@Override
		public void handle(MouseEvent event) {
			if(PanSputter.DBG==true){
				applyMakeup();
				eventChanged(isMakeup());
			}else{
				if(nodeValue.get()==0){
					dev.setNode(1, nameRelay, 3);
				}else{
					dev.setNode(1, nameRelay, 4);
				}
			}
		}
		public void flick(boolean onoff){
			if(onoff==true){
				dev.setNode(1, nameRelay, 3);
			}else{
				dev.setNode(1, nameRelay, 4);
			}
		}
	};
	
	public class ItmChuck extends ItmSwitch {
		public ItmChuck(double gx, double gy){
			super(CATE_CHUCKB,gx,gy);
		}

		public void setRPM(int rpm){
			dev.setRegister(1, "R01131", rpm);
		}
		public int getRPM(){
			return dev.getRegister(1, "R01131");
		}
		
		public Indicator temp = new Indicator("基板溫度","°C",0,30);
		public Indicator rota = new Indicator("基板轉速","RPM",0,30);
	};
		
	public class ItmBurnner extends ItemToggle {
		public ItmBurnner(String category,double gx, double gy){
			super(category,gx,gy);
			biasAdjust.setCycleCount(Timeline.INDEFINITE);			
		}
		public Indicator[] attr = {
			new Indicator(),
			new Indicator(),
			new Indicator(),
			new Indicator(),
			new Indicator(),
		};
		private DevFatek dev;
		private String pulsRelay, ctrlRelay, biasRegist;
		private IntegerProperty nodeValue;
		private final ChangeListener<Number> nodeEvent = new ChangeListener<Number>(){
			@Override
			public void changed(
				ObservableValue<? extends Number> observable, 
				Number oldValue, 
				Number newValue
			) {
				boolean flag = true;
				if(nodeValue.get()==0){
					flag = false;
				}else{
					flag = true;
				}
				applyMakeup(flag);
			}
		};
		public void hookRelay(DevFatek dev, int pulsIdx, int ctrlIdx, int biasIdx){
			this.dev = dev;
			pulsRelay = String.format("M%04d",pulsIdx);
			ctrlRelay = String.format("M%04d",ctrlIdx);
			biasRegist= String.format("R%05d",biasIdx);
			nodeValue = dev.getMarker(String.format("Y%04d",ctrlIdx));
			nodeValue.addListener(nodeEvent);
			nodeEvent.changed(null, null, null);
		}		
		
		public int biasMax = 200;
		public int biasStp = 5;
		public int biasCnt = 0;//When we start, we must set this first~~~
		private EventHandler<ActionEvent> biasAdjustEvent = new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(biasRegist==null || dev==null){
					return;
				}
				if((biasCnt+biasStp)>biasMax){
					biasAdjust.pause();											
					return;
				}
				biasCnt+=biasStp;
				dev.setRegister(1, biasRegist, biasCnt);
				//Misc.logv("writing - (%d)",biasCnt);
			}
		};
		private Timeline biasAdjust = new Timeline(new KeyFrame(
			Duration.seconds(1),
			biasAdjustEvent
		));
		@Override
		public void handle(MouseEvent event) {
			if(PanSputter.DBG==true){
				applyMakeup();//test~~~~
			}else{
				if(biasAdjust.getStatus().equals(Status.PLAYING) || nodeValue.get()!=0 ){
					biasAdjust.pause();				
					dev.setNode(1, ctrlRelay, 4);
					dev.setNode(1, pulsRelay, 4);
					dev.setRegister(1, biasRegist, 0);//turn off power!!!
					towerLamp.setGreen();
					Misc.logv("DC1: -off-");
				}else{
					//check voltage???
					dev.setNode(1, pulsRelay, 3);								
					dev.setNode(1, ctrlRelay, 3);
					biasCnt = 0;
					biasAdjust.play();	
					towerLamp.setYellow();
					Misc.logv("DC1: -on-");
				}
			}
		}
	};
	
	public class ItmSlider extends ItemSlider {
		public ItmSlider(
			String category, 
			double gx, double gy
		){
			super(category, gx,gy, gx+1.1,gy);
		}	
		public ItmSlider(
			String category, 
			double gx1, double gy1, 
			double gx2, double gy2
		){
			super(category, gx1,gy1, gx2,gy2);
		}
		protected DevFatek dev;
		protected String[] tkn = {null, null};
		public void setRegist(DevFatek dev, String regWrite, String regRead){
			this.dev = dev;
			tkn[0] = regWrite;
			tkn[1] = regRead;
		}
		public void setRegist(DevFatek dev, String reg){
			this.dev = dev;
			tkn[0] = tkn[1] = reg;
		}
		public ItmSlider setRange(int minVal, int maxVal, float stpVal){
			field.min = minVal;
			field.max = maxVal;
			field.stp = stpVal;
			return this;
		}
		@Override
		public void makeup() {
		}
		@Override
		public void eventChanged(float newVal) {
		}
		@Override
		public void eventReload() {
		}
	};
	
	public class ItmBattle extends ItmSlider {
		public ItmBattle(
			double gx1, double gy1, 
			double gx2, double gy2
		){
			super(CATE_BATTLE, gx1,gy1, gx2,gy2);
		}
		private float scale = 100.f;
		public ItmBattle setScale(float sv){
			scale = sv;
			return this;
		}
		@Override
		public void eventChanged(float newVal) {
			newVal = (newVal * 8192) / scale;
			dev.setRegister(1, tkn[0], Math.round(newVal));
		}
		@Override
		public void eventReload() {
			int v = dev.getRegister(1,tkn[1]);
			field.val = (v*scale)/8192.f;
		}
	};
	
	public class ItmChiller extends ItmSlider {
		public ItmChiller(double gx, double gy) {
			super(CATE_ICE, gx, gy);
		}
		@Override
		public void eventChanged(float newVal) {
			dev.setRegister(1,tkn[0],(int)Math.round(newVal*10.f));
		}
		@Override
		public void eventReload() {
			field.val = (float)(dev.getRegister(1,tkn[1]))/10.f;
		}
	};
	
	public class ItmValve extends ItmSwitch {
		public ItmValve(String name,double gx, double gy){
			this(name,"",gx,gy);
		}	
		private ItmValve(String name,String title,double gx, double gy){
			locate(CATE_VALVE,gx,gy);
			prepare_cursor();
			if(title.length()!=0){
				addLabel(title, gx, gy+1);
			}
		}
		
		private String[] lstPipeName = null;		
		public ItmValve setPipe(String... lstToken){
			lstPipeName = lstToken;
			return this;
		}
		@Override 
		protected void eventChanged(boolean flag){
			//show the state of the connected pipes
			for(String name:lstPipeName){
				ItemTile itm = getItem(name);
				if(itm==null){
					continue;
				}
				itm.clear();
				if(flag==true){
					itm.draw(1);
				}else{
					itm.draw(0);
				}			
			}
			redraw();
		}
	};
	
	protected class ItmValveB extends ItmSwitch {
		public ItmValveB(String name,String title,double gx,double gy){
			locate(CATE_VALVE_BL,gx,gy);
			prepare_cursor();
			if(title.length()!=0){
				addLabel(title, gx, gy+1);
			}
			
			gx = gx + 0.4;
			double gy1 = gy - 0.8;
			double gy2 = gy1 - 1.;
			addLabel(step.txt, gx, gy1);
			step.txt.setOnMouseClicked(e->{
				field.reload();
			});
			
			field.txt.setPrefWidth(TILE_SIZE*1.7);
			AnchorPane.setLeftAnchor(field, gx*TILE_SIZE);
			AnchorPane.setTopAnchor(field, gy2*TILE_SIZE);
			WidMapPID.this.getChildren().add(field);
		}
		
		@Override
		protected void eventChanged(boolean flag){
			if(flag==true){
				regName = presName;
				regScale = 0.1f;
				field.setRange(0, 50, 0.1);
			}else{
				regName = stepName;
				regScale = 1f;
				field.setRange(0, 1000, 10);
			}
			field.refresh();
		}
		
		private FieldNumSlider field = new FieldNumSlider(new FieldNumSlider.EventHook() {
			@Override
			public void eventReload() {
				if(regName==null){ return; }
				if(PanSputter.DBG==true){
					return;
				}
				field.val = dev.getRegister(1, regName) * regScale;
			}
			@Override
			public void eventChanged(float newVal) {
				if(regName==null){ return; }
				int regValue = (int)(field.val/regScale);
				if(PanSputter.DBG==true){
					return;
				}
				dev.setRegister(1, regName, regValue);
			}
		});
		
		public int getStep(){
			return dev.getRegister(1, stepName);
		}
		public void setStep(int val){
			dev.setRegister(1, stepName, val);
		}		
		public double getPress(){
			return dev.getRegister(1, presName) * 0.1;
		}
		public void setPress(double val){
			dev.setRegister(1, presName, (int)(val*10.));
		}
		
		public Indicator step = new Indicator("APC-step","",0,1000);
		
		private String regName = null, presName, stepName;
		private float regScale = 1.f;
		
		public void hookRelay(DevFatek dev, int idx0, int idx1, int idx2){
			presName = String.format("R%05d",idx1);
			stepName = String.format("R%05d",idx2);
			step.bind("閥門位置：", dev.getMarker("R1005").multiply(1));
			hookRelay(dev, idx0);			
		}
	}
	
	protected class ItmPump extends ItmSwitch {
		public ItmPump(String name,String title,double gx,double gy){
			char tkn = name.charAt(0);
			switch(tkn){
			default:
			case 'c':
			case 'C':
				locate(CATE_C_PUMP,gx,gy);
				break;				
			case 'm':
			case 'M':
				locate(CATE_M_PUMP,gx,gy);
				break;
			}			
			prepare_cursor();
			if(title.length()!=0){
				addLabel(title, gx, gy+1);
			}
		}
		protected void eventChanged(boolean flag){
		}
	};
	//----------------------------------------
	
	public Indicator presChamber1 = new Indicator("絕對氣壓","mTorr",0., 15.);//sensor range is 0~750 mTorr
	public Indicator presChamber2 = new Indicator("腔體氣壓","Torr", 0., 30.);//sensor range is 0~750 Torr
	
	public Indicator presAr = new Indicator("Ar進氣", "sccm", 0, 50);
	public Indicator presO2 = new Indicator("O2進氣", "sccm", 0, 20);
	public Indicator presN2 = new Indicator("N2進氣", "sccm", 0, 5);
	
	public Indicator presCP1 = new Indicator("CP2氣壓", "Torr", 0, 3);
	public Indicator presCP2 = new Indicator("CP2氣壓", "Torr", 0, 3);
	public Indicator tempCP1 = new Indicator("CP1溫度", "K", 0, 273);
	public Indicator tempCP2 = new Indicator("CP2溫度", "K", 0, 273);

	public ItmTowerLamp towerLamp = new ItmTowerLamp(7.5, 0.5);
	
	public ItmChuck chuck = new ItmChuck(11., 0.5);
	public ItmSwitch baffle= new ItmSwitch(CATE_BAFFLE_A, 11, 2);
	
	public ItmBurnner burner_dc1 = new ItmBurnner(CATE_BURNER_A, 7.5, 6.5);
	public ItmBurnner burner_rf1 = new ItmBurnner(CATE_BURNER_C, 10 , 6.5);
	
	public Indicator ampMP1 = new Indicator("MP", "安培", 0., 10.);
	public Indicator ampHeater = new Indicator("加熱器", "安培", 0., 10.);
	public Indicator tempChamber2 = new Indicator("腔體", "°C", 0., 30.);
	public Indicator tempChiller  = new Indicator("冰水機", "°C", 0., 30.);
	
	public ItmSlider Arb = new ItmBattle(0,2, 0.7,3).setScale(100.f).setRange(0, 50, 1f);
	public ItmSlider O2b = new ItmBattle(0,5, 0.7,6).setScale(40.f).setRange(0, 10, 1f);
	public ItmSlider N2b = new ItmBattle(0,8, 0.7,9).setScale(4.f).setRange(0, 5, 0.1f);
	
	public ItmSlider chiller = new ItmChiller(15.5,7).setRange(0, 25, 1);

	public ItmValve Arv= new ItmValve("Ar", 2, 1).setPipe("#0-1","#1-1","#3-1");
	public ItmValve gv1= new ItmValve("GV1","GV1", 4, 1).setPipe("#3-1","#5-1","#5-2","#5-3");
		
	public ItmValve O2v= new ItmValve("O2", 2, 4).setPipe("#0-4","#1-4","#3-4");
	public ItmValve gv2= new ItmValve("GV2","GV2", 4, 4).setPipe("#3-4","#5-4","#6-4");
	
	public ItmValve N2v= new ItmValve("N2", 2, 7).setPipe("#0-7","#1-7","#3-7");
	public ItmValve gv3= new ItmValve("GV3","GV3", 4, 7).setPipe("#3-7","#5-7","#5-6","#5-5");
	
	public ItmValve vv = new ItmValve("VV", "VV", 18, 5).setPipe("#17-5","#19-5");

	public ItmValve rv = new ItmValve("RV", "RV", 13, 10).setPipe(
		"#6-12", "#6-11", "#6-10", "#7-10",
		"#8-10", "#9-10", "#10-10", "#11-10",
		"#12-10","#14-10"
	);
	
	public ItmValve  mv1 = new ItmValve("MV1", "MV1", 13, 13).setPipe("#10-13","#12-13","#14-13");	
	public ItmValveB apc = new ItmValveB("APC", "APC", 11, 13);
	public ItmPump   cp1 = new ItmPump("CP1", "", 9, 13);
	public ItmValve  fv1 = new ItmValve("FV1", "FV1",7, 13).setPipe("#5-13","#6-13","#8-13");
	
	public ItmValve  mv2 = new ItmValve("MV2", "MV2", 13, 16).setPipe("#10-16","#11-16","#12-16","#14-16");	
	public ItmPump   cp2 = new ItmPump("CP2", "", 9, 16);
	public ItmValve  fv2 = new ItmValve("FV2", "FV2", 7, 16).setPipe("#8-16","#6-14","#6-15","#6-16");
		
	public ItmPump   mp1 = new ItmPump("MP1", "MP1", 4, 13);
	//----------------------------------------
	
	private void default_layout(){
		//prepare_gauge();
		addBrick(
			CATE_WALL_A+"#7-0",
			CATE_WALL_B+"#8-0" ,CATE_WALL_B+"#9-0" ,CATE_WALL_B+"#10-0",CATE_WALL_B+"#11-0",
			CATE_WALL_B+"#12-0",CATE_WALL_B+"#13-0",CATE_WALL_B+"#14-0",CATE_WALL_B+"#15-0",
			CATE_WALL_C+"#16-0",
			CATE_WALL_D+"#16-1",CATE_WALL_D+"#16-2",CATE_WALL_D+"#16-3",CATE_WALL_D+"#16-4",
			CATE_WALL_D+"#16-5",CATE_WALL_D+"#16-6",CATE_WALL_D+"#16-7",
			CATE_WALL_E+"#16-8",
			CATE_WALL_F+"#15-8",CATE_WALL_F+"#14-8",CATE_WALL_F+"#13-8",CATE_WALL_F+"#12-8",
			CATE_WALL_F+"#11-8",CATE_WALL_F+"#10-8",CATE_WALL_F+"#9-8" ,CATE_WALL_F+"#8-8",
			CATE_WALL_G+"#7-8" ,
			CATE_WALL_H+"#7-7" ,CATE_WALL_H+"#7-6" ,CATE_WALL_H+"#7-5" ,CATE_WALL_H+"#7-4",
			CATE_WALL_H+"#7-3" ,CATE_WALL_H+"#7-2" ,CATE_WALL_H+"#7-1"
		);
		addItem(CATE_GAUGE+"#17-2",CATE_PIPE_F+"#17-3");
		addLabel(tempChamber2.txt,18,1);		
		addLabel(presChamber1.txt,18,2);		
		addLabel(presChamber2.txt,13.5,2);
		
		addItem("tower",towerLamp);
		
		addItem("burner-dc1",burner_dc1);
		burner_dc1.attr[0].set("DC1功率", "W", 0, 500);
		burner_dc1.attr[1].set("DC1電壓", "V", 0, 1000);
		burner_dc1.attr[2].set("DC1電流","mA", 0, 1000);
		addLabel(burner_dc1.attr[0].txt, 7.5, 4);
		addLabel(burner_dc1.attr[1].txt, 7.5, 5);
		addLabel(burner_dc1.attr[2].txt, 7.5, 6);
		
		addItem("burner-rf1",burner_rf1);
		
		addItem("chuck",chuck);
		addLabel(chuck.temp.txt,14,1);
		addLabel(chuck.rota.txt, 9,1);	
		
		//addLabel(amp_heat.txt,14,1);
				
		addItem("baffle",baffle);
		
		addItem("ice",chiller);
		addLabel(tempChiller.txt, 15, 6);
				
		addLabel("Ar", 0.2, 3);
		addItem("Ar-battle",Arb);
		addItem("Ar-valve0",Arv);
		addItem("Ar-valve1",gv1);
		addItem(
			CATE_PIPE_D+"#0-1",
			CATE_PIPE_A+"#1-1",
			CATE_PIPE_A+"#3-1",
			CATE_PIPE_E+"#5-1",
			CATE_PIPE_B+"#5-2",
			CATE_PIPE_B+"#5-3"
		);
		addLabel(presAr.txt, 1.5, 2);
		
		addLabel("O2", 0.1, 6);
		addItem("O2-battle",O2b);
		addItem("O2-valve0",O2v);		
		addItem("O2-valve1",gv2);
		addItem(
			CATE_PIPE_D+"#0-4",
			CATE_PIPE_A+"#1-4",
			CATE_PIPE_A+"#3-4",
			CATE_PIPE_K+"#5-4",
			CATE_PIPE_A+"#6-4"
		);
		addLabel(presO2.txt, 1.5, 5);
		
		addLabel("N2", 0.1, 9);
		addItem("N2-battle",N2b);
		addItem("N2-valve0",N2v);
		addItem("N2-valve1",gv3);
		addItem(
			CATE_PIPE_D+"#0-7",
			CATE_PIPE_A+"#1-7",
			CATE_PIPE_A+"#3-7",
			CATE_PIPE_F+"#5-7",
			CATE_PIPE_B+"#5-6",
			CATE_PIPE_B+"#5-5"
		);
		addLabel(presN2.txt, 1.5, 8);
		
		addItem("破真空閥",vv);
		addItem(
			CATE_PIPE_A+"#17-5",
			CATE_PIPE_A+"#19-5"
		);
		
		addItem(
			CATE_PIPE_B+"#15-9",
			CATE_PIPE_J+"#15-10",
			CATE_PIPE_B+"#15-11",
			CATE_PIPE_B+"#15-12",
			CATE_PIPE_J+"#15-13",
			CATE_PIPE_B+"#15-14",
			CATE_PIPE_B+"#15-15",
			CATE_PIPE_F+"#15-16"			
		);//pipe for motor~~~
		
		addItem("R-valve",rv);	
		addItem("MP",mp1);
		addLabel(ampMP1.txt,mp1.getGridX(),mp1.getGridY()+2);
		addItem(
			CATE_PIPE_B+"#6-12",
			CATE_PIPE_B+"#6-11",			
			CATE_PIPE_D+"#6-10",
			CATE_PIPE_A+"#7-10",
			CATE_PIPE_A+"#8-10",
			CATE_PIPE_A+"#9-10",
			CATE_PIPE_A+"#10-10",
			CATE_PIPE_A+"#11-10",
			CATE_PIPE_A+"#12-10",
			CATE_PIPE_A+"#14-10"			
		);
				
		addItem("M-valve1",mv1);
		addItem("APC-valve",apc);
		addItem("CP1",cp1);
		addItem("F-valve1",fv1);
		addItem(			
			CATE_PIPE_A+"#5-13",
			CATE_PIPE_K+"#6-13",
			CATE_GAUGE +"#8-12",
			CATE_PIPE_G+"#8-13",
			CATE_PIPE_A+"#10-13",
			CATE_PIPE_A+"#12-13",
			CATE_PIPE_A+"#14-13"
		);
		addLabel(presCP1.txt,9,12.2);
		addLabel(tempCP1.txt,9,14);
		
		addItem("M-valve2",mv2);
		addItem("CP2",cp2);
		addItem("F-valve2",fv2);
		addItem(
			CATE_PIPE_B+"#6-14",
			CATE_PIPE_B+"#6-15",
			CATE_PIPE_C+"#6-16",
			CATE_GAUGE +"#8-15",
			CATE_PIPE_G+"#8-16",
			CATE_PIPE_A+"#11-16",
			CATE_PIPE_A+"#10-16",
			CATE_PIPE_A+"#12-16",
			CATE_PIPE_A+"#14-16"
		);
		addLabel(presCP2.txt,9,15.2);
		addLabel(tempCP2.txt,9,17);
		
		bringup_slider();
	}

	private void addItem(String... tkn){
		for(int i=0; i<tkn.length; i++){
			String[] arg = tkn[i].split("#");
			String[] pos = arg[1].split("-");
			int gx = Integer.valueOf(pos[0]);
			int gy = Integer.valueOf(pos[1]);
			addItem("#"+arg[1],arg[0],gx,gy);
		}
	}
	
	private void addBrick(String... tkn){
		for(int i=0; i<tkn.length; i++){
			String[] arg = tkn[i].split("#");
			String[] pos = arg[1].split("-");
			int gx = Integer.valueOf(pos[0]);
			int gy = Integer.valueOf(pos[1]);
			addBrick(arg[0],gx,gy);
		}
	}
}
