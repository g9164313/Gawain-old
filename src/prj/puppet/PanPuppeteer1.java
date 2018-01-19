package prj.puppet;

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
import narl.itrc.TaskTool;
import narl.itrc.WidImageView;

public class PanPuppeteer1 extends PanBase {

	public PanPuppeteer1(){		
	}
	
	private void sendInputEvent(String addr,String param){
		try {
			URL url = new URL("http://"+addr+":9911/input?"+param);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(50);
			con.getInputStream().close();
			Misc.logv(param);
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			Misc.loge("錯誤的URL --> "+addr+" ("+e.getMessage());
		} catch (IOException e) {
			//e.printStackTrace();
			Misc.loge("傳送失敗："+e.getMessage());
		}
	}
	
	private void takeOutputEvent(String addr,WidImageView prv){
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
	//-------------------------------------//
	
	//private final int[] optSystem  = {13 ,40,90,74};
	//private final int[] optMonitor = {112,40,90,74};
	
	private final int[] optChamber = {210,40,90,74};
	private final int[] ChamberCHAM= {14,139,90,74};
	//private final int[] ChamberPOWR= {14,237,90,74};
	//private final int[] ChamberPROC= {14,340,90,74};
	//private final int[] ChamberCTRL= {14,440,90,74};
	//private final int[] ChamberCOMM= {14,540,90,74};
	//private final int[] ChamberPREV= {14,640,90,74};
	
	//private final int[] pressProgram = {407,40,90,74};
	//private final int[] pressService = {407,40,90,74};
	
	private final int[] roiPress1= {870,220,87,23};
	private final int[] roiPress2= {870,267,87,23};
	private final int[] roiOffset= {870,307,87,23};
	private final int[] roiTorr  = {608,605,87,23};
	
	private final int[] pressPump1= {194,649,171,72};
	private final int[] pressPump2= {612,454,106,141};
	
	private final int[] pressValve1= {412,492, 44, 67};
	private final int[] pressValve2= {751,426, 92, 41};
	
	private final int[] pressDialogConfirm={354,399,86,74};
	private final int[] pressDialogCancel ={476,401,86,74};
	
	private void clickTarget(
		final String addr,
		final WidImageView view, 
		final int[] roi
	){
		try {
			Node obj = view.addCross(roi);
			takeOutputEvent(addr,view);
			TimeUnit.MILLISECONDS.sleep(100);
			String parm = String.format(
				"mouse-click=%d,%d", 
				roi[0]+roi[2]/2, 
				roi[1]+roi[3]/2
			);
			sendInputEvent(addr,parm);
			TimeUnit.MILLISECONDS.sleep(100);
			takeOutputEvent(addr,view);
			view.delMark(obj);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private double recognizeDigi(
		final String addr,
		final WidImageView view, 
		final int[] roi
	){
		final String name1 = "tmp.png";
		final String name2 = "tmp.pbm";
		Node obj = view.addMark(roi);
		
		takeOutputEvent(addr,view);
		view.snapData(name1,roiPress1);
		
		String txt = null;
		txt = Misc.exec("convert",name1,"-resize","300%",name2);
		txt = Misc.exec("ocrad",name2);
		txt = txt.trim().replaceAll("\\s+","");//zero may be recognized as character 'O'
		double val = 0.;
		try{
			val = Double.valueOf(txt);
			Misc.logv("recognize '%s' as %f", txt, val);
		}catch(NumberFormatException e){
			Misc.loge("fail to recognize '%s'", txt);
		}	
		view.delMark(obj);
		return val;
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final VBox lay1 = new VBox();
		
		final BoxLogger boxMesg = new BoxLogger();
		boxMesg.setPrefHeight(100);
		
		final JFXTextField boxAddress = new JFXTextField("172.16.2.144");
		boxAddress.setPrefWidth(70);

		final JFXTextField boxKeyPress = new JFXTextField("test!!");
		boxKeyPress.setOnAction(event->{
			//sendInputEvent(
			//	boxAddress.getText(), 
			//	"keyboard="+boxKeyPress.getText()
			//);
			//boxKeyPress.setText("");
		});
		
		final WidImageView vewMonitor = new WidImageView(1024,768);
		final ButtonTask btnMonitor = new ButtonTask("Monitor");
		
		final ButtonTask btnSnapShoot = new ButtonTask("擷取螢幕");
		btnSnapShoot.setMaxWidth(Double.MAX_VALUE);
		btnSnapShoot.setAction(false,event->{
			String addr = TaskTool.getText(boxAddress);
			takeOutputEvent(addr,vewMonitor);
			vewMonitor.snapData("snap.png");
			TaskTool.clear();
		});
		
		final ButtonTask btnAutoPump = new ButtonTask("Auto Pump");
		btnAutoPump.setMaxWidth(Double.MAX_VALUE);
		btnAutoPump.setAction(false,event->{
			
			String addr = TaskTool.getText(boxAddress);
			double val = 0;
			
			//Step.0: Insure we selected the right page
			clickTarget(addr, vewMonitor, optChamber);
			clickTarget(addr, vewMonitor, ChamberCHAM);
			
			//Step.0: Turn-off cryogenic valve

			
			//step.1: turn on pump, and wait 5 second
			clickTarget(addr, vewMonitor, pressPump1);
			Misc.delay_sec(1);
			clickTarget(addr, vewMonitor, pressDialogConfirm);
			Misc.delay_sec(3);
			
			Misc.delay_sec(5);//experience skill~~~
			
			//step.2: 左上閥打開(等到機器聲音出來)
			clickTarget(addr, vewMonitor, pressValve1);
			Misc.delay_sec(1);
			clickTarget(addr, vewMonitor, pressDialogConfirm);
			Misc.delay_sec(3);
			
			//step.3: 看上方壓力直到(4.5e-2 Torr)
			do{
				val = recognizeDigi(addr, vewMonitor, roiPress1);
				Misc.delay_sec(1);
			}while(val>4E-2);
			
			//step.4: 左上閥關閉(等到機器聲音出來)
			clickTarget(addr, vewMonitor, pressValve1);
			Misc.delay_sec(1);
			clickTarget(addr, vewMonitor, pressDialogConfirm);
			Misc.delay_sec(3);
			
			//step.6: 關閉主泵
			
			//step.7: turn on cryogenic valve(it is always on)
			clickTarget(addr, vewMonitor, pressValve2);
			Misc.delay_sec(1);
			clickTarget(addr, vewMonitor, pressDialogConfirm);
			Misc.delay_sec(3);
			
			//step.8: 看上方壓力直到(4.5e-6 Torr)
			do{
				val = recognizeDigi(addr, vewMonitor, roiPress1);
				Misc.delay_sec(1);
			}while(val>4E-6);
			
			//take the last snapshot			
			takeOutputEvent(addr,vewMonitor);
			TaskTool.clear();
		});
	
		final ButtonTask btnLeaveVacuum = new ButtonTask("破真空");
		btnLeaveVacuum.setMaxWidth(Double.MAX_VALUE);
		btnLeaveVacuum.setAction(false,event->{
			
		});
		
		vewMonitor.setClickEvent(event->{
			if(btnMonitor.isDone()==true){
				return;
			}
			int cx = vewMonitor.getCursorX();
			int cy = vewMonitor.getCursorY();
			sendInputEvent(
				TaskTool.getText(boxAddress),
				String.format("mouse=%d,%d", cx,cy)
			);
			TaskTool.clear();
		});		
		btnMonitor.setAction(true,event->{
			ButtonTask.Action act = (ButtonTask.Action)event.getTarget();
			String addr = TaskTool.getText(boxAddress);
			try {
				TaskTool.setDisable(lay1, true);				
				while(act.isCancelled()==false){
					takeOutputEvent(addr,vewMonitor);
					//byte[] data = view.getBgraData();//get image data~~~
					//recognizeText(
					//	data,
					//	view.getDataWidth(), 
					//	view.getDataHeight()
					//);
					TimeUnit.MILLISECONDS.sleep(200);
				}				
			} catch(Exception e){
				//e.printStackTrace();
				//Misc.loge("內部錯誤！！"+e.getMessage());
				Thread.currentThread().interrupt();				
			}
			TaskTool.setDisable(lay1, false);
			TaskTool.clear();
			//sendInputEvent(boxAddress.getText());
		});
		btnMonitor.prefWidthProperty().bind(boxMesg.prefHeightProperty().subtract(7));
		btnMonitor.prefHeightProperty().bind(boxMesg.prefHeightProperty().subtract(7));
		
		//final GridPane lay1 = new GridPane();
		//lay1.getStyleClass().add("grid-medium");
		//lay1.addRow(0, new Label("主機位置："), boxAddress);
		//lay1.addRow(1, new Label("keyboard："), boxKeyPress);

		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			new Label("主機位置"),
			boxAddress,
			new Label(" "),
			new Label("keyboard"),
			boxKeyPress,
			new Label(" "),
			btnSnapShoot,
			btnAutoPump,
			btnLeaveVacuum
		);
		
		final HBox lay2 = new HBox();
		lay2.getStyleClass().add("hbox-small");
		lay2.getChildren().addAll(boxMesg,btnMonitor);
		lay2.setAlignment(Pos.CENTER);
		HBox.setHgrow(lay1, Priority.NEVER);
		HBox.setHgrow(boxMesg, Priority.ALWAYS);

		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setCenter(vewMonitor);
		lay0.setRight(lay1);
		lay0.setBottom(lay2);
		return lay0;
	}
}
