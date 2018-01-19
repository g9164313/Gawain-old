package prj.puppet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.Rectangle;
import narl.itrc.Misc;
import narl.itrc.WidImageView;

public class WidMonitor extends WidImageView {

	public WidMonitor(int width, int height){
		super(width,height);
	}
	
	private Task<Integer> core = null;
	
	private String addr = null;
	
	public void loopStart(final String ip_addr){		
		if(core!=null){
			if(core.isDone()==false){
				Misc.logv("monitor-core is running");
				return;
			}
		}
		addr = ip_addr;		
		setClickEvent(eventDefault);
		core = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				while(isCancelled()==false){
					takeOutputEvent();
					//process some interrupted events!!!!
					TimeUnit.MILLISECONDS.sleep(250);
				}
				return 0;
			}
		};
		new Thread(core,"web-monitor").start();
	}
	
	public void loopStop(){
		setClickEvent(null);
		if(core!=null){
			if(core.isDone()==false){
				core.cancel();
				return;
			}
		}
	}
	
	private EventHandler<ActionEvent> eventDefault = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			int cx = getCursorX();
			int cy = getCursorY();
			sendInputMouse(cx,cy);
		}
	};
	//------------------------//
	
	private Rectangle markItm = null;
	private int[] markROI = null;
	
	public void markSet(final int[] roi){
		markROI = roi;//keep this data~~~
		if(markItm==null){
			markItm = (Rectangle)addMark(roi);
		}else{
			markItm.setX(roi[0] - infoGeom[0]/2);//why we need this offset ??
			markItm.setY(roi[1] - infoGeom[1]/2);//why we need this offset ??
			markItm.setWidth(roi[2]);
			markItm.setHeight(roi[3]);
		}		
	}
	
	/**
	 * let mark follow the location of cursor.<p>
	 */
	public void markLocate(final TextField[] lstBox){
		markROI = new int[4];
		markROI[0] = Integer.valueOf(lstBox[0].getText());
		markROI[1] = Integer.valueOf(lstBox[1].getText());
		markROI[2] = Integer.valueOf(lstBox[2].getText());
		markROI[3] = Integer.valueOf(lstBox[3].getText());
		markSet(markROI);
		setOnMouseMoved(event->{
			infoGeom[3] = (int)event.getX();
			infoGeom[4] = (int)event.getY();
			markROI[0] = infoGeom[3] - markROI[2]/2;
			markROI[1] = infoGeom[4] - markROI[2]/2;
			markSet(markROI);
		});
		setOnMouseClicked(event->{
			infoGeom[3] = (int)event.getX();
			infoGeom[4] = (int)event.getY();
			markROI[0] = infoGeom[3] - markROI[2]/2;
			markROI[1] = infoGeom[4] - markROI[2]/2;
			markSet(markROI);
			lstBox[0].setText(String.valueOf(markROI[0]));
			lstBox[1].setText(String.valueOf(markROI[1]));
			setOnMouseEntered(null);
			setOnMouseMoved(null);
			setOnMouseClicked(null);
			setClickEvent(eventDefault);
		});
		
	}
	
	public void markClear(){
		if(markItm!=null){
			//ova1.getChildren().remove(mark);
			clearOverlay();
			markItm = null;
			markROI = null;
		}
	}
		
	private Task<Double> taskRecog = null;
	
	public void recognize(final int[] roi,final Label txt){
		if(taskRecog!=null){
			if(taskRecog.isDone()==false){				
				return;
			}
		}
		if(roi!=null){
			markSet(roi);
		}
		taskRecog = new Task<Double>(){
			@Override
			protected Double call() throws Exception {
				return recognizeDigi(markROI);
			}
		};
		taskRecog.setOnSucceeded(event->{
			if(txt==null){
				return;
			}
			try {
				double val = taskRecog.get();
				txt.setText(String.format("%.3E",val));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		new Thread(taskRecog,"task-recognize").start();		
	}	
	//------------------------//
	
	/*private void clickTarget(final int[] roi){
		try {
			Node obj = addCircle(roi);
			takeOutputEvent();
			TimeUnit.MILLISECONDS.sleep(500);
			String parm = String.format(
				"mouse-click=%d,%d", 
				roi[0]+roi[2]/2, 
				roi[1]+roi[3]/2
			);
			sendInputEvent(parm);
			TimeUnit.MILLISECONDS.sleep(500);
			takeOutputEvent();
			delMark(obj);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
		
	private double recognizeDigi(final int[] roi){
		
		final String name1 = "tmp.png";
		final String name2 = "tmp.pbm";
		Node obj = addMark(roi);
			
		takeOutputEvent();
		snapData(name1,roi);
		
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
		delMark(obj);
		return val;
	}
	//------------------------//
	
	private void sendInputMouse(int cx,int cy){
		sendInputEvent(String.format("mouse=%d,%d", cx,cy));
	}
	
	private void sendInputEvent(String param){
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
	
	private void takeOutputEvent(){
		try {
			URL url = new URL("http://"+addr+":9911/output");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(50);
			con.connect();
			InputStream stm = con.getInputStream();
			refresh(stm);			
			stm.close();
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			Misc.loge("錯誤的URL --> "+addr+" ("+e.getMessage());
		} catch (IOException e) {
			//e.printStackTrace();
			Misc.loge("傳送失敗："+e.getMessage());
		}
	}
}
