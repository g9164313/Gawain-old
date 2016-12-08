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
		btnGoing.setOnAction(TskAction.createKeyframe(false,
			Entry.inst.prvw.filterAlign,
			event->PanOption.enableAOI(false),
			tsk_scanning,
			event->PanOption.enableAOI(true)
		));
		
		Button btnDemo1 = PanBase.genButton0("demo-expo","bike.png");
		btnDemo1.setOnAction(TskAction.createKeyframe(true,
			event->PanOption.enableAOI(false),
			tsk_scanning,
			event->PanOption.enableAOI(true),
			tsk_gohome
		));
		
		Button btnDemo2 = PanBase.genButton0("demo-full","bike.png");
		btnDemo2.setOnAction(TskAction.createKeyframe(true,
			Entry.inst.prvw.filterAlign,
			event->PanOption.enableAOI(false),
			tsk_scanning,
			event->PanOption.enableAOI(true),
			tsk_gohome
		));

		Button btnOrigin = PanBase.genButton0("校正原點","arrow-compress-all.png");
		btnOrigin.setOnAction(tsk_gohome);

		Button btnHolder = PanBase.genButton0("進/退片","coffee-to-go.png");
		btnHolder.setOnAction(tsk_holder);
		
		Button btnClose = PanBase.genButton0("關閉程式","close.png");	
		btnClose.setOnAction(event->Entry.inst.dismiss());
		
		Button btnDummy1 = PanBase.genButton0("----",null);
		btnDummy1.setPrefHeight(43);
		Button btnDummy2 = PanBase.genButton0("----",null);
		btnDummy2.setPrefHeight(43);
		
		lay.addRow(0, btnOrigin, btnAlign, btnScan, btnGoing, btnDummy1);
		lay.addRow(2, btnHolder, btnDummy2, btnDemo1,btnDemo2,btnClose);
		return lay;
	}

}
