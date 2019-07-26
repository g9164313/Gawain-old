package narl.itrc.vision;

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
		view.refresh(film);
		return this;
	}
	
	abstract boolean setup();
	
	protected void afterSetup(){		
	}
	
	abstract void fetch(ImgFilm data);
	
	abstract void done();
}
