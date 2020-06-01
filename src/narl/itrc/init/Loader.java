package narl.itrc.init;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;

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
		prepare_resource();
		loading_library();
		updateProgress(-1.,-1.);//let progress-bar waiting~~~
		updateMessage("等待主畫面... ");	
		return 0;
	}
	
	private void prepare_resource() {
		//File fs = new File(Gawain.pathSock);
		//step.1: scan resource files in package
		ArrayList<File> lstRes = new ArrayList<File>();
		URL url = Loader.class.getResource(".");
		if(url==null) {
			return;
		}
		Misc.listFiles(
			new File(url.getPath()), 
			"^.class", 
			lstRes
		);
		//step.2: list files in sock directory
		ArrayList<File> lstSock = new ArrayList<File>();
		Misc.listFiles(
			Gawain.dirSock, 
			"", 
			lstSock
		);
		//step.3: check whether we need to copy it
		updateProgress(0, lstRes.size());
		for(int i=0; i<lstRes.size(); i++) {
			File rr = lstRes.get(i);
			File ss = null;
			boolean found = false;
			for(int j=0; j<lstSock.size(); j++) {
				ss = lstSock.get(j);
				if(rr.getName().equals(ss.getName())==true) {
					found = true;
					lstSock.remove(j);
					break;
				}
			}
			if(found==false) {
				//How to create directory structure??
				Misc.copyFile(
					rr, 
					new File(Gawain.pathSock+File.separatorChar+rr.getName())
				);
			}
		}
	}
	
	private void loading_library(){
		
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
				updateMessage("載入 "+name);
				updateProgress(barCur, barMax);
				barCur+=1L;
			} catch (UnsatisfiedLinkError e1) {
				fs.fail+=1;
				if(fs.fail>(barMax-barCur)){
					//we can't solve the dependency problem, so drop it out.
					updateMessage("拋棄 "+fs.getAbsolutePath());
					break;
				}else{
					//this file have the dependency problem, deal with it later.
					lst.addLast(fs);
					updateMessage("重排 "+fs.getName());
				}				
			}			
		}
	}
	
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
		
		setOnSucceeded(e->{
			Gawain.mainPanel();
			stg.close();
		});
		
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
