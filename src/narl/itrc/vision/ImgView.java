package narl.itrc.vision;

import java.awt.image.RenderedImage;
import java.io.File;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;


public class ImgView extends StackPane {
	
	//show original image~~
	private final ImageView view = new ImageView();
	//show information, result or overlay-image
	private final ImageView over = new ImageView();	

	//parent will include these nodes
	protected final StackPane  lay0 = new StackPane (view, over);
	protected final ScrollPane lay1 = new ScrollPane(lay0);
	
	protected final ContextMenu menu = new ContextMenu();
	
	public ImgView(){
		lay1.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		lay1.setVbarPolicy(ScrollBarPolicy.ALWAYS);		
		lay1.setFitToHeight(false);
		lay1.setFitToWidth(false);
		getChildren().addAll(lay1);
		setStyle("-fx-padding: 7px;");
		init_view(view);
		init_view(over);
		init_menu();
		//getChildren().add(new Button("ggyy"));
		//setStyle("-fx-border-color: chocolate; -fx-border-width: 4px;");//DEBUG!!!		
	}

	private void init_view(ImageView node) {
		StackPane.setAlignment(node, Pos.TOP_LEFT);
		node.setCache(true);
		node.setCacheHint(CacheHint.SPEED);
		node.setMouseTransparent(true);
	}
	public int width() {
		return (int)view.getImage().getWidth();
	}
	public int height() {
		return (int)view.getImage().getHeight();
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
	
	private final static FileChooser dialog = new FileChooser();

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
		
		//dialog.setInitialDirectory(Gawain.dirRoot);
		final MenuItem itm_load = new MenuItem("載入");
		itm_load.setOnAction(e->{
			dialog.setTitle("載入圖檔");
			File fs = dialog.showOpenDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			dialog.setInitialDirectory(fs.getParentFile());
			view.setImage(new Image(fs.toURI().toString()));	
		});
		final MenuItem itm_save = new MenuItem("儲存");
		itm_save.setOnAction(e->{
			dialog.setTitle("儲存圖檔");
			File fs = dialog.showSaveDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			dialog.setInitialDirectory(fs.getParentFile());
			save(fs);
		});
		
		menu.getItems().addAll(itm_load, itm_save, men_zoom);
		lay1.setContextMenu(menu);
	}
	
	public void updateView(final ImgFilm dat){
		Image[] v = dat.mirrorImg();
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


