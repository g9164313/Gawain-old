package prj.shelter;

import java.math.BigDecimal;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import jssc.SerialPort;
import jssc.SerialPortException;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;

/**
 * Unknown machine lathes controller.<p>
 * Someone modify this lathes for radiation experiment.<p> 
 * @author qq
 *
 */
public class DevHustIO extends DevTTY {
	
	private final byte NUL = 0;
	private final byte CR_ = 0x0D;	
	//private final byte DC1 = 17;
	private final byte DC2 = 0x12;
	//private final byte DC3 = 19;	
	private final byte DC4 = 0x14;
	private final byte PER = 37;//'%', percent
	
	public DevHustIO(){
		TAG = "HUSTIO";
	}
	@Override
	public void afterOpen() {
		exec("O9000","N00000010000000001");//start report
		final String LOOPER = "looper"; 
		addState(LOOPER, ()->looper());
		playFlow(LOOPER);
	}
	@Override
	public void beforeClose(){
		exec("O9000","N1");//stop report
		stop_radiaton();
	}
	
	private void looper() {
		try {
			final String txt = make_report();
			parse_report(txt);
		} catch (SerialPortException e) {
			Misc.loge("[%s] make report - %s", TAG, e.getMessage());
		}
	}
	
	///** method.1
	private String make_report() throws SerialPortException{
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			return "";
		}
		char cc;
		do{
			cc = (char)dev.readBytes(1)[0];
			if(cc==NUL || cc==DC4){
				continue;
			}else if(cc==DC2){
				break;
			}			
		}while(true);
		String txt = "";
		do{
			cc = (char)dev.readBytes(1)[0];
			if(cc==NUL){
				continue;
			}else if(cc==DC2 || cc==DC4){
				break;
			}else{
				txt = txt + cc;
			}
		}while(true);
		return txt;
	}

	public final StringProperty H_code = new SimpleStringProperty();//lathe status
	public final StringProperty O_code = new SimpleStringProperty();
	public final StringProperty R_code = new SimpleStringProperty();
	public final StringProperty U_code = new SimpleStringProperty();//location, unit is 'mm'
	
	public final BooleanProperty isMoving = new SimpleBooleanProperty();
	public final BooleanProperty isRadiant= new SimpleBooleanProperty();
	
	public final StringProperty locationText = new SimpleStringProperty("");
	public final StringProperty leftTimeText = new SimpleStringProperty("00:00:00");
	
	private long left_time_start = -1L;//unit is millisecond
	private long left_time_count = -1L;//unit is millisecond
	private long left_time_total = -1L;//unit is millisecond
	
	private boolean radiate, move_on;
	
	private void parse_report(final String report){
		
		String txt = report
			.replaceAll("[%|\r]","")
			.trim();
		
		//O9001H007004U 0500.000R 00000018 <-- 靜止
		//O9001H20?104U 5911.257R 00000018 <-- 移動中		
		if(txt.matches("[O]\\p{ASCII}{4}[H]\\p{ASCII}{6}?[U]\\p{ASCII}{9}[R]\\p{ASCII}{9}")==false) {
			Misc.loge("%s) wrong report-->%s", TAG, txt);
			return;
		}
		//speed parsing~~~
		final String o_val = txt.substring(1,5);
		final String h_val = txt.substring(6,12);
		final String u_val = txt.substring(13,22).trim();
		final String r_val = txt.substring(23).trim();
		
		radiate = (r_val.charAt(6)!='1')?(true):(false);
		move_on = (h_val.charAt(2)=='?')?(true):(false);
		
		if(radiate==true) {
			if(left_time_start>=0L) {
				left_time_count = System.currentTimeMillis() - left_time_start;				
			}
			if(left_time_total>=0L) {
				if(left_time_count>=left_time_total) {
					stop_radiaton();
				}
			}
		}
		
		Application.invokeLater(()->{
			
			H_code.setValue(h_val);
			O_code.setValue(o_val);
			R_code.setValue(r_val);
			U_code.setValue(u_val);
			
			isRadiant.setValue(radiate);
			isMoving.setValue(move_on);
			
			BigDecimal u_dec = new BigDecimal(u_val);
			final int pp = u_dec.precision();//digital的數量
			final int ss = u_dec.scale();//多少 digital在逗點右邊
			if((pp-ss)>=2) {
				u_dec = u_dec.movePointLeft(1);
				locationText.set(u_dec.toString()+" cm");
			}else {
				locationText.set(u_val+" mm");
			}
			leftTimeText.set(Misc.tick2text(
				left_time_count,
				false, 3
			));
		});
	}
	
	public void exec(final String... cmd){
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			return;
		}
		try {
			dev.writeByte(DC2);
			dev.writeByte(PER);
			dev.writeByte(CR_);
			for(String txt:cmd){
				dev.writeString(txt);
				dev.writeByte(CR_);
			}
			dev.writeByte(PER);
			dev.writeByte(DC4);
		} catch (SerialPortException e) {
			Misc.loge("[%s] exec - %s", TAG, e.getMessage());
		}
	}
			
	private void make_radiaton(final Strength act) {
		switch(act) {
		case V_3Ci  : exec("O9000","N0000111","M03"); break;
		case V_05Ci : exec("O9000","N0000111","M04"); break;
		case V_005Ci: exec("O9000","N0000111","M05"); break;
		}
		exec("O9005","N10000000");
		left_time_start = System.currentTimeMillis();
	}
	private void stop_radiaton() {		
		exec("O9005","N01000000");
		left_time_start = -1L;
		left_time_total = -1L;
	}
	
	/**
	 * move tools to position form home point.<p>
	 * The position represent a physical number.(value and unit).<p>
	 * Example: "33cm", "12.5mm".<p>
	 * If position is empty, it means 'home/origin point'.
	 * The position is an absolute location.<p> 
	 * @param position - physical number
	 */
	public void move_to_abs(final String position){
		if(position.length()==0){
			exec("O9005","N00100000");//go home
		}else{
			if(position.matches("[\\d]]+(?:[.]\\d+)?")==true) {
				exec("O9000","N0000111","G01X"+position);
			}else {
				String val = UtilPhysical.convertScale(position,"mm");
				if(val.length()==0) {
					return;//TODO:~~~
				}
				if(val.contains(".")==false) {
					val = val +".000";
				}
				exec("O9000","N0000111",String.format("G01X%s",val));
			}
		}
	}
	public void move_to_abs(
		final double value, 
		final String unit
	){
		double val = UtilPhysical.convert(value, unit, "mm");
		if(val<=0.){
			exec("O9005","N00100000");//go home
		}else{
			exec("O9000","N0000111",String.format("G01X%.2f",val));
		}
	}
	//-------------------------------------------//

	public static enum Strength {
		V_005Ci,V_05Ci,V_3Ci;
		public String toString() {
			return conv_activity.toString(this);
		}
	};

	public static StringConverter<DevHustIO.Strength> conv_activity = new StringConverter<DevHustIO.Strength>() {
		final String TXT_3Ci  = "3 Ci";
		final String TXT_05Ci = "0.5 Ci";
		final String TXT_005Ci= "0.05 Ci";
		@Override
		public String toString(Strength act) {
			switch(act) {
			case V_3Ci  : return TXT_3Ci;
			case V_05Ci : return TXT_05Ci;
			case V_005Ci: return TXT_005Ci;
			}
			return "???";
		}
		@Override
		public Strength fromString(String txt) {
			if(txt.equals(TXT_3Ci)==true) {
				return DevHustIO.Strength.V_3Ci;
			}else if(txt.equals(TXT_05Ci)==true) {
				return DevHustIO.Strength.V_05Ci;
			}else if(txt.equals(TXT_005Ci)==true) {
				return DevHustIO.Strength.V_005Ci;
			}
			return DevHustIO.Strength.V_005Ci;
		}
	};
	
	public final SimpleObjectProperty<Strength> activity = new SimpleObjectProperty<Strength>(Strength.V_005Ci);
	
	public DevHustIO setLeftTime(final long left_time) {
		left_time_total = left_time;
		return this;
	}
	public long getLeftCount() {
		return left_time_count;
	}
	
	public void asyncRadiation(
		final Strength act_value,
		final long left_time
	){
		if(isRadiant.get()==true) {
			return;
		}
		activity.set(act_value);
		asyncRadiation(left_time);
	}
	public void asyncRadiation(
		final long left_time
	){
		if(isRadiant.get()==true) {
			return;
		}
		final Strength act_value = activity.get();
		asyncBreakIn(()->{
			left_time_total = left_time;
			make_radiaton(act_value);
		});
	}

	public void asyncHaltOn(){asyncBreakIn(()->{
		stop_radiaton();
	});}
	
	public void asyncMoveTo(
		final String position
	) {
		asyncBreakIn(()->{
		move_to_abs(position);
	});}
	
	public void asyncMoveTo(
		final double value, 
		final String unit
	) {
		asyncBreakIn(()->{
		move_to_abs(value, unit);
	});}
	
	/*public void asyncWorking(
		final String position,
		final long left_time		
	) {
		if(isRadiant.get()==true || isMoving.get()==true) {
			return;
		}
		final Activity act_value = activity.get();
		asyncBreakIn(()->{
			try {			
				move_to_abs(position);			
				do {
					looper();			
					TimeUnit.MILLISECONDS.sleep(100);			
				}while(move_on==true);		
			
				left_time_total = left_time;				
				make_radiaton(act_value);			
				do {
					looper();
					TimeUnit.MILLISECONDS.sleep(100);
				}while(radiate==true);
				
			} catch (InterruptedException e) {
			}
		});
	}
	
	public void asyncWorking(
		final String position,
		final Activity act_value,
		final long left_time		
	) {
		if(isRadiant.get()==true || isMoving.get()==true) {
			return;
		}
		activity.set(act_value);
		asyncWorking(position,left_time);
	}*/
	
	/*
	 * Get location numeric value, unit is 'mm'.
	 */
	public float get_location_mm() {
		try {
			return Float.parseFloat(U_code.get());
		}catch(NumberFormatException e) {
			return 0f;
		}
	}
	public float get_location_cm() {
		return get_location_mm()/10f;
	}
	
	public boolean isMoving() {
		return isMoving.get();
	}
	public boolean isRadiant() {
		return isRadiant.get();
	} 
	//-------------------------------------------//

	public static Pane genPanel(final DevHustIO dev){
		
		final JFXTextField box_loca = new JFXTextField();
		final JFXTextField box_make = new JFXTextField();
		final JFXButton btn_loca = new JFXButton("移動");
		final JFXButton btn_make = new JFXButton("照射");
		final JFXButton btn_stop = new JFXButton("中止");
		
		final ToggleGroup grp_act = new ToggleGroup();
		final JFXRadioButton[] rad_act = {
			new JFXRadioButton("3Ci"),
			new JFXRadioButton("0.5Ci"),
			new JFXRadioButton("0.05Ci")			
		};
		rad_act[0].setToggleGroup(grp_act);
		rad_act[1].setToggleGroup(grp_act);
		rad_act[2].setToggleGroup(grp_act);		
		grp_act.selectToggle(rad_act[0]);
		rad_act[0].setOnAction(e->dev.activity.set(Strength.V_3Ci));
		rad_act[1].setOnAction(e->dev.activity.set(Strength.V_05Ci));
		rad_act[2].setOnAction(e->dev.activity.set(Strength.V_005Ci));
		
		box_loca.setPromptText("EX: 10cm");
		box_loca.setText("10 cm");
		box_loca.setOnAction(e->{
			try{
				dev.asyncMoveTo(box_loca.getText());
			}catch(NumberFormatException err){
				PanBase.notifyError("","不合法的數字");
			}
		});
		
		box_make.setPromptText("範例 3:27(3分27秒)");
		box_make.setText("01:00");
		box_make.setPrefColumnCount(8);
		box_make.setOnAction(e->{
			final long tick = Misc.text2tick(box_make.getText());
			if(tick==0) {
				PanBase.notifyInfo("","沒有照射時間!!");
				return;
			}
			dev.asyncRadiation(tick);
		});
		
		btn_loca.getStyleClass().add("btn-raised-1");
		btn_loca.setMaxWidth(Double.MAX_VALUE);
		btn_loca.setOnAction(e->box_loca.getOnAction().handle(e));
		
		btn_make.getStyleClass().add("btn-raised-1");
		btn_make.setMaxWidth(Double.MAX_VALUE);
		btn_make.disableProperty().bind(dev.isRadiant);
		btn_make.setOnAction(e->box_make.getOnAction().handle(e));
		
		btn_stop.getStyleClass().add("btn-raised-0");
		btn_stop.setMaxWidth(Double.MAX_VALUE);
		btn_stop.setOnAction(e->dev.asyncHaltOn());

		final Label[] inf = {
			new Label(),
			new Label(),
			new Label(),
			new Label(),
			new Label()
		};
		inf[0].textProperty().bind(dev.H_code);
		inf[1].textProperty().bind(dev.O_code);
		inf[2].textProperty().bind(dev.R_code);
		inf[3].textProperty().bind(dev.U_code);
		inf[4].textProperty().bind(dev.leftTimeText);
		
		final JFXCheckBox[] chk = {
			new JFXCheckBox("移動中"),
			new JFXCheckBox("照射中")
		};
		chk[0].selectedProperty().bind(dev.isMoving);
		chk[1].selectedProperty().bind(dev.isRadiant);
		
		//----------------------------//
				
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","font-console");
		lay0.addRow(0, new Label("H code："), inf[0]);
		lay0.addRow(1, new Label("O code："), inf[1]);
		lay0.addRow(2, new Label("R code："), inf[2]);
		lay0.addRow(3, new Label("U code："), inf[3]);
		lay0.addRow(4, chk[0],chk[1]);
		lay0.addRow(5, new Label("照射時間"), inf[4]);
		lay0.add(new Separator(), 0, 6, 2, 1);
		lay0.add(rad_act[0], 0, 7, 2, 1);
		lay0.add(rad_act[1], 0, 8, 2, 1);
		lay0.add(rad_act[2], 0, 9, 2, 1);
		lay0.addRow(10, new Label("載台位置"), box_loca);
		lay0.addRow(11, new Label("設定計時"), box_make);
		lay0.add(btn_loca, 0,12, 2, 1);
		lay0.add(btn_make, 0,13, 2, 1);	
		lay0.add(btn_stop, 0,14, 2, 1);	
		return lay0;
	}	
}
