package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import narl.itrc.Misc;

public class DevAdam4024 extends DevAdam {

	public DevAdam4024(final String address) {
		TAG = "ADAM4024";
		AA = address;
		//this module range code is special~~~
		range_type.bi_put("30", z20mA);
		range_type.bi_put("31", r4to20mA);
		range_type.bi_put("32", d10V);
	}
	
	private final static String STG_INIT = "init";

	@Override
	public void afterOpen() {
		addState(STG_INIT, ()->{
			get_configuration();
			for(Channel ch:aout) {
				get_type_range(ch);
				read_last_output(ch);
				((AOut)ch).initial_field();
			}
			nextState("");
		});
		playFlow(STG_INIT);
	}

	@Override
	public void beforeClose() {
	}
	//---------------------
	
	//there are 4 analogy-output channel in ADAM4024
	private class AOut extends Channel {
		
		JFXSlider bar = new JFXSlider();
		JFXTextField box = new JFXTextField();
		
		public AOut(int in) {
			super(in);
			title.set("通道"+id);
			
		}
		
		void reset_value() {Misc.invokeLater(()->{
			box.setText(txt.get());
			bar.setValue(val.get());
		});}
		
		void initial_field() {
			Misc.invokeLater(()->{				
				reset_value();
				
				bar.minProperty().bind(min);
				bar.maxProperty().bind(max);
				bar.blockIncrementProperty().bind(max.subtract(min).divide(10));
				
				//After setting the initial value, we can add listener
				bar.valueProperty().addListener((obv,oldVal,newVal)->{
					if(bar.isFocused()==false) {
						return;
					}
					box.setText(String.format("%5.3f", newVal));
					direct_output_data(this,newVal.floatValue());
				});
				box.setPrefWidth(80.);
				box.setOnAction(event->{
					try {
						final float vv = Float.valueOf(box.getText().trim());
						bar.adjustValue(vv);
						direct_output_data(this,vv);
					}catch(NumberFormatException e) {					
					}		
				});
			});
		}
	};

	public final AOut[] aout = {
		new AOut(0), new AOut(1), new AOut(2), new AOut(3),
	};
	
	void read_last_output(Channel ch) {
		final String ans = exec("$"+AA+"6C"+ch.id);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s)%d] unable read last value ", TAG, ch.id);
			return;
		}
		final String txt = ans.substring(3);
		final float val = Misc.txt2float(txt);		
		Misc.invokeLater(()->{
			ch.txt.set(txt);
			ch.val.set(val);			
		});		
	}
	
	void direct_output_data(final Channel ch, final String data) {
		//data format pattern must be "+00.000"
		final String ans = exec("#"+AA+"C"+ch.id+data);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s)%d] unable direct output", TAG, ch.id);
			return;
		}
	}
	void direct_output_data(final Channel ch, final double data) {
		direct_output_data(ch,String.format("%+07.3f", data));
	}
	void direct_output_data(final Channel ch, final float data) {
		direct_output_data(ch,String.format("%+07.3f", data));
	}
	
	public void asyncSetRangeType(
		final AOut aout, 
		final RangeType rng
	) {asyncBreakIn(()->{
		final String code = range_type.dict.get(rng);
		final String ans = exec("$"+AA+"7C"+aout.id+"R"+code);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s)%d] unable set range type", TAG, aout.id);
			return;
		}		
		aout.set_range_type(rng);
		read_last_output(aout);
		aout.reset_value();
	});}
	
	//---------------------
	
	public static Pane genPanel(final DevAdam4024 dev) {

		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		
		for(int i=0; i<dev.aout.length; i++) {
			final AOut aout = dev.aout[i];
			
			Label txt = new Label();
			txt.textProperty().bind(aout.title);
			txt.setPrefWidth(80);
			
			Button btn = new Button();
			btn.textProperty().bind(aout.unit);
			btn.setPrefWidth(30);
			btn.setOnAction(event->{
				//special, we change channel type and range here!!
				final ChoiceDialog<RangeType> dia = new ChoiceDialog<RangeType>(
					z20mA, r4to20mA, d10V
				);
				final Optional<RangeType> opt = dia.showAndWait();
				if(opt.isPresent()==true) {
					dev.asyncSetRangeType(aout,opt.get());
				}
			});	
			
			lay.addRow(i,txt, aout.bar, aout.box, btn);
		}
		return lay;
	}
}
