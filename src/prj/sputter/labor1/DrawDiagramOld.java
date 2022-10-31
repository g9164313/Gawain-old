package prj.sputter.labor1;

import java.util.Arrays;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;


public class DrawDiagramOld extends AnchorPane  {
	
	protected interface DrawNode<T> {
		T locate(final int xx, final int yy);
	};
	
	protected class Pipe1 extends SVGPath 
	  implements DrawNode<Pipe1>
	{
		final public BooleanProperty state = new SimpleBooleanProperty(false);
		public Color[] color = {
			Color.TRANSPARENT,
			Color.CORNFLOWERBLUE,
		};
		public Pipe1(
			final char fmt,
			final int... arg
		) {
			String txt = "";
			switch(fmt) {
			case '▏':
			case '▕':
				int dy = arg[0];
				int sw = arg[1];
				txt = txt + String.format(
					"M0,0 v%d htarget%d v%d h%d", 
					dy, sw, -dy, -sw 
				);
				break;
			case '▔':
			case '▁':
				int dx = arg[0];
				sw = arg[1];
				txt = txt + String.format(
					"M0,0 h%d v%d h%d v%d", 
					dx, sw, -dx, -sw
				);
				break;
			
			case '▛'://U+259B
				dx = arg[0];
				dy = arg[1];
				sw = arg[2];
				double asw = sw/3.;
				txt = txt + String.format(
				  "M%d,%d h%d a%d,%d %d %d %d %d,%d v%d "+
				  "h%d "+
				  "v%.2f a%.2f,%.2f %d %d %d %.2f,%.2f h%.2f "+
				  "v%d",
				   dx,0, -dx+sw, sw,sw,0,0,0,-sw,sw, dy-sw, 
				   sw,
				   -dy+sw+asw, asw,asw,0,0,1,asw,-asw, dx-sw-asw,
				   -sw);
				break;
			case '▟'://U+259F
				dx = arg[0];
				dy = arg[1];
				sw = arg[2];
				asw = sw/3.;
				txt = txt + String.format(
				  "M%d,%d v%d a%d,%d %d %d %d %d,%d h%d "+
				  "v%d "+
				  "h%.2f a%.2f,%.2f %d %d %d %.2f,%.2f v%.2f "+
				  "h%d",
				  dx,0, dy-sw, sw,sw,0,0,1,-sw,sw, -dx+sw, 
				  -sw,
				  dx-sw-asw, asw,asw,0,0,0,asw,-asw, -dy+sw+asw,
				  sw);
				break;				
			case '▜'://U+259C
				dx = arg[0];
				dy = arg[1];
				sw = arg[2];
				asw = sw/3.;
				txt = txt + String.format(
				  "M0,0 h%d a%d,%d %d %d %d %d,%d v%d "+
				  "h%d "+
				  "v%.2f a%.2f,%.2f %d %d %d %.2f,%.2f h%.2f "+
				  "v%d",
				   dx-sw, sw,sw,0,0,1,sw,sw, dy-sw, 
				   -sw,
				   -dy+sw+asw, asw,asw,0,0,0,-asw,-asw, -dx+sw+asw,
				   -sw);
				break;
			case '▙'://U+2599
				dx = arg[0];
				dy = arg[1];
				sw = arg[2];
				asw = sw/3.;
				txt = txt + String.format(
				  "M0,0 v%d a%d,%d %d %d %d %d,%d h%d "+
				  "v%d "+
				  "h%.2f a%.2f,%.2f %d %d %d %.2f,%.2f v%.2f "+
				  "h%d",
				   dy-sw, sw,sw,0,0,0,sw,sw, dx-sw, 
				   -sw,
				   -dx+sw+asw, asw,asw,0,0,1,-asw,-asw, -dy+sw+asw,
				   -sw);
				break;

			case '▚'://U+259A
				dx = arg[0];
				dy = arg[1];
				sw = arg[2];
				asw = sw/3.;
				txt = txt + String.format(
				  "M0,0"+
				  "h%.2f a%d,%d %d %d %d %d,%d "+
				  "v%.2f a%.2f,%.2f %d %d %d %.2f,%.2f "+
				  "h%.2f v%d "+
				  "h%.2f a%d,%d %d %d %d %d,%d "+
				  "v%.2f a%.2f,%.2f %d %d %d %.2f,%.2f "+
				  "h%.2f v%d ",
				   (dx-sw)/2., sw,sw,0,0,1,sw,sw, 
				   dy-2.*sw-asw, asw,asw,0,0,0,asw,asw, 
				   (dx-sw)/2.-asw, sw,
				  -(dx-sw)/2., sw,sw,0,0,1,-sw,-sw,
				  -dy+2.*sw+asw, asw,asw,0,0,0,-asw,-asw,
				  -(dx-sw)/2.+asw, -sw
				);
				break;
			case '▞'://U+259E
				dx = arg[0];
				dy = arg[1];
				sw = arg[2];
				asw = sw/3.;
				txt = txt + String.format(
				  "M%d,0"+
				  "h%.2f a%d,%d %d %d %d %d,%d "+
				  "v%.2f a%.2f,%.2f %d %d %d %.2f,%.2f "+
				  "h%.2f v%d "+
				  "h%.2f a%d,%d %d %d %d %d,%d "+
				  "v%.2f a%.2f,%.2f %d %d %d %.2f,%.2f "+
				  "h%.2f v%d ",
				   dx,
				  -(dx-sw)/2., sw,sw,0,0,0,-sw,sw, 
				   dy-2.*sw-asw, asw,asw,0,0,1,-asw,asw, 
				  -(dx-sw)/2.+asw, sw,
				   (dx-sw)/2., sw,sw,0,0,0,sw,-sw,
				  -dy+2.*sw+asw, asw,asw,0,0,1, asw,-asw,
				   (dx-sw)/2.-asw, -sw
				);
				break;
			}
			setContent(txt);
			setStrokeWidth(3.);
			setStroke(Color.BLACK);
			setFill(color[0]);
			
			state.addListener((obv,oldVal,newVal)->{
				if(newVal==true) {
					setFill(color[1]);
				}else {
					setFill(color[0]);
				}
			});
		}
		@Override
		public Pipe1 locate(final int xx, final int yy) {
			set_location(this,xx,yy);
			return this;
		}
	};
	
	protected class PipeN extends SVGPath 
	  implements DrawNode<PipeN>
	{
		@Override
		public PipeN locate(int xx, int yy) {
			set_location(this,xx,yy);
			return this;
		}
	}
	
	
	protected void set_location(final Node obj,final int xx, final int yy) {
		if(getChildren().contains(obj)==false) {
			getChildren().add(obj);
		}
		AnchorPane.setLeftAnchor(obj, (double)xx);
		AnchorPane.setTopAnchor (obj, (double)yy);
	}
	
	protected void modeEdit() {
		
		final int btn_w=57, btn_h=32;
		final Button btn_edit = new Button("edit");
		final Button btn_save = new Button("save");
		final Button[] btn = { btn_edit, btn_save };
		for(int i=0; i<btn.length; i++) {
			btn[i].setPrefSize(btn_w, btn_h);
			AnchorPane.setRightAnchor (btn[i], (double)(13));
			AnchorPane.setBottomAnchor(btn[i], (double)((btn_h+13)*(btn.length-i)));
		}
		getChildren().addAll(btn);
		
		btn_edit.setOnAction(e->{
			//show editor for geometry
			Node obj = find_selected();
			if(obj!=null) {
				edit_geometry(obj);
			}
		});
		btn_save.setOnAction(e->{
			//save diagram
		});
		
		getChildren().forEach((obj)->{
			//hook everything for edit mode~~~
			if(Arrays.asList(btn).contains(obj)==true) {
				return;
			}
			obj.setOnMouseClicked(e->{
				if(mark_target.isPresent()==true) {
					Node tar = mark_target.get();
					if(tar==obj) {
						mark_target = Optional.empty();
						tar.getStyleClass().remove(MARK_SELECTED);
						return;
					}
				}
				mark_target = Optional.of(obj);
				obj.getStyleClass().add(MARK_SELECTED);
			});
		});
		final String css_border = 
		  "-fx-border-width: 5px;"+
		  "-fx-border-color: black;"+
		  "-fx-border-style: dashed";
		setStyle(css_border);
	}

	private static final String MARK_SELECTED = "mark-selected";
	
	private Optional<Node> mark_target = Optional.empty();
	
	private Node find_selected() {
		ObservableList<Node> lst = getChildren();
		for(int i=0; i<lst.size(); i++) {
			Node obj = lst.get(i);
			if(obj.getStyleClass().contains(MARK_SELECTED)==true) {
				return obj;
			}
		}
		return null;
	}
	
	private void edit_geometry(final Node obj) {
		int xx = AnchorPane.getLeftAnchor(obj).intValue();
		int yy = AnchorPane.getTopAnchor (obj).intValue();		
		TextField[] opt;
		GridPane lay = new GridPane();
		if(obj instanceof Region) {
			opt = new TextField[4];
			int ww = (int)((Region)obj).getPrefWidth();
			int hh = (int)((Region)obj).getPrefHeight();
			opt[2] = new TextField(String.format("%d",ww));
			opt[3] = new TextField(String.format("%d",hh));
			lay.addRow(2, new Label("Width "), opt[2]);
			lay.addRow(3, new Label("Height"), opt[3]);
		}else {
			opt = new TextField[2];
		}
		opt[0] = new TextField(String.format("%d",xx));
		opt[1] = new TextField(String.format("%d",yy));		
		lay.addRow(0, new Label("Pos-X "), opt[0]);
		lay.addRow(1, new Label("Pos-Y "), opt[1]);
		lay.getStyleClass().addAll("box-pad");	 
			
		final Dialog<Integer> dia = new Dialog<>();
		dia.setTitle("幾何設定");
		dia.getDialogPane().getButtonTypes().addAll(
			ButtonType.OK,
			ButtonType.CANCEL
		);
		dia.setResultConverter(btn->{
			if(btn==ButtonType.CANCEL) { return -1; }
			if(opt.length>=4) {
				Region box = (Region)obj;
				box.setPrefWidth (Double.valueOf(opt[2].getText()));
				box.setPrefHeight(Double.valueOf(opt[3].getText()));
			}
			AnchorPane.setLeftAnchor(obj,Double.valueOf(opt[0].getText()));				
			AnchorPane.setTopAnchor (obj,Double.valueOf(opt[1].getText()));
			return 0;
		});
		dia.getDialogPane().setContent(lay);
		dia.showAndWait();	
	}	
}
