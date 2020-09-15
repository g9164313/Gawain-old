package narl.itrc.init;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class Loader extends Task<Integer> {

	@Override
	protected Integer call() throws Exception {
		//check dependency~~~
		check_socket_dir();
		check_dependency();
		//awaken_terminal();
		//progress-bar waiting~~~
		updateMessage("等待主畫面... ");
		updateProgress(-1.,-1.);
		Application.invokeAndWait(()->Gawain.mainPanel());
		return 0;
	}

	private void check_socket_dir() {
		updateMessage("準備資源... ");
		final File fs = new File(Gawain.pathSock);
		if(fs.exists()==false) {
			//In fact, logger will create this directory
			if(fs.mkdirs()==false) {
				Misc.loge("致命錯誤，無法產生 socket！！");
				System.exit(-1);
			}
		}
		
		//step.1: scan resource files in package
		final String appx = "^.class;.css";
		final URL url = Loader.class.getResource("/narl/itrc/res");
		if(url==null) {
			Misc.loge("致命內部URL錯誤！！");
			System.exit(-2);
		}
		ArrayList<File> lstRes = new ArrayList<File>();
		Misc.listFiles(
			new File(url.getPath()),false, 
			appx, lstRes
		);
		//step.2: list files in sock directory
		ArrayList<File> lstSock = new ArrayList<File>();
		Misc.listFiles(
			Gawain.dirSock,false,
			appx, lstSock
		);
		//step.3: check whether we need to copy it
		for(int i=0; i<lstRes.size(); i++) {
			updateProgress(i+1, lstRes.size());
			File rr = lstRes.get(i);
			boolean found = false;
			for(int j=0; j<lstSock.size(); j++) {
				File ss = lstSock.get(j);
				if(rr.getName().equals(ss.getName())==true) {
					found = true;
					lstSock.remove(j);
					break;
				}
			}
			if(found==false) {
				//How to create directory structure??
				//How to the compression file???
				try {
					Files.copy(
						rr.toPath(), 
						new File(Gawain.pathSock+rr.getName()).toPath()
					);
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		}
	}
	
	private void check_dependency(){
		
		LinkedList<LibFile> lst = new LinkedList<LibFile>();
		
		LibUtility.listAll(Gawain.pathSock,lst);
		for(String key:Gawain.prop().stringPropertyNames()) {
			if(key.matches("LIB_PATH[\\d]*")==false) {
				continue;
			}
			String path = Gawain.prop().getProperty(key);
			updateMessage("列舉: "+path);
			LibUtility.listAll(path,lst);
		}
				
		long barCur=0L,  barMax=lst.size();
		updateProgress(0, barMax);

		while(lst.isEmpty()==false){
			LibFile fs = lst.pollFirst();
			try {
				System.loadLibrary(fs.getLibName());
				String name = fs.getName();
				Misc.logv("load "+name);
				updateMessage("載入依賴："+name);
				updateProgress(barCur, barMax);
				barCur+=1L;
			} catch (UnsatisfiedLinkError e1) {
				fs.fail+=1;
				if(fs.fail>(barMax-barCur)){
					//we can't solve the dependency problem, so drop it out.
					updateMessage("拋棄："+fs.getAbsolutePath());
					break;
				}else{
					//this file have the dependency problem, deal with it later.
					lst.addLast(fs);
					updateMessage("重排："+fs.getName());
				}				
			}			
		}
	}
	
	/*private void awaken_terminal() {
		updateMessage("確認外部命令");	
		updateProgress(-1.,-1.);
		for(Entry<Object, Object> e : Gawain.prop().entrySet()){
			final String key = e.getKey().toString();
			if(key.matches("^TERM_\\p{Alnum}+")==false) {
				continue;
			}
			final String tag = key.split("_")[1];
			final String val = e.getValue().toString();
			updateMessage("載入："+tag);	
			Terminal.getInstance().exec(tag,val);
		}
		//test~~~
		Misc.logv(">>"+Terminal.getInstance().exec("/usr/bin/python3","-c","print(4+4)"));
	}*/
	
	private Parent layout(Stage stg){
		
		ImageView img = Misc.getIconView("logo.jpg");
		
		Label txt = new Label();		
		txt.textProperty().bind(messageProperty());

		ProgressBar bar = new ProgressBar();
		bar.prefWidthProperty().bind(stg.widthProperty().subtract(3));
		bar.progressProperty().bind(progressProperty());
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("splash-board");
		lay0.getChildren().addAll(img,txt,bar);
		return lay0;
	}
	
	public void launch(final Stage stg){		
		setOnSucceeded(e->stg.close());		
		final Scene scn = new Scene(layout(stg));
		scn.getStylesheets().add(Gawain.sheet);
		stg.setScene(scn);
		stg.initStyle(StageStyle.TRANSPARENT);
		//stg.setAlwaysOnTop(true);
		stg.setResizable(false);
		stg.centerOnScreen();		
		stg.setOnShown(e->new Thread(this,"Loader").start());
		stg.show();
	}
}
