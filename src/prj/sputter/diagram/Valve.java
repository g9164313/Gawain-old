package prj.sputter.diagram;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import narl.itrc.Misc;

public class Valve extends PixTile {
	
	private final Node[] lst = {null, null, null};// node[1] flowed to node[2]
	
	public final BooleanProperty flowIn = new SimpleBooleanProperty();
	public final BooleanProperty flowOut= new SimpleBooleanProperty();
	
	public Valve(final PixDir dir) {
		super(dir);
		
		switch(dir) {
		case HORI:
		case LF_RH:
			lst[1]=lf; lst[2]=rh;
			break;
		case RH_LF:		
			lst[1]=rh; lst[2]=lf;
			break;
		case TP_BM:
			lst[1]=tp; lst[2]=bm;
			break;
		case VERT:
		case BM_TP:		
			lst[1]=bm; lst[2]=tp;
			break;
		case LF_TP:
			lst[1]=lf; lst[2]=tp;
			break;
		case TP_LF:
			lst[1]=tp; lst[2]=lf;
			break;
		case LF_BM:
			lst[1]=lf; lst[2]=bm;
			break;
		case BM_LF:
			lst[1]=bm; lst[2]=lf;
			break;
		case RH_TP:
			lst[1]=rh; lst[2]=tp;
			break;
		case TP_RH:
			lst[1]=tp; lst[2]=rh;
			break;
		case RH_BM:
			lst[1]=rh; lst[2]=bm;
			break;
		case BM_RH:
			lst[1]=bm; lst[2]=rh;
			break;
		default:
			Misc.logw("[Valve] no support flow direction");
			break;
		}
		flowIn.addListener((obv,oldVal,newVal)->update_flow_style(lst[1],newVal));
		flowOut.addListener((obv,oldVal,newVal)->update_flow_style(lst[2],newVal));
		
		lst[0] = shape_valve(dir);		
		lst[0].setOnMouseClicked(e->popup_confirm());

		add(lst[0],1,1);
		//setGridLinesVisible(true);
	}
	
	private Valve popup_confirm() {
		Shape ss = (Shape)lst[0];
		String t_head = "", t_ctxt="";
		if(ss.getFill()==C_FILL) {
			t_head = "確認關閉閥門？";
			t_ctxt = "關閉閥門";
		}else {
			t_head = "確認開啟閥門？";
			t_ctxt = "開啟閥門";
		}
		//----how to hook this----
		Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("閥門控制");		
		dia.setHeaderText(t_head);
		dia.setContentText(t_ctxt);		
		Optional<ButtonType> opt = dia.showAndWait();
		if(opt.get()==ButtonType.CANCEL) {
			return this;
		}
		//-----------------------
		if(ss.getFill()==C_FILL) {
			ss.setFill(C_EMPTY);
			//if(flowIn.isBound()==false) {
				flowIn.unbind();
				flowIn.set(false);
			//}
		}else {
			ss.setFill(C_FILL);
			//if(flowIn.isBound()==false) {
				flowIn.bind(flowOut);
			//}
		}
		return this;
	}
	
	private void update_flow_style(final Node node, final boolean flag) {
		if(node==null) {
			return;
		}
		if(flag==true) {
			set_style_fill((Region)node);
		}else {
			set_style_empty((Region)node);
		}
	}
	
	private static Shape shape_valve(final PixDir dir) {
		final String p_hori = "M -33 18 L 33 -18 V 18 L -33 -18 V 18 M 0 0 L 0 -27 M -10 -27 L 10 -27";
		final String p_vert = "M -18 33 L 18 -33 H -18 L 18 33 H -18 M 0 0 L 27 0 M 27 10 L 27 -10";
		
		Shape ss = null;		
		switch(dir) {
		case HORI:
			ss = new SVGPath();
			((SVGPath)ss).setContent(p_hori);
			break; 
		case VERT:
			ss = new SVGPath();
			((SVGPath)ss).setContent(p_vert);
			break;
		default:
			ss = new Circle(PIPE_SIZE, Color.TRANSPARENT);
			break;
		}
		ss.setStrokeWidth(3);
		ss.setStroke(Color.BLACK);
		ss.setStrokeType(StrokeType.OUTSIDE);
		ss.setFill(Color.TRANSPARENT);
		return ss;
	}
	
	private PanInfo info = null;
	
	public PanInfo createInfo() {
		if(info!=null) {
			return info;
		}
		info = new PanInfo();
		switch(dir) {
		case HORI:
			getChildren().remove(bm);
			add(info, 0, 2, 3, 1);
			break; 
		case VERT: 
			getChildren().remove(rh);
			add(info, 2, 0, 1, 3);
			break;
		//------------------------
		case LF_TP:
		case TP_LF:
		case LF_BM:
		case BM_LF:
			getChildren().remove(rh);
			add(info, 2, 0, 1, 3);
			break;
		case RH_TP:
		case TP_RH:
		case RH_BM:
		case BM_RH:
			getChildren().remove(lf);
			add(info, 0, 0, 1, 3);
			break;
		//------------------------
		case HORI_TP:
			getChildren().remove(bm);
			add(info, 0, 2, 3, 1);
			break;
		case HORI_BM:
			getChildren().remove(tp);
			add(info, 0, 0, 3, 1);
			break;
				
		case VERT_LF:
			getChildren().remove(rh);
			add(info, 2, 0, 1, 3);
			break;
		case VERT_RH:
			getChildren().remove(lf);
			add(info, 0, 0, 1, 3);
			break;
		//------------------------
		default: break;
		}
		return info;
	}
	
	
	/*private static Shape shape_cross() {
		final double rad = PIPE_SIZE*0.73;
		Shape s1 = new Line(-rad,-rad, rad, rad);
		s1.setStrokeWidth(1.9);
		Shape s2 = new Line(-rad, rad, rad,-rad);		
		s2.setStrokeWidth(1.9);		
		return Shape.union(s1, s2);
	}*/
}
