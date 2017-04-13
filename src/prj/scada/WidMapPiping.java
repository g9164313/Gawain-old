package prj.scada;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import narl.itrc.Misc;

/**
 * Create and control P&ID (piping and instrument diagrams) 
 * @author qq
 *
 */
public class WidMapPiping extends StackPane {

	private BooleanProperty modeEdit = new SimpleBooleanProperty(true);
	
	public WidMapPiping(String full_name){
		
		path_cell_file = full_name;

		ScrollPane pan0 = new ScrollPane();
		pan0.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		pan0.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		pan0.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pan0.setContent(init_grid());		
		
		setPadding(new Insets(17,17,17,17));
		getChildren().addAll(pan0);
	}
	
	public WidMapPiping(){		
		this("");		
	}
		
	private static final int CELL_SIZE = 60;

	private class CellView extends Group{
		
		private int[] loc = {0, 0};
		private int[] tid = {3, 0};
		
		private ImageView back = new ImageView();
		private ImageView midd = new ImageView();
		private ImageView frnt = new ImageView();

		public CellView(int gx, int gy){

			loc[0] = gx;
			loc[1] = gy;
			
			back.setFitWidth(CELL_SIZE);
			back.setFitHeight(CELL_SIZE);
			
			midd.setFitWidth(CELL_SIZE);
			midd.setFitHeight(CELL_SIZE);
			
			frnt.setFitWidth(CELL_SIZE);
			frnt.setFitHeight(CELL_SIZE);
			
			editMode();
			
			setOnMouseEntered(event->{
				if(modeEdit.get()==true){
					cursor = this;					
					frnt.setImage(imgCursor);					
				}else{
					cursor = null;
				}				
			});
			setOnMouseClicked(event->{
				if(event.getButton()!=MouseButton.PRIMARY){
					return;
				}
				
			});
			setOnMouseExited(event->{
				cursor = null;
				if(modeEdit.get()==true){
					frnt.setImage(null);
				}				
			});
			setPickOnBounds(true);
			getChildren().addAll(back,midd,frnt);
		}
		
		public String getHashLoc(){
			return String.format("%d,%d", loc[0],loc[1]);
		}
		
		public void setHashLoc(String txt){
			set_hash(",", loc, txt);
		}
		
		public String getHashTID(){
			return String.format("%d#%d", tid[0],tid[1]); 
		}
		
		public void setHashTID(String txt){
			set_hash("#", tid, txt);
		}
		
		private void set_hash(String tkn, int[] dat, String txt){
			String[] val = txt.split(tkn);
			dat[0] = Integer.valueOf(val[0]);
			dat[1] = Integer.valueOf(val[1]);
		}
		
		private void prev(){
			if(modeEdit.get()==false){
				return;
			}
			/*t_cat--;
			if(t_cat<0){
				t_cat = tile.length - 1;
			}
			t_dir = 0;//reset this variable
			midd.setImage(tile[t_cat][t_dir]);*/
		}
		
		private void next(){
			if(modeEdit.get()==false){
				return;
			}
			/*t_cat = (t_cat + 1) % tile.length;
			t_dir = 0;//reset this variable
			midd.setImage(tile[t_cat][t_dir]);*/
		}
		
		private void editMode(){
			if(modeEdit.get()==false){
				back.setImage(null);
			}else{
				back.setImage(imgBlueprint);
			}
		}
		
		private void clear(){
			if(modeEdit.get()==false){
				return;
			}
			//t_cat = t_dir = 0;//reset this variable
			//midd.setImage(null);
		}
		
		private void rotate(){
			if(modeEdit.get()==false){
				return;
			}
			//t_dir = (t_dir+1) % tile[t_cat].length;
			//midd.setImage(tile[t_cat][t_dir]);
		}
	};

	private CellView cursor;
	private GridPane grid = new GridPane();
	
	private static final int rows = 13;
	private static final int cols = 13;

	private GridPane init_grid(){
		
		grid.setMinSize(0., 0.);//force to the same size~~~
		//grid.setStyle("-fx-background-color:#217acc;");
		
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
		if(path_cell_file.length()!=0){
			File fs = new File(path_cell_file);
			if(fs.exists()==true){
				get_cell(fs);//load object from file
			}			
		}
		
		//sadly, we can`t set key event for Grid Panel
		setOnKeyPressed(event->{
			if(Misc.shortcut_save.match(event)==true){
				save_cell();
			}else if(Misc.shortcut_load.match(event)==true){
				load_cell();
			}else if(Misc.shortcut_edit.match(event)==true){
				edit_grid();
			}else{
				KeyCode cc = event.getCode();
				if(cc==KeyCode.Q){				
					cursor.prev();
				}else if(cc==KeyCode.E){		
					cursor.next();
				}else if(cc==KeyCode.W || cc==KeyCode.DELETE){
					cursor.clear();
				}else if(cc==KeyCode.R){
					cursor.rotate();
				}
			}			
		});		
		return grid;
	}

	private void edit_grid(){
		//grid.setGridLinesVisible(true);
		boolean flag = modeEdit.get();
		flag = !flag;
		modeEdit.set(flag);
		for(int j=0; j<rows; j++){
			for(int i=0; i<cols; i++){
				//cell[j][i].editMode();
			}
		}
	}

	private String path_cell_file = "";//full path name of grid file
	
	private void save_cell(){
		FileChooser dia = new FileChooser();
		dia.setTitle("儲存為...");
		if(path_cell_file.length()!=0){
			dia.setInitialFileName(path_cell_file);
		}
		File fs = dia.showSaveDialog(getScene().getWindow());
		if(fs==null){
			return;
		}
		put_cell(fs);
	}
	
	private void put_cell(File fs){
		try {
			Properties prop = new Properties();
			for(Node node:grid.getChildren()){
				CellView cell = (CellView)node;				
				prop.setProperty(
					cell.getHashLoc(), 
					cell.getHashTID()
				);
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
	
	private void load_cell(){
		FileChooser dia = new FileChooser();
		dia.setTitle("讀取...");
		if(path_cell_file.length()!=0){
			dia.setInitialFileName(path_cell_file);
		}
		File fs = dia.showOpenDialog(getScene().getWindow());
		if(fs==null){
			return;
		}
		get_cell(fs);
	}	
	
	private void get_cell(File fs){
		try {			
			FileInputStream stm = new FileInputStream(fs);
			Properties prop = new Properties();
			prop.loadFromXML(stm);
			stm.close();
			for(Node node:grid.getChildren()){
				CellView cell = (CellView)node;
				String txt = prop.getProperty(cell.getHashLoc(),"");
				if(txt.length()!=0){
					cell.setHashTID(txt);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private static final String IMG_DIR = "/narl/itrc/res/tile/";
	
	private static final Image imgCursor = new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"cursor.png"));
	
	private static final Image imgBlueprint = new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"blureprint.png"));
	
	private static Image[][] tile = {
		{	null	},
		{ 
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"gauge.png"   )), 
		},
		{ 
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"valve.png"   )), 
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"gun-01.png"  )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"gun-02.png"  )),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"holder-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"holder-02.png")),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump1-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump1-02.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump1-03.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump2-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump2-02.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump2-03.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump3-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump3-02.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump3-03.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pump4.png"   )),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank1-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank1-02.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank1-03.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank1-04.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank2-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank2-02.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank2-03.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"tank2-04.png")),
		},
		{ 
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-02.png")),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-03.png")),		
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-04.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-05.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-06.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-07.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-08.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe2-09.png")),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe3-01.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe3-02.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe3-03.png")),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe3-04.png")),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"pipe4.png"   )),
		},
		{
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-01.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-11.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-02.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-03.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-13.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-04.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-05.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-15.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-06.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-07.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-17.png" )),
			new Image(WidMapPiping.class.getResourceAsStream(IMG_DIR+"wall-08.png" )),			
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
