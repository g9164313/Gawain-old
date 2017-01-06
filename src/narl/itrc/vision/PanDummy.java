package narl.itrc.vision;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * Setting panel for CamDummy.<p>
 * CamDummy is used to open a image file or image sequence in directory.<p>
 * This panel can control image sequence like playing, pause, previous or next frame.<p>
 * @author qq
 *
 */
public class PanDummy extends PanBase {

	private CamDummy cam = null;
	
	public PanDummy(CamDummy camera){
		cam = camera;
	}

	private JFXCheckBox chkGray;
	
	private Button btnPrev, btnPlay, btnNext;
	
	public JFXSlider sldFrame = new JFXSlider();

	@Override
	protected void eventShown(WindowEvent e){
		if(cam.isGray()==true){
			chkGray.setSelected(true);
		}else{
			chkGray.setSelected(false);
		}
		if(cam.isPlaying()==true){
			btnPlay.setGraphic(Misc.getIcon("pause.png"));
		}else{
			btnPlay.setGraphic(Misc.getIcon("play.png"));
		}
		sldFrame.setMin(1);
		sldFrame.setMax(cam.countFrame());
		sldFrame.setMajorTickUnit(1.);
	}
	
	@Override
	public Parent layout() {

		/*final Label txtPath = new Label();
		if(lstName.isEmpty()==false){
			//txtPath.setText("路徑："+lstName.peek());
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
			}
		});*/
		
		chkGray = new JFXCheckBox("灰階影像");
		chkGray.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(chkGray.isSelected()==true){
					cam.setFmtGray();
				}else{
					cam.setFmtColor();
				}
			}
		});

		btnPrev = PanBase.genButton2("","skip-previous.png");
		btnPrev.setOnAction(event->{
			cam.prevFrame();
		});		
		btnPlay = PanBase.genButton2("",null);
		btnPlay.setOnAction(event->{
			if(cam.isPlaying()==true){
				cam.playFrame(0);//pause camera
				btnPlay.setGraphic(Misc.getIcon("play.png"));				
			}else{
				cam.playFrame(1);//pause camera
				btnPlay.setGraphic(Misc.getIcon("pause.png"));
			}
		});
		btnNext = PanBase.genButton2("","skip-next.png");
		btnNext.setOnAction(event->{
			cam.nextFrame();
		});
		
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		root.add(chkGray, 0,0, 3,1);
		root.addRow(1, btnPrev, btnPlay, btnNext);
		root.add(sldFrame, 0,2, 3,1);
		return root;
	}
}
