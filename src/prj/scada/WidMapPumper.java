package prj.scada;

import java.util.ArrayList;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import eu.hansolo.medusa.Gauge.SkinType;
import narl.itrc.WidDiagram;

public class WidMapPumper extends WidDiagram {

	private DevFatek dev = null;
	
	public WidMapPumper(DevFatek device){
		dev = device;
		default_layout();
	}
	//-----------------------------------------//
	
	private ItmPart tower_lamp = new ItmPart(CATE_TOWER, 7.5, 2){
		@Override
		protected void eventInit(){
			clickable = false;		
		}
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private ItmPart chuck_sub = new ItmPart(CATE_CHUCKB, 11, 1.5){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private ItmPart ar_valve0 = new ItmPart(CATE_VALVE, 2, 1){
		@Override
		protected void eventInit(){
			addLabel("Ar ",2,2);			
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#0-1","#1-1","#3-1");
		}
	};
	private ItmPart ar_valve1 = new ItmPart(CATE_VALVE, 4, 1){
		@Override
		protected void eventInit(){
			addLabel("GV1",4,2);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#3-1","#5-1","#5-2","#5-3");
		}
	};
	
	private ItmPart o2_valve0 = new ItmPart(CATE_VALVE, 2, 4){
		@Override
		protected void eventInit(){
			addLabel("O2",2,5);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#0-4","#1-4","#3-4");
		}
	};
	private ItmPart o2_valve1 = new ItmPart(CATE_VALVE, 4, 4){
		@Override
		protected void eventInit(){
			addLabel("GV2",4,5);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#3-4","#5-4","#6-4");
		}
	};
	
	private ItmPart n2_valve0 = new ItmPart(CATE_VALVE, 2, 7){
		@Override
		protected void eventInit(){
			addLabel("N2",2,8);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#0-7","#1-7","#3-7");
		}
	};
	private ItmPart n2_valve1 = new ItmPart(CATE_VALVE, 4, 7){
		@Override
		protected void eventInit(){
			addLabel("GV3",4,8);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#3-7","#5-7","#5-6","#5-5");
		}
	};
	
	private ItmPart brk_valve = new ItmPart(CATE_VALVE, 18, 7){
		@Override
		protected void eventInit(){
			addLabel("VV",18,8);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#17-7","#19-7");
		}
	};
	
	private ItmPart r_valve = new ItmPart(CATE_VALVE, 13, 15){
		@Override
		protected void eventInit(){
			addLabel("RV",13,16);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,
				"#5-15","#6-15","#7-15","#8-15",
				"#9-15","#10-15","#11-15","#12-15",
				"#14-15"
			);
		}
	};
	private ItmPart m_pump = new ItmPart(CATE_M_PUMP, 4, 15){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private ItmPart m_valve1 = new ItmPart(CATE_VALVE, 13, 12){
		@Override
		protected void eventInit(){
			addLabel("MV1",13,13);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#12-12","#14-12");
		}
	};
	private ItmPart apc_valve = new ItmPart(CATE_VALVE_BL, 11, 12){
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#10-12","#12-12");
		}
	};
	private ItmPart c_pump1 = new ItmPart(CATE_C_PUMP, 9, 12){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	private ItmPart f_valve1 = new ItmPart(CATE_VALVE, 7, 12){
		@Override
		protected void eventInit(){
			addLabel("FV1",7,13);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#6-14","#6-13","#6-12","#8-12");
		}
	};
	
	private ItmPart m_valve2 = new ItmPart(CATE_VALVE, 13, 18){
		@Override
		protected void eventInit(){
			addLabel("MV2",13,19);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#11-18","#10-18","#12-18","#14-18");
		}
	};	
	private ItmPart c_pump2 = new ItmPart(CATE_C_PUMP, 9, 18){
		@Override
		public void handle(MouseEvent event) {
		}
	};	
	private ItmPart f_valve2 = new ItmPart(CATE_VALVE, 7, 18){
		@Override
		protected void eventInit(){
			addLabel("FV2",7,19);
		}
		@Override
		public void handle(MouseEvent event) {
			pipe_switch(flag,"#8-18","#6-16","#6-17","#6-18");
		}
	};
	
	private void default_layout(){
		mapPart.put("chuck",chuck_sub);
		mapPart.put("tower",tower_lamp);
		
		mapPart.put("Ar-valve0",ar_valve0);		
		mapPart.put("Ar-valve1",ar_valve1);
		addBlock(
			CATE_BATTLE+"#0-2",
			CATE_PIPE_D+"#0-1",
			CATE_PIPE_A+"#1-1",
			CATE_PIPE_A+"#3-1",
			CATE_PIPE_E+"#5-1",
			CATE_PIPE_B+"#5-2",
			CATE_PIPE_B+"#5-3"
		);
		
		mapPart.put("O2-valve0",o2_valve0);		
		mapPart.put("O2-valve1",o2_valve1);
		addBlock(
			CATE_BATTLE+"#0-5",
			CATE_PIPE_D+"#0-4",
			CATE_PIPE_A+"#1-4",
			CATE_PIPE_A+"#3-4",
			CATE_PIPE_K+"#5-4",
			CATE_PIPE_A+"#6-4"
		);
		
		mapPart.put("N2-valve0",n2_valve0);
		mapPart.put("N2-valve1",n2_valve1);
		addBlock(
			CATE_BATTLE+"#0-8",
			CATE_PIPE_D+"#0-7",
			CATE_PIPE_A+"#1-7",
			CATE_PIPE_A+"#3-7",
			CATE_PIPE_F+"#5-7",
			CATE_PIPE_B+"#5-6",
			CATE_PIPE_B+"#5-5"
		);
		
		mapPart.put("破真空閥",brk_valve);
		addBlock(
			CATE_PIPE_A+"#17-7",
			CATE_PIPE_A+"#19-7"
		);

		mapPart.put("M-valve1",m_valve1);
		mapPart.put("APC-valve",apc_valve);
		mapPart.put("CP1",c_pump1);
		mapPart.put("F-valve1",f_valve1);
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
		addLabel("CP1-press",9,11);
		
		mapPart.put("R-valve",r_valve);		
		mapPart.put("MP",m_pump);		
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
		
		mapPart.put("M-valve2",m_valve2);
		mapPart.put("CP2",c_pump2);
		mapPart.put("F-valve2",f_valve2);
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
		
		addBlock(CATE_GAUGE+"#17-2",CATE_PIPE_F+"#17-3");
		addBlock(CATE_GAUGE+"#17-4",CATE_PIPE_F+"#17-5");
		addLabel("body-press",18,2);
		addLabel("body-temp" ,18,4);
		
		addLabel("sub-press",14,2);
		addLabel("sub-temp" ,14,4);
		
		addBlock(
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

		redraw();//important!! show everything~~~~
	}
		
	private void addBlock(String... tkn){
		for(int i=0; i<tkn.length; i++){
			String[] arg = tkn[i].split("#");
			String[] pos = arg[1].split("-");
			int gx = Integer.valueOf(pos[0]);
			int gy = Integer.valueOf(pos[1]);
			mapPart.put("#"+arg[1],new ItmPart(arg[0],gx,gy){
				@Override
				public void handle(MouseEvent event) {
				}
			});
		}
	}

	private void pipe_switch(int flag,String... list){
		for(String name:list){
			mapPart.get(name).flag = flag;			
		}
		redraw();
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
