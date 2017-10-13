package narl.itrc.vision;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * 'Blk' is the abbreviation of "Bulk".<p>
 * This object collects many frames and bundles it into a huge file.<p>
 * This object should also support to 'view' huge image.<p>
 * Attention!! 
 * P.S: Which method should we use? memory-mapping IO or RAM-disk file.<p>  
 * @author qq
 *
 */
public class BlkRender extends BorderPane implements Gawain.EventHook {

	public CamBundle bundle;
	
	public BlkRender(CamBundle bnd){
		Gawain.hook(this);
		bundle = bnd;
		init_layout();
	}

	@Override
	public void kickoff() {
	}
	@Override
	public void shutdown() {
		if(looper!=null){
			if(looper.isDone()==false){
				looper.cancel();
			}
		}
		bundle.close();
		blkFree();
	}
	//--------------------------------------------//
	
	private final String TXT_START = "開始(_s)";
	private final String TXT_CANCEL= "取消(_c)";
	
	private Button actLaunch,actSetting;
	
	private ProgressBar actProgress;

	private Label actMessage;
	
	private void init_layout(){
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-medium");
		lay1.setAlignment(Pos.CENTER_LEFT);
		lay1.setPrefWidth(300);
		
		actLaunch = PanBase.genButton1(TXT_START,null);		
		actLaunch.setPrefWidth(60);
		actLaunch.setOnAction(event->doAction());
		
		VBox lay2 = new VBox();
		actProgress = new ProgressBar();
		actProgress.setProgress(0.);
		actProgress.prefWidthProperty().bind(lay2.widthProperty());
		actMessage = new Label();
		actMessage.setText("待命中...");
		actMessage.prefWidthProperty().bind(lay2.widthProperty());
		lay2.getChildren().addAll(actProgress,actMessage);
		
		actSetting = PanBase.genButtonFlat("","settings.png");
		actSetting.setOnAction(event->{
			System.out.println("ggyy");
		});
		
		HBox.setHgrow(lay2, Priority.ALWAYS);
		lay1.getChildren().addAll(actLaunch,lay2,actSetting);
		setTop(lay1);
	}
	
	private void doStart(){
		actSetting.setDisable(false);
		actLaunch.setText(TXT_START);
	}
	
	private void doCancel(){
		actSetting.setDisable(true);
		actLaunch.setText(TXT_CANCEL);
	}
	//--------------------------------------------//
	
	private final int HEAD_SIZE = 128;//the structure must refer to native code~~~ 
		
	private final int MODE_MEM_DISK= 0x001;
	private final int MODE_MEM_ZIP = 0x002;
	private final int MODE_MEM_PNG = 0x010;
	private final int MODE_MEM_TIF = 0x020;
	private final int MODE_MEM_JPG = 0x030;
	private final int MODE_MAPPING = 0x100;//??? it is not special efficient ???
	
	private int blkMode = MODE_MEM_TIF;
	
	/**
	 * This value presents how many blocks will be transfer.<p>
	 * This is a user defined value.<p>
	 * In native code, the type is 'unit32_t'
	 */
	private long blkCounter = 5500;

	/**
	 * This variable keep the file descriptor.<p>
	 * It is only used by 'MODE_MAPPING'.<p>
	 */
	private int blkFileDesc = 0;
	
	/**
	 * 
	 */
	private long blkAddress = 0L;//memory address
	
	/**
	 * The transfer size is 'header + blkCounter × width × height'.<p>
	 * Header size is 128 byte.<p>
	 * This value is calculated by native code.<p>
	 */
	private long blkAllSize = 0L;
	
	private String txtAllSize = "";
	
	private String blkFileName = "stream.raw";
	
	private Task<Integer> looper = null;

	/**
	 * this variable keep the time tick for looper
	 */
	private long tick0,tick1,tick2;
	
	/**
	 * This implementation is put in 'CamBundle.cpp'.<p>
	 * @param name - file name
	 * @param bnd - it will provide camera information 
	 * @return the current memory address (pointer)
	 */
	private native long blkAllocate(String name,CamBundle bnd);
	
	/**
	 * Flush data in memory to hard-disk.<p>
	 * @param name - the file name of data stream
	 */
	private native void blkFlush(String name);
	
	/**
	 * Free memory resource for bulk transfer.<p>
	 */
	private native void blkFree();
		
	private class TskBulk extends Task<Integer>{

		private long blkIndex = 0L;
		private long blkCurPtr = 0L;
		@Override
		protected Integer call() throws Exception {
			
			tick0 = System.currentTimeMillis();
			
			updateMessage("準備中...");
			//TODO: Here, do we need hook-event to setup size???
			
			blkCurPtr = blkAllocate(blkFileName,bundle);//prepare data and memory mapping~~~
			if(blkCurPtr==0){
				updateProgress(0,0);
				updateMessage("配置記憶體失敗");
				throw new RuntimeException();
			}
			blkCurPtr += HEAD_SIZE;//add head offset, header structure must refer to native code.
			
			txtAllSize = Misc.num2prefix(blkAllSize)+"B";
			updateMessage(String.format(
				"資料傳輸（0/%s）",
				txtAllSize
			));
			
			//just transfer data as soon as possible~~~
			for(int i=0; i<blkCounter; i++){
				if(isCancelled()==true){
					return -2;
				}
				blkCurPtr = bundle.bulk(blkCurPtr);//this method will offset pointer~~
				updateProgress(++blkIndex, blkCounter);
				updateMessage(String.format(
					"資料傳輸（%s/%s）",
					Misc.num2prefix(blkCurPtr-blkAddress)+"B",
					txtAllSize
				));
			}
			
			//TODO: Here,do we need hook-event to cook data???
			
			tick1 = System.currentTimeMillis() - tick0;
			
			//Check whether we need to flush data~~~
			if(blkFileName.length()==0){
				tick2 = 0L;
			}else{
				//tape out data from memory...
				tick0 = System.currentTimeMillis();//reset tick-time
				updateMessage("flush...");
				blkFlush(blkFileName);
				tick2 = System.currentTimeMillis() - tick0;
			}				
			return 0;
		}
	};
	
	/**
	 * User can call this method to execute task or by clicking button.<p>
	 * The method must be called by GUI-thread.<p>
	 */
	public void doAction(){
		if(looper!=null){
			if(looper.isDone()==false){
				if(looper.cancel()==true){
					doStart();//for next turn~~~
					return;
				}
				PanBase.notifyError("內部錯誤","無法取消工作");//show message~~~~
				return;
			}			
		}

		looper = new TskBulk();
		looper.setOnFailed(event->{
			//this event happened when task throw a exception!!!
			actMessage.textProperty().unbind();
			doStart();
		});
		looper.setOnSucceeded(event->{			
			actMessage.textProperty().unbind();
			if(tick2==0L){
				actMessage.setText(String.format(
					"%s，歷時：%s",
					txtAllSize,Misc.num2time(tick1)
				));
			}else{
				actMessage.setText(String.format(
					"%s，歷時：%s，I/O：%s",
					txtAllSize,
					Misc.num2time(tick1),Misc.num2time(tick2)
				));
			}
			doStart();
		});
		looper.setOnCancelled(event->{
			tick1 = System.currentTimeMillis() - tick0;//calculate tick again!!!
			actMessage.textProperty().unbind();
			actMessage.setText(String.format(
				"取消，歷時：%s",
				Misc.num2time(tick1)
			));
		});
		actProgress.progressProperty().bind(looper.progressProperty());
		actMessage.textProperty().bind(looper.messageProperty());
		new Thread(looper,"BlkRender").start();
		
		doCancel();//let user cancel this action~~~		
	}
}
