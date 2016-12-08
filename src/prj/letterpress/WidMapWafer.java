package prj.letterpress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import narl.itrc.BoxPhyValue;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;
import narl.itrc.WidMapBase;

import com.jfoenix.controls.JFXComboBox;

public class WidMapWafer extends WidMapBase {
	
	private JFXComboBox<String> chkWafSeq;
	private JFXComboBox<String> chkWafDia;
	private BoxPhyValue boxDieW;	
	private BoxPhyValue boxDieH;
	private BoxPhyValue boxLane;

	public WidMapWafer(){
		setMapSize(getDiameter());
		setDieSize(
			boxDieW.getValue(),
			boxDieH.getValue()
		);
		generate();
	}
	
	private final String DEF_DIE_WIDTH = "68mm";
	private final String DEF_DIE_HEIGHT= "26mm";
	
	private double getDiameter(){		
		double dia = 8*25.4;//default is 8 inch, but unit is 'mm'
		//hard code!!!
		switch(chkWafDia.getSelectionModel().getSelectedIndex()){
		case 0: dia = 6 *25.4; break;
		case 1: dia = 8 *25.4; break;
		case 2: dia = 12*25.4; break;
		}
		return dia;
	}
	
	/*private int diameter2index(){
		int idx = (int)Math.round(mapSize[0]/25.4);
		//int idx = (val<0)?(Math.round((float)(mapSize[0]/25.4))):(val);
		if(idx!=0 && idx!=1 && idx!=1){
			idx = 1;//why???
		}
		return idx;
	}*/
	
	@Override
	public Node layoutSetting() {
		
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

		final double CHK_SIZE = 110.;
		chkWafDia = new JFXComboBox<String>();
		chkWafDia.setPrefWidth(CHK_SIZE);
		chkWafDia.setOnAction(eventRedraw);
		chkWafDia.getItems().addAll(
			"6'' 晶圓",
			"8'' 晶圓",
			"12''晶圓"
		);
		chkWafDia.getSelectionModel().select(1);//default is 8'' wafer

		chkWafSeq = new JFXComboBox<String>();
		chkWafSeq.setPrefWidth(CHK_SIZE);
		chkWafSeq.setOnAction(eventRedraw);
		chkWafSeq.getItems().addAll(
			"交錯",
			"擴散"
		);
		chkWafSeq.getSelectionModel().select(0);
		
		boxDieW = new BoxPhyValue("顆粒寬").setType("mm").setValue(DEF_DIE_WIDTH);
		boxDieW.setOnAction(eventRedraw);
		boxDieH = new BoxPhyValue("顆粒高").setType("mm").setValue(DEF_DIE_HEIGHT);
		boxDieH.setOnAction(eventRedraw);
		boxLane = new BoxPhyValue("走道寬").setType("mm").setValue("0mm");
		//boxLane.setOnAction(EVENT->{});
		
		Button btnInc = new Button("+");
		btnInc.setOnAction(EVENT->{	incScale();	});
		Button btnDec = new Button("-");
		btnDec.setOnAction(EVENT->{	decScale();	});
		
		GridPane lay1 = new GridPane();
		lay1.getStyleClass().add("grid-small");
		lay1.addRow(0,new Label("掃描模式"),new Label("："),chkWafSeq);
		lay1.addRow(1,new Label("晶圓大小"),new Label("："),chkWafDia);		
		lay1.addRow(2,new Label("顆粒寬")  ,new Label("："),boxDieW);
		lay1.addRow(3,new Label("顆粒高")  ,new Label("："),boxDieH);
		lay1.addRow(4,new Label("間距大小"),new Label("："),boxLane);
		//lay1.addRow(5,new Label("比例尺")  ,new Label("："),txtScale);
		lay1.add(new Label("ZOOM"), 0, 6, 1, 1);
		lay1.add(PanBase.fillHBox(btnInc,btnDec), 1, 6, 2, 1);

		return PanDecorate.group("WAS Status",lay1);
	}
	
	@Override
	public void drawShape(GraphicsContext gc) {
		gc.save();
		gc.setStroke(Color.BLACK);
		gc.strokeArc(
			-mapGrid[0]/2, -mapGrid[1]/2, 
			mapGrid[0],mapGrid[1],
			0, 360, 
			ArcType.CHORD
		);
		gc.setFill(clrWafBack);
		gc.fillArc(
			-mapGrid[0]/2, -mapGrid[1]/2, 
			mapGrid[0],mapGrid[1],
			0, 360, 
			ArcType.CHORD
		);
		double len = flat_height();
		gc.clearRect(
			-mapGrid[0]/2,0+len, 
			mapGrid[0],mapGrid[0]-len
		);
		gc.restore();
	}

	private double flat_height(){
		double rad = Math.min(mapGrid[0]/2, mapGrid[1]/2);
		double len = rad*Math.cos((20./180.)*Math.PI);
		return len;
	}
	
	@Override
	public void layoutDie() {
		double dia = getDiameter();
		double rad = dia/2.;
		double gap = boxLane.getValue();
		double diw = boxDieW.getValue()+gap;
		double dih = boxDieH.getValue()+gap;
		
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
			diw,dih,
			rad,
			lstDie
		);
		//calculate_interleave_path();
		calculate_hardcode_path();
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
	
	public void calculate_hardcode_path(){
		lstDie.get(0).key = 1;
		lstDie.get(2).key = 2;
		lstDie.get(4).key = 3;
		lstDie.get(6).key = 4;
		lstDie.get(8).key = 5;
		lstDie.get(9).key = 6;
		lstDie.get(7).key = 7;
		lstDie.get(5).key = 8;
		lstDie.get(3).key = 9;
		lstDie.get(1).key = 10;
		for(Die dd:lstDie){
			lstSeq.put(dd.key, dd);
		}
	}
	
	public void calculate_interleave_path(){
		
		//the sequence is dependent on "calculate_valid_grid()"
		int cnt = lstDie.size() - 1;
		//always put the last die
		Die d1,d2;
		d1 = lstDie.get(cnt);
		d1.key = cnt + 1;
		lstSeq.put(d1.key,d1);
		
		for(int i=0; i<cnt; i++){
			d1 = lstDie.get(i);
			d2 = lstDie.get(i+1);
			double[] p1 = d1.getPosition();
			double[] p2 = d2.getPosition();
			d1.key = i+1;//key is one-based number!!!
			lstSeq.put(d1.key,d1);
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
				lstSeq.put(d3.key,d3);
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
