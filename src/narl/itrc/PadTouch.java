package narl.itrc;

import com.jfoenix.controls.JFXButton;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Touch pad for typing number and text.<p>
 * This is designed for equipment with touch-screen.<p> 
 * @author qq
 *
 */
public class PadTouch extends Dialog<String> {

	public PadTouch(
		final String item_name,
		final String item_value,
		final char layout_style
	) {
		setHeaderText(null);

		Node lay = create_layout(
			item_name,
			item_value,
			layout_style
		);

		getDialogPane().setContent(lay);
		getDialogPane().getStylesheets().add(Gawain.sheet);
		getDialogPane().getStyleClass().addAll("background");
		getDialogPane().getButtonTypes().addAll(
			ButtonType.CANCEL, ButtonType.OK
		);
	}
	
	public PadTouch(
		final String item_name,
		final char layout_style
	) {
		this(item_name, "", layout_style);
	}
	
	public PadTouch(
		final char layout_style
	) {
		this("輸入", "", layout_style);
	}
	
	public PadTouch() {
		this("輸入", "", 't');
	}
	
	private static final int BTN_SIZE = 42;
	
	private Node create_layout(
		final String item_name,
		final String item_value,
		final char style
	) {
		final Label[] txt = {
			new Label(item_name), 
			new Label(item_value)
		};
		txt[1].setMinWidth(73.);
		txt[1].setAlignment(Pos.CENTER_RIGHT);
		
		setResultConverter(dia->{
			ButtonData btn = (dia==null)?(null):(dia.getButtonData());
			if(btn!=ButtonData.OK_DONE) {
				return null;				
			}
			String res = txt[1].getText();
			if(res.length()==0) {
				switch(style) {
				//integer
				case 'n':
				case 'N':
				case 'i':
				case 'I':
					return "0";
				//float number
				case 'f':
				case 'F':
				case 'q':
				case 'Q':
					return "0.";
				}
			}
			return txt[1].getText();
		});
		
		final Button btnCls = new JFXButton("C");//special button - clear
		btnCls.getStyleClass().add("btn-raised-1");
		btnCls.setPrefSize(BTN_SIZE, BTN_SIZE);
		btnCls.setOnAction(e->txt[1].setText(""));
		
		final Button btnDel = new JFXButton("⇦");//special button - delete one character
		btnDel.getStyleClass().add("btn-raised-1");
		btnDel.setPrefSize(BTN_SIZE, BTN_SIZE);
		btnDel.setOnAction(e->{
			String v = txt[1].getText();
			if(v.length()==0) {
				return;
			}
			v = v.substring(0, v.length()-1);
			txt[1].setText(v);
		});
		
		final Button btnDot = new JFXButton(".");//special button - dot
		btnDot.getStyleClass().add("btn-raised-1");
		btnDot.setPrefSize(BTN_SIZE, BTN_SIZE);
		btnDot.setOnAction(e->appear_once(btnDot,txt[1]));
		
		final Button btnSign = new JFXButton("-");//special button - sign
		btnSign.getStyleClass().add("btn-raised-1");
		btnSign.setPrefSize(BTN_SIZE, BTN_SIZE);
		btnSign.setOnAction(e->prefix_once(btnSign,txt[1]));
		
		final Button btnColon = new JFXButton(":");//special button - sign
		btnColon.getStyleClass().add("btn-raised-1");
		btnColon.setPrefSize(BTN_SIZE, BTN_SIZE);
		btnColon.setOnAction(e->append(btnColon,txt[1]));
		
		GridPane lay1 = null;
		switch(style) {
		//nature (positive integer, include 0)
		case 'n':
		case 'N':
			lay1 = gen_number_pad(txt[1],btnCls,btnDel);
			break;
		//integer (positive and negative integer)
		case 'i':
		case 'I':
			lay1 = gen_number_pad(txt[1],btnCls,btnDel,btnSign);
			break;
		//rational (number with decimal point)
		case 'f':
		case 'F':
		case 'q':
		case 'Q':
			lay1 = gen_number_pad(txt[1],btnCls,btnDel,btnSign,btnDot);
			break;
		//clock (hh:mm:ss)
		case 'c':		
		case 'C':
			lay1 = gen_number_pad(txt[1],btnCls,btnDel,btnColon);
			break;
		//plain text~~~
		default:
		case 't':
		case 'T':
			break;
		}
		
		final HBox lay2 = new HBox();
		lay2.getStyleClass().addAll("box-pad","border");
		lay2.getChildren().addAll(txt);

		final VBox lay0 = new VBox();
		lay0.getStyleClass().addAll("box-pad-inner");
		lay0.getChildren().addAll(lay2, lay1);		
		return lay0;
	}
	
	private GridPane gen_number_pad(
		final Label display,
		final Button... spec
	) {
		Button[] btn = new JFXButton[10];
		for(int i=0; i<btn.length; i++) {
			final JFXButton obj = new JFXButton(""+i);
			obj.getStyleClass().add("btn-raised-1");
			obj.setPrefSize(BTN_SIZE, BTN_SIZE);
			obj.setOnAction(e->append(obj,display));
			btn[i] = obj;			
		}
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addRow(0, btn[7], btn[8], btn[9], spec[0]);
		lay.addRow(1, btn[4], btn[5], btn[6], spec[1]);
		lay.addRow(2, btn[1], btn[2], btn[3]);
		lay.add(btn[0], 1, 3);
		switch(spec.length) {
		case 3:
			lay.add(spec[2], 0, 3);
			break;
		case 4:			
			lay.add(spec[2], 0, 3);
			lay.add(spec[3], 2, 3);
			break;
		}
		return lay;
	}
	
	
	private void append(final Button btn, final Label txt) {
		String v = txt.getText();
		txt.setText(v+btn.getText());
	}
	
	private void appear_once(final Button btn, final Label txt) {
		String v = txt.getText();
		String c = btn.getText();
		if(v.contains(c)==true) {
			return;
		}
		txt.setText(v+c);
	}
	
	private void prefix_once(final Button btn, final Label txt) {
		String v = txt.getText();
		String c = btn.getText();
		if(v.startsWith(c)==true){
			txt.setText(v.substring(c.length()));
		}else {
			txt.setText(c+v);
		}
	}
	
	public static String toSecond(final String txt) {
		return clock_to_value(txt,1);
	}
	public static String toMillsec(final String txt) {
		return clock_to_value(txt,1000);
	}
	public static String clock_to_value(final String txt, final int scale) {
		String[] col;
		if(txt.matches("[\\d]+")==true) {
			int ss = Integer.valueOf(txt);
			return String.format("%d", (ss)*scale);
		}else if(txt.matches("[\\d]+[:][\\d]+")==true) {
			//mm:ss
			col = txt.split(":");
			int mm = Integer.valueOf(col[0]);
			int ss = Integer.valueOf(col[1]);
			return String.format("%d", (mm*60 + ss)*scale);
		}else if(txt.matches("[\\d]+[:][\\d]+[:][\\d]+")==true) {
			//hh:mm:ss
			col = txt.split(":");
			int hh = Integer.valueOf(col[0]);
			int mm = Integer.valueOf(col[1]);
			int ss = Integer.valueOf(col[2]);
			return String.format("%d", (hh*60*60 + mm*60 + ss)*scale);
		}
		return "0";
	}
	
}
