package narl.itrc;

import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class StepSticker extends Stepper {

	public StepSticker(){
		//set(op);
	}
	//final Runnable op = ()->{};
	
	final Label msg = new Label();
	
	@Override
	public Node getContent(){
		Separator ss1 = new Separator();
		Separator ss2 = new Separator();
		HBox.setHgrow(ss1, Priority.ALWAYS);
		HBox.setHgrow(ss2, Priority.ALWAYS);
		HBox lay = new HBox(ss1,msg,ss2);
		lay.setAlignment(Pos.CENTER);
		return lay;
	}
	@Override
	public void eventEdit(){
		TextInputDialog dia = new TextInputDialog();
		//dia.setTitle("Text Input Dialog");
		//dia.setHeaderText("Look, a Text Input Dialog");
		dia.setContentText("內容:");
		Optional<String> res = dia.showAndWait();
		if (res.isPresent()){
		   msg.setText(res.get());
		}
	}
	
	@Override
	public String flatten() {
		final String txt = msg.getText();
		if(txt.length()==0){
			return "";
		}
		return String.format("msg:%s",txt);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
			return;
		}
		String[] arg = txt.split(":|,");
		msg.setText(arg[1]);
	}
}
