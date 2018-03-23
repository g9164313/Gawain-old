package narl.itrc;

import java.util.HashMap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import prj.scada.FieldNumSlider;

public class WidDiagram extends AnchorPane {

	protected static final int TILE_SIZE = 40;

	private static final int DEFAULT_GRID = 20;
	
	private double width = DEFAULT_GRID*TILE_SIZE;
	private double height= DEFAULT_GRID*TILE_SIZE; 
	
	public WidDiagram(){
		init();
	}
	
	public WidDiagram(int gridWidth, int gridHeight){
		width = gridWidth *TILE_SIZE;
		height= gridHeight*TILE_SIZE;
		init();
	}
	
	private void init(){
		
		loadPumpTile();
		
		watcher.setCycleCount(Timeline.INDEFINITE);
		watcher.play();
		
		//setPickOnBounds(false);
		setPickOnBounds(true);
		setPrefSize(width, height);		

		getStyleClass().add("pad-small");
		//getChildren().addAll(pegged);
	}
	//----------------------------------------//
	
	protected abstract class ItemTile extends StackPane {
		
		protected Image[]  face = null;		
		protected double[] loca = {0.,0.};//x,y coordinate...
		
		protected int indx = 0;//indicate which face~~~~
		
		private Canvas ground = new Canvas();
		private Canvas cursor = new Canvas();
		
		public ItemTile(){
			cursor.setWidth(TILE_SIZE);
			cursor.setHeight(TILE_SIZE);
			init();
		}
		
		public ItemTile(String category, double gx, double gy){
			locate(category,gx,gy);
			init();
		}
		
		public ItemTile locate(String category,double gx, double gy){
			loca[0] = gx * TILE_SIZE;
			loca[1] = gy * TILE_SIZE;
			face = lstFace.get(category);
			double ww = face[0].getWidth();
			double hh = face[0].getHeight();
			setPrefSize(ww, hh);
			ground.setWidth(ww);
			ground.setHeight(hh);			
			cursor.setWidth(ww);
			cursor.setHeight(hh);
			AnchorPane.setLeftAnchor(this, loca[0]);
			AnchorPane.setTopAnchor(this, loca[1]);
			draw(-1);
			return this;
		}
		
		private void init(){
			getChildren().addAll(ground,cursor);			
			cursor.setVisible(false);
		}
		
		public double getGridX(){
			return loca[0] / TILE_SIZE;
		}
		
		public double getGridY(){
			return loca[1] / TILE_SIZE;
		}
		
		protected void prepare_cursor(){
			int stk = 1;
			GraphicsContext dc = cursor.getGraphicsContext2D();
			dc.clearRect(
				0, 0, 
				cursor.getWidth(), 
				cursor.getHeight()
			);
			dc.setFill(Color.TRANSPARENT);
			dc.setStroke(Color.CORNFLOWERBLUE);
			dc.setLineWidth(stk);
			dc.strokeRoundRect(
				stk, stk, 
				cursor.getWidth()-stk*2, 
				cursor.getHeight()-stk*2, 
				TILE_SIZE/5,
				TILE_SIZE/5 
			);
			setOnMouseEntered(e->{
				cursor.setVisible(true);
			});
			setOnMouseExited(e->{
				cursor.setVisible(false);
			});
		}
		
		public ItemTile clear(){
			ground.getGraphicsContext2D().clearRect(
				1., 1., 
				face[0].getWidth()-1.,
				face[0].getHeight()-1.
			);
			return this;
		}
		
		public ItemTile draw(int idx){
			if(0<=idx && idx<face.length){
				indx = idx;
			}			
			ground.getGraphicsContext2D().drawImage(face[indx],0, 0);
			return this;
		}
		
		public ItemTile redraw(){
			clear();
			draw(-1);
			return this;
		}
		
		abstract public void makeup();
	};
	
	protected abstract class ItemToggle extends ItemTile 
		implements EventHandler<MouseEvent>
	{
		public ItemToggle(){
			super();
			setOnMouseClicked(this);
			prepare_cursor();
		}
		
		public ItemToggle(String category, double gx, double gy){
			super();
			locate(category,gx,gy);	
			setOnMouseClicked(this);
			prepare_cursor();
		}
		
		private boolean applyMakeup = false;//remember update this in GUI event
		
		public void applyMakeup(){
			if(applyMakeup==true){
				indx = 0;//stop animation~~~
				applyMakeup = false;
			}else{
				indx = 1;//start animation~~~
				applyMakeup = true;
			}
			redraw();
		}
		
		public void applyMakeup(boolean flag){
			applyMakeup = flag;
			if(applyMakeup==false){
				indx = 0;//stop animation~~~
			}else{
				indx = 1;//start animation~~~
			}
			redraw();
		}
		
		protected boolean isMakeup(){
			return applyMakeup;
		}
		
		@Override
		public void makeup() {
			if(applyMakeup==false){
				return;
			}
			indx++;
			if(indx>=face.length){
				indx = 1;
			}
			redraw();
		}
	};

	protected abstract class ItemSlider extends ItemTile 
		implements EventHandler<MouseEvent>, FieldNumSlider.EventHook
	{
		protected FieldNumSlider field = new FieldNumSlider(this);
		
		public ItemSlider(){
			super();
			setOnMouseClicked(this);
			prepare_cursor();
		}		
		public ItemSlider(
			String category, 
			double gx, double gy
		){
			this(category, gx,gy, gx,gy);
		}
		public ItemSlider(
			String category,
			double gx1, double gy1,
			double gx2, double gy2
		){
			super();
			locate(category,gx1,gy1);
			setOnMouseClicked(this);
			prepare_cursor();
			
			field.txt.setPrefWidth(TILE_SIZE*1.7);
			WidDiagram.this.getChildren().add(field);			
			AnchorPane.setLeftAnchor(field, gx2*TILE_SIZE);
			AnchorPane.setTopAnchor (field, gy2*TILE_SIZE);
		}		

		@Override
		public void handle(MouseEvent event) {
			field.reload();
		}
	}
	
	protected class ItemBrick extends ItemTile {
		public ItemBrick(String category, double gx, double gy){
			locate(category,gx,gy);
		}
		@Override
		public void makeup() {
			//Do nothing, brick don't need GUI event.
			//User must show face manually.
		}
	}
	
	protected abstract class TextSlider extends Label
		implements FieldNumSlider.EventHook
	{
		public FieldNumSlider field = new FieldNumSlider(this);
		
		public TextSlider(double gx, double gy){
			this(gx,gy,gx+1,gy);
		}
		public TextSlider(
			double gx1, double gy1,
			double gx2, double gy2
		){	
			setOnMouseClicked(e->{
				field.reload();
			});
			
			setStyle("-fx-font-size: 19px;");
			AnchorPane.setLeftAnchor(this, gx1*TILE_SIZE);
			AnchorPane.setTopAnchor(this, gy1*TILE_SIZE);
			WidDiagram.this.getChildren().add(this);
			
			field.txt.setPrefWidth(TILE_SIZE*1.7);
			AnchorPane.setLeftAnchor(field, gx2*TILE_SIZE);
			AnchorPane.setTopAnchor (field, gy2*TILE_SIZE);
			WidDiagram.this.getChildren().add(field);
		}
	};
	
	private HashMap<String,ItemTile> lstItm  = new HashMap<String,ItemTile>(); 
	
	protected ItemTile getItem(String name){
		return lstItm.get(name);
	}
	
	protected WidDiagram addItem(String name,ItemTile itm){
		lstItm.put(name, itm);
		//pegged.getChildren().add(itm);
		getChildren().add(itm);
		return this;
	}
	
	protected WidDiagram addItem(String name,String category, double gx, double gy){
		ItemBrick itm = new ItemBrick(category,gx,gy);
		lstItm.put(name, itm);
		//pegged.getChildren().add(itm);
		getChildren().add(itm);
		return this;
	}
	
	protected WidDiagram addBrick(String category, double gx, double gy){
		//pegged.getChildren().add(new ItemBrick(category,gx,gy));
		getChildren().add(new ItemBrick(category,gx,gy));
		return this;
	}
	
	protected Label addLabel(String title,double gx,double gy){
		Label txt = new Label(title);
		addLabel(txt, gx, gy);
		return txt;
	}

	protected Label addLabel(Label txt,double gx,double gy){		
		//txt.setStyle("-fx-font-size: 23px;-fx-border-color: #3375FF;");
		txt.setStyle("-fx-font-size: 19px;");
		AnchorPane.setLeftAnchor(txt, gx*TILE_SIZE);//why???
		AnchorPane.setTopAnchor (txt, gy*TILE_SIZE);
		getChildren().add(txt);
		return txt;
	}
	
	protected void bringup_slider(){
		int cnt = getChildren().size();
		for(int i=0; i<cnt; i++){			
			Node nd = getChildren().get(i);
			FieldNumSlider fd = null;
			if((nd instanceof ItemSlider)==true){
				fd = ((ItemSlider)nd).field;
			}else if((nd instanceof TextSlider)==true){
				fd = ((TextSlider)nd).field;
			}			
			if(fd!=null){
				//Then,Z-order will be top!!!
				getChildren().remove(fd);			
				getChildren().add(fd);
			}			
		}		
	}

	protected void redrawAll(){
		lstItm.forEach((name,obj)->obj.redraw());
	}
	
	private final Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(200),
		event->{
			lstItm.forEach((name,obj)->obj.makeup());
		}
	));
	//----------------------------------------//
	
	protected HashMap<String,Image[]> lstFace  = new HashMap<String,Image[]>();

	protected static final String CATE_WALL_A = "wall_1";
	protected static final String CATE_WALL_B = "wall_2";
	protected static final String CATE_WALL_C = "wall_3";
	protected static final String CATE_WALL_D = "wall_4";
	protected static final String CATE_WALL_E = "wall_5";
	protected static final String CATE_WALL_F = "wall_6";
	protected static final String CATE_WALL_G = "wall_7";
	protected static final String CATE_WALL_H = "wall_8";
	
	
	protected static final String CATE_CHUCKA   = "chuck_a";
	protected static final String CATE_CHUCKB   = "chuck_b";
	protected static final String CATE_BURNER_A = "burner-dc-a";
	protected static final String CATE_BURNER_C = "burner-rf-c";
	protected static final String CATE_BAFFLE_A = "baffle_a";
	protected static final String CATE_BAFFLE_B = "baffle_b";	
	protected static final String CATE_BATTLE   = "battle";
	protected static final String CATE_ICE      = "ice";	
	protected static final String CATE_TOWER    = "tower-lamp";
	
	protected static final String CATE_PIPE_A = "pipe2_1";
	protected static final String CATE_PIPE_B = "pipe2_2";
	protected static final String CATE_PIPE_C = "pipe2_3";
	protected static final String CATE_PIPE_D = "pipe2_4";
	protected static final String CATE_PIPE_E = "pipe2_5";
	protected static final String CATE_PIPE_F = "pipe2_6";
	protected static final String CATE_PIPE_G = "pipe3_1";
	protected static final String CATE_PIPE_H = "pipe3_2";
	protected static final String CATE_PIPE_I = "pipe3_3";
	protected static final String CATE_PIPE_J = "pipe3_4";
	protected static final String CATE_PIPE_K = "pipe4";
	
	protected static final String CATE_VALVE   = "valve_manual";
	protected static final String CATE_VALVE_BL= "valve_ball";
	
	protected static final String CATE_GAUGE   = "gauge";
	
	protected static final String CATE_M_PUMP  = "mp";
	protected static final String CATE_C_PUMP  = "cp";
	
	private static final String path = "/narl/itrc/res/tile/";
	
	protected void loadPumpTile(){
		
		lstFace.clear();
		
		final String[][] cate = {
			
			{CATE_CHUCKA , "chuck-a1", "chuck-a2", "chuck-a3", "chuck-a4", "chuck-a5", "chuck-a6" },
			{CATE_CHUCKB , "chuck-b1", "chuck-b2", "chuck-b3", "chuck-b4", "chuck-b5", "chuck-b6" },
			//--------------//
			{CATE_BURNER_A,"burner-a1", "burner-a2", "burner-a3", "burner-a4", },
			{CATE_BURNER_C,"burner-c1", "burner-c2", "burner-c3", "burner-c4", },
			//--------------//
			{CATE_BAFFLE_A,"baffle-a1", "baffle-a2" },
			{CATE_BAFFLE_B,"baffle-b1", "baffle-b2" },
			//--------------//
			{CATE_ICE     ,"ice"},
			//--------------//
			{CATE_BATTLE , "battle"},
			//--------------//
			{CATE_TOWER  , "tower-1", "tower-2", "tower-3", "tower-4"},
			//--------------//
			{CATE_WALL_A , "wall-a"},
			{CATE_WALL_B , "wall-b"},
			{CATE_WALL_C , "wall-c"},
			{CATE_WALL_D , "wall-d"},
			{CATE_WALL_E , "wall-e"},
			{CATE_WALL_F , "wall-f"},
			{CATE_WALL_G , "wall-g"},
			{CATE_WALL_H , "wall-h"},
			//--------------//			
			{CATE_PIPE_A, "pipe-a0", "pipe-a1"},
			{CATE_PIPE_B, "pipe-b0", "pipe-b1"},
			{CATE_PIPE_C, "pipe-c0", "pipe-c1"},
			{CATE_PIPE_D, "pipe-d0", "pipe-d1"},
			{CATE_PIPE_E, "pipe-e0", "pipe-e1"},
			{CATE_PIPE_F, "pipe-f0", "pipe-f1"},
			//--------------//
			{CATE_PIPE_G, "pipe-g0", "pipe-g1"},
			{CATE_PIPE_H, "pipe-h0", "pipe-h1"},
			{CATE_PIPE_I, "pipe-i0", "pipe-i1"},
			{CATE_PIPE_J, "pipe-j0", "pipe-j1"},
			{CATE_PIPE_K, "pipe-k0", "pipe-k1"},
			//--------------//
			{CATE_VALVE   , "valve-0", "valve-1"},
			{CATE_VALVE_BL, "valve-bl-0", "valve-bl-1"},
			{CATE_GAUGE   , "gauge" },
			//--------------//
			{CATE_M_PUMP , "MP-0", 
				           "MP-1", "MP-2", "MP-3", "MP-4",
				           "MP-5", "MP-6", "MP-7", "MP-8", 
			},
			{CATE_C_PUMP , "CP-0", 
		                   "CP-1", "CP-2", "CP-3", "CP-4",
		                   "CP-5", "CP-6", "CP-7", "CP-8", 
			},
		};
			
		for(int i=0; i<cate.length; i++){
			int cnt = cate[i].length - 1;
			Image[] imgs = new Image[cnt];
			for(int j=1; j<=cnt; j++){
				imgs[j-1] = new Image(WidDiagram.class.getResourceAsStream(path+cate[i][j]+".png"));
			}
			lstFace.put(cate[i][0],imgs);
		}		
	}
	//----------------------------------------//
}
