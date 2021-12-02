package prj.daemon;

import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import narl.itrc.PanBase;
import prj.sputter.DevSQM2Usb;

public class TestUnit4 extends PanBase {
	
	final DevSQM2Usb dev = new DevSQM2Usb();
	
	public TestUnit4(final Stage stg) {
		dev.open("/dev/ttyUSB0:115200,8n1");
	}

	@Override
	public Pane eventLayout(PanBase self) {		
		return DevSQM2Usb.genCtrlPanel(dev);
	}
}
