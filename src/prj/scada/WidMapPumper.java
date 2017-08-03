package prj.scada;

import java.util.ArrayList;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.MouseEvent;
import eu.hansolo.medusa.Gauge.SkinType;
import narl.itrc.WidDiagram;

public class WidMapPumper extends WidDiagram {

	public WidMapPumper(){
		default_layout();		
		redrawAll();
	}
	
	public void hookPart(DevFatek dev){
		//trick, we will manually initial all parts again.....
		tower_lamp.hookRelay(dev);
		
		Ar.hookRelay(dev, 9);
		gv1.hookRelay(dev, 12);
		
		O2.hookRelay(dev, 10);
		gv2.hookRelay(dev, 15);
		
		N2.hookRelay(dev, 11);
		gv3.hookRelay(dev, 36);		
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
	private ItmTowerLamp tower_lamp = new ItmTowerLamp(7.5, 2);

	private ItemToggle substrates = new ItemToggle(CATE_CHUCKB, 11, 1.5){
		@Override
		public void handle(MouseEvent event) {
			System.out.println("jjjjj");
		}
	};
	
	private class ItmValve extends ItemToggle {
		
		public ItmValve(String title,double gx, double gy){
			//If valve is special, its name will be lead a special character~~~
			if(title.charAt(0)=='@'){				
				locate(CATE_VALVE_BL,gx,gy);
				title = title.substring(1);
			}else{
				locate(CATE_VALVE,gx,gy);
			}
			if(title.length()!=0){
				addLabel(title, gx, gy+1);
			}
		}
		
		private String[] lstPipeName = null;		
		public ItmValve setPipe(String... lstToken){
			lstPipeName = lstToken;
			return this;
		}		
		
		private String nameRelay;
		private IntegerProperty nodeValue;
		
		private final ChangeListener<Number> event = new ChangeListener<Number>(){
			@Override
			public void changed(
				ObservableValue<? extends Number> observable, 
				Number oldValue, 
				Number newValue
			) {
				//flag = nodeValue.get();
				//redraw();
				//pipe_switch(flag,lstPipeName);
				applyMakeup();//for test~~~~				
				show_pipe(isMakeup());
			}
		};
		
		private DevFatek dev;		
		public void hookRelay(DevFatek dev,int idx){
			this.dev = dev;
			nameRelay = String.format("M%04d",idx);
			nodeValue = dev.getMarker(String.format("Y%04d",idx));
			nodeValue.addListener(event);
		}
		
		@Override
		public void handle(MouseEvent event) {
			applyMakeup();
			//if(nodeValue==null){
			//	return;
			//}
			//if(nodeValue.get()==0){
			//	dev.setNode(1, nameRelay, 3);
			//}else{
			//	dev.setNode(1, nameRelay, 4);
			//}			
		}
		
		private void show_pipe(boolean flag){
			for(String name:lstPipeName){
				ItemTile itm = getItem(name);
				if(itm==null){
					continue;
				}
				itm.clear();
				if(isMakeup()==true){
					itm.draw(1);
				}else{
					itm.draw(0);
				}			
			}
			redraw();
		}
	};
	
	private ItmValve Ar = new ItmValve("Ar", 2, 1).setPipe("#0-1","#1-1","#3-1");
	
	private ItmValve gv1= new ItmValve("GV1", 4, 1).setPipe("#3-1","#5-1","#5-2","#5-3");

	private ItmValve O2 = new ItmValve("O2", 2, 4).setPipe("#0-4","#1-4","#3-4");
	
	private ItmValve gv2= new ItmValve("GV2", 4, 4).setPipe("#3-4","#5-4","#6-4");

	private ItmValve N2 = new ItmValve("N2", 2, 7).setPipe("#0-7","#1-7","#3-7");

	private ItmValve gv3= new ItmValve("GV3", 4, 7).setPipe("#3-7","#5-7","#5-6","#5-5");

	private ItmValve vv = new ItmValve("VV", 18, 7).setPipe("#17-7","#19-7");

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

	private ItemBrick m_pump = new ItemBrick(CATE_M_PUMP, 4, 15);
	
	private ItemBrick c_pump1 = new ItemBrick(CATE_C_PUMP, 9, 12);

	private ItemBrick c_pump2 = new ItemBrick(CATE_C_PUMP, 9, 18);
	
	//motor, rotator <---> M(Y)121, rpm 看 R1016(???)
	//substrate 擋板 <---> M(Y)38
	//m-valve1(MV1) <---> M(Y)4
	//m-valve2(MV2) <---> M(Y)14
	//r-valve(RV) <---> M(Y)3
	//f-valve1(FV1) <---> M(Y)2
	//f-valve2(FV2) <---> M(Y)13
	//CP1 <---> M(Y)33
	//CP2 <---> M(Y)37
	//MP <---> M(Y)20
	//R3849(R1024) --> chamber pressure
	//R3848(R1028) --> CP1/TEMP
	//R3853(R1025) --> CP1/pressure
	//R3856(R1034) --> CP2/TEMP
	//R3857(R1035) --> CP2/pressure
	//暫存器 R01138 寫入 00FA (???)
	//CRV1(這是什麼？) <---> M(Y)23 
	//CRV2(這是什麼？) <---> M(Y)21
	//01 41 1 <-- PLC RUN
	//01 40 <-- PLC 讀取
	//-----------------------------------------//
	
	private void default_layout(){
		
		addItem("chuck",substrates);
		addItem("tower",tower_lamp);
		
		addItem("Ar-valve0",Ar);		
		addItem("Ar-valve1",gv1);
		addBlock(
			CATE_BATTLE+"#0-2",
			CATE_PIPE_D+"#0-1",
			CATE_PIPE_A+"#1-1",
			CATE_PIPE_A+"#3-1",
			CATE_PIPE_E+"#5-1",
			CATE_PIPE_B+"#5-2",
			CATE_PIPE_B+"#5-3"
		);
		
		addItem("O2-valve0",O2);		
		addItem("O2-valve1",gv2);
		addBlock(
			CATE_BATTLE+"#0-5",
			CATE_PIPE_D+"#0-4",
			CATE_PIPE_A+"#1-4",
			CATE_PIPE_A+"#3-4",
			CATE_PIPE_K+"#5-4",
			CATE_PIPE_A+"#6-4"
		);
		
		addItem("N2-valve0",N2);
		addItem("N2-valve1",gv3);
		addBlock(
			CATE_BATTLE+"#0-8",
			CATE_PIPE_D+"#0-7",
			CATE_PIPE_A+"#1-7",
			CATE_PIPE_A+"#3-7",
			CATE_PIPE_F+"#5-7",
			CATE_PIPE_B+"#5-6",
			CATE_PIPE_B+"#5-5"
		);
		
		addItem("破真空閥",vv);
		addBlock(
			CATE_PIPE_A+"#17-7",
			CATE_PIPE_A+"#19-7"
		);

		addItem("M-valve1",mv1);
		addItem("APC-valve",apc);
		addItem("CP1",c_pump1);
		addItem("F-valve1",fv1);
		addBlock(			
			CATE_PIPE_A+"#14-12",	
			CATE_PIPE_A+"#12-12",
			CATE_PIPE_A+"#10-12",
			CATE_PIPE_G+"#8-12",
			CATE_GAUGE +"#8-11",
			CATE_PIPE_D+"#6-12",
			CATE_PIPE_B+"#6-13",
			CATE_PIPE_B+"#6-14"
		);
		addLabel("CP1-temp",9,10);
		addLabel("CP1-press",9,11);
		
		addItem("R-valve",rv);		
		addItem("MP",m_pump);		
		addBlock(
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
		addBlock(			
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
		addLabel("CP2-temp",9,16);
		addLabel("CP2-press",9,17);
		
		//pipe for motor~~~
		addBlock(
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
		
		addBlock(CATE_GAUGE+"#17-3",CATE_PIPE_F+"#17-4");
		addLabel("body-temp" ,18,2);
		addLabel("body-press",18,3);
		addLabel("sub-temp" ,14,2);
		addLabel("sub-press",14,3);
		
		addBrick(
			CATE_WALL_A+"#7-1",
			CATE_WALL_B+"#8-1" ,CATE_WALL_B+"#9-1" ,CATE_WALL_B+"#10-1",CATE_WALL_B+"#11-1",
			CATE_WALL_B+"#12-1",CATE_WALL_B+"#13-1",CATE_WALL_B+"#14-1",CATE_WALL_B+"#15-1",
			CATE_WALL_C+"#16-1",
			CATE_WALL_D+"#16-2",CATE_WALL_D+"#16-3",CATE_WALL_D+"#16-4",CATE_WALL_D+"#16-5",
			CATE_WALL_D+"#16-6",CATE_WALL_D+"#16-7",CATE_WALL_D+"#16-8",CATE_WALL_D+"#16-9",
			CATE_WALL_E+"#16-9",
			CATE_WALL_F+"#15-9",CATE_WALL_F+"#14-9",CATE_WALL_F+"#13-9",CATE_WALL_F+"#12-9",
			CATE_WALL_F+"#11-9",CATE_WALL_F+"#10-9",CATE_WALL_F+"#9-9" ,CATE_WALL_F+"#8-9",
			CATE_WALL_G+"#7-9" ,
			CATE_WALL_H+"#7-8" ,CATE_WALL_H+"#7-7" ,CATE_WALL_H+"#7-6" ,CATE_WALL_H+"#7-5",
			CATE_WALL_H+"#7-4" ,CATE_WALL_H+"#7-3" ,CATE_WALL_H+"#7-2"
		);		
	}
	
	private void addBlock(String... tkn){
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
	//-----------------------------------------//
	
	private class ItmGauge {
		public String name;
		public String unit;
		@SuppressWarnings("rawtypes")
		public ObservableValue prop;
		public double[] limit = {0.,100.};//minimum and maximum value
	};
	
	private ArrayList<ItmGauge> lstInfo = new ArrayList<ItmGauge>();
	
	@SuppressWarnings("rawtypes")
	public void addGauge(String name,String unit,ObservableValue prop){
		if(prop==null){
			return;
		}
		ItmGauge itm = new ItmGauge();
		itm.name = name;
		itm.unit = unit;
		itm.prop = prop;
		lstInfo.add(itm);
	}
	
	@SuppressWarnings("rawtypes")
	public void addGauge(String name,String unit,double min,double max,ObservableValue prop){
		if(prop==null){
			return;
		}
		ItmGauge itm = new ItmGauge();
		itm.name = name;
		itm.unit = unit;
		itm.prop = prop;
		itm.limit[0] = min;
		itm.limit[1] = max;
		lstInfo.add(itm);
	}
	
	@SuppressWarnings("unchecked")
	public Gauge[] listGauge(){
		int cnt = lstInfo.size();
		if(cnt==0){
			return null;
		}
		Gauge[] lst = new Gauge[cnt];
		for(int i=0; i<cnt; i++){
			ItmGauge itm = lstInfo.get(i);
			lst[i] = GaugeBuilder.create()
				//.skinType(SkinType.HORIZONTAL)
				.skinType(SkinType.VERTICAL)
				//.skinType(SkinType.DASHBOARD)
				.title(itm.name)
				.unit(itm.unit)
				.minValue(itm.limit[0])
				.maxValue(itm.limit[1])				
				.build();
			lst[i].valueProperty().bind(itm.prop);
		}
		return lst;
	}
}
