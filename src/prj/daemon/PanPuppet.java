package prj.daemon;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.jfoenix.controls.JFXCheckBox;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import narl.itrc.BoxLogger;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanPuppet extends PanBase {

	public PanPuppet(){
		watcher.setCycleCount(Timeline.INDEFINITE);
	}
	
	private Timeline watcher = new Timeline();
	
	private HttpServer serv;
	
	private HttpHandler eventInput = new HttpHandler(){
		@Override
		public void handle(HttpExchange exc) throws IOException {
			
			StringBuilder resp = new StringBuilder();
			resp.append("<html><body>");
			resp.append("process event done!!");
			resp.append("</html></body>");
			
			String[] lstArg = exc.getRequestURI().getQuery().split("&");
			for(String arg:lstArg){
				String[] cmd = arg.toLowerCase().split("=");
				if(cmd.length!=2){
					continue;
				}
				if(cmd[0].startsWith("mouse")==true){
					if(cmd[1].contains(",")==true){
						String[] loca = cmd[1].split(",");						
						if(cmd[0].endsWith("move")==true){
							//absolute moving
							Misc.logv("mouse-move=%s,%s", loca[0], loca[1]);
						}else if(cmd[0].endsWith("shift")==true){
							//relative moving
							Misc.logv("mouse-shift=%s,%s", loca[0], loca[1]);
						}
					}else{
						if(cmd[0].endsWith("click")==true){
							
						}
					}
					 
				}else if(cmd[0].startsWith("keyboard")==true){
					
				}
			}
			
			exc.sendResponseHeaders(200, resp.length());
			OutputStream stm = exc.getResponseBody();
			stm.write(resp.toString().getBytes());
			stm.close();

			Misc.logv("@@@ input-event @@@");
		}
	};
	
	private HttpHandler eventOutput = new HttpHandler(){
		@Override
		public void handle(HttpExchange exc) throws IOException {
			
			exc.getResponseHeaders().add("Content-Type", "image/png");
			
			int[] inf = {0, 0};
			byte[] buf = Misc.screenshot2png(inf);

			exc.sendResponseHeaders(200, buf.length);
			
			OutputStream stm = exc.getResponseBody();
			stm.write(buf);
			stm.close();
			
			Misc.deleteScreenshot(buf);
			Misc.logv("@@@ screenshot @@@");
		}
	};
	
	@Override
	protected void eventShown(WindowEvent e){		
		try {
			serv = HttpServer.create(new InetSocketAddress(9911),0);
			serv.createContext("/input" ,eventInput);
			serv.createContext("/output",eventOutput);
			serv.setExecutor(null);
			serv.start();
			Misc.logv("Turn On!!!");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	protected void eventClose(WindowEvent e){
		Misc.logv("Shutdown...");
		Misc.deleteScreenshot(null);//release allocated memory~~
		serv.stop(1);		
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final BoxLogger boxMsg = new BoxLogger();
		boxMsg.setPrefSize(200, 100);
		
		
		final String title1 = "滑鼠位置";
		
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
		});
		
		final GridPane lay1 = new GridPane();
		lay1.addRow(0, chk);
		GridPane.setHgrow(chk, Priority.ALWAYS);
		
		final HBox lay0 = new HBox();
		lay0.getChildren().addAll(boxMsg,lay1);
		HBox.setHgrow(boxMsg, Priority.ALWAYS);
		
		return lay0;
	}
}
