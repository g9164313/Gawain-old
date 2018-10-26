package narl.itrc.vision;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import narl.itrc.ControlWheel;
import narl.itrc.DevBase;
import narl.itrc.Misc;

public class DevPreview extends DevBase {

	private static int ref_counter = 1;
	
	private final static double MARGIN = 13.;
	
	//cursor location
	protected final StringProperty infoCurLoca = new SimpleStringProperty("(  0,  0)");
	//image size
	protected final StringProperty infoImgSize = new SimpleStringProperty(Misc.TXT_UNKNOW);
	//pixel value
	protected final StringProperty infoPixValue= new SimpleStringProperty(Misc.TXT_UNKNOW);
	//scope location
	protected final StringProperty infoScopLoca= new SimpleStringProperty("(  0,  0)");
	//scope size	
	protected final StringProperty infoScopSize= new SimpleStringProperty(Misc.TXT_UNKNOW);
	//magnify rate
	protected final StringProperty infoMagValue= new SimpleStringProperty("1:1");
	//frame per second 
	protected final StringProperty infoFPSValue= new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private WritableImage buff = null;

	private final ImageView view = new ImageView();
	
	private final Canvas over = new Canvas();
	
	private final Pane lay0 = gen_panel_scope();//group view and overlay
	
	private final Pane lay2 = gen_panel_info();
	
	private final Pane lay3 = gen_panel_ctrl();
	
	private final AnchorPane root = new AnchorPane();
	
	public DevPreview() {
		super(String.format("Previewer-%02d", ref_counter++));
		root.setMinSize(320,240);
		root.getChildren().addAll(
			lay0, 
			lay2, lay3
		);		
	}

	public Node getNode(){
		return root;
	}
	
	private Pane gen_panel_scope(){
		Pane lay = new Pane();
		//lay0.getStyleClass().add("group-border-1");
		AnchorPane.setTopAnchor(lay, 0.);
		AnchorPane.setLeftAnchor(lay, 0.);
		AnchorPane.setBottomAnchor(lay, 0.);
		AnchorPane.setRightAnchor(lay, 0.);
		//over.setOnMouseMoved(event->{
		//	int xx = (int)event.getX();
		//	int yy = (int)event.getY();
		//	Misc.logv("mv=(%d,%d)", xx,yy);			
		//});					
		lay.getChildren().addAll(view, over);		
		lay.layoutBoundsProperty().addListener((obser,oldVal,newVal)->{
			int ww = (int)newVal.getWidth();
			int hh = (int)newVal.getHeight();
			if(ww==0 || hh==0){
				return;
			}
			if(view.getViewport()==null){
				//first initialize, set all size and information~~~~
				over.setWidth(ww);
				over.setHeight(hh);			
				view.setFitWidth(ww);
				view.setFitHeight(hh);
				view.setViewport(new Rectangle2D(0,0,ww,hh));
				infoScopSize.set(String.format("%3d x %3d",(int)ww, (int)hh));
			}else{
				change_scope('r', ww, hh);
			}			
		});
		return lay;
	}

	private Pane gen_panel_info(){
		
		final GridPane lay = new GridPane();
		//lay.setVisible(false);//default is invisible
		AnchorPane.setTopAnchor(lay, MARGIN);
		AnchorPane.setLeftAnchor(lay, MARGIN);
		lay.setStyle(
			"-fx-background-color: palegreen; "+
			"-fx-padding: 7;"+
			"-fx-spacing: 7; "+
			"-fx-background-radius: 10; "+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
		);
				
		final Label[] txt = new Label[7];
		for(int i=0; i<txt.length; i++){
			txt[i] = new Label();
		}
		txt[0].textProperty().bind(infoCurLoca);
		txt[1].textProperty().bind(infoImgSize);
		txt[2].textProperty().bind(infoPixValue);
		txt[3].textProperty().bind(infoScopLoca);
		txt[4].textProperty().bind(infoScopSize);
		txt[5].textProperty().bind(infoMagValue);
		txt[6].textProperty().bind(infoFPSValue);
		
		final Button btnZoomIn = new Button("+");
		btnZoomIn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnZoomIn.setOnAction(event->change_scope('m', 1, 0));
		HBox.setHgrow(btnZoomIn, Priority.ALWAYS);
		
		final Button btnZoomFit = new Button("0");
		btnZoomFit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnZoomFit.setOnAction(event->change_scope('m', 0, 0));
		HBox.setHgrow(btnZoomFit, Priority.ALWAYS);
		
		final Button btnZoomOut = new Button("-");
		btnZoomOut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnZoomOut.setOnAction(event->change_scope('m',-1, 0));
		HBox.setHgrow(btnZoomOut, Priority.ALWAYS);
		
		final HBox lay1 = new HBox(); 
		lay1.getChildren().addAll(
			btnZoomIn, 
			btnZoomFit, 
			btnZoomOut
		);
		//GridPane.setFillHeight(lay1, true);

		//txtCursor.textProperty().
		lay.addRow(0, new Label("指標位置"), new Label("："), txt[0]);		
		lay.addRow(1, new Label("影像大小"), new Label("："), txt[1]);
		lay.addRow(2, new Label("像素值"  ), new Label("："), txt[2]);		
		lay.addRow(3, new Label("視野位置"), new Label("："), txt[3]);	
		lay.addRow(4, new Label("視野範圍"), new Label("："), txt[4]);
		lay.addRow(5, new Label("放大倍率"), new Label("："), txt[5]);
		lay.addRow(6, new Label("FPS"     ), new Label("："), txt[6]);
		lay.add(lay1 , 0, 7, 3, 1);
		
		return lay;
	}
	
	private Pane gen_panel_ctrl(){
		
		ControlWheel lay = new ControlWheel(24);
		//lay.setVisible(false);//default is invisible
		AnchorPane.setBottomAnchor(lay, MARGIN);
		AnchorPane.setRightAnchor(lay, MARGIN);
		
		lay.setOnPlayPause(null, null);
		
		lay.setOnArrowUp   (null, null, event->change_scope('y',-10, 0));
		lay.setOnArrowDown (null, null, event->change_scope('y', 10, 0));
		lay.setOnArrowLeft (null, null, event->change_scope('x',-10, 0));
		lay.setOnArrowRight(null, null, event->change_scope('x', 10, 0));
		return lay;
	}
	
	private void change_scope(char dir, int arg1, int arg2){
		final Rectangle2D rect = view.getViewport();
		double xx = rect.getMinX();
		double yy = rect.getMinY();
		double ww = rect.getWidth();
		double hh = rect.getHeight(); 
		double w0, h0;
		switch(dir){
		default:
			return;
		case 'X':
		case 'x':
			xx = xx + arg1;
			infoScopLoca.set(String.format("(%3d,%3d)",(int)xx, (int)yy));
			break;
		case 'Y':
		case 'y':
			yy = yy + arg1;
			infoScopLoca.set(String.format("(%3d,%3d)",(int)xx, (int)yy));
			break;
		case 'M':
		case 'm':
			// Scale the image. Conditions are below:
			// 1. magnify >1 -- scaling bigger.
			// 2. magnify =0 -- same as source.
			// 3. magnify<-1 -- scaling smaller.
			w0 = over.getWidth();
			h0 = over.getHeight();
			if(arg1>=1){
				ww = ww/2;
				hh = hh/2;
				xx = xx + ww/2;
				yy = yy + hh/2;
			}else if(arg1==0){
				ww = w0;
				hh = h0;
			}else{
				xx = xx - ww/2;
				yy = yy - hh/2;
				ww = ww*2;
				hh = hh*2;
			}
			infoScopSize.set(String.format("%3d x %3d",(int)ww, (int)hh));
			if(ww>=w0){
				infoMagValue.set(String.format("%d:1", (int)(ww/w0)));
			}else{
				infoMagValue.set(String.format("1:%d", (int)(w0/ww)));
			}
			break;
		case 'R':
		case 'r':
			//view size has changed, reset scope again!!!
			w0 = over.getWidth();			
			ww = arg1 * (ww/w0);
			hh = arg2 * (ww/w0);
			over.setWidth(arg1);
			over.setHeight(arg2);			
			view.setFitWidth(arg1);
			view.setFitHeight(arg2);
			infoScopSize.set(String.format("%3d x %3d",(int)ww, (int)hh));
			break;
		}
		view.setViewport(new Rectangle2D(xx,yy,ww,hh));		
	}
	
	protected void test_draw(
		final Canvas can, 
		final Color cc
	){
		double ww = can.getWidth();
		double hh = can.getHeight();
        //Misc.logv("image size=%d,%d", (int)ww, (int)hh);
        GraphicsContext gc = can.getGraphicsContext2D();
        gc.clearRect(0, 0, ww, hh);
        gc.setStroke(cc);
        gc.strokeLine(0, 0, ww, hh);
        gc.strokeLine(0, hh, ww, 0);
	}
	
	public void test_load_file(){
		File fs = new File("/home/qq/canvas/5 - 1.jpg");
		try {
			Image img = SwingFXUtils.toFXImage(
				ImageIO.read(new FileInputStream(fs)),
				null
			);			
			buff = new WritableImage(
				img.getPixelReader(),
				(int)img.getWidth(), 
				(int)img.getHeight()
			);			
		} catch (IOException e) {
			e.printStackTrace();
		}
		view.setImage(buff);
	}
	//--------------------------------------//
	
	@Override
	protected boolean looper(TokenBase obj) {
		return false;
	}

	@Override
	protected boolean eventReply(TokenBase obj) {
		return false;
	}

	@Override
	protected void eventLink() {
	}

	@Override
	protected void eventUnlink() {
	}
}
