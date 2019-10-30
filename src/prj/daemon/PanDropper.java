package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.CapVidcap;
import narl.itrc.vision.DevCamera;

public class PanDropper extends PanBase {

	private final DevShapeoko oko = new DevShapeoko();
	private final CapVidcap vid = new CapVidcap();
	private final DevCamera cam = new DevCamera(vid);
	
	public PanDropper(Stage primer){
		//some initialization~~~
		super(primer);
		cam.setMinSize(600+23,800+23);
		vid.setFlip('{');
		vid.setupAfter(()->{
			vid.setProperty(3, 800);//CAP_PROP_FRAME_WIDTH
			vid.setProperty(4, 600);//CAP_PROP_FRAME_HEIGHT
			vid.setProperty(10, 0.73);//CAP_PROP_BRIGHTNESS=0.502
			//vid.setProperty(11, 0.1255);//CAP_PROP_CONTRAST=0.1255
			//vid.setProperty(12, 0.1255);//CAP_PROP_SATURATION=0.1255
			//setProperty(13, -1);//CAP_PROP_HUE=-1
			//vid.setProperty(14, 0.5);//CAP_PROP_GAIN=0.2510
			//vid.setProperty(15, 0.09);//CAP_PROP_EXPOSURE=0.0797
			vid.setProperty(44, 0.);//CAP_PROP_AUTO_WB=1
			//setProperty(45, 6148);//CAP_PROP_WB_TEMPERATURE=6148
		});
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		stage().setOnShown(e->{
			chkOko.fire();
			//chkCam.fire();
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
		layA.setExpandedPane(layT[2]);
		
		final BorderPane layB = new BorderPane();
		layB.setCenter(cam);		
		final BorderPane lay0 = new BorderPane();
		//lay0.setLeft(lay1);
		lay0.setCenter(layB);		
		lay0.setRight(layA);
		return lay0;
	}
	
	private native void stub1(DevCamera cam);
	private native void stub2(DevCamera cam, int showState);
	private native void stub3(DevCamera cam);
	
	private boolean stepACC = false;
	private float valNu = 0.0007f;
	private float valModX = 0.183f;
	private float valModY = 0.183f;

	private int moveProber(int dx, int dy) {
		float xx = dx * valModX;
		float yy =-dy * valModY;
		if(dx==0 && dy==0) {
			Misc.logw("no move...");
			return -1;
		}
		if(Math.abs(xx)>100. || Math.abs(xx)>100.) {
			Misc.loge("too long!!!");
			return -2;
		}
		oko.move(xx, yy, 0, false);
		if(stepACC==true) {
			return 0;
		}
		return 1;
	}
	
	private void moveProberAlongPath(int[] waypoint) {
		//ImgPane.Mark mm = cam.getAllMark()[0];
		//for(int i=0; i<waypoint.length; i+=2) {
			//int xx = waypoint[i+0] - mm.locaX;
			//int yy = mm.locaY - waypoint[i+1];
		//}
	}

	private TitledPane panel_process() {
		
		final JFXButton[] btn = {
			new JFXButton("色彩分析"),
			new JFXButton("色彩辨識"),
			new JFXButton("軌跡辨識"),			
			new JFXButton("單步執行"),
			new JFXButton("旅程執行"),
			new JFXButton("自動巡航"),			
			new JFXButton("中止程序"),
		};
		for(JFXButton b:btn) {
			b.setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(b, true);
		}
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->{cam.startProcess(
			()->stub1(cam),
			()->spin.setVisible(true),
			()->spin.setVisible(false)
		);});
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setOnAction(e->{cam.startProcess(
			()->stub2(cam,1),
			()->spin.setVisible(true),
			()->spin.setVisible(false)
		);});
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setOnAction(e->{cam.startProcess(
			()->stub2(cam,2)
		);});
		btn[3].getStyleClass().add("btn-raised-3");
		btn[3].setOnAction(e->{cam.startProcess(()->{
			stepACC = true;
			stub2(cam,0);//單步執行
		});});
		btn[4].getStyleClass().add("btn-raised-3");
		btn[4].setOnAction(e->{cam.startProcess(()->{
			stub3(cam);//旅程執行
		});});
		btn[5].getStyleClass().add("btn-raised-3");
		btn[5].setOnAction(e->{cam.startProcess(()->{
			stepACC = false;
			stub2(cam,0);
		});});
		btn[6].getStyleClass().add("btn-raised-4");
		btn[6].setOnAction(e->{
			cam.stopProcess();
		});
		
		final TextField[] box = new TextField[3];
		for(int i=0; i<box.length; i++) {
			box[i] = new TextField();
			box[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(box[i], true);
		}
		box[0].setText(String.valueOf(valNu));
		box[0].setOnAction(e->{
			valNu = Float.valueOf(box[0].getText());			
		});
		box[1].setText(String.valueOf(valModX));
		box[1].setOnAction(e->{
			valModX = Float.valueOf(box[1].getText());			
		});
		box[2].setText(String.valueOf(valModY));
		box[2].setOnAction(e->{
			valModY = Float.valueOf(box[2].getText());			
		});
		
		final GridPane lay1 = new GridPane();
		lay1.addRow(0, new Label("NU"), box[0]);
		lay1.addRow(1, new Label("ModX"), box[1]);
		lay1.addRow(2, new Label("ModY"), box[2]);
		lay1.getStyleClass().add("ground-pad");
		
		final VBox lay0 = new VBox();
		lay0.getChildren().addAll(btn);
		lay0.getChildren().addAll(lay1);
		lay0.disableProperty().bind(flagCam);
		lay0.getStyleClass().add("box-pad");
		return new TitledPane("校正程序",lay0);		
	}

	private JFXToggleButton chkOko, chkCam;
	private BooleanBinding flagCNC, flagCam, flagALL;
	
	private TitledPane panel_setup() {
		
		chkOko = new JFXToggleButton();
		chkOko.setText("平台");
		chkOko.setStyle("-fx-font-size: 19px;");
		chkOko.setOnAction(e->{
			if(chkOko.isSelected()==true){
				oko.open("/dev/ttyACM0,115200,8n1");
			}else{
				oko.close();
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

		final VBox lay = new VBox(chkOko, chkCam);
		lay.getStyleClass().add("vbox-medium");		
		return new TitledPane("快速設定",lay); 
	}
	
	private TitledPane panel_cnc() {
				
		final Label[] txt = new Label[4];
		for(int i=0; i<4; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
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
			obj.setText("15");
			box[i] = obj;
			GridPane.setFillWidth(obj, true);
		}
		
		final JFXButton[] btn = new JFXButton[6];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton();
			//btn[i].getStyleClass().add("btn-raised-1");
			btn[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(btn[i], true);
		}
		btn[0].setText("⇧");
		btn[0].setOnAction(e->{
			int yy = Math.abs(Misc.txt2int(box[1].getText()));
			oko.move(0, 0, yy, chkMove.isSelected());
		});
		btn[1].setText("⇩");
		btn[1].setOnAction(e->{
			int yy = Math.abs(Misc.txt2int(box[1].getText()));
			oko.move(0, 0, -yy, chkMove.isSelected());
		});
		btn[2].setText("⇦");
		btn[2].setOnAction(e->{
			int xx = Math.abs(Misc.txt2int(box[0].getText()));
			oko.move(-xx, 0, 0, chkMove.isSelected());
		});
		btn[3].setText("⇨");
		btn[3].setOnAction(e->{
			int xx = Math.abs(Misc.txt2int(box[0].getText()));
			oko.move(xx, 0, 0, chkMove.isSelected());
		});
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("ground-pad");
		lay1.add(btn[0], 1, 0, 1, 1);
		lay1.add(btn[2], 0, 1, 1, 1); 
		lay1.add(btn[1], 1, 1, 1, 1); 		
		lay1.add(btn[3], 2, 1, 1, 1);

		btn[4].setText("單點移動");
		btn[4].setOnAction(e->{
			int xx = Misc.txt2int(box[0].getText());
			int yy = Misc.txt2int(box[1].getText());
			oko.move(xx,yy,0,chkMove.isSelected());
		});

		btn[5].setText("歸回原點");
		btn[5].setOnAction(e->oko.exec(
			"$H", 
			"G90", 
			"G0X-170Y-170", 
			"G92X0Y0Z0"
		));

		final GridPane lay = new GridPane();
		lay.addRow(0,new Label("狀態："), txt[0]);
		lay.addRow(1,new Label("X 軸(mm)："), txt[1]);
		lay.addRow(2,new Label("Y 軸(mm)："), txt[2]);
		lay.addRow(3,new Label("Z 軸(mm)："), txt[3]);		
		lay.addRow(4,new Label("X位移/置："), box[0]);
		lay.addRow(5,new Label("Y位移/置："), box[1]);
		lay.add(chkMove, 0, 6, 3, 1);
		lay.add(lay1, 0, 7, 3, 2);
		lay.add(btn[4], 0, 9, 3, 1);
		lay.add(btn[5], 0,10, 3, 1);
		lay.getStyleClass().addAll("ground-pad");
		lay.disableProperty().bind(flagCNC);
		return new TitledPane("Shapeoko",lay); 
	}
}
