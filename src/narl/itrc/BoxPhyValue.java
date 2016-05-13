package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.base.ValidatorBase;

public class BoxPhyValue extends JFXTextField implements
	EventHandler<ActionEvent>
{
	public BoxPhyValue(){
		this("???","");
	}
	
	public BoxPhyValue(String title){
		this(title,"");
	}

	public BoxPhyValue(String title,String value){
		setText(value);
		setPromptText(title);
		setTooltip(new Tooltip(title));
		setOnAction(this);
		setPrefWidth(100);
	}
	
	protected void eventEnter(double value,String unit,String text){
		//user can override this function~~~
	}
	@Override
	public void handle(ActionEvent event) {
		if(validate()==false){
			return;
		}
		eventEnter(getValue(),dstUnit,getText());
	}
	
	private String dstUnit="";
	
	public double getValue(){
		if(getText().length()==0){
			return 0.;//no data??
		}
		return Misc.phyConvert(getText(),dstUnit);
	}
	
	public float getFloat(){
		if(getText().length()==0){
			return 0.f;//no data??
		}
		return (float)Misc.phyConvert(getText(),dstUnit);
	}
	
	/**
	 * set the unit of physical value<p>
	 */
	public BoxPhyValue setType(String unit){		
		ValidatorBase vald = new ValidatorBase(){
			@Override
			protected void eval() {
				
				hasErrors.set(false);
			}
		};
		vald.setMessage("範例: 123.456"+unit);
		dstUnit = unit;
		checkType(unit);		
		getValidators().add(vald);
		return this;
	}
	
	private void checkType(String unit){
		
	}
	
	public HBox decorateTitle(){
		HBox lay0 = new HBox();
		lay0.setAlignment(Pos.BASELINE_CENTER);
		lay0.getStyleClass().add("hbox-small");
		lay0.getChildren().addAll(new Label(getPromptText()),this);
		return lay0;
	}
}
