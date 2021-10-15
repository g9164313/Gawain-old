package prj.daemon;

import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import narl.itrc.Misc;


public class View3DObject extends StackPane {

	final PerspectiveCamera cam;
	final SubScene ss;
	final Group root = new Group();

	static final int DEF_V_WIDTH = 600;
	static final int DEF_V_HIGHT = 600;
	
	public View3DObject() {
		this(DEF_V_WIDTH,DEF_V_HIGHT);
	}

	public View3DObject(final int width, final int height) {
		
		//create_axis();
		//create_test();
		//create_triangle();
		final float[] vals = {
			 0f, 0f,  0f,  0f,
			 0f, 0f, 0f,  0f,
			 0f, 0f, 0f,  0f,
			 0f, 0f,  0f,  0f,
		};
		create_grid_mesh(4,4, vals);
		
		cam = new PerspectiveCamera();
		cam.setTranslateX(-DEF_V_WIDTH/2);
		cam.setTranslateY(-DEF_V_HIGHT/2);
		cam.setTranslateZ(0.);

		ss = new SubScene(root, width, height);
		ss.setFill(Color.rgb(230, 230, 230));
		ss.setCamera(cam);
		 
		final Point3D rr = new Point3D(-1f,1f,0f);
		
		// cam.translateXProperty();
		ss.setOnKeyPressed(event -> {
			final KeyCode kc = event.getCode();
			if (kc == KeyCode.W) {
				
				final Shape3D ss = (Shape3D) root.getChildren().get(0);
				//cam.setTranslateY(cam.getTranslateY() - 10.);
				
			} else if (kc == KeyCode.S) {
				
				final Shape3D ss = (Shape3D) root.getChildren().get(0);
				//cam.setTranslateY(cam.getTranslateY() + 10.);
				
			} else if (kc == KeyCode.A) {
				
				final Shape3D ss = (Shape3D) root.getChildren().get(0);
				ss.setRotationAxis(Rotate.X_AXIS);
				ss.setRotate(ss.getRotate()+5f);
				//cam.setTranslateX(cam.getTranslateX() - 10.);
				
			} else if (kc == KeyCode.D) {
				
				final Shape3D ss = (Shape3D) root.getChildren().get(0);
				ss.setRotationAxis(Rotate.X_AXIS);
				ss.setRotate(ss.getRotate()-5f);
				//cam.setTranslateX(cam.getTranslateX() + 10.);
				
			} else if (kc == KeyCode.UP) {
				cam.setTranslateZ(cam.getTranslateZ() + 10.);
			} else if (kc == KeyCode.DOWN) {
				cam.setTranslateZ(cam.getTranslateZ() - 10.);
			} else if (kc == KeyCode.LEFT) {
				cam.setRotate(cam.getRotate()-1.);				
			}else if (kc == KeyCode.RIGHT) {
				cam.setRotate(cam.getRotate()+1.);
			}
		});
		ss.setFocusTraversable(true);

		final Button btn = new Button("ggyy");
		btn.setOnAction(e -> {
			ss.requestFocus();
		});

		final GridPane lay = create_ctl();
		StackPane.setAlignment(lay, Pos.BOTTOM_RIGHT);

		getChildren().addAll(ss, lay);
	}

	private GridPane create_ctl() {

		final Label[] txt = { 
			new Label("tranX"), 
			new Label(), 
			new Label("tranY"), 
			new Label(), 
			new Label("tranZ"),
			new Label(),
		};
		txt[1].textProperty().bind(cam.translateXProperty().asString("%.1f"));
		txt[3].textProperty().bind(cam.translateYProperty().asString("%.1f"));
		txt[5].textProperty().bind(cam.translateZProperty().asString("%.1f"));

		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad", "box-border", "background-red");
		lay.addRow(0, txt[0], txt[1]);
		lay.addRow(1, txt[2], txt[3]);
		lay.addRow(2, txt[4], txt[5]);
		return lay;
	}
	
	private void create_grid_mesh(
		int g_width,
		int g_height,
		final float... vals
	) {
		if(g_width <2) { g_width=2; }
		if(g_height<2) { g_height=2; }
		
		final int cnt = g_width * g_height;
		
		final float[] points = new float[cnt * 3];
		
		for(int j=0; j<g_height; j++) {
			for(int i=0; i<g_width; i++) {
				final int off = j*g_width*3 + i*3;
				points[off + 0] = i*10f;//x
				points[off + 2] = j*10f;//z			
				int k = j*g_width + i;
				if(k<vals.length) {
					points[off + 1] = vals[k];//y
				}
			}
		}
		
		final float[] texCoords = new float[cnt * 2];
		
		for(int j=0; j<g_height; j++) {
			for(int i=0; i<g_width; i++) {
				final int off = j*g_width*2 + i*2;
				texCoords[off + 0] = i * (1f/(float)(g_width -1));//texture map in u direction
				texCoords[off + 1] = i * (1f/(float)(g_height-1));//texture map in v direction
			}
		}
		
		final int[] faces = new int[(g_width-1)*(g_height-1)*6*2];
		
		for(int j=0; j<g_height-1; j++) {
			for(int i=0; i<g_width-1; i++) {
				
				//square has two triangle and four vertext.				
				//the vertex order is according to the grid order
				
				int v1 = j*g_width + i;
				int v2 = v1 + 1;
				int v3 = (j+1)*g_width + i;
				int v4 = v3 + 1;
				
				final int off = (j*(g_width-1) + i)*6*2;
				faces[off + 0] = v2;
				faces[off + 1] = v2;
				faces[off + 2] = v1;
				faces[off + 3] = v1;
				faces[off + 4] = v3;
				faces[off + 5] = v3;
				
				faces[off + 6] = v2;
				faces[off + 7] = v2;
				faces[off + 8] = v3;
				faces[off + 9] = v3;
				faces[off +10] = v4;
				faces[off +11] = v4;
			}
		}
		
		final TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().setAll(points);
		mesh.getTexCoords().setAll(texCoords);
		mesh.getFaces().setAll(faces);
		
		final PhongMaterial material = new PhongMaterial();		
		material.setSpecularColor(Color.BLACK);
		material.setDiffuseColor(Color.GREEN);
		
		final MeshView mm = new MeshView(mesh);
		mm.setCullFace(CullFace.NONE);
		mm.setDrawMode(DrawMode.FILL);
		mm.setMaterial(material);
		
		mm.setRotationAxis(Rotate.X_AXIS);
		mm.setRotate(-73);
		
		root.getChildren().addAll(mm);
	}	
	
	private void create_axis() {

		final PhongMaterial redMaterial = new PhongMaterial();
		redMaterial.setDiffuseColor(Color.DARKRED);
		redMaterial.setSpecularColor(Color.RED);

		final Cylinder xAxis = new Cylinder(5., 5.);
		xAxis.setMaterial(redMaterial);
		root.getChildren().addAll(xAxis);
	}
	
	private void create_test() {
		final PhongMaterial mm1 = new PhongMaterial();		
		mm1.setSpecularColor(Color.WHITE);
		mm1.setDiffuseColor(Color.RED);
		
		final PhongMaterial mm2 = new PhongMaterial();		
		mm2.setSpecularColor(Color.CHOCOLATE);
		mm2.setDiffuseColor(Color.BLUEVIOLET);
		
        final Box oo1 = new Box(100,100,100);
        oo1.setMaterial(mm1);
        //oo1.setCullFace(CullFace.NONE);
        oo1.setTranslateX(0);
        oo1.setTranslateY(0);
        oo1.setTranslateZ(0);        
        oo1.setRotationAxis(Rotate.X_AXIS);
        
		final Cylinder oo2 = new Cylinder(5.,100.);
		oo2.setMaterial(mm1);
		oo2.setTranslateX(0.);
		oo2.setTranslateX(0.);
		oo2.setTranslateX(0.);
		oo2.setRotationAxis(Rotate.Z_AXIS);
		
		final Sphere oo3 = new Sphere(100.);
		oo3.setMaterial(mm1);
		oo3.setTranslateX(0.);
		oo3.setTranslateY(0.);
		oo3.setTranslateZ(0.);
		
		root.getChildren().addAll(oo1);
	}
	
	private void create_triangle() {
		
		final PhongMaterial mm2 = new PhongMaterial();		
		mm2.setSpecularColor(Color.BLACK);
		mm2.setDiffuseColor(Color.GREEN);
		
		final float ss = 50f;
		float[] points = {
			 0,  0, 0,
			ss,  0, 0, 
			ss, ss, 0, 
			 0, ss, 0,
		};
		float[] texCoords = { 
			0, 0,
			0, 1, 
			1, 0,
			1, 1,
		};
		int[] faces = {
			2, 2, 1, 1, 0, 0,
			2, 2, 0, 0, 3, 3
		};
		//one face is compound as:
		//clockwise is faced to camera~~~
		//Vertex-X, Texture-X, Vertex-Y, Texture-Y, Vertex-Z, Texture-Z
		
		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().setAll(points);
		mesh.getTexCoords().setAll(texCoords);
		mesh.getFaces().setAll(faces);
		
		MeshView mm = new MeshView(mesh);
		mm.setDrawMode(DrawMode.FILL);
		mm.setMaterial(mm2);		
		mm.setRotationAxis(new Point3D(-1f,1f,0f));
		mm.setRotate(45.);
		
		root.getChildren().addAll(mm);
	}
}
