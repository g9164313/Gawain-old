package narl.itrc.vision;

import java.awt.image.RenderedImage;
import java.io.File;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;

public class ImgView extends ScrollPane {
	
	//show original image~~
	private final ImageView view = new ImageView();
	
	//show information, result or overlay-image
	private final ImageView over = new ImageView();
	
	//put something like label or mark, else
	private final AnchorPane pane = new AnchorPane();
	
	public ImgView(){
		setContent(new StackPane(view, over, pane));
		setHbarPolicy(ScrollBarPolicy.ALWAYS);
		setVbarPolicy(ScrollBarPolicy.ALWAYS);		
		setFitToHeight(false);
		setFitToWidth(false);
		setMinSize(240+23,240+23);	
		init_menu();
		init_dia_mark();
		//setStyle("-fx-border-color: chocolate; -fx-border-width: 4px;");//DEBUG!!!
		//final MenuItem itm1 = new MenuItem("導航");
		//itm1.disableProperty().bind(pan_navi.visibleProperty());
		//itm1.setOnAction(e->pan_navi.setVisible(true));		
	}

	public Pane getPane() {
		return pane;
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
	
	/*private static final char SHAPE_RECT  = 'R';
	private static final char SHAPE_CROSS = 'T';
	private static final char SHAPE_CIRCLE= 'C';
	private class Mark extends Path {
		final CheckMenuItem chkItem;
		boolean used = false;
		char shape = SHAPE_RECT;
		int[] loca = { 0,  0};
		int[] volu = {50, 50};
		public Mark(String name, Color cc){
			chkItem = new CheckMenuItem(name);
			chkItem.setOnAction(event->{
				if(chkItem.isSelected()==true){
					used = true;
					bord.getChildren().add(this);
				}else{
					used = false;
					bord.getChildren().remove(this);
				}
				update();
			});
			setStrokeWidth(3);
			setStroke(cc);
			setPickOnBounds(false);
			setOnMouseClicked(e->{
				if(e.getButton().equals(MouseButton.PRIMARY)==false){
					return;
				}
				if(e.getClickCount()<2){
					return;
				}
				diaMark.getDialogPane().setUserData(this);
				diaMark.show();
			});
			getElements().add(new MoveTo(0.,0.));//prevent crash~~~
		}
		void update(){
			AnchorPane.setLeftAnchor(this, (double)loca[0]);
			AnchorPane.setTopAnchor (this, (double)loca[1]);
			getElements().remove(
				1, 
				getElements().size()
			);
			switch(shape){
			case SHAPE_RECT:
				getElements().addAll(
					new HLineTo(volu[0]), new VLineTo(volu[1]),
					new HLineTo(0), new VLineTo(0)
				);
				break;
			case SHAPE_CROSS:
				getElements().addAll(
					new MoveTo(volu[0]/2,0.), new VLineTo(volu[1]),
					new MoveTo(0,volu[1]/2 ), new HLineTo(volu[0])
				);
				break;
			case SHAPE_CIRCLE:
				getElements().addAll(
					new ArcTo(
						volu[0]/2, volu[1]/2,
						0.,
						1, 0,
						true, false
					)
				);
				break;
			}
		}
	};*/

	private class Mark extends Group {
		final CheckMenuItem chk = new CheckMenuItem();
		final Arc pin_1 = new Arc();
		final Arc pin_2 = new Arc();
		final Rectangle fence = new Rectangle();
		final double PIN_SIZE = 13.;
		boolean used = false;
		Mark(String name, Color cc){
			chk.setText(name);
			chk.setOnAction(e->{
				used = chk.isSelected();
				if(used==true){
					pane.getChildren().add(this);
				}else{
					pane.getChildren().remove(this);
				}
			});
			update(0,0,100,100);
			fence.setStrokeWidth(2);
			fence.setStroke(cc);
			fence.setFill(Color.TRANSPARENT);
			pin_1.setRadiusX(PIN_SIZE);
			pin_1.setRadiusY(PIN_SIZE);
			pin_1.setStartAngle(0.);
			pin_1.setLength(-90.);
			pin_1.setFill(cc);
			pin_1.setType(ArcType.ROUND);
			pin_1.setOnMouseDragged(e->{
				update(e.getX(), e.getY(), -1, -1);
			});
			pin_2.setRadiusX(PIN_SIZE);
			pin_2.setRadiusY(PIN_SIZE);
			pin_2.setStartAngle(90.);
			pin_2.setLength(90.);
			pin_2.setFill(cc);
			pin_2.setType(ArcType.ROUND);
			pin_2.setOnMouseDragged(e->{
				double ww = e.getX() - fence.getX();
				double hh = e.getY() - fence.getY();
				update(-1, -1, ww, hh);
			});
			getChildren().addAll(fence,pin_1,pin_2);
		}
		
		void update(double xx, double yy, double ww, double hh) {
			if(xx>=0) {
				fence.setX(xx);	
			}
			if(yy>=0) {
				fence.setY(yy);			
			}
			if(ww>0) {
				fence.setWidth(ww);
			}
			if(hh>0) {
				fence.setHeight(hh);
			}
			pin_1.setCenterX(fence.getX());
			pin_1.setCenterY(fence.getY());
			pin_2.setCenterX(fence.getX()+fence.getWidth());
			pin_2.setCenterY(fence.getY()+fence.getHeight());
		}
		void setInfo(int xx, int yy, int ww, int hh) {
			update(xx,yy,ww,hh);
			chk.setSelected(true);
			chk.fire();
		}
		int[] getInfo() {
			int[] info = {
				(int)fence.getX(), 
				(int)fence.getY(),
				(int)fence.getWidth(),
				(int)fence.getHeight()				
			};
			return info;
		}
	};
	private Mark[] lstMark = {
		new Mark("mark-1", Color.YELLOW),
		new Mark("mark-2", Color.DEEPSKYBLUE),
		new Mark("mark-3", Color.BLUEVIOLET),
		new Mark("mark-4", Color.CRIMSON),
	};
	
	private Dialog<Void> diaMark = new Dialog<>();
	private void init_dia_mark(){
		final ComboBox<String> cmbShape = new ComboBox<>();
		cmbShape.getItems().addAll("方框","十字","圓框");
		final TextField[] boxValue = {
			new TextField(),
			new TextField(),	
			new TextField(),
			new TextField(),
		};
		for(TextField b:boxValue){
			b.setMaxWidth(73);
			//GridPane.setHgrow(b,Priority.ALWAYS);
		}
		diaMark.setTitle("標記設定");
		diaMark.setHeaderText(null);
		final GridPane lay = new GridPane();
		lay.setHgap(10);
		lay.setVgap(10);
		lay.setPadding(new Insets(20, 150, 10, 10));
		lay.addRow(0, new Label("形狀"), cmbShape);
		lay.addRow(1, new Label("X"), boxValue[0]);
		lay.addRow(2, new Label("Y"), boxValue[1]);
		lay.addRow(3, new Label("W"), boxValue[2]);
		lay.addRow(4, new Label("H"), boxValue[3]);
		/*diaMark.setOnShown(e->{
			Mark mark = (Mark)diaMark.getDialogPane().getUserData();
			int sid = 0;
			switch(mark.shape){
			case SHAPE_RECT  : sid=0; break;
			case SHAPE_CROSS : sid=1; break;
			case SHAPE_CIRCLE: sid=2; break;
			}
			cmbShape.getSelectionModel().select(sid);
			boxValue[0].setText(""+mark.loca[0]);
			boxValue[1].setText(""+mark.loca[1]);
			boxValue[2].setText(""+mark.volu[0]);
			boxValue[3].setText(""+mark.volu[1]);
		});		
		diaMark.getDialogPane().getButtonTypes().addAll(
			ButtonType.APPLY,
			ButtonType.CANCEL
		);
		((Button)diaMark.getDialogPane()
			.lookupButton(ButtonType.APPLY))
			.addEventFilter(ActionEvent.ACTION, event->{
				Mark mark = (Mark)diaMark.getDialogPane().getUserData();
				int sid = cmbShape.getSelectionModel().getSelectedIndex();
				switch(sid){
				case 0: mark.shape = SHAPE_RECT; break;
				case 1: mark.shape = SHAPE_CROSS; break;
				case 2: mark.shape = SHAPE_CIRCLE; break;
				}
				mark.loca[0] = Integer.valueOf(boxValue[0].getText());
				mark.loca[1] = Integer.valueOf(boxValue[1].getText());
				mark.volu[0] = Integer.valueOf(boxValue[2].getText());
				mark.volu[1] = Integer.valueOf(boxValue[3].getText());
				mark.update();
				event.consume();
			});
		diaMark.getDialogPane().setContent(lay);*/
	}
	
	/**
	 * this function must be invoked by main thread.<p>
	 * @param txt - flat text for recording mark information
	 */
	public void setMarkByFlat(final String txt){
		if(txt.length()==0){
			return;
		}
		String[] col = txt.split("#");
		for(int i=0; i<col.length; i++){
			if(col[i].length()<=1){
				continue;
			}
			if(i>=lstMark.length){
				return;
			}
			String[] val = col[i].split(",");
			lstMark[i].setInfo(
				Integer.valueOf(val[1]), 
				Integer.valueOf(val[2]),
				Integer.valueOf(val[3]),
				Integer.valueOf(val[4])
			);
		}
	}
	
	public String getMarkByFlat(){
		String txt ="";
		for(Mark mm:lstMark){
			if(mm.used==false){
				txt = txt + "_#";
			}else{
				int[] info = mm.getInfo();
				txt = txt + String.format(
					"R,%d,%d,%d,%d#",
					info[0], info[1],					
					info[2], info[3]
				);
			}
		}
		return txt;
	}
	
	public void getMarkByArray(final int[] array){
		int i=0;
		for(Mark mm:lstMark){
			if((i*4+3)>=array.length){
				return;
			}
			if(mm.used==true){
				int[] info = mm.getInfo();
				array[i*4+0] = info[0];//location - x
				array[i*4+1] = info[1];//location - y
				array[i*4+2] = info[2];//geometry - width
				array[i*4+3] = info[3];//geometry - height
			}
			i+=1;
		}
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
		
		final Menu itm_mark = new Menu("標記");
		itm_mark.getItems().addAll(
			lstMark[0].chk,
			lstMark[1].chk,
			lstMark[2].chk,
			lstMark[3].chk
		);
		
		setContextMenu(new ContextMenu(
			itm_load,itm_save,
			itm_mark,
			men_zoom
		));
	}
	
	public void refresh(final ImgFlim dat){
		Image[] v = dat.getImage();
		view.setImage(v[0]);
		over.setImage(v[1]);
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
}


