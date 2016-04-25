package prj.seesaw;

import com.jfoenix.controls.JFXButton;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.CamBundle;
import narl.itrc.CamVidcap;
import narl.itrc.PanBase;

public class Entry extends PanBase {

	public Entry(){
		panTitle="晶圓對位程式";
	}
	
	public DevMP21xx stage= new DevMP21xx();
	public CamBundle cam0 = new CamVidcap();
	public CamBundle cam1 = new CamVidcap();
	//------------------------//
	
	@Override
	protected void eventShown(WindowEvent event){
		//this method happened when windows pop-up....
		stage.setup(0,null);
		cam0.setup(0,null);
		cam1.setup(1,null);
		watchStart(100);
	}
	
	@Override
	protected void eventWatch(int cnt){
		//period work - GUI thread
		cam0.fetch();
		cam1.fetch();
		view[0].setImage(cam0.getImage());
		view[1].setImage(cam1.getImage());
	}
	//------------------------//
	
	@Override
	public Parent layout() {		
		TabPane root = new TabPane();
		root.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		Tab page = new Tab("對位");
		page.setContent(layout_Main());
		root.getTabs().add(page);
		
		page = new Tab("教導");
		page.setContent(layout_Teach());
		root.getTabs().add(page);
		
		page = new Tab("設定DIO");
		page.setContent(layout_SetDIO());
		root.getTabs().add(page);
		
		page = new Tab("設定MP21xx");
		page.setContent(layout_SetMP21xx());
		root.getTabs().add(page);
		
		return root;
	}
	
	private ImageView[] view = new ImageView[2];

	public PanMotion panMotion;
	
	private Pane layout_Main(){
		BorderPane root = new BorderPane();
		
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("vbox-small");
		for(int i=0; i<2; i++){
			view[i] = new ImageView();
			view[i].setFitWidth(320);
			view[i].setFitHeight(240);
			view[i].setPreserveRatio(true);
			lay0.getChildren().add(view[i]);
		}		
		root.setCenter(lay0);
		
		panMotion = new PanMotion(stage);
		
		DoubleBinding sizeh= panMotion.heightProperty().multiply(0.3);
		Label txt = new Label("-information-");
		txt.prefHeightProperty().bind(sizeh);
		
		JFXButton btn1 = new JFXButton("參數設定");
		btn1.getStyleClass().add("btn-raised");
		btn1.prefWidthProperty().bind(panMotion.heightProperty());
		btn1.prefHeightProperty().bind(sizeh);
		
		TskMatch btn2 = new TskMatch(this);		
		btn2.prefWidthProperty().bind(panMotion.heightProperty());
		btn2.prefHeightProperty().bind(sizeh);
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			txt,
			btn1,
			btn2
		);
		
		HBox lay2 = new HBox();
		lay2.getStyleClass().add("vbox-small");
		lay2.setAlignment(Pos.CENTER_LEFT);
		lay2.getChildren().addAll(
			lay1,
			PanBase.decorate("平台操作", panMotion)
		);
		root.setBottom(lay2);
		return root;
	}
		
	private Pane layout_Teach(){
		BorderPane root = new BorderPane();
		return root;
	}
	
	private Pane layout_SetDIO(){
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-small");
		return root;
	}
	
	private Pane layout_SetMP21xx(){
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-small");
		return root;
	}
}
