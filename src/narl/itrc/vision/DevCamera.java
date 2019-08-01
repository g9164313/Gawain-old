package narl.itrc.vision;

import java.util.concurrent.Phaser;

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
	
	private Task<?> tskLoop;
	private Thread looper;
	public void play(final int count) {
		if(tskLoop!=null) {
			Misc.logw("camera is busy!!");
			return;
		}
		tskLoop = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {				
				if(cap.setupAll()==false) {
					Misc.logw("fail to steup capture!!");
					return -1;
				}
				loop_inner(count);
				cap.done();
				return 0;
			}
		};		
		looper = new Thread(tskLoop,"DevCamera");
		looper.start();
	}
		
	public void pause() {
		if(tskLoop==null) {
			return;
		}
		if(tskLoop.isDone()==true) {
			return;
		}
		tskLoop.cancel();
	}

	private void loop_inner(final int count) {
		ImgFilm film = cap.getFilm();
		film.setSnapCount(count);
		do {
			if(Gawain.isExit()==true) {
				return;
			}				
			film.refMask = maskBrush;
			film.setMark(this);
			cap.fetch(film);
			if(syncPoint.getArrivedParties()>0) {
				syncPoint.arrive();
				syncPoint.arriveAndAwaitAdvance();
			}
			film.mirrorPool();
			updateView(film);
		}while(tskLoop.isCancelled()==false);
	}	
	private Phaser syncPoint = new Phaser(2);
	
	@SuppressWarnings("unused")
	private void doSync(final boolean lock) {
		if(lock==true) {
			syncPoint.arriveAndAwaitAdvance();
		}else {
			syncPoint.arrive();
		}
	}
	
	private Task<Integer> tskProc;
	/**
	 * Every task 'must' call syncProcess().<p> 
	 * @param task
	 */
	public void startProcess(
		final Runnable task,
		final Runnable eventBegin,
		final Runnable eventDone
	) {
		if(tskProc!=null) {
			if(tskProc.isRunning()==true) {
				return;
			}
		}
		tskProc = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {
				task.run();
				looper.interrupt();
				return 0;
			}
		};
		if(eventBegin!=null) {
			tskProc.setOnScheduled(e->eventBegin.run());
		}
		if(eventDone!=null) {
			tskProc.setOnCancelled(e->eventDone.run());
			tskProc.setOnSucceeded(e->eventDone.run());
		}
		new Thread(tskProc,"Process-Image").start();
	}
	
	public void startProcess(final Runnable task){
		startProcess(task,null,null);
	} 
	
	public void stopProcess() {
		if(tskProc.isDone()==true) {
			return;
		}
		tskProc.cancel();
	}
	
	@SuppressWarnings("unused")
	private boolean isTaskDone() {
		//this function is called by native code!!!
		return tskProc.isDone() || tskLoop.isDone() || Gawain.isExit(); 
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
