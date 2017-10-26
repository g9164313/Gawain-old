package narl.itrc.vision;

import narl.itrc.DevBase;

public class DevRender extends DevBase {

	private CamBundle bundle;
	
	public DevRender(CamBundle bnd){
		bundle = bnd;
		addUsual(event->{
			if(bundle.ctrlPlay==true){
				bundle.fetch();
			}
		});
	}
	
	@Override
	protected boolean taskInit(){
		bundle.setup();
		return false;
	}
	
	@Override
	protected void taskFinal(){
		bundle.close();
	}
		
	@Override
	public void eventShutdown() {		
	}
}
