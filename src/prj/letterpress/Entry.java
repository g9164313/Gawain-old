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
import narl.itrc.CamFlyCapture;
import narl.itrc.DevTTY;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;

public class Entry extends PanBase {

	public Entry(){
		firstAction = FIRST_MAXIMIZED;
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
	
	public WidMapWafer wmap = new WidMapWafer();
	public WidAoiViews prvw = new WidAoiViews(rndr);

	/**
	 * this flag means that we don't enable camera (render stage)
	 */
	private boolean camEnable = false;
	
	/**
	 * this flag means that we don't enable motion stage
	 */
	private boolean stgEnable = false;

	@Override
	protected void eventShown(WindowEvent e){
		if(camEnable==true){
			//cam0.setROI( 546, 651, 800, 800);
			//cam1.setROI(1176, 810, 800, 800);			
			rndr.play();
		}
		if(stgEnable==true){
			//1pps <==> 5um
			stg0.setFactor(200,200,200,200);
			stg0.setTokenBase('A');
			stg0.setRoutine('B','A','D','C');
			stg0.exec("RS\r\n");//this command must be executed independently.
			stg0.exec(
				"SP 10000,10000,10000,10000;"+
			    "AC 10000,10000,10000,10000;"+
				"DC 10000,10000,10000,10000;"+
			    "TP\r\n"
			);
			PanHelpful.enableAOI(true);
		}		
	}
	
	@Override
	protected void eventClose(WindowEvent e){
	}
	
	public static JFXTabPane pager = new JFXTabPane();
	
	@Override
	public Parent layout() {
		
		//----perspective view----
		pager.setSide(Side.LEFT);
		pager.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		Tab stp1 = new Tab("AOI");
		stp1.setContent(prvw);
		Tab stp2 = new Tab("晶圓");
		stp2.setContent(wmap);		
		pager.getTabs().addAll(stp1,stp2);
		pager.getSelectionModel().select(0);
		//pager.getSelectionModel().select(1);
		
		//----operation & logger----
		Node nod1 = new BoxLogger(100);
		Node nod2 = new PanHelpful();
		HBox lay2 = new HBox();
		lay2.getChildren().addAll(nod1,nod2);
		HBox.setHgrow(nod1,Priority.ALWAYS);
		HBox.setHgrow(nod2,Priority.ALWAYS);

		//combine them all~~~
		BorderPane lay1 = new BorderPane();
		lay1.setCenter(pager);
		lay1.setBottom(lay2);
		return lay1;
	}
}
