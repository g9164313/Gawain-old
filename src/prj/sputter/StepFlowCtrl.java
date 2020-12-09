package prj.sputter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.FloatProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepFlowCtrl extends Stepper {

	public static ModCouple dev;
	
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
		final FloatProperty prop,
		final TextField box,
		final Label txt		
	) {
		txt.setText("----");
		txt.setUserData(null);
		String sval = box.getText();		
		if(sval.length()==0) { 
			return;
		}
		try {
			float val = Float.valueOf(sval);
			prop.set(val);
			txt.setUserData(new DescriptiveStatistics(5));
		}catch(NumberFormatException e) {
			e.printStackTrace();
		}
	};
	
	private boolean reach_stable(
		final FloatProperty prop,
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
			if(Math.abs(val-avg)<0.5) {
				return true;
			}
		}catch(NumberFormatException e) {
			e.printStackTrace();			
		}
		return false;
	}
	
	final Runnable op1 = ()->{
		status.setText("準備中");
		arrange_prop(dev.SV_FlowAr,box_sval[0],txt_mean[0]);
		arrange_prop(dev.SV_FlowO2,box_sval[1],txt_mean[1]);
		arrange_prop(dev.SV_FlowN2,box_sval[2],txt_mean[2]);
		next_step();
	};
	
	final Runnable op2 = ()->{
		status.setText("穩定中");
		boolean flg = true;
		flg &= reach_stable(dev.PV_FlowAr,box_sval[0],txt_mean[0]);
		flg &= reach_stable(dev.PV_FlowO2,box_sval[1],txt_mean[1]);
		flg &= reach_stable(dev.PV_FlowN2,box_sval[2],txt_mean[2]);		
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
			obj.setPrefWidth(83); 
		}
		for(Label obj:txt_mean) { 
			obj.setPrefWidth(83);
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
	
	private static void set_flow_value(
		final FloatProperty prop,
		final TextField box
	) {
		final String txt = box.getText().trim();
		try {
			prop.set(Float.valueOf(txt));
		}catch(NumberFormatException e) {
			Misc.loge("Wrong Format:%s", txt);
		}
	}
	
	public static Pane genCtrlPanel() {
				
		final JFXTextField[] box = {
			new JFXTextField(),
			new JFXTextField(),
			new JFXTextField()
		};
		for(JFXTextField obj:box) { 
			obj.setPrefWidth(63);
		}
		box[0].setOnAction(e->set_flow_value(dev.SV_FlowAr,box[0]));
		box[1].setOnAction(e->set_flow_value(dev.SV_FlowO2,box[1]));
		box[2].setOnAction(e->set_flow_value(dev.SV_FlowN2,box[2]));
		
		final Label[] txt = {
			new Label(),
			new Label(),
			new Label(),
		};
		for(Label obj:txt) { 
			obj.setPrefWidth(63);
		}
		txt[0].textProperty().bind(dev.PV_FlowAr.asString("%5.2f"));
		txt[1].textProperty().bind(dev.PV_FlowO2.asString("%5.2f"));
		txt[2].textProperty().bind(dev.PV_FlowN2.asString("%5.2f"));
		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("box-pad");
		lay.add(new Label("微量氣體（SCCM）"),0,0,3,1);
		lay.addColumn(0, new Label("Ar"), new Label("O2"), new Label("N2"));
		lay.addColumn(1, box);
		lay.addColumn(2, txt);
		return lay; 
	}
	
	/*private static final String UNIT_VOLT = "Volt";
	private static final String UNIT_SCCM = "SCCM";
	private static final String FMT_FLOAT = "%5.3f";
	
	private interface BindUnit {
		void action(
			int cid,
			float max_sccm,
			ToggleGroup grp,			
			Label txt_pv,
			Label txt_sv
		);
	}

	private interface VSetting {
		void action(
			int cid,
			float max_sccm,
			ToggleGroup grp,
			TextField box
		);
	};
	
	private final static BindUnit bindunit_5850E = new BindUnit() {
		@Override
		public void action(
			int cid, 
			float max_sccm, 
			ToggleGroup grp, 
			Label txt_pv, 
			Label txt_sv
		) {
			String unit = ((RadioButton)grp.getSelectedToggle()).getText();	
			
			FloatProperty pv_v = dev.getAnalogIn(cid);
			FloatProperty sv_v = dev.getAnalogOut(cid);
			txt_pv.textProperty().unbind();
			txt_sv.textProperty().unbind();
			float fac = 1.f;
			if(unit.equals(UNIT_VOLT)==true) {
				fac = 1f;
			}else if(unit.equals(UNIT_SCCM)==true) {
				fac = max_sccm / 5f;
			}
			txt_pv.textProperty().bind(pv_v.multiply(fac).asString(FMT_FLOAT));
			txt_sv.textProperty().bind(sv_v.multiply(fac).asString(FMT_FLOAT));
		}
	};
	
	private final static VSetting settting_5850E = new VSetting() {
		@Override
		public void action(
			int cid,
			float max_sccm,			
			ToggleGroup grp,
			TextField box
		) {
			String unit = ((RadioButton)grp.getSelectedToggle()).getText();
			float val = 0f;
			if(box==null) {
				PadTouch pad = new PadTouch('f',unit);
				Optional<String> opt = pad.showAndWait();			
				if(opt.isPresent()==false) {
					return;
				}
				val = Float.valueOf(opt.get());
			}else {
				String txt = box.getText().trim();
				if(txt.length()==0){
					return;
				}
				try {
					val = Float.valueOf(txt);
				}catch(NumberFormatException err) {
					Misc.loge("Wrong Flow Setting:%s", txt);
					return;
				}
			}
			if(unit.equals(UNIT_VOLT)==true) {
				if(val>5.f){ 
					return; 
				}
			}else if(unit.equals(UNIT_SCCM)==true) {
				if(val>max_sccm){ 
					return; 
				}
				val = (val * 5f) / max_sccm;
			}
			dev.asyncAanlogOut(cid,val);
		}
	};
	
	private static Pane gen_panel(
		final String title,
		final int cid,
		final float max_sccm,
		final BindUnit hook1,
		final VSetting hook2
	) {		
		final Label[] txt = {
			new Label(title),
			new Label("PV"), new Label(),
			new Label("SV"), new Label(),
		};
		for(Label obj:txt) {
			obj.getStyleClass().add("font-size5");
			obj.setMaxWidth(Double.MAX_VALUE);
		}
		txt[2].setAlignment(Pos.BASELINE_RIGHT);
		txt[4].setAlignment(Pos.BASELINE_RIGHT);
		
		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton[] rad = {
			new JFXRadioButton(UNIT_VOLT),
			new JFXRadioButton(UNIT_SCCM)
		};		
		rad[0].setToggleGroup(grp);
		rad[1].setToggleGroup(grp);	
		rad[0].setOnAction(e->hook1.action(cid,max_sccm,grp,txt[2],txt[4]));
		rad[1].setOnAction(e->hook1.action(cid,max_sccm,grp,txt[2],txt[4]));

		//speed-up input-box
		final JFXTextField box = new JFXTextField();
		box.setId("speed-box");
		box.setMaxWidth(Double.MAX_VALUE);
		box.setOnAction((act)->hook2.action(cid,max_sccm,grp,box));
		
		//default PV(Practice Value)
		grp.selectToggle(rad[1]);
		((JFXRadioButton)grp.getSelectedToggle()).getOnAction().handle(null);
		
		//label for SV(Setting Value)
		txt[3].setOnMouseClicked(e->hook2.action(cid,max_sccm,grp,null));
		txt[4].setOnMouseClicked(txt[3].getOnMouseClicked());
				
		GridPane lay = new GridPane();
		lay.getStyleClass().add("box-pad");
		lay.add(txt[0], 0, 0);
		lay.add(box, 1, 0, 2, 1);
		//lay.addRow(1, txt[3], txt[4]);
		lay.addRow(1, txt[1], txt[2], rad[1]);
		return lay; 
	}
	
	public static Pane genPanel(
		final String title,
		final int dev_id,
		final float max_sccm
	) {
		return gen_panel(
			title, dev_id, max_sccm,
			bindunit_5850E,
			settting_5850E
		);
	}*/
}


