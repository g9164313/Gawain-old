package narl.itrc;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Decorate root-panel with lines.<p>
 * @author qq
 *
 */
public abstract class PanDecorate extends StackPane {
	
	public StringProperty title = new SimpleStringProperty();
	
	private Node cntxt = null; 
	
	public PanDecorate(Node body){
		this("",body);
	}
	
	public PanDecorate(String name){
		this(name,null);
	}
	
	public PanDecorate(StringProperty name){
		this(name,null);
	}
	
	public PanDecorate(StringProperty name,Node body){
		title = name;
		cntxt = body;
		getStyleClass().add("decorate0-border");
	}
	
	public PanDecorate(String name,Node body){
		title = new SimpleStringProperty(name);
		cntxt = body;
		getStyleClass().add("decorate0-border");
	}
	
	public PanDecorate build(){
		
		String name = title.get();
		
		if(name.length()!=0){			
			Label txtTitle = new Label();
			txtTitle.getStyleClass().add("decorate0-title");
			txtTitle.textProperty().bind(title);
			
			StackPane.setAlignment(txtTitle,Pos.TOP_LEFT);
			getChildren().add(txtTitle);
			
		}
		
		if(cntxt==null){
			cntxt = eventLayout();
			if(cntxt==null){
				//no context, generate a dummy one~~~
			}			
		}
		cntxt.getStyleClass().add("decorate0-content");
				
		StackPane.setAlignment(cntxt,Pos.BOTTOM_LEFT);
		getChildren().add(cntxt);
		return this;
	}
	
	public abstract Node eventLayout();

	/**
	 * the convenient method to generate the group frame.
	 * @param cntxt - group context
	 * @return
	 */
	public static PanDecorate group(final Node cntxt){
		return group("",cntxt);
	}
	
	/**
	 * the convenient method to generate the group frame.
	 * @param txt - group title
	 * @param cntxt - group context
	 * @return
	 */
	public static PanDecorate group(final String txt,final Node cntxt){
		return new PanDecorate(txt,cntxt){
			@Override
			public Node eventLayout() {				
				return cntxt;//do nothing~~~
			}
		}.build();
	}
}
