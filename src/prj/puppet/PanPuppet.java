package prj.puppet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.BoxLogger;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanPuppet extends PanBase {

	public PanPuppet(){
		watcher.setCycleCount(Timeline.INDEFINITE);
	}
	
	private final String dummyHTML = "<html><body>dummy!!</html></body>";
	
	private Timeline watcher = new Timeline();
	
	private HttpServer serv;
	
	private HttpHandler eventInput = new HttpHandler(){
				
		@Override
		public void handle(HttpExchange exc) throws IOException {
			String[] lstArg = exc.getRequestURI().getQuery().split("&");
			for(String arg:lstArg){
				String[] cmd = arg.split("=");				
				if(cmd.length!=2){
					continue;
				}
				cmd[0] = cmd[0].toLowerCase();
				if(cmd[0].startsWith("mouse")==true){
					String[] loca = cmd[1].split(",");
					int cx = Integer.valueOf(loca[0]);
					int cy = Integer.valueOf(loca[1]);
					Misc.sendMouseClick(cx,cy);
					Misc.logv("mouse-click=%s,%s", loca[0], loca[1]);
				}else if(cmd[0].startsWith("keyboard")==true){
					Misc.sendKeyboardText(cmd[1]);
					Misc.logv("press key = ["+cmd[1]+"]");
				}
			}
			exc.sendResponseHeaders(200, dummyHTML.length());
			OutputStream stm = exc.getResponseBody();
			stm.write(dummyHTML.toString().getBytes());
			stm.close();			
		}
	};
	
	private HttpHandler eventOutput = new HttpHandler(){
		@Override
		public void handle(HttpExchange exc) throws IOException {
			
			int[] inf = {0, 0};
			byte[] buf = Misc.screenshot2png(inf);
			
			exc.getResponseHeaders().add("Content-Type", "image/png");
			exc.sendResponseHeaders(200, buf.length);
			
			OutputStream stm = exc.getResponseBody();
			stm.write(buf);
			stm.close();
			
			Misc.deleteScreenshot(buf);
			Misc.logv("@@@ screenshot @@@");
		}
	};
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final BoxLogger boxMsg = new BoxLogger();
		boxMsg.setPrefSize(400, 100);
		
		/*final String title1 = "滑鼠位置";
		final JFXCheckBox chk = new JFXCheckBox(title1);
		final KeyFrame loop1 = new KeyFrame(Duration.millis(250), event->{
			final int[] pos = {0,0};
			Misc.getCursorPos(pos);
			chk.setText(String.format("%d,%d",pos[0],pos[1]));
		});
		chk.setOnAction(event->{
			if(chk.isSelected()==true){
				watcher.getKeyFrames().add(loop1);
				watcher.play();
			}else{
				chk.setText(title1);
				watcher.stop();
			}
		});*/
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().add("grid-small");
		//lay1.addRow(0, chk);
		//GridPane.setHgrow(chk, Priority.ALWAYS);
		
		final HBox lay0 = new HBox();
		lay0.getStyleClass().add("hbox-small");
		lay0.getChildren().addAll(boxMsg,lay1);
		HBox.setHgrow(boxMsg, Priority.ALWAYS);
		
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
		try {
			serv = HttpServer.create(new InetSocketAddress(9911),0);
			serv.createContext("/input" ,eventInput);
			serv.createContext("/output",eventOutput);
			serv.createContext("/screen",eventOutput);//alias,another name
			serv.setExecutor(null);
			serv.start();
			Misc.logv("Turn on HTTP server !!!");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	protected void eventClose(PanBase self){
		Misc.logv("Shutdown...");
		Misc.deleteScreenshot(null);//release allocated memory~~
		serv.stop(1);
	}
}
