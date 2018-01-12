package narl.itrc;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


public abstract class WidImageViewEx extends WidImageView {
	
	protected IntegerProperty propFPS = new SimpleIntegerProperty(0);	
	//private StringProperty propScale= new SimpleStringProperty();
	private StringProperty propLoca = new SimpleStringProperty(Misc.TXT_UNKNOW);	
	private StringProperty propPixv = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	public WidImageViewEx(){
		this(640,480);
	}
	
	public WidImageViewEx(int width, int height){
		super(width,height);
		 
		ova1.setOnMouseMoved(eventCursorMove);
		
		Pane layCtrl = create_ctrl_pane();
		StackPane.setMargin(layCtrl, new Insets(7,7,7,7));
		StackPane.setAlignment(layCtrl, Pos.CENTER_LEFT);				
		
		getChildren().addAll(root);
		root.toFront();
	}
	//-----------------------------------------//
	
	private int lastMarkID = 1;
	
	protected class Mark extends Rectangle {
		
		public String name;
		public int[] geom = {100, 100, 30, 30};//location and size
		
		public Mark(){
			name = String.format("MARK-%02d", lastMarkID);
			lastMarkID+=1;
			setFill(Color.TRANSPARENT);
			setStroke(Color.CRIMSON);
			setStrokeWidth(3);
			//update_geom();			
		}
		public void update_geom(){
			setX(geom[0]);
			setY(geom[1]);
			setWidth(geom[2]);
			setHeight(geom[3]);			
			AnchorPane.setLeftAnchor(this,(double)geom[0]);
			AnchorPane.setTopAnchor (this,(double)geom[1]);
		}
		@Override
		public String toString(){
			return name;
		}
	};
	
	private ComboBox<Mark> cmbMark = new ComboBox<Mark>();	
	
	//private Cursor old_cursor = null;
	
	//private final int STA_IDLE = 0;
	//private final int STA_MARK_NAIL = 1;
	//private final int STA_MARK_MOVE = 2;
	//private int state = STA_IDLE;
	
	protected GridPane layCtrl = new GridPane();
	
	private Pane create_ctrl_pane(){
		
		Label txtSize = new Label();
		txtSize.textProperty().bind(propSize);
		
		Label txtScale = new Label();
		change_scale(txtScale);
		
		Label txtLoca = new Label();
		txtLoca.textProperty().bind(propLoca);
		
		Label txtPixV = new Label();
		txtPixV.textProperty().bind(propPixv);
		
		Label txtFps = new Label();
		txtFps.textProperty().bind(propFPS.asString());
		
		Button btnPlay = new Button();
		btnPlay.setMaxWidth(Double.MAX_VALUE);		
		btnPlay.setOnAction(e->{
		});

		Button btnZoomIn = new Button("Zoom+");
		btnZoomIn.setMaxWidth(Double.MAX_VALUE);
		btnZoomIn.setOnAction(e->{
			infoGeom[2]++;
			if(infoGeom[2]==0){
				infoGeom[2] = 2;//-1, 0, 1 are all same~~~~
			}
			change_scale(txtScale);
		});

		Button btnZoomOut = new Button("Zoom-");
		btnZoomOut.setMaxWidth(Double.MAX_VALUE);
		btnZoomOut.setOnAction(e->{
			infoGeom[2]--;
			if(infoGeom[2]==0){
				infoGeom[2] = -2;//-1, 0, 1 are all same~~~~
			}
			change_scale(txtScale);
		});
		
		Button btnSnapBuf = new Button("Snap");
		btnSnapBuf.setMaxWidth(Double.MAX_VALUE);
		btnSnapBuf.setOnAction(e->{
			snapData("snap.png");
			//Misc.imwrite("snap.png", getBgraData(), null);
		});
		
		GridPane layInfo = new GridPane();
		layInfo.add(btnPlay   , 0, 0, 3, 1);
		layInfo.add(btnZoomIn , 0, 1, 3, 1);
		layInfo.add(btnZoomOut, 0, 2, 3, 1);
		layInfo.add(btnSnapBuf, 0, 4, 3, 1);		
		layInfo.addRow(5, new Label("尺寸："), txtSize);
		layInfo.addRow(6, new Label("比例："), txtScale);
		layInfo.addRow(7, new Label("位置："), txtLoca);
		layInfo.addRow(8, new Label("像素："), txtPixV);
		layInfo.addRow(9, new Label("FPS:"  ), txtFps);
		layInfo.getChildren().forEach(obj->{
			GridPane.setHgrow(obj, Priority.ALWAYS);
			GridPane.setFillWidth(obj, true);
		});
		
		cmbMark.setMaxWidth(Double.MAX_VALUE);
		//cmbMark.setEditable(true);
		//cmbMark.itemsProperty().bind(lstMark);
		
		final TextField[] lstBoxMark = {
			new TextField(), new TextField(),
			new TextField(), new TextField(),
		};
		for(TextField box:lstBoxMark){
			box.setPrefWidth(53);
		}
		
		final Button btnAddMark = new Button("+");
		btnAddMark.setOnAction(event->{
			Mark itm = new Mark();
			cmbMark.getItems().add(itm);
			cmbMark.getSelectionModel().selectLast();
			
			ova1.getChildren().add(itm);
			
			//old_cursor = Gawain.getMainScene().getCursor();
			Gawain.getMainScene().setCursor(Cursor.CROSSHAIR);
						
			//state = STA_MARK_NAIL;
		});
		final Button btnDelMark = new Button("-");
		btnDelMark.setOnAction(event->{			
			Mark itm = cmbMark.getSelectionModel().getSelectedItem();
			cmbMark.getItems().remove(itm);
			//cmbMark.getSelectionModel().clearSelection();
			
			ova1.getChildren().remove(itm);
			
			for(TextField box:lstBoxMark){
				box.setText("");
			}
		});
		
		HBox layMarkPick = new HBox();
		HBox.setHgrow(cmbMark, Priority.ALWAYS);
		layMarkPick.getChildren().addAll(btnDelMark, cmbMark, btnAddMark);

		GridPane LayMarkInfo = new GridPane();
		LayMarkInfo.addRow(0, new Label("位置："), lstBoxMark[0], lstBoxMark[1]);
		LayMarkInfo.addRow(1, new Label("寬高："), lstBoxMark[2], lstBoxMark[3]);
		
		VBox lay1 = new VBox();
		lay1.setVisible(false);//Hiding control panel is default~~~
		lay1.getChildren().addAll(
			layInfo,
			new Separator(),
			layMarkPick,
			LayMarkInfo,
			layCtrl
		);
		lay1.setMinSize(130, 300);
		lay1.setStyle(
			"-fx-vgap: 7px;"+			
			"-fx-padding: 7px;"+
			"-fx-spacing: 7px;"+
			"-fx-background-radius: 10;"+	
			"-fx-background-color: gainsboro;"+
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"
		);
		AnchorPane.setTopAnchor (lay1, 32.);
		AnchorPane.setLeftAnchor(lay1, 32.);		
		
		return lay1;
	}
	
	//-----------------------------------//
	
	public abstract void eventChangeScale(int scale);
	
	private void change_scale(Label txt){
		if(view==null){
			return;
		}
		if( 2<=infoGeom[2] ){
			txt.setText("+"+infoGeom[2]);
			view.setFitWidth (infoGeom[0]*infoGeom[2]);
			view.setFitHeight(infoGeom[1]*infoGeom[2]);
		}else if(infoGeom[2]==-1 || infoGeom[2]==0 || infoGeom[2]==1){
			txt.setText("x1");
			view.setFitWidth (infoGeom[0]);
			view.setFitHeight(infoGeom[1]);
		}else if( infoGeom[2]<=-2 ){
			txt.setText(""+infoGeom[2]);
			view.setFitWidth (-infoGeom[0]/infoGeom[2]);
			view.setFitHeight(-infoGeom[1]/infoGeom[2]);
		}		
	}
	
	private void update_cursor_value(int locaX, int locaY){
		Image img = view.getImage();
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
	
	/*private void set_prop_scale(){
		if( 2<=infoGeom[2] ){			    
			propScale.setValue(String.format("1→%d",infoGeom[2]));
		}else if( infoGeom[2]<=-2 ){
			propScale.setValue(String.format("%d→1",infoGeom[2]));
		}else{
			propScale.setValue("1→1");
		}
	}*/
	//-----------------------------------//
	
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
}
