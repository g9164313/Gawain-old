package prj.scada;

import java.io.File;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PID_Pallet extends PanBase {

	private PID_Root root;
	private EventHandler<? super MouseEvent> event_1 = null;
	private EventHandler<? super MouseEvent> event_2 = null;
	
	public PID_Pallet(PID_Root inst){
		root = inst;
		dia.setInitialDirectory(new File(Gawain.pathSock));		
	}
	
	private class Cursor extends Rectangle{		
		private final int off = 2;
		private int[] size = {
			PID_Const.GRID_SIZE, 
			PID_Const.GRID_SIZE
		};
		private int type;
		private int gx=0, gy=0;//grid index location
		public Cursor(){
			setArcWidth (3);
			setArcHeight(3);
			setWidth (size[0] + off * 2);
			setHeight(size[1] + off * 2);
			setStrokeDashOffset(20);
			getStrokeDashArray().addAll(4.,3.);
			setFill(Color.TRANSPARENT);
			setStroke(Color.DARKCYAN);
		}	
		public void setLoca(double cx, double cy){
			gx = ((int)cx) / PID_Const.GRID_SIZE;
			gy = ((int)cy) / PID_Const.GRID_SIZE;
			setX(gx * PID_Const.GRID_SIZE - off);
			setY(gy * PID_Const.GRID_SIZE - off);
		}
		public void moveUp(){
			int _v = gy - 1;
			if(_v>=0){ 
				setY(_v * size[1] - off);
				gy = _v;
			}			
		}
		public void moveDown(){
			int _v = gy + 1;
			if(_v>=0){ 
				setY(_v * size[1] - off);
				gy = _v;
			}
		}
		public void moveLeft(){
			int _v = gx - 1;
			if(_v>=0){ 
				setX(_v * size[0] - off);
				gx = _v;
			}
		}
		public void moveRight(){
			int _v = gx + 1;
			if(_v>=0){ 
				setX(_v * size[0] - off);
				gx = _v;
			}
		}
		public void setType(int t){
			type = t;
			switch(type){
			default:
				cursor.setStroke(Color.DARKCYAN);
				int[] res = PID_Leaf.getImageSize(type);
				size[0] = res[0];
				size[1] = res[1];
				break;
			case PID_Const.CURSOR_SELECT:
				cursor.setStroke(Color.GREEN);
				size[0] = PID_Const.GRID_SIZE;
				size[1] = PID_Const.GRID_SIZE;
				break;
			case PID_Const.CURSOR_DELETE:
				cursor.setStroke(Color.RED);
				size[0] = PID_Const.GRID_SIZE;
				size[1] = PID_Const.GRID_SIZE;
				break;
			}
			setWidth (size[0] + off * 2);
			setHeight(size[1] + off * 2);
		}
		public void delBrick(){
			root.deleteLeaf(gx, gy);
		}
		public void putBrick(){
			switch(type){
			default:
				root.createLeaf(type, gx, gy);
				break;
			case PID_Const.CURSOR_SELECT:
				break;
			case PID_Const.CURSOR_DELETE:
				root.deleteLeaf(gx, gy);
				break;
			}
		}
	};
	private Cursor cursor = new Cursor();
	
	@Override
	public PanBase appear(){
		
		root.getChildren().add(cursor);
		
		event_1 = root.getOnMouseMoved();
		event_2 = root.getOnMouseClicked();
		
		root.setOnMouseMoved(event->{
			cursor.setLoca(event.getX(), event.getY());
		});
		root.setOnMouseClicked(event->{
			cursor.setLoca(event.getX(), event.getY());
			cursor.putBrick();
		});
		super.appear((Stage)(root.getScene().getWindow()));
		
		//short key for move cursor and place brick-tile.
		getScene().setOnKeyPressed(event->{
			KeyCode kc = event.getCode();
			if(kc==KeyCode.W){
				cursor.moveUp();
			}else if(kc==KeyCode.S){
				cursor.moveDown();
			}else if(kc==KeyCode.A){				
				cursor.moveLeft();
			}else if(kc==KeyCode.D){
				cursor.moveRight();
			}else if(kc==KeyCode.F){
				cursor.putBrick();
			}else if(kc==KeyCode.G){
				cursor.delBrick();
			}
		});
		return this;
	}
	
	@Override
	public void dismiss(){		
		root.getChildren().remove(cursor);
		getScene().setOnKeyPressed(null);		
		root.setOnMouseMoved(event_1);
		root.setOnMouseClicked(event_2);
		super.dismiss();
	}
	
	private final FileChooser dia = new FileChooser();
	private final ToggleGroup grp = new ToggleGroup();
	
	private ToggleButton gen_toggle(final int type){
		ToggleButton btn = new ToggleButton();
		btn.setGraphic(PID_Leaf.getThumb(type));
		btn.setToggleGroup(grp);
		btn.setOnAction(e->cursor.setType(type));
		return btn;
	}
	
	@Override
	public Node eventLayout(PanBase self) {

		final Button btnFun1 = new Button("LF"); 
		btnFun1.setOnAction(e->{
			for(int i=0; i<root.leafSize(); i++){
				PID_Leaf itm = root.selectLeaf(i);
				int[] idx = itm.getGridIndx();
				itm.setGridIndx(idx[0]-1, idx[1]);
			}			
		});
		
		final Button btnFun2 = new Button("RH"); 
		btnFun2.setOnAction(e->{
			for(int i=0; i<root.leafSize(); i++){
				PID_Leaf itm = root.selectLeaf(i);
				int[] idx = itm.getGridIndx();
				itm.setGridIndx(idx[0]+1, idx[1]);
			}			
		});
		
		final ToggleButton[] lst = {
			gen_toggle(PID_Const.CURSOR_SELECT),
			gen_toggle(PID_Const.CURSOR_DELETE),
			
			gen_toggle(PID_Const.Pipe1UP),
			gen_toggle(PID_Const.Pipe1DW),
			gen_toggle(PID_Const.Pipe1LF),
			gen_toggle(PID_Const.Pipe1RH),
			
			gen_toggle(PID_Const.PipeL1a)/* index = 6 */,
			gen_toggle(PID_Const.PipeL2a),
			gen_toggle(PID_Const.PipeL3a),
			gen_toggle(PID_Const.PipeL4a),
			gen_toggle(PID_Const.PipeL1b),
			gen_toggle(PID_Const.PipeL2b)/* index = 11 */,
			gen_toggle(PID_Const.PipeL3b),
			gen_toggle(PID_Const.PipeL4b),			
			gen_toggle(PID_Const.PipeT1),
			gen_toggle(PID_Const.PipeT2),
			gen_toggle(PID_Const.PipeT3),
			gen_toggle(PID_Const.PipeT4),			
			gen_toggle(PID_Const.PipeXX)/* index = 18 */,
			
			gen_toggle(PID_Const.Wall1),
			gen_toggle(PID_Const.Wall2),
			gen_toggle(PID_Const.Wall3),
			gen_toggle(PID_Const.Wall4),
			gen_toggle(PID_Const.Wall5),
			gen_toggle(PID_Const.Wall6),
			gen_toggle(PID_Const.Wall7),
			gen_toggle(PID_Const.Wall8)/* index = 26 */,
			gen_toggle(PID_Const.Join1),
			gen_toggle(PID_Const.Join2),
			gen_toggle(PID_Const.Join3),
			gen_toggle(PID_Const.Join4)/* index = 30 */,
			
			gen_toggle(PID_Const.Gauge),
			gen_toggle(PID_Const.Valve1),
			gen_toggle(PID_Const.Valve2),
			gen_toggle(PID_Const.Pump),
			gen_toggle(PID_Const.Cryo),
			gen_toggle(PID_Const.Strata),
			gen_toggle(PID_Const.Sputer),			
		};
		grp.selectToggle(lst[0]);//default~~~
		
		final Label dummy_1 = new Label();
		final Label dummy_2 = new Label();
				
		final GridPane layList = new GridPane();
		//layList.setStyle("-fx-vgap: 3px; -fx-hgap: 3px;");
		layList.addRow( 0, lst[ 0], lst[ 1], btnFun1, btnFun2);
		layList.addRow( 1, lst[ 2], lst[ 3], lst[ 4], lst[ 5]);
		layList.addRow( 2, lst[ 6], lst[ 7], lst[10], lst[11]);
		layList.addRow( 3, lst[ 9], lst[ 8], lst[13], lst[12]);
		layList.addRow( 4, lst[14], lst[15], lst[16], lst[17]);
		layList.addRow( 5, lst[18]);
		layList.addRow( 6, lst[26], lst[28], lst[19], lst[20]);		
		layList.addRow( 7, lst[25], lst[36], dummy_1, lst[29]);
		layList.addRow( 8, lst[30], lst[37], dummy_2, lst[21]);		
		layList.addRow( 9, lst[24], lst[23], lst[27], lst[22]);
		layList.addRow(10, lst[31], lst[32], lst[33]);
		layList.addRow(11, lst[34], lst[35]);
		
		final Button btnSave = PanBase.genButton2("儲存", "inbox-arrow-down.png");
		btnSave.setMaxWidth(Double.MAX_VALUE);
		btnSave.setOnAction(event->{
			dia.setTitle("儲存 PID");
			root.save(dia.showSaveDialog(getStage()));
		});
		final Button btnLoad = PanBase.genButton2("讀取", "inbox-arrow-up.png");
		btnLoad.setMaxWidth(Double.MAX_VALUE);
		btnLoad.setOnAction(event->{
			dia.setTitle("讀取 PID");
			root.load(dia.showOpenDialog(getStage()));
		});
		
		final HBox lay1 = new HBox();
		HBox.setHgrow(btnSave, Priority.ALWAYS);
		HBox.setHgrow(btnLoad, Priority.ALWAYS);
		
		lay1.setStyle("-fx-spacing: 7;");
		lay1.getChildren().addAll(btnSave, btnLoad);
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay0.getChildren().addAll(layList, lay1);			
		return lay0;
	}
	@Override
	public void eventShown(PanBase self) {
	}
	@Override
	protected void eventClose(PanBase self) {
		root.editMode(false);//exit edit mode
	}
}
