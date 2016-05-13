package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.base.ValidatorBase;

public class BoxPhyValue extends JFXTextField implements
	EventHandler<ActionEvent>
{
	public BoxPhyValue(){
		super("???");
	}
	
	public BoxPhyValue(String title){
		setPromptText(title);
		setTooltip(new Tooltip(title));
		setOnAction(this);
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
}
