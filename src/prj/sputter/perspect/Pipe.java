package prj.sputter.perspect;

import javafx.scene.layout.Region;

public class Pipe extends PElement {

	private String s_txt = STY_BORD;
	
	public Pipe(final PDir dir) {
		super(dir);
		
		Region cc = new Region();		
		cc.setPrefSize(PIPE_SIZE, PIPE_SIZE);
		add(cc,1,1);
		
		switch(dir) {
		case HORI:
			s_txt+=String.format("-fx-border-width: %.1fpx 0px %.1fpx 0px;", PIPE_STROKE, PIPE_STROKE);
			break; 
		case VERT:
			s_txt+=String.format("-fx-border-width: 0px %.1fpx 0px %.1fpx;", PIPE_STROKE, PIPE_STROKE);			
			break;
		case LF_TP:
		case TP_LF:
			s_txt+=String.format("-fx-border-width: 0px %.1fpx %.1fpx 0px;", PIPE_STROKE, PIPE_STROKE);
			break;
		case LF_BM:
		case BM_LF:
			s_txt+=String.format("-fx-border-width: %.1fpx %.1fpx 0px 0px;", PIPE_STROKE, PIPE_STROKE);			
			break;
		case RH_TP:
		case TP_RH:
			s_txt+=String.format("-fx-border-width: 0px 0px %.1fpx %.1fpx;", PIPE_STROKE, PIPE_STROKE);			
			break;
		case RH_BM:
		case BM_RH:
			s_txt+=String.format("-fx-border-width: %.1fpx 0px 0px %.1fpx;", PIPE_STROKE, PIPE_STROKE);			
			break;
			
		case HORI_TP:
			s_txt+=String.format("-fx-border-width: 0px 0px %.1fpx 0px;", PIPE_STROKE);			
			break;
		case HORI_BM:
			s_txt+=String.format("-fx-border-width: %.1fpx 0px 0px 0px;", PIPE_STROKE);			
			break;
			
		case VERT_LF:
			s_txt+=String.format("-fx-border-width: 0px %.1fpx 0px 0px;", PIPE_STROKE);			
			break;
		case VERT_RH:
			s_txt+=String.format("-fx-border-width: 0px 0px 0px %.1fpx;", PIPE_STROKE);			
			break;
			
		case CROSS:
			break;
		}
		cc.setStyle(s_txt);
	}
}
