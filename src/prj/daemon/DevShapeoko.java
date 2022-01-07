package prj.daemon;


import java.util.concurrent.atomic.AtomicBoolean;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import narl.itrc.DevTTY;
import narl.itrc.DevTTY2;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import prj.sputter.DevDCG100;

@SuppressWarnings("unused")
public class DevShapeoko extends DevTTY2 {

	public DevShapeoko() {
		TAG = "shapeoko";
	}
	
	@Override
	public void afterOpen() {
		addState(STG_INIT,()->state_init()).
		addState(STG_LOOP,()->state_loop()).
		playFlow(STG_INIT);
	}
	private static final String STG_INIT = "init";
	private static final String STG_LOOP = "loop";

	private void state_init() {
		if(check_grbl()==false) {
			return;
		}
		String txt;
		//exec("~"  ,null);//???	
		exec("$X");//unlock device~~
		exec("M05");//spindle off
		exec("G21");//set units to Millimeters
		nextState(STG_LOOP);
	}

	private boolean is_idle = false;
	
	public final BooleanProperty isIdle = new SimpleBooleanProperty(is_idle);
	
	public final StringProperty lastError = new SimpleStringProperty();
	
	public final StringProperty State= new SimpleStringProperty("???"); 

	public final StringProperty MPosX = new SimpleStringProperty();
	public final StringProperty MPosY = new SimpleStringProperty();
	public final StringProperty MPosZ = new SimpleStringProperty();
	
	public final IntegerProperty Bf1 = new SimpleIntegerProperty();
	public final IntegerProperty Bf2 = new SimpleIntegerProperty();
	
	public final IntegerProperty FS1 = new SimpleIntegerProperty();
	public final IntegerProperty FS2 = new SimpleIntegerProperty();
	
	public final StringProperty WCOX = new SimpleStringProperty();
	public final StringProperty WCOY = new SimpleStringProperty();
	public final StringProperty WCOZ = new SimpleStringProperty();
	
	public final IntegerProperty Ov1= new SimpleIntegerProperty();
	public final IntegerProperty Ov2= new SimpleIntegerProperty();
	
	private void state_loop() {
		sleep(50);
		//status text example:
		//<Idle|MPos:0.000,0.000,0.000|Bf:14,127|FS:0,0|WCO:0.000,0.000,0.000>
		final String txt = exec("?").replaceAll("[\r\n]", "");
		if(txt.matches("^<[\\w]+([|][\\w]+[:][\\-0-9.,]+)+>$")==false) {
			return;
		}
		final String[] col = txt.replaceAll("[<>]", "").split("\\|");
		
		final String state = col[0];
		final String[] mpos= {"","",""}, wco={"", "", ""};
		final int[] bf={0,0}, fs= {0,0}, ov={0, 0};
		
		is_idle = state.toLowerCase().equals("idle");
		
		for(int i=1; i<col.length; i++) {
			String[] val = col[i].split("[:,]");
			if(val[0].startsWith("MPos")==true) {
				mpos[0] = val[1];
				mpos[1] = val[2];
				mpos[2] = val[3];
			}else if(val[0].startsWith("Bf")==true) {
				bf[0] = Integer.valueOf(val[1]);
				bf[1] = Integer.valueOf(val[2]);
			}else if(val[0].startsWith("FS")==true) {
				fs[0] = Integer.valueOf(val[1]);
				fs[1] = Integer.valueOf(val[2]);
			}else if(val[0].startsWith("WCO")==true) {
				wco[0] = val[1];
				wco[1] = val[2];
				wco[2] = val[3];
			}else if(val[0].startsWith("Ov")==true) {
				ov[0] = Integer.valueOf(val[1]);
				ov[1] = Integer.valueOf(val[2]);
			}else {
				Misc.logw("[%s] unknow status: %s", TAG, col[i]);
			}
		}
		Application.invokeLater(()->{
			isIdle.set(is_idle);
			State.set(state);
			MPosX.set(mpos[0]);
			MPosY.set(mpos[1]);
			MPosZ.set(mpos[2]);
			WCOX.set(wco[0]);
			WCOY.set(wco[1]);
			WCOZ.set(wco[2]);
			Bf1.set(bf[0]);
			Bf2.set(bf[1]);
			FS1.set(fs[0]);
			FS2.set(fs[1]);
			Ov1.set(ov[0]);
			Ov2.set(ov[1]);
		});
	}
	private void wait_idle() {
		do{ 
			state_loop(); 
		}while(is_idle==false);	
	}

	private String exec(String cmd) {
		final SerialPort dev = port.get();
		try {
			if(dev.isOpened()==false) {
				return "No opened~~";
			}
			cmd = cmd.replaceAll("[\r|\n]", "");
			dev.writeString(cmd+'\n');
			String msg = "";
			int retry = 0;//if device have no response, just return to looper~~~
			for(;retry<10;){
				final String txt = wait_one_line(dev);
				if(txt.length()==0) {
					retry+=1;
				}else {
					retry =0;
					msg = msg + txt;
				}
				if(msg.contains("ok")==true) {
					int pos = msg.indexOf("ok");
					return msg.substring(0,pos);
				}else if(
					msg.contains("error")==true ||
					msg.contains("ALARM")==true ||
					msg.contains("Hold")==true ||
					msg.contains("Door")==true
				) {	
					return error_code_to_mesg(cmd,msg);
				}
			}
		}catch(SerialPortException e) {
			Misc.loge(e.getMessage());
		}
		return "!!Escape!!";
	}
	private String wait_one_line(final SerialPort dev) throws SerialPortException {
		String txt = "";
		int counter = 0;
		do {
			char cc;
			try {
				cc = (char)dev.readBytes(1,100)[0];
				txt = txt + cc;
				if(cc=='\n') {
					break;
				}
			} catch (SerialPortTimeoutException e) {
				counter+=1;
				if(counter>30) {
					return txt;
				}else {
					continue;
				}
			}			
		}while(true);		
		return txt;
	}
	private String error_code_to_mesg(final String cmd,String txt) {
		if(txt.contains("error:8")==true) {
			txt = "command cannot be used unless Grbl is IDLE.";
		}else if(txt.contains("error:16")==true) {
			txt = "Jog command with no ‘=’ or contains prohibited g-code.";
		}else if(txt.contains("error:22")==true) {
			txt = "Feed rate has not yet been set or is undefined.";
		}
		final String msg = txt;
		Misc.logw("[%s][NG] %s-->%s", TAG, cmd, msg);
		Application.invokeLater(()->lastError.set("["+cmd+"] "+msg));
		return txt;
	}
	
	private boolean check_grbl() {
		//try to empty input buffer after connection~~
		final SerialPort dev = port.get();
		try {
			if(dev.isOpened()==true) {
				String txt = "";
				txt = txt + wait_one_line(dev);
				txt = txt + wait_one_line(dev);
				txt = txt + wait_one_line(dev);
				return txt.contains("Grbl");
			}
		}catch(SerialPortException e) {
			Misc.loge(e.getMessage());
		}
		return false;
	}
	
	public void asyncHome() {asyncBreakIn(()->{
		exec("$H");	
	});}
	public void asyncJogging(
		final char axs,
		final char dir
	) {asyncBreakIn(()->{
		String cmd="";
		int val = 0;
		switch(axs) {
		case 'x':
		case 'X':
			val = (dir=='+')?(840):(-840);
			cmd=String.format("$J=G91 X%d F3000",val); 
			break;
		case 'Y': 
			val = (dir=='+')?(840):(-840);
			cmd=String.format("$J=G91 Y%d F3000",val); 
			break;
		case 'z':
		case 'Z':
			val = (dir=='+')?(80):(-80);
			cmd=String.format("$J=G91 Z%d F3000",val); 
			break;		
		default: cmd="!"; break;
		}
		exec(cmd);
	});}

	public void asyncGridPath(			
		final Runnable handler,
		final int grid_x, 
		final int grid_y,
		final float step_x,
		final float step_y
	) {
		final String val_orig_x = MPosX.get();
		final String val_orig_y = MPosY.get();
		final int val_grid_x = Math.abs(grid_x);
		final int val_grid_y = Math.abs(grid_y);
		final String txt_step_x = String.format("%.3f", step_x);
		final String txt_step_y = String.format("%.3f", step_y);
		asyncBreakIn(()->{		
		for(int gy=0; gy<val_grid_y; gy++) {
			for(int gx=0; gx<val_grid_x; gx++) {				
				if(gy%2==0) {
					if(handler!=null) {
						Application.invokeAndWait(handler);
					}
					exec(String.format("G91 G0X+%s",txt_step_x));
					wait_idle();
				}else {
					exec(String.format("G91 G0X-%s",txt_step_x));
					wait_idle();
					if(handler!=null) {
						Application.invokeAndWait(handler);
					}
				}		
			}
			//end of one row, goto the next line~~~
			exec(String.format("G91 G0Y%s",txt_step_y));
			wait_idle();
		}
		exec(String.format("G90 G0X%sY%s",val_orig_x,val_orig_y));
		wait_idle();
	});}	
	//---------------------------------------//
	
	private static final String unit1 = "mm"; 
	
	public static Node genCtrlPanel(final DevShapeoko dev) {
		
		final Label[] txt = {
			new Label("狀態："), new Label(), new Label(),
			new Label("X 軸："), new Label(), new Label(unit1),
			new Label("Y 軸："), new Label(), new Label(unit1),
			new Label("Z 軸："), new Label(), new Label(unit1),
		};
		for(Label obj:txt) {			
			//obj.getStyleClass().addAll("font-size7","box-border");
			obj.getStyleClass().addAll("font-size7");
			obj.setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(obj, true);
		}
		final Label[] txt_axis = {
			txt[4], txt[7], txt[10]
		};
		for(Label obj:txt_axis) {
			obj.setMinWidth(143);
			obj.setAlignment(Pos.BASELINE_RIGHT);
		}
		txt[ 1].textProperty().bind(dev.State);
		txt[ 4].textProperty().bind(dev.MPosX);
		txt[ 7].textProperty().bind(dev.MPosY);
		txt[10].textProperty().bind(dev.MPosZ);

		final JFXButton btn_setting = new JFXButton();
		btn_setting.setGraphic(Misc.getIconView("settings.png"));
		
		final JFXButton btn_home = new JFXButton();
		btn_home.setGraphic(Misc.getIconView("home.png"));
		btn_home.setOnAction(e->dev.asyncHome());
		
		final JFXButton btn_AXIS_X_LF = new JFXButton();		
		final JFXButton btn_AXIS_X_RH = new JFXButton();
		btn_AXIS_X_LF.setGraphic(Misc.getIconView("dir-left.png"));		
		btn_AXIS_X_RH.setGraphic(Misc.getIconView("dir-right.png"));

		final JFXButton btn_AXIS_Y_LF = new JFXButton();
		final JFXButton btn_AXIS_Y_RH = new JFXButton();
		btn_AXIS_Y_LF.setGraphic(Misc.getIconView("dir-left.png"));
		btn_AXIS_Y_RH.setGraphic(Misc.getIconView("dir-right.png"));
		
		final JFXButton btn_AXIS_Z_LF = new JFXButton();
		final JFXButton btn_AXIS_Z_RH = new JFXButton();
		btn_AXIS_Z_LF.setGraphic(Misc.getIconView("dir-left.png"));
		btn_AXIS_Z_RH.setGraphic(Misc.getIconView("dir-right.png"));
		
		btn_AXIS_X_LF.setOnMousePressed (e->dev.asyncJogging('X','+'));
		btn_AXIS_X_LF.setOnMouseReleased(e->dev.asyncJogging(' ',' '));
		btn_AXIS_X_RH.setOnMousePressed (e->dev.asyncJogging('X','-'));
		btn_AXIS_X_RH.setOnMouseReleased(e->dev.asyncJogging(' ',' '));
		
		btn_AXIS_Y_LF.setOnMousePressed (e->dev.asyncJogging('Y','+'));
		btn_AXIS_Y_LF.setOnMouseReleased(e->dev.asyncJogging(' ',' '));
		btn_AXIS_Y_RH.setOnMousePressed (e->dev.asyncJogging('Y','-'));
		btn_AXIS_Y_RH.setOnMouseReleased(e->dev.asyncJogging(' ',' '));
		
		btn_AXIS_Z_LF.setOnMousePressed (e->dev.asyncJogging('Z','-'));
		btn_AXIS_Z_LF.setOnMouseReleased(e->dev.asyncJogging(' ',' '));
		btn_AXIS_Z_RH.setOnMousePressed (e->dev.asyncJogging('Z','+'));
		btn_AXIS_Z_RH.setOnMouseReleased(e->dev.asyncJogging(' ',' '));
		
		final Label txt_error_msg = new Label();
		txt_error_msg.textProperty().bind(dev.lastError);
		
		final JFXButton btn_path_pane = new JFXButton("scan grid");
		btn_path_pane.getStyleClass().add("btn-raised-1");
		btn_path_pane.setMaxWidth(Double.MAX_VALUE);
		btn_path_pane.setOnAction(e->new DialogGridPath(dev).show());
		GridPane.setFillWidth(btn_path_pane, true);

		final GridPane lay = new GridPane();		
		lay.getStyleClass().addAll("box-pad");
		lay.add(txt[0], 0, 0, 1, 1);
		lay.add(txt[1], 1, 0, 2, 1);
		lay.addRow(0, btn_setting, btn_home);
		lay.addRow(1, txt[3], btn_AXIS_X_LF, txt[ 4], txt[ 5], btn_AXIS_X_RH);
		lay.addRow(2, txt[6], btn_AXIS_Y_LF, txt[ 7], txt[ 8], btn_AXIS_Y_RH);
		lay.addRow(3, txt[9], btn_AXIS_Z_LF, txt[10], txt[11], btn_AXIS_Z_RH);
		lay.add(txt_error_msg, 0, 4, 5, 1);
		lay.add(btn_path_pane, 0, 5, 5, 1);
		return lay;
	}
	
	private static String txt_grid_x = "5";	
	private static String txt_grid_y = "5";
	private static String txt_step_x = "10";
	private static String txt_step_y = "10";

	private static class DialogGridPath extends Dialog<Integer>{
		
		DialogGridPath(final DevShapeoko dev){
			
			final TextField box_grid_x = new TextField(txt_grid_x);			
			final TextField box_grid_y = new TextField(txt_grid_y);
			final TextField box_step_x = new TextField(txt_step_x);
			final TextField box_step_y = new TextField(txt_step_y);

			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad");
			lay.addRow(0, new Label(), new Label("步伐"), new Label("位移（mm）"));
			lay.addRow(1, new Label("X 軸："), box_grid_x, box_step_x);
			lay.addRow(2, new Label("Y 軸："), box_grid_y, box_step_y);

			final DialogPane pan = getDialogPane();			
			pan.getStylesheets().add(Gawain.sheet);
			pan.getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);			
			pan.setContent(lay);
			
			setResultConverter(dia->{
				final ButtonData btn = dia.getButtonData();
				if(btn!=ButtonData.OK_DONE) {
					return -1;				
				}
				txt_grid_x = box_grid_x.getText();
				txt_grid_y = box_grid_y.getText();
				txt_step_x = box_step_x.getText();
				txt_step_y = box_step_y.getText();
				dev.asyncGridPath(
					null,
					Integer.valueOf(txt_grid_x), Integer.valueOf(txt_grid_y), 
					Float.valueOf  (txt_step_x), Float.valueOf  (txt_step_y)
				);
				return 0;
			});
		}
	};	
}
