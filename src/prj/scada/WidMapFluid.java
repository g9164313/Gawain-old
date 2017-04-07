package prj.scada;

import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class WidMapFluid extends ScrollPane {

	public WidMapFluid(){		

		GridPane grid = new GridPane();
		grid.setMinSize(0., 0.);//force to the same size~~~
		grid.setGridLinesVisible(true);
		init_grid(grid);
		
		getStyleClass().add("pad-medium");
		setFitToWidth(true);
		setFitToHeight(true);
		setHbarPolicy(ScrollBarPolicy.ALWAYS);
		setVbarPolicy(ScrollBarPolicy.ALWAYS);
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		setContent(grid);
	}
	
	private void init_grid(GridPane grid){
	
		final int CELL_SIZE = 60;
		
		final int rows = 10;
		final int cols = 20;
				
		for(int i=0; i<cols; i++){
			ColumnConstraints col = new ColumnConstraints(CELL_SIZE);
			grid.getColumnConstraints().add(col);
		}
		
		for(int i=0; i<rows; i++){
			RowConstraints row = new RowConstraints(CELL_SIZE);
			grid.getRowConstraints().add(row);
		}

		ImageView img1 = new ImageView(tile[0]);
		ImageView img2 = new ImageView(tile[1]);
		ImageView img3 = new ImageView(tile[2]);
		ImageView img4 = new ImageView(tile[4]);
		
		grid.add(img1, 2, 3);
		grid.add(img2, 3, 3);
		grid.add(img3, 4, 3);
		grid.add(img4, 2, 4);
	}
	
	private static final String IMG_DIR = "/narl/itrc/res/tile/";
	
	private static Image[] tile = {
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"gauge.png"   ))/* index - 0 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"valve.png"   ))/* index - 1 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe2-01.png"))/* index - 2 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe2-02.png"))/* index - 3 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe2-03.png"))/* index - 4 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe2-04.png"))/* index - 5 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe2-05.png"))/* index - 6 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe2-06.png"))/* index - 7 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe3-01.png"))/* index - 8 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe3-02.png"))/* index - 9 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe3-03.png"))/* index -10 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe3-04.png"))/* index -11 */,
		new Image(Gawain.class.getResourceAsStream(IMG_DIR+"pipe4.png"   ))/* index -12 */,
	};
}
