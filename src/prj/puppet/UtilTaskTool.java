package prj.puppet;

import java.util.concurrent.ConcurrentHashMap;

import com.sun.glass.ui.Application;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Because only event-thread can access JavaFX controls and property.<p>
 * These static methods provide the convenience way to swap data between threads.<p>  
 * @author qq
 *
 */
public class UtilTaskTool {
	
	public static ConcurrentHashMap<Long,String> map_text = new ConcurrentHashMap<Long,String>();
	
	public static void setDisable(final Node nod, final boolean flag){
		
		if(Application.isEventThread()==true){
			nod.setDisable(flag);
			return;
		}		
		final Runnable event = new Runnable(){
			@Override public void run() {
				nod.setDisable(flag);
			}
		};
		Application.invokeAndWait(event);
	}
	
	public static void setText(final TextField box,final String txt){
		
		if(Application.isEventThread()==true){
			box.setText(txt);
			return;
		}
		final Runnable event = new Runnable(){
			@Override public void run() {
				box.setText(txt);
			}
		};
		Application.invokeAndWait(event);
	}
	
	public static String getText(final TextField box){
		
		final long id = Thread.currentThread().getId();
		
		if(Application.isEventThread()==true){
			return box.getText();
		}
		final Runnable event = new Runnable(){
			@Override public void run() {
				map_text.put(id,box.getText());
			}
		};
		Application.invokeAndWait(event);
		
		return map_text.get(id);
	}
	
	public static String getText(final TextArea box){
		
		final long id = Thread.currentThread().getId();
		
		if(Application.isEventThread()==true){
			return box.getText();
		}
		final Runnable event = new Runnable(){
			@Override public void run() {
				map_text.put(id,box.getText());
			}
		};
		Application.invokeAndWait(event);
		
		return map_text.get(id);
	}
	
	
	public static int[] getTextRoi(final TextField[] roi){		
		final int[] roiVal = {0,0,0,0};		
		final Runnable event = new Runnable(){
			@Override public void run() {
				roiVal[0] = Integer.valueOf(roi[0].getText());
				roiVal[1] = Integer.valueOf(roi[1].getText());
				roiVal[2] = Integer.valueOf(roi[2].getText());
				roiVal[3] = Integer.valueOf(roi[3].getText());
			}
		};
		if(Application.isEventThread()==true){			
			event.run();
		}else{
			Application.invokeAndWait(event);
		}		
		return roiVal;
	}
	
	public static void clear(){
		map_text.remove(Thread.currentThread().getId());
	}	
}
