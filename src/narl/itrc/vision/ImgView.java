package narl.itrc.vision;

import java.awt.image.RenderedImage;
import java.io.File;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;

public class ImgView extends ScrollPane {

	//show original image~~
	private ImageView view = new ImageView();
	
	//show information, result or overlay-image
	private ImageView over = new ImageView();
	
	//put something like label or mark, else
	private AnchorPane bord = new AnchorPane();
	
	public ImgView(){
		
		init();
		
		setContent(new StackPane(view, over, bord));
		setHbarPolicy(ScrollBarPolicy.ALWAYS);
		setVbarPolicy(ScrollBarPolicy.ALWAYS);		
		setFitToHeight(false);
		setFitToWidth(false);
		setMinSize(240+23,240+23);
		
		//setStyle("-fx-border-color: chocolate; -fx-border-width: 4px;");//DEBUG!!!
		
		//final MenuItem itm1 = new MenuItem("導航");
		//itm1.disableProperty().bind(pan_navi.visibleProperty());
		//itm1.setOnAction(e->pan_navi.setVisible(true));		
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
	
	private Rectangle[] mark = {
		new Rectangle(),
		new Rectangle(),
		new Rectangle(),
		new Rectangle(),
		new Rectangle(),
	};
		
	private FileChooser diag = new FileChooser();
	
	private void init(){
		
		diag.setInitialDirectory(Gawain.dirRoot);
	
		/*final Menu menu1 = new Menu("標記");
		menu1.getItems().addAll(
			itm2,
			gen_item_mark(1),
			gen_item_mark(2),
			gen_item_mark(3),
			gen_item_mark(4)
		);*/
		
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
		
		final MenuItem itm_load = new MenuItem("載入");
		itm_load.setOnAction(e->{
			diag.setTitle("載入圖檔");
			File fs = diag.showOpenDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			diag.setInitialDirectory(fs.getParentFile());
			view.setImage(new Image(fs.toURI().toString()));	
		});

		final MenuItem itm_save = new MenuItem("儲存");
		itm_save.setOnAction(e->{
			diag.setTitle("儲存圖檔");
			File fs = diag.showSaveDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			diag.setInitialDirectory(fs.getParentFile());
			try {
				RenderedImage rend = SwingFXUtils.fromFXImage(
					view.getImage(),
					null
				);
				ImageIO.write(rend,	"png",	fs);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		setContextMenu(new ContextMenu(
			itm_load,itm_save,
			men_zoom
		));
	}
	
	public void refresh(final ImgData dat){
		Image[] v = dat.getImage();
		view.setImage(v[0]);
		over.setImage(v[1]);
	}
	
	/*private void init_mark(){
		for(Rectangle m:mark){
			m.setStrokeWidth(3.);
			m.setX(10.);
			m.setY(100.);
			m.setWidth(100.);
			m.setHeight(100.);
			m.setFill(Color.TRANSPARENT);
			m.setVisible(false);
		}
		bord.getChildren().addAll(mark);
	}
	
	private MenuItem gen_item_mark(final int ID){
		final int id = ID-1;
		final Rectangle m = mark[id];
		switch(id%4){
		case 0: m.setStroke(Color.YELLOW); break;
		case 1: m.setStroke(Color.DEEPSKYBLUE); break;
		case 2: m.setStroke(Color.BLUEVIOLET); break;
		case 3: m.setStroke(Color.CRIMSON); break;
		}		
		final CheckMenuItem itm = new CheckMenuItem("mark-"+id);
		itm.setOnAction(e->m.setVisible(itm.isSelected()));
		return itm;
	}*/
}


