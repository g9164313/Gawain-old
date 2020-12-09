package prj.shelter;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class StepMeasure extends StepRadiate {

	private final DevAT5350 at5350;
	private final DevCDR06 cdr06;
	
	public StepMeasure(
		final DevHustIO dev1,
		final DevAT5350 dev2,
		final DevCDR06 dev3
	) {
		super(dev1);
		at5350= dev2;
		cdr06 = dev3;
		addRun(op_measure, op_wait);
		title.setText("輻射量測");
	}
	
	private Runnable op_measure = ()->{
		Misc.logv("op-measure");
		next_step();
	};

	private Runnable op_wait = ()->{
		Misc.logv("op-measuring");
		next_step();
	};	
	
	private final Label[] info = {
		new Label("＊＊＊＊＊"),
		new Label("＊＊＊＊＊"),
		new Label("＊＊＊＊＊"),
		new Label(),
		new Label(),
		new Label()
	};

	protected final Label txt_max = info[0]; 
	protected final Label txt_avg = info[1]; 
	protected final Label txt_min = info[2]; 

	protected final Label txt_med = info[3];//median
	protected final Label txt_dev = info[4];//deviation
	protected final Label txt_skw = info[5];//skewness 
	
	@Override
	public Node getContent() {
		GridPane lay = (GridPane)super.getContent();		
		lay.addColumn(5, new Label("最大"), new Label("平均"), new Label("最小"));
		lay.addColumn(6, txt_max, txt_avg, txt_min);
		lay.add(new Separator(Orientation.VERTICAL), 7, 0, 1, 3);
		return lay;
	}	
}
