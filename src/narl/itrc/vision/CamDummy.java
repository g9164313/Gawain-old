package narl.itrc.vision;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import narl.itrc.Misc;

/**
 * Create a dummy camera, just for simulation.<p>
 * The image source is file.<p>
 * @author qq
 *
 */
public class CamDummy extends CamBundle {

	public CamDummy(){		
	}

	public CamDummy(String name){
		super(name);//configuration can be file or directory
	}
	
	public final static int IMREAD_GRAYSCALE = 0;
	public final static int IMREAD_COLOR = 1;
	
	/**
	 * This value is refereed to 'imgcodecs.hpp'
	 * IMREAD_UNCHANGED = -1 
	 * IMREAD_GRAYSCALE = 0
	 * IMREAD_COLOR     = 1
	 * IMREAD_ANYDEPTH  = 2
	 * IMREAD_ANYCOLOR  = 4
	 * IMREAD_LOAD_GDAL = 8
	 */
	private AtomicInteger frameFlag = new AtomicInteger(IMREAD_COLOR);
	
	private AtomicInteger frameIndx = new AtomicInteger(0);
	
	private AtomicInteger frameStep = new AtomicInteger(1);
	
	private AtomicBoolean toggle = new AtomicBoolean(false);
	
	private ArrayList<String> frameList = new ArrayList<String>();
	
	private double frameRate = 10.; 
	
	//TODO:we can't save image, why ???
	@Override
	public void setup() {
		//it always should be success :-)
		//prepare image list for showing~~~		
		if(txtConfig.length()==0){
			return;
		}
		frameIndx.set(0);//reset this~~		
		File fs = new File(txtConfig);
		if(fs.isFile()==true){
			frameStep.set(0);//reset this~~
			frameList.add(txtConfig);			
			toggle.set(true);
		}else if(fs.isDirectory()==true){
			txtConfig = Misc.checkSeparator(txtConfig);
			frameStep.set(1);//reset this~~
			String[] lst = fs.list();
			Arrays.sort(lst);
			for(String name:lst){
				frameList.add(txtConfig+name);
			}
			//frameList.addAll(Arrays.asList(lst));
		}else{
			frameStep.set(0);//reset this~~
			//support regular-expression?			
		}
		setContext(-1);
	}

	@Override
	public void fetch() {
		if(frameList.isEmpty()==true){
			return;
		}
		int stp = frameStep.get();
		int idx = frameIndx.get();
		if(stp==0){
			if(toggle.get()==true){
				loadImage(
					frameList.get(idx),
					frameFlag.get()
				);
				toggle.set(false);
			}
		}else{
			idx += stp;
			int cnt = frameList.size();
			if(idx<0){
				idx = cnt - 1;
			}else if(idx>=cnt){
				idx = 0;
			}
			loadImage(
				frameList.get(idx),
				frameFlag.get()
			);
			frameIndx.set(idx);
		}
		check_slide();
		delay_gap();
	}

	@Override
	public long bulk(long addr) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() {
		setContext(0);//it always should be success :-)		
	}
	
	private PanDummy pan = new PanDummy(this);//setting panel
	@Override
	public void showSetting(ImgPreview1 prv) {
		pan.appear();
	}
	
	private void delay_gap(){
		Misc.delay((int)(1000./frameRate));
	}
	
	private void check_slide(){
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				int idx = frameIndx.get() + 1;
				pan.sldFrame.setValue(idx);
			}
		};
		Application.invokeAndWait(event);
	}
	
	private void check_pause(){
		if(frameStep.get()==0){
			return;
		}
		frameStep.set(0);//waiting~~~~		
		delay_gap();
	}
	
	public void prevFrame(){
		check_pause();		
		int idx = frameIndx.decrementAndGet();
		if(idx<0){
			idx = 0;
			frameIndx.set(idx);
		}
		toggle.set(true);//next turn, image will be updated.
	}
	
	public void playFrame(int flag){
		frameStep.set(flag);//waiting~~~~
	}
	
	public void nextFrame(){
		check_pause();	
		int idx = frameIndx.incrementAndGet();
		int cnt = frameList.size();
		if(idx>=cnt){
			idx = cnt - 1;
			frameIndx.set(idx);
		}
		toggle.set(true);//next turn, image will be updated.
	}
	
	public void setFmtGray(){
		frameFlag.set(IMREAD_GRAYSCALE);
	}
	
	public void setFmtColor(){
		frameFlag.set(IMREAD_COLOR);
	}
	
	public int countFrame(){
		return frameList.size();
	}
	
	public boolean isPlaying(){
		if(frameStep.get()==0){
			return false;
		}
		return true;
	}
	
	public boolean isGray(){
		if(frameFlag.get()==IMREAD_GRAYSCALE){
			return true;
		}
		return false;
	}
}
