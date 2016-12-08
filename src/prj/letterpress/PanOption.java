package prj.letterpress;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.Misc;
import narl.itrc.PanDecorate;

public class PanOption extends PanDecorate {

	public PanOption(){
		super("Miscellaneous");		
	}
	
	private final String TXT_START = "UV照射";
	private final String TXT_STOP  = "停止UV";
	private Timeline actExpose;
	private TextField boxExpose;
	private Button btnExpose;
		
	private void begExpose(){
		Entry.stg0.exec("OB 2,1\r\n");
		btnExpose.setText(TXT_STOP);
		btnExpose.setUserData(true);
		boxExpose.setDisable(true);		
	}
	
	private void endExpose(){
		Entry.stg0.exec("OB 2,0\r\n");
		btnExpose.setText(TXT_START);
		btnExpose.setUserData(false);
		boxExpose.setDisable(false);		
	}

	public static void enableAOI(boolean flag){
		//this function is hard code!!!!
		if(flag==true){
			Entry.stg0.exec("OB 1,1\r\n");
			//put down mirror
			Entry.stg2.writeTxt('F');
			//open upper-LED
			Entry.stg1.writeTxt("1,45\r\n",20);
			Entry.stg1.writeTxt("2,45\r\n",20);
			//open bottom-LED
			Entry.stg0.exec("OB 3,1\r\n");
		}else{
			Entry.stg0.exec("OB 1,0\r\n");
			//raise up mirror
			Entry.stg2.writeTxt('R');
			//close upper-LED
			Entry.stg1.writeTxt("1,0\r\n",20);
			Entry.stg1.writeTxt("2,0\r\n",20);
			//open bottom-LED
			Entry.stg0.exec("OB 3,0\r\n");
		}
	}
		
	private static int valLight = 45;
	
	private Node layoutOption3(){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-small");
		
		final int BOX_SIZE = 90;
		
		boxExpose = new TextField("1sec");
		boxExpose.setPrefWidth(BOX_SIZE);
		
		btnExpose = new Button(TXT_START);		
		btnExpose.setUserData(false);
		btnExpose.setOnAction(event->{
			double val = 0.;
			try{
				val = Misc.phyConvert(boxExpose.getText().trim(),"sec");
				val = val*1000.;
			}catch(NumberFormatException e){
				boxExpose.setText("1sec");
				return;
			}
			boolean flag = (boolean)(btnExpose.getUserData());
			if(flag==false){
				begExpose();
				actExpose = new Timeline(new KeyFrame(
					Duration.millis(val),
					event1->endExpose()
				));
				actExpose.play();
			}else{
				actExpose.stop();
				endExpose();
			}
		});
		btnExpose.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(btnExpose, Priority.ALWAYS);

		TextField boxLight = new TextField(""+valLight);
		boxLight.setPrefWidth(BOX_SIZE);
		Button btnLight = new Button("光源強度");		
		btnLight.setOnAction(event->{
			String txt = boxLight.getText();			
			try{
				valLight = Integer.valueOf(txt);
				Entry.stg1.writeTxt("1,"+valLight+"\r\n",20);
				Entry.stg1.writeTxt("2,"+valLight+"\r\n",20);
			}catch(NumberFormatException e){
				boxLight.setText(""+valLight);//We fail, just reset value~~~
			}
		});
		btnLight.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(btnLight, Priority.ALWAYS);
		
		lay.addRow(0,boxExpose,btnExpose);
		lay.addRow(1,boxLight,btnLight);
		return lay;
	}

	private Node layoutOption4(){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-small");
		
		final CheckBox chkPump = new CheckBox("幫浦");
		chkPump.setIndeterminate(true);
		chkPump.setOnAction(event->{
			if(chkPump.isSelected()){
				Entry.stg0.exec("OB 1,1\r\n");
			}else{
				Entry.stg0.exec("OB 1,0\r\n");
			}
		});
		
		final CheckBox chkMirror = new CheckBox("反射鏡");
		chkMirror.setIndeterminate(true);
		chkMirror.setOnAction(event->{
			if(chkMirror.isSelected()){
				Entry.stg2.writeTxt('F');
			}else{
				Entry.stg2.writeTxt('R');
			}
		});
		
		final CheckBox chkLED = new CheckBox("底部光源");
		chkLED.setIndeterminate(true);
		chkLED.setOnAction(event->{
			if(chkLED.isSelected()){
				Entry.stg0.exec("OB 3,1\r\n");
			}else{
				Entry.stg0.exec("OB 3,0\r\n");
			}
		});
		
		lay.addRow(0, chkPump,chkMirror,chkLED);
		return lay;
	}

	@Override
	public Node layoutBody() {
		VBox lay2 = new VBox();
		lay2.getStyleClass().add("vbox-small");
		lay2.getChildren().addAll(
			layoutOption3(),
			layoutOption4()
		);
		return lay2;
	}
}
