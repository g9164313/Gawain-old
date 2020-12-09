package prj.shelter;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepCalibrateBak extends Stepper {
	
	public static final FloatProperty[] S_PRED_DIST = {
		new SimpleFloatProperty(700f),//unit is 'mm'
	};
	/**
	 * 衰退劑量公式：= radiation * 0.977
	 */
	public static float S_RADI = 0.977f;
	/**
	 * 衰退距離公式：=((distance+90)*0.988)-90
	 */
	public static float S_DIST1 = 90f;
	public static float S_DIST2 = 0.988f;
	public static float S_DIST3 = 90f;
	
	DevHustIO hustio;
	DevAT5350 at5350;
	DevCDR06 cdr06;
	
	public StepCalibrateBak(
		final DevHustIO dev1,
		final DevAT5350 dev2,
		final DevCDR06 dev3
	){
		hustio= dev1;
		at5350= dev2;
		cdr06 = dev3;
		//set(op_upp_a, op_mov1, op_mov2, op_mov3, op_mea1, op_mea2, op_upp_b,
		//	op_low_a, op_mov1, op_mov2, op_mov3, op_mea1, op_mea2, op_low_b);
		set(op_upp_a, op_mov1, op_mov2, op_mov3, op_mea1, op_mea2, op_upp_b);//test~~~
	}

	private static final String UNKNOWN = "＊＊＊";
	
	public class GradValue {
		
		char col_name = 0;
		
		TextField dose = new TextField();//unit is 'μSv/hr'
		TextField loca = new TextField();//unit is 'cm'
		
		Label dose_1y = new Label(UNKNOWN);		
		Label loca_1y = new Label(UNKNOWN);
					
		Label sdv = new Label(UNKNOWN);//Standard Deviation
		Label cov = new Label(UNKNOWN);//Coefficient of Variation
		
		String pin_unit = "";
		double[] pin_dose;
		double pin_loca;
		
		GradValue(){
			
			final double W_VAL = 130.; 
			dose.setPrefWidth(W_VAL);
			loca.setPrefWidth(W_VAL);
			sdv.setPrefWidth(W_VAL);
			cov.setPrefWidth(W_VAL);
			dose_1y.setPrefWidth(W_VAL);
			loca_1y.setPrefWidth(W_VAL);
			
			dose.textProperty().addListener((obv,old,cur)->{
				try{
					float val = Float.valueOf(cur);
					dose_1y.setText(String.format(
						"%.4f",
						val * S_RADI
					));
				}catch(NumberFormatException e){
					dose_1y.setText(UNKNOWN);
				}
			});
			dose.setOnAction(e->update_goal_dose());
			loca.textProperty().addListener((obv,old,cur)->{
				try{
					float val = Float.valueOf(cur);
					loca_1y.setText(String.format(
						"%.4f",
						((val+S_DIST1)*S_DIST2) - S_DIST3 
					));
					
				}catch(NumberFormatException e){
					loca_1y.setText(UNKNOWN);
				}
			});
		}
	};
	
	public GradValue[] pts = {
		new GradValue(),//上限數值
		new GradValue() //下限數值
	};
	public String ispt_name = "";//isotope name
	private float goal_dose = 0f;//unit is 'μSv/hr'
	
	private Label msg1 = new Label();
	private Label msg2 = new Label();
	
	private GradValue cur_pin = null;
	
	//decide which pin mark.
	Runnable op_upp_a = ()->{
		//upper boundary
		cur_pin = pts[0];
		msg1.setText("上邊界");
		next.set(LEAD);
	};
	Runnable op_low_a = ()->{
		//lower boundary
		cur_pin = pts[1];
		msg1.setText("下邊界");
		next.set(LEAD);
	};
	
	//moving and measure dose rate
	Runnable op_mov1 = ()->{
		String pos = cur_pin.loca.textProperty().get().trim();
		hustio.moveToAbs(pos+"cm");
		next.set(LEAD);
	};
	Runnable op_mov2 = ()->{
		if(hustio.isMoving.get()==true){
			msg1.setText("移動中");
			next.set(HOLD);	
		}else{
			msg1.setText("照射中");
			//hustio.isotope.set(ispt_name);
			//hustio.makeRadiation();
			next.set(LEAD);
		}		
	};
	Runnable op_mov3 = ()->{
		long tt = waiting_time(10*1000);
		if(tt>=10){
			msg2.setText(String.format(
				"%s",
				Misc.tick2text(tt,true)
			));
		}else{
			msg2.setText("");
		}
	};
	Runnable op_mea1 = ()->{
		String temp = cdr06.getChannelText(2).get();
		String pres = cdr06.getChannelText(0).get();
		if(
			temp.matches("[-]?\\d+[.]?\\d+")==false ||
			pres.matches("[-]?\\d+[.]?\\d+")==false
		){
			msg1.setText("溫壓?");
			at5350.measure();
		}else{
			msg1.setText("量測中");
			//at5350.measure(temp,pres);
		}		
		next.set(1);
	};
	Runnable op_mea2 = ()->{
		if(at5350.isAsyncDone()==false){
			next.set(HOLD);
			return;
		}
		next.set(LEAD);
	};
	
	//decide boundary and test whether we need to continue~~~
	Runnable op_upp_b = ()->{
		next.set(LEAD);
	};
	Runnable op_low_b = ()->{
		next.set(LEAD);
	};
	
	@Override
	public Node getContent() {

		msg1.setPrefWidth(90.);
		
		GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.addColumn(0, 
			msg2, 
			new Label("上限"), 
			new Label("下限")
		);
		lay1.addColumn(2, 
			new Label("μSv/hr"), 
			pts[0].dose, 
			pts[1].dose 
		);
		lay1.addColumn(3, 
			new Label("DEV"),
			pts[0].sdv,
			pts[1].sdv
		);
		lay1.addColumn(4, 
			new Label("CV"),
			pts[0].cov,
			pts[1].cov
		);
		lay1.addColumn(5, 
			new Label("1y衰退"), 
			pts[0].dose_1y, 
			pts[1].dose_1y 
		);
		lay1.add(new Separator(Orientation.VERTICAL), 6, 0, 1, 3);
		lay1.addColumn(7, 
			new Label("位置(cm)"), 
			pts[0].loca, 
			pts[1].loca 
		);
		lay1.addColumn(8, 
			new Label("1y衰退"), 
			pts[0].loca_1y, 
			pts[1].loca_1y 
		);
		
		HBox lay0 = new HBox();
		lay0.getStyleClass().addAll("box-pad");
		lay0.getChildren().addAll(
			msg1,
			new Separator(Orientation.VERTICAL),
			lay1
		);
		return lay0;
	}

	/**
	 * set value in box.<p>
	 * @param arg - sheet, upper(dose, location, column), lower
	 * @return
	 */
	public StepCalibrateBak editValue(
		final String... arg
	){	
		ispt_name = arg[0];
		
		pts[0].dose.setText(arg[1]);
		pts[0].loca.setText(arg[2]);
		pts[0].col_name = arg[3].charAt(0);
		
		pts[1].dose.setText(arg[4]);
		pts[1].loca.setText(arg[5]);
		pts[1].col_name = arg[6].charAt(0);
		
		update_goal_dose();		
		return this;
	}
	
	private void  update_goal_dose(){
		try{
			float upper = Float.valueOf(pts[0].dose.getText().trim());
			float lower = Float.valueOf(pts[1].dose.getText().trim());
			final float[] golden = {
				10_000f, 
				 9_000f, 8_000f, 7_000f, 6_000f, 5_000f, 4_000f, 3_000f, 2_000f, 1_000f,
				   900f,   800f,   700f,   600f,   500f,   400f,   300f,   200f,   100f,  
				    90f,    80f,    70f,    60f,    50f,    40f,    30f,    20f,    10f,     
				     5f,
			};//unit is 'μSv/hr'
			for(int i=0; i<golden.length; i++){
				float gg = golden[i];
				if(lower<gg && gg<upper){
					goal_dose = gg;
					msg2.setText(String.format("%.0f", gg));
					break;
				}
			}
		}catch(NumberFormatException e){
			
		}
	}
	
	@Override
	public void eventEdit() {
	}

	@Override
	public String flatten() {
		return null;
	}

	@Override
	public void expand(String txt) {
	}

}
