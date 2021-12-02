package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import narl.itrc.DevBase;
import narl.itrc.Gawain;
import narl.itrc.Misc;

/**
 * INFICON STM-2 USB Rate and Thickness Monitor.<p>
 * @author qq
 *
 */
public class DevSQM2Usb extends DevBase {
	
	private Optional<SerialPort> port = Optional.empty();

	public DevSQM2Usb() {
		TAG = "SQM2Usb";
	}
	
	public void open(final String name) {
		if(port.isPresent()==true) {
			return;
		}
		try {
			final TTY_NAME tty = new TTY_NAME(name);			
			final SerialPort dev = new SerialPort(tty.path);
			dev.openPort();
			dev.setParams(
				tty.baudrate,
				tty.databit,
				tty.stopbit,
				tty.parity
			);			
			port = Optional.of(dev);
			
			addState(STG_INIT,()->state_initial()).
			addState(STG_MONT,()->state_monitor());
			playFlow(STG_INIT);
			
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void open() {		
		final String prop = Gawain.prop().getProperty(TAG, "");
		if(prop.length()==0) {
			Misc.logw("No default tty path...");
			return;
		}
		this.open(prop);
	}

	@Override
	public void close() {
		if(port.isPresent()==false) {
			return;
		}
		try {
			port.get().closePort();
		} catch (SerialPortException e) {				
			e.printStackTrace();
		}
		port = Optional.empty();
	}

	@Override
	public boolean isLive() {
		if(port.isPresent()==false) {
			return false;
		}
		return port.get().isOpened();
	}	
	//------------------------------------//

	private final String STG_INIT = "init";
	private final String STG_MONT = "monitor";
	
	private String serial = "";
	
	private String[] film_data = {
		"",// name or title
		"",// density
		"",// Z-ratio
		"",// tooling
	};
		
	private static final String UNKNOW_HIGH = "??? Å";
	private static final String UNKNOW_RATE = "??? Å/s";
	private static final String UNKNOW_TIME = "?:??:??";
	
	public final StringProperty  filmTitle = new SimpleStringProperty("");
	
	public final StringProperty  high = new SimpleStringProperty(UNKNOW_HIGH);
	public final StringProperty  rate = new SimpleStringProperty(UNKNOW_RATE);	
	public final IntegerProperty freq = new SimpleIntegerProperty(-1);
	public final IntegerProperty life = new SimpleIntegerProperty(-1);
	public final StringProperty  time = new SimpleStringProperty(UNKNOW_TIME);
		
	private void state_initial() {
		serial = exec("@A");
		if(serial.length()==0) {
			Misc.logw("[%s] fail", TAG);
			stopFlow();
			return;
		}
		film_data[0] = exec("q?");//name
		film_data[1] = exec("E?");//density
		film_data[2] = exec("F?");//Z-ratio
		film_data[3] = exec("J?");//tooling
		exec("r=25");
		//Misc.logv("M >> %s", exec("M"));
		//Misc.logv("r >> %s", exec("r?"));
		
		Application.invokeLater(()->{
			filmTitle.set(film_data[0]);
		});		
		nextState(STG_MONT);
		//nextState("");
	}
	private void state_monitor() {
		//Command 's' : thickness, unit is μg/cm²
		//Command 't' : mass rate, unit is μg/ (*s/cm²)	
		
		//Command 'S' : thickness, unit is Å
		//Command 'T' : mass rate, unit is Å/s	
		//Command 'U' : sensor frequency, unit is Hz
		//Command 'V' : crystal life, proportional(0~100, 5MHz~6MHz)
		//Command 'W' : time, in H:MM:SS format
		int val = 0;
		
		final String t_high = exec("S");
		
		final String t_rate = exec("T");
		
		final String t_freq = exec("U");		
		if(t_freq.length()!=0) {
			try {
				int pos = t_freq.indexOf('.');
				if(pos>=1) {
					val = Integer.valueOf(t_freq.substring(0,pos));
				}else {
					val = Integer.valueOf(t_freq);
				}
			}catch(NumberFormatException e) {
				Misc.loge("[%s] invalid frequence(%s)", TAG, t_freq);
			}
		}else {
			val = 0;
		}
		final int v_freq = val;
		
		final String t_life = exec("V");
		if(t_life.length()!=0) {
			try {
				val = Integer.valueOf(t_life);
			}catch(NumberFormatException e) {
				Misc.loge("[%s] invalid life cycle(%s)", TAG, t_life);
			}
		}else {
			val = 0;
		}
		final int v_life = val;
		
		final String t_time = exec("W");		
		
		Application.invokeLater(()->{			
			if(t_high.length()==0){ 
				high.set(UNKNOW_HIGH); 
			}else{
				if(t_high.startsWith("-")==false) {
					high.set(" "+t_high+" Å"); 
				}else {
					high.set(t_high+" Å"); 
				}
			}
			if(t_rate.length()==0){ 
				rate.set(UNKNOW_RATE); 
			}else{
				if(t_rate.startsWith("-")==false) {
					rate.set(" "+t_rate+" Å/s"); 
				}else {
					rate.set(t_rate+" Å/s"); 
				}				
			}
			if(t_time.length()==0){ 
				time.set(UNKNOW_TIME); 
			}else{ 
				time.set(t_time); 
			}
			freq.set(v_freq);
			life.set(v_life);
		});
		sleep(500);
	}
	
	/**
	 * Execute Sycon Multi-Drop Protocol(SMDP) without stamp.<p>
	 * Package format is:
	 * <STX><ADDR><CMD_RSP>[<DATA>...]<CKSUM1><CKSUM2><CR>
	 * Each field in angle brackets (< >) is a byte, not optional.<p>
	 * Fields in regular brackets ([ ]) are optional.<p>
	 * Ellipses (...) mean one or more of the previous.<p> 
	 * In more detail, see operation manual.
	 * @param cmd
	 * @return
	 */
	public String exec(final int cmd,final String data) {
		if(port.isPresent()==false) {
			return "";
		}
		byte buf0, chkm;
		int indx=data.length();
		String recv = "";
		final SerialPort dev = port.get();
		try {
			final byte[] send = new byte[1+1+1+indx+1+1+1];
			send[0] = (byte) 0x02;//STX
			send[1] = (byte) 0x10;//ADDR, When using USB-to-serial, no multi-drop problem.
			send[2] = (byte)((cmd & 0x0F)<<4);//CMD_RSP bit7~4 for command(master to slave), bit3~1 for response(slave to master)						
			chkm = (byte)(send[1] + send[2]);
			for(int i=0;i<indx;i++) {
				buf0 = (byte)data.charAt(i);				
				chkm+=buf0;
				send[3+i] = buf0;
			}
			send[3+indx] = (byte)(((chkm&0xF0)>>4)+0x30);//checksum-1
			send[4+indx] = (byte)(((chkm&0x0F)>>0)+0x30);//checksum-2
			send[5+indx] = 0x0D;//CR, carry return
			
			dev.writeBytes(send);//send package!!!
						
			byte r_stx = dev.readBytes(1,1000)[0];//STX
			if(r_stx!=0x02) {
				dev.purgePort(SerialPort.PURGE_RXCLEAR);
				Misc.loge("R_STX is fail!!!");
				return "";
			}
			byte r_addr = dev.readBytes(1,1000)[0];//ADDR			
			byte r_cmrs = dev.readBytes(1,1000)[0];//CMD_RSP
			switch(r_cmrs&0x07) {
			case 0: break;//??? no document			
			case 1: break;//command understood and executed
			case 2: Misc.loge("Illegal : %s", data); break;//illegal command
			case 3: Misc.loge("Sytax?? : %s", data); break;//Syntax error
			case 4: Misc.loge("Range?? : %s", data); break;//Data range error.
			case 5: Misc.loge("Inhibite: %s", data); break;//Inhibited
			case 6: Misc.loge("Obsolete: %s", data); break;//obsolete command.no action
			case 7: break;//??? no document
			}
			do{
				buf0 = dev.readBytes(1,1000)[0];//CMD_RSP
				recv = recv + ((char)buf0);				
			}while(buf0!=0x0D);
			
			//trim 3 bytes in tail
			indx = recv.length();
			chkm = (byte)(r_addr + r_cmrs);
			for(int i=0; i<indx-3; i++) {
				chkm+=(byte)(recv.charAt(i));
			}
			
			buf0 = (byte)((recv.charAt(indx-3)-0x30)<<4);			
			buf0 = (byte)(buf0 + (recv.charAt(indx-2)-0x30));
			if(chkm!=buf0) {
				Misc.logw("checksum is invalid!!");
			}
			if(indx<=3) {
				return "";
			}
			recv = recv.substring(0,indx-3);
						
		} catch (SerialPortException e) {
			Misc.loge(e.getMessage());
			return "";
		} catch (SerialPortTimeoutException e) {			
			Misc.loge(e.getMessage());
			return "";
		}
		return recv;
	}

	public String exec(final String command) {
		return exec(8,command);
	}
	
	public void zeros(final boolean zeroHigh, final boolean zeroTime) {asyncBreakIn(()->{
		if(zeroHigh==true && zeroTime==true) {
			exec("B");
		}else if(zeroHigh==true) {
			exec("C");
		}else if(zeroTime==true) {
			exec("D");
		}
	});}
	public void zeroThickness() { zeros(true, false); }
	public void zeroTimer() { zeros(false, true); }
	public void zeros() { zeros(true, true); }
	
	public void setFilmData(final String[] data) {
		filmTitle.set(data[0]);
		asyncBreakIn(()->{
			film_data = data;//reset data~~~
			exec("q="+film_data[0]);//film name
			exec("E="+film_data[1]);//film density
			exec("F="+film_data[2]);//film Z-ratio
			exec("J="+film_data[3]);//film tooling
		});
	}
	//----------------------------------------------------------------------//
	
	private static class DialogFilm extends Dialog<String[]>{
		DialogFilm(final String[] arg){
			final DialogPane pan = getDialogPane();			
			pan.getStylesheets().add(Gawain.sheet);
			pan.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
			
			final Label[] txt = new Label[arg.length];
			txt[0] = new Label("薄膜名稱：");
			txt[1] = new Label("Density：");
			txt[2] = new Label("Z-ratio：");
			txt[3] = new Label("Tooling：");
			
			final TextField[] box = new TextField[arg.length];
			for(int i=0; i<arg.length; i++) {
				TextField obj = new TextField(arg[i]);
				obj.setPrefColumnCount(13);				
				box[i] = obj;
			}
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad");
			for(int i=0; i<arg.length; i++) {
				lay.addRow(i, txt[i], box[i]);
			}
			pan.setContent(lay);
			
			setResultConverter(dia->{
				final ButtonData btn = (dia==null)?(null):(dia.getButtonData());
				if(btn!=ButtonData.OK_DONE) {
					return null;				
				}
				String[] result = new String[box.length];
				for(int i=0; i<result.length; i++) {
					result[i] = box[i].getText().trim();
				}
				//First field is film name, it is special
				//The name must be 8 char string
				String name = result[0].replaceAll("\\s", "_");
				while(name.length()<8) {
					name = name.concat("_");
				}
				result[0] = name.substring(0,8);
				return result;
			});
		}		
	};
		
	public static Pane genCtrlPanel(final DevSQM2Usb dev) {
		
		final Label[] info = new Label[8];
		for(int i=0; i<info.length; i++) {
			info[i] = new Label();
			info[i].setMinWidth(80);
			info[i].getStyleClass().addAll("font-size5");
			GridPane.setHgrow(info[i], Priority.ALWAYS);
		}
		info[0].setText("速率："); info[1].setPrefWidth(200.); info[1].textProperty().bind(dev.rate); 
		info[2].setText("厚度："); info[3].setPrefWidth(140.); info[3].textProperty().bind(dev.high); 
		info[4].setText("時間："); info[5].setPrefWidth(140.); info[5].textProperty().bind(dev.time); 
		info[6].setText("薄膜："); info[7].setPrefWidth(140.); info[7].textProperty().bind(dev.filmTitle);
		
		final Button btn_zero_high = new Button("歸零");
		btn_zero_high.setFocusTraversable(false);
		btn_zero_high.setOnAction(e->dev.zeroThickness());
		
		final Button btn_zero_time = new Button("歸零");
		btn_zero_time.setFocusTraversable(false);
		btn_zero_time.setOnAction(e->dev.zeroTimer());
		
		final Button btn_film_pick = new Button("選取");
		btn_film_pick.setFocusTraversable(false);
		//btn_film_pick.setOnAction(e->{});
		
		final JFXButton btn_film = new JFXButton("薄膜設定");
		btn_film.getStyleClass().add("btn-raised-1");
		btn_film.setMaxWidth(Double.MAX_VALUE);
		btn_film.setOnAction(e->{
			final DialogFilm dia = new DialogFilm(dev.film_data);
			final Optional<String[]> opt = dia.showAndWait();
			if(opt.isPresent()==true) {
				dev.setFilmData(opt.get());
			}
		});
		
		final JFXButton btn_zero = new JFXButton("歸零鍍膜");
		btn_zero.getStyleClass().add("btn-raised-1");
		btn_zero.setMaxWidth(Double.MAX_VALUE);
		btn_zero.setOnAction(e->dev.zeros());
		
		final GridPane lay = new GridPane(); 
		lay.getStyleClass().addAll("box-pad");
		lay.add(info[0], 0, 0);
		lay.add(info[1], 1, 0, 2, 1);
		lay.addRow(1, info[2], info[3]);
		lay.addRow(2, info[4], info[5]);
		lay.addRow(3, info[6], info[7]);
		lay.add(btn_zero_high, 2, 1, 1, 1);
		lay.add(btn_zero_time, 2, 2, 1, 1);
		lay.add(btn_film_pick, 2, 3, 1, 1);
		lay.add(btn_film, 0, 4, 3, 1);
		lay.add(btn_zero, 0, 5, 3, 1);
		return lay;
	}
}
