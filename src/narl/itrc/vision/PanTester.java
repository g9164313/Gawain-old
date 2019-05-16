package narl.itrc.vision;

import javafx.scene.Node;
import narl.itrc.PanBase;

public class PanTester extends PanBase {

	public PanTester(){		
	}
	
	
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final ImgView img = new ImgView();
		img.setMinSize(640+23, 480+23);
		
		final DevCamera cam = new DevCamera();
		final CapVidcap vid = new CapVidcap();
		
		stage().setOnShown(e->{			
			cam.livePlay(1);
		});
		stage().setOnHidden(e->{
			cam.unlink();
		});
		return img;
	}

}
