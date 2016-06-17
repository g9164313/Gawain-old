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
		setOnAction(this);
		setPrefWidth(100);
		setPromptText(title);
		setTooltip(new Tooltip(title));		
		setValue(value);
		setMode(0);
		getValidators().add(vald);
	}
	

	@Override
	public void handle(ActionEvent event) {
		if(validate()==false){
			setText(String.valueOf(value.get()));//restore old data!!!
			return;
		}
		if(eventHand!=null){
			eventHand.handle(event);
		}else{
			eventEnter(getValue());
		}
	}

	private EventHandler<ActionEvent> eventHand = null;
	public BoxIntValue setEventEnter(EventHandler<ActionEvent> event){
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
	public SimpleIntegerProperty value = new SimpleIntegerProperty();//case by case
		
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
	
	public int getValue(){
		return value.get();
	}
	
	public void setValue(String txt){
		try{
			txt = txt.trim();
			value.set(Integer.valueOf(txt));
			setText(txt);
		}catch(NumberFormatException e){
			return;
		}
	}
	
	public void setValue(int val){
		value.set(val);
		setText(String.valueOf(val));
	}

	private ValidatorBase vald = new ValidatorBase(){
		@Override
		protected void eval() {
			String txt = BoxIntValue.this.getText();
			try{
				value.set(Integer.valueOf(txt));
				int vv = value.get();
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
