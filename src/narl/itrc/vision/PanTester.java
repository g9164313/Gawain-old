package narl.itrc.vision;

import javafx.scene.Node;
import narl.itrc.PanBase;

public class PanTester extends PanBase {

	public PanTester(){		
	}
	
	
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final CapVidcap vid = new CapVidcap();
		final DevCamera cam = new DevCamera(vid);		
		cam.setMinSize(600+33, 600+33);
		
		stage().setOnShown(e->{			
		});
		
		stage().setOnHidden(e->{
		});
		return cam;
	}

}
