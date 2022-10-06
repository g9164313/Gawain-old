package prj.sputter.cargo1;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class LayAppear {

	private interface EventInit {
		void run(final CheckBox... chk);
	};
	private interface EventHook {
		void run(final boolean... flg);
	};
	
	private static Pane gen_ctrl_panel(
		final String[] title,
		final EventInit event_init,
		final boolean   multi_flag,
		final EventHook event_hook
	) {
		final CheckBox[] chk = new CheckBox[title.length];
		for(int i=0; i<chk.length; i++) {
			chk[i] = PanBase.genIndicator(title[i]);
		}
		event_init.run(chk);
	
		final Button act_on_off = new Button("設定開關");
		act_on_off.setMaxWidth(Double.MAX_VALUE);
		act_on_off.setOnAction(e->{
			
			ToggleGroup grp = new ToggleGroup();
			
			Node[] opt = new Node[chk.length];
			for(int i=0; i<chk.length; i++) {
				if(multi_flag==true) {
					JFXCheckBox obj = new JFXCheckBox(title[i]);
					obj.setSelected(chk[i].isSelected());
					opt[i] = obj;					
				}else {
					JFXRadioButton obj = new JFXRadioButton(title[i]);
					obj.setSelected(chk[i].isSelected());
					obj.setToggleGroup(grp);					
					opt[i] = obj;					
				}				
			}
			
			GridPane lay = new GridPane();
			lay.setHgap(17.);
			lay.setVgap(17.);
			append_box(lay,opt);
			
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("設定");
			//dia.setHeaderText("選擇");
			dia.getDialogPane().setContent(lay);
			if(dia.showAndWait().get()==ButtonType.OK) {
				final boolean[] flg = new boolean[opt.length];
				for(int i=0; i<opt.length; i++) {
					if(opt[i] instanceof JFXCheckBox) {
						flg[i] = ((JFXCheckBox)opt[i]).isSelected();
					}else if(opt[i] instanceof JFXRadioButton) {
						flg[i] = ((JFXRadioButton)opt[i]).isSelected();
					}
				}
				event_hook.run(flg);
			}
		});
			
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");		
		lay.add(act_on_off, 0, append_box(lay,chk)+1, 4, 1);
		return lay;
	}
	
	private static int append_box(
		final GridPane lay,
		final Node... obj
	) {
		int rr = 0;
		for(int i=0; i<obj.length; i++) {
			lay.addRow(rr, obj[i]);
			if(i%4==0 && i!=0) {
				rr+=1;
			}
		}
		return rr;
	}
	
	/**
	 * SH-1 上檔片，SH-2 左檔片, SH-3 右檔片(正視腔體)
	 * SH-1-->DO0, MFC-1-->DO3, 
	 * SH-2-->DO1, MFC-1-->DO4, 
	 * SH-3-->DO2, MFC-1-->DO5, 
	 * 
	 * SQM160
	 * Crystal Fail  -->DI0, Sample/Thick-->N.C
	 * Time/Dual/SNS2-->N.C,
	 * Shutter       -->DI1
	 */

	public static Pane gen_indi1() {
		final CheckBox chk1 = PanBase.genIndicator("石英失效", PanMain.adam1.DI[0]);
		//CheckBox chk2 = PanBase.genIndicator("RL2", adam1.DI[1]);
		final CheckBox chk3 = PanBase.genIndicator("Shutter", PanMain.adam1.DI[1]);
		//CheckBox chk4 = PanBase.genIndicator("RL4", adam1.DI[3]);
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");		
		lay.addRow(0, chk1, chk3);
		return lay;
	}
	
	
	public static Pane genCtrl_Shutter() {
		return gen_ctrl_panel(
			new String[] {"上檔", "左下", "右下"},
			chk->{				
				chk[0].selectedProperty().bind(PanMain.adam1.DO[0]); 
				chk[1].selectedProperty().bind(PanMain.adam1.DO[1]);
				chk[2].selectedProperty().bind(PanMain.adam1.DO[2]);
			}, true,
			flg->{
				PanMain.adam1.asyncSetAllLevel(
					flg[0], flg[1], flg[2]
				);
				Misc.logv("shutter-select->%s,%s,%s", flg[0], flg[1], flg[2]);
			}
		);
	}
	
	public static Pane genCtrl_Valve() {//MFC 前閥		
		return gen_ctrl_panel(
			new String[] {"MFC-1", "MFC-2", "MFC-3"},
			chk->{
				chk[0].selectedProperty().bind(PanMain.adam1.DO[3]); 
				chk[1].selectedProperty().bind(PanMain.adam1.DO[4]);
				chk[2].selectedProperty().bind(PanMain.adam1.DO[5]);
			}, true,
			flg->{
				PanMain.adam1.asyncSetAllLevel(
					null  , null  , null,
					flg[0], flg[1], flg[2]
				);
				Misc.logv("MFC-select->%s,%s,%s", flg[0], flg[1], flg[2]);
			}
		);
	}
	
	public static Pane genCtrl_Gunhub() {
		return gen_ctrl_panel(
			new String[] {"Gun-1", "Gun-2", "Gun-3"},
			chk->{
				chk[0].selectedProperty().bind(PanMain.adam2.Relay[1]);
				chk[1].selectedProperty().bind(PanMain.adam2.Relay[2]);
				chk[2].selectedProperty().bind(PanMain.adam2.Relay[3]);
			}, false,
			flg->{
				PanMain.adam2.asyncSetAllRelay(
					null,
					flg[0], flg[1], flg[2]
				);
				Misc.logv("gunhub-select->%s,%s,%s", flg[0], flg[1], flg[2]);	
			}
		);
	}
}
