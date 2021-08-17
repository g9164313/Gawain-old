package prj.daemon;

import java.util.Timer;
import java.util.TimerTask;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import narl.itrc.Misc;

public class View3DObject extends StackPane {
	
	private final PerspectiveCamera cam;
	private final SubScene ss;
	
	private void create_axis(final Group grp) {
		
		final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);
        
        final Cylinder xAxis = new Cylinder(100., 10.);
        xAxis.setMaterial(redMaterial);
		grp.getChildren().addAll(xAxis);
	}
	

	
	public View3DObject(final int width,final int height) {
		
		Group grp = new Group();
		
		create_axis(grp);
		
		cam = new PerspectiveCamera();
		//cam.setTranslateX(width/2);
		//cam.setTranslateY(height/2);
		
		ss = new SubScene(grp, width, height);
		ss.setFill(Color.rgb(200, 200, 200));
		ss.setCamera(cam);
		
		//cam.translateXProperty();
		ss.setOnKeyPressed(event->{
			final KeyCode kc = event.getCode();
			if(kc==KeyCode.W || kc==KeyCode.UP || kc==KeyCode.KP_UP) {
				cam.setTranslateZ(cam.getTranslateZ()+10.);
			}else if(kc==KeyCode.S || kc==KeyCode.DOWN || kc==KeyCode.KP_DOWN ) {
				cam.setTranslateZ(cam.getTranslateZ()-10.);
			}else if(kc==KeyCode.A || kc==KeyCode.LEFT || kc==KeyCode.KP_LEFT ) {
				cam.setTranslateX(cam.getTranslateX()-10.);
			}else if(kc==KeyCode.D || kc==KeyCode.RIGHT || kc==KeyCode.KP_RIGHT ) {
				cam.setTranslateX(cam.getTranslateX()+10.);
			}else if(kc==KeyCode.Q ) {
			}else if(kc==KeyCode.E ) {
			}
		});
		
		Button btn = new Button("ggyy");
		btn.setOnAction(e->{
			ss.requestFocus();
		});
		
		GridPane lay = create_ctl();
		StackPane.setAlignment(lay,Pos.BOTTOM_RIGHT);
		
		getChildren().addAll(ss,lay);
	}
	public View3DObject() {
		this(600,600);
	}
	
	private GridPane create_ctl() {
		
		final Label[] txt = {
			new Label("tranX"), new Label(),
			new Label("tranY"), new Label(), 	
			new Label("tranZ"), new Label(),
		};
		txt[1].textProperty().bind(cam.translateXProperty().asString("%.1f"));
		txt[3].textProperty().bind(cam.translateYProperty().asString("%.1f"));
		txt[5].textProperty().bind(cam.translateZProperty().asString("%.1f"));
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad","box-border","background-red");
		lay.addRow(0, txt[0], txt[1]);
		lay.addRow(1, txt[2], txt[3]);
		lay.addRow(2, txt[4], txt[5]);
		return lay;
	}
	
}
