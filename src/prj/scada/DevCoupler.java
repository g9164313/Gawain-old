package prj.scada;

import com.jfoenix.controls.JFXCheckBox;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.DevBase;
import narl.itrc.DevModbus;
import narl.itrc.Misc;

public class DevCoupler extends DevBase {

	/**
	 * 4 (A) in --> 1.1 out1
	 * 11 in --> 2.1 out2
	 * 9  switch, com --> 1.2 gnd
	 * 12 safety, com --> 2.2 gnd
	 * 13 (B) in --> 1.4 out3
	 * 7 (A) out DC-on --> 1.1 sin1 
	 * 1 (b) out DC-on --> 2.1 sin2 
	 * 8 DC-on com --> 1.3 gnd
	 * 2 (A) out --> 1.1 ain1
	 * 6 com --> 2.1 gnd
	 * 3 (A) out --> 1.2 ain2
	 * 5 (A) in  --> 1.1 aout1
	 * 14(B) out --> 1.3 ain3
	 * 15(B) out --> 1.4 ain4
	 * 10(B) in  --> 1.2 aout2
	 */
	
	public DevCoupler() {
		super("COUPLER");
	}

	private DevModbus conn = new DevModbus();
	
	public void link(final String ip_addr){		
		offerTask(0,event->{
			conn.open(ip_addr);
		});
		offerAnony(100,true);//period-task for looper!!!
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
	public final BooleanProperty[] p_INT0 = {
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty()
	};
	
	public final BooleanProperty[] p_OUT0 = {
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty()
	};
	public final IntegerProperty[] p_OUT = {
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty()
	};
	
	public static Node gen_panel(final DevCoupler dev){
		
		final JFXCheckBox[] chkIn0 = new JFXCheckBox[4];
		for(int i=0; i<chkIn0.length; i++){
			JFXCheckBox box = new JFXCheckBox();
			box.setText(String.format("IN-%d", i+1));
			box.getStyleClass().add("checkbox-no-dimness");
			box.setDisable(true);
			box.selectedProperty().bind(dev.p_INT0[i]);
			chkIn0[i] = box;
		}
		
		final JFXCheckBox[] chkOut0 = new JFXCheckBox[4];
		for(int i=0; i<chkOut0.length; i++){
			final JFXCheckBox box = new JFXCheckBox();
			box.setText(String.format("OUT-%d", i+1));
			box.setUserData((1<<i));
			box.setOnAction(event->{				
				int msk = (int)box.getUserData();
				int val = ((int)dev._OUT[0])&0xFFFF;
				if(box.isSelected()==true){					
					val = (short)(val | msk);
				}else{
					val = (short)(val & (~msk));
				}
				dev.conn.setValue(8005, val);
			});
			chkOut0[i] = box;
		}
		
		final Label txtInt1 = new Label();
		txtInt1.textProperty().bind(dev.p_INT[1]
			.divide(1000.f)
			.asString("AI-1:  %.3f Volt")
		);
		final Label txtInt2 = new Label();
		txtInt2.textProperty().bind(dev.p_INT[2]
			.divide(1000.f)
			.asString("AI-2:  %.3f Volt")
		);
		final Label txtInt3 = new Label();
		txtInt3.textProperty().bind(dev.p_INT[3]
			.divide(1000.f)
			.asString("AI-3:  %.3f Volt")
		);
		final Label txtInt4 = new Label();
		txtInt4.textProperty().bind(dev.p_INT[4]
			.divide(1000.f)
			.asString("AI-4:  %.3f Volt")
		);
		
		final Label txtOut1 = new Label();
		txtOut1.textProperty().bind(dev.p_OUT[1]
			.divide(1000.f)
			.asString("AO-1 (%.3fV)")
		);
		final Label txtOut2 = new Label();
		txtOut2.textProperty().bind(dev.p_OUT[2]
			.divide(1000.f)
			.asString("AO-2 (%.3fV)")
		);
		final Label txtOut3 = new Label();
		txtOut3.textProperty().bind(dev.p_OUT[3]
			.divide(1000.f)
			.asString("AO-3 (%.3fV")
		);
		final Label txtOut4 = new Label();
		txtOut4.textProperty().bind(dev.p_OUT[4]
			.divide(1000.f)
			.asString("AO-4 (%.3fV)")
		);
		
		final TextField boxOut1 = new TextField("0");
		boxOut1.setPrefWidth(43);
		boxOut1.setOnAction(event->dev.set_analog_out(8006, boxOut1));
		
		final TextField boxOut2 = new TextField("0");
		boxOut2.setPrefWidth(43);
		boxOut2.setOnAction(event->dev.set_analog_out(8007, boxOut2));
		
		final TextField boxOut3 = new TextField("0");
		boxOut3.setPrefWidth(43);
		boxOut3.setOnAction(event->dev.set_analog_out(8008, boxOut3));
		
		final TextField boxOut4 = new TextField("0");
		boxOut4.setPrefWidth(43);
		boxOut4.setOnAction(event->dev.set_analog_out(8009, boxOut4));
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().add("grid-small");
		VBox.setVgrow(lay1, Priority.ALWAYS);
		lay1.add(new Label("--數位輸入--"), 0, 0, 2, 1);
		lay1.addRow(1, chkIn0[0], chkIn0[1]);
		lay1.addRow(2, chkIn0[2], chkIn0[3]);
		lay1.add(new Label("--類比輸入--"), 0, 3, 2, 1);
		lay1.add(txtInt1, 0, 4, 2, 1);
		lay1.add(txtInt2, 0, 5, 2, 1);
		lay1.add(txtInt3, 0, 6, 2, 1);
		lay1.add(txtInt4, 0, 7, 2, 1);
		
		lay1.add(new Label("--數位輸出--"), 0, 8, 2, 1);
		lay1.addRow( 9, chkOut0[0], chkOut0[1]);
		lay1.addRow(10, chkOut0[2], chkOut0[3]);
		lay1.addRow(11, txtOut1, boxOut1);
		lay1.addRow(12, txtOut2, boxOut2);
		lay1.addRow(13, txtOut3, boxOut3);
		lay1.addRow(14, txtOut4, boxOut4);
		
		final VBox lay0 = new VBox();
		lay0.getStyleClass().add("pad-space-small");
		lay0.getChildren().addAll(lay1);
		return lay0;
	}	
	
	private void set_analog_out(int addr, TextField box){
		String txt = box.getText();
		try{
			int val = (int)((Float.valueOf(txt))*1000.f);
			if(val>10000){
				val = 10000;
			}
			conn.setValue(addr, val);
		}catch(NumberFormatException e){
			box.setText("");
		}
	}
	
	@Override
	protected boolean looper(TokenBase obj) {
		if(conn.isValid()==false){
			return false;
		}
		conn.readH(8000, _INT);
		conn.readR(8005, _OUT);
		return true;
	}

	@Override
	protected boolean eventReply(TokenBase obj) {
		
		int val = ((int)_INT[0])&0xFFFF;
		p_INT0[0].set(((val&0x1)!=0)?(true):(false));
		p_INT0[1].set(((val&0x2)!=0)?(true):(false));
		p_INT0[2].set(((val&0x4)!=0)?(true):(false));
		p_INT0[3].set(((val&0x8)!=0)?(true):(false));
		p_INT[0].set(val);
		p_INT[1].set(((int)_INT[1])&0xFFFF);
		p_INT[2].set(((int)_INT[2])&0xFFFF);
		p_INT[3].set(((int)_INT[3])&0xFFFF);
		p_INT[4].set(((int)_INT[4])&0xFFFF);
					
		val = ((int)_OUT[0])&0xFFFF;
		p_OUT0[0].set(((val&0x1)!=0)?(true):(false));
		p_OUT0[1].set(((val&0x2)!=0)?(true):(false));
		p_OUT0[2].set(((val&0x4)!=0)?(true):(false));
		p_OUT0[3].set(((val&0x8)!=0)?(true):(false));
		p_OUT[0].set(val);
		p_OUT[1].set(((int)_OUT[1])&0xFFFF);
		p_OUT[2].set(((int)_OUT[2])&0xFFFF);
		p_OUT[3].set(((int)_OUT[3])&0xFFFF);
		p_OUT[4].set(((int)_OUT[4])&0xFFFF);	
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
