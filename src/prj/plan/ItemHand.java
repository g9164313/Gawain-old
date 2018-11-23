package prj.plan;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ItemHand {

	
	
	public final StringProperty name = new SimpleStringProperty();
	public final StringProperty phone= new SimpleStringProperty();
	public final StringProperty zone = new SimpleStringProperty();//location
	
	public final IntegerProperty loading = new SimpleIntegerProperty();
	
	public String getName(){
		return name.get();
	}
	
	public String getPhone(){
		return phone.get();
	}
	
	public String getZone(){
		return zone.get();
	}
	
	public int getLoading(){
		return loading.get();
	}
}
