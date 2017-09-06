package narl.itrc;

import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class BoxValFloat extends HBox {
	
	private int fixed;
	
	public FloatProperty propValue = new SimpleFloatProperty();
	
	private JFXTextField box = new JFXTextField();
	
	public BoxValFloat(){
		this("",0.f,2);
	}
		
	public BoxValFloat(String title){
		this(title,0.f,2);
	}
	
	public BoxValFloat(float value, int fixed){
		this("", 0.f, fixed);
	}
	
	public BoxValFloat(String title, float value, int fixed){
		
		setAlignment(Pos.CENTER_LEFT);
		setSpacing(7.);
		if(title.length()!=0){
			Label txt = new Label(title);
			getChildren().add(txt);
		}
		getChildren().add(box);
		
		this.fixed = fixed;
				
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
			box.setText(String.format("%."+fixed+"f",newVal));
		});
	}

	public void clear(){
		box.setText("");
	}
	
	public float get(){
		return propValue.get();
	}
	public String getValTxt(){
		return String.format("%."+fixed+"f",get());
	}
	
		
	private BoxValFloat set(){
		try{
			float _v = Float.valueOf(box.getText());
			set(_v);
		}catch(NumberFormatException e){
			box.setText(getValTxt());
		}
		return this;
	}
	
	public BoxValFloat set(float val){
		if(validRange==true){
			if(val<range[0] || range[1]<val){
				box.setText(getValTxt());//restore the original value
				return this;
			}
		}
		_set(val);
		return this;
	}
	
	private void _set(float val){
		propValue.set(val);
		box.setText(String.format("%."+fixed+"f",val));
	}
	
	private float[] range = {0, 0};
	private boolean validRange = false;
	
	public BoxValFloat setRange(float min, float max){
		range[0] = min;
		range[1] = max;
		validRange = true;
		//check again~~~
		float val = propValue.get();
		if(val<min){
			_set(min);
		}else if(max<val){
			_set(max);
		}
		return this;
	}
}
