package prj.letterpress;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;
import narl.itrc.TskAction;

public class PanHelpful extends PanDecorate {

	public PanHelpful(){
		super("Shortcut");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Node layoutBody() {
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
		
		final TskAction tsk_gohome = new TskGoHome(null);
		final TskAction tsk_holder = new TskHolder(null);
		final TskAction tsk_scanning = new TskScanning(Entry.inst.wmap,null);
		
		Button btnHome = PanBase.genButton0("原點校正","arrow-compress-all.png");
		btnHome.setOnAction(tsk_gohome);

		Button btnAlign = PanBase.genButton0("標靶對位","selection.png");
		btnAlign.setOnAction(Entry.inst.prvw.filterAlign);
		
		Button btnScan = PanBase.genButton0("晶圓曝光","blur.png");
		btnScan.setOnAction(tsk_scanning);

		Button btnGoing = PanBase.genButton0("快速執行","run.png");		
		btnGoing.setOnAction(TskAction.createKeyframe(
			Entry.inst.prvw.filterAlign,
			event->{
				PanOption.enableAOI(false);
				Entry.pager.getSelectionModel().select(1);
			},
			tsk_scanning,
			event->{
				PanOption.enableAOI(true);
				Entry.pager.getSelectionModel().select(0);
			}
		));
		
		Button btnHold = PanBase.genButton0("進/退片","coffee-to-go.png");
		btnHold.setOnAction(tsk_holder);
		
		Button btnClose = PanBase.genButton0("關閉程式","close.png");	
		btnClose.setOnAction(event->Entry.inst.dismiss());
		
		lay.addRow(0, btnHome, btnAlign, btnScan);
		lay.addRow(1, btnHold, btnGoing, btnClose);
		return lay;
	}

}
