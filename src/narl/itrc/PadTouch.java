package narl.itrc;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Touch pad for typing number and text.<p>
 * This is designed for equipment with touch-screen.<p> 
 * @author qq
 *
 */
public class PadTouch extends Dialog<String> {
	
	//--------------------------------//
	
	public PadTouch(
		final char numeric_style,
		final String numeric_name,
		final String numeric_value			
	) {
		final DialogPane pan = new NumericPad(numeric_style);
		setTitle(numeric_name);
		pan.getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
		setDialogPane(pan);		
	}
	public PadTouch(
		final char numeric_pad_style,
		final String item_name
	) {
		this(numeric_pad_style,item_name,"");
	}
	public PadTouch(final char numeric_pad_style) {
		this(numeric_pad_style,"輸入","");
	}
	public PadTouch() {
		this('i',"輸入","");
	}
	
	private class NumericPad extends PadPaneBase {
		
		NumericPad(final char style){
		
			final Label screen = new Label();
			screen.getStyleClass().addAll(
				"black-border",
				"font-size9",
				"font-console"
			);
			screen.setMaxWidth(Double.MAX_VALUE);
			screen.setAlignment(Pos.CENTER_RIGHT);

			setResultConverter(dia->{
				ButtonData btn = (dia==null)?(null):(dia.getButtonData());
				if(btn!=ButtonData.OK_DONE) {
					return null;				
				}
				String res = screen.getText();
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
				return res;
			});
			
			final Button btnCls = new JFXButton("C");//special button - clear
			btnCls.getStyleClass().addAll("btn-raised-2","font-console");
			btnCls.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			btnCls.setOnAction(e->screen.setText(""));
			
			final Button btnDel = new JFXButton("Del");//special button - delete one character
			btnDel.getStyleClass().addAll("btn-raised-2","font-console");
			btnDel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			btnDel.setOnAction(e->{
				String v = screen.getText();
				if(v.length()==0) {
					return;
				}
				v = v.substring(0, v.length()-1);
				screen.setText(v);
			});
			
			final Button btnDot = new JFXButton(".");//special button - dot
			btnDot.getStyleClass().addAll("btn-raised-1","font-console");
			btnDot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			btnDot.setOnAction(e->appear_once(btnDot,screen));
			
			final Button btnSign = new JFXButton("-");//special button - sign
			btnSign.getStyleClass().addAll("btn-raised-1","font-console");
			btnSign.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			btnSign.setOnAction(e->prefix_once(btnSign,screen));
			
			final Button btnColon = new JFXButton(":");//special button - sign
			btnColon.getStyleClass().addAll("btn-raised-1","font-console");
			btnColon.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			btnColon.setOnAction(e->append(btnColon,screen));
			
			GridPane lay1 = null;
			switch(style) {
			//nature (positive integer, include 0)
			case 'n':
			case 'N':
				lay1 = gen_number_pad(screen,btnCls,btnDel);
				break;
			//integer (positive and negative integer)
			case 'i':
			case 'I':
				lay1 = gen_number_pad(screen,btnCls,btnDel,btnSign);
				break;
			//rational (number with decimal point)
			case 'f':
			case 'F':
			case 'q':
			case 'Q':
				lay1 = gen_number_pad(screen,btnCls,btnDel,btnSign,btnDot);
				break;
			//clock or time (hh:mm:ss)
			case 'c':		
			case 'C':
			case 't':
			case 'T':
				lay1 = gen_number_pad(screen,btnCls,btnDel,btnColon);
				break;
			}
			
			final VBox lay0 = new VBox();
			lay0.getStyleClass().addAll("box-pad");
			lay0.getChildren().addAll(screen, lay1);				
			setContent(lay0);
		}

		private static final int BTN_SIZE = 64;
		
		private GridPane gen_number_pad(
			final Label display,
			final Button... spec
		) {
			Button[] btn = new JFXButton[10];
			for(int i=0; i<btn.length; i++) {
				final JFXButton obj = new JFXButton(""+i);
				obj.getStyleClass().addAll("btn-raised-1","font-console");
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
	};
	
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
	public static String toSecond(final Optional<String> opt) {
		return toSecond(opt.get());
	}
	public static long toSecondValue(final String txt) {
		return Long.valueOf(toSecond(txt)).longValue();
	}
	public static long toSecondValue(final Optional<String> opt) {
		return toSecondValue(opt.get());
	}
	public static String toMillsec(final String txt) {
		return clock_to_value(txt,1000);
	}
	public static String toMillsec(final Optional<String> opt) {
		return toMillsec(opt.get());
	}
	public static long toMillsecValue(final String txt) {
		return Long.valueOf(toMillsec(txt)).longValue();
	}
	public static long toMillsecValue(final Optional<String> opt) {
		return toMillsecValue(opt.get());
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

	//--------------------------------//
	
	public PadTouch(final String combo_value) {
		String[] lst = combo_value.split("[,，]");
		String[][] cmb = new String[lst.length][2];
		for(int i=0; i<lst.length; i++) {
			String[] val = lst[i].split("[:=]");
			cmb[i][0] = val[0].trim();
			cmb[i][1] = val[1].trim();
		}
		final DialogPane pan = new ComboPad(cmb);
		pan.getButtonTypes().add(ButtonType.CANCEL);
		setDialogPane(pan);
	}
	
	private class ComboPad extends PadPaneBase {
		ComboPad(final String[][] cmb){
			setContent(layout(cmb));
		}
		private Node layout(final String[][] combo) {
			final Button[] lst = new Button[combo.length];
			for(int i=0; i<lst.length; i++) {
				lst[i] = new JFXButton();
				lst[i].getStyleClass().addAll(
					"btn-raised-1",
					"font-size5"
				);
				lst[i].setMaxWidth(Double.MAX_VALUE);
				lst[i].setMinWidth(200.);
				lst[i].setMinHeight(64.);
				lst[i].setText(combo[i][0]);
				lst[i].setUserData(combo[i][1]);
				lst[i].setOnAction(e->{
					Button btn = (Button)e.getSource();
					String val = (String)btn.getUserData();
					setResult(val);
				});
			}
			final VBox lay0 = new VBox();
			lay0.getStyleClass().addAll("box-pad");
			lay0.getChildren().addAll(lst);		
			return lay0;
		}
	};
	//--------------------------------//
	
	private class PadPaneBase extends DialogPane {
		
		PadPaneBase(){
			getStylesheets().add(Gawain.sheet);
		}
		
		@Override
		protected Node createButton(ButtonType buttonType) {
			final JFXButton button = new JFXButton(buttonType.getText());
			button.getStyleClass().addAll("btn-raised-3","font-console");
			button.setPrefSize(48*2, 48);

	        final ButtonData buttonData = buttonType.getButtonData();
		    ButtonBar.setButtonData(button, buttonData);
		    button.setDefaultButton(buttonType != null && buttonData.isDefaultButton());
		    button.setCancelButton(buttonType != null && buttonData.isCancelButton());
		    button.addEventHandler(ActionEvent.ACTION, ae -> {
		    	if (ae.isConsumed()) return;
		    	if (this != null) {
		    		this.impl_setResultAndClose(buttonType, true);
		    	}
		    });
	        return button;
		}
	    void impl_setResultAndClose(ButtonType cmd, boolean close) {
	    	if(cmd==ButtonType.CANCEL) {
	    		//remember!!, close dialog then, set result
	    		//don't change sequence.....
	    		close();
	    		setResult(null);
	    		return;
	    	}
	    	Callback<ButtonType, String> resultConverter = getResultConverter();	        
	    	String priorResultValue = getResult();
	        String newResultValue = null;
	        newResultValue = resultConverter.call(cmd);
	        setResult(newResultValue);
	        if (close && priorResultValue == newResultValue) {
	            close();
	        }
	    }
	};
}
