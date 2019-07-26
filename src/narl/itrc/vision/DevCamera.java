package narl.itrc.vision;

import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.control.MenuItem;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class DevCamera extends ImgPane {
	
	private Capture cap;
	
	public DevCamera(final Capture dev) {
		cap = dev;
		init_menu();
	}
	
	private void init_menu() {
		final MenuItem itm_clear = new MenuItem("清除");
		itm_clear.setOnAction(e->cap.getFilm().clearOverlay());
		menu.getItems().add(itm_clear);
	}
	
	public ImgFilm getFilm() {
		return cap.getFilm();
	}
	
	private Thread[] core = {null, null};
	
	private Task<?> looper;

	public void play(final int count) {
		if(looper!=null) {
			Misc.logw("camera is busy!!");
			return;
		}
		looper = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {				
				if(cap.setup()==false) {
					Misc.logw("fail to steup capture!!");
					return -1;
				}
				Application.invokeAndWait(()->cap.afterSetup());
				loop_inner(count);
				cap.done();
				return 0;
			}
		};
		core[0] = new Thread(looper,"DevCamera");
		core[0].start();
	}
		
	public void pause() {
		if(looper==null) {
			return;
		}
		if(looper.isDone()==true) {
			return;
		}
		looper.cancel();
	}

	private void loop_inner(final int count) {
		ImgFilm ff = cap.getFilm();
		cap.getFilm().setSnapCount(count);
		do {
			if(Gawain.isExit()==true) {
				return;
			}
			ff.refMask = maskBrush;
			ff.setMark(this);
			cap.fetch(ff);		
			ff.mirrorPool();
			syncPoint();
			refresh(ff);
		}while(looper.isCancelled()==false);
	}
	
	private Task<Integer> procTask;
	
	/**
	 * Every task 'must' call syncProcess().<p> 
	 * @param task
	 */
	public void startProcess(
		final Runnable task,
		final Runnable eventBegin,
		final Runnable eventDone
	) {
		if(procTask!=null) {
			if(procTask.isRunning()==true) {
				return;
			}
		}
		procTask = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {
				doSync.set(true);
				task.run();
				core[0].interrupt();
				doSync.set(false);
				return 0;
			}
		};
		if(eventBegin!=null) {
			procTask.setOnScheduled(e->eventBegin.run());
		}
		if(eventDone!=null) {
			procTask.setOnCancelled(e->eventDone.run());
			procTask.setOnSucceeded(e->eventDone.run());
		}
		core[1] = new Thread(procTask,"CamProcess");
		core[1].start();
	}
	
	public void startProcess(final Runnable task){
		startProcess(task,null,null);
	} 
	
	public void stopProcess() {
		if(doSync.get()==false) {
			return;
		}
		if(procTask.isDone()==true) {
			return;
		}
		procTask.cancel();
		core[1].interrupt();
	}
	
	@SuppressWarnings("unused")
	private boolean isProcessDone() {
		//this function is called by native code!!!
		return procTask.isDone() || Gawain.isExit(); 
	}
	
	private final Exchanger<Void> sp = new Exchanger<Void>();
	private AtomicBoolean doSync = new AtomicBoolean(false);
	private void syncPoint() {
		//this function is also invoked by native code!!!
		if(doSync.get()==false) {
			return;
		}
		try {
			if(procTask.isDone()==true) {
				return;
			}
			sp.exchange(null);			
		} catch (InterruptedException e) {
		}
		return ;
	}
	//-------------------------------------//
	
	/*
	private static final ImageView icon_film_on = Misc.getIconView("filmstrip-on.png");
	private static final ImageView icon_film_off= Misc.getIconView("filmstrip-off.png");
	public Button bindMonitor(final Button btn){
		btn.setGraphic(icon_film_off);
		btn.setOnAction(e->{
			if(btn.getGraphic()==icon_film_off){
				monitor(1);//start to monitor
				btn.setGraphic(icon_film_on);
			}else{
				monitor(-1);//stop action, and live again
				btn.setGraphic(icon_film_off);
			}
		});
		return btn;
	}
	
	private static final ImageView icon_eye_on = Misc.getIconView("eye-on.png");
	private static final ImageView icon_eye_off= Misc.getIconView("eye-off.png");
	public Button bindPipe(final Button btn){
		btn.setGraphic(icon_eye_off);
		btn.setOnAction(e->{
			if(btn.getGraphic()==icon_eye_on){
				Misc.logv("camera pip is busy!!");
				return;
			}
			//start to process data
			btn.setGraphic(icon_eye_on);
			pipeImage(1,()->{
				//Processing is done
				btn.setGraphic(icon_eye_off);
			});
		});
		return btn;
	}*/
}
