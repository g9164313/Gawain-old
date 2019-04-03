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
		
		stage().setOnShown(e->{
			cam.setCapture(new CapVidcap());
			cam.setFace(img);
			cam.link();
			cam.play();
		});
		stage().setOnHidden(e->{
			cam.unlink();
		});
		return img;
	}

}
