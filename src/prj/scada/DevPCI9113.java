package prj.scada;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.ComboExtra;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * ADLink PCI-9113 Analog Input Card.<p>
 * Support 32 channel<p> 
 * 12-bit resolution <p>
 * 100k sample per second <p>
 * 1k FIFO Buffer <p>
 * Measure Bipolar Volt including: ±10V, ±5V, ±1V, ±0.5V, ±0.1V, ±0.05V <p>
 * Measure Unipolar Volt including: 0~10V, 0~5V, 0~1V, 0~0.1V <p>
 * 
 * @author qq
 *
 */
public class DevPCI9113 extends DevDask {

	private int bus_num = 0;
	
	/**
	 * mapping all channels value
	 */
	private static class Token extends Work {
		
		int   chann = 0;
		int[] range = new int[32];		
				
		int[]    toll = new int[32];
		//double[] volt = new double[32];		
		Label[]  stamp= new Label[32];
		
		Token(){
			for(int i=0; i<range.length; i++){
				range[i] = AD_B_10_V;
				stamp[i] = null;
			}
		}
		
		Token observe(int ch, int rng){
			chann = chann | (1<<ch);
			range[ch] = rng;
			return this;
		}
		//Token observe(int ch, Label txt){
		//	chann = chann | (1<<ch);
		//	stamp[ch] = txt;
		//	return this;
		//}		
		Token observe(int ch, int rng, Label txt){
			chann = chann | (1<<ch);
			range[ch] = rng;
			stamp[ch] = txt;
			return this;
		}
		Token blind(int ch){
			chann = chann ^ (1<<ch);
			stamp[ch] = null;
			return this;
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
	
	public DevPCI9113(int id) {
		super("Dev-PCI9113");
		bus_num = id;
	}

	protected boolean looper(Work obj) {
		Token tkn = (Token)obj;
		for(int ch=0; ch<32; ch++){
			if( ((1<<ch)&tkn.chann)==0 ){
				continue;
			}
			tkn.toll[ch] = syncReadValue(ch,tkn.range[ch]);
			//tkn.volt[ch] = syncReadVolt (ch,tkn.range[ch]);
		}
		return true;
	}

	protected boolean eventReply(Work obj) {
		Token tkn = (Token)obj;
		for(int ch=0; ch<32; ch++){
			if( ((1<<ch)&tkn.chann)==0 ){
				continue;
			}
			if(tkn.stamp[ch]==null){
				continue;
			}
			tkn.stamp[ch].setText(String.format("%d", tkn.toll[ch]));
		}
		return true;
	}
	
	/**
	 * Measure signal value from DAQ card.
	 * @param channel - signal pin, or probe, maximum index is 31. 
	 * @param range - voltage level, 
	 * @return
	 */	
	public int syncReadValue(int channel, int range){
		int[] val = { 0 };
		int res = AIReadChannel(card, channel, range, val);
		if(res!=NoError){
			Misc.loge("[%s] syncRead=%d", TAG, res);
		}
		return (val[0]&0xFFF);
	}
	
	/**
	 * Measure 'Voltage' value from DAQ card.
	 * @param channel - signal pin, or probe, maximum index is 31.
	 * @param range - voltage level
	 * @return
	 */
	public double syncReadVolt(int channel, int range){
		double[] val = { 0 };
		int res = AIVReadChannel(card, channel, range, val);
		if(res!=NoError){
			Misc.loge("[%s] syncReadVolt=%d", TAG, res);
		}
		return val[0];
	}
	
	public short[] syncSample(int channel, int range, int count, double rate){		
		short[] buffer = new short[count];
		int res = AIContReadChannel(
			card,
			channel,
			range,
			buffer,
			count,
			rate,
			SYNCH_OP
		);
		if(res!=NoError){
			Misc.logv("syncSample=%d", res);
		}
		return buffer;
	}
	
	@Override
	protected boolean eventLink() {
		card = RegisterCard(PCI_9113, bus_num);
		if(card<0){
			Misc.loge("Fail to open %s", TAG);
			return false;
		}
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
		if(card<0){
			return;
		}
		ReleaseCard(card);
	}
	//--------below method are convenient for GUI event--------//
	
	private static Object[] range_combo = {
		"±10V"  , AD_B_10_V, 
		"±5V"   , AD_B_5_V,
		"±1V"   , AD_B_1_V, 
		"±0.5V" , AD_B_0_5_V,
		"±0.1V" , AD_B_0_1_V,		 
		"±0.05V", AD_B_0_05_V, 
		"0~10V" , AD_U_10_V, 
		"0~1V"  , AD_U_1_V,
		"0~0.1V", AD_U_0_1_V
	};
	
	private static void create_channel_node(
		final VBox root, 
		final int channel,
		final int range,
		final Token token
	){	
		HBox lay = new HBox();
		
		Label txt1 =new Label(String.format("CH%2d:", channel));
		txt1.setMaxWidth(80);
		
		Label txt2 =new Label("--------");
		txt2.setMaxWidth(130);
		
		ComboExtra<Integer> cmb = new ComboExtra<Integer>(range_combo); 
		cmb.setMaxWidth(130);
		cmb.select(range);
		cmb.setOnAction(event->{
			token.observe(channel, cmb.getSelected().intValue());
		});
		
		Button btn = new Button("X");
		btn.setMaxWidth(30);
		btn.setOnAction(event->{
			token.blind(channel);
			root.getChildren().remove(lay);
		});
		
		lay.getChildren().addAll(txt1, txt2, cmb, btn);
		root.getChildren().add(lay);
		
		token.observe(channel, range, txt2);
	}
	
	public static Node gen_panel(final DevPCI9113 dev){
		
		final Token tkn = new Token();
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-spacing: 7;");
		
		final Button btnLook = PanBase.genButton2("新增項目",null);
		btnLook.setMaxWidth(Double.MAX_VALUE);
		btnLook.setOnAction(event->{
			
			final ComboBox<Integer> cmb1 = new ComboBox<Integer>();
			for(int i=0; i<32; i++){
				cmb1.getItems().add(i);
			}
			cmb1.getSelectionModel().select(0);
			cmb1.setMaxWidth(130);
			
			final ComboExtra<Integer> cmb2 = new ComboExtra<Integer>(range_combo);
			cmb2.setMaxWidth(130);
			
			final GridPane lay1 = new GridPane();
			lay1.setStyle("-fx-hgap: 7; -fx-vgap: 7;");
			lay1.addRow(0, new Label("來源"), cmb1);			
			lay1.addRow(1, new Label("範圍"), cmb2);
			
			Alert alt = new Alert(AlertType.CONFIRMATION);
			alt.setTitle("");
			alt.setHeaderText("監視哪一個訊號源?");
			alt.getDialogPane().setExpandableContent(lay1);
			alt.getDialogPane().setExpanded(true);
			
			alt.showAndWait().ifPresent(result->{
				if(result==ButtonType.CANCEL){
					return;
				}
				create_channel_node(lay0, 
					cmb1.getSelectionModel().getSelectedIndex(),
					cmb2.getSelected().intValue(),
					tkn
				);
			});
		});
				
		final String TXT_START= "開始擷取";
		final String TXT_STOP = "停止擷取";
		final Button btnPlay = PanBase.genButton2(TXT_START,null);
		btnPlay.setMaxWidth(Double.MAX_VALUE);
		btnPlay.setOnAction(event->{			
			if(btnPlay.getText().equals(TXT_START)==true){
				btnPlay.setText(TXT_STOP);				
				dev.offer(200,true,tkn);
			}else{
				btnPlay.setText(TXT_START);
			}
		});
		
		final VBox root = new VBox();
		root.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		root.getChildren().addAll(
			lay0,
			new Separator(),
			btnLook,
			btnPlay
		);
		return root;
	}
}
