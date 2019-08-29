package narl.itrc.vision;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
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

public class ImgPane extends ImgView {
	
	//let user do mask, one-brush means one-category 
	protected final Canvas tarp = new Canvas();
	//put something like label or mark, whatever~~~
	protected final AnchorPane pane = new AnchorPane();
	
	public ImgPane() {
		StackPane.setAlignment(pane, Pos.TOP_LEFT);
		StackPane.setAlignment(tarp, Pos.TOP_LEFT);
		tarp.setMouseTransparent(true);
		lay0.getChildren().addAll(tarp,pane);
		init_menu_item();
	}
	
	@Override
	public void updateView(final ImgFilm dat){
		super.updateView(dat);
		tarp.setWidth(width());
		tarp.setHeight(height());
		//over.getGraphicsContext2D().drawImage(v[1], 0., 0.);
	}
	
	private void init_menu_item() {
		
		final ToggleGroup grp = new ToggleGroup();
		final RadioMenuItem[] itm = {
			new RadioMenuItem("位置"),
			new RadioMenuItem("ROI"),
			new RadioMenuItem("圖刷")
		};
		itm[0].setToggleGroup(grp);
		itm[1].setToggleGroup(grp);
		itm[2].setToggleGroup(grp);
		itm[0].setOnAction(e->press_pane_item(
			grp, itm[0], 
			()->applyMarkPin(),
			()->{}
		));
		itm[1].setOnAction(e->press_pane_item(
			grp, itm[1], 
			()->applyMarkROI(),
			()->{}
		));
		itm[2].setOnAction(e->press_pane_item(
			grp, itm[2], 
			()->applyMarkBrush(),
			()->snap_brush()
		));
		
		final Menu men_mark = new Menu("標記");
		men_mark.getItems().addAll(itm);
		menu.getItems().add(men_mark);
	}

	private void press_pane_item(
		final ToggleGroup grp,
		final RadioMenuItem itm,
		final Runnable hook1,
		final Runnable hook2
	) {
		if((RadioMenuItem)grp.getUserData()==itm) {
			grp.setUserData(null);
			grp.selectToggle(null);
			unattachPane();
			hook2.run();
		}else{
			grp.setUserData(itm);
			hook1.run();
		}
	}
	
	private static final String pane_name = "mark_panel";
	
	private void attachPane(final Node node) {
		StackPane.setAlignment(node, Pos.TOP_LEFT);
		getChildren().add(node);
		node.setId(pane_name);
	}
	private void unattachPane() {
		pane.setOnMouseClicked(null);
		pane.setOnMouseDragged(null);
		for(Node obj:getChildren()) {
			String id = obj.getId();
			if(id==null) {
				continue;
			}
			if(obj.getId().equals(pane_name)==false) {
				continue;
			}
			getChildren().remove(obj);
			break;
		}
	}
	//---------------------------//
	
	public abstract class Mark extends Group {
		public boolean isUsed = false;
		public int locaX=0, locaY=0;
		public int sizeW=1, sizeH=1;
		abstract void setLoca(double x, double y);
		abstract void setSize(double w, double h);
		void remove() {
			ObservableList<Node> lst = pane.getChildren();
			if(lst.contains(this)==false) {
				return;
			}
			lst.remove(this);
			isUsed = false;
		}
		void append() {
			ObservableList<Node> lst = pane.getChildren();
			if(lst.contains(this)==true) {
				return;
			}
			lst.add(this);
			isUsed = true;
		}
	};
	private class MarkPin extends Mark {
		int rr = 10;
		MarkPin(Color cc){			
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
			sizeW =(int) w;
			sizeH = (int)h;
		}
	};
	private class MarkRect extends Mark {
		Rectangle rect;
		MarkRect(Color cc){
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
			if(w>=0.) {
				sizeW = (int)w;
				rect.setWidth(w);
			}
			if(h>=0.) {
				sizeH = (int)h;
				rect.setHeight(h);
			}
		}
	};
	
	private final Mark[] markObj = {
		new MarkPin(Color.YELLOW),
		new MarkPin(Color.DEEPSKYBLUE),
		new MarkPin(Color.BLUEVIOLET),
		new MarkPin(Color.CRIMSON),
		new MarkRect(Color.YELLOW),
		new MarkRect(Color.DEEPSKYBLUE),
		new MarkRect(Color.BLUEVIOLET),
		new MarkRect(Color.CRIMSON),
	};

	public Mark[] getAllMark() {
		return markObj;
	}
	
	public String flattenMark() {
		String txt = "";
		for(Mark mm:markObj) {
			if(mm.isUsed==true) {
				txt = txt + String.format(
					"T%d,%d,%d,%d;",
					mm.locaX, mm.locaY,
					mm.sizeW, mm.sizeH
				);
			}else {
				txt = txt + "F;";
			}
		}
		return txt;
	}
	
	public void unflattenMark(final String flatTxt) {
		if(flatTxt.length()==0) {
			return;
		}
		String[] tkn = flatTxt.split(";");
		int idx = 0;
		for(String txt:tkn) {
			char cc = txt.charAt(0);
			txt = txt.substring(1);
			if(cc=='T') {
				String[] val = txt.split(",");
				Mark mm = markObj[idx];				
				mm.setLoca(
					Integer.valueOf(val[0]), 
					Integer.valueOf(val[1])
				);
				mm.setSize(
					Integer.valueOf(val[2]), 
					Integer.valueOf(val[3])
				);
				mm.append();
			}
			idx+=1;
		}
	}
	
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
		mm.append();
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
		((Mark)tgl.getUserData()).remove();
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
		lay0.getStyleClass().addAll("view-mark","pad-medius");
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
	
	protected Image maskBrush = null;
	private GraphicsContext b_gc;
	private Color b_cc;
	private int b_rad = 40;
	
	private void snap_brush() {
		final SnapshotParameters pp = new SnapshotParameters();
		pp.setFill(Color.TRANSPARENT);
		maskBrush = tarp.snapshot(pp, null);
	}
	
	private void make_brush(
		final MouseEvent event,
		final ToggleGroup grp
	) {
		if(grp.getSelectedToggle()==null) {
			return;
		}
		b_gc.setStroke(b_cc);
		b_gc.setFill(b_cc);
		double xx = event.getX()-b_rad/2;
		double yy = event.getY()-b_rad/2;
		if(b_cc==Color.TRANSPARENT) {				
			b_gc.clearRect(xx , yy,	b_rad, b_rad);
		}else {
			b_gc.fillOval(xx, yy, b_rad, b_rad);
		}
	}
	
	private void applyMarkBrush() {
		final ToggleGroup grp = new ToggleGroup();
		final ToggleButton btn[] = {
			new ToggleButton("brush-1"),
			new ToggleButton("brush-2"),
			new ToggleButton("brush-3"),
			new ToggleButton("brush-x"),
		};
		btn[0].setUserData(Color.rgb(200,0, 0, 0.5));
		btn[1].setUserData(Color.rgb(0,200, 0, 0.5));
		btn[2].setUserData(Color.rgb(0, 0,200, 0.5));
		btn[3].setUserData(Color.TRANSPARENT);
		for(ToggleButton b:btn) {
			b.setToggleGroup(grp);
			b.setMaxWidth(Double.MAX_VALUE);
			b.setOnAction(e->{ b_cc=(Color)b.getUserData(); });
		}
		final Button btnClear = new Button("clear");
		btnClear.setMaxWidth(Double.MAX_VALUE);
		btnClear.setOnAction(e->b_gc.clearRect(
			0, 0, 
			tarp.getWidth(), tarp.getHeight()
		));
		
		final Button btnApply = new Button("apply");
		btnApply.setMaxWidth(Double.MAX_VALUE);
		btnApply.setOnAction(e->snap_brush());
		
		final TextField boxSize = new TextField();
		boxSize.setPrefWidth(43);
		boxSize.setText(String.valueOf(b_rad));
		boxSize.setOnAction(e->{
			b_rad = Integer.valueOf(boxSize.getText());
		});
		
		b_gc = tarp.getGraphicsContext2D();
		pane.setOnMouseClicked(e->{
			if(e.getButton()!=MouseButton.PRIMARY) {
				return;
			}
			make_brush(e,grp);
		});
		pane.setOnMouseDragged(e->make_brush(e,grp));	
		
		final GridPane lay = new GridPane();
		lay.add(btn[0], 0, 0, 4, 1);
		lay.add(btn[1], 0, 1, 4, 1);
		lay.add(btn[2], 0, 2, 4, 1);
		lay.add(btn[3], 0, 3, 4, 1);		
		lay.addRow(4, new Label("Size"), boxSize);
		lay.add(btnClear, 0, 5, 4, 1);
		lay.add(btnApply, 0, 6, 4, 1);
		lay.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		lay.getStyleClass().addAll("view-mark","grid-medium");
		attachPane(lay);	
	}
}
