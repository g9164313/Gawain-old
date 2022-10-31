package prj.sputter;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import narl.itrc.Misc;
import narl.itrc.PadChoise;
import narl.itrc.PadNumber;

/**
 * ADAM4024: 4 analogy-output channel
 * @author qq
 *
 */
public class DevAdam4024 extends DevAdam {

	public DevAdam4024(final int address) {
		TAG= "ADAM4024";
		AA = String.format("%02X", address);
		//this module has special range codes
		range_type.bi_put("30", z20mA);
		range_type.bi_put("31", r4to20mA);
		range_type.bi_put("32", d10V);
	}
	public DevAdam4024(
		final int address,
		final RangeType... init_rng
	) {
		this(address);
		init_range = init_rng;
	}
	
	private final static String STG_INIT = "init";

	@Override
	public void afterOpen() {
		addState(STG_INIT, ()->{
			get_configuration();
			init_range_type(aout);
			if(init_range!=null) {
				exec("$AA4");
			}
			for(Channel ch:aout) {
				get_range_type(ch);
				read_last_output(ch);
			}			
			nextState("");
		});
		playFlow(STG_INIT);
	}

	@Override
	public void beforeClose() {
	}
	//---------------------
	
	private final static String fmt_val = "%+07.3f";
	
	//there are 4 analogy-output channel in ADAM4024
	public class AOut extends Channel {
		//JFXSlider bar = new JFXSlider();
		//JFXTextField box = new JFXTextField();

		public AOut(int in) {
			super(in);
			title.set("通道"+id);
		}
		
		public void assign(float val) {
			asyncDirectOuput(this,val);
		}
		
		Pane gen_layout() {
			final double size_h = 47.;
			
			Label txt_name = new Label();
			txt_name.textProperty().bind(title);
			txt_name.setPrefSize(80, size_h);
			
			JFXButton btn_value= new JFXButton();
			btn_value.getStyleClass().add("btn-raised-10");
			btn_value.textProperty().bind(txt);
			btn_value.setPrefSize(100., size_h);
			btn_value.setOnAction(event->{
				new PadNumber()
				.subset(null, min.get(), max.get()).format(fmt_val).popup(opt->{
					//Misc.logv("number=%s, (%.1f,%.1f)", opt, min.get(), max.get());
					asyncDirectOuput(this,opt);
				});
			});
			
			JFXButton btn_unit = new JFXButton();
			btn_unit.getStyleClass().add("btn-raised-10");
			btn_unit.textProperty().bind(unit);
			btn_unit.setPrefSize(64., size_h);
			btn_unit.setOnAction(event->{
				new PadChoise<RangeType>(z20mA, r4to20mA, d10V)
				.assign(range_type).popup(opt->{
					//Misc.logv("choise=%s", opt.toString());
					asyncSetRangeType(this,opt);					
				});
			});
			
			HBox lay = new HBox(txt_name,btn_value,btn_unit);
			lay.setAlignment(Pos.CENTER_LEFT);
			return lay;
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
		Application.invokeLater(()->{
			ch.txt.set(txt);
			ch.val.set(val);			
		});		
	}
	
	void direct_output_data(final Channel ch, final String data) {
		//data format pattern must be "+00.000"
		final String ans = exec("#"+AA+"C"+ch.id+data);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s)%d] unable direct output", TAG, ch.id);
		}else {
			Application.invokeLater(()->{
				ch.txt.set(data);
				ch.val.set(Float.parseFloat(data));			
			});
			Misc.logv("[%s)%d] out=%s", TAG, ch.id, data);
		}
	}
	void direct_output_data(final Channel ch, final double data) {
		direct_output_data(ch,String.format(fmt_val, data));
	}
	void direct_output_data(final Channel ch, final float data) {
		direct_output_data(ch,String.format(fmt_val, data));
	}
	//---------------------
	
	public void asyncDirectOuput(
		final int idx, 
		final String data
	) {
		if(idx>=aout.length||idx<0) { return; }
		asyncBreakIn(()->{
		direct_output_data(aout[idx],data);
	});}
	public void asyncDirectOuput(
		final int idx, 
		final float data
	) {
		if(idx>=aout.length||idx<0) { return; }
		asyncBreakIn(()->{
		direct_output_data(aout[idx],data);
	});}
	
	public void asyncDirectOuput(
		final AOut aout, 
		final String data
	) {asyncBreakIn(()->{
		direct_output_data(aout,data);
	});}
	public void asyncDirectOuput(
		final AOut aout, 
		final float data
	) {asyncBreakIn(()->{
		direct_output_data(aout,data);
	});}
	
	public void asyncDirectOuput(
		final Float... val
	) {asyncBreakIn(()->{
		final int size = (val.length>aout.length)?(aout.length):(val.length);
		for(int i=0; i<size; i++) {
			if(val[i]==null) {
				continue;
			}
			direct_output_data(aout[i],val[i].floatValue());
		}		
	});}
		
	
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
		aout.update_property(rng);
		read_last_output(aout);
	});}
	//---------------------
	
	public static Pane genPanel(final DevAdam4024 dev) {

		FlowPane lay = new FlowPane();
		lay.getStyleClass().addAll("box-pad");
		
		for(AOut aout:dev.aout) {
			lay.getChildren().add(aout.gen_layout());
		}
		return lay;
	}
}
