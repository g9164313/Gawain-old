package prj.scada;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.ChartLine;
import narl.itrc.DevBase;
import narl.itrc.DevUSB;
import narl.itrc.Misc;

/**
 * GIE Optoelectronics Inc.
 * Support Mars HS2000+ spectrum meter.<p>
 * Connection is USB, it is still experimental.<p>
 * @author qq
 *
 */
public class DevHS2000 extends DevBase {

	final static String TAG = "HS2000+";
	
	final DevUSB conn = new DevUSB(0x8866,0x8866);
	
	public DevHS2000(){	
		super(TAG);
	}

	protected boolean looper(Work obj) {
		Token tkn = (Token)obj;
		if(tkn.suspend==true){
			return true;
		}
		//tkn.isEvent = get_spectrum();
		//if(tkn.isEvent==false){
			//not sure why the first acquisition is fail?
		//	return true;
		//}
		for(int i=0; i<DATA_SIZE; i++){
			normPix[i] = pixel[i] / pBase;
		}
		tkn.do_loop(normPix);
		return true;
	}

	protected boolean eventReply(Work obj) {
		Token tkn = (Token)obj;
		if(tkn.suspend==true){
			return true;
		}
		tkn.do_event(pixel_tick[0], pixel_tick[2], normPix);
		return true;
	}

	@Override
	protected boolean eventLink() {
		conn.open();
		//get information from device		
		get_information();
		get_calibration();
		//initialize parameters
		set_parameter();
		return true;
	}
	@Override
	protected boolean afterLink() {
		return true;
	}
	@Override
	protected void beforeUnlink() {
	}
	@Override
	protected void eventUnlink() {
	}
	//---------------------------------------//
	
	private final byte[] buf = new byte[4200];
	
	private float[] pixel_tick = {0, 0, 0};//range of value, tick
	
	//private String pixel_unit = "";//unit for tick
	
	private int[] exp_range = {0, 0};//range of exposure
	
	private float pBase = (1<<16);//normalize pixel data	
		
	private int pOffset = 0;//the start index of pixel array
	
	private int pLength = 1;//the valid length of pixel array
		
	private float[] normPix = null;//normalize pixel
	
	private final int DATA_SIZE = 1024;
	
	private void get_information(){
		String info = get_text(0x01);//keep context~~~
		if(info==null){
			Misc.loge("[%s] fail to get information",TAG);
			return;
		}
		info = info.replace('\r', ' ');
		String[] args = info.split("\n");
		//String[] cols = null;
		String txt = null;
		int idx = 0;		
		//1. Range of wavelength
		txt = args[4].trim().substring(11);
		txt = txt.substring(0, txt.length()-2);//strip unit
		idx = txt.indexOf('-');
		pixel_tick[0] = Float.valueOf(txt.substring(0,idx));
		pixel_tick[1] = Float.valueOf(txt.substring(idx+1));
		//2. Resolution - tick
		//cols = UtilPhysical.split_value_unit(args[3].trim().substring(12));
		//pixel_tick[2] = Float.valueOf(cols[0]);
		//pixel_unit = cols[1];
		pixel_tick[2] = (pixel_tick[1] - pixel_tick[0])/DATA_SIZE;
		//3. pixel range base
		pBase = 1<<(Integer.valueOf(args[10].substring(3)));
		//4. Range of exposure
		txt = args[11].trim().substring(4);
		idx = txt.indexOf('-');
		exp_range[0] = Integer.valueOf(txt.substring(0,idx));
		exp_range[1] = Integer.valueOf(txt.substring(idx+1));
		//5. Calculate the index of pixel array
		pOffset = (int)Math.ceil(pixel_tick[0]/pixel_tick[2]);
		pLength = (int)Math.ceil(pixel_tick[1]/pixel_tick[2]);
		pLength = pLength - pOffset + 1;
		
		normPix = new float[DATA_SIZE];
	}
	
	private double[] calValue = {0., 0., 0., 0.};
	//private String calDate = "";
	private void get_calibration(){
		
		String txt = get_text(0x07);
		if(txt==null){
			Misc.loge("[%s] fial to get calibration data", TAG);
			return;
		}
		
		String[] arg = txt.split("\n");
		for(int i=0; i<arg.length; i++){
			if(arg[i].charAt(0)=='C'){
				int j = arg[i].charAt(1) - '0';
				int k = arg[i].indexOf(":");
				if(k<0){					
					Misc.logw("invalid parameter: %s", arg[i]);
					continue;
				}
				calValue[j] = Double.valueOf(arg[i].substring(k+1));
				
			}else if(arg[i].charAt(0)=='D'){
				//calDate = arg[i];
			}			
		}		
	}
		
	private int exposure = 50000;//unit is microsecond
	
	private byte exp_count = 1;
	
	/**
	 * camera trigger method:
	 * 0x11 - internal clock
	 * 0x12 - software trigger
	 * 0x13 - external trigger
	 * 0x14 - ??
	 * 0x15 - ??
	 * 0x22 - exist ??
	 */
	private byte trig_mode = 0x11;
	
	private boolean set_parameter(){
		
		byte[] exp = set_seposure(exposure);
		
		int len = pack_command(
			buf, 0x03,			
			exp[0], exp[1], exp[2],
			exp_count,
			trig_mode
		);
		
		conn.write(0, buf, len);
		
		len = conn.read(0, buf, -1, -1);
		
		if(len==0){
			Misc.loge("[%s] no response", TAG);
			return false;
		}else if(buf[5]==0){
			Misc.loge("[%s] fail to set device", TAG);
			return false;
		}
		return true;
	}
	
	private int[] pixel = null;	

	private boolean get_spectrum(){
		
		int len = pack_command(buf, 0x04);
		
		conn.write(0, buf, len);
		
		len = conn.read(0, buf, -1, 500);
		if(len==0){
			return false;
		}
		
		exposure = get_exposure(buf[5], buf[6], buf[7]);
		exp_count= buf[8];
		trig_mode= buf[9];
		
		int cnt = (len-10-3)/2;
		
		if(pixel==null){			
			pixel = new int[cnt];			
		}else if(pixel.length!=cnt){
			//Could this happen???
			pixel = new int[cnt];
		}
		
		for(int i=0; i<cnt ; i+=2){
			int val = (buf[10+i+1] & 0xFF);
			val = val << 8;
			val = val | (buf[10+i+0] & 0xFF);
			pixel[i/2] = val;
		}
		
		return true;
	}
	/*
	public boolean reset(){
		//no working, it will result in device fail~~~
		int len = pack_command(buf, 0x09);
		conn.write(0, buf, len);
		len = conn.read(0, buf, -1, 100);
		if(buf[5]==0){
			Misc.loge("[%s] fail to reset device");
			return false;
		}		
		return true;
	}*/
	
	private int get_exposure(byte b1, byte b2, byte b3){
		
		int res = b1 & 0xFF;		
		res = res << 8;		
		res = res | (b2 & 0xFF);		
		res = res << 8;
		res = res | (b3 & 0xFF);
		
		boolean us = ((res&0x1)==0)?(false):(true);
		
		res = res >> 1;
		if(us==false){
			res = res * 1000;
		}
		return res;
	}
	
	private byte[] set_seposure(int val){
				
		final byte[] res = {0, 0, 0};
		
		if(val>1000 && val%1000==0){
			//use millisecond as unit
			val = val / 1000;
			val = val << 1;			
		}else{
			//use microsecond as unit
			val = val << 1;
			val = val | 0x01;
		}
		
		res[0] = (byte)((val&0xFF0000)>>16);
		res[1] = (byte)((val&0x00FF00)>>8);
		res[2] = (byte)((val&0x0000FF)>>0);
		return res;
	}
	
	
	private String get_text(int cmd){
		int len = pack_command(buf,cmd);
		conn.write(0, buf, len);
		len = conn.read(0, buf, -1, 200);
		if(len==0){
			return null;
		}
		buf[len-3] = 0;//drop tail character and CRC
		return new String(buf, 5, len-3-5);//only keep data field		
	}
	
	private int pack_command(
		final byte[] buf,
		int cmd,
		int... val
	){
		int len = 8 + val.length;
		buf[0] = (byte)0xAA;//header
		buf[1] = (byte)0x00;//direction, 0->To device, 1->from device
		buf[2] = (byte)((len & 0x00FF)>>0);//package length
		buf[3] = (byte)((len & 0xFF00)>>8);//package length
		buf[4] = (byte)cmd;//command
		for(int i=0; i<val.length; i++){
			buf[5+i] = (byte)val[i];
		}
		int off = 5 + val.length;
		buf[off++]= (byte)0x55;//CRC-1
		buf[off++]= (byte)0xAA;//CRC-2
		buf[off++]= (byte)0x7E;//tail character
		return len;
	}
	//------------------------------------//
	
	private static class Token extends Work {
		ChartLine chart = null;
		ImgSpectrum image = null;
		boolean suspend = false;
		void do_event(
			final float head, 
			final float step, 
			final float[] value
		){
			if(image!=null){
				image.refresh();
			}
			if(chart!=null){
				chart.update(head, step, value).setName("波長(nm)");
			}
		}
		void do_loop(float[] value){
			if(image!=null){
				image.setFreq(value, 0, 1, 600);
			}
		}
		@Override
		public int looper(Work obj, final int pass) {
			return 0;
		}
		@Override
		public int event(Work obj,final int pass) {
			return 0;
		}
	};
	
	public static Node gen_panel(final DevHS2000 dev){
		
		final Token tkn = new Token();
		
		final ChartLine chart = new ChartLine();
		chart.setRangeY(0, 1, true);
		chart.setRangeX(200, 1000);
		
		final ImgSpectrum image = new ImgSpectrum();
		
		final Button btnPlay = new Button("pause");
		GridPane.getVgrow(btnPlay);
		btnPlay.setOnAction(event->{
			tkn.suspend = !tkn.suspend;
			if(tkn.suspend==true){
				btnPlay.setText("play");
			}else{
				btnPlay.setText("pause");
			}
		});
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().add("grid-small-with-boarder");
		lay1.add(btnPlay, 4, 0, 2, 2);
		
		final VBox lay0 = new VBox();
		VBox.setVgrow(chart, Priority.ALWAYS);
		VBox.setVgrow(image, Priority.ALWAYS);
		lay0.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay0.getChildren().addAll(chart, image, lay1);
		
		tkn.chart = chart;
		tkn.image = image;
		tkn.suspend = false;
		dev.offer(200, true, tkn);
				
		return lay0;
	}	
}
