package prj.daemon;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import narl.itrc.PanBase;


public class TestUnit2 extends PanBase {

	public TestUnit2(final Stage stg) {
		super(stg);
	}
	
	private MeshView loadMeshView() {
		float[] points = { 
			-5, 5, 0, 
			-5, -5, 0, 
			5, 5, 0, 
			5, -5, 0 
		};
		float[] texCoords = { 
			1, 1, 
			1, 0, 
			0, 1, 
			0, 0
		};
		int[] faces = { 
			2, 2, 1, 1, 0, 0, 
			2, 2, 3, 3, 1, 1 
		};

		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().setAll(points);
		mesh.getTexCoords().setAll(texCoords);
		mesh.getFaces().setAll(faces);

		PhongMaterial mm = new PhongMaterial();

		MeshView vv =  new MeshView(mesh);
		vv.setTranslateX(150.);
		vv.setTranslateY(150.);
		vv.setTranslateZ(0.);
		vv.setScaleX(5.);
		vv.setScaleY(5.);
		vv.setScaleZ(5.);
		vv.setMaterial(mm);
		return vv;
	}

	@Override
	public Pane eventLayout(PanBase self) {
		
		/*MeshView obj = loadMeshView();
		
		RotateTransition rotate = new RotateTransition(Duration.millis(500), obj);
	    rotate.setAxis(Rotate.Y_AXIS);
	    rotate.setFromAngle(0);
	    rotate.setToAngle(360);
	    rotate.setInterpolator(Interpolator.LINEAR);
	    rotate.setCycleCount(RotateTransition.INDEFINITE);
	    rotate.play();
		
		SubScene scene3d = new SubScene(new Group(obj), 300., 300.);
	    scene3d.setFill(Color.rgb(200, 200, 200));
	    scene3d.setCamera(new PerspectiveCamera());
	    
		return new StackPane(scene3d);*/
		
		return new View3DObject();
	}
}
