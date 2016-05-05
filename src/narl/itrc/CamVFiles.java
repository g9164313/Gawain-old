package narl.itrc;

import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

public class CamVFiles extends CamBundle {

	public CamVFiles(){		
	}

	public native void mapOverlay(CamBundle cam);//copy data to overlay layer
	public native void updateInfo(CamBundle cam);//copy data to overlay layer
	
	@Override
	public void setup(int idx, String txtConfig) {		
		if(txtConfig==null){
			//create dummy layers...
			setMatx(0,Misc.imCreate(640,480,CvType.CV_8UC3));
		}else{
			//only support loading one file
			File fs = new File(txtConfig);
			if(fs.exists()==true){
				lstName.add(txtConfig);
				setMatx(0,Misc.imRead(txtConfig));
			}else{
				setMatx(0,Misc.imCreate(640,480,CvType.CV_8UC3));
			}
		}
		updateInfo(this);
		updateOptEnbl(true);//it always success!!!
		updateMsgLast("open virtual file");
	}

	@Override
	public void fetch() {
		
		
		updateInfo(this);//we may change picture, so update them again
		mapOverlay(this);
	}

	@Override
	public void close() {
		//release all data!!!
		for(int i=0; i<PTR_SIZE; i++){
			long ptr = getMatx(i);
			if(ptr!=0){
				Misc.imRelease(ptr);
			}
			setMatx(i,0L);
		}
		updateOptEnbl(false);//it always success!!!
		updateMsgLast("close virtual file");
	}
	//---------------------------//
	
	private AtomicInteger mode = new AtomicInteger();
	
	private LinkedBlockingQueue<String> lstName = new LinkedBlockingQueue<String>();

	@Override
	public Node getPanSetting() {
		
		final Label txtPath = new Label();
		if(lstName.isEmpty()==false){
			txtPath.setText(lstName.peek());
		}
		
		final JFXButton btnFile = new JFXButton();
		btnFile.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				FileChooser chs = Misc.genChooseImage();
				List<File> lst = chs.showOpenMultipleDialog(btnFile.getScene().getWindow());
				if(lst.isEmpty()==true){
					return;
				}
				String name = lst.get(0).getAbsolutePath();
				if(lst.size()>1){
					name = Misc.trimFileName(name);
				}
				txtPath.setText(name);
				lstName.clear();
				for(File fs:lst){
					lstName.add(fs.getAbsolutePath());
				}
			}
		});
		btnFile.setText("檔案");
		btnFile.getStyleClass().add("btn-raised");
		btnFile.setMaxWidth(Double.MAX_VALUE);
		
		GridPane pan = new GridPane();
		pan.getStyleClass().add("grid-small");
		pan.addRow(0,new Label("路徑："),txtPath);
		pan.add(btnFile,0,1,2,1);
		return pan;
	}
}
