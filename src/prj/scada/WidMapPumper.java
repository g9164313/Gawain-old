package prj.scada;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import eu.hansolo.medusa.Gauge.SkinType;
import narl.itrc.Misc;
import narl.itrc.WidDiagram;

public class WidMapPumper extends WidDiagram {

	public WidMapPumper(){
		default_layout();		
		redrawAll();
	}
	
	//%%%rotator轉盤 <---> M121, Y18 
	//%%%baffle 擋板 <---> M(Y)38
	//%%%m-valve1(MV1) <---> M(Y)4
	//%%%m-valve2(MV2) <---> M(Y)14
	//%%%r-valve(RV) <---> M(Y)3
	//%%%f-valve1(FV1) <---> M(Y)2
	//%%%f-valve2(FV2) <---> M(Y)13
	//%%%VV  <---> M(Y)5
	//%%%MP  <---> M(Y)20
	//%%%CP1 <---> M(Y)33
	//%%%CP2 <---> M(Y)37
	//SPI-2K <---> M(Y)128
	//RF1    <---> M(Y)32
	//DC1    <---> M(Y)34
	//DC2    <---> M(Y)35
		
	//%%%R????(R1016) --> 轉盤速度
	//%%%R????(R1007) --> 腔體溫度
	//%%%R????(R1032) --> 基板溫度
	//%%%R####(R1038) --> 讀取 chiller 溫度, 固定點小數一位
	//%%%R####(R1138) --> 設定 chiller 溫度, 固定點小數一位
	//%%%R2000(R1004) --> chamber pressure(right) [MKS626B]
	//%%%R3849(R1024) --> chamber pressure(left，陰極管) [PKR251]
	//%%%R3848(R1028) --> CP1/TEMP
	//%%%R3853(R1025) --> CP1/pressure [TPR 280]
	//%%%R3856(R1034) --> CP2/TEMP
	//%%%R3857(R1035) --> CP2/pressure [TPR 280]
	//%%%R3854(R1000) --> Ar pressure
	//%%%R3858(R1001) --> O2 pressure
	//%%%R????(R1002) --> N2 pressure
	//R1026 --> 加熱器電流
	//R1027 --> 離心機電流
	//%%%SCCM 設定 R1100-Ar, R1101-O2, R1102-N2, 
	//暫存器 R01138 寫入 00FA (???)
	//CRV1(這是什麼？) <---> M(Y)23 
	//CRV2(這是什麼？) <---> M(Y)21
	
	//01 41 1 <-- PLC RUN
	//01 40 <-- PLC 讀取
	public void hookPart(DevFatek dev){
		//trick, we will manually initial all parts again.....
		tower_lamp.hookRelay(dev);
		
		Arv.hookRelay(dev, 9);		
		O2v.hookRelay(dev, 10);		
		N2v.hookRelay(dev, 11);
		
		gv1.hookRelay(dev, 12);
		gv2.hookRelay(dev, 15);
		gv3.hookRelay(dev, 36);	
		
		mv1.hookRelay(dev, 4);
		mv2.hookRelay(dev, 14);
		
		fv1.hookRelay(dev, 1);
		fv2.hookRelay(dev, 13);
		
		rv.hookRelay(dev, 3);
		vv.hookRelay(dev, 5);
		
		chuck.hookRelay(dev, 121, 18);
		chuck.temp.bind(prop_Temp_Chuck(dev));
		chuck.rota.bind(dev.getMarker("R1016").multiply(1));
		
		baffle.hookRelay(dev, 38);
		
		burner_dc1.hookRelay(dev, 128, 34);
		burner_dc1.attr[0].bind(dev.getMarker("R1010").multiply(1));
		burner_dc1.attr[1].bind(dev.getMarker("R1011").multiply(1));
		burner_dc1.attr[2].bind(dev.getMarker("R1012").multiply(1));
		
		chiller.setRegist(dev, "R01138", "R01038");		
		
		Arb.setRegist(dev, "R01100");
		O2b.setRegist(dev, "R01101");
		N2b.setRegist(dev, "R01102");
		
		presAr.bind(prop_Press_Ar(dev));
		presO2.bind(prop_Press_O2(dev));
		presN2.bind(prop_Press_N2(dev));
		
		tempChamber2.bind(prop_Temp_Chamber(dev));
		tempChiller.bind(prop_Temp_Chiller(dev));
		
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
		return dev.getMarker("R1001").divide(8192.f).multiply(100.f);
	}
	private NumberBinding prop_Press_N2(DevFatek dev){
		return dev.getMarker("R1002").divide(8192.f).multiply(100.f);
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
	
	private class ItmTowerLamp extends ItemBrick {
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
		public void hookRelay(DevFatek dev){
			Y29 = dev.getMarker("Y0029");
			Y30 = dev.getMarker("Y0030");
			Y31 = dev.getMarker("Y0031");
			Y29.addListener(event);
			Y30.addListener(event);
			Y31.addListener(event);
		}
	};
	
	private class Indicator {
		public Label txt;
		public Gauge gag;
		public Indicator(){
			txt = new Label("");
			gag = GaugeBuilder.create()
				.skinType(SkinType.HORIZONTAL)
				.build();
		}
		/*public Indicator(String name){
			txt = new Label(name);
			gag = GaugeBuilder.create()
				.skinType(SkinType.HORIZONTAL)
				.title(name)
				.build();
		}*/
		public Indicator(
			String name,
			String unit,
			double min, double max
		){
			create(name,unit,min,max);
		}		
		public void create(
			String name,
			String unit,
			double min, double max
		){
			txt = new Label(name);
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
			return this;
		}
		public Indicator set(String name,String unit,double min, double max){
			set(name);
			set(unit,min,max);
			return this;
		}
		public Indicator bind(NumberBinding propValue){
			gag.valueProperty().bind(propValue);
			txt.textProperty().bind(propValue.multiply(1.).asString("%.1f "+gag.getUnit()));
			return this;
		}
		public Indicator bind(NumberBinding propValue, StringProperty propText){			
			gag.valueProperty().bind(propValue);
			txt.textProperty().bind(propText);
			return this;
		}
	};
	
	private class ItemSwitch extends ItemToggle {
		public ItemSwitch(){
		}		
		public ItemSwitch(String name,double gx, double gy){
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
		private DevFatek dev;
		@Override
		public void handle(MouseEvent event) {
			if(nodeValue.get()==0){
				dev.setNode(1, nameRelay, 3);
			}else{
				dev.setNode(1, nameRelay, 4);
			}
			//applyMakeup();//test~~~~
			//eventChanged(isMakeup());//test~~~~
		}
	};
	private class ItemChuck extends ItemSwitch {
		public ItemChuck(double gx, double gy){
			super(CATE_CHUCKB,gx,gy);
		}
		public Indicator temp = new Indicator("基板溫度","°C",0,30);
		public Indicator rota = new Indicator("基板轉速","RPM",0,30);
	};
		
	private class ItemBurnner extends ItemToggle {
		public ItemBurnner(String category,double gx, double gy){
			super(category,gx,gy);
		}
		public Indicator[] attr = {
			new Indicator(),
			new Indicator(),
			new Indicator(),
			new Indicator(),
			new Indicator(),
		};
		private DevFatek dev;
		private String pulsRelay, ctrlRelay;
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
		public void hookRelay(DevFatek dev, int ex_idx, int idx){
			this.dev = dev;
			pulsRelay = String.format("M%04d",ex_idx);
			ctrlRelay = String.format("M%04d",idx);
			nodeValue = dev.getMarker(String.format("Y%04d",idx));
			nodeValue.addListener(nodeEvent);
			nodeEvent.changed(null, null, null);
		}		
		@Override
		public void handle(MouseEvent event) {
			if(nodeValue.get()==0){
				dev.setNode(1, pulsRelay, 3);
				dev.setNode(1, ctrlRelay, 3);
			}else{				
				dev.setNode(1, ctrlRelay, 4);
				dev.setNode(1, pulsRelay, 4);
			}
			//applyMakeup();//test~~~~
		}
	};
	
	private class ItmSNode extends ItemSlider {
		public ItmSNode(
			String category, 
			double gx, double gy
		){
			super(category, gx,gy, gx+1.1,gy);
		}	
		public ItmSNode(
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
		public ItmSNode setRange(int minVal, int maxVal, int stpVal){
			min = minVal;
			max = maxVal;
			stp = stpVal;
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
	private class ItmBattle extends ItmSNode {
		public ItmBattle(
			double gx1, double gy1, 
			double gx2, double gy2
		){
			super(CATE_BATTLE, gx1,gy1, gx2,gy2);
		}
		@Override
		public void eventChanged(float newVal) {
			dev.setRegister(1, tkn[0], Math.round((newVal*8192)/100) );
		}
		@Override
		public void eventReload() {
			val = ((dev.getRegister(1,tkn[1]))*100)/8192.f;
		}
	};	
	private class ItmChiller extends ItmSNode {
		public ItmChiller(double gx, double gy) {
			super(CATE_ICE, gx, gy);
		}
		@Override
		public void eventChanged(float newVal) {
			dev.setRegister(1,tkn[0],(int)Math.round(newVal*10.f));
		}
		@Override
		public void eventReload() {
			val = (float)(dev.getRegister(1,tkn[1]))/10.f;
		}
	};
	
	
	private class ItmValve extends ItemSwitch {
		public ItmValve(String name,double gx, double gy){
			init(name,name,gx,gy);
		}
		public ItmValve(String name,String title,double gx, double gy){
			init(name,title,gx,gy);
		}		
		private void init(String name,String title,double gx, double gy){
			if(name.charAt(0)=='@'){				
				locate(CATE_VALVE_BL,gx,gy);
				name = name.substring(1);
			}else{
				locate(CATE_VALVE,gx,gy);
			}
			prepare_cursor();
			if(title!=null){
				if(title.length()!=0){
					addLabel(name, gx, gy+1);
				}
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
	//----------------------------------------
	/*
	private void prepare_gauge(){

		create_gauge("加熱器電流", "A", 0., 5.);	
		create_gauge("離心機電流", "A", 0., 5.);			
	}*/
	
	private ItmTowerLamp tower_lamp = new ItmTowerLamp(7.5, 2);
	
	private Indicator tempChamber2 = new Indicator("腔體溫度", "°C", 0., 30.);
	private Indicator tempChiller  = new Indicator("冰水機", "°C", 0., 30.);
	
	private Indicator presChamber1 = new Indicator("絕對氣壓","mTorr",0., 750.);
	private Indicator presChamber2 = new Indicator("腔體氣壓","Torr", 0., 750.);
	
	private Indicator presAr = new Indicator("Ar進氣", "sccm", 0, 100);//?? max and min ??
	private Indicator presO2 = new Indicator("O2進氣", "sccm", 0, 100);
	private Indicator presN2 = new Indicator("N2進氣", "sccm", 0, 100);
	
	private Indicator tempCP1 = new Indicator("CP1溫度", "K", 0, 273);
	private Indicator tempCP2 = new Indicator("CP2溫度", "K", 0, 273);
	private Indicator presCP1 = new Indicator("CP2氣壓", "Torr", 0, 3);
	private Indicator presCP2 = new Indicator("CP2氣壓", "Torr", 0, 3);
	
	private ItemChuck chuck = new ItemChuck(11., 1.5);
	private ItemSwitch baffle= new ItemSwitch(CATE_BAFFLE_A, 11, 3);
	
	private ItemBurnner burner_dc1 = new ItemBurnner(CATE_BURNER_A, 7.5, 7.5);
	private ItemBurnner burner_rf1 = new ItemBurnner(CATE_BURNER_C, 10 , 7.5);

	private ItmSNode Arb = new ItmBattle(0,2, 0.7,3).setRange(0, 50, 1);
	private ItmSNode O2b = new ItmBattle(0,5, 0.7,6).setRange(0, 50, 1);
	private ItmSNode N2b = new ItmBattle(0,8, 0.7,9).setRange(0, 50, 1);
	
	private ItmSNode chiller = new ItmChiller(15.5,8).setRange(0, 25, 1);

	private ItmValve Arv= new ItmValve("Ar", null, 2, 1).setPipe("#0-1","#1-1","#3-1");
	private ItmValve gv1= new ItmValve("GV1", 4, 1).setPipe("#3-1","#5-1","#5-2","#5-3");
		
	private ItmValve O2v= new ItmValve("O2", null, 2, 4).setPipe("#0-4","#1-4","#3-4");
	private ItmValve gv2= new ItmValve("GV2", 4, 4).setPipe("#3-4","#5-4","#6-4");
	
	private ItmValve N2v= new ItmValve("N2", null, 2, 7).setPipe("#0-7","#1-7","#3-7");
	private ItmValve gv3= new ItmValve("GV3", 4, 7).setPipe("#3-7","#5-7","#5-6","#5-5");
	
	private ItmValve vv = new ItmValve("VV", 18, 6).setPipe("#17-6","#19-6");

	private ItmValve rv = new ItmValve("RV", 13, 15).setPipe(
		"#5-15","#6-15","#7-15","#8-15",
		"#9-15","#10-15","#11-15","#12-15",
		"#14-15"
	);
	
	private ItmValve mv1 = new ItmValve("MV1", 13, 12).setPipe("#12-12","#14-12");
	private ItmValve fv1 = new ItmValve("FV1", 7, 12).setPipe("#6-14","#6-13","#6-12","#8-12");

	private ItmValve mv2 = new ItmValve("MV2", 13, 18).setPipe("#11-18","#10-18","#12-18","#14-18");
	private ItmValve fv2 = new ItmValve("FV2", 7, 18).setPipe("#8-18","#6-16","#6-17","#6-18");
	
	private ItmValve apc = new ItmValve("@", 11, 12).setPipe("#10-12","#12-12");
	
	private ItemSwitch m_pump = new ItemSwitch(CATE_M_PUMP, 4, 15);
	private ItemSwitch c_pump1= new ItemSwitch(CATE_C_PUMP, 9, 12);
	private ItemSwitch c_pump2= new ItemSwitch(CATE_C_PUMP, 9, 18);
	//----------------------------------------
	
	private void default_layout(){
		//prepare_gauge();
		addBrick(
			CATE_WALL_A+"#7-1",
			CATE_WALL_B+"#8-1" ,CATE_WALL_B+"#9-1" ,CATE_WALL_B+"#10-1",CATE_WALL_B+"#11-1",
			CATE_WALL_B+"#12-1",CATE_WALL_B+"#13-1",CATE_WALL_B+"#14-1",CATE_WALL_B+"#15-1",
			CATE_WALL_C+"#16-1",
			CATE_WALL_D+"#16-2",CATE_WALL_D+"#16-3",CATE_WALL_D+"#16-4",CATE_WALL_D+"#16-5",
			CATE_WALL_D+"#16-6",CATE_WALL_D+"#16-7",CATE_WALL_D+"#16-8",
			CATE_WALL_E+"#16-9",
			CATE_WALL_F+"#15-9",CATE_WALL_F+"#14-9",CATE_WALL_F+"#13-9",CATE_WALL_F+"#12-9",
			CATE_WALL_F+"#11-9",CATE_WALL_F+"#10-9",CATE_WALL_F+"#9-9" ,CATE_WALL_F+"#8-9",
			CATE_WALL_G+"#7-9" ,
			CATE_WALL_H+"#7-8" ,CATE_WALL_H+"#7-7" ,CATE_WALL_H+"#7-6" ,CATE_WALL_H+"#7-5",
			CATE_WALL_H+"#7-4" ,CATE_WALL_H+"#7-3" ,CATE_WALL_H+"#7-2"
		);
		addItem(CATE_GAUGE+"#17-3",CATE_PIPE_F+"#17-4");
		addLabel(tempChamber2.txt,18,2);		
		addLabel(presChamber1.txt,18,3);		
		addLabel(presChamber2.txt,13.5,3);
		
		addItem("tower",tower_lamp);
		
		addItem("burner-dc1",burner_dc1);
		burner_dc1.attr[0].set("DC1功率", "W", 0, 200);
		burner_dc1.attr[1].set("DC1電壓", "V", 0, 200);
		burner_dc1.attr[2].set("DC1電流","mA", 0, 200);
		addLabel(burner_dc1.attr[0].txt, 7.5, 5);
		addLabel(burner_dc1.attr[1].txt, 7.5, 6);
		addLabel(burner_dc1.attr[2].txt, 7.5, 7);
		
		addItem("burner-rf1",burner_rf1);
		
		addItem("chuck",chuck);
		addLabel(chuck.temp.txt,14,2);
		addLabel(chuck.rota.txt, 9,2);	
		
		addItem("baffle",baffle);
		
		addItem("ice",chiller);
		addLabel(tempChiller.txt, 15, 7);
				
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
			CATE_PIPE_A+"#17-6",
			CATE_PIPE_A+"#19-6"
		);

		addItem("M-valve1",mv1);
		addItem("APC-valve",apc);
		addItem("CP1",c_pump1);
		addItem("F-valve1",fv1);
		addItem(			
			CATE_PIPE_A+"#14-12",	
			CATE_PIPE_A+"#12-12",
			CATE_PIPE_A+"#10-12",
			CATE_PIPE_G+"#8-12",
			CATE_GAUGE +"#8-11",
			CATE_PIPE_D+"#6-12",
			CATE_PIPE_B+"#6-13",
			CATE_PIPE_B+"#6-14"
		);
		addLabel(tempCP1.txt,9,10);
		addLabel(presCP1.txt,9,11);
		
		addItem("R-valve",rv);		
		addItem("MP",m_pump);		
		addItem(
			CATE_PIPE_A+"#5-15",
			CATE_PIPE_K+"#6-15",
			CATE_PIPE_A+"#7-15",
			CATE_PIPE_A+"#8-15",
			CATE_PIPE_A+"#9-15",
			CATE_PIPE_A+"#10-15",
			CATE_PIPE_A+"#11-15",
			CATE_PIPE_A+"#12-15",
			CATE_PIPE_A+"#14-15"
		);
		
		addItem("M-valve2",mv2);
		addItem("CP2",c_pump2);
		addItem("F-valve2",fv2);
		addItem(			
			CATE_PIPE_A+"#14-18",
			CATE_PIPE_A+"#12-18",
			CATE_PIPE_A+"#10-18",
			CATE_PIPE_A+"#11-18",			
			CATE_PIPE_G+"#8-18",
			CATE_GAUGE +"#8-17",
			CATE_PIPE_C+"#6-18",
			CATE_PIPE_B+"#6-17",
			CATE_PIPE_B+"#6-16"
		);
		addLabel(tempCP2.txt,9,16);
		addLabel(presCP2.txt,9,17);
		
		//pipe for motor~~~
		addItem(
			CATE_PIPE_B+"#15-10",
			CATE_PIPE_B+"#15-11",
			CATE_PIPE_J+"#15-12",
			CATE_PIPE_B+"#15-13",
			CATE_PIPE_B+"#15-14",
			CATE_PIPE_J+"#15-15",
			CATE_PIPE_B+"#15-16",
			CATE_PIPE_B+"#15-17",
			CATE_PIPE_F+"#15-18"			
		);
		
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
