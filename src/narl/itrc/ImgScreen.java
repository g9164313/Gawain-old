package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class ImgScreen extends ImageView implements 
	EventHandler<MouseEvent>
{
	public ImgScreen(){
		this(640,480);
	}
	
	public ImgScreen(ImgControl control){		
		this(640,480);
		ctrl = control;
	}
	
	public ImgScreen(int width,int height){
		setFitWidth(width);
		setFitHeight(height);
		//setPreserveRatio(true);//width is same as height
		setOnMouseEntered(this);
		setOnMouseExited(this);
		setOnMouseMoved(this);
		setOnDragDetected(this);
		setOnMouseDragged(this);
		setOnMouseReleased(this);
	}

	private ImgControl ctrl = null;
	
	public int camIdx = 0;
	public String camConf = null;

	private CamBundle renderPlug;
	private Task<Integer> renderTask;
	
	private void swtEnable(boolean flag){
		if(ctrl!=null){
			return;
		}
		Application.invokeAndWait(new Runnable(){
			@Override
			public void run() {
				ctrl.swtEnable.selectedProperty().set(flag);
			}		
		});
	}
	
	public void bindControl(ImgControl control){
		if(ctrl!=null){
			return;
		}
		ctrl = control;
		ctrl.bindScreen(this);
	}
	
	public void bindCamera(CamBundle cam){
		if(renderTask!=null){
			if(renderTask.isRunning()==true){
				return;
			}
		}
		renderTask = new Task<Integer>(){			
			@Override
			protected Integer call() throws Exception {
				if(renderPlug==null){
					swtEnable(false);
					return -1;
				}
				
				//stage.1 - try to open camera~~~
				renderPlug.setup(camIdx, camConf);				

				//stage.2 - continue to grab image from camera			
				while(isCancelled()==false){
					if(renderPlug.optEnbl.get()==false){
						//this option must check as soon as possible~~~
						//Application may shutdown or other things close device...
						return -2;
					}
					if(ctrl!=null){
						if(ctrl.swtPlayer.get()==false){
							Thread.sleep(50);
							continue;
						}
					}
					renderPlug.fetch();
					renderPlug.markData();
					//TODO: hook something~~~~
					Image img = renderPlug.getImage(1);//show overlay~~
					if(img==null){
						continue;
					}
					setImage(img);
				}
				return 0;
			}
		};
		renderPlug = cam;
		renderTask.setOnCancelled(event);
		new Thread(renderTask,"imgScreen-render").start();
	}
	
	private EventHandler<WorkerStateEvent> event = 
		new EventHandler<WorkerStateEvent>()
	{
		@Override
		public void handle(WorkerStateEvent event) {
			//When we cancel thread, it will drop from the execution pool.
			//stage.3 - we finish the job~~~
			renderPlug.close();
		}
	};	
		
	public void unbind(){
		if(renderTask==null){
			return;
		}
		while(renderTask.isRunning()==true){
			renderTask.cancel();
		}
	}

	@Override
	public void handle(MouseEvent e) {
		if(renderTask==null){
			return;
		}		
		if(renderTask.isRunning()==false){
			return;
		}
		//Do we need focus this control then got mouse-event??		
		EventType<?> typ = e.getEventType();
		
		//Misc.logv("event="+typ.getName());
		
		if(typ==MouseEvent.MOUSE_ENTERED){
			
			getScene().setCursor(Cursor.CROSSHAIR);
		
		}else if(typ==MouseEvent.DRAG_DETECTED){

			renderPlug.setTick0();

		}else if(typ==MouseEvent.MOUSE_MOVED){
			
			renderPlug.setCursor(e.getX(),e.getY());
			
		}else if(typ==MouseEvent.MOUSE_DRAGGED){
			
			renderPlug.setCursor(e.getX(),e.getY());
			
		}else if(typ==MouseEvent.MOUSE_RELEASED){
			
			renderPlug.setTick1(0,CamBundle.ROI_TYPE_RECT);
			
		}else if(typ==MouseEvent.MOUSE_EXITED){
			
			getScene().setCursor(Cursor.DEFAULT);
		}
	}
}

