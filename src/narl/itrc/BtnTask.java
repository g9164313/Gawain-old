package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import com.jfoenix.controls.JFXButton;

public abstract class BtnTask extends JFXButton implements EventHandler<ActionEvent> {
	
	private String txtReady="",txtCancel="";
	private boolean flagOneshot = false;
	
	public BtnTask(String txt){
		this(txt,txt,false);
	}
	
	public BtnTask(String txt,boolean oneShoot){
		this(txt,txt,oneShoot);
	}
	
	public BtnTask(
		String txt0,
		String txt1
	){
		this(txt0,txt1,false);
	}
	
	public BtnTask(
		String txt0,
		String txt1,
		boolean oneShoot
	){
		super(txt0,Misc.getIcon("run.png"));
		txtReady = txt0;
		txtCancel= txt1;
		flagOneshot = oneShoot;
		setOnAction(this);
		getStyleClass().add("btn-raised");
	}
	
	public abstract boolean execute(DlgTask dlg);
	public abstract boolean isReady(DlgTask dlg);
	public abstract boolean prepare(DlgTask dlg);	
	public abstract boolean isEnded(DlgTask dlg);
	
	private DlgTask core;
	
	@Override
	public void handle(ActionEvent event) {
		core = new DlgTask(){
			@Override
			protected boolean isReady(DlgTask dlg) {			
				return BtnTask.this.isReady(core);//GUI thread
			}
			@Override
			protected boolean prepare(DlgTask dlg) {			
				return BtnTask.this.prepare(core);//Working thread
			}
			@Override
			public boolean execute(DlgTask dlg) {			
				return BtnTask.this.execute(core);//Working thread
			}
			@Override
			protected boolean isEnded(DlgTask dlg) {
				setText(txtReady);
				return BtnTask.this.isEnded(core);//GUI thread
			}
		};//how to improve this???
		core.propOneShoot = flagOneshot;
		if(core.isRunning()==true){
			core.cancel();
			setText(txtReady);
		}else{
			setText(txtCancel);
			core.show();
		}
	}
}
