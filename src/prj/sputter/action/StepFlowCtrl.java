package prj.sputter.action;

import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

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
		set(op1,op2);
		for(int i=0; i<box_sval.length; i++) {
			box_sval[i].setUserData(sval[i]);
			sval[i].box = box_sval[i];
		}
	}
	
	private class SetValue {
		TextField box = null;
		float goal = -1f;
		ArrayList<Float> roll = new ArrayList<Float>();
			
		void init() {
			goal = -1f;
			roll.clear();
			final String sccm = box.getText();
			if(sccm.length()!=0) {
				try{
					float val = Float.valueOf(sccm);
					box.getStyleClass().remove("error");
					goal = val;
					return;
				}catch(NumberFormatException e) {
					box.getStyleClass().add("error");
				}
			}
			goal = -1f;
		}
		boolean reach_stable(final ReadOnlyFloatProperty prop) {
			if(goal<=0f) { 
				return true;
			}
			roll.add(prop.get());
			if(roll.size()<25) {
				return false;
			}
			roll.remove(0);//skip old data~~~
			double[] sample = get_sample_set();
			DescriptiveStatistics stats = new DescriptiveStatistics(sample);
			if(stats.getN()>=2) {
				double p_val = TestUtils.tTest(goal, sample);
				//p_val = Math.abs(p_val);
				Misc.logv(String.format(
					"AVG: %5.2f, DEV: %5.3f, P:%.3f", 
					stats.getMean(), stats.getStandardDeviation(), p_val
				));				
				if(p_val<=0.3){
					return true;
				}
			}		
			return false;
		}
		
		double[] get_sample_set() {
			ArrayList<Float> buf = new ArrayList<Float>();
			for(Float v:roll) {
				if(buf.contains(v)==false) {
					buf.add(v);
				}
			}
			double[] res;
			final int len = buf.size();
			if(len<2) {
				res = new double[2];
				res[0] = buf.get(0);
				res[1] = buf.get(0);
			}else {
				res = new double[len];
				for(int i=0; i<len; i++) {
					res[i] = buf.get(i);
				}
			}
			return res; 
		}		
	};
	private SetValue[] sval = {
		new SetValue(),//Ar
		new SetValue(),//N2
		new SetValue(),//O2
	};
	private TextField[] box_sval = {
		new TextField(),//Ar 
		new TextField(),//N2 
		new TextField(),//O2
	};
	
	final String action_name = "準備氣體";
	
	final Runnable op1 = ()->{
		show_mesg(action_name);
		for(SetValue sv:sval) {
			sv.init();
		}
		wait_async();
		coup.asyncBreakIn(()->{
			coup.set_all_mass_flow(
				sval[0].goal, 
				sval[1].goal, 
				sval[2].goal
			);
			notify_async();
		});
	};
		
	final Runnable op2 = ()->{
		show_mesg("穩定中");
		boolean flg = true;
		flg &= sval[0].reach_stable(coup.PV_FlowAr);
		flg &= sval[1].reach_stable(coup.PV_FlowN2);
		flg &= sval[2].reach_stable(coup.PV_FlowO2);		
		if(flg==true) {
			show_mesg(action_name);
			next_step();
		}else {
			hold_step();
		}
	};
	
	@Override
	public Node getContent() {
		msg[0].setText("準備氣體");
		
		for(TextField obj:box_sval) { 
			obj.setPrefWidth(83); 
		}
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");		
		lay.addColumn(0, msg[0]);		
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 1);
		lay.addRow(0, 
			new Label("Ar"), box_sval[0],
			new Label("N2"), box_sval[1],
			new Label("O2"), box_sval[2]);
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
		box[0].setOnAction(event->{
			try {
				final String txt = box[0].getText();
				final float val = Float.valueOf(txt);
				box[0].getStyleClass().remove("error");
				Misc.logv("手動調整Ar=%s", txt);
				coup.asyncSetMassFlow(val,-1f,-1f);
			}catch(NumberFormatException e) {
				box[0].getStyleClass().add("error");
			}
		});
		box[1].setOnAction(event->{
			try {
				final String txt = box[1].getText();
				final float val = Float.valueOf(txt);
				box[1].getStyleClass().remove("error");
				Misc.logv("手動調整N2=%s", txt);
				coup.asyncSetMassFlow(-1f,val,-1f);
			}catch(NumberFormatException e) {
				box[1].getStyleClass().add("error");
			}
		});
		box[2].setOnAction(event->{
			try {
				final String txt = box[2].getText();
				final float val = Float.valueOf(txt);
				box[2].getStyleClass().remove("error");
				Misc.logv("手動調整O2=%s", txt);
				coup.asyncSetMassFlow(-1f,-1f,val);
			}catch(NumberFormatException e) {
				box[2].getStyleClass().add("error");
			}
		});
		
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


