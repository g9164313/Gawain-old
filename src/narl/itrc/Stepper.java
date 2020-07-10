package narl.itrc;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public abstract class Stepper extends HBox {
	
	private static final Image v_empty = new WritableImage(24,24);
	private static final Image v_arrow = Misc.getIconImage("arrow-right.png");
	private static final Image v_pen = Misc.getIconImage("pen.png");
	private static final Image v_tash_can = Misc.getIconImage("trash-can.png");
		
	private ImageView imgSign = new ImageView();

	private JFXButton btnEdit = new JFXButton();
	private JFXButton btnDrop = new JFXButton();
	
	public Optional<Runnable[]> works = Optional.empty();
	public Optional<ObservableList<Stepper>> items = Optional.empty();//we can see other stepper
	
	public Stepper(){
		imgSign.setImage(v_empty);
	}
	
	public void indicator(boolean flag){
		if(flag){
			imgSign.setImage(v_arrow);
		}else{
			imgSign.setImage(v_empty);
		}
	}
	
	public Stepper doLayout(){
		
		indicator(false);
		
		Node cntx = getContent();
		HBox.setHgrow(cntx, Priority.ALWAYS);
		
		btnEdit.setGraphic(new ImageView(v_pen));
		btnEdit.setOnAction(e->eventEdit());
		
		btnDrop.setGraphic(new ImageView(v_tash_can));		
		btnDrop.setOnAction(e->dropping());
	
		setAlignment(Pos.CENTER_LEFT);
		getChildren().addAll(imgSign,cntx,btnEdit,btnDrop);
		return this;
	}
		
	private void dropping(){
		if(items.isPresent()==false){
			return;
		}
		items.get().remove(this);
	}

	protected void set(Runnable run){
		Runnable[] tmp = {run};
		works = Optional.of(tmp);
	}
	
	protected void set(Runnable... runs){
		works = Optional.of(runs);
	}
	//-----------------------------------//
	
	public static final int SPEC_JUMP = 10000;

	public static final int FORWARD= 1 + SPEC_JUMP;
	public static final int ABORT=  SPEC_JUMP;
	public static final int LEAD =  1;
	public static final int HOLD =  0;
	public static final int BACK = -1;
	public static final int PAUSE= -SPEC_JUMP;
	public static final int BACKWARD= -1 - SPEC_JUMP;
	
	protected final AtomicInteger next = new AtomicInteger(1);	
	//-----------------------------------//
	
	public boolean waiting_async = false;
	
	protected void waiting_async(){
		waiting_async = true;
		next.set(HOLD);
	}
	
	private long tick = -1L;	
	protected long waiting(long period){
		if(tick<=0L){
			tick = System.currentTimeMillis();
		}
		long pass = System.currentTimeMillis() - tick;
		if(pass>=period){
			tick = -1L;//reset for next turn~~~
			next.set(LEAD);
		}else{
			next.set(HOLD);
		}
		long rem = period - pass;
		return (rem>0)?(rem):(0);
	}
	protected long waiting(final String time){
		return waiting(Misc.time2tick(time));
	}
	
	protected void prepare(){
		indicator(false);
		next.set(HOLD);
	}
	
	public abstract Node getContent();//{
	//	Label txt = new Label("-DEFAULT-");
	//	txt.setMaxWidth(Double.MAX_VALUE);
	//	txt.getStyleClass().addAll("border");
	//	return txt;
	//}
	
	//let user pop a panel to edit parameter~~~
	public abstract void eventEdit();

	public abstract String flatten();
	public abstract void expand(String txt);
	
	//--------below lines are common stepper for ladder
	
	public static class Replay extends Stepper {
		int index = 0;
		int count = 0;
		final Label msg1 = new Label();
		final TextField arg1 = new TextField("1");
		final TextField arg2 = new TextField("1");
		public Replay() {
			set(jump);//it must be atomic operation!!
		}
		final Runnable jump = ()->{			
			if(index>=count){
				next.set(LEAD);
			}else{
				int val = Integer.valueOf(arg1.getText());
				next.set(-val-SPEC_JUMP);
				index+=1;
				update_msg();
			}
		};
		void update_msg(){
			msg1.setText(String.format("%3d/%3d",index,count));
		}
		@Override
		protected void prepare(){
			super.prepare();
			index = 0;
			count = Integer.valueOf(arg2.getText());
			update_msg();
		}
		@Override
		public Node getContent(){
			arg1.setPrefWidth(90);
			arg2.setPrefWidth(90);
			update_msg();
			GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad");
			lay.addColumn(0, msg1);
			lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
			lay.addColumn(2,new Label("回跳步驟"),new Label("重複次數"));
			lay.addColumn(3,arg1,arg2);
			return lay;
		}
		@Override
		public void eventEdit(){
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
			String[] arg = {
				arg1.getText().trim(),
				arg2.getText().trim()
			};
			return String.format("jump:%s, repeat:%s",arg[0],arg[1]);
		}
		@Override
		public void expand(String txt) {
			if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
				return;
			}
			String[] arg = txt.split(":|,");
			for(int i=0; i<arg.length; i+=2){
				final String tag = arg[i+0].toLowerCase().trim();
				final String val = arg[i+1].toLowerCase().trim();
				if(tag.equals("jump")==true){
					arg1.setText(val);
				}else if(tag.equals("repeat")==true){
					arg2.setText(val);
				}
			}
		}
	};
	
	public static class Sticker extends Stepper {
		final Label msg = new Label();
		@Override
		public Node getContent(){
			set(()->{
				Misc.logv(":%s.1", msg.getText()); next.set(LEAD);
			},()->{
				Misc.logv(":%s.2", msg.getText()); next.set(LEAD);
			},()->{
				Misc.logv(":%s.3", msg.getText()); next.set(LEAD);
			});//~~test~~
			Separator ss1 = new Separator();
			Separator ss2 = new Separator();
			HBox.setHgrow(ss1, Priority.ALWAYS);
			HBox.setHgrow(ss2, Priority.ALWAYS);
			HBox lay = new HBox(ss1,msg,ss2);
			lay.setAlignment(Pos.CENTER);
			return lay;
		}
		@Override
		public void eventEdit(){
			TextInputDialog dia = new TextInputDialog();
			//dia.setTitle("Text Input Dialog");
			//dia.setHeaderText("Look, a Text Input Dialog");
			dia.setContentText("內容:");
			Optional<String> res = dia.showAndWait();
			if (res.isPresent()){
			   msg.setText(res.get());
			}
		}
		public Sticker editValue(final String txt){
			msg.setText(txt);
			return this;
		}
		@Override
		public String flatten() {
			final String txt = msg.getText();
			if(txt.length()==0){
				return "";
			}
			return String.format("msg:%s",txt);
		}
		@Override
		public void expand(String txt) {
			if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
				return;
			}
			String[] arg = txt.split(":|,");
			msg.setText(arg[1]);
		}
	};	
}