package prj.scada;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import narl.itrc.Misc;
import narl.itrc.UtilRandom;

public class PID_Leaf extends Canvas {

	private PID_Root root;
	
	private String  name = null;
	private int[]   info = {
		-1,//skin type 
		0, //skin index
		0, //grid index - x
		0, //grid index - y
	};
	private Image[] skin = null; 
	
	public PID_Leaf(){
		setWidth(PID_Const.GRID_SIZE);
		setHeight(PID_Const.GRID_SIZE);
	}
	public PID_Leaf(final int type){
		this(type, null, 0, 0);
	}
	public PID_Leaf(
		final int type, 
		final int grid_x, 
		final int grid_y
	){
		this(type, null, grid_x, grid_y);
	}
	public PID_Leaf(		
		final int type,
		String name,
		final int grid_x, 
		final int grid_y		
	){
		setType(type);
		setName(name);
		setGridIndx(grid_x, grid_y);
		setOnMouseClicked(event->{
			if(root.isEditMode()==true){
				return;
			}
			if(info[1]==0){
				info[1] = 1;//active
			}else{
				info[1] = 0;//inactive
			}
			getGraphicsContext2D().drawImage(skin[info[1]], 0., 0.);
			//TODO: post-action event
		});
	}
	
	public PID_Leaf setType(int type){
		info[0] = type;
		skin = lstSkin.get(type);
		Image img = skin[info[1]];
		setWidth(img.getWidth());
		setHeight(img.getHeight());
		getGraphicsContext2D().drawImage(img, 0., 0.);		
		return this;
	}	
	public PID_Leaf setName(String Name){
		if(Name==null){
			name = "@"+UtilRandom.uuid(4, 10);
		}else{
			name = Name;
		}
		return this;
	}
	public PID_Leaf setRoot(PID_Root Root){
		if(root!=null && root!=Root){
			//remove self from the previous root
			root.getChildren().remove(this);
			root = Root;
		}
		if(Root.getChildren().contains(this)==false){
			Root.getChildren().add(this);
		}
		root = Root;//Finally, rewrite variable again~~~
		_update_location();
		return this;
	}
	public PID_Leaf setLocation(int px, int py){
		return setGridIndx(
			px/PID_Const.GRID_SIZE, 
			py/PID_Const.GRID_SIZE
		);
	}
	public PID_Leaf setGridIndx(int gx, int gy){
		info[2] = gx;
		info[3] = gy;
		_update_location();
		return this;
	}
	
	public int[] getSize(){
		final int[] size = {
			info[2],
			info[3],
			info[2] + (int)getWidth() / PID_Const.GRID_SIZE - 1,
			info[3] + (int)getHeight()/ PID_Const.GRID_SIZE - 1,
		};
		return size;
	}
	
	private void _update_location(){
		AnchorPane.setLeftAnchor(
			this, 
			(double)(info[2] * PID_Const.GRID_SIZE)
		);
		AnchorPane.setTopAnchor (
			this, 
			(double)(info[3] * PID_Const.GRID_SIZE)
		);	
	}

	public PID_Leaf castSkin(){
		if(info[1]==0){
			return this;
		}
		info[1]+=1;
		if(info[1]>=skin.length){
			info[1]= 1;
		}
		getGraphicsContext2D().drawImage(skin[info[1]], 0., 0.);
		return this;
	}

	public void setOnAction(){
		
	}
	
	public static String flatten(PID_Leaf itm){		
		return String.format(
			"%d,%s,%d#%d",
			itm.info[0],
			itm.name,
			itm.info[2], itm.info[3]
		);
	}
	
	public static PID_Leaf unflatten(String txt){	
		String[] parm = txt.split(",");	
		String[] loca = parm[2].split("#");
		return new PID_Leaf(
			Integer.valueOf(parm[0]),
			parm[1], 
			Integer.valueOf(loca[0]),
			Integer.valueOf(loca[1])
		);
	}
	
	public static ImageView getThumb(int type){
		switch(type){
		case PID_Const.CURSOR_SELECT:
			return Misc.getResIcon("cursor-pointer.png"); 
		case PID_Const.CURSOR_DELETE:
			return Misc.getResIcon("trash-can-outline.png");
		}
		return getLastSkin(type);
	}
	
	private static ImageView getLastSkin(int type){
		Image[] img = lstSkin.get(type);
		if(img==null){
			return null;
		}
		return new ImageView(img[img.length-1]);
	}
	
	private static Hashtable<Integer,Image[]> lstSkin = new Hashtable<>();
	
	public static void initialize(){
		
		final String pkg = "/narl/itrc/res/pid";
		
		final Image pi1_0 = Misc.getResImage(pkg, "pipe-I1-0.png");
		final Image pi1_a = Misc.getResImage(pkg, "pipe-I1-A.png");
		final Image pi1_b = Misc.getResImage(pkg, "pipe-I1-B.png");
		final Image pi1_c = Misc.getResImage(pkg, "pipe-I1-C.png");
		final Image pi1_d = Misc.getResImage(pkg, "pipe-I1-D.png");
		final Image pi1_e = Misc.getResImage(pkg, "pipe-I1-E.png");
		final Image pi2_0 = Misc.getResImage(pkg, "pipe-I2-0.png");
		final Image pi2_a = Misc.getResImage(pkg, "pipe-I2-A.png");
		final Image pi2_b = Misc.getResImage(pkg, "pipe-I2-B.png");
		final Image pi2_c = Misc.getResImage(pkg, "pipe-I2-C.png");
		final Image pi2_d = Misc.getResImage(pkg, "pipe-I2-D.png");
		final Image pi2_e = Misc.getResImage(pkg, "pipe-I2-E.png");
		
		final Image pl1_0 = Misc.getResImage(pkg, "pipe-L1-0.png");
		final Image pl1_a = Misc.getResImage(pkg, "pipe-L1-A.png");
		final Image pl1_b = Misc.getResImage(pkg, "pipe-L1-B.png");
		final Image pl2_0 = Misc.getResImage(pkg, "pipe-L2-0.png");
		final Image pl2_a = Misc.getResImage(pkg, "pipe-L2-A.png");
		final Image pl2_b = Misc.getResImage(pkg, "pipe-L2-B.png");
		final Image pl3_0 = Misc.getResImage(pkg, "pipe-L3-0.png");
		final Image pl3_a = Misc.getResImage(pkg, "pipe-L3-A.png");
		final Image pl3_b = Misc.getResImage(pkg, "pipe-L3-B.png");
		final Image pl4_0 = Misc.getResImage(pkg, "pipe-L4-0.png");
		final Image pl4_a = Misc.getResImage(pkg, "pipe-L4-A.png");
		final Image pl4_b = Misc.getResImage(pkg, "pipe-L4-B.png");
		
		final Image pt1_0 = Misc.getResImage(pkg, "pipe-T1-0.png");
		final Image pt1_a = Misc.getResImage(pkg, "pipe-T1-A.png");
		final Image pt2_0 = Misc.getResImage(pkg, "pipe-T2-0.png");
		final Image pt2_a = Misc.getResImage(pkg, "pipe-T2-A.png");
		final Image pt3_0 = Misc.getResImage(pkg, "pipe-T3-0.png");
		final Image pt3_a = Misc.getResImage(pkg, "pipe-T3-A.png");
		final Image pt4_0 = Misc.getResImage(pkg, "pipe-T4-0.png");
		final Image pt4_a = Misc.getResImage(pkg, "pipe-T4-A.png");
		
		final Image[] pipe_i1_lf = { pi1_0, pi1_e, pi1_d, pi1_c, pi1_b, pi1_a, };
		final Image[] pipe_i1_rh = { pi1_0, pi1_a, pi1_b, pi1_c, pi1_d, pi1_e, };
		final Image[] pipe_i2_up = { pi2_0, pi2_e, pi2_d, pi2_c, pi2_b, pi2_a, };
		final Image[] pipe_i2_dw = { pi2_0, pi2_a, pi2_b, pi2_c, pi2_d, pi2_e, };
		lstSkin.put(PID_Const.Pipe1LF, pipe_i1_lf);
		lstSkin.put(PID_Const.Pipe1RH, pipe_i1_rh);
		lstSkin.put(PID_Const.Pipe1UP, pipe_i2_up);
		lstSkin.put(PID_Const.Pipe1DW, pipe_i2_dw);
		
		final Image[] pipe_l1_a = { pl1_0, pl1_a, pl1_b };
		final Image[] pipe_l2_a = { pl2_0, pl2_a, pl2_b };
		final Image[] pipe_l3_a = { pl3_0, pl3_a, pl3_b };
		final Image[] pipe_l4_a = { pl4_0, pl4_a, pl4_b };
		lstSkin.put(PID_Const.PipeL1a, pipe_l1_a);
		lstSkin.put(PID_Const.PipeL2a, pipe_l2_a);
		lstSkin.put(PID_Const.PipeL3a, pipe_l3_a);
		lstSkin.put(PID_Const.PipeL4a, pipe_l4_a);
		final Image[] pipe_l1_b = { pl1_0, pl1_b, pl1_a };
		final Image[] pipe_l2_b = { pl2_0, pl2_b, pl2_a };
		final Image[] pipe_l3_b = { pl3_0, pl3_b, pl3_a };
		final Image[] pipe_l4_b = { pl4_0, pl4_b, pl4_a };
		lstSkin.put(PID_Const.PipeL1b, pipe_l1_b);		
		lstSkin.put(PID_Const.PipeL2b, pipe_l2_b);		
		lstSkin.put(PID_Const.PipeL3b, pipe_l3_b);		
		lstSkin.put(PID_Const.PipeL4b, pipe_l4_b);
				
		final Image[] pipe_t1 = { pt1_0, pt1_a };
		final Image[] pipe_t2 = { pt2_0, pt2_a };
		final Image[] pipe_t3 = { pt3_0, pt3_a };
		final Image[] pipe_t4 = { pt4_0, pt4_a };
		lstSkin.put(PID_Const.PipeT1, pipe_t1);
		lstSkin.put(PID_Const.PipeT2, pipe_t2);
		lstSkin.put(PID_Const.PipeT3, pipe_t3);
		lstSkin.put(PID_Const.PipeT4, pipe_t4);
		
		final Image[] pipe_xx = { 
			Misc.getResImage(pkg, "pipe-XX-0.png"), 
			Misc.getResImage(pkg, "pipe-XX-A.png")
		};
		lstSkin.put(PID_Const.PipeXX, pipe_xx);
		
		final Image[] wall_1 = { Misc.getResImage(pkg, "wall-1.png") };
		final Image[] wall_2 = { Misc.getResImage(pkg, "wall-2.png") };
		final Image[] wall_3 = { Misc.getResImage(pkg, "wall-3.png") };
		final Image[] wall_4 = { Misc.getResImage(pkg, "wall-4.png") };
		final Image[] wall_5 = { Misc.getResImage(pkg, "wall-5.png") };
		final Image[] wall_6 = { Misc.getResImage(pkg, "wall-6.png") };
		final Image[] wall_7 = { Misc.getResImage(pkg, "wall-7.png") };
		final Image[] wall_8 = { Misc.getResImage(pkg, "wall-8.png") };
		lstSkin.put(PID_Const.WALL_1, wall_1);
		lstSkin.put(PID_Const.WALL_2, wall_2);
		lstSkin.put(PID_Const.WALL_3, wall_3);
		lstSkin.put(PID_Const.WALL_4, wall_4);
		lstSkin.put(PID_Const.WALL_5, wall_5);
		lstSkin.put(PID_Const.WALL_6, wall_6);
		lstSkin.put(PID_Const.WALL_7, wall_7);
		lstSkin.put(PID_Const.WALL_8, wall_8);
	}
	//--------------------------------//
	
	public PID_Leaf generate_brick(){
		//special method for generating all bricks in pipe diagram
		//draw_all_pipe();
		//draw_all_wall();
		return this;
	}
	
	/*private void draw_all_wall(){
		
		getGraphicsContext2D().clearRect(0, 0, gs, gs);
		save_image("wall-0.png");
		
		draw_wall_I("wall-1.png", 
			0 , 0, 
			gs, 0
		);
		draw_wall_C("wall-2.png", 
			0 , 0, gs, 0, gs, gs,
			0 , gs/2, gs/2, gs
		);
		draw_wall_I("wall-3.png", 
			gs, 0 , 
			gs, gs
		);
		draw_wall_C("wall-4.png", 
			gs, 0, gs, gs, 0 , gs,
			gs/2, 0, 0, gs/2
		);
		draw_wall_I("wall-5.png", 
			gs, gs, 
			0 , gs
		);
		draw_wall_C("wall-6.png", 
			gs, gs, 0 , gs, 0 , 0,
			gs/2, 0, gs, gs/2
		);
		draw_wall_I("wall-7.png", 
			0 , gs, 
			0 , 0
		);
		draw_wall_C("wall-8.png", 
			0 , gs, 0 , 0, gs, 0,
			gs/2, gs, gs, gs/2
		);
		
		Misc.exec("montage",
			"wall-8.png","wall-1.png","wall-2.png",
			"wall-7.png","wall-0.png","wall-3.png",
			"wall-6.png","wall-5.png","wall-4.png",
			"-geometry", "+0+0", "qqq.png"
		);
	}
	
	private void draw_wall_I(String name, double... pot){
		final int wall_0 = gs-4;
		final int wall_a = 12;
		final int wall_b = 2;
		GraphicsContext gc = getGraphicsContext2D();
		
		gc.setStroke(Color.DIMGREY);
		gc.setLineWidth(wall_0);
		gc.strokeLine(
			pot[0], pot[1],
			pot[2], pot[3]
		);
		
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(wall_a);
		gc.strokeLine(
			pot[0], pot[1],
			pot[2], pot[3]
		);
				
		gc.setLineWidth(wall_b);
		if(Math.abs(pot[0]-pot[2])<=0.01){
			gc.strokeLine(
				gs/2, 0,
				gs/2, gs
			);
		}else{
			gc.strokeLine(
				0 , gs/2,
				gs, gs/2
			);
		}

		save_image(name);
		gc.clearRect(0, 0, gs, gs);
	}
	
	private void draw_wall_C(String name, double... pot){
		final int wall_0 = gs-4;
		final int wall_a = 12;
		final int wall_b = 2;
		GraphicsContext gc = getGraphicsContext2D();
		
		gc.setStroke(Color.DIMGREY);
		gc.setLineWidth(wall_0);
		gc.beginPath();
		gc.bezierCurveTo(
			pot[0], pot[1],
			pot[2], pot[3],
			pot[4], pot[5]
		);
		gc.moveTo(pot[4], pot[5]);
		gc.closePath();
		gc.stroke();
		
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(wall_a);
		gc.strokeLine(
			pot[0], pot[1],
			pot[2], pot[3]
		);		
		gc.strokeLine(
			pot[2], pot[3],
			pot[4], pot[5]
		);
		
		gc.setLineWidth(wall_b);
		gc.beginPath();
		gc.bezierCurveTo(
			pot[6], pot[7],
			gs/2, gs/2,
			pot[8], pot[9]
		);
		gc.moveTo(pot[8], pot[9]);
		gc.closePath();
		gc.stroke();
		
		save_image(name);
		gc.clearRect(0, 0, gs, gs);
	}
	
	private static final Stop beg = new Stop(0.0, Color.GREENYELLOW);
	private static final Stop end = new Stop(1.0, Color.GREEN);
	private static final LinearGradient grad1 = new LinearGradient(
		0, 0, 
		1, 1,
		true, CycleMethod.REFLECT, beg, end
	);
	private static final LinearGradient grad2 = new LinearGradient(
		1, 1, 
		0, 0,
		true, CycleMethod.REFLECT, beg, end
	);
	private static final LinearGradient grad3 = new LinearGradient(
		0, 0, 
		1, 0,
		true, CycleMethod.REFLECT, beg, end
	);
	private static final LinearGradient grad4 = new LinearGradient(
		0, 0, 
		0, 1,
		true, CycleMethod.REFLECT, beg, end
	);
	private static final double grad_step = 5.;
	private static final int gs = PID_Const.GRID_SIZE;	
	private static final int ps = 10;
	private static final int flow_s = ps - 4;
	
	private void draw_all_pipe(){

		//horizontal pipe
		final double[] i1 = {
			0 , gs/2, 
			gs, gs/2
		};
		draw_shape_I("pipe-I1-0.png", '@', i1);
		draw_shape_I("pipe-I1-A.png", 'A', i1);
		draw_shape_I("pipe-I1-B.png", 'B', i1);
		draw_shape_I("pipe-I1-C.png", 'C', i1);
		draw_shape_I("pipe-I1-D.png", 'D', i1);
		draw_shape_I("pipe-I1-E.png", 'E', i1);

		
		//vertical pipe
		final double[] i2 = {
			gs/2, 0, 
			gs/2, gs
		};
		draw_shape_I("pipe-I2-0.png", '@', i2); 
		draw_shape_I("pipe-I2-A.png", 'A', i2);
		draw_shape_I("pipe-I2-B.png", 'B', i2);
		draw_shape_I("pipe-I2-C.png", 'C', i2);
		draw_shape_I("pipe-I2-D.png", 'D', i2);
		draw_shape_I("pipe-I2-E.png", 'E', i2);
		
		//L-shape pipe, zone-1
		final double[] l1 = {
			gs  , gs/2,
			gs/2, 0
		};
		draw_shape_L("pipe-L1-0.png",'@', l1);
		draw_shape_L("pipe-L1-A.png",'A', l1);
		draw_shape_L("pipe-L1-B.png",'B', l1);
		
		//L-shape pipe, zone-2
		final double[] l2 = {
			0   , gs/2,
			gs/2, 0,			
		};
		draw_shape_L("pipe-L2-0.png",'@', l2);
		draw_shape_L("pipe-L2-A.png",'C', l2);
		draw_shape_L("pipe-L2-B.png",'D', l2);

		//L-shape pipe, zone-3
		final double[] l3 = {
			0   , gs/2,
			gs/2, gs
		};
		draw_shape_L("pipe-L3-0.png",'@', l3);
		draw_shape_L("pipe-L3-A.png",'A', l3);
		draw_shape_L("pipe-L3-B.png",'B', l3);
		
		//L-shape pipe, zone-4
		final double[] l4 = {
			gs  , gs/2,
			gs/2, gs
		};
		draw_shape_L("pipe-L4-0.png",'@', l4);
		draw_shape_L("pipe-L4-A.png",'C', l4);
		draw_shape_L("pipe-L4-B.png",'D', l4);

		//T-shape pipe, up
		final double[] t1={
			0, gs,
			0, gs/2
		};
		draw_shape_XT("pipe-T1-0.png",'@',t1);
		draw_shape_XT("pipe-T1-A.png",'A',t1);
		
		//T-shape pipe, down
		final double[] t2={
			0   , gs,
			gs/2, gs	
		};
		draw_shape_XT("pipe-T2-0.png",'@',t2);
		draw_shape_XT("pipe-T2-A.png",'A',t2);
		
		//T-shape pipe, left
		final double[] t3={
			0, gs/2,
			0, gs
		};
		draw_shape_XT("pipe-T3-0.png",'@',t3);
		draw_shape_XT("pipe-T3-A.png",'A',t3);
		
		//T-shape pipe, right
		final double[] t4={
			gs/2, gs,
			0   , gs
		};
		draw_shape_XT("pipe-T4-0.png",'@',t4);
		draw_shape_XT("pipe-T4-A.png",'A',t4);
		
		//Cross-shape pipe
		final double[] x1 = {0, gs, 0, gs};
		draw_shape_XT("pipe-XX-0.png", '@', x1);
		draw_shape_XT("pipe-XX-A.png", 'A', x1);
	}
	
	private void draw_shape_I(String name, char step, double... loca){

		GraphicsContext gc = getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(ps);
		
		gc.strokeLine(
			loca[0], loca[1], 
			loca[2], loca[3]
		);
		int stp = ((int)(step-'@'));
		if(stp!=0){
			double dir_x = loca[0] + ((loca[2]-loca[0])/grad_step) * stp;
			double dir_y = loca[1] + ((loca[3]-loca[1])/grad_step) * stp;
			gc.setLineWidth(flow_s);
			gc.setStroke(grad1);			
			gc.strokeLine(
				loca[0], loca[1], 
				dir_x  , dir_y
			);
			gc.setStroke(grad2);
			gc.strokeLine(
				dir_x  , dir_y,
				loca[2], loca[3]
			);
		}
		
		save_image(name);
		gc.clearRect(0, 0, gs, gs);
	}
	
	private void draw_shape_L(String name, char step, double... loca){

		GraphicsContext gc = getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(ps);
		
		gc.beginPath();
		gc.bezierCurveTo(
			loca[0], loca[1],
			gs/2, gs/2,
			loca[2], loca[3]
		);
		gc.moveTo(loca[2], loca[3]);
		gc.closePath();
		gc.stroke();
		
		int stp = ((int)(step-'@'));
		if(stp!=0){
			gc.setLineWidth(flow_s);
			switch(stp){
			case 1:
				gc.setStroke(grad1);
				break;
			case 2:
				gc.setStroke(grad2);
				break;
			case 3:
				gc.setStroke(grad3);
				break;
			case 4:
				gc.setStroke(grad4);
				break;
			}
			gc.beginPath();
			gc.bezierCurveTo(
				loca[0], loca[1],
				gs/2, gs/2,
				loca[2], loca[3]
			);
			gc.moveTo(loca[2], loca[3]);
			gc.closePath();
			gc.stroke();
		}
		
		save_image(name);
		gc.clearRect(0, 0, gs, gs);
	}
	
	private void draw_shape_XT(String name, char step, double... loca){

		GraphicsContext gc = getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(ps);
		
		gc.strokeLine(
			loca[0], gs/2, 
			loca[1], gs/2
		);
		gc.strokeLine(
			gs/2, loca[2],  
			gs/2, loca[3]
		);
		
		int stp = ((int)(step-'@'));
		if(stp!=0){
			gc.setLineWidth(flow_s);
			gc.setStroke(Color.GREENYELLOW);
			gc.strokeLine(
				loca[0], gs/2, 
				loca[1], gs/2
			);
			gc.strokeLine(
				gs/2, loca[2],  
				gs/2, loca[3]
			);
		}
		
		save_image(name);
		gc.clearRect(0, 0, gs, gs);
	}
	
	private void save_image(String fileName){
	    try {
	    	int idx = fileName.lastIndexOf('.');
	    	if(idx<0){
	    		return;
	    	}
	    	String fmt = fileName.substring(idx+1);

			SnapshotParameters parm = new SnapshotParameters();
			parm.setFill(Color.TRANSPARENT);

	    	WritableImage img = snapshot(parm, null);
	    	
			ImageIO.write(
				SwingFXUtils.fromFXImage(img, null), 
				fmt, 
				new File(fileName)
			);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
}
