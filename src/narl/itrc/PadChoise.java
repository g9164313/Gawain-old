package narl.itrc;

import com.jfoenix.controls.JFXRadioButton;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class PadChoise<T> extends PanDialog<T> {

	final ToggleGroup grp = new ToggleGroup();
	final JFXRadioButton[] arg;
	
	public PadChoise(@SuppressWarnings("unchecked") T... lst){
		
		arg = new JFXRadioButton[lst.length];
		for(int i=0; i<lst.length; i++) {
			JFXRadioButton rad = new JFXRadioButton(lst[i].toString());
			rad.setUserData(lst[i]);
			rad.setToggleGroup(grp);
			arg[i] = rad;
		}
		
		VBox lay = new VBox(arg);
		lay.getStyleClass().addAll("box-pad");

		init(lay);		
	}
	
	@SuppressWarnings("unchecked")
	public PadChoise<T> assign(T obj) {
		if(obj==null) {
			return this;
		}
		for(Toggle t:arg) {
			T ref = (T)t.getUserData();
			if(ref.equals(obj)==true) {
				grp.selectToggle(t);
				break;
			}
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	boolean set_result_and_close(ButtonType type) {
		if(type!=ButtonType.OK) {
			setResult(null);
		}else {
        	Toggle tgl = grp.getSelectedToggle();
        	if(tgl==null) {
        		//user don't select any options~~~
        		setResult(null);
        	}else {
        		setResult((T)tgl.getUserData());
        	}
		}
		return true;
	}	
}
