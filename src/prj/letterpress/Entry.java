package prj.letterpress;

import com.jfoenix.controls.JFXTabPane;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.CamBundle;
import narl.itrc.CamVidcap;
import narl.itrc.DevTTY;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

public class Entry extends PanBase {

	public Entry(){
		//firstAction = FIRST_MAXIMIZED;
	}

	public static CamBundle cam0 = new CamVidcap("0");
	public static CamBundle cam1 = new CamVidcap("1");	
	public static ImgRender rndr = new ImgRender(cam0,cam1);
	
	public static DevB140M stg0 = new DevB140M("/dev/ttyS0,115200,8n1");
	public static DevTTY   stg1 = new DevTTY("/dev/ttyACM0,9600,8n1");//this connect to ATmega controller  
	
	private WidMapWafer wmap = new WidMapWafer();
	private WidAoiViews prvw = new WidAoiViews(rndr);
	
	private TskAction tsk0 = new TskAligment(rndr,Entry.this);
	private TskAction tsk1 = new TskGoHome(stg0,Entry.this);
	private TskAction tsk2 = new TskScanning(stg0,wmap,Entry.this);
	
	/**
	 * this flag means that we don't enable camera (render stage)
	 */
	private boolean camDryRun = false;
	
	/**
	 * this flag means that we don't enable motion stage
	 */
	private boolean stgDryRun = true;

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
	
	/*private Node layoutAction(){
		VBox lay = new VBox();
		lay.getStyleClass().add("vbox-small");
		
		Button btn;
		btn = genButton1("快速執行","run.png");
		btn.setOnAction(new TskDialog(Entry.this){
			@Override
			public int looper(Task<Integer> tsk) {
				logv("working...");
				Misc.delay(100);
				return 0;
			}
		});
		lay.getChildren().add(btn);
		
		btn = genButton1("回歸原點","arrow-compress-all.png");
		btn.setOnAction(tsk1);
		lay.getChildren().add(btn);
		
		btn = genButton1("定位標靶","selection.png");
		btn.setOnAction(tsk0);
		lay.getChildren().add(btn);
		
		btn = genButton1("掃描程序","play.png");
		btn.setOnAction(tsk2);
		lay.getChildren().add(btn);
		
		btn = genButton3("關閉程式","close.png");	
		btn.setOnAction(EVENT->Entry.this.dismiss());
		lay.getChildren().add(btn);
		
		return lay;
	}*/
	
	@Override
	public Parent layout() {
		
		//----perspective view----
		JFXTabPane lay3 = new JFXTabPane();
		lay3.setSide(Side.LEFT);
		lay3.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		Tab stp1 = new Tab("AOI");
		stp1.setContent(prvw);
		Tab stp2 = new Tab("晶圓");
		stp2.setContent(wmap);		
		lay3.getTabs().addAll(stp1,stp2);
		lay3.getSelectionModel().select(0);
		
		//----operation & logger----
		Node nod1 = new BoxLogger(100);
		Node nod2 = new PanHelpful();
		HBox lay2 = new HBox();
		lay2.getChildren().addAll(nod1,nod2);
		HBox.setHgrow(nod1,Priority.ALWAYS);
		HBox.setHgrow(nod2,Priority.ALWAYS);

		//combine them all~~~
		BorderPane lay1 = new BorderPane();
		lay1.setCenter(lay3);
		lay1.setBottom(lay2);
		return lay1;
	}
}
