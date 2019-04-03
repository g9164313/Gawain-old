package narl.itrc.nat;

import java.util.LinkedList;

import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class Loader extends Task<Integer> {
	
	public static Runnable hooker = null;
	
	@Override
	protected Integer call() throws Exception {
		
		long t2=0L, t1=System.currentTimeMillis();
		
		loadLibrary(UtilLib.flatten());
		
		if(hooker!=null){
			updateProgress(-1L,-1L);
			hooker.run();
		}
		
		//wait sometime, let user see LOGO....
		do{
			t2 = System.currentTimeMillis();
		}while((t2-t1)<1000);
		
		return 0;
	}
	
	/**
	 * Indicate whether we need to print message....
	 */
	private static boolean debug = false;
	
	private void loadLibrary(final LinkedList<LibFile> lst){
		
		long barCur=0L,  barMax=lst.size();
		
		updateProgress(0, barMax);
		if(barMax==0L){
			return;
		}
		
		String msg = null;
		
		while(lst.isEmpty()==false){
			
			LibFile tkn = lst.pollFirst();
			
			try {
				System.loadLibrary(tkn.getLibName());
				msg = "載入 "+tkn.getName();
				updateMessage(msg);
				if(debug==true){
					System.out.println(msg);
				}
				barCur+=1L;
				
			} catch (UnsatisfiedLinkError e1) {
				
				tkn.fail+=1;

				if(tkn.fail>=(barMax-barCur)){
					//we can't solve the dependency problem, so drop it out.
					msg = "拋棄 "+tkn.getAbsolutePath();
					updateMessage(msg);
					if(debug==true){
						System.out.println(msg);
					}
					break;
				}else{
					//this file have the dependency problem, deal with it later.
					lst.addLast(tkn);
					if(debug==true){
						System.out.println("重排 "+tkn.getName());
					}
				}				
			}
			updateProgress(barCur, barMax);
		}
	}
	
	private Parent layout(Stage stg){
		
		VBox lay0 = new VBox();
		
		lay0.getStyleClass().add("splash-board");
		
		ImageView img = Misc.getIconView("logo.jpg");
		
		ProgressBar bar = new ProgressBar();
		bar.prefWidthProperty().bind(stg.widthProperty().subtract(3));
		bar.progressProperty().bind(progressProperty());
		
		lay0.getChildren().addAll(img,bar);
		
		return lay0;
	}
	
	public void standby(){
		
		String val = Gawain.getSetting().getProperty("LIB_DEBUG","false");
		if(val.toLowerCase().endsWith("true")==true){
			debug = true;
		}
		
		final Stage stg = new Stage(StageStyle.UNIFIED);
		
		stg.initModality(Modality.WINDOW_MODAL);
		
		stg.initStyle(StageStyle.TRANSPARENT);
		
		stg.setResizable(false);
		
		stg.centerOnScreen();
		
		Scene scn = new Scene(layout(stg));
		scn.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());
		
		stg.setScene(scn);

		setOnSucceeded(event->{
			stg.close();
		});
		
		stg.setOnShown(event->{			
			new Thread(Loader.this,"Loader").start();
		});
		stg.showAndWait();
	}
}
