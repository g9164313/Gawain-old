package narl.itrc;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.base.ValidatorBase;

public class BoxIntValue extends JFXTextField implements
	EventHandler<ActionEvent>
{
	public BoxIntValue(){
		this("",0,0);
	}
	
	public BoxIntValue(String title){
		this(title,0,0);
	}
	
	public BoxIntValue(int value){
		this("",value,0);
	}
	
	public BoxIntValue(String title,int value){
		init(title,value,0);
	}
	
	public BoxIntValue(String title,int value,int mode){
		init(title,value,mode);
	}
	
	private void init(String title,int value,int mode){
		textProperty().bind(propValue.asString());		
		setOnAction(this);
		setPrefWidth(130);
		setPromptText(title);
		setTooltip(new Tooltip(title));		
		propValue.set(value);
		setMode(0);
		getValidators().add(vald);
		setOnMouseClicked(event->{
			textProperty().unbind();
		});
	}
	
	@Override
	public void handle(ActionEvent event) {		
		if(validate()==false){
			setText(String.valueOf(propValue.get()));//restore old data!!!
			return;
		}
		if(eventHand!=null){
			eventHand.handle(event);
		}else{
			eventEnter(propValue.get());
		}
		textProperty().bind(propValue.asString());
	}

	private EventHandler<ActionEvent> eventHand = null;
	
	public BoxIntValue setEvent(EventHandler<ActionEvent> event){
		eventHand = event;
		return this;
	}
	
	/**
	 * user can override/hook this function.<p>
	 * @param value
	 */
	protected void eventEnter(int value){
	}
	
	/**
	 * this variable control how to parse value.<p>
	 * -1 : only negative value is valid.<p>
	 *  0 : positive and negative value are valid
	 * +1 : only positive value is valid.<p>
	 */
	private int mode=0;//-1:only negative value, 
	public SimpleIntegerProperty propValue = new SimpleIntegerProperty();//case by case
		
	public BoxIntValue setMode(int mode){
		this.mode = mode;
		if(mode<0){
			vald.setMessage("只能輸入負整數");
		}else if(mode>0){
			vald.setMessage("只能輸入正整數");
		}else{
			vald.setMessage("只能輸入整數值");
		}
		return this;
	} 
	
	public void setInteger(String txt){
		try{
			txt = txt.trim();
			propValue.set(Integer.valueOf(txt));
		}catch(NumberFormatException e){
			Misc.loge("Wrong format->%s",txt);
			return;
		}
	}

	public void setInteger(int val){
		propValue.set(val);
	}
	
	private ValidatorBase vald = new ValidatorBase(){
		@Override
		protected void eval() {
			String txt = BoxIntValue.this.getText();
			try{
				propValue.set(Integer.valueOf(txt));
				int vv = propValue.get();
				vv =vv * mode;
				if(vv<=0){
					hasErrors.set(false);					
				}else{
					hasErrors.set(true);
				}
			}catch(NumberFormatException e){
				hasErrors.set(true);
			}			
		}
	};
}
