package prj.shelter;

import java.util.Arrays;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

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
	
	private static final String UNKNOWN = "＊＊＊＊";
	
	private final char NUL = 0;
	private final char CR_ = 0x0D;
	//private final char DC1 = 17;
	private final char DC2 = 0x12;
	//private final char DC3 = 19;
	private final char DC4 = 0x14;
	
	public DevHustIO(){
		TAG = "Hust-IO";
		readTimeout = 5000;//why so long?
		flowControl = 2;//remember~~~
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
	//*/
	
	/** method.2
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
	*/
	
	public final StringProperty H_code = new SimpleStringProperty(UNKNOWN);//lathe status
	public final StringProperty O_code = new SimpleStringProperty(UNKNOWN);
	public final StringProperty R_code = new SimpleStringProperty(UNKNOWN);
	public final StringProperty U_code = new SimpleStringProperty(UNKNOWN);//location
	
	public final StringProperty location= U_code;
	
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
				case 'U': U_code.setValue(val); break;
				}
			}
			if(radition_flag==true){
				remain_t.setValue(Misc.tick2time(
					 System.currentTimeMillis() - radition_tick
				));
			}
		});
	}
	
	private void looper() {
		parse_report(make_report());
	}

	protected void afterOpen() { 
		final String LOOPER = "looper"; 
		addState(LOOPER, ()->looper());
		playFlow(LOOPER);		
		exec("O9000","N00000010000000001");//start report
	}
	@Override
	protected void beforeClose(){		
		stopRadiation();
		exec("O9000","N1");//stop report
	}
	//-------------------------------------------//
	
	public void exec(final String... cmd){
		final byte[] beg = {DC2,'%',CR_};
		final byte[] end = {'%',DC4};
		writeByte(beg);
		for(String txt:cmd){
			writeTxt(txt+CR_);
		}
		writeByte(end);
	}
	
	private String m_code="";
	
	private boolean radition_flag = false;
	private long radition_tick = -1;
	
	public final StringProperty isotope = new SimpleStringProperty("0.05Ci");//default 	
	public final StringProperty remain_t= new SimpleStringProperty(UNKNOWN);//remain time
	
	public void makeRadiation(){		
		Misc.invoke(()->{
			String name = isotope.get()
				.toLowerCase()
				.replaceAll("\\s", "");
			if(name.equals("3ci")==true){
				m_code = "M03";
				Misc.invoke(()->isotope.setValue("3Ci"));
			}else if(name.equals("0.5ci")==true){
				m_code = "M04";
				Misc.invoke(()->isotope.setValue("0.5Ci"));
			}else if(name.equals("0.05ci")==true){
				m_code = "M05";
				Misc.invoke(()->isotope.setValue("0.05Ci"));
			}
		});
		if(m_code.length()==0){
			return;
		}
		//R_code --> 00000028
		exec("O9000","N0000111",m_code);
		exec("O9005","N10000000");
		radition_flag = true;
		radition_tick = System.currentTimeMillis();
	}
	public void stopRadiation(){
		//R_code --> 00000018
		exec("O9005","N01000000");
		radition_flag = false;
	}
	
	/**
	 * move tools to position form home point.<p>
	 * The position represent a physical number.(value and unit).<p>
	 * Example: "33cm", "12.5mm".<p>
	 * If position is empty, it means 'home/origin point'.
	 * The position is an absolute location.<p> 
	 * @param position - physical number
	 */
	public void moveToAbs(final String position){
		if(position.length()==0){
			exec("O9005","N00100000");//go home
		}else{
			double val = UtilPhysical.convert(position, "mm");
			exec("O9000","N0000111",String.format("G01X%.2f",val));
		}
	}
	public void moveToAbs(
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
	/**
	 * check whether the head of lather is moving.<p>
	 * Don't read this method immediately.<p> 
	 * @return moving or staying
	 */
	public boolean isMoving(){
		//[H] moving --> 00?104
		//[H] stable --> 007004
		char cc = H_code.get().charAt(2);
		if(cc=='?'){
			return true;
		}
		return false;
	}
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
		rad[0].setOnAction(e->dev.isotope.set(rad[0].getText()));
		rad[1].setToggleGroup(grp);
		rad[1].setOnAction(e->dev.isotope.set(rad[1].getText()));
		rad[2].setToggleGroup(grp);
		rad[2].setOnAction(e->dev.isotope.set(rad[2].getText()));
		grp.selectToggle(rad[0]);
		
		JFXButton[] btn = {
			new JFXButton("move"),
			new JFXButton("home"),
			new JFXButton("照射"),
			new JFXButton("關閉"),
			new JFXButton("報告"),
			new JFXButton("靜音"),
		};
		
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setMaxWidth(Double.MAX_VALUE);
		btn[0].setOnAction(e->dev.moveToAbs(box_loca.getText()));
		
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setMaxWidth(Double.MAX_VALUE);
		btn[1].setOnAction(e->dev.moveToAbs(""));
		
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setMaxWidth(Double.MAX_VALUE);
		btn[2].setOnAction(e->dev.makeRadiation());
		
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setMaxWidth(Double.MAX_VALUE);
		btn[3].setOnAction(e->{
			dev.stopRadiation();
		});
		
		btn[4].getStyleClass().add("btn-raised-3");
		btn[4].setMaxWidth(Double.MAX_VALUE);
		btn[4].setOnAction(e->dev.exec("O9000","N00000010000000001"));
		btn[5].getStyleClass().add("btn-raised-3");
		btn[5].setMaxWidth(Double.MAX_VALUE);
		btn[5].setOnAction(e->dev.exec("O9000","N1"));
		
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
		
		VBox lay1 = new VBox(
			new Label(),
			box_loca,
			btn[0],	btn[1],
			rad[0],	rad[1],	rad[2],
			btn[2], btn[3],
			btn[4],	btn[5]
		);
		lay1.getStyleClass().addAll("box-pad","font-console");
				
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","font-console");
		lay0.addRow(0, new Label("H"), code[0]);
		lay0.addRow(1, new Label("O"), code[1]);
		lay0.addRow(2, new Label("R"), code[2]);
		lay0.addRow(3, new Label("U"), code[3]);
		
		return new HBox(lay0,lay1);
	}	
}
