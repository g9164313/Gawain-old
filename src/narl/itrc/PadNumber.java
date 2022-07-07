package narl.itrc;

import java.util.IllegalFormatException;

import com.jfoenix.controls.JFXButton;

import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;


public class PadNumber extends PanDialog<String> {

	public PadNumber() {		
		init(layout());		
		setResultConverter(callback->{
			if(callback==ButtonType.CANCEL) {
				return null;
			}
			return screen.getText();
		});		
	}

	@Override
	boolean set_result_and_close(ButtonType type) {
		String txt = screen.getText();
		if(txt.length()==0) {
			setResult(null);
			return true;
		}
		if(range[0]==null) {
			//we don't need to check range~~~~
			setResult(txt);
			return true;
		}
		try {
			if(range[0] instanceof Integer) {
				
				int val = Integer.valueOf(txt);
				if(val<range[0].intValue()) {
					val = range[0].intValue();
				}else if(range[1].intValue()<val) {
					val = range[1].intValue();
				}
				if(fmt.length()>0) { 
					txt = String.format(fmt,val);
				}else {
					txt = String.valueOf(val);
				}	
				
			}else if(range[0] instanceof Float) {
				float val = Float.valueOf(txt);
				if(val<range[0].floatValue()) {
					val = range[0].floatValue();
				}else if(range[1].floatValue()<val) {
					val = range[1].floatValue();
				}
				if(fmt.length()>0) { 
					txt = String.format(fmt,val);
				}else {
					txt = String.valueOf(val);
				}
				
			}else if(range[0] instanceof Double) {
				
				double val = Double.valueOf(txt);
				if(val<range[0].doubleValue()) {
					val = range[0].doubleValue();
				}else if(range[1].doubleValue()<val) {
					val = range[1].doubleValue();
				}
				if(fmt.length()>0) { 
					txt = String.format(fmt,val);
				}else {
					txt = String.valueOf(val);
				}
				
			}			
		}catch(NumberFormatException|IllegalFormatException e) {
			Misc.loge("[PadNumber] %s", e.getMessage());
			setResult(null);
			return false;
		}
		setResult(txt);
		return true;
	}
	
	private GridPane layout = new GridPane();
	private JFXButton btn_dot;
	
	private Number[] range = {null,null};//minimum and maximum
	
	public PadNumber subset(
		final String cur,
		final Number min, final Number max
	) {
		if(cur!=null) {
			screen.setText(cur);
		}
		range[0] = min;
		range[1] = max;
		if(range[0] instanceof Integer) {
			layout.getChildren().remove(btn_dot);
		}
		return this;
	}
	
	private String fmt = "";
	public PadNumber format(final String txt) {
		fmt = txt;
		return this;
	}
	//--------------------------------
	
	private Label screen = new Label();

	private Pane layout() {
		
		screen.getStyleClass().addAll("font-size7","box-border");
		screen.setAlignment(Pos.CENTER_RIGHT);//??
		screen.setMaxWidth(Double.MAX_VALUE);
		
		final JFXButton[] btn = {
			new JFXButton("7"),new JFXButton("8"),new JFXButton("9"),
			new JFXButton("3"),new JFXButton("4"),new JFXButton("5"),
			new JFXButton("1"),new JFXButton("2"),new JFXButton("3"),
			new JFXButton("0"),new JFXButton("+/-"),new JFXButton("."),
			new JFXButton("C"),new JFXButton("Del")
		};
		for(JFXButton obj:btn) {
			obj.getStyleClass().add("btn-raised-11");
			obj.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			GridPane.setHgrow(obj, Priority.ALWAYS);
			GridPane.setVgrow(obj, Priority.ALWAYS);
			obj.setOnAction(e->{
				String txt = screen.getText();
				txt = txt + obj.getText();
				screen.setText(txt);
			});			
		}

		//sign!!, resign action event
		btn[10].getStyleClass().add("red-font");
		btn[10].setOnAction(e->{
			String txt = screen.getText();
			if(txt.length()==0) { return; }
			if(txt.startsWith("-")==true) {
				txt = txt.substring(1);
			}else{
				txt = "-"+txt;
			}
			screen.setText(txt);
		});
		//dot!!, reassign action event
		btn_dot = btn[11];
		btn[11].setOnAction(e->{
			String txt = screen.getText();
			if(txt.length()==0 || txt.contains(".")==true) {
				return;
			}
			screen.setText(txt+".");
		});
		//clear!!, resign action event
		btn[12].setOnAction(e->{
			screen.setText("");
		});
		//delete or backspace!!, resign action event
		btn[13].setOnAction(e->{
			String txt = screen.getText();
			if(txt.length()==0) { return; }
			txt = txt.substring(0,txt.length()-1);
			screen.setText(txt);
		});

		layout.getStyleClass().addAll("box-pad");
		layout.add(screen, 0, 0, 3, 1);
		layout.add(btn[12], 0, 1, 2, 1);
		layout.add(btn[13], 2, 1, 1, 1);
		layout.addRow(2, btn[0], btn[1], btn[2]);
		layout.addRow(3, btn[3], btn[4], btn[5]);
		layout.addRow(4, btn[6], btn[7], btn[8]);
		layout.addRow(5, btn[10], btn[9], btn[11]);
		return layout;
	}	
}
