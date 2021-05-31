package narl.itrc.init;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class PanSplash extends Application {
	//syntax:
	//text=xxxx    $ show text in label
	//prog=10,1000 $ show progress
	//prog-max=100 $ set the maximum value in progress bar
	//prog-cur=100 $ set the current value in progress bar
	private static final String TKN_DONE = "done";
	private static final String TKN_TEXT = "text";
	private static final String TKN_PCUR = "pcur";
	private static final String TKN_PROG = "prog";
	
	@Override
	public void start(Stage stg) throws Exception {
		//do layout~~~
		final ImageView img = Misc.getIconView("logo.jpg");
		
		final Label txt = new Label("ggyy");		
		
		final ProgressBar bar = new ProgressBar();
		bar.prefWidthProperty().bind(stg.widthProperty().subtract(3));
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("splash-board");
		lay0.getChildren().addAll(img,txt,bar);
		
		//put them all into stage
		final Scene scn = new Scene(lay0);
		scn.getStylesheets().add(Gawain.sheet);		
		stg.initStyle(StageStyle.TRANSPARENT);
		stg.setScene(scn);		
		stg.setResizable(false);
		stg.centerOnScreen();
		
		stg.setOnShown(e->{
			final Task<Integer> tsk = new Task<Integer>() {
				
				private int prog_max = 100;
				
				private int parse_token(final String msg) {
					System.out.printf("-->%s", msg);
					String[] val;
					if(msg.startsWith(TKN_DONE)==true) {
						return -1;
					}else if(msg.startsWith(TKN_TEXT)==true) {
						val = msg.split("=");
						if(val.length>=2) {
							updateMessage(val[1]);
						}
					}else if(msg.startsWith(TKN_PCUR)==true) {
						val = msg.split("=");
						if(val.length>=2) {
							long cur = Long.valueOf(val[1]);
							updateProgress(cur,prog_max);
						}
					}else if(msg.startsWith(TKN_PROG)==true) {
						val = msg.split("[=|,]");
						if(val.length>=3) {
							int cur = Integer.valueOf(val[1]);
							prog_max= Integer.valueOf(val[2]);
							updateProgress(cur,prog_max);
						}
					}
					return 0;
				}
				
				@Override
				protected Integer call() throws Exception {
					//System.out.print("splash wait!!\n");
					Scanner ss = new Scanner(System.in);
					while(true) {	
						String msg = ss.nextLine();
						System.out.printf("%s",msg);					
						if(parse_token(msg)<0) {
							break;
						}
					}
					//System.out.print("splash done!!\n");
					ss.close();
					Platform.exit();
					return 1;
				}				
			};
			txt.textProperty().bind(tsk.messageProperty());
			bar.progressProperty().bind(tsk.progressProperty());
			new Thread(tsk,"--splash--").start();
		});
		stg.show();
	}
	
	private static PrintWriter writer;
	
	//private static PrintWriter writer = null;
	
	public static void done() { 
		send(TKN_DONE);
		if(writer!=null) {
			writer.close();
		}
		writer = null;
	}
	public static void updateMessage(final String txt) {
		send(String.format("%s=%s",TKN_TEXT,txt)); 
	}
	public static void updateProgressCur(final int cur) {
		send(String.format("%s=%d",TKN_PCUR,cur));
	}
	public static void updateProgress(final long cur, final long max) {
		send(String.format("%s=%d,%d",TKN_PROG,cur,max));
	}
	
	public static void send(final String msg) {
		if(writer==null) {
			return;
		}
		//BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		//BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));		
		writer.println(msg);
		writer.flush();
	}
	
	private static final String ARG_TAG = "--splash";
	
	public static void spawn(final String[] argv) {		
		
		String tag = Gawain.prop().getProperty("SPLASH", "");
		if(tag.length()!=0) {
			tag = tag.toLowerCase();
			if(tag.charAt(0)=='f') {
				return;
			}
		}		
		if(argv.length!=0) {
			for(String txt:argv) {
				//System.out.print(txt);
				if(txt.equals(ARG_TAG)==true) {
					launch();
					break;
				}
			}
			System.exit(-88);
		}
		
		final String cmd = System.getProperty("java.home") + 
			File.separatorChar + "bin" + 
			File.separatorChar + "java";
		
		try {
			ProcessBuilder build = new ProcessBuilder();
			if(Gawain.jarName.length()==0) {
				//when we are debugging(in IDE), there is no jar file.					
				String clp = Gawain.prop().getProperty("CLASSPATH", "");
				if(clp.length()==0) {
					//no class-path will fail~~
					return; 
				}
				build.command(cmd,"-classpath",clp,"narl.itrc.Gawain",ARG_TAG);
			}else {
				build.command(cmd,"-jar",Gawain.jarName,ARG_TAG);
			}
			//build.redirectError(new File("splash.err"));
			//build.redirectOutput(new File("splash.txt"));
			Process proc = build.start();
			writer = new PrintWriter(proc.getOutputStream());

		} catch (IOException e) {
		}
	}
}
