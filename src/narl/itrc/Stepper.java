package narl.itrc;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.jfoenix.controls.JFXButton;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
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
	protected Optional<ObservableList<Stepper>> item = Optional.empty();//we can see other stepper	
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
			ObservableList<Stepper> lst = item.get();
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
		if(item.isPresent()==false){
			return;
		}
		item.get().remove(this);
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
		if(item.isPresent()==false) {
			return null;
		}
		ObservableList<Stepper> lst = item.get();
		int idx = lst.indexOf(this) + cnt;
		if(idx<0 || lst.size()<idx) {
			return null;
		}
		return lst.get(idx);
	}	
	//-----------------------------------//
	
	public static final int SPEC_JUMP = 0x800000;
	
	public static final int LEAD =  1;
	public static final int HOLD =  0;
	public static final int BACK = -1;
	
	public static final int FORWARD = SPEC_JUMP+1;	
	public static final int ABORT   = SPEC_JUMP*1;
	public static final int PAUSE   =-SPEC_JUMP*1;
	public static final int BACKWARD=-SPEC_JUMP-1;
	
	public final AtomicInteger next = new AtomicInteger(LEAD);
			
	protected void abort_step(){ next.set(ABORT); async.set(0); }
	protected void pause_step(){ next.set(PAUSE);}
	protected void hold_step() { next.set(HOLD); }	
	protected void next_step() { next.set(LEAD); }
	protected void next_step(final int stp) {		
		if(Math.abs(stp)<SPEC_JUMP) { next.set(stp); }
	}
	protected void step_jump(final int stp) {
		next.set((stp>0)?(SPEC_JUMP):(-SPEC_JUMP)+stp); 
	}
	protected void next_step(final Runnable from,final Runnable to__) {
		if(works.isPresent()==false) {
			return;
		}
		final Runnable[] lst = works.get();
		int bb=0, ee=0;
		for(int i=0; i<lst.length; i++) {
			if(from==lst[i]) { bb = i; }
			if(to__==lst[i]) { ee = i; }
		}
		next.set(ee-bb);
	}
	
	/** 
	 *  1--> async going!!!!
	 *  0--> no async
	 * -1--> async done~~~ 
	 */
	protected final AtomicInteger async= new AtomicInteger(0);	
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
	protected void wait_async(final Runnable tsk) {
		if(async.get()==1) {
			return;
		}
		wait_async();
		new Thread(()->{
			tsk.run();
			async.set(-1);
			next_step(LEAD);	
		},"stepper-task").start();
	}
	//--------------------------------------------//
	
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
	
	public static class Counter extends Stepper {
		int index = 0;
		int count = 0;
		final Label msg1 = new Label();
		final TextField arg1 = new TextField("1");
		final TextField arg2 = new TextField("1");
		public Counter() {
			set(jump);//it must be atomic operation!!
		}
		final Runnable jump = ()->{			
			if((index+1)>=count){
				next_step();
			}else{
				int val = Integer.valueOf(arg1.getText());
				step_jump(-val);				
			}
			index+=1;
			update_msg();
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
			lay.addRow(2, new Label("回跳步驟"), arg1);
			lay.addRow(3, new Label("重複次數"), arg2);
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
	
	public String control2text(Object... lst) {
		String txt = "";
		int idx = 0;
		for(Object obj:lst) {
			String val = "";
			if(obj instanceof TextField) {
				val = ((TextField)obj).getText();
			}else if(obj instanceof ComboBox<?>) {
				val = ""+((ComboBox<?>)obj)
					.getSelectionModel()
					.getSelectedIndex();
			}else if(obj instanceof CheckBox) {
				val = (((CheckBox)obj).isSelected())?("T"):("F");
			}else if(obj instanceof RadioButton) {
				val = (((RadioButton)obj).isSelected())?("T"):("F");
			}else {
				Misc.loge("[FLATTEN] ?? %s", obj.getClass().getName());
				continue;
			}
			txt = txt + String.format("arg%d=%s, ", idx, val);
			idx+=1;
		}
		//remove last dot~~~
		txt = txt.trim();
		if(txt.lastIndexOf(',')==(txt.length()-1)) {
			txt = txt.substring(0,txt.length()-1);
		}
		return txt;
	}
	protected void text2control(String txt, Object... lst) {
		String[] col = txt.trim().replace("\\s", "").split(", ");
		if(col.length==0) {
			return;
		}
		//remove last dot~~~
		String tmp = col[col.length-1];
		if(tmp.charAt(tmp.length()-1)==',') {
			col[col.length-1] = tmp.substring(0,tmp.length()-1);
		}
		//fill data~~~~
		final int cnt = (col.length>=lst.length)?(lst.length):(col.length);
		for(int i=0; i<cnt; i++) {
			final String[] map = col[i].split("=");
			//final String key = arg[0];
			if(map.length==1) {
				continue;//just empty
			}
			final String val = map[1];
			final Object obj = lst[i];
			if(obj instanceof TextField) {
				((TextField)obj).setText(val);
			}else if(obj instanceof ComboBox<?>) {
				((ComboBox<?>)obj)
					.getSelectionModel()
					.select(Integer.parseInt(val));
			}else if(obj instanceof CheckBox) {
				if(val.charAt(0)=='T') {
					((CheckBox)obj).setSelected(true);
				}else if(val.charAt(0)=='F') {
					((CheckBox)obj).setSelected(false);
				}else {
					Misc.loge("[UNFLATEN:CheckBox] %s ? boolean", col[i]);
				}
			}else if(obj instanceof RadioButton) {
				if(val.charAt(0)=='T') {
					((RadioButton)obj).setSelected(true);
				}else if(val.charAt(0)=='F') {
					((RadioButton)obj).setSelected(false);
				}else {
					Misc.loge("[UNFLATEN:RadioButton] %s ? boolean", col[i]);
				}
			}else {
				Misc.loge("[UNFLATEN] ?? %s", obj.getClass().getName());
				continue;
			}
		}
	}	
	//--------------------------------------------//
	
	private long tick = -1L;	
	protected long waiting_time(long msec){
		if(tick<=0L){
			tick = System.currentTimeMillis();
		}
		long pass = System.currentTimeMillis() - tick;
		if(pass>=msec){
			reset_waiting();//reset for next turn~~~
			next_step();
		}else{
			hold_step();
		}
		long rem = msec - pass;
		return (rem>0)?(rem):(0);
	}
	protected long waiting_time(final String time){
		return waiting_time(Misc.text2tick(time));
	}
	public Runnable run_waiting(
		final long msec,
		final Label mesg
	) {
		final Runnable obj = ()->{
			final long rem = waiting_time(msec);
			if(mesg!=null) {
				mesg.setText(Misc.tick2text(rem, true));
			}			
		};
		return obj;
	}
	public Runnable run_waiting(
		final String time,
		final Label mesg
	) {
		return run_waiting(Misc.text2tick(time), mesg);
	}
	protected void reset_waiting() {
		tick = -1L;
	}
	//--------------------------------------------//
	
	private static String default_stick_text = "";
	
	public static String LAST_STICKER = "";
	
	public static class Sticker extends Stepper {
		private final Label msg = new Label();
		
		public Sticker setValues(String txt) {
			msg.setText(txt);
			return this;
		}		
		@Override
		public Node getContent(){
			set(()->{
				String txt = msg.getText();
				if(txt.length()==0){
					txt = "STICKER";
				}
				Misc.logv(">> %s <<", txt);
				LAST_STICKER = txt;
				next_step();
			});
			msg.getStyleClass().add("font-size3");
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
				init_text = default_stick_text;
			}
			TextInputDialog dia = new TextInputDialog(init_text);
			//dia.setTitle("Text Input Dialog");
			//dia.setHeaderText("Look, a Text Input Dialog");
			dia.setContentText("內容:");
			Optional<String> res = dia.showAndWait();
			if (res.isPresent()){
				default_stick_text = res.get();
				msg.setText(default_stick_text);			   
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
