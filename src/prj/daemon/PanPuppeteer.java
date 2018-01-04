package prj.daemon;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.BoxLogger;
import narl.itrc.ButtonTask;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.ImgPreview;
import narl.itrc.vision.WidPreview;

public class PanPuppeteer extends PanBase {

	public PanPuppeteer(){		
	}

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
	
	private void takeOutputEvent(String addr,ImgPreview prv){
		try {
			URL url = new URL("http://"+addr+":9911/output");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(50);
			con.connect();
			InputStream stm = con.getInputStream();
			prv.refresh(stm);			
			stm.close();
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			Misc.loge("錯誤的URL --> "+addr+" ("+e.getMessage());
		} catch (IOException e) {
			//e.printStackTrace();
			Misc.loge("傳送失敗："+e.getMessage());
		}
	}
	
	private native void recognizeText(byte[] image, int width, int height);
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final WidPreview view = new WidPreview(800,600);
		view.setClickEvent(event->{
			int cx = view.getCursorX();
			int cy = view.getCursorY();
			sendInputEvent(
				"172.16.2.144",
				String.format("mouse=%d,%d", cx,cy)
			);
		});
		
		final BoxLogger boxMesg = new BoxLogger();
		boxMesg.setPrefHeight(100);
		
		final JFXTextField boxAddress = new JFXTextField("172.16.2.144");
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
					takeOutputEvent("172.16.2.144",view);
					//Misc.logv("~~~monitor~~~");
					byte[] data = view.getBgraData();//get image data~~~
					recognizeText(
						data, 
						view.getDataWidth(), 
						view.getDataHeight()
					);
					TimeUnit.MILLISECONDS.sleep(1000);					
				}
			} catch (Exception e) {
				e.printStackTrace();
				Misc.loge("內部錯誤！！"+e.getMessage());
			}
			//sendInputEvent(boxAddress.getText());
		});
		btnMonitor.prefWidthProperty().bind(boxMesg.prefHeightProperty().subtract(7));
		btnMonitor.prefHeightProperty().bind(boxMesg.prefHeightProperty().subtract(7));
		
		//final GridPane lay1 = new GridPane();
		//lay1.getStyleClass().add("grid-medium");
		//lay1.addRow(0, new Label("主機位置："), boxAddress);
		//lay1.addRow(1, new Label("keyboard："), boxKeyPress);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			new Label("主機位置"),
			boxAddress,
			new Label(" "),
			new Label("keyboard"),
			boxKeyPress,
			new Label(" ")
		);
		
		final HBox lay2 = new HBox();
		lay2.getStyleClass().add("hbox-small");
		lay2.getChildren().addAll(boxMesg,btnMonitor);
		lay2.setAlignment(Pos.CENTER);
		HBox.setHgrow(lay1, Priority.NEVER);
		HBox.setHgrow(boxMesg, Priority.ALWAYS);

		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setCenter(view);
		lay0.setRight(lay1);
		lay0.setBottom(lay2);
		return lay0;
	}
}
