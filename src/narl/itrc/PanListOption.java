package narl.itrc;

import java.util.ArrayList;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.layout.GridPane;

public abstract class PanListOption extends PanBase {
	
	class ItemOption {
		public Label name;
		public Control ctrl;
		
		public ItemOption(String title,int min,int max,int stp,int val){			
			JFXSlider sld = new JFXSlider();
			sld.setMin(min);
			sld.setMax(max);
			sld.setValue(val);
			sld.setBlockIncrement(stp);
			sld.setMajorTickUnit(stp);
			sld.setMinorTickCount(0);			
			sld.setSnapToTicks(true);
			final ChangeListener<Number> event = new ChangeListener<Number>(){
				@Override
				public void changed(
					ObservableValue<? extends Number> observable,
					Number oldValue, 
					Number newValue
				) {
					PanListOption.this.slider2value(
						name.getText(), 
						newValue.intValue()
					);
				}	
			};
			sld.valueProperty().addListener(event);
			name = new Label(title);
			ctrl = sld;
		}
		
		public ItemOption(String title,boolean flag){			
			final JFXCheckBox chk = new JFXCheckBox();
			chk.setSelected(flag);
			final EventHandler<ActionEvent> event = 
				new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event) {
					PanListOption.this.boxcheck2value(
						name.getText(),
						chk.selectedProperty().get()
					);
				}
			};
			chk.setOnAction(event);			
			name = new Label(title);
			ctrl = chk;
		}
		
		public ItemOption(String title,int idx,String... lst){
			JFXComboBox<String> cmb = new JFXComboBox<String>();
			for(int i=0; i<lst.length; i++){
				cmb.getItems().add(lst[i]);
			}
			cmb.getSelectionModel().select(idx);
			final EventHandler<ActionEvent> event = 
				new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event) {
					SingleSelectionModel<String> model = cmb.getSelectionModel();
					PanListOption.this.boxcombo2value(
						name.getText(),
						model.getSelectedIndex(),
						model.getSelectedItem()
					);
				}			
			};			
			cmb.setOnAction(event);
			name = new Label(title);
			ctrl = cmb;
		}
		
		public ItemOption(String title,int min,int max,int val){
			JFXTextField txt = new JFXTextField();
			String prompt = String.format("輸入整數值，範圍:%d~%d",min,max);
			txt.setPromptText(prompt);
			txt.setText(String.valueOf(val));
			ValidNumber vld = new ValidNumber(min,max);
			vld.setMessage(prompt);
			txt.getValidators().add(vld);
			final EventHandler<ActionEvent> eventEnter = new EventHandler<ActionEvent>(){
				@Override
				public void handle(ActionEvent event) {					
					if(txt.validate()==true){
						boxinteger2value(name.getText(),Integer.valueOf(txt.getText().trim()));
					}else{
						//show something???
					}
				}
			};
			final ChangeListener<Boolean> eventFocus = new ChangeListener<Boolean>(){
				private String orgText = "";
				@Override
				public void changed(
					ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue
				) {
					if(newValue==true){
						orgText = txt.getText();//keep this for restoring!!!
					}else{
						//unfocused
						if(txt.validate()==false){
							txt.setText(orgText);
						}
					}
				}
			};
			txt.setOnAction(eventEnter);
			txt.focusedProperty().addListener(eventFocus);
			name = new Label(title);
			ctrl = txt;
		}
	};

	protected ArrayList<ItemOption> lstOpt = new ArrayList<ItemOption>();
	
	public PanListOption(){		
	}
		
	public abstract void slider2value(String name,int newValue);
	public abstract void boxcheck2value(String name,boolean newValue);
	public abstract void boxcombo2value(String name,int newValue,String newTitle);
	
	public abstract void boxinteger2value(String name,int newValue);
	
	protected ItemOption addSlider(String txt,int min,int max,int stp,int val){
		if(isValid(txt)==false){ return null; }
		ItemOption itm = new ItemOption(txt,min,max,stp,val);
		lstOpt.add(itm);
		return itm;
	}
	
	protected ItemOption addSlider(String txt,int min,int max,int stp){
		if(isValid(txt)==false){ return null; }
		ItemOption itm = new ItemOption(txt,min,max,stp,(min+max)/2);
		lstOpt.add(itm);
		return itm;
	}
	
	protected ItemOption addSlider(String txt,int min,int max){
		if(isValid(txt)==false){ return null; }
		ItemOption itm = new ItemOption(txt,min,max,1,(min+max)/2);
		lstOpt.add(itm);
		return itm;
	}
	
	protected ItemOption addBoxCheck(String txt,boolean flag){
		if(isValid(txt)==false){ return null; }
		ItemOption itm = new ItemOption(txt,flag);
		lstOpt.add(itm);
		return itm;
	}
	
	protected ItemOption addBoxCombo(String txt,int idx,String... lst){
		if(isValid(txt)==false){ return null; }
		ItemOption itm = new ItemOption(txt,idx,lst);
		lstOpt.add(itm);
		return itm;
	}
	
	protected ItemOption addBoxInteger(String txt,int min,int max,int val){
		if(isValid(txt)==false){ return null; }
		ItemOption itm = new ItemOption(txt,min,max,val);
		lstOpt.add(itm);
		return itm;
	}

	private boolean isValid(String txt){
		txt = txt.trim();
		for(ItemOption itm:lstOpt){
			if(itm.name.getText().equals(txt)==true){
				return false;
			}
		}
		return true;
	}
	
	public abstract Parent rootLayout();
	
	protected Parent boxLayout(){
		GridPane pane = new GridPane();		
		pane.getStyleClass().add("grid-small");
		pane.setAlignment(Pos.CENTER);
		
		final int DEF_SIZE = 200;
		for(int i=0; i<lstOpt.size(); i++){
			ItemOption itm = lstOpt.get(i);
			pane.add(itm.name,0,i,1,1);
			pane.add(itm.ctrl,1,i,3,1);
			itm.ctrl.setPrefWidth(DEF_SIZE);
			itm.ctrl.setPrefHeight(32);
		}
		return pane;
	}
	
	@Override
	public Parent layout() {
		Parent root = rootLayout();
		if(root==null){
			root=boxLayout();
		}		
		return root;
	}
}
