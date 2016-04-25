package prj.reheating;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.WindowEvent;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class Entry extends PanBase {
	
	public Entry(){
		panTitle="真空熱循環儀";
	}
	
	public PanFMenu panMenu = new PanFMenu();
	public PanControl panCtrl = new PanControl(this);
	public PanInform panInfo = new PanInform();
	public PanAction panAct = new PanAction(this);
	
	public DevHDxx pidT1 = new DevHDxx();//爐管1
	public DevHDxx pidT2 = new DevHDxx();//爐管2
	public DevHDxx pidB1 = new DevHDxx();//油桶	
	public DevDIO3232 padIO = new DevDIO3232();
	
	@Override
	protected void eventShown(WindowEvent event){
		final int PERIOD=500;//million second
		
		padIO.open(0);
		pidT1.open(Gawain.prop.getProperty("pid2",""));
		pidT2.open(Gawain.prop.getProperty("pid3",""));
		pidB1.open(Gawain.prop.getProperty("pid1",""));
		panInfo.setPeriod(PERIOD+"ms","10sec");
		
		panInfo.initTubeGauge(pidT1.getLower(), pidT1.getUpper(),  pidT1.getValue());
		panInfo.initTubxGauge(pidT2.getLower(), pidT2.getUpper(), pidT2.getValue());
		panInfo.initBuckGauge(pidB1.getLower(), pidB1.getUpper(), pidB1.getValue());
		panInfo.initPressGauge(0, 100, 50);
		
		watchStart(PERIOD);
	}
	
	@Override
	protected void eventWatch(int cnt){
		if(padIO.panel.isVisible()==true){
			padIO.panel.updateInputBox();
		}
		panInfo.updateInfo(this);
		panAct.eventLooper();
	}
	
	private PanFMenu.Showing eventPID = new PanFMenu.Showing(){
		@Override
		public void callback() {
			//first, refresh data from device~~~
			pidB1.panel.refreshData();
			pidT1.panel.refreshData();
			pidT2.panel.refreshData();
		}
	};
	
	@Override
	public Parent layout() {
		BorderPane root = new BorderPane();
		Pane panPID = genGridPack(
			3,root,
			PanBase.decorate("PID1",pidB1.panel), 
			PanBase.decorate("PID2",pidT1.panel),
			PanBase.decorate("PID3",pidT2.panel)
		);
		panMenu.setEachPane(
			root,
			"主畫面",panInfo,null,
			"溫度控制器",panPID,eventPID,
			"輸位輸出/入",padIO.panel,null
		);
		root.setTop(panMenu);
		root.setLeft(panCtrl);
		root.setCenter(panInfo);
		root.setRight(panAct);
		
		//adjust layout size~~~~
		//panAct.prefWidthProperty().bind(root.widthProperty().multiply(0.23));
		//panAct.bindHeight(root.heightProperty());
		
		pidB1.panel.lst.prefWidthProperty().bind(root.widthProperty().divide(3));
		pidT1.panel.lst.prefWidthProperty().bind(root.widthProperty().divide(3));
		pidT2.panel.lst.prefWidthProperty().bind(root.widthProperty().divide(3));
		
		return root;
	}
}
