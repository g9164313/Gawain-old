package prj.letterpress;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.DevTTY;
import narl.itrc.PanBase;
import narl.itrc.vision.CamFlyCapture;
import narl.itrc.vision.ImgRender;

public class Entry extends PanBase {

	public Entry(){
		firstAction = FIRST_MAXIMIZED;
		customCSS = Entry.class.getResource("style.css");
		inst = this;
	}

	public static Entry inst = null;
	
	//public static CamVidcap cam0 = new CamVidcap("0");
	//public static CamVidcap cam1 = new CamVidcap("1");
	public static CamFlyCapture cam0 = new CamFlyCapture("s:16025855");
	public static CamFlyCapture cam1 = new CamFlyCapture("s:16138125");
	public static ImgRender rndr = new ImgRender(cam0,cam1);
	
	public static DevB140M stg0 = new DevB140M("/dev/ttyS0,115200,8n1");
	public static DevTTY   stg1 = new DevTTY("/dev/ttyS1,9600,8n1");//this connect to light controller
	public static DevTTY   stg2 = new DevTTY("/dev/ttyACM0,9600,8n1");//this connect to ATmega controller  
		
	/**
	 * this flag means that we don't enable camera (render stage)
	 */
	private boolean camEnable = true;
	
	/**
	 * this flag means that we don't enable motion stage
	 */
	private boolean stgEnable = true;

	public static void resetDefSpeed(){
		stg0.exec(
			"SP 20000,20000,10000,10000;"+
			"AC 10000,10000,10000,10000;"+
			"DC 10000,10000,10000,10000;"+
			"TP\r\n"
		);
	}
	
	@Override
	protected void eventShown(WindowEvent e){
		if(camEnable==true){
			//cam0.setROI( 546, 651, 800, 800);
			//cam1.setROI(1176, 810, 800, 800);			
			rndr.play();
		}
		if(stgEnable==true){
			//1pps <==> 5um
			stg0.setFactor(100,100,100,100);
			stg0.setTokenBase('A');
			stg0.setRoutine('B','A','D','C');
			stg0.exec("RS\r\n");//this command must be executed independently.
			resetDefSpeed();
			PanOption.enableAOI(true);
		}		
	}
	
	@Override
	protected void eventClose(WindowEvent e){
		if(stgEnable==true){
			PanOption.enableAOI(false);
		}
	}
	public WidMapWafer wmap;
	public WidAoiViews prvw;
	public TabPane pager;
	
	@Override
	public Node eventLayout(PanBase pan) {
		wmap = new WidMapWafer();
		prvw = new WidAoiViews(rndr);
		
		//----perspective view----
		pager = new TabPane();
		pager.setSide(Side.LEFT);
		pager.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		Tab stp1 = new Tab("AOI");
		stp1.setContent(prvw);
		Tab stp2 = new Tab("晶圓");
		stp2.setContent(wmap);		
		pager.getTabs().addAll(stp1,stp2);
		pager.getSelectionModel().select(0);
		//pager.getSelectionModel().select(1);
		
		//---main function and shortcut---//
		Node nod1 = new BoxLogger(100);		
		Node nod2 = new PanMotion1();
		Node nod3 = new PanOption();
		Node nod4 = new PanHelpful();
		
		HBox lay2 = new HBox();
		lay2.getStyleClass().add("hbox-medium");
		lay2.getChildren().addAll(nod1,nod2,nod3,nod4);
		HBox.setHgrow(nod1,Priority.ALWAYS);
		HBox.setHgrow(nod2,Priority.ALWAYS);
		HBox.setHgrow(nod3,Priority.ALWAYS);
		HBox.setHgrow(nod4,Priority.ALWAYS);
		
		//combine them all~~~
		BorderPane lay1 = new BorderPane();
		lay1.setCenter(pager);		
		lay1.setBottom(lay2);
		return lay1;
	}
}
