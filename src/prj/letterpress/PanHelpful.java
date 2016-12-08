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
		
		Button btnAlign = PanBase.genButton0("標靶對位","selection.png");
		btnAlign.setOnAction(Entry.inst.prvw.filterAlign);
		
		Button btnScan = PanBase.genButton0("晶圓曝光","blur.png");
		btnScan.setOnAction(tsk_scanning);

		Button btnGoing = PanBase.genButton0("快速執行","run.png");		
		btnGoing.setOnAction(TskAction.createKeyframe(true,
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
		
		Button btnDemo1 = PanBase.genButton0("demo-1","bike.png");
		
		Button btnDemo2 = PanBase.genButton0("demo-2","bike.png");
		btnDemo2.setOnAction(TskAction.createKeyframe(true,			
			tsk_scanning,
			tsk_holder,
			event->{
				PanOption.enableAOI(true);
				Entry.pager.getSelectionModel().select(0);
			},
			tsk_holder,
			event->{
				PanOption.enableAOI(false);
				Entry.pager.getSelectionModel().select(1);
			}
		));
		
		Button btnHome = PanBase.genButton0("校正原點","arrow-compress-all.png");
		btnHome.setOnAction(tsk_gohome);

		Button btnHold = PanBase.genButton0("進/退片","coffee-to-go.png");
		btnHold.setOnAction(tsk_holder);
		
		Button btnClose = PanBase.genButton0("關閉程式","close.png");	
		btnClose.setOnAction(event->Entry.inst.dismiss());
		
		lay.addRow(0, btnAlign, btnScan, btnGoing, btnDemo1);
		lay.addRow(2, btnHome, btnHold, btnClose, btnDemo2);
		return lay;
	}

}
