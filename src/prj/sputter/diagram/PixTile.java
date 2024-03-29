package prj.sputter.diagram;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class PixTile extends GridPane {
	
	protected static final double PIPE_SIZE = 23.;	
	protected static final double PIPE_STROKE = 2.1; 
	protected static final double PIPE_INSECT = PIPE_SIZE*1.6;
	
	protected static final String STY_EMPTY = 
		"-fx-background-color: WHITE;";
	protected static Color C_EMPTY = Color.WHITE;//it sould be same as STY_EMPTY.
	
	protected static final String STY_FILL = 
		"-fx-background-color: DODGERBLUE;";
	protected static Color C_FILL = Color.DODGERBLUE;//it sould be same as STY_FILL.
	
	protected static final String STY_BORD = 
		"-fx-border-color: BLACK;"+
		"-fx-border-style: SOLID;";
	
	protected static final String STY_VERT = STY_BORD + STY_EMPTY +
		String.format("-fx-border-width: 0px %.1fpx 0px %.1fpx;", PIPE_STROKE, PIPE_STROKE);
	protected static final String STY_HORN = STY_BORD + STY_EMPTY +
		String.format("-fx-border-width: %.1fpx 0px %.1fpx 0px;", PIPE_STROKE, PIPE_STROKE);
	
	public final Region tp = new Region();
	public final Region rh = new Region();
	public final Region bm = new Region();
	public final Region lf = new Region();
	
	protected final PixDir dir;
	
	public PixTile(final PixDir direction) {
		
		dir = direction;
		
		GridPane.setVgrow(this, Priority.ALWAYS);
		GridPane.setHgrow(this, Priority.ALWAYS);
		
		init_vertical(tp);
		init_vertical(bm);
		init_horizon(rh);
		init_horizon(lf);
		
		style_line(dir);
		
		add(tp, 1, 0);
		add(rh, 2, 1);
		add(bm, 1, 2);
		add(lf, 0, 1);
	}
	
	void style_line(final PixDir dir) {
		switch(dir) {
		case LF_RH:
		case RH_LF:
		case HORI:
			lf.setStyle(STY_HORN);
			rh.setStyle(STY_HORN);
			break;
		case TP_BM:
		case BM_TP:
		case VERT: 
			tp.setStyle(STY_VERT);
			bm.setStyle(STY_VERT);
			break;
		//------------------------
		case LF_TP:
		case TP_LF:
			lf.setStyle(STY_HORN);
			tp.setStyle(STY_VERT);
			break;
		case LF_BM:
		case BM_LF:
			lf.setStyle(STY_HORN);
			bm.setStyle(STY_VERT);
			break;
		case RH_TP:
		case TP_RH:
			tp.setStyle(STY_VERT);
			rh.setStyle(STY_HORN);
			break;
		case RH_BM:
		case BM_RH:
			bm.setStyle(STY_VERT);
			rh.setStyle(STY_HORN);
			break;
		//------------------------				
		case HORI_BM:
			lf.setStyle(STY_HORN);
			rh.setStyle(STY_HORN);
			bm.setStyle(STY_VERT);
			break;
		case HORI_TP:
			lf.setStyle(STY_HORN);
			rh.setStyle(STY_HORN);
			tp.setStyle(STY_VERT);
			break;
			
		case VERT_LF:
			lf.setStyle(STY_HORN);
			tp.setStyle(STY_VERT);
			bm.setStyle(STY_VERT);
			break;
		case VERT_RH:
			rh.setStyle(STY_HORN);
			tp.setStyle(STY_VERT);
			bm.setStyle(STY_VERT);
			break;
		//------------------------			
		case CROSS:
			lf.setStyle(STY_HORN);
			rh.setStyle(STY_HORN);
			tp.setStyle(STY_VERT);
			bm.setStyle(STY_VERT);
			break;
		}
	}
	
	protected void set_style_fill(final Node reg) {
		replace_style(reg,STY_EMPTY,STY_FILL);	
	}
	protected void set_style_empty(final Node reg) {
		replace_style(reg,STY_FILL,STY_EMPTY);	
	}
	private void replace_style(
		final Node reg, 
		final String txt1, final String txt2
	) {
		reg.setStyle(reg.getStyle().replaceAll(txt1, txt2));
	}
	
	private void init_vertical(final Region reg) {
		reg.setMaxWidth(PIPE_SIZE);
		GridPane.setHalignment(reg, HPos.CENTER);
		GridPane.setVgrow(reg, Priority.ALWAYS);
	}
	
	private void init_horizon(final Region reg) {
		reg.setMaxHeight(PIPE_SIZE);
		GridPane.setValignment(reg, VPos.CENTER);
		GridPane.setHgrow(reg, Priority.ALWAYS);
	}
}
