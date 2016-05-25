package narl.itrc;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public abstract class PanDecorate extends StackPane {
	
	private Label name = null;
	private Node  body = null;
	
	public PanDecorate(){
		initBody();
		getStyleClass().add("group-border");
		getChildren().addAll(body);
	}
	
	public PanDecorate(String txt){
		initTitle(txt);			
		initBody();			
		getStyleClass().add("group-border");
		getChildren().addAll(name,body);
	}

	public PanDecorate(Node cntxt){
		body = cntxt;
		getStyleClass().add("group-border");
		getChildren().addAll(body);
	}
	
	public PanDecorate(String txt,Node cntxt){
		initTitle(txt);
		body = cntxt;
		getStyleClass().add("group-border");
		getChildren().addAll(name,body);
	}
	
	private void initTitle(String txt){
		name = new Label(" "+txt);
		name.getStyleClass().add("group-title");
		StackPane.setAlignment(name,Pos.TOP_LEFT);
	}
	private void initBody(){
		body = layoutBody();
		body.getStyleClass().add("group-content");
		StackPane.setAlignment(body,Pos.BOTTOM_LEFT);
	}
	
	public abstract Node layoutBody();
	
	public static Pane group(Node cntxt){
		return new PanDecorate(cntxt){
			@Override
			public Node layoutBody() {
				return null;//do nothing~~~
			}
		};
	}
	
	public static Pane group(String txt,Node cntxt){
		return new PanDecorate(txt,cntxt){
			@Override
			public Node layoutBody() {
				return null;//do nothing~~~
			}
		};
	}
}
