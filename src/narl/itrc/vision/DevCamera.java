package narl.itrc.vision;

import java.util.function.Predicate;

import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import narl.itrc.DevBase;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class DevCamera extends DevBase {

	public DevCamera() {
		super("DevCamera");
	}	
	public DevCamera(Capture... lst) {
		super("DevCamera");
		binding(lst);
	}
	
	private Capture[] lstCapt;
	private ImgView[] lstView;
	
	public void binding(Capture... lst){
		int cnt = lst.length;
		lstCapt = lst;
		lstView = new ImgView[cnt];
		for(int i=0; i<cnt; i++){
			lstView[i] = new ImgView();
			append_menu(lstView[i]);
		}
	}
	
	public ImgView getView(final int id){
		if(lstView==null){
			return null;
		}
		if(id>=lstView.length){
			return null;
		}
		return lstView[id];
	}
	
	@Override
	protected boolean eventLink() {
		return true;
	}
	@Override
	protected boolean afterLink() {
		for(Capture cap:lstCapt){
			cap.setup();
			cap.afterSetup();
		}
		return true;
	}
	@Override
	protected void beforeUnlink() {
		for(Capture cap:lstCapt){
			cap.done();
		}
	}
	@Override
	protected void eventUnlink() {
	}
	//-------------------------------------//
	
	/**
	 * This variable was updated by native code.<p>
	 */
	private byte[] p_key = null;
	private static final String nodeName = Gawain.getConfigName();
	private native void pipeFetch(byte[] key, ImgFlim data, boolean sync);
	private native void pipeClose(byte[] key);
	
	private int fcount = 1;
	private DevBase.Work fetch = new DevBase.Work(0, true){
		@Override
		public int looper(Work obj, int pass) {
			for(int i=0; i<lstCapt.length; i++){
				final Capture cap = lstCapt[i];
				final ImgView vew = lstView[i];
				cap.refresh(vew, ()->{
					if(p_key==null){
						return;
					}
					pipeFetch(p_key,cap.getFilm(),false);
				});
			}
			return 0;
		}
		@Override
		public int event(Work obj, int pass) {
			return 0;
		}
	};
	public void livePlay(final int snap){
		if(snap>0){
			remove(fetch);
			fcount = snap;
			offer(0,true,fetch);
		}else{
			remove(fetch);
		}
	}
	public void monitor(final int snap){
		if(snap>0){
			remove(fetch);
			fcount = snap;
			p_key = new byte[24];
			offer(0,true,fetch);
		}else{
			remove(fetch);
			pipeClose(p_key);
			p_key = null;
			remove(fetch);
		}		
	}
	
	public void pipeImage(
		final int id,
		final int count,
		final Runnable eventHook
	){
		if(id>=lstCapt.length){
			return;
		}
		if(p_key==null){
			p_key = new byte[24];
		}
		offer(new DevBase.Work(0, false){
			@Override
			public int looper(Work obj, int pass) {
				final Capture cap = lstCapt[id];
				final ImgView vew = lstView[id];
				cap.refresh(vew, ()->{
					pipeFetch(p_key,cap.getFilm(),true);
				});
				if(eventHook!=null){
					return 1;
				}
				return 0;
			}
			@Override
			public int event(Work obj, int pass) {
				eventHook.run();
				return 0;
			}
		});
	}
	//-------------------------------------//
	
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
			pipeImage(0,1,()->{
				//Processing is done
				btn.setGraphic(icon_eye_off);
			});
		});
		return btn;
	}
	//-------------------------------------//
	
	private final String MENU_ID = "Menu-Control";
	
	private void append_menu(final Control obj){
		
		final MenuItem itm1 = new MenuItem("play");
		itm1.setOnAction(e->livePlay(1));
		
		final MenuItem itm2 = new MenuItem("stop");
		itm2.setOnAction(e->livePlay(0));
		
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
