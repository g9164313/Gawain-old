package prj.daemon;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.BoxLogger;
import narl.itrc.ButtonTask;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.ImgPreview;

public class PanPuppeteer extends PanBase {

	public PanPuppeteer(){		
	}
	
	private ImgPreview prv = new ImgPreview();
	
	private void sendInputEvent(String addr,String param){
		try {
			URL url = new URL("http://"+addr+":9911/input?"+param);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(50);
			con.getInputStream().close();
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			Misc.loge("錯誤的URL --> "+addr+" ("+e.getMessage());
		} catch (IOException e) {
			//e.printStackTrace();
			Misc.loge("傳送失敗："+e.getMessage());
		}
	}
	
	private void takeScreenResult(String addr){
		
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final BoxLogger boxMesg = new BoxLogger();
		boxMesg.setPrefHeight(100);
		
		final JFXTextField boxAddress = new JFXTextField("172.16.2.145");
		boxAddress.setPrefWidth(100);

		final JFXTextField boxKeyPress = new JFXTextField("test!!");
		boxKeyPress.setOnAction(event->{
			sendInputEvent(
				boxAddress.getText(), 
				"keyboard="+boxKeyPress.getText()
			);
			boxKeyPress.setText("");
		});
		
		//final JFXCheckBox chkMonitor = new JFXCheckBox("監看畫面");
		//chkMonitor.setMaxWidth(Double.MAX_VALUE);
		//GridPane.setHgrow(chkMonitor, Priority.ALWAYS);
		//GridPane.setHalignment(chkMonitor, HPos.LEFT);
		
		//final Button btnTest1 = PanBase.genButton2("test",null);
		//btnTest1.setMaxWidth(Double.MAX_VALUE);
		//GridPane.setHgrow(btnTest1, Priority.ALWAYS);//???

		final ButtonTask btnMonitor = new ButtonTask("Monitor");
		btnMonitor.setAction(true,event->{
			ButtonTask.Action act = (ButtonTask.Action)event.getTarget();
			try {
				while(act.isCancelled()==false){
					takeScreenResult(boxAddress.getText());

					Misc.logv("~~~monitor~~~");
					TimeUnit.MILLISECONDS.sleep(1000);					
				}
			} catch (Exception e) {
				//e.printStackTrace();
				Misc.loge("內部錯誤！！"+e.getMessage());
			}
			//sendInputEvent(boxAddress.getText());
		});
		btnMonitor.prefWidthProperty().bind(boxMesg.prefHeightProperty().subtract(7));
		btnMonitor.prefHeightProperty().bind(boxMesg.prefHeightProperty().subtract(7));
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().add("grid-medium");
		lay1.addRow(0, new Label("主機位置："), boxAddress);
		lay1.addRow(1, new Label("keyboard："), boxKeyPress);
		
		final HBox lay2 = new HBox();
		lay2.getStyleClass().add("hbox-small");
		lay2.getChildren().addAll(boxMesg,btnMonitor);
		lay2.setAlignment(Pos.CENTER);
		HBox.setHgrow(lay1, Priority.NEVER);
		HBox.setHgrow(boxMesg, Priority.ALWAYS);

		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setCenter(prv);
		lay0.setRight(lay1);
		lay0.setBottom(lay2);
		return lay0;
	}
}
