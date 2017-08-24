package narl.itrc;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class FieldNumSlider extends HBox {
	
	public interface EventHook {
		public abstract void eventReload();
		public abstract void eventChanged(float newVal);		
	};
	
	public float val=0, min=0, max=100, stp=1;
	
	public TextField txt;
	
	private EventHook hook;
	
	public FieldNumSlider(EventHook h){
		hook = h;
		prepare();
	}
	
	public FieldNumSlider(){
		prepare();
	}
	
	public FieldNumSlider setRange(
		double minimum, 
		double maximum, 
		double stepper
	){
		return setRange(minimum,minimum,maximum,stepper);
	}
	public FieldNumSlider setRange(
		double current,
		double minimum, 
		double maximum, 
		double stepper
	){
		val = (float)current;
		min = (float)minimum;
		max = (float)maximum;
		stp = (float)stepper;
		return this;
	}
	
	private void prepare(){
		
		Button btnDW = new Button("<");
		btnDW.setOnAction(e->{
			if((val-stp)<min){
				return;
			}
			val-=stp;
			if(hook!=null){ 
				hook.eventChanged(val);
			}
			update_text();
		});
		
		Button btnUp = new Button(">");
		btnUp.setOnAction(e->{
			if(max<(val+stp)){
				return;
			}
			val+=stp;
			if(hook!=null){ 
				hook.eventChanged(val);
			}
			update_text();
		});
		
		txt = new TextField();			
		txt.setOnAction(e->{
			String _txt = txt.getText();
			try{
				float _v = Float.valueOf(_txt);
				if(min<_v && _v<max){
					val = _v;//important!!, change the value~~~
					if(hook!=null){ 
						hook.eventChanged(val);
					}					
				}	
			}catch(NumberFormatException h){
				//do nothing, we will take back the origin value~~~
			}
			update_text();
		});
		
		setVisible(false);
		setStyle(
			"-fx-background-color: palegreen;"+
			"-fx-padding: 4;"+
			"-fx-background-radius: 5;"+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
		);
		getChildren().addAll(btnDW,txt,btnUp);		
	}
	
	public void reload(){
		boolean flag = !isVisible();
		if(flag==true){
			if(hook!=null){ 
				hook.eventReload();
			}
			update_text();
		}
		setVisible(flag);
	}
	
	private void update_text(){
		txt.setText(String.format("%.1f", val));
	}
}

