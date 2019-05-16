package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.CapVidcap;
import narl.itrc.vision.DevCamera;
import narl.itrc.vision.ImgView;

public class PanDropper extends PanBase {

	private final DevShapeoko oko = new DevShapeoko();
	private final CapVidcap vid = new CapVidcap(800,600);
	private final DevCamera cam = new DevCamera(vid);
	
	@Override
	public Node eventLayout(PanBase self) {
		cam.getView(0).setMinSize(800+23, 600+23);
		stage().setOnHidden(e->{
			String txt = cam.getView(0).getMarkByFlat();
			Gawain.getSetting().setProperty("VIEW_MARK", txt);
		});
		return new BorderPane(
			cam.getView(0),
			null, null, null, pan_setting()
		);
	}
	
	private VBox pan_setting(){
		
		final JFXCheckBox chkOko = new JFXCheckBox("CNC");
		chkOko.setOnAction(e->{
			if(chkOko.isSelected()==true){
				oko.link("/dev/ttyACM0,115200,8n1");
			}else{
				oko.unlink();
			}
		});
		
		final JFXCheckBox chkCam = new JFXCheckBox("相機");
		chkCam.setOnAction(e->{
			if(chkCam.isSelected()==true){
				cam.link();
				cam.livePlay(1);
			}else{
				cam.unlink();
			}
		});

		final BooleanBinding flag1 = chkOko.selectedProperty().not();
		final BooleanBinding flag2 = chkCam.selectedProperty().not();

		final VBox lay0 = new VBox(
			chkOko, chkCam,
			lay_procesure(flag1, flag2),
			lay_cnc_inform(flag1),
			lay_cnc_jogging(flag1)			
		);
		lay0.getStyleClass().add("vbox-medium");
		
		stage().setOnShown(e->{
			//chkOko.fire(true);
			chkCam.fire();
			String txt = Gawain.getSetting().getProperty("VIEW_MARK","");
			cam.getView(0).setMarkByFlat(txt);
		});
		return lay0;
	}
	
	private Pane lay_procesure(
		BooleanBinding flag1, 
		BooleanBinding flag2
	){
		final JFXButton btnHome = new JFXButton("歸位");		
		btnHome.getStyleClass().add("btn-raised-1");
		btnHome.setMaxWidth(Double.MAX_VALUE);
		btnHome.disableProperty().bind(flag1);
		btnHome.setOnAction(e->{
			oko.exec("$H\n");
		});
		
		final JFXButton btnTest = new JFXButton("Test1");		
		btnTest.getStyleClass().add("btn-raised-1");
		btnTest.setMaxWidth(Double.MAX_VALUE);
		btnTest.disableProperty().bind(flag2);
		cam.bindPipe(btnTest);
		
		final JFXButton btnMont = new JFXButton("Test2");		
		btnMont.getStyleClass().add("btn-raised-1");
		btnMont.setMaxWidth(Double.MAX_VALUE);
		btnMont.disableProperty().bind(flag2);
		cam.bindMonitor(btnMont);
		
		VBox lay = new VBox();
		lay.getStyleClass().add("vbox-one-dir");
		lay.getChildren().addAll(
			btnHome,
			btnTest,
			btnMont
		);
		return lay;
	}
	
	private Pane lay_cnc_inform(BooleanBinding flag){
		
		final Label txtStat = new Label();
		txtStat.textProperty().bind(oko.State);
		final Label txtPosX = new Label();
		txtPosX.textProperty().bind(oko.MPosX.asString("%.2f"));
		final Label txtPosY = new Label();
		txtPosY.textProperty().bind(oko.MPosY.asString("%.2f"));
		final Label txtPosZ = new Label();
		txtPosZ.textProperty().bind(oko.MPosZ.asString("%.2f"));
		
		final GridPane lay = new GridPane();
		lay.disableProperty().bind(flag);
		lay.addRow(0, new Label("狀態："), txtStat);
		lay.addRow(1, new Label("X 軸(mm)："), txtPosX);
		lay.addRow(2, new Label("Y 軸(mm)："), txtPosY);
		lay.addRow(3, new Label("Z 軸(mm)："), txtPosZ);
		return lay;
	}
	
	private Pane lay_cnc_jogging(BooleanBinding flag){
		
		jog_watch.setCycleCount(Timeline.INDEFINITE);
		
		final Button btnFw = gen_bounce_button("chevron-up.png",DIR_Y_INC);//forward		
		final Button btnBk = gen_bounce_button("chevron-down.png",DIR_Y_DEC);//backward		
		final Button btnRh = gen_bounce_button("chevron-right.png",DIR_X_INC);		
		final Button btnLf = gen_bounce_button("chevron-left.png",DIR_X_DEC);
		final Button btnUp = gen_bounce_button("arrow-up.png",DIR_Z_INC);//Z-axis up		
		final Button btnDw = gen_bounce_button("arrow-down.png",DIR_Z_DEC);//Z-axis down
		
		final GridPane lay = new GridPane();
		lay.disableProperty().bind(flag);
		lay.addRow(0, btnUp, btnFw, btnDw);
		lay.addRow(1, btnLf, btnBk, btnRh);
		return lay;
	}
	
	private final char DIR_Y_INC = 'w';
	private final char DIR_Y_DEC = 's';
	private final char DIR_X_INC = 'd';
	private final char DIR_X_DEC = 'a';
	private final char DIR_Z_INC = 'q';
	private final char DIR_Z_DEC = 'e';
	private char jog_dir_c = 0;
	
	private Timeline jog_watch = new Timeline(new KeyFrame(
		Duration.millis(110), event->{
			final String param = "1.5F800\n";
			switch(jog_dir_c){
			case DIR_Y_INC: oko.exec("$J=G91Y+"+param); break;
			case DIR_Y_DEC: oko.exec("$J=G91Y-"+param); break;
			case DIR_X_INC: oko.exec("$J=G91X+"+param); break;
			case DIR_X_DEC: oko.exec("$J=G91X-"+param); break;
			case DIR_Z_INC: oko.exec("$J=G91Z+"+param); break;
			case DIR_Z_DEC: oko.exec("$J=G91Z-"+param); break;
			}
		}
	));
	
	private Button gen_bounce_button(
		final String icon_name,
		final char dir
	){
		final JFXButton btn = new JFXButton();
		btn.setGraphic(Misc.getIconView(icon_name));
		btn.setOnMousePressed(e->{
			if(e.getButton()==MouseButton.PRIMARY){
				jog_dir_c = dir;
				jog_watch.play();
			}
		});		
		btn.setOnMouseReleased(e->{
			if(e.getButton()==MouseButton.PRIMARY){
				jog_dir_c = 0;
				oko.exec("G4P0\n");
				jog_watch.stop();
			}
		});
		return btn;
	}
}
