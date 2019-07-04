package narl.itrc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class BoxLogger extends TextArea {

	private boolean flagEnable = true;
	
	public BoxLogger(){
		
		setEditable(true);
		setStyle(
			"-fx-control-inner-background: white;"+
			"-fx-text-box-border: black;"
		);

		ContextMenu menu = new ContextMenu();
		CheckMenuItem itm1 = new CheckMenuItem("停止紀錄");
		itm1.setOnAction(event->{
			flagEnable = !flagEnable;
		});		
		MenuItem itm2 = new MenuItem("清除紀錄");
		itm2.setOnAction(event->{
			setText("");
		});
		MenuItem itm3 = new MenuItem("另存紀錄");
		itm3.setOnAction(event->{
			FileChooser chs = new FileChooser();
            chs.setTitle("另存");
            chs.setInitialDirectory(Gawain.dirRoot);
            File fs = chs.showSaveDialog(Misc.getParent(event));
            if(fs!=null){
            	try {
        			PrintWriter out = new PrintWriter(fs);
        			out.write(getText());
        			out.close();
        		} catch (FileNotFoundException e) {
        			//System.err.println(e.getMessage());
        			//how to show message???
        		}
            }
		});
		menu.getItems().addAll(itm1,itm2,itm3);
		setContextMenu(menu);
		
		if(watcher==null){
			watcher = new Timeline(new KeyFrame(Duration.millis(50),eventRefresh));
			watcher.setCycleCount(Timeline.INDEFINITE);
			watcher.play();
		}
		lstBox.add(this);
	}
	
	private static ArrayList<BoxLogger> lstBox = new ArrayList<BoxLogger>();
	
	private static EventHandler<ActionEvent> eventRefresh = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			String txt = "";
			Gawain.LogMessage msg = null;
			do{
				msg = Gawain.logQueue.poll();
				if(msg!=null){
					txt = txt + msg.text;
				}
			}while(msg!=null);
			if(txt.length()==0){
				return;
			}
			for(BoxLogger box:lstBox){
				if(box.flagEnable==false){
					continue;
				}
				box.appendText(txt);
				box.setScrollTop(Double.MAX_VALUE);
			}
		}
	};	
	private static Timeline watcher;
}
