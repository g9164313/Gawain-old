package narl.itrc.nat;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class Loader extends Task<Integer> {

	@Override
	protected Integer call() throws Exception {
		prepare_resource();
		load_library();
		prepare_main_panel();
		return 0;
	}
	
	private void prepare_resource() {
		//File fs = new File(Gawain.pathSock);
		//step.1: scan resource files in package
		ArrayList<File> lstRes = new ArrayList<File>();
		//TODO:how to get resource files in jar???
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
	
	private void load_library(){
		
		LinkedList<LibFile> lst = new LinkedList<LibFile>();
		LibUtil.listAll(Gawain.pathSock,lst);
		
		long barCur=0L,  barMax=lst.size();
		updateProgress(0, barMax);

		while(lst.isEmpty()==false){
			LibFile tkn = lst.pollFirst();
			try {
				System.loadLibrary(tkn.getLibName());
				updateMessage("載入 "+tkn.getName());
				barCur+=1L;
			} catch (UnsatisfiedLinkError e1) {
				tkn.fail+=1;
				if(tkn.fail>=(barMax-barCur)){
					//we can't solve the dependency problem, so drop it out.
					updateMessage("拋棄 "+tkn.getAbsolutePath());
					break;
				}else{
					//this file have the dependency problem, deal with it later.
					lst.addLast(tkn);
					updateMessage("重排 "+tkn.getName());
				}				
			}
			updateProgress(barCur, barMax);
		}
	}
		
	private Stage primer = null;
	
	private void prepare_main_panel() {
		String name = Gawain.prop().getProperty("LAUNCH","");
		//Example: if contractor has parameter, try this~~~
		//panRoot = (PanBase)Class.forName(name)
		//	.getConstructor(Stage.class)
		//	.newInstance(primaryStage);
		updateMessage("啟動-"+name);
		try {
			if(name.length()==0) {
			}else {			
				long t1 = System.currentTimeMillis();
				Gawain.mainPanel = (PanBase)Class.forName(name)
					.getConstructor(Stage.class)
					.newInstance(primer);
				Gawain.mainPanel.prepare();//lazy initialization~~~
				long t2 = System.currentTimeMillis();
				updateMessage(String.format("wait %dms", t2-t1));
			}
		} catch (
			InstantiationException | 
			IllegalAccessException | 
			IllegalArgumentException | 
			InvocationTargetException | 
			NoSuchMethodException | 
			SecurityException | 
			ClassNotFoundException e
		) {
			Misc.loge(e.getMessage());
			updateMessage("致命的類別錯誤!!");
			Gawain.mainPanel = null;
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
	
	public void launch(final Stage primaryStage){
		
		primer = primaryStage;

		final Stage stg = new Stage();
		stg.initModality(Modality.WINDOW_MODAL);
		stg.initStyle(StageStyle.TRANSPARENT);
		stg.setResizable(false);
		stg.centerOnScreen();
		setOnSucceeded(event->{
			Gawain.mainPanel.appear();
			stg.close();
		});
		
		Scene scn = new Scene(layout(stg));
		scn.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());
		stg.setScene(scn);
		stg.setOnShown(event->{			
			new Thread(Loader.this,"Loader").start();
		});
		stg.show();
	}
}
