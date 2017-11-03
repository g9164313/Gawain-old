package narl.itrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

public abstract class WidImageView extends StackPane {

	public WidImageView(){
		init(640,480);
	}
	
	public WidImageView(int width, int height){
		init(width,height);
	}
	
	/**
	 * information for image size, pixel location and value.<p>
	 * Attention!! the coordinate systems of cursor is same as overlay panel!!!
	 */
	private int infoGeom[] = {
		0/* image width */, 
		0/* image height*/,
		1/* image scale */,
		0/* cursor-X */, 
		0/* cursor-Y */, 
		0, 0, 0,/* pixel value - Red, Green, Blue */
		0
	};
	
	private StringProperty propSize = new SimpleStringProperty(Misc.TXT_UNKNOW);
	private StringProperty propScale= new SimpleStringProperty();
	private StringProperty propLoca = new SimpleStringProperty(Misc.TXT_UNKNOW);	
	private StringProperty propPixv = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private ImageView  vew1 = new ImageView();
	private ImageView  vew2 = new ImageView();
	private AnchorPane ova1 = new AnchorPane();
	private ScrollPane lay1 = new ScrollPane();
	private GridPane   lay2 = create_ctrl_pane();

	protected ObservableList<Node> getOverlayList(){
		return ova1.getChildren();
	}
	
	protected Image getOverlayView(){
		return vew2.getImage();
	}
	
	protected int getCursorX(){
		return infoGeom[3];
	}	
	protected int getCursorY(){
		return infoGeom[4];
	}
	
	private EventHandler<MouseEvent> eventCursorMove = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			int locaX = (int)event.getX();
			int locaY = (int)event.getY();
			infoGeom[3] = locaX;
			infoGeom[4] = locaY;
			//check scale~~~
			if( 2<=infoGeom[2] ){			    
				locaX = locaX / infoGeom[2];
				locaY = locaY / infoGeom[2];
			}else if( infoGeom[2]<=-2 ){
				locaX = locaX * -infoGeom[2];
				locaY = locaY * -infoGeom[2];
			}
			//check boundary~~~
			if(locaX>=infoGeom[0]){
				locaX = infoGeom[0] - 1;
			}else if(locaX<0){
				locaX = 0;
			}
			if(locaY>=infoGeom[1]){
				locaY = infoGeom[1] - 1;
			}else if(locaY<0){
				locaY = 0;
			}			
			update_cursor_value(locaX,locaY);
		}		
	};
	//-----------------------------------//
	
	public abstract void eventChangeScale(int scale);
	
	private void change_scale(char tkn){
		if(tkn=='-'){
			infoGeom[2]--;
			if(infoGeom[2]==0){
				infoGeom[2] = -2;//-1, 0, 1 are all same~~~~
			}
		}else if(tkn=='+'){
			infoGeom[2]++;
			if(infoGeom[2]==0){
				infoGeom[2] = 2;//-1, 0, 1 are all same~~~~
			}
		}else{
			return;
		}
		int ww=0,hh=0;
		if( 2<=infoGeom[2] ){
			ww=infoGeom[0]*infoGeom[2];
			hh=infoGeom[1]*infoGeom[2];
		}else if(infoGeom[2]==-1 || infoGeom[2]==0 || infoGeom[2]==1){
			ww=infoGeom[0];
			hh=infoGeom[1];
		}else if( infoGeom[2]<=-2 ){
			ww=-infoGeom[0]/infoGeom[2];
			hh=-infoGeom[1]/infoGeom[2];
		}
		vew1.setFitWidth (ww);
		vew1.setFitHeight(hh);
		vew2.setFitWidth (ww);
		vew2.setFitHeight(hh);
		set_prop_scale();
		eventChangeScale(infoGeom[2]);
	}
	
	private void update_cursor_value(int locaX, int locaY){
		Image img = vew1.getImage();
		if(img==null){
			propLoca.setValue(Misc.TXT_UNKNOW);
			propPixv.setValue(Misc.TXT_UNKNOW);
			return;
		}
		int val = img.getPixelReader().getArgb(locaX,locaY);
		infoGeom[5] = (val&0xFF0000)>>16;
		infoGeom[6] = (val&0x00FF00)>>8;
		infoGeom[7] = (val&0x0000FF);
		propLoca.setValue(String.format(
			"(%d,%d)",
			locaX,locaY
		));
		propPixv.setValue(String.format(
			"%03d %03d %03d",
			infoGeom[5],infoGeom[6],infoGeom[7]
		));
	}
	
	private void set_prop_scale(){
		if( 2<=infoGeom[2] ){			    
			propScale.setValue(String.format("1→%d",infoGeom[2]));
		}else if( infoGeom[2]<=-2 ){
			propScale.setValue(String.format("%d→1",infoGeom[2]));
		}else{
			propScale.setValue("1→1");
		}
	}
	//-----------------------------------//
	
	private Cursor DEF_CURSOR = null;
	
	private void init(int width, int height){
		
		vew1.setSmooth(false);
		vew2.setSmooth(false);
		
		//ova1.setStyle("-fx-border-color: chocolate; -fx-border-width: 4px;");//DEBUG!!!
		ova1.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		ova1.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		ova1.prefWidthProperty().bind(vew1.fitWidthProperty());
		ova1.prefHeightProperty().bind(vew1.fitHeightProperty());
		ova1.setOnMouseEntered(e->{
			DEF_CURSOR = getScene().getCursor();
			getScene().setCursor(Cursor.CROSSHAIR);
		});
		ova1.setOnMouseExited(e->{
			getScene().setCursor(DEF_CURSOR);
		});
		ova1.setOnMouseMoved(eventCursorMove);

		final StackPane grp = new StackPane();
		grp.getChildren().addAll(vew1, vew2, ova1);
		lay1.setContent(grp);
		lay1.setStyle("-fx-background: lightskyblue;");
		lay1.setMinViewportWidth(width);
		lay1.setMinViewportHeight(height);
		lay1.setFitToWidth(true);
		lay1.setFitToHeight(true);
		lay1.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		lay1.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		
		StackPane.setMargin(lay2, new Insets(7,7,7,7));
		StackPane.setAlignment(lay2, Pos.CENTER_LEFT);				
		getChildren().addAll(lay1,lay2);
		
		/*setOnKeyPressed(e->{
			if(hotkeyScaleDown.match(e)==true){				
				change_scale('-');
			}else if(
				hotkeyScaleUp1.match(e)==true ||
				hotkeyScaleUp2.match(e)==true
			){				
				change_scale('+');
			}else if(hotkeyLoadImage.match(e)==true){
				
			}else if(hotkeySaveImage.match(e)==true){
			
			}else if(hotkeySnapImage.match(e)==true){
				//Misc.logv("vvalue=%.1f , %d, %d",
				//	(getVvalue()),
				//	(int)(lay2.getHeight()),
				//	(int)(getHeight())
				//);
			}else{
				return;
			}		
			e.consume();
		});*/
		
		//hvalueProperty().addListener((obs,oldValue,newValue)->{});
		//vvalueProperty().addListener((obs,oldValue,newValue)->{});		
	}
	
	private GridPane create_ctrl_pane(){
		
		final Label txtSize = new Label();
		txtSize.textProperty().bind(propSize);
		
		final Label txtScale=new Label();
		txtScale.textProperty().bind(propScale);
		set_prop_scale();//first initialize~~~
		
		final Label txtLoca = new Label();
		txtLoca.textProperty().bind(propLoca);
		
		final Label txtPixV = new Label();
		txtPixV.textProperty().bind(propPixv);
		
		Button btnZoomIn = new Button("Zoom+");
		btnZoomIn.setMaxWidth(Double.MAX_VALUE);
		btnZoomIn.setOnAction(e->{
			change_scale('+');
		});
		GridPane.setHgrow(btnZoomIn, Priority.ALWAYS);
		GridPane.setFillWidth(btnZoomIn, true);
		
		Button btnZoomOut = new Button("Zoom-");
		btnZoomOut.setMaxWidth(Double.MAX_VALUE);
		btnZoomOut.setOnAction(e->{
			change_scale('-');
		});
		
		Button btnSnapBuf = new Button("Snap");
		btnSnapBuf.setMaxWidth(Double.MAX_VALUE);
		btnSnapBuf.setOnAction(e->{
			Misc.logv("btnSnap");
		});
		
		GridPane lay = new GridPane();				
		lay.add(new Label("尺寸："), 0, 0);lay.add(txtSize , 1, 0);
		lay.add(new Label("比例："), 0, 1);lay.add(txtScale, 1, 1);
		lay.add(new Label("位置："), 0, 2);lay.add(txtLoca , 1, 2);
		lay.add(new Label("像素："), 0, 3);lay.add(txtPixV , 1, 3);	
		lay.add(btnZoomIn      , 0, 4, 4, 1);
		lay.add(btnZoomOut     , 0, 5, 4, 1);
		lay.add(new Separator(), 0, 6, 4, 1);
		
		lay.setStyle(
			"-fx-vgap: 7px;"+
			"-fx-background-color: gainsboro;"+
			"-fx-padding: 7px;"+
			"-fx-spacing: 7px;"+
			"-fx-background-radius: 10;"+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"
		);
		//lay.maxWidthProperty().bind(widthProperty().multiply(0.23));
		lay.setMaxWidth(133);
		lay.maxHeightProperty().bind(heightProperty().multiply(0.76));
		lay.getChildren().forEach(obj->{
			GridPane.setHgrow(obj, Priority.ALWAYS);
			GridPane.setHalignment(obj, HPos.LEFT);
			//GridPane.setFillWidth(obj, true);
		});
		return lay;
	}
	//-----------------------------------//
	
	public void loadImageFile(String name){
		loadImage(new Image("file:"+name));
	}
	public void loadImageFile(File fs){
		if(fs.isFile()==true){
			return;
		}
		FileInputStream stm;
		try {
			stm = new FileInputStream(fs);
			loadImage(new Image(stm));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void loadImage(Image img){
		infoGeom[0] = (int)img.getWidth();
		infoGeom[1] = (int)img.getHeight();
		
		vew1.setImage(img);		
		vew1.setFitWidth (infoGeom[0]);
		vew1.setFitHeight(infoGeom[1]);		
		
		vew2.setImage(new WritableImage(infoGeom[0], infoGeom[1]));
		vew2.setFitWidth (infoGeom[0]);
		vew2.setFitHeight(infoGeom[1]);	

		setFocused(true);
		
		propSize.setValue(String.format(
			"%dx%d",
			infoGeom[0],infoGeom[1]
		));
	}

	/*private static final KeyCombination hotkeyScaleDown= new KeyCodeCombination(KeyCode.MINUS , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeyScaleUp1 = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeyScaleUp2 = new KeyCodeCombination(KeyCode.ADD   , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeyLoadImage= new KeyCodeCombination(KeyCode.L   , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeySaveImage= new KeyCodeCombination(KeyCode.S   , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeySnapImage= new KeyCodeCombination(KeyCode.D   , KeyCombination.CONTROL_ANY);*/
}
