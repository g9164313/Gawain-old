package narl.itrc.vision;

public abstract class Capture {
	
	protected long context;
	
	abstract boolean setup();
	
	protected void afterSetup(){		
	}
	
	abstract void fetch(ImgData data);
	
	abstract void done();
}
