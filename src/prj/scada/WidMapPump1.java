package prj.scada;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import narl.itrc.Misc;

/**
 * Create and control P&ID (piping and instrument diagrams)
 * TODO: ready to deprecate this class :-( 
 * @author qq
 *
 */
public class WidMapPump1 extends StackPane {

	private BooleanProperty modeEdit = new SimpleBooleanProperty(false);
	
	public WidMapPump1(){
		
		ScrollPane pan0 = new ScrollPane();
		//pan0.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		//pan0.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		pan0.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pan0.getStyleClass().add("pad-small");
		pan0.setContent(init_grid());
				
		//setPadding(new Insets(17,17,17,17));
		getStyleClass().addAll("pad-medium");
		getChildren().addAll(pan0);		
	}
	
	public WidMapPump1(String xml_name){
		this();		
		loadCell(xml_name);		
	}
	//----------------------------------//
	
	private static final int CELL_SIZE = 60;
	
	private CellView cur_cell;
	
	private class CellView extends Group {
		
		public String hashLoc = null;

		public int t_id[] = {0, 0, 0};
		
		public boolean working = false;
		
		public String name = "";
		
		public EventHandler<ActionEvent> eventHook = null;
		
		private ImageView back = new ImageView();
		private ImageView midd = new ImageView();
		private ImageView frnt = new ImageView();

		public CellView(int gx, int gy){
			
			hashLoc = String.format("%d#%d", gx, gy);
			
			back.setFitWidth(CELL_SIZE);
			back.setFitHeight(CELL_SIZE);
			midd.setFitWidth(CELL_SIZE);
			midd.setFitHeight(CELL_SIZE);
			frnt.setFitWidth(CELL_SIZE);
			frnt.setFitHeight(CELL_SIZE);

			setPickOnBounds(true);
			getChildren().addAll(back,midd,frnt);
			
			setOnMouseEntered(event->{
				cur_cell = this;
				if(modeEdit.get()==true){					
					update_cell_image();
				}else{
				}		
			});
			setOnMouseClicked(event->{
				if(event.getButton()!=MouseButton.PRIMARY){
					return;
				}
				if(modeEdit.get()==true){
					t_id[0] = cur_tid[0];
					t_id[1] = cur_tid[1];
					midd.setImage(imgCell[t_id[0]][t_id[1]][0]);
				}else{
					working = !working;
					if(working==true){
						do_working();
					}else{
						frnt.setImage(null);
					}
					//just launch once~~~ 
					if(eventHook!=null){
						final ActionEvent obj = new ActionEvent(working,null);
						eventHook.handle(obj);
					}
				}
			});
			setOnMouseExited(event->{
				cur_cell = null;
				if(modeEdit.get()==true){					
					midd.setImage(imgCell[t_id[0]][t_id[1]][0]);
					frnt.setImage(null);
				}else{
					
				}				
			});			
		}
		
		public String getHashID(){
			return String.format("%d#%d#%s", t_id[0], t_id[1], name);
		}
		
		public void setHashID(String txt){
			String[] val = txt.split("#");
			t_id[0] = Integer.valueOf(val[0]);
			t_id[1] = Integer.valueOf(val[1]);
			t_id[2] = 0;//always, reset this value~~~
			if(val.length==3){
				name = val[2];
			}
			midd.setImage(imgCell[t_id[0]][t_id[1]][t_id[2]]);
		}
		
		private void do_working(){
			int len = imgCell[t_id[0]][t_id[1]].length;
			t_id[2] = (t_id[2] + 1) % len;
			if(t_id[2]==0){
				t_id[2] = 1;
			}			
			frnt.setImage(imgCell[t_id[0]][t_id[1]][t_id[2]]);
		}		
	};
	
	/**
	 * There are two checking point, mode_change() and write_cell_from()
	 */
	private ArrayList<CellView> lstCell = new ArrayList<CellView>();
	
	public void setCellAction(String name,EventHandler<ActionEvent> action){
		//polling list, find the target~~~
		for(CellView cv:lstCell){
			if(cv.name.equals(name)==true){
				cv.eventHook = action;
				return;
			}
		}
		Misc.loge("No Cell -> %s", name);		
	} 
	
	public void setCellWorking(String name,boolean flag){
		for(CellView cv:lstCell){
			if(cv.name.equals(name)==true){
				cv.working = flag;
				return;
			}
		}
		Misc.loge("No Cell -> %s", name);
	}
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(250),
		event->{
			if(modeEdit.get()==true){
				return;
			}
			//animation for CellView, it is ugly polling.....			
			for(CellView cv:lstCell){
				if(cv.working==false){
					continue;
				}
				cv.do_working();
			}
		}
	));
	//----------------------------------//

	private GridPane grid = new GridPane();
	
	private static final int rows = 13;
	private static final int cols = 13;

	private GridPane init_grid(){
		
		grid.setMinSize(0., 0.);//force to the same size~~~
		grid.setStyle("-fx-background-color:#FFFFFF;");
		
		for(int i=0; i<cols; i++){
			ColumnConstraints col = new ColumnConstraints(CELL_SIZE);
			grid.getColumnConstraints().add(col);
		}
		for(int j=0; j<rows; j++){
			RowConstraints row = new RowConstraints(CELL_SIZE);
			grid.getRowConstraints().add(row);
		}
		for(int j=0; j<rows; j++){
			for(int i=0; i<cols; i++){
				grid.add(new CellView(i,j), i, j);
			}
		}
		mode_change(modeEdit.get());//refresh cell-view
		
		//start to play animation~~~
		watcher.setCycleCount(Timeline.INDEFINITE);
		watcher.play();

		//Why we can`t hook key event for Grid Panel?
		setOnKeyPressed(event->{
			if(Misc.shortcut_edit.match(event)==true){
				mode_change();
				return;
			}
			if(modeEdit.get()==false){
				return;
			}
			if(Misc.shortcut_save.match(event)==true){
				save_cell();
			}else if(Misc.shortcut_load.match(event)==true){
				load_cell();	
			}else{
				KeyCode cc = event.getCode();
				if(cur_cell==null){
					return;
				}
				if(cc==KeyCode.A){				
					prev_cell();
				}else if(cc==KeyCode.D){		
					next_cell();
				}else if(cc==KeyCode.W){
					prev_germ();
				}else if(cc==KeyCode.S){
					next_germ();
				}else if(cc==KeyCode.F){
					final TextInputDialog dia = new TextInputDialog(cur_cell.name);
					dia.setTitle("設定名稱");
					dia.setContentText("名稱：");
					if(dia.showAndWait().isPresent()==true){
						cur_cell.name = dia.getResult();
					}
				}else if(cc==KeyCode.DELETE){
					clear_cell_view();
				}
			}			
		});		
		return grid;
	}

	private void mode_change(){
		boolean flag = modeEdit.get();
		flag = !flag;		
		mode_change(flag);
	}
	
	private void mode_change(boolean flag){
		//grid.setGridLinesVisible(true);
		modeEdit.set(flag);
		lstCell.clear();
		for(Node nd:grid.getChildren()){
			CellView cv = (CellView)nd;
			if(cv.midd.getImage()!=null){
				lstCell.add(cv);
			}
			if(flag==true){
				cv.back.setImage(imgBlueprint);				
			}else{
				cv.back.setImage(null);
			}
			cv.frnt.setImage(null);
		}
	}
	//----------------------------------//
	
	private void save_cell(){
		FileChooser dia = new FileChooser();
		dia.setTitle("儲存為...");
		File fs = dia.showSaveDialog(getScene().getWindow());
		if(fs==null){
			return;
		}
		write_cell_to(fs);
	}

	private void load_cell(){
		FileChooser dia = new FileChooser();
		dia.setTitle("讀取...");
		File fs = dia.showOpenDialog(getScene().getWindow());
		if(fs==null){
			return;
		}
		read_cell_from(fs);
	}	
	
	public void loadCell(String path){
		File fs = new File(path);
		if(fs.exists()==false){
			return;
		}
		read_cell_from(fs);
	}
	
	private void write_cell_to(File fs){
		try {
			Properties prop = new Properties();
			for(Node nd:grid.getChildren()){
				CellView cv = (CellView)nd;
				prop.setProperty(cv.hashLoc, cv.getHashID());
			}
			FileOutputStream stm = new FileOutputStream(fs);
			prop.storeToXML(stm, "P&I diagram");
			stm.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void read_cell_from(File fs){
		try {			
			FileInputStream stm = new FileInputStream(fs);
			Properties prop = new Properties();
			prop.loadFromXML(stm);
			stm.close();
			lstCell.clear();
			for(Node nd:grid.getChildren()){
				CellView cv = (CellView)nd;
				lstCell.add(cv);
				cv.setHashID(prop.getProperty(cv.hashLoc,"0#0"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	//----------------------------------//
	
	private static final String IMG_DIR = "/narl/itrc/res/tile/";
	
	private static final Image imgBlueprint = new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"blueprint.png"));
	
	private static final Image imgCursor = new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"cursor.png"));
	
	private int[] cur_tid = { 1, 0 };
	
	private void prev_cell(){
		cur_tid[0]--;
		if(cur_tid[0]<0){
			cur_tid[0] = imgCell.length - 1;
		}
		cur_tid[1] = 0;//reset this index~~~
		update_cell_image();
	}
	
	private void next_cell(){
		cur_tid[0] = (cur_tid[0]+1)%imgCell.length;
		cur_tid[1] = 0;//reset this index~~~
		update_cell_image();
	}
	
	private void prev_germ(){
		int len = imgCell[cur_tid[0]].length;
		cur_tid[1]--;
		if(cur_tid[1]<0){
			cur_tid[1] = len-1;
		}
		update_cell_image();
	}
	
	private void next_germ(){
		int len = imgCell[cur_tid[0]].length;
		cur_tid[1] = (cur_tid[1]+1)%len;
		update_cell_image();
	}

	private void update_cell_image(){
		if(cur_cell==null){
			cur_cell.frnt.setImage(imgCursor);
			return;
		}
		int i = cur_tid[0];
		int j = cur_tid[1];
		cur_cell.frnt.setImage(imgCell[i][j][0]);
		cur_cell.midd.setImage(null);
	}
	
	private void clear_cell_view(){
		if(cur_cell==null){
			return;
		}
		cur_cell.midd.setImage(null);
		cur_cell.frnt.setImage(null);
		cur_cell.setHashID("0#0");
	}
	
	private static final Image[] p_ani = {
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-1.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-2.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-3.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-4.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-5.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-6.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-7.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-8.png")),
	};
	
	private static final Image[] c_ani = {
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-1.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-2.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-3.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-4.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-5.png")),
		new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-6.png")),
	};
	
	private static final Image[][][] imgCell = {
		{//0：dummy image!!!
			{ null, null, } 
		},
		{//1：pipe 
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-a1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-a2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-b1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-b2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-b3.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-b4.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-c1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-c2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-c3.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pipe-c4.png")), null, },
		},
		{//2：gauge
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gauge.png")), null, },
		},
		{//3：pump 
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-a1.png")), p_ani[0], p_ani[1], p_ani[2], p_ani[3], p_ani[4], p_ani[5], p_ani[6], p_ani[7], },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-a2.png")), p_ani[0], p_ani[1], p_ani[2], p_ani[3], p_ani[4], p_ani[5], p_ani[6], p_ani[7], },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-b.png" )), p_ani[0], p_ani[1], p_ani[2], p_ani[3], p_ani[4], p_ani[5], p_ani[6], p_ani[7], },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"pump-c.png" )), p_ani[0], p_ani[1], p_ani[2], p_ani[3], p_ani[4], p_ani[5], p_ani[6], p_ani[7], },
		},
		{//4：tank
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"tank-a1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"tank-a2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"tank-b1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"tank-b2.png")), null, },
		},
		{//5：valve 
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"valve.png")), new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"valve-1.png")), },
		},
		{//6：wall
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-a1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-a2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-a3.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-a4.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-b1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-b2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-b3.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-b4.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-c1.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-c2.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-c3.png")), null, },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"wall-c4.png")), null, },
		},
		{//7：sputter-gun
			{ 
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a1.png")),
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a1-1.png")),
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a1-2.png")), 
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a1-3.png")), 
			},
			{ 
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a2.png")),
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a2-1.png")),
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a2-2.png")), 
				new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"gun-a2-3.png")), 
			},
		},
		{//8：chuck, or stage
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-a1.png")), c_ani[0], c_ani[1], c_ani[2], c_ani[3], c_ani[4], c_ani[5], },
			{ new Image(WidMapPump1.class.getResourceAsStream(IMG_DIR+"chuck-a2.png")), c_ani[0], c_ani[1], c_ani[2], c_ani[3], c_ani[4], c_ani[5], },
		},
	};

	/*	HBox lay = new HBox();
		lay.setStyle(
			"-fx-background-color: palegreen; "+
			"-fx-padding: 13;"+
			"-fx-spacing: 7; "+
			"-fx-background-radius: 10; "+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
		);
	*/
}
