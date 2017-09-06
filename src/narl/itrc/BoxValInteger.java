package narl.itrc;

import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class BoxValInteger extends HBox {
	
	public IntegerProperty propValue = new SimpleIntegerProperty();

	private JFXTextField box = new JFXTextField();

	public BoxValInteger(){
		this("",0);
	}
	
	public BoxValInteger(String title){
		this(title,0);
	}
	
	public BoxValInteger(int value){
		this("",value);
	}
	
	public BoxValInteger(String title, int value){

		setAlignment(Pos.CENTER_LEFT);
		setSpacing(7.);
		if(title.length()!=0){
			Label txt = new Label(title);
			getChildren().add(txt);
		}

		getChildren().add(box);
		
		set(value);
		
		box.setPrefWidth(60);		
		box.setOnAction(e->{
			set();
		});
		box.focusedProperty().addListener((obv,oldVal,newVal)->{
			if(newVal==false){
				set();//it is out focus
			}
		});
		propValue.addListener((obv,oldVal,newVal)->{
			box.setText(String.format("%d",newVal));
		});
	}
	
	public void clear(){
		box.setText("");
	}
	
	public int get(){
		return propValue.get();
	}
	
	private BoxValInteger set(){
		try{
			int _v = Integer.valueOf(box.getText());
			set(_v);
		}catch(NumberFormatException e){
			box.setText(String.format("%d",get()));
		}
		return this;
	}
	
	public BoxValInteger set(int val){
		if(validRange==true){
			if(val<range[0] || range[1]<val){
				box.setText(String.format("%d",get()));//restore the original value
				return this;
			}
		}
		_set(val);
		return this;
	}
	
	private void _set(int val){
		propValue.set(val);
		box.setText(String.format("%d",val));
	}
	
	private int[] range = {0, 0};
	private boolean validRange = false;
	
	public BoxValInteger setRange(int min, int max){
		range[0] = min;
		range[1] = max;
		validRange = true;
		//check again~~~
		int val = propValue.get();
		if(val<min){
			_set(min);
		}else if(max<val){
			_set(max);
		}
		return this;
	}
}

