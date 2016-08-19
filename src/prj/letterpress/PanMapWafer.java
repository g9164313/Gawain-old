package prj.letterpress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.ArcType;
import narl.itrc.BoxPhyValue;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;
import narl.itrc.PanMapBase;

import com.jfoenix.controls.JFXComboBox;

public class PanMapWafer extends PanMapBase {

	private Label txtScale= new Label("??? mm/px");	
	private JFXComboBox<String> chkWafD;
	private BoxPhyValue boxDieW;	
	private BoxPhyValue boxDieH;
	private BoxPhyValue boxLane;

	public PanMapWafer(){
		super("配置圖");		
		init_widget();
		setMapSize(getDiameter());
		setDieSize(
			boxDieW.getValue(),
			boxDieH.getValue()
		);
		generate();
	}
	
	private final String DEF_DIE_WIDTH = "68mm";
	private final String DEF_DIE_HEIGHT= "26mm";
	
	private void init_widget(){
		chkWafD = new JFXComboBox<String>();
		chkWafD.getItems().addAll(
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
		chkWafD.getSelectionModel().select(diameter2index(8));//default is 8' wafer
		boxDieW = new BoxPhyValue("顆粒寬").setType("mm").setValue(DEF_DIE_WIDTH);
		boxDieH = new BoxPhyValue("顆粒高").setType("mm").setValue(DEF_DIE_HEIGHT);
		boxLane = new BoxPhyValue("走道寬").setType("mm").setValue("0mm");
	}
		
	private int diameter2index(int val){
		int idx = (val<0)?(Math.round((float)(mapSize[0]/25.4))):(val);
		idx = idx - 4;//the minimum diameter of wafer is 4''
		if(idx<0){ 
			return 0;
		}
		return idx;
	}

	/**
	 * Get the wafer diameter, unit is 'mm'.<p>
	 * @return - diameter
	 */
	public double getDiameter(){
		int idx = chkWafD.getSelectionModel().getSelectedIndex();
		return (idx+4)*25.4;//unit is millimeter
	}
	
	private GridPane con = null;
	public Pane getConsole(){
		if(con!=null){			
			return null;//we just create one console
		}
		final double CHK_SIZE = 110.;
		
		final EventHandler<ActionEvent> eventRedraw = 
			new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event) {
				setMapSize(getDiameter());
				setDieSize(
					boxDieW.getValue(),
					boxDieH.getValue()
				);
				generate();
			}
		};
		
		chkWafD.setPrefWidth(CHK_SIZE);
		chkWafD.setOnAction(eventRedraw);
		
		JFXComboBox<String> chkMethod = new JFXComboBox<String>();
		chkMethod.setPrefWidth(CHK_SIZE);
		chkMethod.getItems().addAll("循序掃描");
		chkMethod.getSelectionModel().select(0);
				
		boxDieW.setOnAction(eventRedraw);
		boxDieH.setOnAction(eventRedraw);
		boxLane.setOnAction(EVENT->{
			//???
		});
		
		Button btnInc = new Button("+");
		btnInc.setOnAction(EVENT->{	incScale();	});
		Button btnDec = new Button("-");
		btnDec.setOnAction(EVENT->{	decScale();	});
		
		con = new GridPane();
		con.getStyleClass().add("grid-small");
		con.addRow(0,new Label("掃描方式"),new Label("："),chkMethod);
		con.addRow(1,new Label("晶圓大小"),new Label("："),chkWafD);		
		con.addRow(2,new Label("顆粒寬")  ,new Label("："),boxDieW);
		con.addRow(3,new Label("顆粒高")  ,new Label("："),boxDieH);
		con.addRow(4,new Label("走道寬")  ,new Label("："),boxLane);
		con.addRow(5,new Label("比例尺")  ,new Label("："),txtScale);
		con.add(new Label("ZOOM"), 0, 6, 1, 1);
		con.add(PanBase.fillHBox(btnInc,btnDec), 1, 6, 2, 1);
		//con.add(txtInfo, 0, 5, 4, 1);
		
		return PanDecorate.group("配置圖設定",con);
	}

	@Override
	public void drawShape(GraphicsContext gc) {
		gc.save();
		gc.strokeArc(
			-mapGrid[0]/2, -mapGrid[1]/2, 
			mapGrid[0],mapGrid[1],
			0, 360, 
			ArcType.CHORD
		);
		gc.restore();
	}

	@Override
	public void layoutDie(ArrayList<Die> lst) {
		double rad = getDiameter()/2.;
		double diw = boxDieW.getValue();
		double dih = boxDieH.getValue();
		
		//int max = Math.round((float)((rad*rad*Math.PI)/(diw*dih)));
		//list all possibility
		double pos[][]={
			{0     ,0     },
			{diw/2.,0     },
			{0     ,dih/2.},
			{diw/2.,dih/2.}
		};
		int[] cnt = new int[pos.length];
		for(int i=0; i<pos.length; i++){
			cnt[i] = calculate_valid_grid(
				pos[i][0],pos[i][1],
				diw,dih,rad,
				null
			);
		}
		int idx = 0;
		for(int i=1; i<pos.length; i++){
			if(cnt[i]>cnt[idx]){
				idx = i;
			}
		}		
		calculate_valid_grid(
			pos[idx][0],pos[idx][1],
			diw,dih,rad,
			lst
		);
		//which scan path is depend on check-box
		calculate_sequence_path();
	}
	
	private int calculate_valid_grid(
		double offsetX,double offsetY,
		double dieWidth,double dieHeight,
		double wafRadius,
		ArrayList<Die> lstCell
	){
		int count = 0;
		int cnt_w = Math.round((float)(wafRadius*2/dieWidth ));
		if(cnt_w%2==0){ cnt_w++; }
		
		int cnt_h = Math.round((float)(wafRadius*2/dieHeight));		
		if(cnt_h%2==0){ cnt_h++; }
		
		//for(int j=cnt_h/2; j>-cnt_h/2; --j){
		for(int j=-cnt_h/2; j<=cnt_h/2; j++){
			for(int i=-cnt_w/2; i<=cnt_w/2; i++){
				double xx = i*dieWidth  - offsetX;
				double yy = j*dieHeight - offsetY;
				double[][] vtx={
					{xx,yy},
					{xx+dieWidth, yy},
					{xx,yy+dieHeight},
					{xx+dieWidth,yy+dieHeight},
				};
				boolean is_valid = true;
				for(int k=0; k<4; k++){
					double dist = Math.hypot(vtx[k][0],vtx[k][1]);
					if(dist>wafRadius){
						is_valid = false;
						break;
					}
				}
				if(is_valid==true){					
					count++;
					if(lstCell!=null){
						lstCell.add(new Die().setLfBm(xx,yy));
					}
				}
			}
		}
		return count;
	}
	
	private void calculate_sequence_path(){		
		//the sequence is dependent on "calculate_valid_grid()"
		
		int cnt = lstDie.size() - 1;
		//always put the last die
		Die d1,d2;
		d1 = lstDie.get(cnt);
		d1.key = cnt + 1;
		lstPath.put(d1.key,d1);
		
		for(int i=0; i<cnt; i++){
			d1 = lstDie.get(i);
			d2 = lstDie.get(i+1);
			double[] p1 = d1.getPosition();
			double[] p2 = d2.getPosition();
			d1.key = i+1;//key is one-based number!!!
			lstPath.put(d1.key,d1);
			if(p1[1]==p2[1]){
				//check whether they are in one line(row).				
				continue;
			}
			//check the boundary of next line
			int k = i+1;
			int j = i+2;			
			Die d3 = null;
			for(; j<cnt; j++){
				d3 = lstDie.get(j);
				double[] p3 = d3.getPosition();
				if(p2[1]!=p3[1]){
					i = --j;//override this for next turn~~~
					break;
				}
			}
			//check whether we got the tail of line~~~
			if(j==cnt){	i = cnt; }
			//inverse all key number
			int sum = j + k;
			for(; k<=j; k++){
				d3 = lstDie.get(k);
				d3.key = sum - k + 1;//key is one-based number!!!
				lstPath.put(d3.key,d3);
			}
		}
	}
	
	protected void eventExport(File fs){ 
		//This is just experiment code
		try {
			PrintWriter wr = new PrintWriter(fs);
			for(int i=0; i<lstDie.size(); i++){
				Die die = lstDie.get(i);
				double[] pos = die.getPosition();
				wr.printf("%03d) - %.3fmm, %.3fmm\r\n",i,pos[0],pos[1]);
			}
			wr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
