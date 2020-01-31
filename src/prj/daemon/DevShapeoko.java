package prj.daemon;


import java.util.concurrent.atomic.AtomicBoolean;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import prj.scada.DevDCG100;

@SuppressWarnings("unused")
public class DevShapeoko extends DevTTY {

	public DevShapeoko() {
		TAG = "dev-shapeoko";
		readTimeout = 5;
	}
	
	private final static String STG_INIT = "initial";
	private final static String STG_MONT = "monitor";

	private void state_initialize() {
		String ans = readTxt(2000);//message is very slow.... 
		if(ans.contains("Grbl")==false) {
			Misc.loge("No Grbl controller!!");
			nextState.set(null);
			return;
		}
		Misc.logv(ans);
		exec("$X" ,txt->{Application.invokeAndWait(()->{
			isReady.set(true);
		});});//unlock device
		exec("M05",null);//spindle off
		exec("G21",null);//set units to Millimeters
		exec("~"  ,null);		
		nextState.set(STG_MONT);
		//nextState.set(null);
	}
	private void state_monitor() {
		exec("?",txt->update_property(txt));
		//nextState.set(STG_PIPE);
	}
	@Override
	protected void afterOpen() {
		setupState0(STG_INIT, ()->state_initialize()).
		setupStateX(STG_MONT, ()->state_monitor());
		playFlow();
	}

	private final AtomicBoolean isIdle = new AtomicBoolean(false);//mirror for state flag.
	
	private final BooleanProperty isReady = new SimpleBooleanProperty(false);
	
	public final StringProperty State= new SimpleStringProperty("Reset"); 
	
	public final FloatProperty MPosX = new SimpleFloatProperty();
	public final FloatProperty MPosY = new SimpleFloatProperty();
	public final FloatProperty MPosZ = new SimpleFloatProperty();
	
	public final IntegerProperty Bf1 = new SimpleIntegerProperty();
	public final IntegerProperty Bf2 = new SimpleIntegerProperty();
	
	public final IntegerProperty Fs1 = new SimpleIntegerProperty();
	public final IntegerProperty Fs2 = new SimpleIntegerProperty();
	
	public final FloatProperty WCOX = new SimpleFloatProperty();
	public final FloatProperty WCOY = new SimpleFloatProperty();
	public final FloatProperty WCOZ = new SimpleFloatProperty();
	
	public final IntegerProperty Ov1= new SimpleIntegerProperty();
	public final IntegerProperty Ov2= new SimpleIntegerProperty();
	
	private void update_property(final String txt) {
		
		int p1 = txt.indexOf('<');
		int p2 = txt.indexOf('>');
		if(p1<0 || p2<=0 || p1>=p2) {
			return;
		}
		String[] col = txt
			.substring(p1+1,p2)
			.split("[|]");
		if(col[0].toLowerCase().equals("idle")==true) {
			isIdle.set(true);
		}else {
			isIdle.set(false);
		}
		Application.invokeAndWait(()->{
			State.set(col[0]);		
			for(int i=1; i<col.length; i++){
				String itm = col[i];
				try{
					String[] val;
					if(itm.startsWith("MPos:")==true){
						val = itm.substring(5).split(",");
						MPosX.set(Float.valueOf(val[0]));
						MPosY.set(Float.valueOf(val[1]));
						MPosZ.set(Float.valueOf(val[2]));
					}else if(itm.startsWith("Bf:")==true){
						val = itm.substring(3).split(",");
						Bf1.set(Integer.valueOf(val[0]));
						Bf2.set(Integer.valueOf(val[1]));
					}else if(itm.startsWith("FS:")==true){
						val = itm.substring(3).split(",");
						Fs1.set(Integer.valueOf(val[0]));
						Fs2.set(Integer.valueOf(val[1]));
					}else if(itm.startsWith("WCO:")==true){
						val = itm.substring(4).split(",");
						WCOX.set(Float.valueOf(val[0]));
						WCOY.set(Float.valueOf(val[1]));
						WCOZ.set(Float.valueOf(val[2]));
					}else if(itm.startsWith("Ov:")==true){
						val = itm.substring(3).split(",");
						Ov1.set(Integer.valueOf(val[0]));
						Ov2.set(Integer.valueOf(val[1]));
					}else if(itm.startsWith("Pn:")==true){
						//touch to limit!!!
					}else {
						Misc.loge("Wrong Item: %s",itm);
					}
				}catch(NumberFormatException e){
					Misc.loge("Wrong Fromat: %s",itm);
				}
			}
		});
	}
		
	public String exec(
		final String cmd,
		final ReadBack event
	) {
		String _cmd = cmd.toUpperCase();
		if(_cmd.endsWith("\n")==false) {
			_cmd = _cmd + "\n";
		}		
		writeTxt(_cmd);
		final String ans = readTxt("(?s).*(ok|error).*\\r\\n$");
		if(ans.contains("ok")==true) {
			if(event!=null) { event.callback(ans); }
		}else {
			Misc.loge("%s",ans);
		}
		return ans;
	}
	public String exec(final String cmd) {
		return exec(cmd,null);
	}
	public void asyncExec(final String cmd) {breakIn(()->{
		exec(cmd,null);
	});}	
	public void syncExec(final String cmd) {syncBreakIn(()->{
		exec(cmd,null);
	});}
	
	public void move(
		final char axis,
		final float val,
		final boolean abs
	){
		final String cmd = String.format(
			"%s\nG00%C%.1f\n",
			(abs==true)?("G90"):("G91"),
			axis, val
		);
		breakIn(()->exec(cmd,null));
	}
	
	/**
	 * move probe head.<p>
	 * @param abs : relative or absolute position value
	 * @param xx : x-axis position value.
	 * @param yy : y-axis position value.
	 * @param zz : y-axis position value.
	 */
	public void move(
		final boolean abs,
		final float xx, 
		final float yy,
		final float zz		
	){
		final String cmd = String.format(
			"%sG00X%.1fY%.1fZ%.1f\n",
			(abs==true)?("G90\n"):("G91\n"),
			xx,yy,zz
		);
		breakIn(()->exec(cmd));
	}
	/**
	 * Same as 'move()', but block current thread(caller).<p>
	 * @param abs : relative or absolute position value
	 * @param xx : x-axis position value.
	 * @param yy : y-axis position value.
	 * @param zz : y-axis position value.
	 */
	public void syncMove(
		final boolean abs,
		final float xx, 
		final float yy,
		final float zz		
	) {
		final String cmd = String.format(
			"%sG00X%.1fY%.1fZ%.1f\n",
			(abs==true)?("G90\n"):("G91\n"),
			xx,yy,zz
		);		
		breakIn(()->{
			isIdle.set(true);
			exec(cmd);
		});
		waiting();
		while(isIdle.get()==false) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
	}
	public void moveAbs(
		float xx, 
		float yy,
		float zz
	){
		move(true,xx,yy,zz);
	}
	public void moveRel(
		float xx, 
		float yy, 
		float zz
	){
		move(false,xx,yy,zz);
	}
	public void syncMoveAbs(
		float xx, 
		float yy,
		float zz
	){
		syncMove(true,xx,yy,zz);
	}
	public void syncMoveRel(
		float xx, 
		float yy, 
		float zz
	){
		syncMove(false,xx,yy,zz);
	}
	
	/**
	 * scan a rectangle.<p>
	 * if step is positive, move along x-axis and step by y-axis.<p>
	 * if step is negative, move along y-axis and step by x-axis.<p>
	 * @param step - step size, how many segments.
	 * @param points - step size, start and end point (X and Y).
	 */
	public void travelScan(
		final int step,
		final float... points
	) {breakIn(()->{
		
		final float LF = Math.min(points[0],points[2]);//left
		final float RH = Math.max(points[0],points[2]);//right
		final float TP = Math.max(points[1],points[3]);//top
		final float BM = Math.min(points[1],points[3]);//bottom
		final float stp = (step>0)?(
			-Math.abs((TP-BM)/step)
		):(
			-Math.abs((RH-LF)/step)
		);
		int cnt = Math.abs(step);
		
		float[][] line = new float[cnt][4];	
		
		if(step>0) {
			//step is vertical
			//fist line coordinate
			line[0][0] = LF; line[0][1] = TP;
			line[0][2] = RH; line[0][3] = TP;
			//last line coordinate
			line[cnt-1][0] = LF; line[cnt-1][1] = BM;
			line[cnt-1][2] = RH; line[cnt-1][3] = BM;
			//other segments~~~
			for(int i=1; i<cnt-1; i+=1) {
				line[i][0] = LF; line[i][1] = TP + i*stp;
				line[i][2] = RH; line[i][3] = TP + i*stp;
			}
		}else {
			//step is horizontal
			//fist line coordinate
			line[0][0] = LF; line[0][1] = TP;
			line[0][2] = LF; line[0][3] = BM;
			//last line coordinate
			line[cnt-1][0] = RH; line[cnt-1][1] = TP;
			line[cnt-1][2] = RH; line[cnt-1][3] = BM;
			//other segments~~~
			for(int i=1; i<cnt-1; i+=1) {
				line[i][0] = LF + i*stp; line[i][1] = TP;
				line[i][2] = LF + i*stp; line[i][3] = BM;
			}
		}
		
		String cmd = "G90\n";
		for(int i=0; i<cnt; i+=1) {
			float x1 = line[i][(0+2*(i%2))%4];
			float y1 = line[i][(1+2*(i%2))%4];
			float x2 = line[i][(2+2*(i%2))%4];
			float y2 = line[i][(3+2*(i%2))%4];
			cmd = cmd + String.format(
				"G01X%.1fY%.1fF5000\n"+
				"G01X%.1fY%.1fF5000\n",
				x1,y1, x2,y2
			);
		}
		cmd = cmd + String.format(
			"G01X%.1fY%.1fF5000\n",
			(LF+RH)/2f, (TP+BM)/2f
		);//go to original point
		Misc.logv("\n"+cmd);//dry-run~~
		exec(cmd);
	});}
	//---------------------------------------//
	
	private static void move_gui(
		final DevShapeoko dev,
		final char token,
		final CheckBox chkJog,
		final CheckBox chkAbs,
		final TextField boxVal
	) {	
		float val = 0f;
		try {
			val = Float.valueOf(boxVal.getText());
		}catch(NumberFormatException e) {
			return;
		}
		char axs = 'X';
		switch(token) {
		case 'x':
		case 'X':
			axs = 'X';
			break;
		case 'y':
		case 'Y':
			axs = 'Y';
			break;
		case 'z':
		case 'Z': 
			axs = 'Z';
			break;
		case 'm':
		case 'M':
			axs = 'X';
			val = -1f * val;
			break;
		case 'n':
		case 'N':
			axs = 'Y';
			val = -1f * val;
			break;
		case 'o':
		case 'O':
			axs = 'Z';
			val = -1f * val;
			break;
		default: Misc.logw("invalid direction token."); return;
		}
		dev.move(axs, val, chkAbs.isSelected());
	}
	
	private static final String FMT_VAL = "%#7.1f mm";
	
	private static String cook(final String txt) {
		return txt.substring(0,txt.length()-3);
	}
	
	public static Pane genPanel(final DevShapeoko dev) {
		
		final Label[] txt = new Label[10];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setFillWidth(txt[i], true);
		}		
		txt[0].textProperty().bind(dev.State);
		txt[1].textProperty().bind(dev.MPosX.asString(FMT_VAL));
		txt[2].textProperty().bind(dev.MPosY.asString(FMT_VAL));
		txt[3].textProperty().bind(dev.MPosZ.asString(FMT_VAL));
		txt[4].setText(String.format(FMT_VAL, -30.00));
		txt[5].setText(String.format(FMT_VAL,  30.00));
		txt[6].setText(String.format(FMT_VAL,  30.00));
		txt[7].setText(String.format(FMT_VAL, -30.00));
		
		final JFXCheckBox chkJog = new JFXCheckBox("Jogging");
		GridPane.setFillWidth(chkJog, true);
		
		final Label txtMove = new Label("偏移：");
		
		final JFXCheckBox chkMove = new JFXCheckBox("相對移動");
		chkMove.disableProperty().bind(chkJog.selectedProperty());
		chkMove.setOnAction(e->{
			if(chkMove.isSelected()==true){
				chkMove.setText("絕對位置");
				txtMove.setText("位置：");
			}else{
				chkMove.setText("相對移動");
				txtMove.setText("偏移：");
			}
		});
		GridPane.setFillWidth(chkMove, true);
		
		final TextField[] box = {
			new TextField(),
			new TextField(),
		};
		for(TextField obj:box) {
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setPrefWidth(80);
			GridPane.setFillWidth(obj, true);
		}
		
		box[0].disableProperty().bind(chkJog.selectedProperty());			
		box[0].setText("15");
		
		box[1].setText("5");//grid for segment lines
		
		final JFXButton[] btn = new JFXButton[11];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(obj, true);
			btn[i] = obj;
		}
		btn[0].setGraphic(Misc.getIconView("dir-up.png"));
		btn[0].setOnAction(e->move_gui(dev,'Y',chkJog,chkMove,box[0]));
		
		btn[1].setGraphic(Misc.getIconView("arrow-up.png"));
		btn[1].setOnAction(e->move_gui(dev,'Z',chkJog,chkMove,box[0]));
		
		btn[2].setGraphic(Misc.getIconView("dir-left.png"));
		btn[2].setOnAction(e->move_gui(dev,'M',chkJog,chkMove,box[0]));
		
		btn[3].setGraphic(Misc.getIconView("dir-down.png"));
		btn[3].setOnAction(e->move_gui(dev,'N',chkJog,chkMove,box[0]));
		
		btn[4].setGraphic(Misc.getIconView("dir-right.png"));
		btn[4].setOnAction(e->move_gui(dev,'X',chkJog,chkMove,box[0]));
		
		btn[5].setGraphic(Misc.getIconView("arrow-down.png"));
		btn[5].setOnAction(e->move_gui(dev,'O',chkJog,chkMove,box[0]));
		
		btn[6].setText("歸零");
		btn[6].setGraphic(Misc.getIconView("home.png"));
		btn[6].getStyleClass().add("btn-raised-1");
		//btn[6].setOnAction();
		
		final GridPane lay3 = new GridPane();
		lay3.getStyleClass().addAll("box-pad");
		lay3.add(btn[0], 1, 0);
		lay3.add(btn[1], 3, 0);
		lay3.addRow(1, btn[2], btn[3], btn[4], btn[5]);
		lay3.add(chkJog , 0, 2, 2, 1);
		lay3.add(chkMove, 2, 2, 2, 1);
		lay3.add(txtMove, 0, 4, 1, 1);
		lay3.add(box[0], 1, 4, 3, 1);
		lay3.add(btn[6], 0, 5, 4, 1);
		
		final GridPane lay1 = new GridPane();		
		lay1.getStyleClass().addAll("box-pad");
		lay1.addRow(0, new Label("狀態："), txt[0]);
		lay1.addRow(1, new Label("X 軸："), txt[1]);
		lay1.addRow(2, new Label("Y 軸："), txt[2]);
		lay1.addRow(3, new Label("Z 軸："), txt[3]);
		lay1.add(new Separator(), 0, 4, 2, 1);
		
		lay1.add(lay3, 0, 5, 2, 1);
		lay1.disableProperty().bind(dev.isReady.not());
		return lay1;
	}
	
	public static Pane genPanelInfo(final DevShapeoko dev) {
		
		final Label[] txt = new Label[4];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setFillWidth(txt[i], true);
		}		
		txt[0].textProperty().bind(dev.State);
		txt[1].textProperty().bind(dev.MPosX.asString(FMT_VAL));
		txt[2].textProperty().bind(dev.MPosY.asString(FMT_VAL));
		txt[3].textProperty().bind(dev.MPosZ.asString(FMT_VAL));
		
		final GridPane lay = new GridPane();
		lay.disableProperty().bind(dev.isReady.not());
		lay.getStyleClass().addAll("box-border");
		lay.addRow(0, new Label("狀態："), txt[0]);
		lay.addRow(1, new Label("X 軸："), txt[1]);
		lay.addRow(2, new Label("Y 軸："), txt[2]);
		lay.addRow(3, new Label("Z 軸："), txt[3]);		
		return lay;
	}
	
	public static Pane genPanelMove(final DevShapeoko dev) {
		
		final JFXCheckBox chkJog = new JFXCheckBox("Jogging");
		GridPane.setFillWidth(chkJog, true);
		
		final Label txtMove = new Label("偏移：");
		
		final JFXCheckBox chkMove = new JFXCheckBox("相對移動");
		chkMove.disableProperty().bind(chkJog.selectedProperty());
		chkMove.setOnAction(e->{
			if(chkMove.isSelected()==true){
				chkMove.setText("絕對位置");
				txtMove.setText("位置：");
			}else{
				chkMove.setText("相對移動");
				txtMove.setText("偏移：");
			}
		});
		GridPane.setFillWidth(chkMove, true);
		
		final TextField boxVal = new TextField();
		boxVal.setMaxWidth(Double.MAX_VALUE);
		boxVal.setPrefWidth(80);
		GridPane.setFillWidth(boxVal, true);
			
		boxVal.disableProperty().bind(chkJog.selectedProperty());			
		boxVal.setText("15");
			
		final JFXButton[] btn = new JFXButton[7];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(obj, true);
			btn[i] = obj;
		}
		btn[0].setGraphic(Misc.getIconView("dir-up.png"));
		btn[0].setOnAction(e->move_gui(dev,'Y',chkJog,chkMove,boxVal));
			
		btn[1].setGraphic(Misc.getIconView("arrow-up.png"));
		btn[1].setOnAction(e->move_gui(dev,'Z',chkJog,chkMove,boxVal));
			
		btn[2].setGraphic(Misc.getIconView("dir-left.png"));
		btn[2].setOnAction(e->move_gui(dev,'M',chkJog,chkMove,boxVal));
			
		btn[3].setGraphic(Misc.getIconView("dir-down.png"));
		btn[3].setOnAction(e->move_gui(dev,'N',chkJog,chkMove,boxVal));
		
		btn[4].setGraphic(Misc.getIconView("dir-right.png"));
		btn[4].setOnAction(e->move_gui(dev,'X',chkJog,chkMove,boxVal));
			
		btn[5].setGraphic(Misc.getIconView("arrow-down.png"));
		btn[5].setOnAction(e->move_gui(dev,'O',chkJog,chkMove,boxVal));
		
		btn[6].setText("歸零");
		btn[6].setGraphic(Misc.getIconView("home.png"));
		btn[6].getStyleClass().add("btn-raised-1");
		
		final GridPane lay = new GridPane();
		lay.disableProperty().bind(dev.isReady.not());
		lay.getStyleClass().addAll("box-pad");
		lay.add(btn[0], 1, 0);
		lay.add(btn[1], 3, 0);
		lay.addRow(1, btn[2], btn[3], btn[4], btn[5]);
		lay.add(chkJog , 0, 2, 2, 1);
		lay.add(chkMove, 2, 2, 2, 1);
		lay.add(txtMove, 0, 4, 1, 1);
		lay.add(boxVal , 1, 4, 3, 1);
		lay.add(btn[6] , 0, 5, 4, 1);
		
		return lay;
	}
	//---- below functions are for convenience
	
	/**
	 * convenience function for generating absolute location.<p> 
	 * @param offsetX
	 * @param offsetY
	 * @param width  - grid width (mm).
	 * @param height - grid height (mm).
	 * @param gridX  - sample points in horizontal
	 * @param gridY  - sample points in vertical
	 * @return float array
	 */
	public static float[][] genGridPoint(
		final float offsetX,
		final float offsetY,
		final float width,
		final float height,
		final int gridX,
		final int gridY		
	){
		float[][] grid = new float[gridX*gridY+1][3];
		float[] step = {
			width /(float)(gridX - 1),
			height/(float)(gridY - 1),	
		};
		float[] loca = { 
			-width /2f, 
			 height/2f
		};
		float dir = 1f;
		for(int gy=0; gy<gridY; gy+=1) {			
			for(int gx=0; gx<gridX; gx+=1) {
				int i = gx + gy * gridX;
				grid[i][0] = loca[0] + offsetX;
				grid[i][1] = loca[1] + offsetY;
				if(gx<gridX-1) {
					loca[0] = loca[0] + dir * step[0];
				}
			}
			loca[1] = loca[1] - step[1];
			dir = dir * -1f;
		}
		//last location~~~
		grid[gridX*gridY][0] = offsetX;
		grid[gridX*gridY][1] = offsetY;
		return grid;
	}
	public static float[][] genGridPoint(
		final float width,
		final float height,
		final int gridX,
		final int gridY		
	){
		return genGridPoint(
			0f,0f,
			width,height,
			gridX,gridY
		);
	}
	
	
	//lay1.addRow(5, new Label("切片數："), box[1]);
	//lay1.add(btn[8], 0, 6, 2, 1);
	//lay1.addRow(7, new Label("X-1："), txt[4]);
	//lay1.addRow(8, new Label("Y-1："), txt[5]);
	//lay1.add(btn[9], 0, 9, 2, 1);
	//lay1.addRow(10, new Label("X-2："), txt[6]);
	//lay1.addRow(11, new Label("Y-2："), txt[7]);
	//lay1.add(new Separator(), 0, 12, 2, 1);
	
	/*btn[7].setText("掃描");
	btn[7].setGraphic(Misc.getIconView("walk.png"));
	btn[7].getStyleClass().add("btn-raised-2");
	btn[7].setOnAction(e->{
		int stp = Integer.valueOf(box[1].getText());
		float[] pts = {
			Float.valueOf(cook(txt[4].getText())),
			Float.valueOf(cook(txt[5].getText())),
			Float.valueOf(cook(txt[6].getText())),
			Float.valueOf(cook(txt[7].getText()))
		};				
		dev.travelScan(stp, pts);
	});		
	btn[8].setText("標定-1");
	btn[8].setGraphic(Misc.getIconView("flag.png"));
	btn[8].setOnAction(e->{
		txt[4].setText(String.format(FMT_VAL,dev.MPosX.get()));
		txt[5].setText(String.format(FMT_VAL,dev.MPosY.get()));
	});
	btn[9].setText("標定-2");
	btn[9].setGraphic(Misc.getIconView("flag.png"));
	btn[9].setOnAction(e->{
		txt[6].setText(String.format(FMT_VAL,dev.MPosX.get()));
		txt[7].setText(String.format(FMT_VAL,dev.MPosY.get()));
	});*/
}
