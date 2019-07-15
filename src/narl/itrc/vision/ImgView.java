package narl.itrc.vision;

import java.awt.image.RenderedImage;
import java.io.File;

import javax.imageio.ImageIO;

import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;

public class ImgView extends StackPane {
	
	//show original image~~
	private final ImageView view = new ImageView();
	//show information, result or overlay-image
	private final ImageView over = new ImageView();	
	//let user do mask, one-brush means one-category 
	private final Canvas tarp = new Canvas();
	//put something like label or mark, whatever~~~
	private final AnchorPane pane = new AnchorPane();
	//layout-base
	private final ScrollPane base = new ScrollPane(
		new StackPane(view, over, tarp, pane)
	);
	
	public ImgView(){
		StackPane.setAlignment(view, Pos.TOP_LEFT);
		StackPane.setAlignment(over, Pos.TOP_LEFT);
		StackPane.setAlignment(pane, Pos.TOP_LEFT);
		StackPane.setAlignment(tarp, Pos.TOP_LEFT);
		view.setCache(true);
		view.setMouseTransparent(true);
		over.setCache(true);
		over.setMouseTransparent(true);
		tarp.setMouseTransparent(true);
		//lay1.setMouseTransparent(true);
		base.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		base.setVbarPolicy(ScrollBarPolicy.ALWAYS);		
		base.setFitToHeight(false);
		base.setFitToWidth(false);
		getChildren().addAll(base);
		setStyle("-fx-padding: 7px;");
		init_menu();
		//getChildren().add(new Button("ggyy"));
		//setStyle("-fx-border-color: chocolate; -fx-border-width: 4px;");//DEBUG!!!		
	}

	public ImageView getOverlay() {
		return over;
	}
	public AnchorPane getImgBoard() {
		return pane;
	}
	public void attachPane(final Node node,final String id_name) {
		StackPane.setAlignment(node, Pos.TOP_LEFT);
		getChildren().add(node);
		node.setId(id_name);
	}
	public void unattachPane(final String id_name) {
		pane.setOnMouseClicked(null);
		pane.setOnMouseDragged(null);
		for(Node obj:getChildren()) {
			String id = obj.getId();
			if(id==null) {
				continue;
			}
			if(obj.getId().equals(id_name)==false) {
				continue;
			}
			getChildren().remove(obj);
			return;
		}
	}
	
	private static final String pane_name = "view_ctrl_pane";
	public void attachPane(final Node node) {
		attachPane(node, pane_name);
	}
	public void unattachPane() {
		unattachPane(pane_name);
	}
	
	
	/**
	 * scale image parameter.<p>
	 * <=2 : smaller
	 * -1,0,1 : no change
	 * >=2 : bigger
	 */
	private int zoom = 1;
	
	private void update_zoom(final int val){
		zoom = val;
		final Image img = view.getImage();
		int ww = (int)img.getWidth();
		int hh = (int)img.getHeight();
		if(val<=-2){
			ww = ww / (-zoom);
			hh = hh / (-zoom);
		}else if(val>=2){
			ww = ww * zoom;
			hh = hh * zoom;
		}
		view.setFitWidth(ww);
		view.setFitHeight(hh);
		view.setPreserveRatio(true);
	}
	
	private final static FileChooser diaFile = new FileChooser();
	
	private void init_menu(){		
		final ToggleGroup tgl_zoom = new ToggleGroup();
		final RadioMenuItem[] itm_zoom = {
			new RadioMenuItem("4:1"),
			new RadioMenuItem("3:1"),
			new RadioMenuItem("2:1"),
			new RadioMenuItem("1:1"),
			new RadioMenuItem("1:2"),
			new RadioMenuItem("1:3"),
			new RadioMenuItem("1:4"),
		};
		itm_zoom[0].setToggleGroup(tgl_zoom);
		itm_zoom[0].setOnAction(e->update_zoom(-4));
		itm_zoom[1].setToggleGroup(tgl_zoom);
		itm_zoom[1].setOnAction(e->update_zoom(-3));
		itm_zoom[2].setToggleGroup(tgl_zoom);
		itm_zoom[2].setOnAction(e->update_zoom(-2));
		itm_zoom[3].setToggleGroup(tgl_zoom);
		itm_zoom[3].setOnAction(e->update_zoom( 0));
		itm_zoom[4].setToggleGroup(tgl_zoom);
		itm_zoom[4].setOnAction(e->update_zoom( 2));
		itm_zoom[5].setToggleGroup(tgl_zoom);
		itm_zoom[5].setOnAction(e->update_zoom( 3));
		itm_zoom[6].setToggleGroup(tgl_zoom);
		itm_zoom[6].setOnAction(e->update_zoom( 4));
		tgl_zoom.selectToggle(itm_zoom[3]);//default is 1:1
		final Menu men_zoom = new Menu("Zoom");
		men_zoom.getItems().addAll(itm_zoom);
		
		diaFile.setInitialDirectory(Gawain.dirRoot);
		final MenuItem itm_load = new MenuItem("載入");
		itm_load.setOnAction(e->{
			diaFile.setTitle("載入圖檔");
			File fs = diaFile.showOpenDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			diaFile.setInitialDirectory(fs.getParentFile());
			view.setImage(new Image(fs.toURI().toString()));	
		});
		final MenuItem itm_save = new MenuItem("儲存");
		itm_save.setOnAction(e->{
			diaFile.setTitle("儲存圖檔");
			File fs = diaFile.showSaveDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			diaFile.setInitialDirectory(fs.getParentFile());
			save(fs);
		});
		final ToggleGroup tgl_mark = new ToggleGroup();
		final RadioMenuItem[] itm_mark = {
			new RadioMenuItem("位置"),
			new RadioMenuItem("ROI"),
			new RadioMenuItem("圖刷")
		};
		itm_mark[0].setUserData(0);
		itm_mark[1].setUserData(1);
		itm_mark[2].setUserData(2);
		for(RadioMenuItem itm:itm_mark) {
			itm.setToggleGroup(tgl_mark);
			itm.setOnAction(e->{
				unattachPane();
				RadioMenuItem obj = (RadioMenuItem)tgl_mark.getUserData();
				if(obj==itm) {
					tgl_mark.setUserData(null);
					tgl_mark.selectToggle(null);
				}else{
					tgl_mark.setUserData(itm);
					switch((int)itm.getUserData()) {
					case 0: applyMarkPin(); break;
					case 1: applyMarkROI(); break;
					case 2: applyMarkBrush(); break;
					}
				}				
			});
		}

		final Menu men_mark = new Menu("標記");
		men_mark.getItems().addAll(itm_mark);
		base.setContextMenu(new ContextMenu(
			men_mark,
			men_zoom,
			itm_load,
			itm_save			
		));
	}
	
	public void refresh(final ImgFilm dat){
		Image[] v = dat.getImage();
		view.setImage(v[0]);
		over.setImage(v[1]);
		tarp.setWidth(v[0].getWidth());
		tarp.setHeight(v[0].getHeight());		
		//over.getGraphicsContext2D().drawImage(v[1], 0., 0.);
	}
	
	public void save(final String name){
		save(new File(name));
	}	
	public void save(final File fs){
		if(fs.isDirectory()==true){
			return;
		}
		String appx = fs.getName();
		int idx = appx.lastIndexOf(".");
		if(idx>=1){
			appx = appx.substring(idx+1);
		}else{
			appx = "png";
		}
		try {
			RenderedImage rend = SwingFXUtils.fromFXImage(
				view.getImage(),
				null
			);
			ImageIO.write(rend,	appx, fs);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	//---------------------------------//

	private abstract class Mark extends Group {
		char shape = 0;
		int locaX=0, locaY=0;
		int sizeW=1, sizeH=1;
		abstract void setLoca(double x, double y);
		abstract void setSize(double w, double h);
	};
	private class MarkPin extends Mark {
		int rr = 10;
		MarkPin(Color cc){			
			shape = 'x';
			final Path p = new Path(
				new MoveTo(-rr, -rr), new LineTo(-1,-1),
				new MoveTo(-rr,  rr), new LineTo(-1, 1),
				new MoveTo( rr, -rr), new LineTo( 1,-1),
				new MoveTo( rr,  rr), new LineTo( 1, 1)
			);
			p.setStroke(cc);
			p.setStrokeWidth(2);
			getChildren().add(p);
		}
		@Override
		void setLoca(double x, double y) {
			locaX = (int)x;
			locaY = (int)y;
			AnchorPane.setLeftAnchor(this, x-rr);
			AnchorPane.setTopAnchor (this, y-rr);
		}
		@Override
		void setSize(double w, double h) {
			//For this mask, this is no function~~~~
		}
	};
	private class MarkRect extends Mark {
		Rectangle rect;
		MarkRect(Color cc){
			shape = 'r';
			sizeW = 100;
			sizeH = 100;
			rect = new Rectangle(0, 0, sizeW, sizeH);
			rect.setStrokeWidth(2);
			rect.setStroke(cc);
			rect.setFill(Color.TRANSPARENT);
			getChildren().addAll(rect);			
		}
		@Override
		void setLoca(double x, double y) {
			if(x>=0.) {
				locaX = (int)x - sizeW/2;
				AnchorPane.setLeftAnchor(this, (double)locaX);
			}
			if(y>=0.) {
				locaY = (int)y - sizeW/2;
				AnchorPane.setTopAnchor (this, (double)locaY);
			}
		}
		@Override
		void setSize(double w, double h) {
			if(w>=0) {
				sizeW = (int)w;
				rect.setWidth(w);
			}
			if(h>=0) {
				sizeH = (int)h;
				rect.setHeight(h);
			}
		}
	};
	
	private Mark[] markObj = {
		new MarkPin(Color.YELLOW),
		new MarkPin(Color.DEEPSKYBLUE),
		new MarkPin(Color.BLUEVIOLET),
		new MarkPin(Color.CRIMSON),
		new MarkRect(Color.YELLOW),
		new MarkRect(Color.DEEPSKYBLUE),
		new MarkRect(Color.BLUEVIOLET),
		new MarkRect(Color.CRIMSON),
	};

	private Mark apply_mark(
		final ToggleGroup grp,
		final MouseEvent event		
	) {
		if (event.getButton()!=MouseButton.PRIMARY) {
			return null;
		}
		Toggle tgl = grp.getSelectedToggle();
		if(tgl==null) {
			return null;
		}
		Mark mm = (Mark)tgl.getUserData();
		mm.setLoca(event.getX(), event.getY());
		ObservableList<Node> lst = pane.getChildren();
		if(lst.contains(mm)==false) {
			lst.add(mm);
		}
		return mm;
	}
	
	private void apply_rect(
		final ToggleGroup grp,		
		final TextField box,
		final char name
	) {
		Toggle tgl = grp.getSelectedToggle();
		if(tgl==null) {
			return;
		}
		Mark mm = (Mark)tgl.getUserData();
		try {
			int vv = Integer.valueOf(box.getText());
			switch(name) {
			case 'x': mm.setLoca(vv, -1); break;
			case 'y': mm.setLoca(-1, vv); break;
			case 'w': mm.setSize(vv, -1); break;
			case 'h': mm.setSize(-1, vv); break;
			}			
		}catch(NumberFormatException e) {
			switch(name) {
			case 'x': box.setText(String.valueOf(mm.locaX)); break;
			case 'y': box.setText(String.valueOf(mm.locaY)); break;
			case 'w': box.setText(String.valueOf(mm.sizeW)); break;
			case 'h': box.setText(String.valueOf(mm.sizeH)); break;
			}
		}
	}
	
	private void clear_mark(final ToggleGroup grp) {
		Toggle tgl = grp.getSelectedToggle();
		if(tgl==null) {
			return;
		}
		Mark mm = (Mark)tgl.getUserData();
		ObservableList<Node> lst = pane.getChildren();
		if(lst.contains(mm)==true) {
			lst.remove(mm);
		}
		grp.selectToggle(null);
	}
	
	private void applyMarkPin() {
		final ToggleGroup grp = new ToggleGroup();
		final ToggleButton btn[] = {
			new ToggleButton("pin-1"),
			new ToggleButton("pin-2"),
			new ToggleButton("pin-3"),
			new ToggleButton("pin-4"),
		};
		btn[0].setToggleGroup(grp);
		btn[1].setToggleGroup(grp);
		btn[2].setToggleGroup(grp);
		btn[3].setToggleGroup(grp);
		btn[0].setUserData(markObj[0]);
		btn[1].setUserData(markObj[1]);
		btn[2].setUserData(markObj[2]);
		btn[3].setUserData(markObj[3]);
		
		final Button btnClear = new Button("清除");
		btnClear.setMaxWidth(Double.MAX_VALUE);
		btnClear.setOnAction(e->clear_mark(grp));
		
		pane.setOnMouseClicked(e->apply_mark(grp,e));
		
		final VBox lay0 = new VBox();
		lay0.getChildren().addAll(btn);
		lay0.getChildren().addAll(btnClear);
		lay0.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		//lay0.setStyle(vbox_css);
		lay0.getStyleClass().add("view-mark-vbox");
		attachPane(lay0);
	}
	
	private void applyMarkROI() {
		final ToggleGroup grp = new ToggleGroup();
		final ToggleButton btn[] = {
			new ToggleButton("ROI-1"),
			new ToggleButton("ROI-2"),
			new ToggleButton("ROI-3"),
			new ToggleButton("ROI-4"),
		};
		for(ToggleButton b:btn) {
			b.setToggleGroup(grp);
			b.setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].setUserData(markObj[4]);
		btn[1].setUserData(markObj[5]);
		btn[2].setUserData(markObj[6]);
		btn[3].setUserData(markObj[7]);
		
		final TextField[] box = {
			new TextField(),new TextField(),
			new TextField(),new TextField(),
		};
		for(TextField b:box) {
			b.setPrefWidth(43);			
		}
		box[0].setOnAction(e->apply_rect(grp,box[0],'x'));
		box[1].setOnAction(e->apply_rect(grp,box[1],'y'));
		box[2].setOnAction(e->apply_rect(grp,box[2],'w'));
		box[3].setOnAction(e->apply_rect(grp,box[3],'h'));
		
		final Button btnClear = new Button("清除");
		btnClear.setMaxWidth(Double.MAX_VALUE);
		btnClear.setOnAction(e->clear_mark(grp));
		
		pane.setOnMouseClicked(e->{
			Mark mm = apply_mark(grp,e);
			if(mm==null) {
				return;
			}
			box[0].setText(String.format("%d",mm.locaX));
			box[1].setText(String.format("%d",mm.locaY));
			box[2].setText(String.format("%d",mm.sizeW));
			box[3].setText(String.format("%d",mm.sizeH));
		});
		
		final GridPane lay = new GridPane();
		lay.add(btn[0], 0, 0, 4, 1);
		lay.add(btn[1], 0, 1, 4, 1);
		lay.add(btn[2], 0, 2, 4, 1);
		lay.add(btn[3], 0, 3, 4, 1);
		lay.addRow(4, new Label("X"), box[0], new Label("W"), box[2]);
		lay.addRow(5, new Label("Y"), box[1], new Label("H"), box[3]);
		lay.add(btnClear, 0, 6, 4, 1);
		lay.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		lay.getStyleClass().addAll("view-mark","grid-medium");
		attachPane(lay);
	}
	
	private GraphicsContext b_gc;
	private Color b_cc;
	private int b_rad = 20;
	
	private void applyMarkBrush() {
		final ToggleGroup grp = new ToggleGroup();
		final ToggleButton btn[] = {
			new ToggleButton("brush-1"),
			new ToggleButton("brush-2"),
			new ToggleButton("brush-3"),
			new ToggleButton("brush-4"),
			new ToggleButton("Clear"),
		};
		btn[0].setUserData(Color.YELLOW);
		btn[1].setUserData(Color.DEEPSKYBLUE);
		btn[2].setUserData(Color.BLUEVIOLET);
		btn[3].setUserData(Color.CRIMSON);
		btn[4].setUserData(Color.TRANSPARENT);
		for(ToggleButton b:btn) {
			b.setToggleGroup(grp);
			b.setOnAction(e->{
				b_cc = (Color)b.getUserData();
			});
		}
		
		final TextField boxSize = new TextField();
		boxSize.setPrefWidth(43);
		boxSize.setText(String.valueOf(b_rad));
		boxSize.setOnAction(e->{
			b_rad = Integer.valueOf(boxSize.getText());
		});
		
		b_gc = tarp.getGraphicsContext2D();
		pane.setOnMouseDragged(e->{
			if(grp.getSelectedToggle()==null) {
				return;
			}
			b_gc.setStroke(b_cc);
			b_gc.setFill(b_cc);
			double xx = e.getX()-b_rad/2;
			double yy = e.getY()-b_rad/2;
			if(b_cc==Color.TRANSPARENT) {				
				b_gc.clearRect(xx , yy,	b_rad, b_rad);
			}else {
				b_gc.fillOval(xx, yy, b_rad, b_rad);
			}
		});
		
		final GridPane lay = new GridPane();
		lay.add(btn[0], 0, 0, 4, 1);
		lay.add(btn[1], 0, 1, 4, 1);
		lay.add(btn[2], 0, 2, 4, 1);
		lay.add(btn[3], 0, 3, 4, 1);
		lay.add(btn[4], 0, 4, 4, 1);
		lay.addRow(5, new Label("Size"), boxSize);
		lay.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		lay.getStyleClass().addAll("view-mark","grid-medium");
		attachPane(lay);
	}
}


