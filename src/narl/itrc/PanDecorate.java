package narl.itrc;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public abstract class PanDecorate extends StackPane {
	
	public static final int STYLE_DEFAULT = 0;
	public static final int STYLE_BORDER1 = 1;
	
	public static int STYLE = STYLE_BORDER1;//TODO: how to set a global flag??
	
	private Label _name = null;
	private Node  _body = null;
	
	public PanDecorate(){
		this(null,null);
	}
	
	public PanDecorate(String name){
		this(name,null);
	}

	public PanDecorate(Node body){
		this(null,body);
	}
	
	public PanDecorate(String name,Node body){
		switch(STYLE){
		case STYLE_DEFAULT:
			init_default(name,body);
			break;
		case STYLE_BORDER1:
			init_white1(name,body);
			break;
		}
	}
	
	private void init_default(String name,Node body){
		if(name!=null){
			_name = new Label(" "+name);
			_name.getStyleClass().add("group0-title");			
		}
		if(body!=null){
			_body = body;
		}else{
			_body = layoutBody();
			_body.getStyleClass().add("group0-content");
		}
		getChildren().add(_name);
		getChildren().add(_body);
		getStyleClass().add("group0-border");
		StackPane.setAlignment(_name,Pos.TOP_LEFT);		
		StackPane.setAlignment(_body,Pos.BOTTOM_LEFT);
		
	}
	
	private void init_white1(String name,Node body){		
		if(name!=null){
			_name = new Label(name);
			_name.getStyleClass().add("group1-title");
			_name.setMaxWidth(Double.MAX_VALUE);
		}
		if(body!=null){
			_body = body;
		}else{
			_body = layoutBody();
		}
		VBox lay = new VBox();
		lay.getStyleClass().add("vbox-small");
		lay.getChildren().add(_name);
		lay.getChildren().add(_body);
		getChildren().add(lay);
		getStyleClass().add("group1-border");	
	}
	
	public abstract Node layoutBody();
	//-----------------------------//

	/**
	 * the convenient method to generate the group frame.
	 * @param cntxt - group context
	 * @return
	 */
	public static Pane group(final Node cntxt){
		return new PanDecorate(cntxt){
			@Override
			public Node layoutBody() {
				return cntxt;//do nothing~~~
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
