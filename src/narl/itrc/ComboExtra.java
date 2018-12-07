package narl.itrc;

import java.util.ArrayList;

import javafx.scene.control.ComboBox;

public class ComboExtra<T> extends ComboBox<String> {

	private ArrayList<T> lst = new ArrayList<T>();
	
	public ComboExtra(){
	}
	
	public ComboExtra(Object... args){
		addItems(args);
	}
	
	@SuppressWarnings("unchecked")
	public ComboExtra<T> addItems(Object... args){
		
		getItems().clear();
		
		int cnt = args.length / 2;		
		for(int i=0; i<cnt; i++){
			String key = (String)(args[i*2+0]);
			lst.add((T)(args[i*2+1]));
			getItems().add(key);
		}
		getSelectionModel().select(0);
		return this;
	}
	
	public ComboExtra<T> select(T b){
		int idx = 0;
		for(T a:lst){
			if(b.equals(a)==true){
				getSelectionModel().select(idx);
				return this;
			}
			idx++;
		}
		return this;
	}
	
	public T getSelected(){
		return lst.get(getSelectionModel().getSelectedIndex());
	}
}
