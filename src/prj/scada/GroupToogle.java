package prj.scada;

import com.jfoenix.controls.JFXRadioButton;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class GroupToogle extends VBox {

	private String[] arg;
		
	private JFXRadioButton[] rad;
	
	public ToggleGroup grp;
	
	public GroupToogle(String... arg){
		
		this.arg = arg;
		
		grp = new ToggleGroup();
		
		rad = new JFXRadioButton[arg.length];
		
		for(int i=0; i<rad.length; i++){
			
			rad[i] = new JFXRadioButton(arg[i]);
			
			rad[i].setToggleGroup(grp);
		}
		
		grp.selectedToggleProperty().addListener((obv,oldVal,newVal)->{
			for(int i=0; i<rad.length; i++){
				if(newVal==rad[i]){
					select(i);
					break;
				}
			}
		});
		
		propIndx.addListener((obv,oldVal,newVal)->{
			grp.selectToggle((Toggle)newVal);
		});
		//propName.addListener((obv,oldVal,newVal)->{	
		//});
		select(0);
		
		//setAlignment(Pos.CENTER_LEFT);
		getChildren().addAll(rad);
		setSpacing(20);		
	}
	
	private IntegerProperty propIndx = new SimpleIntegerProperty();
	
	public IntegerProperty selectedProperty(){
		return propIndx;
	}
	
	public int getSelectIndx(){
		return propIndx.get();
	}

	private StringProperty propName = new SimpleStringProperty();
	
	public StringProperty selectedNameProperty(){
		return propName;
	}
	
	public GroupToogle select(int idx){
		if(idx>=arg.length){
			return this;
		}
		propIndx.set(idx);
		propName.set(arg[idx]);
		rad[idx].selectedProperty().set(true);
		return this;
	}
}


