package narl.itrc;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.jfoenix.controls.JFXButton;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
//import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public abstract class Stepper extends HBox {
	
	private static final Image v_id_arrow = Misc.getIconImage("arrow-right.png");
	private static final Image v_edit_pen = Misc.getIconImage("pen.png");
	private static final Image v_tash_can = Misc.getIconImage("trash-can.png");	
	private static final Image v_cube_out = Misc.getIconImage("cube-outline.png");

	public final ImageView imgSign = new ImageView(v_id_arrow);

	private JFXButton btnEdit = new JFXButton();
	private JFXButton btnDrop = new JFXButton();
	
	protected Optional<Runnable[]> works = Optional.empty();
	protected Optional<ObservableList<Stepper>> items = Optional.empty();//we can see other stepper	
	protected String uuid = "";
	
	public Stepper(){
		
		imgSign.setVisible(false);
		
		setOnDragDetected(e->{
			//Misc.logv("setOnDragDetected");
			
			//'Dragboard' must have content!!!
			Dragboard db = startDragAndDrop(TransferMode.MOVE);
			ClipboardContent content = new ClipboardContent();
			content.putString(Stepper.this.toString());
			db.setDragView(v_cube_out);
            db.setContent(content);
            
            e.consume();
		});
		setOnDragOver(e->{
			//'getGestureSource()' is equal to object in 'TransferMode.MOVE'
			//'getSource()' is equal to object when mouse over
			if (e.getGestureSource() != this) {
            	//Misc.logv(e.getGestureSource().toString()+" over "+e.getSource().toString());
            	e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
		});
		setOnDragDropped(e->{
			Stepper dst = (Stepper)e.getSource();
			Stepper src = (Stepper)e.getGestureSource();
			if(src==null) {
				e.consume();
				return;
			}
			//Misc.logv(src.toString() + " dropped in " + dst.toString());
			ObservableList<Stepper> lst = items.get();
			lst.remove(src);
			lst.add(lst.indexOf(dst)+1, src);
            e.setDropCompleted(true);
            e.consume();
		});
		setOnDragDone(DragEvent::consume);	
	}
	
	public Stepper doLayout(){
		
		imgSign.setVisible(false);
		
		Node cntx = getContent();
		if(cntx==null) {
			cntx = new Label(this.toString());//dummy~~~
		}
		HBox.setHgrow(cntx, Priority.ALWAYS);
		
		btnEdit.setGraphic(new ImageView(v_edit_pen));
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
	
	protected void addRun(Runnable... runs){
		if(works.isPresent()==false) {
			set(runs);
		}else {
			Stream<?> lst1 = Arrays.stream(works.get());
			Stream<?> lst2 = Arrays.stream(runs);
			works = Optional.of(
				Stream.concat(lst1,lst2).toArray(Runnable[]::new)
			);
		}
	}
	
	protected Stepper get(final int cnt) {		
		if(items.isPresent()==false) {
			return null;
		}
		ObservableList<Stepper> lst = items.get();
		int idx = lst.indexOf(this) + cnt;
		if(idx<0 || lst.size()<idx) {
			return null;
		}
		return lst.get(idx);
	}	
	//-----------------------------------//
	
	public static final int SPEC_JUMP = 0x100000;
	
	public static final int LEAD =  1;
	public static final int HOLD =  0;
	public static final int BACK = -1;
	
	public static final int FORWARD = SPEC_JUMP+1;	
	public static final int ABORT   = SPEC_JUMP*1;
	public static final int PAUSE   =-SPEC_JUMP*1;
	public static final int BACKWARD=-SPEC_JUMP-1;
	
	/** 
	 *  1--> async going!!!!
	 *  0--> no async
	 * -1--> async done~~~ 
	 */
	public final AtomicInteger async = new AtomicInteger(0);
	public final AtomicInteger next = new AtomicInteger(LEAD);
			
	protected void abort_step(){ next.set(ABORT);}
	protected void pause_step(){ next.set(PAUSE);}
	protected void hold_step() { next.set(HOLD); }	
	protected void next_step() { next.set(LEAD); }
	protected void next_step(final int stp) {		
		if(Math.abs(stp)<SPEC_JUMP) { next.set(stp); }
	}
	protected void step_jump(final int stp) {
		next.set((stp>0)?(SPEC_JUMP):(-SPEC_JUMP)+stp); 
	}
	
	protected void wait_async(){
		async.set(1);
		hold_step();
	}
	protected void notify_async(final int stp){
		async.set(-1);
		next_step(stp);
	}
	protected void notify_async(){
		notify_async(LEAD);
	}
	
	protected Task<?> waiting_async(final Task<?> tsk) {
		wait_async();
		new Thread(tsk,"step-async").start();		
		return tsk;
	}
		
	private long tick = -1L;	
	protected long waiting_time(long period){
		if(tick<=0L){
			tick = System.currentTimeMillis();
		}
		long pass = System.currentTimeMillis() - tick;
		if(pass>=period){
			tick = -1L;//reset for next turn~~~
			next_step();
		}else{
			hold_step();
		}
		long rem = period - pass;
		return (rem>0)?(rem):(0);
	}
	protected long waiting_time(final String time){
		return waiting_time(Misc.text2tick(time));
	}
	
	protected void prepare(){
		imgSign.setVisible(false);
		hold_step();
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
	
	private static String def_stick_text = "";
	
	public static class Sticker extends Stepper {
		final Label msg = new Label();
		@Override
		public Node getContent(){
			set(()->{
				String txt = msg.getText();
				if(txt.length()==0){
					txt = "STICKER";
				}
				Misc.logv(">> %s <<", txt); 
				next.set(LEAD);
			});
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
			String init_text = msg.getText();
			if(init_text.length()==0) {
				init_text = def_stick_text;
			}
			TextInputDialog dia = new TextInputDialog(init_text);
			//dia.setTitle("Text Input Dialog");
			//dia.setHeaderText("Look, a Text Input Dialog");
			dia.setContentText("內容:");
			Optional<String> res = dia.showAndWait();
			if (res.isPresent()){
				def_stick_text = res.get();
				msg.setText(def_stick_text);			   
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
