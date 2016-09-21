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
		this(null,null);
	}
	
	public PanDecorate(String txt){
		this(txt,null);
	}

	public PanDecorate(Node cntxt){
		this(null,cntxt);
	}
	
	public PanDecorate(String txt,Node nod){
		init_title(txt);
		init_frame(nod);		
		getStyleClass().add("group-border");
		if(name!=null){
			getChildren().add(name);
		}
		if(body!=null){
			getChildren().add(body);
		}	
	}
	
	private void init_title(String txt){
		if(txt==null){
			return;
		}
		name = new Label(" "+txt);
		name.getStyleClass().add("group-title");
		StackPane.setAlignment(name,Pos.TOP_LEFT);
	}
	
	private void init_frame(Node nod){
		if(nod!=null){
			body = nod;
			return;
		}
		body = layoutBody();
		body.getStyleClass().add("group-content");
		StackPane.setAlignment(this.body,Pos.BOTTOM_LEFT);
	}
	
	public abstract Node layoutBody();
	
	/**
	 * the convenient method to generate the group frame.
	 * @param cntxt - group context
	 * @return
	 */
	public static Pane group(final Node cntxt){
		return new PanDecorate(cntxt){
			@Override
			public Node layoutBody() {
				return null;//do nothing~~~
			}
		};
	}
	
	/**
	 * the convenient method to generate the group frame.
	 * @param txt - group title
	 * @param cntxt - group context
	 * @return
	 */
	public static Pane group(final String txt,final Node cntxt){
		return new PanDecorate(txt,cntxt){
			@Override
			public Node layoutBody() {
				return cntxt;//do nothing~~~
			}
		};
	}
}
