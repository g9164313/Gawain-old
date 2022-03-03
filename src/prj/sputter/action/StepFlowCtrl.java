package prj.sputter.action;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;

public class StepFlowCtrl extends Bumper {

	public StepFlowCtrl() {
		set(op1,op2,op3);
	}
	
	private Label status = new Label();
	private TextField[] box_sval = {
		new TextField(), 
		new TextField(), 
		new TextField()
	};
	private Label[] txt_mean = {
		new Label(),
		new Label(),
		new Label()
	};
	
	private void arrange_prop(
		final TextField box,
		final Label txt		
	) {
		txt.setText("----");
		txt.setUserData(null);	
		if(box.getText().length()!=0) {
			//FIXME:here sampling~~~
			txt.setUserData(new DescriptiveStatistics(5));
		}
	};
	
	private boolean reach_stable(
		final ReadOnlyFloatProperty prop,
		final TextField box,
		final Label txt
	) {
		DescriptiveStatistics sts = (DescriptiveStatistics)txt.getUserData();
		if(sts==null) {
			return true;
		}else {
			sts.addValue(prop.doubleValue());
		}
		String sval = box.getText();		
		if(sval.length()==0) { 
			return true;
		}
		try {
			float val = Float.valueOf(sval);
			float avg = (float)sts.getMean();
			txt.setText(String.format("%.2fsccm",avg));
			//FIXME:how to decide stable state?
			if(Math.abs(val-avg)<0.7) {
				return true;
			}
		}catch(NumberFormatException e) {
			e.printStackTrace();			
		}
		return false;
	}
	
	final Runnable op1 = ()->{
		status.setText("準備氣體");
		arrange_prop(box_sval[0],txt_mean[0]);
		arrange_prop(box_sval[1],txt_mean[1]);
		arrange_prop(box_sval[2],txt_mean[2]);
		coup.asyncSetMassFlow(
			box_sval[0].getText(), 
			box_sval[2].getText(), 
			box_sval[1].getText()
		);
		next_step();
	};
		
	final Runnable op2 = ()->{
		status.setText("穩定中");
		boolean flg = true;
		flg &= reach_stable(coup.PV_FlowAr,box_sval[0],txt_mean[0]);
		flg &= reach_stable(coup.PV_FlowO2,box_sval[1],txt_mean[1]);
		flg &= reach_stable(coup.PV_FlowN2,box_sval[2],txt_mean[2]);		
		if(flg==true) {
			next_step();
		}else {
			hold_step();
		}
	};
	
	final Runnable op3 = ()->{
		//clear message
		status.setText("");
		for(TextField obj:box_sval) { obj.setUserData(null); }
		next_step();
	};
	
	@Override
	public Node getContent() {
		
		status.setId("status");
		status.setPrefWidth(150);
		
		for(TextField obj:box_sval) { 
			obj.setPrefWidth(97); 
		}
		for(Label obj:txt_mean) { 
			obj.setPrefWidth(97);
		}
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,new Label("流量控制"),status);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		lay.addRow(0, new Label("Ar"), new Label("O2"), new Label("N2"));
		lay.addRow(1, box_sval);
		lay.addRow(2, txt_mean);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "MFC-1";
	private static final String TAG1 = "MFC-2";
	private static final String TAG2 = "MFC-3";
	
	@Override
	public String flatten() {		
		return String.format(
			"%s:%s,  %s:%s,  %s:%s", 
			TAG0, box_sval[0].getText(), 
			TAG1, box_sval[1].getText(), 
			TAG2, box_sval[2].getText()
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\p{Space}]+[:]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",txt);
			return;
		}
		String[] arg = txt.split(":|,");
		for(int i=0; i<arg.length; i+=2){
			final String tag = arg[i+0].trim();
			if(i+1>=arg.length){
				break;
			}
			final String val = arg[i+1].trim();
			if(tag.equals(TAG0)==true){
				box_sval[0].setText(val);
			}else if(tag.equals(TAG1)==true) {
				box_sval[1].setText(val);
			}else if(tag.equals(TAG2)==true) {
				box_sval[2].setText(val);
			}
		}
	}
	//-------------------------------------//
	
	public static Pane genCtrlPanel() {
				
		final JFXTextField[] box = {
			new JFXTextField(),//Ar
			new JFXTextField(),//N2
			new JFXTextField() //O2
		};
		for(JFXTextField obj:box) { 
			obj.setPrefWidth(63);
		}
		box[0].setOnAction(e->coup.asyncSetMassFlow(box[0].getText(),"",""));
		box[1].setOnAction(e->coup.asyncSetMassFlow("",box[1].getText(),""));
		box[2].setOnAction(e->coup.asyncSetMassFlow("","",box[2].getText()));
		
		final Label[] txt = {
			new Label(),
			new Label(),
			new Label(),
		};
		for(Label obj:txt) { 
			obj.setPrefWidth(63);
		}
		txt[0].textProperty().bind(coup.PV_FlowAr.asString("%5.2f"));
		txt[1].textProperty().bind(coup.PV_FlowN2.asString("%5.2f"));
		txt[2].textProperty().bind(coup.PV_FlowO2.asString("%5.2f"));
		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("box-pad");
		lay.add(new Label("微量氣體（SCCM）"),0,0,3,1);
		lay.addColumn(0, new Label("Ar"), new Label("N2"), new Label("O2"));
		lay.addColumn(1, txt);
		lay.addColumn(2, box);		
		return lay; 
	}
}


