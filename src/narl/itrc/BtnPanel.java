package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.stage.WindowEvent;

import com.jfoenix.controls.JFXButton;

abstract class BtnPanel extends JFXButton implements EventHandler<ActionEvent> {

	private static ImageView icon0 = Misc.getIcon("message-outline.png");
	private static ImageView icon1 = Misc.getIcon("message-text.png");

	class PanPoper extends PanBase {
		@Override
		protected void eventShown(WindowEvent event){
			BtnPanel.this.eventShown(event);
		}
		@Override
		protected void eventClose(WindowEvent event){
			act = false;
			setFace0();
			BtnPanel.this.eventClose(event);
		}
		@Override
		public Parent layout() {
			return BtnPanel.this.eventLayout();
		}
	};
	
	private boolean act = false;
	
	private String txt0,txt1;
	
	private PanPoper pop = new PanPoper();
	
	abstract Parent eventLayout();
	abstract void eventShown(WindowEvent event);
	abstract void eventClose(WindowEvent event);
	
	public BtnPanel(String txt){
		this(txt,txt);
	}
	
	public BtnPanel(String text0,String text1){
		txt0 = text0;
		txt1 = text1;
		pop = new PanPoper();
		pop.setTitle(text0);		
		setFace0();
		setOnAction(this);
		getStyleClass().add("btn-raised");
		setMaxWidth(Double.MAX_VALUE);
	}

	@Override
	public void handle(ActionEvent event) {
		if(pop.getOwner()==null){
			pop.makeStage(getScene().getWindow());
		}
		act = !act;
		if(act==true){
			setFace1();
			pop.appear();
		}else{
			pop.dismiss();
		}
	}
	
	private void setFace0(){
		setText(txt0);
		setGraphic(icon0);
	}
	
	private void setFace1(){
		setText(txt1);
		setGraphic(icon1);
	}
}


