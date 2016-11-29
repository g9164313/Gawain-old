package narl.itrc;

import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class CamVFiles extends CamBundle {

	public CamVFiles(){		
	}

	private static AtomicBoolean isGray = new AtomicBoolean(true);
	
	@Override
	public void setup() {
		/*try {
			if(txtConfig.length()==0){
				lstName.put("####");
				setMatx(0,Misc.imCreate(640,480,CvType.CV_8UC3));
			}else{
				File fs = new File(txtConfig);
				if(fs.exists()==true){
					lstName.add(txtConfig);
					int flag = 1;//IMREAD_COLOR
					if(isGray.get()==true){
						flag = 0;//IMREAD_GRAYSCALE
					}
					setMatx(0,Misc.imRead(txtConfig,flag));
				}
			}
			updateOptEnbl(true);//it is always success!!!
			updateMsgLast("open virtual file");
		} catch (InterruptedException e) {
			e.printStackTrace();
			updateOptEnbl(false);//WTF
			updateMsgLast("fail to manage queue");
		}*/
	}

	@Override
	public void fetch() {
		int cnt = lstName.size();
		if(cnt>1){
			String txt = null;
			try {
				//we may change picture, so update them again
				switch(modePlayer.get()){
				case MODE_AUTO:
					txt = lstName.pollFirst();
					lstName.putLast(txt);
					break;
				case MODE_REST:
					//do nothing, just rewrite overlay~~~
					//mapOverlay(this);
					return;
				case MODE_NEXT:
					txt = lstName.pollFirst();
					lstName.putLast(txt);
					modePlayer.set(MODE_REST);
					break;
				case MODE_PREV:
					txt = lstName.pollLast();
					lstName.putFirst(txt);
					modePlayer.set(MODE_REST);
					break;
				}
				//refreshInf(this);
				int flag = 1;//IMREAD_COLOR
				if(isGray.get()==true){
					flag = 0;//IMREAD_GRAYSCALE
				}
				//setMatx(0,Misc.imRead(txt,flag));
				//Misc.logv("read picture:"+txt);				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//mapOverlay(this);
		//auto adjust delay time~~~
		cnt = 1000/cnt;
		if(cnt>=10){
			Misc.delay(cnt);
		}
	}

	@Override
	public void close() {
		//releasePtr(this);
	}
	//---------------------------//
	
	private final int MODE_AUTO = -1;
	private final int MODE_REST =  0;
	private final int MODE_NEXT =  1;
	private final int MODE_PREV =  2;
	/**
	 * this variable presents how to deal with list<p>
	 * -1 : auto polling the sequence(poll head, then put it to tail)<p>
	 *  0 : don't change sequence<p>
	 *  1 : just change to the next item<p>
	 *  2 : just change to the previous item <p>
	 */
	private AtomicInteger modePlayer = new AtomicInteger(MODE_REST);
	
	private LinkedBlockingDeque<String> lstName = new LinkedBlockingDeque<String>();

	@Override
	public Parent genPanelSetting(PanBase pan) {
		
		final Label txtPath = new Label();
		if(lstName.isEmpty()==false){
			txtPath.setText("路徑："+lstName.peek());
		}else{
			txtPath.setText("路徑：????");
		}
		
		final JFXButton btnFile = new JFXButton();
		btnFile.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				FileChooser chs = Misc.genChooseImage();
				List<File> lst = chs.showOpenMultipleDialog(btnFile.getScene().getWindow());
				if(lst==null){
					return;
				}
				String name = lst.get(0).getAbsolutePath();
				if(lst.size()==1){
					int flag = 1;//IMREAD_COLOR
					if(isGray.get()==true){
						flag = 0;//IMREAD_GRAYSCALE
					}
					//TODO:setMatx(0,Misc.imRead(name,flag));					
				}else{
					name = Misc.trimName(name);
				}
				txtPath.setText("路徑："+name);
				lstName.clear();				
				try {
					for(File fs:lst){
						lstName.putLast(fs.getAbsolutePath());
					}
				} catch (InterruptedException e) {
					Misc.loge("fail to dequeue data");						
				}
			}
		});		
		btnFile.getStyleClass().add("btn-raised-1");
		btnFile.setMaxWidth(Double.MAX_VALUE);
		btnFile.setText("選取檔案");
		
		final JFXButton btnPrev = new JFXButton();
		btnPrev.getStyleClass().add("btn-raised-1");
		btnPrev.setMaxWidth(Double.MAX_VALUE);
		//btnPrev.prefWidthProperty().bind(btnFile.prefWidthProperty().divide(2));
		btnPrev.setText("上一張");
		btnPrev.setGraphic(Misc.getIcon("ic_keyboard_arrow_left_black_24dp_1x.png"));
		btnPrev.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(modePlayer.get()!=MODE_REST){ return; }
				modePlayer.set(MODE_PREV);
			}
		});
		
		final JFXButton btnNext = new JFXButton();
		btnNext.getStyleClass().add("btn-raised-1");
		btnNext.setMaxWidth(Double.MAX_VALUE);
		//btnNext.prefWidthProperty().bind(btnFile.prefWidthProperty().divide(2));
		btnNext.setText("下一張");
		btnNext.setGraphic(Misc.getIcon("ic_keyboard_arrow_right_black_24dp_1x.png"));
		btnNext.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(modePlayer.get()!=MODE_REST){ return; }
				modePlayer.set(MODE_NEXT);
			}
		});
		
		final JFXCheckBox chkAuto = new JFXCheckBox("自動模式");
		chkAuto.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(chkAuto.selectedProperty().get()==true){
					modePlayer.set(MODE_AUTO);
					btnPrev.setDisable(true);
					btnNext.setDisable(true);
				}else{
					modePlayer.set(MODE_REST);
					btnPrev.setDisable(false);
					btnNext.setDisable(false);
				}
			}
		});
		
		final JFXCheckBox chkGray = new JFXCheckBox("灰階影像");
		chkGray.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				isGray.set(chkGray.selectedProperty().get());
			}
		});
		chkGray.setSelected(isGray.get());//this is default~~~
		
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-small");
		root.add(chkGray,0,1,2,1);
		root.add(chkAuto,0,2,2,1);
		root.add(btnFile,0,3,2,1);		
		root.addRow(4,btnPrev,btnNext);
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(txtPath,root);
		return lay0;
	}
}
