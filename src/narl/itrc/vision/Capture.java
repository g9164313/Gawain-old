package narl.itrc.vision;

import com.sun.glass.ui.Application;

public abstract class Capture {
	
	protected long context;
	
	private ImgFilm film = new ImgFilm();
	
	public ImgFilm getFilm(){
		return film;
	}
	
	public Capture refresh(
		final ImgView view
	){
		return refresh(1,view,null);
	}
	public Capture refresh(
		final ImgView view,
		final Runnable hook
	){
		return refresh(1,view,hook);
	}
	public Capture refresh(
		final int count,
		final ImgView view,
		final Runnable hook
	){
		film.setSnapCount(count);		
		fetch(film);
		if(hook!=null){ hook.run();	}
		view.updateView(film);
		return this;
	}
	
	private Runnable setup_afer = null;
	
	public Capture setupAfter(final Runnable run) {
		setup_afer = run;
		return this;
	}
	
	public boolean setupAll() {
		if(setup()==false) {
			return false;
		}
		if(setup_afer!=null) {
			if(Application.isEventThread()==true) {
				setup_afer.run();
			}else {
				Application.invokeAndWait(setup_afer);
			}
		}
		return true;
		
	}
	
	abstract boolean setup();
		
	abstract void fetch(ImgFilm data);
	
	abstract void done();
}
