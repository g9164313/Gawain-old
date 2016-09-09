package narl.itrc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.sun.glass.ui.Application;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;

/**
 * Show text message, just like stdout screen~~~
 * Don't call 'Misc.log' in this class, this will result in re-entry!!!
 * @author qq
 *
 */
public class BoxLogger extends PanDecorate {

	private TextArea box;
	
	public BoxLogger(){
		super("輸出紀錄");		
		prepare();
	}
	
	public BoxLogger(int height){
		this();
		box.setPrefHeight(height);
	}
	
	private static SimpleDateFormat logTime = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
	
	public void prepare() {
		lstBox.add(this);
		box.setEditable(false);
		if(logFile!=null){
			return;
		}
		try {
			String name = Misc.pathTemp + "logger.txt";
			FileWriter fw = new FileWriter(name, true);
			BufferedWriter bw = new BufferedWriter(fw);
			logFile = new PrintWriter(bw);
			logFile.println("========"+logTime.format(new Date())+"========");
		} catch (IOException e) {
			logFile = null;
			e.printStackTrace();
		}		
	}
	
	@Override
	public Node layoutBody() {
		box = new TextArea();
		return box;
	}
	
	public void printf(String fmt,Object... arg){
		String txt = String.format(fmt, arg);
		box.appendText(txt);
		box.setScrollTop(Double.MAX_VALUE);
		if(logFile!=null){
			if(txt.endsWith("\n")==true){
				logFile.print(txt);
			}else{
				logFile.println(txt);
			}
		}
	}

	private static PrintWriter logFile = null;
	
	private static ArrayList<BoxLogger> lstBox = new ArrayList<BoxLogger>();

	private static String oldMessage = "";
	
	public static void printAll(final String fmt,final Object... arg){
		final Runnable task = new Runnable(){
			@Override
			public void run() {
				for(BoxLogger box:lstBox){
					if(oldMessage.length()!=0){
						box.printf(oldMessage);
					}
					box.printf(fmt,arg);
				}
				if(lstBox.isEmpty()==true){
					oldMessage = oldMessage + String.format(fmt,arg);
				}else{
					oldMessage = "";//reset 
				}
			}
		};
		if(Application.isEventThread()==true){
			task.run();
		}else{
			Application.invokeAndWait(task);
		}
	}
	
	public static void pruneList(ObservableList<Node> lst){		
		for(Node nd:lst){
			if(nd instanceof Parent){
				pruneList(((Parent)nd).getChildrenUnmodifiable());
			}
			if(nd instanceof BoxLogger){
				lstBox.remove(((BoxLogger)nd));
				if(lstBox.isEmpty()==true){
					if(logFile!=null){
						logFile.close();
					}					
				}
				return;
			}
		}
	}
}
