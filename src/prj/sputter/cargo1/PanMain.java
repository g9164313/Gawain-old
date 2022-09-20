package prj.sputter.cargo1;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.PanBase;
import prj.sputter.DevAdam4024;
import prj.sputter.DevAdam4055;
import prj.sputter.DevAdam4068;
import prj.sputter.DevAdam4x17;
import prj.sputter.DevCESAR;
import prj.sputter.DevSPIK2k;
import prj.sputter.DevSQM160;
import prj.sputter.LayTool;

public class PanMain extends PanBase {
	
	public static DevCESAR sar1 = new DevCESAR("CESAR1");
	public static DevCESAR sar2 = new DevCESAR("CESAR2");
	public static DevSPIK2k spik= new DevSPIK2k();
	public static DevSQM160 sqm1= new DevSQM160();
	/**
	 * SH-1 上檔片，SH-2&3 下檔片,
	 * SH-1-->DO0, MFC-1-->DO3, Crystal Fail-->DI1, Sample/Thick-->DI4
	 * SH-2-->DO1, MFC-1-->DO4, Time/Dual/SNS2-->DI2 
	 * SH-3-->DO2, MFC-1-->DO5, Shutter-->DI3
	 */
	public static DevAdam4055 adam1 = new DevAdam4055(1);//shutter 1~3, MFC valve 1~3, DI:sqm160
	public static DevAdam4068 adam2 = new DevAdam4068(2);//Gun1~Gun3(RL1~RL3)
	public static DevAdam4x17 adam3 = new DevAdam4x17(3);//MFC PV:1~3
	public static DevAdam4024 adam4 = new DevAdam4024(4);//MFC SV:1~3
	
	private LayLadder ladder = new LayLadder();
	
	public static final float MFC1_MAX_SCCM = 100f;
	public static final float MFC2_MAX_SCCM = 100f;
	public static final float MFC3_MAX_SCCM = 100f;
	
	public static final ReadOnlyFloatProperty mfc1_pv = LayTool.transform(
		adam3.ain[0].val, src->{
		return (src*MFC1_MAX_SCCM)/5f; //0~5V --> 0~100sccm
	});
	public static final ReadOnlyFloatProperty mfc2_pv = LayTool.transform(
		adam3.ain[1].val, src->{
		return (src*MFC2_MAX_SCCM)/5f; //0~5V --> 0~100sccm
	});
	public static final ReadOnlyFloatProperty mfc3_pv = LayTool.transform(
		adam3.ain[2].val, src->{
		return (src*MFC3_MAX_SCCM)/5f; //0~5V --> 0~100sccm
	});
	
	public static void douse_fire() {
		//sar1.setRFOutput(false);
		//sar2.setRFOutput(false);
		spik.setAllOnOff(false, false, false);
	}

	public PanMain(Stage stg) {
		super(stg);
		stg.setOnShown(e->{
			//adam1.open();
			//adam2.open(adam1);
			//adam3.open(adam1);
			//adam4.open(adam1);			
			//sar1.open();
			//sar2.open();
			//sqm1.open();
			//spik.open();
		});
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		JFXButton btn_hold= new JFXButton("停止");
		btn_hold.getStyleClass().add("btn-raised-0");
		btn_hold.setPrefHeight(64.);
		btn_hold.setMaxWidth(Double.MAX_VALUE);
		btn_hold.setOnAction(e->douse_fire());
		AnchorPane.setLeftAnchor(btn_hold, 7.);
		AnchorPane.setRightAnchor(btn_hold, 7.);
		AnchorPane.setBottomAnchor(btn_hold, 7.);
		
		VBox lay_lf = new VBox(
			new Label("CESAR-1"), DevCESAR.genInfoPanel(sar1),
			new Separator(),
			//new Label("CESAR-2"), DevCESAR.genInfoPanel(sar2),
			//new Separator(),
			new Label("SPIK2000"), DevSPIK2k.genInfoPanel(spik),			
			new Separator()
		);
		VBox lay_rh = new VBox(
			new Label("閥門控制"), genValvePan(),
			new Separator(),
			new Label("微量氣體"), gen_MFC_Pan(),
			new Separator(),
			new Label("SQM160"), DevSQM160.genInfoPanel(sqm1),
			new Separator()
			//DevAdam4068.genPanel(adam2)
		);
		lay_lf.getStyleClass().addAll("box-pad");
		lay_rh.getStyleClass().addAll("box-pad");

		BorderPane lay0 = new BorderPane();	
		lay0.setLeft(lay_lf);
		lay0.setCenter(ladder);
		lay0.setRight(new AnchorPane(lay_rh,btn_hold));
		return lay0;
	}
		
	private Node genValvePan() {
		
		final Button act_on_off = new Button("設定開關");
		act_on_off.setMaxWidth(Double.MAX_VALUE);
		act_on_off.setOnAction(e->{
			JFXCheckBox[] chk = {
				new JFXCheckBox("擋板-1"), new JFXCheckBox("擋板-2"), new JFXCheckBox("擋板-3"), 
				new JFXCheckBox("MFC-1"), new JFXCheckBox("MFC-2"), new JFXCheckBox("MFC-3"),
			};
			chk[0].setSelected(adam1.DO[0].get()); 
			chk[1].setSelected(adam1.DO[1].get());
			chk[2].setSelected(adam1.DO[2].get()); 
			chk[3].setSelected(adam1.DO[3].get());
			chk[4].setSelected(adam1.DO[4].get()); 
			chk[5].setSelected(adam1.DO[5].get());
			
			GridPane lay0 = new GridPane();
			lay0.getStyleClass().addAll("box-pad");
			lay0.addColumn(0, chk[0], chk[1], chk[2]);
			lay0.addColumn(1, chk[3], chk[4], chk[5]);
			
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("設定開關");
			dia.setHeaderText("確認開關設定");
			dia.getDialogPane().setContent(lay0);
			if(dia.showAndWait().get()==ButtonType.OK) {
				adam1.asyncSetLevelAll(
					chk[0].isSelected(),chk[1].isSelected(),
					chk[2].isSelected(),chk[3].isSelected(),
					chk[4].isSelected(),chk[5].isSelected()
				);
			}
		});
		
		final Button act_gunhub = new Button("設定Gun-Hub");
		act_gunhub.setMaxWidth(Double.MAX_VALUE);
		act_gunhub.setOnAction(e->{
			JFXRadioButton[] lst = {
				new JFXRadioButton("Gun-1"),
				new JFXRadioButton("Gun-2"),	
				new JFXRadioButton("Gun-3")
			};
			ToggleGroup grp = new ToggleGroup();
			for(JFXRadioButton obj:lst) {
				obj.setToggleGroup(grp);
			}
			VBox lay0 = new VBox(lst);
			lay0.getStyleClass().addAll("box-pad");
			lay0.setSpacing(13);
			
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("設定Gun-Hub");
			dia.setHeaderText("選擇Gun");
			dia.getDialogPane().setContent(lay0);
			if(dia.showAndWait().get()==ButtonType.OK) {
				adam2.asyncSetRelayAll(
					null,
					lst[0].isSelected(),
					lst[1].isSelected(),
					lst[2].isSelected()
				);
			}
		});
		
		CheckBox chk11 = PanBase.genIndicator("石英失效", adam1.DI[1]);
		CheckBox chk12 = PanBase.genIndicator("RL2", adam1.DI[2]);
		CheckBox chk13 = PanBase.genIndicator("Shutter", adam1.DI[3]);
		CheckBox chk14 = PanBase.genIndicator("RL4", adam1.DI[4]);
		
		CheckBox chk01 = PanBase.genIndicator("擋板-1", adam1.DO[0]);
		CheckBox chk02 = PanBase.genIndicator("擋板-2", adam1.DO[1]);
		CheckBox chk03 = PanBase.genIndicator("擋板-3", adam1.DO[2]);
		CheckBox chk04 = PanBase.genIndicator("MFC-1", adam1.DO[3]);
		CheckBox chk05 = PanBase.genIndicator("MFC-2", adam1.DO[4]);
		CheckBox chk06 = PanBase.genIndicator("MFC-3", adam1.DO[5]);
				
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, chk11, chk12);
		lay.addColumn(1, chk13, chk14);		
		lay.addColumn(0, chk01, chk02, chk03);
		lay.addColumn(1, chk04, chk05, chk06);
		lay.add(act_on_off, 0, 4, 2, 1);
		lay.add(act_gunhub, 0, 5, 2, 1);
		return lay;
	}

	
	private Node gen_MFC_Pan() {
		
		final Label[][] txt = new Label[1+3][1+2];
		for(int j=0; j<txt.length; j++) {
			for(int i=0; i<txt[j].length; i++) {
				Label obj = new Label();
				obj.setMinWidth(70.);
				obj.setMaxWidth(Double.MAX_VALUE);
				obj.setAlignment(Pos.CENTER_RIGHT);
				GridPane.setHgrow(obj, Priority.ALWAYS);
				txt[j][i] = obj;
			}
		}
		
		txt[0][1].setText("PV");
		txt[0][2].setText("SV");
		
		txt[1][0].setText("MFC-1");
		txt[2][0].setText("MFC-2");
		txt[3][0].setText("MFC-3");
		
		//PV
		txt[1][1].textProperty().bind(mfc1_pv.asString("%.1f"));
		txt[2][1].textProperty().bind(mfc2_pv.asString("%.1f"));
		txt[3][1].textProperty().bind(mfc3_pv.asString("%.1f"));
		//SV
		txt[1][1].setOnMouseClicked(bind_MFC_SV(txt[1][2],adam4.aout[0],MFC1_MAX_SCCM));
		txt[2][1].setOnMouseClicked(bind_MFC_SV(txt[2][2],adam4.aout[1],MFC2_MAX_SCCM));
		txt[3][1].setOnMouseClicked(bind_MFC_SV(txt[3][2],adam4.aout[2],MFC3_MAX_SCCM));
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		for(int j=0; j<txt.length; j++) {
			lay.addRow(j, txt[j]);
		}
		return lay; 
	}
	
	private EventHandler<? super MouseEvent> bind_MFC_SV(
		Label txt, 
		final DevAdam4024.AOut aout,
		final float MAX_SCCM
	) {
		final ReadOnlyFloatProperty sv = LayTool.transform(aout.val, src->{
			return (src*MAX_SCCM)/5f; //0~5V --> 0~100sccm
		});
		final EventHandler<? super MouseEvent> event = (e)->{
			final TextInputDialog dia = new TextInputDialog(String.format("%.1f", sv.get()));
			dia.setTitle("SCCM");
			dia.setHeaderText("");
			dia.setContentText("");
			final Optional<String> opt = dia.showAndWait();
			if(opt.isPresent()==false) {
				return;
			}
			try {
				//0~100sccm --> 0~5V 
				float sccm = Float.parseFloat(opt.get());
				float volt = 0f;
				if(sccm>=MAX_SCCM) {
					volt = 5f;
				}else if(sccm<=0f){
					volt = 0f;
				}else {
					volt = (sccm*5f)/MAX_SCCM;
				}				
				aout.assign(volt);
			}catch(NumberFormatException e1) {				
			}
		};		
		txt.textProperty().bind(sv.asString("%.1f"));		
		txt.setOnMouseClicked(event);		
		return event;
	}
	
}
