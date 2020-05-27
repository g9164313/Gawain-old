package narl.itrc;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class StepReplay extends Stepper {

	public StepReplay(){
		set(operation);
	}
	
	int index = 0;
	int count = 0;
	
	final Label msg1 = new Label();
	final TextField arg1 = new TextField("1");
	final TextField arg2 = new TextField("1");
	
	final Runnable operation = ()->{
		index+=1;
		if(index>=count){
			result.set(NEXT);
		}else{
			int val = Integer.valueOf(arg2.getText());
			result.set(-1*val);
		}
		update_msg();		
	};
	
	void update_msg(){
		msg1.setText(String.format("%3d/",index));
	}
	
	@Override
	protected void prepare(){
		super.prepare();
		index = 0;
		count = Integer.valueOf(arg1.getText());
		update_msg();
	}
	
	@Override
	protected Node getContent(){
		arg1.setPrefWidth(90);
		arg2.setPrefWidth(90);
		update_msg();
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addRow(0,new Label("重複次數"), new Label("："), msg1, arg1);
		lay.addRow(1,new Label("回跳步驟"), new Label("："), arg2);
		return lay;
	}
	
	@Override
	protected void eventEdit(){
		/*PadTouch pad = new PadTouch("重複次數:",'N');
		Optional<String> opt = pad.showAndWait();
		if(opt.isPresent()==false) {
			return;
		}
		count = Integer.valueOf(opt.get());
		update_msg();*/
	}
	
	@Override
	public String flatten() {
		return "";
	}
	@Override
	public void expand(String txt) {
	}
}
