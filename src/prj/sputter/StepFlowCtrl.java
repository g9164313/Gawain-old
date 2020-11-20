package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.FloatProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import narl.itrc.Stepper;

public class StepFlowCtrl extends Stepper {

	private final ModCouple dev;
	
	public StepFlowCtrl(final ModCouple coupler) {
		dev = coupler;
		set(op1,op2,op3);
	}
	
	private TextField box1, box2;
	
	final Runnable op1 = ()->{
		box1.getOnAction().handle(null);
		next_work();
	};
	final Runnable op2 = ()->{
		box2.getOnAction().handle(null);
		next_work();
	};
	final Runnable op3 = ()->{
		//delay~~~
		next_work();
	};
	
	@Override
	public Node getContent() {
		
		final Pane lay1 = gen_panel(
			"Ar", 2, 100f,
			bindunit_5850E,
			settting_5850E
		);
		box1 = (TextField)lay1.lookup("#speed-box");
		
		final Pane lay2 = gen_panel(
			"O2", 1, 10f,
			bindunit_5850E,
			settting_5850E
		);
		box2 = (TextField)lay2.lookup("#speed-box");
		
		final HBox lay = new HBox();
		lay.getChildren().addAll(
			lay1,
			new Separator(Orientation.VERTICAL),
			lay2,
			new Separator(Orientation.VERTICAL)
		);
		return lay;
	}

	@Override
	public void eventEdit() {
	}
	
	@Override
	public String flatten() {
		return "";
	}

	@Override
	public void expand(String txt) {
	}
	
	//-------------------------------//
	
	private static final String UNIT_VOLT = "Volt";
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
	
	private final BindUnit bindunit_5850E = new BindUnit() {
		@Override
		public void action(
			int cid, 
			float max_sccm, 
			ToggleGroup grp, 
			Label txt_pv, 
			Label txt_sv
		) {
			String unit = ((RadioButton)grp.getSelectedToggle()).getText();	
			
			FloatProperty pv_v = dev.getChannelIn(cid);
			FloatProperty sv_v = dev.getChannelOut(cid);
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
	
	private final VSetting settting_5850E = new VSetting() {
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
	
	private Pane gen_panel(
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
		box.setOnAction((act)->{			
			if(ladder.isWorking()==false) {
				return;
			}			
			hook2.action(cid,max_sccm,grp,box);
		});
		
		//default PV(Practice Value)
		grp.selectToggle(rad[1]);
		((JFXRadioButton)grp.getSelectedToggle()).getOnAction().handle(null);
		
		//label for SV(Setting Value)
		txt[3].setOnMouseClicked(e->hook2.action(cid,max_sccm,grp,null));
		txt[4].setOnMouseClicked(txt[3].getOnMouseClicked());
				
		GridPane lay = new GridPane();
		lay.getStyleClass().add("box-pad");
		lay.addColumn(0, txt[0], txt[1], txt[3]);
		lay.addColumn(1, box   , txt[2], txt[4]);
		lay.addColumn(2, new Label(), rad[0], rad[1]);
		return lay; 
	}
}

