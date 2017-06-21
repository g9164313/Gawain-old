package prj.refuge;

import java.math.BigDecimal;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import narl.itrc.DevModbus;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

/**
 * 讀取川得科技公司代理的溫濕度紀錄器數值，這裡是用 TCP/IP 透過 Modbus 界面達成 
 * @author qq
 *
 */
public class DevCDR06 extends DevModbus {
	
	private String[] title = null;	
	private short[]  fixpt = null;
	private short[]  value = null;
	
	//private double[] value = null;
	private StringProperty[] propValue = null;
	
	public DevCDR06(){
		watcher.setCycleCount(Timeline.INDEFINITE);
		root.getStyleClass().add("grid-small");
	}
	
	public DevCDR06(String attr){
		this();
		connect(attr);
	}
	
	public void connect(String attr){
		title = null;
		if(attr.length()==0){
			attr = Gawain.prop.getProperty("CDR06","TCP,127.0.0.1;CH0;CH1;CH3");
		}
		//format: [IPv4 address];[CH0 title];[CH1 title];...
		String[] argv = attr.split(";");
		if(argv.length>=1){
			open(argv[0]);
		}
		if(argv.length>=2){
			int cnt = argv.length-1;
			title = new String[cnt];
			fixpt = new short[cnt];
			value = new short[cnt];
			propValue = new SimpleStringProperty[cnt];
			for(int i=0; i<cnt; i++){
				title[i] = argv[1+i];
				propValue[i] = new SimpleStringProperty();
			}
			readR(0x300002,fixpt);//version, ??, decimal point, decimal point, ...
			eventWatcher.handle(null);
		}else{
			//reset all variable!!!
			title = null;
			fixpt = null;
			value = null;
			propValue = null;
		}
	}
	//--------------------------------//
	
	private EventHandler<ActionEvent> eventWatcher = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			if(title==null){
				return;
			}
			readH(0x300000,value);
			for(int i=0; i<title.length; i++){
				BigDecimal val = new BigDecimal(value[i] - 19999);
				val = val.movePointLeft(fixpt[i]);
				propValue[i].set(val.toString());
			}
		}
	};
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(500),
		eventWatcher
	));
	
	public void update_auto(boolean flag){
		if(title==null){
			return;
		}
		if(flag==true){
			watcher.play();
		}else{
			watcher.pause();
		}
	}

	private GridPane root = new GridPane();
	
	public void layout_grid(){
		if(title==null){
			return;
		}
		
		final String _sty = 
			"-fx-font-size: 20px;"+
			"-fx-label-padding: 7,0,0,0;";
		
		for(int i=0; i<title.length; i++){
			
			Label txtTitle = new Label(title[i]);
			txtTitle.setStyle(_sty);
			
			Label txtValue = new Label();
			txtValue.setStyle(_sty);
			txtValue.textProperty().bind(propValue[i]);
			
			root.add(txtTitle, 0, i);
			root.add(txtValue, 1, i);
		}		
	} 
	
	@Override
	protected Node eventLayout(PanBase pan) {
		layout_grid();
		return root;
	}
}
