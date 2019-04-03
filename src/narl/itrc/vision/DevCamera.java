package narl.itrc.vision;

import javafx.scene.control.MenuItem;
import narl.itrc.DevBase;

public class DevCamera extends DevBase {

	public DevCamera() {
		super("DevCamera");
	}
	
	@Override
	protected boolean eventLink() {
		return true;
	}
	@Override
	protected boolean afterLink() {
		return cap.setup();
	}
	@Override
	protected void beforeUnlink() {
		cap.done();
	}
	@Override
	protected void eventUnlink() {
	}
	
	public interface Capture {
		public boolean setup();
		public void fetch(DevCamera cam);
		public void done();
	};

	private byte[] cvBuffer = new byte[3*1024*1024];	
	private int cvWidth = 0;
	private int cvHeight= 0;
	@SuppressWarnings("unused")
	private int cvType = 0;
	
	private DevBase.Work fetch = new DevBase.Work(0, true){
		@Override
		public int looper(Work obj, int pass) {
			if(cap!=null){
				cap.fetch(DevCamera.this);
			}
			if(face!=null){
				//TODO:convert color again for RGB buffer
				face.refresh(cvBuffer, cvWidth, cvHeight);
			}
			return 0;
		}
		@Override
		public int event(Work obj, int pass) {
			return 0;
		}
	};
	
	public void play(){
		offer(0,true,fetch);
	}
	
	public void stop(){
		remove(fetch);
	}
	
	private Capture cap = null;
	
	public void setCapture(Capture obj){
		if(cap!=null){
			unlink();
		}
		cap = obj;
	}
	public Capture getCapture(Capture obj){
		return cap;
	}
	
	private ImgView face = null;
	
	public void setFace(ImgView obj){
		
		face = obj;
		
		final MenuItem itm1 = new MenuItem("play");
		itm1.setOnAction(e->play());
		
		final MenuItem itm2 = new MenuItem("stop");
		itm2.setOnAction(e->stop());
		
		face.getContextMenu()
			.getItems()
			.addAll(itm1, itm2);
	}
	public ImgView getFace(ImgView obj){
		return face;
	}
}
