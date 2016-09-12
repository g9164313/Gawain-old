package prj.letterpress;

import com.jfoenix.controls.JFXTabPane;

import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.CamBundle;
import narl.itrc.CamVidcap;
import narl.itrc.DevTTY;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;
import narl.itrc.TskDialog;

public class Entry extends PanBase {

	public Entry(){
		//firstAction = FIRST_MAXIMIZED;
	}
	
	private PanMapWafer wmap = new PanMapWafer();
	
	public static CamBundle cam0 = new CamVidcap("0");
	public static CamBundle cam1 = new CamVidcap("1");	
	public static ImgRender rndr = new ImgRender(cam0);
	
	public static DevB140M stg0 = new DevB140M();
	public static DevTTY   stg1 = new DevTTY("/dev/ttyACM0,9600,8n1");//this connect to ATmega controller  
	
	private TskAction tsk0 = new TskAligment(rndr,Entry.this);
	private TskGoHome tsk1 = new TskGoHome(stg0,Entry.this);
	private TskAction tsk2 = new TskScanning(stg0,wmap,Entry.this);
	
	/**
	 * this flag means that we don't enable camera (render stage)
	 */
	private boolean camDryRun = true;
	
	/**
	 * this flag means that we don't enable motion stage
	 */
	private boolean stgDryRun = false;

	@Override
	protected void eventShown(WindowEvent e){
		if(camDryRun==false){
			rndr.play();
		}
		if(stgDryRun==false){
			//10pps <==> 50um
			stg0.setFactor(200,200,200,200);
			stg0.setTokenBase('A');
			stg0.setRoutine('B','A','C','D');
			stg0.exec("RS\r\n");//this command must be executed independently.
			stg0.exec(
				"SP 10000,10000,50000,10000;"+
			    "AC 10000,10000,50000,10000;"+
				"DC 10000,10000,50000,10000;"+
			    "TP\r\n"
			);
		}		
	}
	
	@Override
	protected void eventClose(WindowEvent e){
		if(camDryRun==false){
			rndr.stop();//let application release resource~~
		}	
	}
	
	private Node layoutAction(){
		VBox lay = new VBox();
		lay.getStyleClass().add("vbox-small");
		
		Button btn;
		btn = genButton0("快速執行","run.png");
		btn.setOnAction(new TskDialog(Entry.this){
			@Override
			public int looper(Task<Integer> tsk) {
				logv("working...");
				Misc.delay(100);
				return 0;
			}
		});
		lay.getChildren().add(btn);
		
		btn = genButton0("回歸原點","arrow-compress-all.png");
		btn.setOnAction(tsk1);
		lay.getChildren().add(btn);
		
		btn = genButton0("定位標靶","selection.png");
		btn.setOnAction(tsk0);
		lay.getChildren().add(btn);
		
		btn = genButton0("掃描程序","play.png");
		btn.setOnAction(tsk2);
		lay.getChildren().add(btn);
		
		final Button btnPan2 = genButton1("晶圓設定","wrench.png");
		btnPan2.setOnAction(event->{
			//TODO:we must re-design this panel!!!!
			/*btnPan2.setDisable(false);
			new PanBase(btnPan2){
				@Override
				public Parent layout() {
					return wmap.getConsole();
				}	
			}.appear();*/
		});
		lay.getChildren().add(btnPan2);
		
		btn = genButton2("關閉程式","close.png");	
		btn.setOnAction(EVENT->Entry.this.dismiss());
		lay.getChildren().add(btn);
		
		return lay;
	}
	
	private Node layoutPerspective(){
		JFXTabPane tabs = new JFXTabPane();
		tabs.setSide(Side.LEFT);
		tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		//tabs.setPrefSize(800,800);
		
		Tab stp1 = new Tab("晶圓");
		stp1.setContent(wmap);		
		Tab stp2 = new Tab("影像");
		//stp2.setContent();
		
		tabs.getTabs().addAll(stp1,stp2);
		return tabs;
	}
	
	@Override
	public Parent layout() {

		HBox layConvenient = new HBox();		
		layConvenient.getChildren().addAll(
			new BoxLogger(100),
			new PanHelpful()		
		);
		
		BorderPane lay0 = new BorderPane();
		lay0.setCenter(layoutPerspective());
		lay0.setRight(layoutAction());
		lay0.setBottom(layConvenient);
		return lay0;
	}
}
