package prj.scada;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import narl.itrc.DevBase;
import narl.itrc.DevModbus;

public class DevCoupler extends DevBase {

	public DevCoupler() {
		super("COUPLER");
	}

	private DevModbus conn = new DevModbus();
	
	public void link(final String ip_addr){		
		offerTask(0,event->{
			conn.open(ip_addr);
		});
		//offerAnony(200,true);//period-task for looper!!!
		super.link();		
	}
	
	private final short[] _INT = new short[5];
	private final short[] _OUT = new short[5];
	
	public final IntegerProperty[] p_INT = {
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty()
	};
							
	public final IntegerProperty[] p_OUT = {
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty()
	};
	
	public static Node gen_panel(final DevCoupler dev){
		
		final Label txt1 = new Label();
		txt1.textProperty().bind(dev.p_OUT[0].asString("OUT：%04X"));
		
		final Button btn1 = new Button("test");
		btn1.setOnAction(event->{			
			dev.conn.setValue(8001, 4);
		});
		
		final GridPane lay1 = new GridPane();
		lay1.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay0.getChildren().addAll(txt1,btn1);
		return lay0;
	}	
	
	@Override
	protected boolean looper(TokenBase obj) {
		if(conn.isValid()==false){
			return false;
		}
		//conn.readH(8000, _INT);
		//conn.readR(8001, _OUT);
		return true;
	}

	@Override
	protected boolean eventReply(TokenBase obj) {
		for(int i=0; i<5; i++){
			p_INT[i].set(((int)_INT[i])&0xFFFF);
			p_OUT[i].set(((int)_OUT[i])&0xFFFF);
		}
		return true;
	}

	@Override
	protected void eventLink() {
	}

	@Override
	protected void eventUnlink() {
		conn.close();
	}
}
