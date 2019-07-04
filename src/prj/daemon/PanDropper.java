package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
	
	private void process_1(){
		final int sp = 20;
		check_point(  0,  0, "../bang/pos0.png");		
		check_point( sp,  0, "../bang/pos1.png");
		check_point(-sp,  0, "../bang/pos2.png");
		check_point(  0, sp, "../bang/pos3.png");
		check_point(  0,-sp, "../bang/pos4.png");
		check_point( sp, sp, "../bang/pos5.png");
		check_point( sp,-sp, "../bang/pos6.png");
		check_point(-sp, sp, "../bang/pos7.png");
		check_point(-sp,-sp, "../bang/pos8.png");
		oko.moveAbs(0, 0);
	}
	
	private void check_point(
		final int xx, 
		final int yy, 
		final String name
	){
		oko.moveAbs(xx, yy);
		Misc.delay(1000);
		cam.getView(0).save(name);
		Misc.delay(1000);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		cam.getView(0).setMinSize(800+23, 600+23);
		//cam.getPane(0).setOnMouseClicked(e->{	
		//});
		stage().setOnHidden(e->{
			String txt = cam.getView(0).getMarkByFlat();
			Gawain.getSetting().setProperty("VIEW_MARK", txt);
		});
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(cam.getView(0));
		final BorderPane lay2 = new BorderPane();
		lay2.setCenter(lay0);
		lay2.setLeft(pan_setting());
		return lay2;
	}
	
	private VBox pan_setting(){
		
		final JFXToggleButton chkOko = new JFXToggleButton();
		chkOko.setText("移動平台");
		chkOko.setStyle("-fx-font-size: 1.8em;");
		chkOko.setOnAction(e->{
			if(chkOko.isSelected()==true){
				oko.link("/dev/ttyACM0,115200,8n1");
			}else{
				oko.unlink();
			}
		});
		
		final JFXToggleButton chkCam = new JFXToggleButton();
		chkCam.setText("相機");
		chkCam.setStyle("-fx-font-size: 1.8em;");
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
			lay_cnc_jogging(flag1),
			lay_cnc_anchor(flag1)
		);
		lay0.getStyleClass().add("vbox-medium");
		
		stage().setOnShown(e->{
			//chkOko.fire();
			//chkCam.fire();
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
			if(oko.isIdle()==false){
				return;
			}
			oko.exec("$H");
			oko.exec("G90");
			oko.exec("G0X-170Y-170");
			oko.exec("G92X0Y0Z0");
		});
		
		final JFXButton btnProc1 = new JFXButton("校正");		
		btnProc1.getStyleClass().add("btn-raised-1");
		btnProc1.setMaxWidth(Double.MAX_VALUE);
		btnProc1.disableProperty().bind(flag1.or(flag2));
		btnProc1.setOnAction(e->doDuty(()->process_1()));

		final JFXButton btnProc2 = new JFXButton("Test2");		
		btnProc2.getStyleClass().add("btn-raised-1");
		btnProc2.setMaxWidth(Double.MAX_VALUE);
		btnProc1.disableProperty().bind(flag1.or(flag2));
		//cam.bindMonitor(btnMont);
		
		VBox lay = new VBox();
		lay.getStyleClass().add("vbox-one-dir");
		lay.getChildren().addAll(
			btnHome,
			btnProc1,
			btnProc2
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
		lay.getStyleClass().add("layout-medius");
		lay.disableProperty().bind(flag);
		lay.addRow(0, new Label("狀態："), txtStat);
		lay.addRow(1, new Label("X 軸(mm)："), txtPosX);
		lay.addRow(2, new Label("Y 軸(mm)："), txtPosY);
		lay.addRow(3, new Label("Z 軸(mm)："), txtPosZ);
		return lay;
	}
	
	private Pane lay_cnc_anchor(BooleanBinding flag){
		
		final JFXCheckBox chk = new JFXCheckBox("相對位置");
		chk.setOnAction(e->{
			if(chk.isSelected()==true){
				chk.setText("絕對位置");
			}else{
				chk.setText("相對位置");
			}
		});
		final TextField[] box = {
			new TextField(),
			new TextField(),
		};
		box[0].setMaxWidth(73);
		box[1].setMaxWidth(73);
		box[0].setText("0");
		box[1].setText("0");
		
		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().add("layout-medius");
		lay2.add(chk, 0, 0, 2, 1);
		lay2.addRow(1, new Label("X："), box[0]);
		lay2.addRow(2, new Label("Y："), box[1]);
		
		final JFXButton btn1 = new JFXButton("單步");
		btn1.getStyleClass().add("btn-raised-1");
		btn1.setMaxWidth(Double.MAX_VALUE);
		btn1.setOnAction(e->{
			int xx = Misc.txt2int(box[0].getText());
			int yy = Misc.txt2int(box[1].getText());
			oko.move_atom(xx, yy, chk.isSelected());
		});
		
		final JFXButton btn2 = new JFXButton("零點");
		btn2.getStyleClass().add("btn-raised-1");
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setOnAction(e->oko.exec("G92X0Y0Z0"));
		
		final VBox lay = new VBox();
		lay.disableProperty().bind(flag);
		lay.getStyleClass().add("vbox-one-dir");
		lay.getChildren().addAll(lay2, btn1, btn2);
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
		Duration.millis(10), event->{
			final String param = "2F600\n";
			switch(jog_dir_c){
			case DIR_Y_INC: oko.exec_atom("$J=G91Y+"+param); break;
			case DIR_Y_DEC: oko.exec_atom("$J=G91Y-"+param); break;
			case DIR_X_INC: oko.exec_atom("$J=G91X+"+param); break;
			case DIR_X_DEC: oko.exec_atom("$J=G91X-"+param); break;
			case DIR_Z_INC: oko.exec_atom("$J=G91Z+"+param); break;
			case DIR_Z_DEC: oko.exec_atom("$J=G91Z-"+param); break;
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
				jog_watch.stop();
				oko.exec("G4P0\n");				
			}
		});
		return btn;
	}
}
