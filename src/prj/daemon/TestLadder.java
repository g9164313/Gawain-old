package prj.daemon;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class TestLadder extends PanBase {

	Ladder ladder = new Ladder();
	
	public static class stp_count extends Stepper {
		
		final Label label = new Label();
		
		public stp_count(final String title){
			label.setText(title);
			set(gen_op(1),gen_op(2),gen_op(3));
			//set(op11,op12,op13,op14,op15);
		}
		void show_mesg(final int idx) {
			String txt = label.getText();
			int i = txt.indexOf(".");
			if(i>=0) {
				txt = txt.substring(0,i);
			}
			txt = txt + "." + idx;
			Misc.logv(txt);
			label.setText(txt);
		}
		
		Runnable gen_op(final int idx) {			
			return new Runnable() {
				@Override
				public void run() {
					show_mesg(idx);
					next_step();
				}
			};
		}
		
		int hold;
		Runnable op11 = ()->{
			show_mesg(11);
			hold = 3;
			next_step();
		};
		Runnable op12 = ()->{
			show_mesg(12);
			hold--;
			if(hold>0) {
				hold_step();
			}else {
				hold = 2;//reset counter~~~
				next_step();
			}
		};
		Runnable op13 = ()->{
			show_mesg(13);
			next_step();
		};
		Runnable op14 = ()->{
			show_mesg(14);
			next_step();
		};
		Runnable op15 = ()->{
			show_mesg(15);
			hold--;
			if(hold>0) {				
				next_step(-2);
			}else {				
				next_step();
			}			
		};
		@Override
		public Node getContent() {
			label.getStyleClass().add("box-border");
			label.setMinWidth(100);
			return label;
		}
		@Override
		public void eventEdit() { }
		@Override
		public String flatten() { return ""; }
		@Override
		public void expand(String txt) { }		
	};
	
	public static class aaa extends stp_count {
		//default constructor must be 'public'.
		//class must be'static'.
		public aaa() { super("aaa"); }
	};
	public static class bbb extends stp_count {
		public bbb() { super("bbb"); }
	};
	public static class ccc extends stp_count {
		public ccc() { super("ccc"); }
	};

	public TestLadder(final Stage stg) {
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		ladder.addStep("aaa", aaa.class);
		ladder.addStep("bbb", bbb.class);
		ladder.addStep("ccc", ccc.class);

		final BorderPane lay = new BorderPane();
		lay.setCenter(ladder);
		return lay;
	}
}
