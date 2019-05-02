package narl.itrc.vision;

import java.util.function.Predicate;

import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import narl.itrc.DevBase;
import narl.itrc.Gawain;

public class DevCamera extends DevBase {

	public DevCamera() {
		super("DevCamera");
	}
	
	private class Watch {
		Capture cap;
		ImgView img;
		ImgData dat = new ImgData();
		Watch(Capture c,ImgView v){
			cap = c;
			img = v;
		}
		void setup(){
			if(cap.setup()==true){
				cap.afterSetup();
			}
		}
		void done(){
			cap.done();
		}
		void clear_data(int snap){
			dat.clear(snap);
		}
		void fetch_data(byte[] pkey){
			cap.fetch(dat);
			if(dat.isValid()==true){
				if(pkey!=null){
					pipeFetch(pkey,dat);
				}
				img.refresh(dat);
			}			
		}
	};
	
	private Watch[] lstWatch;

	public void link(
		final Capture capture,
		final ImgView imgview
	){
		lstWatch = new Watch[1];
		lstWatch[0] = new Watch(capture,imgview);
		link();
	}
	
	public void link(
		final Capture[] lst_c,
		final ImgView[] lst_v
	){
		int cnt = 
			(lst_c.length>lst_v.length)?
			(lst_c.length):
			(lst_v.length);
		lstWatch = new Watch[cnt];
		for(int i=0; i<cnt; i++){
			if(i<lst_c.length){
				lstWatch[i].cap = lst_c[i];
			}else{
				lstWatch[i].cap = null;
			}
			if(i<lst_v.length){
				lstWatch[i].img = lst_v[i];
			}else{
				lstWatch[i].img = null;
			}
		}
		link();
	}
	
	@Override
	protected boolean eventLink() {
		for(Watch ww:lstWatch){
			append_menu(ww.img);
		}
		return true;
	}
	@Override
	protected boolean afterLink() {
		for(Watch ww:lstWatch){
			ww.setup();
		}
		return true;
	}
	@Override
	protected void beforeUnlink() {
		for(Watch ww:lstWatch){
			ww.done();
		}
	}
	@Override
	protected void eventUnlink() {
		for(Watch ww:lstWatch){
			remove_menu(ww.img);
		}
	}
	
	public Capture getCapture(int i){
		if(i<0 || lstWatch.length<=i){
			return null;
		}
		return lstWatch[i].cap;
	}	
	//-------------------------------------//
	
	private DevBase.Work fetchLive = new DevBase.Work(0, true){
		@Override
		public int looper(Work obj, int pass) {
			for(Watch ww:lstWatch){
				ww.fetch_data(p_key);
			}
			return 0;
		}
		@Override
		public int event(Work obj, int pass) {
			return 0;
		}
	};
	public void play(){
		waitForEmpty();
		p_key = null;
		for(Watch ww:lstWatch){
			ww.clear_data(1);
		}		
		offer(0,true,fetchLive);
	}
	public void pause(){
		remove(fetchLive);
	}
	public void monitor(int snap){
		if(snap>0){
			waitForEmpty();
			p_key = new byte[24];
			for(Watch ww:lstWatch){
				ww.clear_data(snap);
			}
			offer(0,true,fetchLive);
		}else{
			remove(fetchLive);
			pipeClose(p_key);
			p_key = null;
			play();
		}		
	}

	/**
	 * This variable was updated by native code.<p>
	 */
	private byte[] p_key = null;
	private static final String nodeName = Gawain.getConfigName();
	private native void pipeFetch(byte[] key, ImgData data);
	private native void pipeClose(byte[] key);
	
	//-------------------------------------//
	private final String MENU_ID = "Menu-Control";
	
	private void append_menu(final Control obj){
		
		final MenuItem itm1 = new MenuItem("play");
		itm1.setOnAction(e->play());
		
		final MenuItem itm2 = new MenuItem("stop");
		itm2.setOnAction(e->pause());
		
		final Menu menu = new Menu("播放");
		menu.getItems().addAll(itm1, itm2);
		menu.setId(MENU_ID);
		
		obj.getContextMenu()
			.getItems()
			.addAll(menu);
	}
	
	private void remove_menu(final Control obj){
		final Predicate<MenuItem> event = new Predicate<MenuItem>(){
			@Override
			public boolean test(MenuItem itm) {
				String id = itm.getId();
				if(id==null){
					return false;
				}
				return id.equalsIgnoreCase(MENU_ID);
			}
		};
		obj.getContextMenu().getItems().removeIf(event);
	}
}
