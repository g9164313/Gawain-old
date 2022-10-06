package prj.sputter.cargo1;

import com.jfoenix.controls.JFXCheckBox;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.Misc;
import narl.itrc.Stepper;
import prj.sputter.DevAdam4024;
import prj.sputter.DevAdam4055;
import prj.sputter.DevAdam4068;
import prj.sputter.DevAdam4x17;
import prj.sputter.DevSPIK2k;
import prj.sputter.DevSQM160;

public abstract class StepCommon extends Stepper {

	protected final PortCesar sar1= PanMain.sar1;
	protected final PortCesar sar2= PanMain.sar2;	
	protected final DevSPIK2k spik= PanMain.spik;
	protected final DevSQM160 sqm1= PanMain.sqm1;
		
	//Shutter 1~3 --> DO0(上:true->開),1(左下),2(右下)
	//MFC valve 1~3 --> DO3,4,5
	//SQM160 valid, shutter-->DI0,1
	//CESAR-1 Ready, Error ON-->DI2,3,4
	//CESAR-2 Ready, Error ON-->DI5,6,7
	protected final DevAdam4055 adam1 = PanMain.adam1;
	//Gun1~3 --> RL1~3
	//CESAR-1 Mode_A --> RL4, RF_ON --> RL5
	//CESAR-2 Mode_A --> RL6, RF_ON --> RL7
	protected final DevAdam4068 adam2 = PanMain.adam2;
	//MFC PV:1~3 --> ain0,1,2
	//CESAR-1 forward/reflect power --> ain4,5 0~10v
	//CESAR-2 forward/reflect power --> ain6,7 0~10v
	protected final DevAdam4x17 adam3 = PanMain.adam3;
	//MFC SV:1~3 --> aout1,2,3
	protected final DevAdam4024 adam4 = PanMain.adam4;
	//CESAR-1:DC Set, RF Set --> aout0, aout1
	//CESAR-2:DC Set, RF Set --> aout2, aout3
	protected final DevAdam4024 adam5 = new DevAdam4024(5);
	
	protected final Label[] msg = {
		new Label(), 
		new Label(),
	};
	
	protected final TextField box_hold = new TextField();//holding time
	protected final JFXCheckBox chk_cont= new JFXCheckBox("連續");//go-on or continue~
	
	protected final JFXCheckBox chk_sh2 = new JFXCheckBox("左下檔板");
	protected final JFXCheckBox chk_sh3 = new JFXCheckBox("右下檔板");
	
	protected final Runnable run_holding = ()->{
		final long rem = waiting_time(box_hold.getText());		
		if(rem>0) {
			msg[1].setText("倒數"+Misc.tick2text(rem, true));
		}else {
			msg[1].setText("");
		}
	};
			
	protected Node gen_grid_pane(
		final String title,
		final String hold_time,
		final boolean iscont,
		final Node... objs
	) {
		if(hold_time!=null) {
			box_hold.setPrefWidth(100.);
			box_hold.setText(hold_time);
			chk_cont.setSelected(iscont);
		}

		msg[0].setText(title);
		msg[0].setMinWidth(100.);
		msg[1].setMinWidth(100.);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		for(int i=0; i<objs.length/2; i++) {
			if(objs[i*2+0]==null || objs[i*2+1]==null) {
				lay.add(new Separator(Orientation.VERTICAL), 2+i, 0, 1, 2);
			}else {
				lay.addColumn(2+i, objs[i*2+0], objs[i*2+1]);
			}					
		}
		if(hold_time==null) {
			return lay;
		}
		final int cc = 2 + objs.length/2;
		lay.add(new Separator(Orientation.VERTICAL), cc+0, 0, 1, 2);
		lay.addColumn(cc+1, new HBox(new Label("維持:"),box_hold), chk_cont);
		return lay;
	}

	protected Float box2float(
		final TextField box,
		final Float val
	) {
		final String txt = box.getText().trim();
		if(txt.length()==0) {
			return val;
		}
		try {
			return Float.valueOf(box.getText().trim());
		}catch(NumberFormatException e) {
			return val;
		}
	}
	protected Float clip_float(Float val, final Float min, final Float max) {
		if(val!=null) {
			if(min!=null) {
				if(val<min) { return min; }
			}
			if(max!=null) {
				if(val>max) { return max; }
			}
		}
		return val;
	}
}
