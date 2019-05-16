package narl.itrc.vision;

public abstract class Capture {
	
	protected long context;
	
	private ImgFlim film = new ImgFlim();
	
	public ImgFlim getFilm(){
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
		film.setSnap(count);		
		fetch(film);
		view.getMarkByArray(film.mark);
		if(hook!=null){
			hook.run();
		}
		view.refresh(film);
		return this;
	}
	
	abstract boolean setup();
	
	protected void afterSetup(){		
	}
	
	abstract void fetch(ImgFlim data);
	
	abstract void done();
}
