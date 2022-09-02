package prj.sputter.cargo1;

import com.jfoenix.controls.JFXCheckBox;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.Stepper;
import prj.sputter.DevAdam4024;
import prj.sputter.DevAdam4055;
import prj.sputter.DevAdam4x17;
import prj.sputter.DevCESAR;
import prj.sputter.DevSPIK2k;
import prj.sputter.DevSQM160;

public abstract class StepCommon extends Stepper {

	protected final DevCESAR sar1 = PanMain.sar1;
	protected final DevCESAR sar2 = PanMain.sar2;
	protected final DevSPIK2k spik= PanMain.spik;
	protected final DevSQM160 sqm1= PanMain.sqm1;
	protected final DevAdam4055 adam1 = PanMain.adam1;//shutter 1~3, MFC valve 1~3
	//relay for wire-hub!!!!
	protected final DevAdam4x17 adam3 = PanMain.adam3;//MFC PV:1~3
	protected final DevAdam4024 adam4 = PanMain.adam4;//MFC SV:1~3
	
	final Label[] msg = {
		new Label(), 
		new Label(),
	};
	
	final TextField box_hold = new TextField();//holding time
	final JFXCheckBox chk_cont = new JFXCheckBox("連續");//go-on or continue~
	
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
			lay.addColumn(2+i, objs[i*2+0], objs[i*2+1]);
		}
		if(hold_time==null) {
			return lay;
		}
		final int cc = 2 + objs.length/2;
		lay.add(new Separator(Orientation.VERTICAL), cc+0, 0, 1, 2);
		lay.addColumn(cc+1, new HBox(new Label("維持:"),box_hold), chk_cont);
		return lay;
	}
}
