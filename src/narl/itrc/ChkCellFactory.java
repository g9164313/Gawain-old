package narl.itrc;

import java.lang.reflect.Field;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.util.Callback;

public class ChkCellFactory<T> implements 
	Callback<TableColumn<T,Boolean>,TableCell<T,Boolean>>
{
	private static Boolean workaround = null;
	
	private static final int PROP_FXCHECK=0;
	private static final int PROP_FXTOGGLE=1;
	
	private String propName="";
	private String clzzName="";
	private int typeBox = PROP_FXCHECK;

	public ChkCellFactory(String spec){
		String[] val = spec.split(":");
		switch(val.length){
		default:
		case 3:
			val[2] = val[2].toLowerCase().trim();
			if(val[2].startsWith("fxcheck")){
				typeBox = PROP_FXCHECK;
			}else if(val[2].startsWith("fxtoggle")){
				typeBox = PROP_FXTOGGLE;
			}			
		case 2:
			clzzName = val[1];
		case 1:
			propName = val[0];
			break;
		}
		if(workaround==null){
			String flag = Gawain.prop.getProperty("WA_ChkCellFactory","f").toLowerCase();
			if(flag.charAt(0)=='f'){
				workaround = false;
			}else{
				workaround = true;
			}			
		}
	}
	
	public ChkCellFactory(String prop,String clzz){
		init(prop,clzz,PROP_FXCHECK);
	}
	
	public ChkCellFactory(String prop,String clzz,int type){
		init(prop,clzz,PROP_FXTOGGLE);
	}
	
	private void init(String prop,String clzz,int type){
		propName = prop;
		clzzName = clzz;		
		typeBox = type;
	}
		
	@Override
	public TableCell<T, Boolean> call(TableColumn<T, Boolean> param) {
		return new ChkCellBox();
	}
	
	private Node genBox(Object obj){
		Class<?> clz = obj.getClass();
		if(clzzName.length()!=0){
			while(clz!=null){
				if(clz.getName().indexOf(clzzName)>=0){
					break;
				}
				clz = clz.getSuperclass();
			}
		}
		if(clz==null){
			Misc.loge("I can't find class:"+clzzName+" from "+obj.toString());
			return null;//we don't set graphic, it means we have problem~~~
		}		
		for(Field fd:clz.getDeclaredFields()){
			if(fd.getName().indexOf(propName)>=0){
				try {
					BooleanProperty pp = (BooleanProperty)fd.get(obj);
					switch(typeBox){
					case PROP_FXCHECK: 
						JFXCheckBox box = new JFXCheckBox();						
						pp.bindBidirectional(box.selectedProperty());
						return (Node)box; 
					case PROP_FXTOGGLE: 
						JFXToggleButton btn = new JFXToggleButton();
						pp.bindBidirectional(btn.selectedProperty());
						return (Node)btn;
					}
					return null;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				break;
			}
		}
		return null;
	}
	
	public class ChkCellBox extends TableCell<T,Boolean>{
		
		public ChkCellBox(){
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		
		@Override
	    public void updateItem(Boolean item, boolean empty) {
			if(empty==false){
				TableRow<?> row = getTableRow();
				if(row==null){
					return;
				}
				Object obj = ((Object)row.getItem());
				if(workaround==false){
					workaround = true;
					return;
				}
				setGraphic(genBox(obj));
			}
		}		
	}
}
