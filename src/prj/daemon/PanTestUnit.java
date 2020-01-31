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
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanTestUnit extends PanBase {

	public PanTestUnit() {		
	}
	
	private DevShapeoko shko = new DevShapeoko();
	
	private DevLKIF2 lkif = new DevLKIF2(); 
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		stage().setOnShown(e->{
			String txt;
			txt = Gawain.prop().getProperty("shapeoko","");
			if(txt.length()!=0) {
				shko.open(txt);
			}
			if(Gawain.propFlag("USE_IF2")==true) {
				lkif.open();
			}
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
		
		final JFXButton[] btn = new JFXButton[4];
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
			notifyTask(new TaskGetSurface(grid),()->{
				
			});
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(new Label("網格大小"), 0, 0, 4, 1);
		lay.addRow(1, new Label("水平："), box[0]);
		lay.addRow(2, new Label("垂直："), box[1]);
		lay.add(new Label("格點數目"), 0, 3, 4, 1);
		lay.addRow(4, new Label("水平："), box[2]);
		lay.addRow(5, new Label("垂直："), box[3]);
		lay.add(btn[0], 0, 6, 6, 1);
		lay.add(btn[1], 0, 7, 7, 1);
		lay.add(btn[2], 0, 8, 8, 1);
		lay.add(btn[3], 0, 9, 9, 1);
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
