package prj.plan;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.UtilRandom;

public class ItemBill extends HBox {

	public final StringProperty serialNumber = new SimpleStringProperty();
	
	public final StringProperty workingDate = new SimpleStringProperty();
	
	//contact information
	public final StringProperty name = new SimpleStringProperty();
	public final StringProperty phone= new SimpleStringProperty();
	public final StringProperty addr = new SimpleStringProperty();
	public final StringProperty memo = new SimpleStringProperty();
	
	public ItemBill(){
		init_face();
	}
	
	public ItemBill(String... val){
		gen_serial_number();
		setAll(val);
		init_face();
	}
	
	
	private void init_face(){
		
		final Label txtName = new Label();		
		txtName.textProperty().bind(name);
		txtName.setMaxWidth(20);
		
		final Label txtPhone= new Label();
		txtPhone.textProperty().bind(phone);
		
		final HBox lay1 = new HBox();
		lay1.getChildren().addAll(txtName, txtPhone);
		
		final Label txtAddr = new Label();
		txtAddr.textProperty().bind(addr);
		txtAddr.prefWidthProperty().bind(txtPhone.prefWidthProperty().add(txtName.prefWidthProperty()));
		
		final VBox lay0 = new VBox();
		lay0.getChildren().addAll(lay0,txtAddr);		
		getChildren().add(lay0);
	}
	
	private void gen_serial_number(){		
		serialNumber.set(UtilRandom.uuid(5, 10));
	}
	
	public ItemBill setAll(String... val){
		workingDate.set(val[0]);
		name.set(val[1]);
		phone.set(val[2]);
		addr.set(val[3]);
		memo.set(val[4]);
		return this;
	} 
	
	public String getSerialNumber(){
		return serialNumber.get();
	}
	
	public String getWorkingDate(){
		return workingDate.get();
	}	
}
