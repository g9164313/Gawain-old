package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.CapVidcap;
import narl.itrc.vision.DevCamera;
import narl.itrc.vision.ImgFilm;

public class PanDropper extends PanBase {

	private final DevShapeoko oko = new DevShapeoko();
	private final CapVidcap vid = new CapVidcap(800,600);
	private final DevCamera cam = new DevCamera(vid);

	@Override
	public Node eventLayout(PanBase self) {
		cam.setMinSize(600+23,800+23);
		
		stage().setOnShown(e->{
			chkOko.fire();
			chkCam.fire();
			cam.unflattenMark(Gawain.prop().getProperty("VIEW_MARK",""));
		});
		stage().setOnHidden(e->{
			Gawain.prop().setProperty("VIEW_MARK", cam.flattenMark());
		});
		
		final TitledPane[] layT = {
			panel_setup(),
			panel_process(),
			panel_cnc()
		};
		final Accordion layA = new Accordion(layT);
		layA.setExpandedPane(layT[0]);
		
		final BorderPane layB = new BorderPane();
		layB.setCenter(cam);		
		final BorderPane lay0 = new BorderPane();
		//lay0.setLeft(lay1);
		lay0.setCenter(layB);		
		lay0.setRight(layA);
		return lay0;
	}
	
	private native void stub1(DevCamera cam);
	private native void stub2(DevCamera cam,boolean showMask);
	private native void stub3(DevCamera cam);
	
	private void moveProber(int dx, int dy) {
		Misc.logv("vec=%3d,%3d", dx, dy);
	}
	
	private TitledPane panel_process() {
		final JFXButton[] btn = {
			new JFXButton("色彩分析"),
			new JFXButton("色彩辨識"),
			new JFXButton("自動巡航"),
			new JFXButton("中止程序"),
		};
		for(JFXButton b:btn) {
			b.setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].getStyleClass().add("btn-raised-2");
		btn[0].setOnAction(e->{cam.startProcess(
			()->stub1(cam),
			()->spin.setVisible(true),
			()->spin.setVisible(false)
		);});
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setOnAction(e->{cam.startProcess(
			()->stub2(cam,true),
			()->spin.setVisible(true),
			()->spin.setVisible(false)
		);});
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setOnAction(e->{cam.startProcess(
			()->stub2(cam,false)
		);});
		btn[3].getStyleClass().add("btn-raised-3");
		btn[3].setOnAction(e->{
			cam.stopProcess();
		});
		
		final VBox lay = new VBox(btn);
		lay.disableProperty().bind(flagCam);
		lay.getStyleClass().add("vbox-medium");
		final TitledPane gg =  new TitledPane("校正程序",lay);
		gg.setPrefHeight(70.);
		return gg;		
	}
	
	private JFXToggleButton chkOko, chkCam;
	private BooleanBinding flagCNC, flagCam, flagALL;
	
	private TitledPane panel_setup() {
		
		chkOko = new JFXToggleButton();
		chkOko.setText("平台");
		chkOko.setStyle("-fx-font-size: 19px;");
		chkOko.setOnAction(e->{
			if(chkOko.isSelected()==true){
				oko.link("/dev/ttyACM0,115200,8n1");
			}else{
				oko.unlink();
			}
		});
		
		chkCam = new JFXToggleButton();
		chkCam.setText("相機");
		chkCam.setStyle("-fx-font-size: 19px;");
		chkCam.setOnAction(e->{
			if(chkCam.isSelected()==true){
				cam.play(1);
			}else{
				cam.pause();
			}
		});
		
		flagCNC = chkOko.selectedProperty().not();
		flagCam = chkCam.selectedProperty().not();
		flagALL = flagCNC.or(flagCam);

		final VBox lay = new VBox(
			chkOko, chkCam
		);
		lay.getStyleClass().add("vbox-medium");		
		return new TitledPane("總設定",lay); 
	}
	
	private TitledPane panel_cnc() {
				
		final Label[] txt = new Label[4];
		for(int i=0; i<4; i++) {
			Label obj = new Label();
			obj.setMaxWidth(Double.MAX_VALUE);			
			txt[i] = obj;
			GridPane.setFillWidth(txt[i], true);
		}		
		txt[0].textProperty().bind(oko.State);
		txt[1].textProperty().bind(oko.MPosX.asString("%.2f"));
		txt[2].textProperty().bind(oko.MPosY.asString("%.2f"));
		txt[3].textProperty().bind(oko.MPosZ.asString("%.2f"));
		
		final JFXCheckBox chkMove = new JFXCheckBox("相對移動");
		chkMove.setOnAction(e->{
			if(chkMove.isSelected()==true){
				chkMove.setText("絕對移動");
			}else{
				chkMove.setText("相對位置");
			}
		});
		GridPane.setFillWidth(chkMove, true);
		
		final TextField[] box = new TextField[2];
		for(int i=0; i<box.length; i++) {
			TextField obj = new TextField();
			obj.setMaxWidth(Double.MAX_VALUE);			
			obj.setText("30");
			box[i] = obj;
			GridPane.setFillWidth(obj, true);
		}
		
		final JFXButton btnMove = new JFXButton("單步移動");
		btnMove.getStyleClass().add("btn-raised-1");
		btnMove.setMaxWidth(Double.MAX_VALUE);
		btnMove.setOnAction(e->{
			int xx = Misc.txt2int(box[0].getText());
			int yy = Misc.txt2int(box[1].getText());
			oko.syncMove(xx, yy, chkMove.isSelected());
		});
		GridPane.setFillWidth(btnMove, true);
		
		final JFXButton btnHome = new JFXButton("歸回原點");		
		btnHome.getStyleClass().add("btn-raised-1");
		btnHome.setMaxWidth(Double.MAX_VALUE);
		btnHome.setOnAction(e->{
			if(oko.isIdle()==false){
				return;
			}
			oko.exec("$H");
			oko.exec("G90");
			oko.exec("G0X-170Y-170");
			oko.exec("G92X0Y0Z0");
		});
		GridPane.setFillWidth(btnHome, true);
		
		final GridPane lay = new GridPane();
		lay.addRow(0, new Label("狀態："), txt[0]);
		lay.addRow(1, new Label("X 軸(mm)："), txt[1]);
		lay.addRow(2, new Label("Y 軸(mm)："), txt[2]);
		lay.addRow(3, new Label("Z 軸(mm)："), txt[3]);
		lay.addRow(4, new Label("X："), box[0]);
		lay.addRow(5, new Label("Y："), box[1]);
		lay.add(chkMove, 0, 6, 4, 1);
		lay.add(btnMove, 0, 7, 4, 1);
		lay.add(btnHome, 0, 8, 4, 1);
		lay.getStyleClass().addAll("ground-pad");
		lay.disableProperty().bind(flagCNC);
		return new TitledPane("Shapeoko",lay); 
	}
}
