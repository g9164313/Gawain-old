package prj.daemon;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import prj.sputter.DevCESAR;
import prj.sputter.DevSQM2Usb;

public class TestUnit4 extends PanBase {
	
	//final DevSQM2Usb dev = new DevSQM2Usb();
	//final DevTDS3000 dev = new DevTDS3000();
	//final DevCESAR dev = new DevCESAR();
	final DevShapeoko dev = new DevShapeoko();
	
	public TestUnit4(final Stage stg) {
		super(stg);
		stg.setOnShown(e->{
			//dev.open("/dev/ttyUSB0,115200,8n1");
			//dev.open("/dev/ttyUSB0,38400,8n1");
			dev.open("/dev/ttyACM0,115200,8n1");
		});
	}

	@Override
	public Node eventLayout(PanBase self) {		
		//return DevSQM2Usb.genCtrlPanel(dev);
		//return DevTDS3000.genCtrlPanel(dev);
		//return DevTDS3000.genScreenView("10.10.0.30");
		//return DevCESAR.genCtrlPanel(dev);
		return DevShapeoko.genCtrlPanel(dev);
	}
}
