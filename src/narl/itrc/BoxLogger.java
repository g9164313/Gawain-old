package narl.itrc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

/**
 * Show text message, just like stdout screen~~~
 * Don't call 'Misc.log' in this class, this will result in re-entry!!!
 * @author qq
 *
 */
public class BoxLogger extends PanDecorate {

	private TextArea boxMsg;

	public BoxLogger(int height){
		this();
		boxMsg.setPrefHeight(height);
	}
	
	public BoxLogger(int width,int height){
		this();
		boxMsg.setPrefWidth(width);
		boxMsg.setPrefHeight(height);
	}
	
	public BoxLogger(){
		super("輸出紀錄");		
		prepare();
		build();
	}

	private static SimpleDateFormat logTime = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
	
	public void prepare() {
		lstBox.add(this);
		boxMsg.setEditable(false);		
		init_menu();		
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
	
	private void init_menu(){
		ContextMenu menu = new ContextMenu();
		MenuItem itm1 = new MenuItem("清除");
		itm1.setOnAction(event->{
			boxMsg.setText("");
		});
		MenuItem itm2 = new MenuItem("另存");
		itm2.setOnAction(event->{
			FileChooser chs = new FileChooser();
            chs.setTitle("另存新檔");
            chs.setInitialDirectory(Misc.fsPathRoot);
            File fs = chs.showSaveDialog(Misc.getParent(event));
            if(fs!=null){
            	save_box(fs);
            }
		});
		menu.getItems().addAll(itm1,itm2);
		boxMsg.setContextMenu(menu);
	}
	
	@Override
	public Node eventLayout() {
		boxMsg = new TextArea();
		return boxMsg;
	}
	
	private void save_box(File fs){
		try {
			PrintWriter out = new PrintWriter(fs);
			out.write(boxMsg.getText());
			out.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
			printf(e.getMessage());
		}
	}

	public void printf(String fmt,Object... arg){
		String txt = String.format(fmt, arg);
		boxMsg.appendText(txt);
		boxMsg.setScrollTop(Double.MAX_VALUE);
		if(logFile!=null){
			if(txt.endsWith("\n")==true){
				logFile.print(txt);
			}else{
				logFile.println(txt);
			}
		}
	}
	//----------------------------------------//
	
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
