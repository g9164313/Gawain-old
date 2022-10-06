package prj.sputter.cargo1;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;

import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import prj.sputter.DevAdam4024;
import prj.sputter.DevAdam4055;
import prj.sputter.DevAdam4068;
import prj.sputter.DevAdam4x17;
import prj.sputter.DevSPIK2k;
import prj.sputter.DevSQM160;
import prj.sputter.LayTool;

public class PanMain extends PanBase {
	
	//Shutter 1~3 --> DO0(上:true->開),1(左下),2(右下)
	//MFC valve 1~3 --> DO3,4,5
	//SQM160 quartz, shutter-->DI0,1
	//CESAR-1 Ready, Error ON-->DI2,3,4
	//CESAR-2 Ready, Error ON-->DI5,6,7
	public static DevAdam4055 adam1 = new DevAdam4055(1);
	//Gun1 --> RL1, Gun2 --> RL2, Gun3 --> RL3,
	//CESAR-1 Mode_A --> RL4, RF_ON --> RL5
	//CESAR-2 Mode_A --> RL6, RF_ON --> RL7
	public static DevAdam4068 adam2 = new DevAdam4068(2);
	//MFC PV:1~3 --> ain0,1,2
	//CESAR-1 forward/reflect power --> ain4,5 0~10v
	//CESAR-2 forward/reflect power --> ain6,7 0~10v
	public static DevAdam4x17 adam3 = new DevAdam4x17(3);
	//MFC SV:1~3 --> aout1,2,3
	public static DevAdam4024 adam4 = new DevAdam4024(4);
	//CESAR-1:DC Set, RF Set --> aout0, aout1
	//CESAR-2:DC Set, RF Set --> aout2, aout3
	public static DevAdam4024 adam5 = new DevAdam4024(5);
	
	public static DevSPIK2k spik= new DevSPIK2k();
	public static DevSQM160 sqm1= new DevSQM160();
	public static PortCesar sar1= new PortCesar(4,5,0,1);
	public static PortCesar sar2= new PortCesar(6,7,2,3);
	
	private LayLadder ladder = new LayLadder();
	
	public static void douse_fire() {
		sar1.set_onoff(false);
		sar2.set_onoff(false);
		spik.toggle(false, false, false);
	}
	
	public PanMain(Stage stg) {
		super(stg);
		sar1.bind(
			adam1.DI[2], adam1.DI[3], adam1.DI[4], 
			adam3.ain[4].val, adam3.ain[5].val
		);
		sar2.bind(
			adam1.DI[5], adam1.DI[6], adam1.DI[7], 
			adam3.ain[6].val, adam3.ain[7].val
		);
		stg.setOnShown(e->load_all_device());
	}

	private void load_all_device() {
		adam1.open();
		adam2.open(adam1);
		adam3.open(adam1);
		adam4.open(adam1);
		adam5.open(adam1);
		sqm1.open();
		spik.open();
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
			new Label("選擇電極"), LayAppear.genCtrl_Gunhub(),
			new Separator(),
			new Label("CESAR"), new Accordion(
				new TitledPane("離子清洗",sar1),
				new TitledPane("RF濺鍍",sar2)
			),
			new Separator(),
			new Label("SPIK2000"), DevSPIK2k.genInfoPanel(spik),			
			new Separator()
		);
		VBox lay_rh = new VBox(
			new Label("控制檔板"), LayAppear.genCtrl_Shutter(),
			new Separator(),
			new Label("控制閥門"), LayAppear.genCtrl_Valve(),
			new Separator(),
			new Label("微量氣體"), gen_MFC_Pan(),
			new Separator(),			
			new Label("SQM160"), LayAppear.gen_indi1(), DevSQM160.genInfoPanel(sqm1),
			new Separator()
		);
		lay_lf.getStyleClass().addAll("box-pad");
		lay_rh.getStyleClass().addAll("box-pad");

		BorderPane lay0 = new BorderPane();	
		lay0.setLeft(lay_lf);
		lay0.setCenter(ladder);
		lay0.setRight(new AnchorPane(lay_rh,btn_hold));
		return lay0;
	}
	
	private Node gen_MFC_Pan() {
		
		final Label[][] txt = new Label[1+3][1+2];
		for(int j=0; j<txt.length; j++) {
			for(int i=0; i<txt[j].length; i++) {
				Label obj = new Label("0.0");
				obj.setMinWidth(70.);
				obj.setMaxWidth(Double.MAX_VALUE);
				obj.setAlignment(Pos.CENTER_RIGHT);
				GridPane.setHgrow(obj, Priority.ALWAYS);
				txt[j][i] = obj;
			}
		}
		
		txt[0][0].setText("");
		txt[0][1].setText("PV");
		txt[0][2].setText("SV");
		
		txt[1][0].setText("MFC-1");
		txt[2][0].setText("MFC-2");
		txt[3][0].setText("MFC-3");
		
		final EventHandler<? super MouseEvent> e1 = e->MFC_pop_editor("MFC-1 (sccm)", txt[1][2].getText(), adam4.aout[0], MFC1_MAX_SCCM);
		final EventHandler<? super MouseEvent> e2 = e->MFC_pop_editor("MFC-2 (sccm)", txt[2][2].getText(), adam4.aout[1], MFC2_MAX_SCCM);
		final EventHandler<? super MouseEvent> e3 = e->MFC_pop_editor("MFC-3 (sccm)", txt[3][2].getText(), adam4.aout[2], MFC3_MAX_SCCM);
		
		//PV
		txt[1][1].textProperty().bind(mfc1_pv.asString("%.1f"));
		txt[2][1].textProperty().bind(mfc2_pv.asString("%.1f"));
		txt[3][1].textProperty().bind(mfc3_pv.asString("%.1f"));
		txt[1][1].setOnMouseClicked(e1);
		txt[2][1].setOnMouseClicked(e2);
		txt[3][1].setOnMouseClicked(e3);
		//SV
		txt[1][2].textProperty().bind(mfc1_sv.asString("%.1f"));
		txt[2][2].textProperty().bind(mfc2_sv.asString("%.1f"));
		txt[3][2].textProperty().bind(mfc3_sv.asString("%.1f"));
		txt[1][2].setOnMouseClicked(e1);
		txt[2][2].setOnMouseClicked(e2);
		txt[3][2].setOnMouseClicked(e3);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		for(int j=0; j<txt.length; j++) {
			lay.addRow(j, txt[j]);
		}
		return lay; 
	}
	
	public static final float MFC1_MAX_SCCM = 100f;
	public static final float MFC2_MAX_SCCM = 10f;
	public static final float MFC3_MAX_SCCM = 30f;
	
	public static final ReadOnlyFloatProperty mfc1_pv = LayTool.transform(
		adam3.ain[0].val, src->{
		float val = (src*MFC1_MAX_SCCM)/5f;
		if(val<0f) { val = 0f; }
		return val;//volt-->sccm
	});
	public static final ReadOnlyFloatProperty mfc2_pv = LayTool.transform(
		adam3.ain[1].val, src->{
		float val = (src*MFC2_MAX_SCCM)/5f;
		if(val<0f) { val = 0f; }
		return val;//volt-->sccm
	});
	public static final ReadOnlyFloatProperty mfc3_pv = LayTool.transform(
		adam3.ain[2].val, src->{
		float val = (src*MFC3_MAX_SCCM)/5f;
		if(val<0f) { val = 0f; }
		return val;//volt-->sccm
	});
	public static final ReadOnlyFloatProperty mfc1_sv = LayTool.transform(
		adam4.aout[0].val, src->{
		return (src*MFC1_MAX_SCCM)/5f;//volt-->sccm
	});
	public static final ReadOnlyFloatProperty mfc2_sv = LayTool.transform(
		adam4.aout[1].val, src->{
		return (src*MFC2_MAX_SCCM)/5f;//volt-->sccm
	});
	public static final ReadOnlyFloatProperty mfc3_sv = LayTool.transform(
		adam4.aout[2].val, src->{
		return (src*MFC3_MAX_SCCM)/5f;//volt-->sccm
	});
	public static final Float sccm2volt(Float sccm,final float MAX_SCCM) {
		if(sccm==null) {
			return sccm;
		}
		if(sccm>=MAX_SCCM) {
			sccm = 5f;
		}else if(sccm<=0f){
			sccm = 0f;
		}else {
			sccm = (sccm*5f)/MAX_SCCM;
		}
		return sccm;
	}
	private void MFC_pop_editor(
		final String title,
		final String init_val,
		final DevAdam4024.AOut aout,
		final float MAX_SCCM
	) {
		final TextInputDialog dia = new TextInputDialog(init_val);
		dia.setTitle(title);
		dia.setHeaderText("");
		dia.setContentText("");
		final Optional<String> opt = dia.showAndWait();
		if(opt.isPresent()==true) {
			try {
				//sccm --> volt
				aout.assign(sccm2volt(Float.parseFloat(opt.get()),MAX_SCCM));
			}catch(NumberFormatException e1) {				
			}	
		}	
	}
}
