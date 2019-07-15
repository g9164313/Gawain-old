package narl.itrc.vision;


import java.util.concurrent.Exchanger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class DevCamera extends ImgView {
	
	private Capture cap;
	
	public DevCamera(final Capture dev) {
		cap = dev;
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
	
	private final Exchanger<ImgFilm> sp = new Exchanger<ImgFilm>();
	
	private void loop_inner(final int count) {
		
		ImgFilm ff = cap.getFilm();
		
		ff.setSnap(count);

		do {
			if(Gawain.isExit()==true) {
				return;
			}
			
			cap.fetch(ff);
			
			ff = syncProcess(ff);

			refresh(ff);

		}while(looper.isCancelled()==false);
	}
	
	private Task<?> stubber;
	
	/**
	 * Every task 'must' call syncProcess().<p> 
	 * @param task
	 */
	public void startProcess(final Runnable task) {
		if(stubber!=null) {
			if(stubber.isRunning()==true) {
				return;
			}
		}		
		stubber = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {
				task.run();
				core[0].interrupt();
				return 0;
			}
		};
		//stubber.setOnCancelled(e->reset_stubber());
		//stubber.setOnSucceeded(e->reset_stubber());
		core[1] = new Thread(stubber,"CamStubber");
		core[1].start();
	}
	
	public void stopProcess() {
		if(stubber==null) {
			return;
		}
		if(stubber.isDone()==true) {
			return;
		}
		stubber.cancel();
	}
	
	@SuppressWarnings("unused")
	private boolean isProcessDone() {
		//this function is called by native code!!!
		return stubber.isDone() || Gawain.isExit(); 
	}
	
	private ImgFilm syncProcess(ImgFilm ff) {		
		if(stubber==null) {
			return ff;
		}
		if(stubber.isDone()==true) {
			return ff;
		}
		try {
			ff = sp.exchange(ff);
		} catch (InterruptedException e) {
		}
		return ff;
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
