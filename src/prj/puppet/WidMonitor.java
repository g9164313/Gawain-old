package prj.puppet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.shape.Rectangle;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.WidImageView;

public class WidMonitor extends WidImageView {

	public WidMonitor(int width, int height){
		super(width,height);
	}
	
	private Task<Integer> core = null;
	
	private String addr = null;
	
	private final static int CMD_CLICK = 1; 
	private final static int CMD_RECOG = 2;
	
	private static class Bundle {
		public int cmd = 0;
		public int[] arg = {0,0,0,0};
		public Bundle(int... val){
			cmd = val[0];
			for(int i=1; i<val.length; i++){
				if((i-1)>=arg.length){
					break;
				}
				arg[i-1] = val[i];
			}
		}
	};
	
	private ArrayBlockingQueue<Bundle> queCommand = new ArrayBlockingQueue<Bundle>(10);
	
	private ArrayBlockingQueue<String> queRespone = new ArrayBlockingQueue<String>(10);
	
	public void click(
		final int pos_x, 
		final int pos_y
	){
		try {
			queCommand.put(new Bundle(CMD_CLICK,pos_x,pos_y));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * recognize digital character via the third party tool.
	 * @param roi - array present [x,y,width,height] 
	 * @return - result
	 */
	public double recognize(final int[] roi){
		return recognize(roi[0],roi[1],roi[2],roi[3]);
	}
	
	/**
	 *  recognize digital character via the third party tool.
	 * @param roi_x - location X<p>
	 * @param roi_y - location Y<p>
	 * @param roi_w - width <p>
	 * @param roi_h - height<p>
	 * @return - result
	 */
	public double recognize(
		final int roi_x, final int roi_y,
		final int roi_w, final int roi_h
	){
		while(true){
			String txt = null;
			try {
				queCommand.put(new Bundle(
					CMD_RECOG,
					roi_x,roi_y,
					roi_w,roi_h
				));
				txt = queRespone.take();
				return Double.valueOf(txt);
			} catch (InterruptedException e) {
				Misc.loge("fail to control queue");
			} catch (NumberFormatException e) {
				Misc.loge("fail to parse %s",txt);
			}
		}
	}
	
	
	/**
	 * It is a convenience method for script engine
	 * @param val
	 */
	public void delay_sec(int val){		
		try {
			TimeUnit.SECONDS.sleep(val);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
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
				try{
				while(isCancelled()==false){					
					//Thread.holdsLock()
					//process some interrupted events!!!!					
					if(queCommand.isEmpty()==false){
						Bundle bnd = queCommand.poll();
						//Misc.logv("command = %d", bnd.cmd);
						switch(bnd.cmd){
						case CMD_CLICK:							
							clickTarget(bnd.arg[0],bnd.arg[1]);
							break;
						case CMD_RECOG:
							String txt = recognizeDigital(bnd.arg);
							queRespone.put(txt);
							break;
						}
					}else{
						takeOutputEvent();
						TimeUnit.MILLISECONDS.sleep(247);
					}					
				}
			} catch (InterruptedException e) {
	            System.err.println("Interrupted."+e.getMessage());
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
	
	private EventHandler<ActionEvent> eventHookClick = null;

	public WidMonitor setHookEvent(EventHandler<ActionEvent> event){
		eventHookClick = event;
		return this;
	}
	
	private EventHandler<ActionEvent> eventDefault = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			//it should be invoked by event-thread~~~
			int cx = getCursorX();
			int cy = getCursorY();
			sendInputMouse(cx,cy);
			if(eventHookClick!=null){
				eventHookClick.handle(event);
			}
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
	//------------------------//
	
	private void clickTarget(final int pos_x, final int pos_y){
		try {
			Node obj = addCircle(pos_x, pos_y, 13);
			takeOutputEvent();
			TimeUnit.MILLISECONDS.sleep(500);
			String parm = String.format(
				"mouse-click=%d,%d", 
				pos_x,pos_y
			);
			sendInputEvent(parm);
			TimeUnit.MILLISECONDS.sleep(500);
			takeOutputEvent();
			delMark(obj);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	public String recognizeDigital(final int[] roi){
		final String name1 = Gawain.pathSock+"tmp.png";
		final String name2 = Gawain.pathSock+"tmp.pbm";
		Node obj = addMark(roi);
		takeOutputEvent();
		snapData(name1,roi);
		String txt = null;
		txt = Misc.exec("convert",name1,name2);
		txt = Misc.exec("ocrad","--filter=numbers","--scale=3",name2);
		txt = txt.trim()
			.replaceAll("a","0")
			.replaceAll("o","0")
			.replaceAll("O","0")
			.replaceAll("l","1")
			.replaceAll("\\s+","");
		//double val = 0.;
		//try{
		//	val = Double.valueOf(txt);
		///	Misc.logv("recognize '%s' as %f", txt, val);
		//}catch(NumberFormatException e){
		//	Misc.loge("fail to recognize '%s'", txt);
		//}	
		delMark(obj);
		return txt;
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
