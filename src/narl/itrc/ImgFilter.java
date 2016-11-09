package narl.itrc;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public abstract class ImgFilter implements 
	EventHandler<ActionEvent>
{
	/**
	 * This function is same as TskAction.isTrigger.<p>
	 */
	public boolean isTrigger = false;

	/**
	 * This variable will be provided by other thread or task.<p>
	 * When this object is builded, render will only show image, but not cook data.<p>
	 */
	public AtomicBoolean asyncDone = null;
	
	public static final int STA_IDLE = 0;
	public static final int STA_REDY = 1;
	public static final int STA_COOK = 2;
	
	public AtomicInteger state = new AtomicInteger(STA_IDLE);
	
	protected void refreshData(final ArrayList<ImgPreview> list){
		
		if(Application.isEventThread()==true){			
			return;
		}
		
		final Runnable eventUpdate = new Runnable(){
			@Override
			public void run() {
				for(ImgPreview prv:list){
					prv.rendering();
				}
			}
		};
		Application.invokeAndWait(eventUpdate);
		
		//get the next image~~~
		for(ImgPreview prv:list){
			prv.fetchBuff();
			prv.fetchInfo();
		}				
	}
		
	/**
	 * The meaning of this variable is dependent on filter.
	 * But this number presents the index of 'ImgPreview' list.
	 */
	public int prvIdx = -1;
	
	/**
	 * this invoked by working-thread.<p>
	 * user can process or cook data here.<p>
	 * @param list - the list of preview
	 * @return true - we done, take off.<p> false- keep this in queue.<p>
	 */
	public abstract void cookData(ArrayList<ImgPreview> list);//this invoked by render-thread
	
	/**
	 * this invoked by GUI-thread.<p>
	 * user can show charts or change the state of widget here.<p>
	 * So, clearing region is the user responsibility.<p> 
	 * @param list - the list of preview
	 * @return true - we done, take off.<p> false- keep this in queue.<p>
	 */
	public abstract boolean showData(ArrayList<ImgPreview> list);//this invoked by GUI thread

	@Override
	public void handle(ActionEvent event) {
		//user can override this function
	}	
};

