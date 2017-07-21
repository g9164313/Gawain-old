package narl.itrc;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TabPairItem {

	public final StringProperty key = new SimpleStringProperty("");
	public final StringProperty val = new SimpleStringProperty("");
	
	public StringProperty keyProperty() { return key; }
	public String getKey() { return key.get(); }
	public void setKey(String txt) { key.set(txt); }
	
	public StringProperty valProperty() { return val; }
	public String getVal() { return val.get(); }
	public void setVal(String txt) { val.set(txt); }
	
	
	public TabPairItem(String _key,String _val){
		key.set(_key);
		val.set(_val);
	}
	
	public TabPairItem(String _key){
		key.set(_key);
		val.set("0");
	}
}
