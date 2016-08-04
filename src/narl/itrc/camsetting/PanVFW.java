package narl.itrc.camsetting;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import narl.itrc.CamVidcap;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * This is the setting panel for VFW/V4L/V4L2  
 * @author qq
 *
 */
public class PanVFW extends ScrollPane implements PanBase.EventHook {

	private static final String CMD_V4L2 = "v4l2-ctl";
	
	private String ARG_NAME = "";
	
	public PanVFW(PanBase pan,CamVidcap device){
		pan.hook = this;
		dev = device;
		initLayout();
	}
	
	private CamVidcap dev;
	
	private ArrayList<BoxProp> lstBox = new ArrayList<BoxProp>();
	
	private void initLayout(){		
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("grid-small");
		lay0.setAlignment(Pos.CENTER);
		
		ARG_NAME = "--device=/dev/video"+dev.getIndex();
		
		parse_prop(Misc.syncExec(CMD_V4L2,ARG_NAME,"--list-ctrls-menus"));		
		
		lay0.addRow(0,new Label("項目"),new Label("控制"));
		for(int i=0; i<lstBox.size(); i++){
			BoxProp box = lstBox.get(i);
			lay0.add(box.name, 0, i+1);
			lay0.add(box.genContainer(), 1, i+1);
			lay0.getStyleClass().add("vbox-small");
		}		
		
		setPrefWidth(200);
		setPrefHeight(300);
		setContent(lay0);
	}
	
	private class BoxProp {
		public BoxProp(String name,String type,String vals){
			this.name = new Label(name);			
			if(type.equalsIgnoreCase("int")==true){
				genScroll(vals);
			}else if(type.equalsIgnoreCase("menu")==true){
				genCombo(vals);
			}else if(type.equalsIgnoreCase("bool")==true){
				genChecker(vals);
			}else{
				ctrl = new Label("no support-->"+type); 
			}
		}
		public int min,max,stp,def,val;
		public Label name,indi;
		public Control ctrl;
		public CheckBox flag = null;
		public HBox genContainer(){
			HBox lay = new HBox();			
			lay.getStyleClass().add("vbox-small");
			lay.setAlignment(Pos.CENTER_LEFT);
			lay.getChildren().add(ctrl);
			if(indi!=null){
				lay.getChildren().add(indi);
			}
			if(flag!=null){
				ctrl.disableProperty().bind(flag.selectedProperty().not());
				lay.getChildren().add(flag);
			}			
			return lay;
		}
		private void genScroll(String vals){
			mapping(vals.split("\\s"));
			final ScrollBar sld = new ScrollBar();
			sld.setMin(min);
			sld.setMax(max);
			sld.setValue(val);
			sld.setUnitIncrement(stp);
			sld.setPrefWidth(200);
			sld.setSnapToPixel(true);
			sld.valueProperty().addListener(event->{
				int _val = (int)sld.getValue();
				_val = _val + _val%stp;
				exec(_val);
			});
			indi = new Label();
			indi.textProperty().bind(sld.valueProperty().asString("%.0f"));			
			ctrl = sld;
		}
		private void genCombo(String vals){
			String[] tmp = vals.split("&");
			mapping(tmp[0].split("\\s"));
			String[] opt = tmp[1].split(";");
			final ComboBox<String> cmb = new ComboBox<String>(FXCollections.observableArrayList(opt));
			for(int i=0; i<opt.length; i++){
				if(opt[i].contains(""+val+":")==true){
					cmb.getSelectionModel().select(opt[i]);
					break;
				}
			}
			cmb.valueProperty().addListener(event->{
				String _itm = cmb.getSelectionModel().getSelectedItem();
				String _val = _itm.substring(0,_itm.indexOf(":"));
				exec(_val);
			});
			ctrl = cmb;
		}
		private void genChecker(String vals){
			mapping(vals.split("\\s"));
			final CheckBox chk = new CheckBox();
			if(val!=0){
				chk.setSelected(true);
			}else{
				chk.setSelected(false);
			}
			chk.setOnAction(event->{
				int _val = (chk.isSelected()==true)?(1):(0);
				exec(_val);
			});
			ctrl = chk;
		}
		private void mapping(String[] args){
			for(String v:args){
				if(v.contains("min")==true){
					min = getValue(v);
				}else if(v.contains("max")==true){
					max = getValue(v);
				}else if(v.contains("step")==true){
					stp = getValue(v);
				}else if(v.contains("default")==true){
					def = getValue(v);
				}else if(v.contains("value")==true){
					val = getValue(v);
				}else if(v.contains("flags")==true){
					flag = new CheckBox();
					if(v.contains("inactive")==true){
						flag.setSelected(false);
					}else{
						flag.setSelected(true);
					}
				}
			}
		}
		private int getValue(String txt){
			int pos = txt.indexOf("=");
			if(pos<0){
				return -1;
			}
			return Integer.valueOf(txt.substring(pos+1).trim());
		}
		private void exec(String _val){
			String msg = Misc.syncExec(
				CMD_V4L2,ARG_NAME,
				"--set-ctrl="+name.getText()+"="+_val
			);
			if(msg.contains("error")==true){
				name.setTextFill(Color.RED);
			}else{
				name.setTextFill(Color.BLACK);
			}
		}
		private void exec(int _val){
			exec(""+val);
		}
	};
	
	//TODO:how to decide active flags option?? 
	private void parse_prop(String txt){
		String arg;
		int pos;
		while(txt.length()!=0){
			pos = txt.indexOf('\n');
			if(pos<0){
				break;
			}
			arg = txt.substring(0,pos).trim();
			txt = txt.substring(pos+1);//for next line~~~
			pos = arg.indexOf('(');
			if(pos<0){
				continue;
			}
			String name = arg.substring(0,pos).trim();
			arg = arg.substring(pos+1);
			pos = arg.indexOf(')');
			if(pos<0){
				continue;
			}
			String type = arg.substring(0,pos).trim();
			arg = arg.substring(pos+1);
			pos = arg.indexOf(':');
			if(pos<0){
				continue;
			}
			String vals = arg.substring(pos+1).trim();
			if(type.equalsIgnoreCase("menu")==true){
				//it is a special case~~~
				pos = vals.indexOf("max=");
				String tmp = vals.substring(
					pos+4,
					vals.indexOf(' ',pos)
				);
				tmp = tmp + ":";
				vals= vals+ "&";//try to append menu options
				String opt="";
				while(txt.length()!=0 && opt.startsWith(tmp)==false){
					pos = txt.indexOf('\n');
					if(pos<0){
						break;
					}
					opt = txt.substring(0,pos).trim();
					vals = vals + opt +";";
					txt = txt.substring(pos+1);//for next line~~~				
				}
			}
			lstBox.add(new BoxProp(name,type,vals));
			System.out.println(name+"%"+type+"%"+vals);
		}
	}
	
	@Override
	public void eventShowing(WindowEvent e) {

	}

	@Override
	public void eventShown(WindowEvent e) {

	}

	@Override
	public void eventWatch(int cnt) {
	}

	@Override
	public void eventClose(WindowEvent e) {
	}
}
