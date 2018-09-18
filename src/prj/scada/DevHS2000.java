package prj.scada;

import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
	
	//private int PIX_MAX = 1024 * 64 - 1;
	//private int PIX_MIN = 0;
	//private final static int SAMPLE = 2048;
	//private final static int M_WIDTH = 600;
	//private final static int M_HEIGHT= 600;
	
	LineChart<Number,Number> chart = null;
	
	ImgSpectrum image = null;
	//WritableImage img = new WritableImage(SAMPLE,M_HEIGHT);
	//GraphicsContext monitor= null;
	
	@Override
	protected boolean looper(TokenBase token) {
		get_spectrum();
		
		//rolling image~~~~
		/*PixelReader rd = img.getPixelReader();
		PixelWriter wt = img.getPixelWriter();
		for(int yy=M_HEIGHT-2; yy>=0; --yy){			
			wt.setPixels(
				0 , yy+1, 
				SAMPLE, 1, 
				rd, 
				0 , yy
			);
		}
		
		//draw first line in image~~~		
		for(int xx=0; xx<SAMPLE; xx++){
			int val = pixel[xx];
			//update peak value for auto-range
			if(val>pix_max){
				pix_max = val;
			}
			if(val<pix_min){
				pix_min = val;
			}
			int idx = (val*255)/PIX_MAX;
			wt.setArgb(xx, 0, ColorHue.mapOct[idx]);
		}*/		
		return true;
	}

	@Override
	protected boolean eventReply(TokenBase token) {
		if(chart!=null){
			chart.getData().clear();
			final XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
			for(int i=0; i<1024; i++){
				series.getData().add(
					new XYChart.Data<Number,Number>(i,pixel[i])
				);
			}
			chart.getData().add(series);
		}

		/*monitor.drawImage(img, 
			0, 0, 
			M_WIDTH, M_HEIGHT
		);*/
		return true;
	}

	@Override
	protected void eventLink() {
		conn.open();
		//get all information from device		
		get_information();
		get_calibration();
		set_parameter();
		offer(new TokenBase(), 200, true);
	}

	@Override
	protected void eventUnlink() {
		clearAll();
	}
	//---------------------------------------//
	
	private final byte[] buf = new byte[4200];
	
	private void get_information(){
		String info = get_text(0x01);//keep context~~~
		if(info==null){
			Misc.loge("[%s] fail to get information",TAG);
			return;
		}
		String[] args = info.split("\n");
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

	private void get_spectrum(){
		
		int len = pack_command(buf, 0x04);
		
		conn.write(0, buf, len);
		
		len = conn.read(0, buf, -1, -1);
		
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
	}
	
	public boolean reset(){
		
		int len = pack_command(buf, 0x09);
		
		conn.write(0, buf, len);
		
		len = conn.read(0, buf, -1, 100);
		
		if(buf[5]==0){
			Misc.loge("[%s] fail to reset device");
			return false;
		}		
		return true;
	}
	
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
	
	public static Node gen_panel(final DevHS2000 dev){
		
		final LineChart<Number, Number> chart = new LineChart<Number, Number>(
			new NumberAxis(), 
			new NumberAxis()
		);		
		chart.setAnimated(false);
		chart.setCreateSymbols(false);
		dev.chart = chart;

		final TextField txtTick = new TextField("500");
		txtTick.setOnAction(event->{
			NumberAxis axs = (NumberAxis)chart.getYAxis();
			try{
				axs.setUpperBound(Double.valueOf(txtTick.getText()));
			}catch(NumberFormatException e){
				txtTick.setText(String.format("%d", (int)axs.getTickUnit()));
			}
		});
		
		final TextField txtUpper = new TextField("3500");
		txtUpper.setPrefWidth(100);
		txtUpper.setOnAction(event->{
			NumberAxis axs = (NumberAxis)chart.getYAxis();
			try{
				axs.setUpperBound(Double.valueOf(txtUpper.getText()));
			}catch(NumberFormatException e){
				txtUpper.setText(String.format("%d", (int)axs.getUpperBound()));
			}
		});
		final TextField txtLower = new TextField("500");
		txtLower.setPrefWidth(200);
		txtLower.setOnAction(event->{
			NumberAxis axs = (NumberAxis)chart.getYAxis();
			try{
				axs.setLowerBound(Double.valueOf(txtLower.getText()));
			}catch(NumberFormatException e){
				txtLower.setText(String.format("%d", (int)axs.getLowerBound()));
			}
		});
		final CheckBox chkRange = new CheckBox("自動邊界");	
		chkRange.setOnAction(event->{
			NumberAxis axs = (NumberAxis)chart.getYAxis();
			if(chkRange.isSelected()==true){
				axs.setAutoRanging(true);
				txtUpper.setDisable(true);
				txtLower.setDisable(true);
				txtTick.setDisable(true);
			}else{
				axs.setAutoRanging(false);
				txtUpper.setDisable(false);
				txtLower.setDisable(false);
				txtTick.setDisable(false);
				try{					
					axs.setUpperBound(Double.valueOf(txtUpper.getText()));
					axs.setLowerBound(Double.valueOf(txtLower.getText()));
					axs.setTickUnit(Double.valueOf(txtTick.getText()));
				}catch(NumberFormatException e){
					txtUpper.setText(String.format("%d", (int)axs.getUpperBound()));
					txtLower.setText(String.format("%d", (int)axs.getLowerBound()));
					txtTick.setText(String.format("%d", (int)axs.getTickUnit()));
				}
			}
		});
		chkRange.setSelected(true);
		
		//final Canvas monitor= new Canvas(M_WIDTH, M_HEIGHT);		
		//dev.monitor= monitor.getGraphicsContext2D();

		final ImgSpectrum image = new ImgSpectrum();
		dev.image = image;
		VBox.setVgrow(image, Priority.ALWAYS);
		
		final GridPane lay1 = new GridPane();
		lay1.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		lay1.add(chkRange         , 0, 0, 2, 1);
		lay1.add(new Label("上限"), 2, 0);
		lay1.add(txtUpper         , 3, 0);
		
		lay1.add(new Label("Tick"), 0, 1);
		lay1.add(txtTick          , 1, 1);
		lay1.add(new Label("下限"), 2, 1);
		lay1.add(txtLower         , 3, 1);
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay0.getChildren().addAll(chart, image, lay1);
		return lay0;
	}	
}
