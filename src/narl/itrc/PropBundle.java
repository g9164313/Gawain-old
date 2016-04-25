package narl.itrc;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class PropBundle {
	
	public SimpleStringProperty name = new SimpleStringProperty();
	public SimpleStringProperty desc1= new SimpleStringProperty();
	public SimpleStringProperty desc2= new SimpleStringProperty();
	public SimpleStringProperty value= new SimpleStringProperty();
		
	public SimpleIntegerProperty arg1 = new SimpleIntegerProperty();//case by case
	public SimpleIntegerProperty arg2 = new SimpleIntegerProperty();//case by case
	public SimpleIntegerProperty arg3 = new SimpleIntegerProperty();//case by case
	public SimpleIntegerProperty arg4 = new SimpleIntegerProperty();//case by case
	
	public PropBundle(String _name){
		this(_name,"","");
	}
	
	public PropBundle(String _name,String _desc1){
		this(_name,_desc1,"");
	}
	
	public PropBundle(String _name,String _desc1,String _value){
		name.set(_name);		
		desc1.set(_desc1);
		value.set(_value);
	}
	
	public PropBundle(String _name,String _desc,int _arg1){
		this(_name,_desc,"");
		arg1.set(_arg1);
	}
	
	public SimpleStringProperty nameProperty(){ return name; }
	public SimpleStringProperty desc1Property(){ return desc1; }
	public SimpleStringProperty desc2Property(){ return desc2; }
	public SimpleStringProperty valueProperty(){ return value; }
	
	public SimpleIntegerProperty arg1Property(){ return arg1; }
	public SimpleIntegerProperty arg2Property(){ return arg2; }
	public SimpleIntegerProperty arg3Property(){ return arg3; }
	public SimpleIntegerProperty arg4Property(){ return arg4; }
};
