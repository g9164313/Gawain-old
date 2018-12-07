package prj.puppet;

import java.io.File;
import java.io.FilenameFilter;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import narl.itrc.ButtonEx;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanPuppeteer extends PanBase {

	public PanPuppeteer(){		
	}
	
	private final WidMonitor monitor = new WidMonitor(1024,768);
	
	private final WidRecorder recorder = new WidRecorder(monitor);
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final VBox layDefault = new VBox();
		layDefault.getStyleClass().add("vbox-small");
				
		final VBox layLinkIP = new VBox();
		//lay2.setStyle("-fx-padding: 10;");
		layLinkIP.getStyleClass().add("vbox-one-dir");
		
		final VBox layFunction = new VBox();
		layFunction.getStyleClass().add("vbox-one-dir");
		layFunction.disableProperty().bind(layLinkIP.disabledProperty().not());
		
		final JFXTextField boxAddress = new JFXTextField("172.16.2.144");
		
		final ButtonEx btnConnect = new ButtonEx(
			"連線","lan-disconnect.png",
			"離線","lan-connect.png"
		).setOnToggle(eventOn->{
			monitor.loopStart(boxAddress.getText());
			layLinkIP.setDisable(true);
		},eventOff->{
			monitor.loopStop();
			layLinkIP.setDisable(false);
		});
		btnConnect.setMaxWidth(Double.MAX_VALUE);

		final JFXButton btnSetting = new JFXButton("設定");		
		btnSetting.getStyleClass().add("btn-raised-3");
		btnSetting.setMaxWidth(Double.MAX_VALUE);
		
		final JFXButton btnAdvance = new JFXButton("進階");		
		btnAdvance.getStyleClass().add("btn-raised-3");
		btnAdvance.setMaxWidth(Double.MAX_VALUE);
		//btnAdvance.disableProperty().bind(layLinkIP.disabledProperty().not());
		btnAdvance.setOnAction(event->{
			recorder.appear(PanPuppeteer.this.getStage());
		});
				
		String[] lstName = Gawain.dirSock.list(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {				
				return name.endsWith(".js");
			}
		});
		final ButtonEx[] lstAction = new ButtonEx[lstName.length];
		for(int i=0; i<lstName.length; i++){
			String name = lstName[i].substring(0, lstName[i].length()-3);
			lstAction[i] = new ButtonEx(name);
			lstAction[i].setOnTaskScriptFile(
				Gawain.pathSock+lstName[i], 
				"mo", monitor
			);
			lstAction[i].setMaxWidth(Double.MAX_VALUE);
		}
		
		//---- the block of speed button----//
		layLinkIP.getChildren().addAll(
			new Label("主機位置"),
			boxAddress
		);		
		layDefault.getChildren().addAll(
			layLinkIP,
			btnConnect,
			btnSetting,			
			btnAdvance,
			layFunction
		);
		layFunction.getChildren().addAll(lstAction);
		
		//---- main frame----//
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		//lay0.setLeft(layAdvn);
		lay0.setCenter(monitor);
		lay0.setRight(layDefault);
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
	}
}
