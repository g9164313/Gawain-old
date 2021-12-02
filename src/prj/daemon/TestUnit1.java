package prj.daemon;

import com.jfoenix.controls.JFXButton;

import javafx.concurrent.Task;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.DevBase;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class TestUnit1 extends PanBase {

	public TestUnit1(final Stage stg) {		
	}
	
	private DevShapeoko shko = new DevShapeoko();
	
	private DevLKIF2 lkif = new DevLKIF2(); 
	
	private DevBase dummy = new DevBase() {

		private final static String STG_INIT = "initial";
		private final static String STG_MONT = "monitor";
		
		private void stg_init() {
			Misc.logv("dummy-init");
			nextState(STG_MONT);
		}
		private void stg_mont() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
			Misc.logv("dummy-mont");
		}
		@Override
		public void open() {
			addState(STG_INIT, ()->stg_init()).
			addState(STG_MONT, ()->stg_mont());
			playFlow(STG_INIT);
		}
		@Override
		public void close() {
		}
		@Override
		public boolean isLive() {
			return true;
		}
	};
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		stage().setOnShown(e->{
			/*String txt;
			txt = Gawain.prop().getProperty("shapeoko","");
			if(txt.length()!=0) {
				shko.open(txt);
			}
			if(Gawain.propFlag("USE_IF2")==true) {
				lkif.open();
			}*/
			dummy.open();
		});
		final VBox lay2 = new VBox();
		lay2.getStyleClass().addAll("box-pad");
		lay2.getChildren().addAll(
			DevLKIF2.genPanel(lkif,0),
			DevShapeoko.genPanelInfo(shko)
		);
		
		final TitledPane[] _lay1 = {
			new TitledPane("快速設定",genSpeedButton()),
			new TitledPane("運動平台",DevShapeoko.genPanelMove(shko)),
		};
		final Accordion lay1 = new Accordion(_lay1);
		lay1.setExpandedPane(_lay1[0]);
		
		
        /*Tile gg = TileBuilder.create()
                .skinType(SkinType.GAUGE)
                .prefSize(200, 200)
                .title("Gauge Tile")
                .unit("V")
                .threshold(75)
                .build();*/
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().addAll("box-pad"); 
		lay0.setLeft(lay2);
		//lay0.setCenter(gg);
		//lay0.setCenter(DevLKIF2.genPanelMulti(lkif,0,1));
		//lay0.setCenter(DevLKIF2.genPanelMulti(lkif,0,1));
		lay0.setRight(lay1);
		return lay0;
	}
	
	private Pane genSpeedButton() {
		
		final TextField[] box = new TextField[4];
		for(int i=0; i<box.length; i++) {
			TextField obj = new TextField();
			obj.setPrefWidth(70);
			GridPane.setFillWidth(obj, true);
			box[i] = obj;
		}
		box[0].setText("50");
		box[1].setText("50");
		box[2].setText("3");
		box[3].setText("3");
		
		final JFXButton[] btn = new JFXButton[6];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(obj, true);
			btn[i] = obj;
		}
		
		//btn[0].setGraphic(Misc.getIconView("home.png"));
		btn[0].setText("test-1");
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->{
			float[][] grid = DevShapeoko.genGridPoint(
				shko.WCOX.get(),
				shko.WCOY.get(),
				Float.valueOf(box[0].getText()),
				Float.valueOf(box[1].getText()),
				Integer.valueOf(box[2].getText()),
				Integer.valueOf(box[3].getText())
			);
			notifyTask(new TaskGetSurface(grid));
		});
		
		btn[1].setText("test-2");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setOnAction(e->{
			//dummy.nextState("");
		});
		
		btn[2].setText("test-3");
		btn[2].getStyleClass().add("btn-raised-1");
		btn[2].setOnAction(e->{
			dummy.nextState("monitor");
		});		
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(new Label("網格大小"), 0, 0, 4, 1);
		lay.addRow(1, new Label("水平："), box[0]);
		lay.addRow(2, new Label("垂直："), box[1]);
		lay.add(new Label("格點數目"), 0, 3, 4, 1);
		lay.addRow(4, new Label("水平："), box[2]);
		lay.addRow(5, new Label("垂直："), box[3]);
		lay.add(btn[0], 0, 6);
		lay.add(btn[1], 0, 7);
		lay.add(btn[2], 0, 8);
		lay.add(btn[3], 0, 9);
		lay.add(btn[4], 0, 9);
		lay.add(btn[5], 0, 9);
		return lay;
	}
	
	private class TaskGetSurface extends Task<Void> {
		float[][] grid;
		public TaskGetSurface(float[][] arg0) {
			grid = arg0;
		}
		@Override
		protected Void call() throws Exception {
			for(int i=0; i<grid.length; i++) {
				updateMessage(String.format(
					"定位至: (%6.1f, %6.1f)",
					grid[i][0], grid[i][1]
				));
				shko.syncMoveAbs(
					grid[i][0], 
					grid[i][1], 
					0f
				);						
				grid[i][2] = lkif.getValue(0);
			}
			updateMessage("擬合平面方程式...");
			
			return null;
		}
	};
	
	
}
