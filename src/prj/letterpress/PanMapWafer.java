package prj.letterpress;

import com.jfoenix.controls.JFXComboBox;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import narl.itrc.BoxPhyValue;
import narl.itrc.Misc;
import narl.itrc.PanDecorate;

public class PanMapWafer extends PanDecorate {
	
	private static final SnapshotParameters sp = new SnapshotParameters();
	
	/**
	 * Draw a map to show the location of cursor or field.<p>
	 * It also decides how to operate motor(?).<p>
	 */
	public PanMapWafer(){
		super("配置圖");
		sp.setFill(Color.TRANSPARENT);
		map_calculate();
		map_draw_ground();
	}
	private Canvas map;//Remember,parent will create this object!!!
	@Override
	public Node layoutBody() {
		map = new Canvas();
		map.setOnMouseClicked(eventMapping);
		map.setOnMouseMoved(eventMapping);
		ScrollPane root = new ScrollPane();
		root.setContent(map);
		return root;
	}
	
	private EventHandler<MouseEvent> eventMapping = new EventHandler<MouseEvent>(){		
		private void pos2cell(double mx,double my){
			int dia = wafBound*2;
			int px = (int)mx+cellSize[0]/2-MAP_OFFSET;
			if(px<0){
				px = 0;
			}else if(px>dia){
				px = dia;
			}
			int py = (int)my+cellSize[0]/2-MAP_OFFSET;
			if(py<0){
				py = 0;
			}else if(py>dia){
				py = dia;
			}
			int ii = px/cellSize[0];
			if(ii>=gridSize[0]){
				ii = gridSize[0] - 1;
			}
			int jj = py/cellSize[1];
			if(jj>=gridSize[1]){
				jj = gridSize[1] - 1;
			}

			curCell = cellArray[jj][ii];
			if(preCell!=null){
				//clear previous drawing~~~
				preCell.clean(map.getGraphicsContext2D());
			}
			curCell.drawing(
				map.getGraphicsContext2D(),
				clrFrontSide
			);
			preCell = curCell;
		}
		private CellInfo preCell = null;
		private CellInfo curCell = null;
		@Override
		public void handle(MouseEvent event) {
			EventType<?> typ = event.getEventType();
			//Misc.logv("ggyy="+mx+","+my);
			if(typ==MouseEvent.MOUSE_CLICKED){
				
			}else if(typ==MouseEvent.MOUSE_MOVED){
				pos2cell(
					event.getX(),
					event.getY()
				);
				txtInfo.setText(String.format("%d",curCell.idx));
			}else if(typ==MouseEvent.MOUSE_ENTERED){
				PanMapWafer.this.getScene().setCursor(Cursor.CROSSHAIR);
			}else if(typ==MouseEvent.MOUSE_EXITED){
				preCell.clean(map.getGraphicsContext2D());
				preCell = null;//reset for next turn~~~
				PanMapWafer.this.getScene().setCursor(Cursor.DEFAULT);
			}
		}
	};

	class CellInfo{
		/**
		 * Left-Top vertex.<p>
		 */
		public int[] vtx = {0,0};
		/**
		 * Position in canvas widget, unit is pixel.<p>
		 * It means the center of cell.<p>
		 */
		public int[] pos = {0,0};
		/**
		 * A special location. Motor Stepper uses this information to move stage.<p>
		 */
		public int[] pin = {0,0};
		/**
		 * this number indicate the sequence index of "valid" cell in wafer.<p>
		 * The word, "valid" means that a complete cell can be contained in wafer
		 */
		public int idx = 0;		
		public CellInfo(){			
		}
		public CellInfo(int x,int y){
			this(x,y,0);
		}
		public CellInfo(int x,int y,int index){
			pos[0] = x;
			pos[1] = y;			
			vtx[0] = x - cellSize[0]/2;
			vtx[1] = y - cellSize[1]/2;
			idx = index;
		}		
		public boolean isValid(int dia){
			//test each vertex
			int ww = cellSize[0]/2;
			int hh = cellSize[1]/2;
			int vx,vy;
			//left-top
			vx = pos[0] - ww;
			vy = pos[1] - hh;
			if(checkRadius(vx,vy,dia)==false){
				return false;
			}
			//right-top
			vx = pos[0] + ww;
			vy = pos[1] - hh;
			if(checkRadius(vx,vy,dia)==false){
				return false;
			}
			//right-bottom
			vx = pos[0] + ww;
			vy = pos[1] + hh;
			if(checkRadius(vx,vy,dia)==false){
				return false;
			}
			//left-bottom
			vx = pos[0] - ww;
			vy = pos[1] + hh;
			if(checkRadius(vx,vy,dia)==false){
				return false;
			}
			return true;
		}
		private boolean checkRadius(int vx,int vy,int dia){
			vx = vx - dia;
			vy = vy - dia;
			int dd = (int)Math.sqrt(vx*vx+vy*vy);
			if(dd>dia){
				return false;
			}
			return true;
		}
		public Color clrState = clrBackSide;
		public void drawing(GraphicsContext gc,Color clr){
			gc.setFill(clr);
			gc.fillRect(
				vtx[0], vtx[1], 
				cellSize[0], cellSize[1]
			);
			gc.setStroke(Color.BLACK);
			gc.setLineWidth(1.);
			gc.strokeRect(
				vtx[0]+1, vtx[1]+1, 
				cellSize[0]-2, cellSize[1]-2
			);//why do we need a offset???
		}
		public void clean(GraphicsContext gc){
			gc.clearRect(
				vtx[0]+1, vtx[1]+1,
				cellSize[0]-1, cellSize[1]-1
			);//why do we need a offset???
			gc.drawImage(
				imgBack, 
				vtx[0]+MAP_OFFSET, vtx[1]+MAP_OFFSET, 
				cellSize[0], cellSize[1], 
				vtx[0], vtx[1], 
				cellSize[0], cellSize[1]
			);
		}
	};
	
	private final int MAP_OFFSET = 7;//unit is pixel
	private final int MIN_SCALE = 10;
	private final int MAX_SCALE = 30;
	
	private Label txtInfo = new Label("--------");
	private Label txtScale = new Label("??? mm/px");
	private BoxPhyValue boxDieW = new BoxPhyValue("寬").setType("mm").setValue("100mil");	
	private BoxPhyValue boxDieH= new BoxPhyValue("高").setType("mm").setValue("100mil");

	/**
	 * unit is millimeter, default is 8inch
	 */
	private double wafDiameter = 4*25.4;
	/**
	 * unit is millimeter, it means the size of grain in wafer
	 */
	private double[] dieSize = {0.,0.};
	/**
	 * how many cell in this grid
	 */
	private int[] gridSize = {0,0};
	/**
	 * cell dimension, unit is pixel
	 */
	private int[] cellSize = {0,0};
	/**
	 * how to convert pixel size into millimeter, 10 is minimum value
	 */
	private int   cellScale= 20;
	private CellInfo[][] cellArray = null;
	/**
	 * the center of circle.It is also the half of diameter(Radius!!!).
	 */
	private int   wafBound = 1;

	private void map_calculate(){
		dieSize[0] = boxDieW.getValue();
		dieSize[1] = boxDieH.getValue();
		gridSize[0] = (int)Math.ceil(wafDiameter/dieSize[0]);
		gridSize[1] = (int)Math.ceil(wafDiameter/dieSize[1]);
		//fit die ratio
		float ratio = 1.f;
		if(dieSize[0]>=dieSize[1]){
			ratio = Math.round(dieSize[0]/dieSize[1]);
			cellSize[0] = cellScale*Math.round(ratio);
			cellSize[1] = cellScale;
			ratio = (float)dieSize[1]/cellScale;
		}else{
			ratio = Math.round(dieSize[1]/dieSize[0]);
			cellSize[0] = cellScale;
			cellSize[1] = cellScale*Math.round(ratio);	
			ratio = (float)dieSize[0]/cellScale;			
		}

		txtScale.setText(String.format("%.2f mm/pix",ratio));
		wafBound = Math.round((float)wafDiameter/ratio);
		map.setWidth(wafBound+2*MAP_OFFSET);
		map.setHeight(wafBound+2*MAP_OFFSET);		
		map.getGraphicsContext2D().translate(MAP_OFFSET,MAP_OFFSET);
		//when map is resized, we must hook again~~~
		map.setOnMouseEntered(eventMapping);
		map.setOnMouseClicked(eventMapping);
		map.setOnMouseMoved(eventMapping);
		map.setOnMouseExited(eventMapping);
		wafBound = wafBound / 2;
		
		int idx = 0;
		int cx0 = wafBound - cellSize[0]*gridSize[0]/2;
		int ccx = cx0;
		int ccy = wafBound - cellSize[1]*gridSize[1]/2;		
		cellArray = new CellInfo[gridSize[1]][];
		for(int j=0; j<cellArray.length; j++){
			cellArray[j] = new CellInfo[gridSize[0]];
			for(int i=0; i<cellArray[j].length; i++){
				CellInfo cf = new CellInfo(ccx,ccy);
				cf.pin[0] = i - gridSize[0]/2;
				cf.pin[1] = j - gridSize[1]/2;
				if(cf.isValid(wafBound)==true){
					cf.idx = (++idx);
				}				
				cellArray[j][i] = cf;				
				ccx = ccx + cellSize[0];
			}
			ccy = ccy + cellSize[1];
			ccx = cx0;//reset row index~~~
		}		
		//imgGround = new WritableImage(
		//	cellSize[0]*gridSize[0]+2*MAP_PADDING,
		//	cellSize[1]*gridSize[1]+2*MAP_PADDING
		//);
	}
	
	private static final Color clrBackSide = Color.web("#b0bec5");
	private static final Color clrFrontSide= Color.web("#ffeb3b");
	private static final Color clrWorking = Color.web("#3f51b5");
	
	private WritableImage imgBack = null;

	private void map_draw_ground(){
		
		
		GraphicsContext gc = map.getGraphicsContext2D();
		//draw the shape of wafer
		gc.setStroke(Color.BLACK);
		//gc.setFill(Color.valueOf(value));
		gc.setLineWidth(1.);
		gc.strokeArc(
			0, 0, 
			wafBound*2,wafBound*2,
			0, 360, 
			ArcType.CHORD
		);
		//draw each cell
		for(int j=0; j<cellArray.length; j++){
			for(int i=0; i<cellArray[j].length; i++){
				CellInfo cf = cellArray[j][i];
				if(cf.idx<=0){
					continue;
				}
				cf.drawing(gc,cf.clrState);
			}
		}
		//draw scan line~~
		//TODO:
		
		imgBack = map.snapshot(sp,null);
	}

	private int diameter2index(){
		int idx = Math.round((float)(wafDiameter/25.4));
		idx = idx - 4;
		if(idx<0){ return 0; }
		if(idx>8){ return 8; }
		return idx;
	}
	
	private void index2diameter(int idx){
		wafDiameter = (idx+4)*25.4;//unit is millimeter
	}
		
	private GridPane con = null;
	public Pane getConsole(){
		if(con!=null){			
			return null;//we just create one console
		}
		final double CHK_SIZE = 110.;

		JFXComboBox<String> chkWType = new JFXComboBox<String>();
		chkWType.setPrefWidth(CHK_SIZE);
		chkWType.getItems().addAll(
			"4''晶圓",
			"5''晶圓",
			"6''晶圓",
			"7''晶圓",
			"8''晶圓",
			"9''晶圓",
			"10''晶圓",
			"11''晶圓",
			"12''晶圓"
		);
		chkWType.getSelectionModel().select(diameter2index());
		chkWType.setOnAction(EVENT->{
			index2diameter(chkWType.getSelectionModel().getSelectedIndex());
		});

		JFXComboBox<String> chkMethod = new JFXComboBox<String>();
		chkMethod.setPrefWidth(CHK_SIZE);
		chkMethod.getItems().addAll(
			"method-1",
			"method-2"
		);
		chkMethod.getSelectionModel().select(0);
		chkMethod.setOnAction(EVENT->{
			//TODO: refresh canvas
		});
		
		con = new GridPane();
		con.getStyleClass().add("grid-small");
		con.addRow(0,new Label("掃描方式"),new Label("："),chkMethod);
		con.addRow(1,new Label("晶圓大小"),new Label("："),chkWType);		
		con.addRow(2,new Label("顆粒寬")  ,new Label("："),boxDieW);
		con.addRow(3,new Label("顆粒高")  ,new Label("："),boxDieH);
		con.addRow(4,new Label("比例尺")  ,new Label("："),txtScale);
		con.add(txtInfo, 0, 5, 4, 1);
		
		return PanDecorate.group("配置圖設定",con);
	}	
}
