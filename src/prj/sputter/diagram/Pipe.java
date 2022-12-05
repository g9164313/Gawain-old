package prj.sputter.diagram;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.Region;


public class Pipe extends PixTile {

	private Node[] lst = {null, null, null, null, null};
	
	public final BooleanProperty flowInOut = new SimpleBooleanProperty();
	
	public Pipe connect(BooleanProperty isFlow) {
		flowInOut.bind(isFlow);
		return this;
	}
	
	public Pipe(final PixDir dir) {
		super(dir);
				
		flowInOut.addListener((obv,oldVal,newVal)->{
			for(Node rr:lst) {
				if(rr==null) {
					continue;
				}
				if(newVal==true) {
					set_style_fill(rr);
				}else {
					set_style_empty(rr);
				}
			}			
		});
				
		Region cc = new Region();		
		cc.setPrefSize(PIPE_SIZE, PIPE_SIZE);
		
		String s_txt = STY_BORD+STY_EMPTY;
		switch(dir) {
		case LF_RH:
		case RH_LF:
		case HORI:
			lst[1]=lf; lst[2]=rh;
			s_txt+=String.format("-fx-border-width: %.1fpx 0px %.1fpx 0px;", PIPE_STROKE, PIPE_STROKE);
			break;
		case TP_BM:
		case BM_TP:
		case VERT:
			lst[1]=tp; lst[2]=bm;
			s_txt+=String.format("-fx-border-width: 0px %.1fpx 0px %.1fpx;", PIPE_STROKE, PIPE_STROKE);			
			break;
		case LF_TP:
		case TP_LF:
			lst[1]=lf; lst[2]=tp;
			s_txt+=String.format("-fx-border-width: 0px %.1fpx %.1fpx 0px;", PIPE_STROKE, PIPE_STROKE);
			break;
		case LF_BM:
		case BM_LF:
			lst[1]=lf; lst[2]=bm;
			s_txt+=String.format("-fx-border-width: %.1fpx %.1fpx 0px 0px;", PIPE_STROKE, PIPE_STROKE);			
			break;
		case RH_TP:
		case TP_RH:
			lst[1]=tp; lst[2]=rh;
			s_txt+=String.format("-fx-border-width: 0px 0px %.1fpx %.1fpx;", PIPE_STROKE, PIPE_STROKE);			
			break;
		case RH_BM:
		case BM_RH:
			lst[1]=bm; lst[2]=rh;
			s_txt+=String.format("-fx-border-width: %.1fpx 0px 0px %.1fpx;", PIPE_STROKE, PIPE_STROKE);			
			break;
			
		case HORI_TP:
			lst[1]=tp; lst[2]=rh; lst[3]=lf;
			s_txt+=String.format("-fx-border-width: 0px 0px %.1fpx 0px;", PIPE_STROKE);			
			break;
		case HORI_BM:
			lst[1]=bm; lst[2]=rh; lst[3]=lf;
			s_txt+=String.format("-fx-border-width: %.1fpx 0px 0px 0px;", PIPE_STROKE);			
			break;
			
		case VERT_LF:
			lst[1]=tp; lst[2]=bm; lst[3]=lf;
			s_txt+=String.format("-fx-border-width: 0px %.1fpx 0px 0px;", PIPE_STROKE);			
			break;
		case VERT_RH:
			lst[1]=tp; lst[2]=bm; lst[3]=rh;
			s_txt+=String.format("-fx-border-width: 0px 0px 0px %.1fpx;", PIPE_STROKE);			
			break;
			
		case CROSS:
			break;
		}
		cc.setStyle(s_txt);
		
		add(cc,1,1);
		lst[0] = cc;
	}
}
