package prj.daemon;

import javafx.scene.Node;
import javafx.stage.WindowEvent;
import narl.itrc.PanBase;
import narl.itrc.vision.BlkRender;
import narl.itrc.vision.CamVidcap;

public class PanBlkRender extends PanBase {

	public PanBlkRender(){
		//final String testFile = "/home/qq/labor/bang/edge.pgm";
		//rndr = new BlkRender(new CamDummy(testFile));
		//rndr = new BlkRender(new CamVidcap("FILE:0:"+testFile));
		rndr = new BlkRender(new CamVidcap("0"));
		//rndr = new BlkRender(new CamVidcap("VFW:0"));
		//rndr = new BlkRender(new CamMulticam("ral12288-FULL"));
		//rndr = new BlkRender(new CamFlyCapture());
		//rndr = new BlkRender(new CamXIMEA());
	}

	protected BlkRender rndr;
	
	@Override
	protected void eventShown(WindowEvent e){
		rndr.bundle.setup();
	}
	
	@Override
	public Node eventLayout(PanBase pan) {
		return rndr;
	}
}
