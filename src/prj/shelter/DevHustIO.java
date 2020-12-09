package prj.shelter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
	
	private final char NUL = 0;
	private final char CR_ = 0x0D;
	//private final char DC1 = 17;
	private final char DC2 = 0x12;
	//private final char DC3 = 19;
	private final char DC4 = 0x14;
	
	public DevHustIO(){
		TAG = "Hust-IO";
		readTimeout=50;
		flowControl=2;
	}
	public DevHustIO(final String path){
		this();
		setPathName(path);
	}

	///** method.1
	private String make_report(){
		char cc;
		do{
			cc = (char)readByte();
			if(cc==NUL){
				continue;
			}else if(cc==DC2){
				break;
			}else if(cc==DC4){
				continue;
			}			
		}while(true);
		String txt = "";
		do{
			cc = (char)readByte();
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
	
	public final StringProperty location = U_code;//= new SimpleStringProperty();

	public final StringProperty remainTime = new SimpleStringProperty("00:00:00");
	
	private long radiation_start = -1L;
	private long radiation_time = -1L;
	
	private AtomicInteger reamin_time = new AtomicInteger(-1);
	
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
		
		final boolean radiant= (r_val.charAt(6)!='1')?(true):(false);
		final boolean moving = (h_val.charAt(2)=='?')?(true):(false);
		
		if(radiant==true) {
			if(radiation_start<0L) {
				radiation_start = System.currentTimeMillis();
				reamin_time.set((int)radiation_time);
			}else {
				long count = System.currentTimeMillis() - radiation_start;
				if(radiation_time>0 && count>=radiation_time) {
					exec("O9005","N01000000");//stop radiation~~~
					radiation_time = -1L;
					reamin_time.set(0);
				}else {
					reamin_time.set((int)(radiation_time-count));
				}
			}
		}else{
			radiation_start = -1L;//for next turn~~~
		}
		
		Application.invokeAndWait(()->{
			
			H_code.setValue(h_val);
			O_code.setValue(o_val);
			R_code.setValue(r_val);
			U_code.setValue(u_val);
			
			isRadiant.setValue(radiant);
			isMoving.setValue(moving);
			
			remainTime.set(Misc.tick2text(reamin_time.get(),false,3));
		});
	}
	
	public void exec(final String... cmd){
		String CMD = "";;
		for(String txt:cmd){
			CMD = CMD + txt + CR_;
		}
		byte[] buf = String.format(
			"%c%c%c%s%c%c",
			DC2,'%',CR_,
			CMD,
			'%',DC4
		).getBytes();
		writeByteDelay(2,buf);
	}
	
	private void make_radiaton(final String isotope) {
		exec("O9000","N0000111",isotope);
		exec("O9005","N10000000");
	}
	private void stop_radiaton() {
		exec("O9005","N01000000");
	}
	
	@Override
	protected void afterOpen() {
		exec("O9000","N00000010000000001");//start report
		final String LOOPER = "looper"; 
		addState(LOOPER, ()->parse_report(make_report()));
		playFlow(LOOPER);
	}
	@Override
	protected void beforeClose(){
		exec("O9000","N1");//stop report
		stop_radiaton();
	}
	//-------------------------------------------//

	public static final String ISOTOPE_3Ci  = "M03";
	public static final String ISOTOPE_05Ci = "M04";
	public static final String ISOTOPE_005Ci= "M05";
	
	public void makeRadiation(
		final String isotope,
		final int time_val
	){asyncBreakIn(()->{
		make_radiaton(isotope);
		radiation_time = time_val;
	});}
	public void stopRadiation(){
		//R_code --> 00000018
		asyncBreakIn(()->stop_radiaton());
	}
	
	/**
	 * move tools to position form home point.<p>
	 * The position represent a physical number.(value and unit).<p>
	 * Example: "33cm", "12.5mm".<p>
	 * If position is empty, it means 'home/origin point'.
	 * The position is an absolute location.<p> 
	 * @param position - physical number
	 */
	public void moveToAbs(
		final String position
	){asyncBreakIn(()->{
		if(position.length()==0){
			exec("O9005","N00100000");//go home
		}else{
			double val = UtilPhysical.convert(position, "mm");
			if(val<=0.){
				exec("O9005","N00100000");//go home
			}else{
				exec("O9000","N0000111",String.format("G01X%.2f",val));
			}			
		}
	});}
	public void moveToAbs(
		final double value, 
		final String unit
	){asyncBreakIn(()->{
		double val = UtilPhysical.convert(value, unit, "mm");
		if(val<=0.){
			exec("O9005","N00100000");//go home
		}else{
			exec("O9000","N0000111",String.format("G01X%.2f",val));
		}
	});}
	//-------------------------------------------//

	public static Pane genPanelD(final DevHustIO dev){

		JFXTextField box_loca = new JFXTextField();
		box_loca.setPromptText("絕對位置");
		box_loca.setText("10 cm");
		box_loca.setPrefColumnCount(8);
		box_loca.setLabelFloat(true);
		box_loca.setOnAction(e->{			
			String[] val = UtilPhysical.split(box_loca.getText());
			if(val[1].length()==0){
				PanBase.notifyError("","非法的物理量");
				return;
			}
			try{
				dev.moveToAbs(Double.valueOf(val[0]), val[1]);
			}catch(NumberFormatException err){
				PanBase.notifyError("","非法的數字");
			}
		});
				
		ToggleGroup grp = new ToggleGroup();
		JFXRadioButton[] rad = {
			new JFXRadioButton("0.05Ci"),
			new JFXRadioButton("0.5Ci"),
			new JFXRadioButton("3Ci")			
		};
		rad[0].setToggleGroup(grp);
		rad[0].setUserData(ISOTOPE_005Ci);
		rad[1].setToggleGroup(grp);
		rad[1].setUserData(ISOTOPE_05Ci);
		rad[2].setToggleGroup(grp);
		rad[2].setUserData(ISOTOPE_3Ci);
		grp.selectToggle(rad[0]);
		
		JFXButton[] btn = {
			new JFXButton("move"),
			new JFXButton("home"),
			new JFXButton("照射"),
			new JFXButton("關閉"),
		};
		
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setMaxWidth(Double.MAX_VALUE);
		btn[0].setOnAction(e->dev.moveToAbs(box_loca.getText()));
		
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setMaxWidth(Double.MAX_VALUE);
		btn[1].setOnAction(e->dev.moveToAbs(""));
		
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setMaxWidth(Double.MAX_VALUE);
		btn[2].setOnAction(e->{
			String isotope = (String)grp.getSelectedToggle().getUserData();
			dev.makeRadiation(isotope,3000);
		});
		
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setMaxWidth(Double.MAX_VALUE);
		btn[3].setOnAction(e->dev.stopRadiation());
		
		/*btn[4].getStyleClass().add("btn-raised-3");
		btn[4].setMaxWidth(Double.MAX_VALUE);
		btn[4].setOnAction(e->dev.exec("O9000","N00000010000000001"));
		btn[5].getStyleClass().add("btn-raised-3");
		btn[5].setMaxWidth(Double.MAX_VALUE);
		btn[5].setOnAction(e->dev.exec("O9000","N1"));*/
		
		final Label[] code = {
			new Label(),
			new Label(),
			new Label(),
			new Label()
		};
		code[0].textProperty().bind(dev.H_code);
		code[1].textProperty().bind(dev.O_code);
		code[2].textProperty().bind(dev.R_code);
		code[3].textProperty().bind(dev.U_code);
		
		final JFXCheckBox[] chk = {
			new JFXCheckBox("移動中"),
			new JFXCheckBox("照射中")
		};
		chk[0].selectedProperty().bind(dev.isMoving);
		chk[1].selectedProperty().bind(dev.isRadiant);
		
		VBox lay1 = new VBox(
			new Label(),
			box_loca,
			btn[0],	btn[1],
			rad[0],	rad[1],	rad[2],
			btn[2], btn[3]
		);
		lay1.getStyleClass().addAll("box-pad","font-console");
				
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","font-console");
		lay0.addRow(0, new Label("H"), code[0]);
		lay0.addRow(1, new Label("O"), code[1]);
		lay0.addRow(2, new Label("R"), code[2]);
		lay0.addRow(3, new Label("U"), code[3]);
		lay0.add(chk[0],0,4,2,1);
		lay0.add(chk[1],0,5,2,1);
		
		return new HBox(lay0,lay1);
	}	
}

/**
 * 		String t_loca = lst_code[3];
		try {
			float loca = Float.valueOf(t_loca);//location, unit is 'mm'
			if(loca<=10f) {
				t_loca = String.format("%8.3f mm", t_loca);
			}else {
				loca = loca / 10f;//unit is 'cm'
				t_loca = String.format("%8.3f cm", t_loca);
			}
		}catch(NumberFormatException e) {
			Misc.loge("%s) wrong U_code-->%s", TAG, t_loca);
		}
		final String txt_loca = t_loca;
 */

/**
 
 	//* method.2
	private boolean make_report(){
		report = readTxt(".*[\\x12].{5,}+[\\x14].*");
		int beg = report.lastIndexOf(DC2);
		int end = report.lastIndexOf(DC4);
		if(end<0 || beg<0){
			return false;
		}
		report = report.substring(beg+1,end);
		return true;
	}

 	private void parse_report(final String report){
		String txt = report
			.replaceAll("[%|\r]","")
			.trim();
		//Misc.logv("[TAG]%s", txt);//debug
		String[] lst_code = {
			null,
			null,
			null,
			null
		};
		int[] pos = {
			txt.indexOf('H'),
			txt.indexOf('O'),
			txt.indexOf('R'),
			txt.indexOf('U'),
			txt.length()
		};
		Arrays.sort(pos);
		for(int i=0; i<pos.length-1; i++){
			if(pos[i]<0){
				continue;
			}
			lst_code[i] = txt.substring(pos[i],pos[i+1]).trim();
		}
		
		Application.invokeAndWait(()->{
			for(String val:lst_code){
				if(val==null){
					continue;
				}
				char tkn = val.charAt(0);
				val = val.substring(1).trim();				
				switch(tkn){
				case 'H': H_code.setValue(val); break;
				case 'O': O_code.setValue(val); break;
				case 'R': R_code.setValue(val); break;
				case 'U': U_code.setValue(val);	break;
				}
			}
			location.setValue(txt_loca);
			if(radition_flag==true){
				remain_t.setValue(Misc.tick2text(
					 System.currentTimeMillis() - radition_tick
				));
			}
		});
	}

 * */
