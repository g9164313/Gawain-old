package prj.reheating;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn.CellEditEvent;
import narl.itrc.DevModbus;
import narl.itrc.PanListParameter;
import narl.itrc.PropBundle;

public class DevHDxx {
	
	//HTECH - microprocessor PID controller
	//it support H-D48, H-D72, H-D96Q/H/W
	
	private final ObservableList<PropBundle> lstParm = FXCollections.observableArrayList(
		
		new PropBundle("Sv","設定值",0x02),
			
		new PropBundle("HiSP","溫度上限",0x4D),
		new PropBundle("LoSP","溫度下限",0x4C),
		
		new PropBundle("SoAK","持溫時間(00.00-->時.分)",0x0F),
		new PropBundle("rAmp","溫控斜率(0~200.0,單位/分)",0x10),
		new PropBundle("Pvof","溫度偏差修正(-200~200)",0x11),
		new PropBundle("Pvrr","溫度斜率修正(0.001~9.999)",0x12),
		
		new PropBundle("rPt","程序重複次數",0x2A),
		new PropBundle("StAt","程序啟動模式 0:手動，1:送電，2:記憶斷電",0x2B),
		new PropBundle("PvSt","程序啟動點 0:從0開始，1:從Pv開始",0x2C),
		new PropBundle("Pid","控制模式 0:Pid，1:Level Pid",0x2E),
		new PropBundle("EndP","程序結束模式 0:連續，1:單一組",0x2F),
		
		new PropBundle("Sv1","第1段設定值",0x7E),
		new PropBundle("tP1","第1組升溫時間(00.00-->時.分)",0x7F),
		new PropBundle("tS1","第1組持溫時間(00.00-->時.分)",0x80),
		
		new PropBundle("Sv2","第2段設定值",0x81),
		new PropBundle("tP2","第2組升溫時間(00.00-->時.分)",0x82),
		new PropBundle("tS2","第2組持溫時間(00.00-->時.分)",0x83),
		
		new PropBundle("Sv3","第3段設定值",0x84),
		new PropBundle("tP3","第3組升溫時間(00.00-->時.分)",0x85),
		new PropBundle("tS3","第3組持溫時間(00.00-->時.分)",0x86),
		
		new PropBundle("Sv4","第4段設定值",0x87),
		new PropBundle("tP4","第4組升溫時間(00.00-->時.分)",0x88),
		new PropBundle("tS4","第4組持溫時間(00.00-->時.分)",0x89),
		
		new PropBundle("Sv5","第5段設定值",0x8A),
		new PropBundle("tP5","第5組升溫時間(00.00-->時.分)",0x8B),
		new PropBundle("tS5","第5組持溫時間(00.00-->時.分)",0x8C),
		
		new PropBundle("Sv6","第6段設定值",0x8D),
		new PropBundle("tP6","第6組升溫時間(00.00-->時.分)",0x8E),
		new PropBundle("tS6","第6組持溫時間(00.00-->時.分)",0x8F),
		
		new PropBundle("Sv7","第7段設定值",0x90),
		new PropBundle("tP7","第7組升溫時間(00.00-->時.分)",0x91),
		new PropBundle("tS7","第7組持溫時間(00.00-->時.分)",0x92),
		
		new PropBundle("AL1F","第1組警報功能 - 1:SV值加上限，2：SV值加下限，3：超過AL1S，4：低於AL1S，5：SV值在偏差內，6：SV值超過偏差",0x30),
		new PropBundle("AL2F","第2組警報功能 - 1:SV值加上限，2：SV值加下限，3：超過AL1S，4：低於AL1S，5：SV值在偏差內，6：SV值超過偏差",0x34),		
		new PropBundle("AL3F","第3組警報功能 - 1:SV值加上限，2：SV值加下限，3：超過AL1S，4：低於AL1S，5：SV值在偏差內，6：SV值超過偏差",0x38),
		
		new PropBundle("AL1S","第1組警報設定值",0x06),
		new PropBundle("AL1u","第1組警報上限值",0x08),
		new PropBundle("AL1l","第1組警報下限值",0x07),
				
		new PropBundle("AL2S","第2組警報設定值",0x09),
		new PropBundle("AL2u","第2組警報上限值",0x0B),
		new PropBundle("AL2l","第2組警報下限值",0x0A),
				
		new PropBundle("AL2S","第3組警報設定值",0x0C),
		new PropBundle("AL2u","第3組警報上限值",0x0E),
		new PropBundle("AL2l","第3組警報下限值",0x0D),
		
		new PropBundle("unit","單位 ºC/ºF",0x52)	
	);
	
	class PanSetting extends PanListParameter {

		public PanSetting(){
			super(lstParm);
			addButton("更新資料",event);
		}
		
		private final int BUF_OFF1 = 0x4B;
		private short[] buf00 = new short[75];
		private short[] buf4B = new short[75];
		
		@Override
		public void refreshData() {
			//mapping to buffer~~~
			dev.readR(0x00,buf00);
			dev.readR(BUF_OFF1,buf4B);
			for(PropBundle itm:lstParm){
				int idx = itm.arg1.get();
				if(idx<BUF_OFF1){
					itm.value.set(String.valueOf(buf00[idx]));
				}else{
					idx = idx - BUF_OFF1;
					itm.value.set(String.valueOf(buf4B[idx]));
				}				
			}
			return;
		}
		@Override
		public void updateData(CellEditEvent<PropBundle, String> event) {
			PropBundle itm = event.getRowValue();
			String txt = event.getNewValue();
			try{				
				short val = Short.valueOf(txt);				
				int addr = itm.arg1.get();
				final short[] buf = {val};
				dev.write(addr,buf);
				itm.value.set(txt);
			}catch(NumberFormatException e){
				return;
			}
		}
		private EventHandler<ActionEvent> event = new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				refreshData();
			}
		};
	};
	
	public PanSetting panel = new PanSetting();
	
	private DevModbus dev = new DevModbus(); 
	
	public DevHDxx(){
	}
	
	public DevHDxx(String name){
		open(name);
	}
	
	public void open(String name){
		if(name==null){
			return;
		}
		if(name.length()==0){ 
			return; 
		}
		dev.open(name);
	}
	
	/*private double simulate(double min,double max){
		double time = (System.currentTimeMillis()%2000L);
		return (1.+(Math.sin((time*Math.PI)/2000.))*(max-min))/2.;
	}*/

	public double getValue(){
		//return simulate(0.,200.);//just a simulation~~~
		return get_val(0x100,100.);
	}
	
	public double getUpper(){
		return get_val(0x4D,400.);
	}
	
	public double getLower(){
		return get_val(0x4C,0.);
	}
	
	private double get_val(int addr,double def){
		if(dev.isValid()==false){
			return def;
		}
		final short[] buf = {0};
		dev.readR(addr,buf);
		return 0.1*((double)buf[0]);
	}
	
	public void setValue(double val){
		if(dev.isValid()==false){
			return;
		}
		final short[] buf = {(short)(val*10.)};
		dev.write(0x2,buf);
	}
}
