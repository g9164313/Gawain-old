package prj.shelter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;
import narl.itrc.UtilPhysical;

public class LayLadder extends Ladder {
	
	//private static StringProperty txt_press, txt_humid, txt_celus;
	
	public LayLadder(final LayAbacus lay1){
		
		abacus = lay1;
		epilogue = event_finally;
		
		addStep("分隔線", Stepper.Sticker.class);
		addStep("原點補償", RadiateStep.Reset.class);
		addStep(RadiateLoca.NAME, RadiateLoca.class);
		addStep(RadiateDose.NAME, RadiateDose.class);
		genButton("校正程序",e->{
			final DevHustIO.Strength[] lst_ss = quest_strength();
			final String M_TIME = quest_meas_time();
			if(lst_ss==null||M_TIME==null) {
				return;
			}
			
			Stepper stp;
			for(DevHustIO.Strength ss:lst_ss) {
				
				stp = genStep(Stepper.Sticker.class);
				((Stepper.Sticker)stp).setValues("[  校正"+ss.toString()+"  ]");
				
				stp =genStep(RadiateStep.Reset.class);
				((RadiateStep.Reset)stp).setValue(true,true,false);
				
				for(String loca:def_location) {
					stp = genStep(RadiateLoca.class);
					((RadiateLoca)stp).setValues(loca, M_TIME, ss, true, true);
				}
				int cnt = 0;
				for(String dose:def_doserate(ss,true)) {
					if(cnt%2==0) { 
						stp = genStep(Stepper.Sticker.class);
						((Stepper.Sticker)stp).setValues(String.format("%02d", cnt+1));
					}
					cnt++;
					stp = genStep(RadiateDose.class);
					((RadiateDose)stp).setValues(dose, M_TIME, ss, true, true, true);					
				}
			}			
		});
		
		genButton("定點標定",e->{
			final DevHustIO.Strength[] lst_ss = quest_strength();
			final String M_TIME = quest_meas_time();
			if(lst_ss==null||M_TIME==null) {
				return;
			}
			
			Stepper stp;
			for(DevHustIO.Strength ss:lst_ss) {
				
				stp = genStep(Stepper.Sticker.class);
				((Stepper.Sticker)stp).setValues("[  觀測"+ss.toString()+"  ]");
				
				for(String loca:def_location) {
					stp = genStep(RadiateLoca.class);
					((RadiateLoca)stp).setValues(loca, M_TIME, ss, true, true);
				}
			}
		});		
		genButton("劑量驗證",e->{
			final DevHustIO.Strength[] lst_ss = quest_strength();
			final String M_TIME = quest_meas_time();
			if(lst_ss==null||M_TIME==null) {
				return;
			}
			
			Stepper stp;
			for(DevHustIO.Strength ss:lst_ss) {
				
				stp = genStep(Stepper.Sticker.class);
				((Stepper.Sticker)stp).setValues("[  驗證"+ss.toString()+"  ]");
				
				for(String dose:def_doserate(ss,false)) {
					stp = genStep(RadiateDose.class);
					((RadiateDose)stp).setValues(dose, M_TIME, ss, true, false, false);
				}
			}
		});
	}
	
	private DevHustIO.Strength[] quest_strength(){
		final ChoiceDialog<String> dia = new ChoiceDialog<String>("All","3Ci","0.05Ci","0.5Ci");
		dia.setTitle("選取劑量");
		dia.setHeaderText("");
		dia.setContentText("");
		final Optional<String> opt = dia.showAndWait(); 
		if(opt.isPresent()==false) {
			return null;
		}
		
		if(opt.get().equals("0.05Ci")) {
			return new DevHustIO.Strength[] { DevHustIO.Strength.V_005Ci };
		}else if(opt.get().equals("0.5Ci")) {
			return new DevHustIO.Strength[] { DevHustIO.Strength.V_05Ci };
		}else if(opt.get().equals("3Ci")) {
			return new DevHustIO.Strength[] { DevHustIO.Strength.V_3Ci };
		}else if(opt.get().equals("All")) {
			return DevHustIO.Strength.values();
		}
		return null;
	}
	private String quest_meas_time(){
		final TextInputDialog dia = new TextInputDialog("1:00");
		dia.setTitle("照射時間");
		dia.setHeaderText("");
		dia.setContentText("");
		final Optional<String> opt = dia.showAndWait();
		if(opt.isPresent()==false) {
			return null;
		}
		return opt.get();
	}
	
	private final static String[] def_location = {
		"500 cm","400 cm","300 cm","200 cm","100 cm"
	};
	private final static String[] gen_doserate(
		final boolean bound,
		String... values
	) {
		if(bound==false) {
			return values;
		}
		String[] lst = new String[values.length*2];
		for(int i=0; i<values.length; i++) {
			final float vv = Float.valueOf(values[i]);
			final float dd = vv * 0.03f;
			float up = dd*1.7f;
			float dw = dd*0.4f;
			if(up<2f) { up = 2f; }//修正最小的邊界
			if(dw<1f) {	dw = 1f; }
			lst[i*2+0] = String.format("%d uSv/hr", (int)(vv+up)); 
			lst[i*2+1] = String.format("%d uSv/hr", (int)(vv-dw));
			//Misc.logv("%s ~ %s ~ %s", lst[i*2+0], values[i], lst[i*2+1]);
		}
		return lst;
	}
	private final static String[] def_doserate(DevHustIO.Strength ss,boolean bound) {
		String[] lst = null;
		switch(ss) {		
		//case V_005Ci: lst = def_dose_low; break;
		//case V_05Ci : lst = def_dose_middle; break;
		//case V_3Ci  : lst = def_dose_high; break;
		default:
		case V_3Ci  : lst = gen_doserate(bound,
			  "400", "500", "800","1000",
			 "2000","4000","5000","8000","10000"
			 
			 ); break;
		case V_05Ci : lst = gen_doserate(bound,
			  "40", "50", "80","100",
			 "200","400","500","800","1000"
			 ); break;
		case V_005Ci: lst = gen_doserate(bound,
			"5","10","20","25","40","50","80","100"
			); break;
		}
		return lst;
	}
	//----------------------------------//
	
	@Override
	protected void import_step(){
		//final PanBase pan = PanBase.self(this);
		//mark_src = pan.loadFrom();
		//if(mark_src==null){ return; }
		/*Alert dlg = new Alert(AlertType.CONFIRMATION);
		dlg.setTitle("確認");
		dlg.setHeaderText("使用前次校正值？");
		//dlg.setContentText("Are you ok with this?");
		ButtonType[] btn = {
			new ButtonType("是"),
			new ButtonType("否"),
		};
		dlg.getButtonTypes().setAll(btn);
		Optional<ButtonType> res = dlg.showAndWait();
		if(res.isPresent()==false){
			return;
		}*/
		//mark_data = new File("C:\\Users\\qq\\mark-2017.xls");//debug~~~
		//pan.notifyTask(new TskImport());
	}
	
	@Override
	protected void export_step(){
		//mark_data = new File("C:\\Users\\qq\\mark-2017.xls");//debug~~
		final PanBase pan = PanBase.self(this);
		final TskExport tsk = new TskExport();
		//tsk.setOnSucceeded(e->{
			//pan.saveAs(default_name)
		//});
		pan.notifyTask(tsk);		
	}
	
	private LayAbacus abacus;
	private final Runnable event_finally = ()->{
		RadiateStep.hustio.asyncHaltOn();//stop radiation~~~
		if(abacus==null) { return; }
		abacus.dumpModel();
	};
	
	//----------------------------------------//
	
	/**
	 * 現在數值跟衰減後數值，兩兩一組<p>
	 * 第一個是劑量，第二個距離，依序成為一組<p>
	 */
	private static final String[][][] pin_clip = {
		{{"T5","T7", "U5","U7"}, {"T6","T8", "U6","U8"}},
		{{"R5","R7", "S5","S7"}, {"R6","R8", "S6","S8"}},
		{{"P5","P7", "Q5","Q7"}, {"P6","P8", "Q6","Q8"}},
		{{"N5","N7", "O5","O7"}, {"N6","N8", "O6","O8"}},
		{{"L5","L7", "M5","M7"}, {"L6","L8", "M6","M8"}},			
		{{"J5","J7", "K5","K7"}, {"J6","J8", "K6","K8"}},
		{{"H5","H7", "I5","I7"}, {"H6","H8", "I6","I8"}},
		{{"F5","F7", "G5","G7"}, {"F6","F8", "G6","G8"}},
		{{"D5","D7", "E5","E7"}, {"D6","D8", "E6","E8"}},
	};
	
	private DataFormatter fmt;
	private FormulaEvaluator eva;
	
	private String getCellText(
		final Sheet sheet,
		final String address
	){
		CellReference ref = new CellReference(address);
		int yy = ref.getRow();
		int xx = ref.getCol();
		Cell cc = sheet.getRow(yy).getCell(xx);
		String res = fmt.formatCellValue(cc, eva);
		if(res.startsWith("#DIV")==true){
			return "???";
		}
		return res;
	}
	
	private static Cell get_cell(
		final Sheet sheet,
		final String address
	){
		CellReference ref = new CellReference(address);
		int yy = ref.getRow();
		int xx = ref.getCol();
		Row rr = sheet.getRow(yy);
		if(rr==null){
			rr = sheet.createRow(yy);			
		}
		Cell cc = rr.getCell(xx);
		if(cc==null){
			cc = rr.createCell(xx);
		}
		return cc;
	}
	//----------------------------------------//
	
	private class TskExport extends Task<Void>{		
		final File mark_dst;
		final String mark_day;
		
		TskExport(){
			final DateFormat fmt1 = new SimpleDateFormat("yyyyMMdd");
			final DateFormat fmt2 = new SimpleDateFormat("yyyy/MM/dd");
			final Date now_day = Calendar.getInstance().getTime();			
			mark_dst = new File(
				Misc.getHomePath()+
				"標定-"+fmt1.format(now_day)+".xlsx"
			);
			mark_day = fmt2.format(now_day);
		}
		
		@Override
		protected Void call() throws Exception {
			try {
				final Workbook wb = WorkbookFactory.create(true);
				eva = wb.getCreationHelper().createFormulaEvaluator();
				fmt = new DataFormatter();
				
				final RadiateDose[][] radi_all = {
					filter_step(DevHustIO.Strength.V_3Ci),
					filter_step(DevHustIO.Strength.V_05Ci),
					filter_step(DevHustIO.Strength.V_005Ci),
				};
				
				dump_step_result(wb,"3Ci"   ,radi_all[0]);
				dump_step_result(wb,"0.5Ci" ,radi_all[1]);
				dump_step_result(wb,"0.05Ci",radi_all[2]);
				dump_metrix_vals(wb,radi_all);

				//export to file
				FileOutputStream dst = new FileOutputStream(mark_dst);
				wb.write(dst);
				dst.close();
			} catch (IOException e) {
				updateMessage("無法建立試算表："+e.getMessage());
			}
			return null;
		}
		
		private RadiateDose[] filter_step(final DevHustIO.Strength stng) {
			ArrayList<RadiateDose> lst = new ArrayList<RadiateDose>();
			ObservableList<Stepper> all_step = recipe.getItems();
			for(Stepper stp:all_step){
				final String name = stp.getClass().getName();
				updateMessage("檢查 "+name);				
				if(name.equals(RadiateDose.class.getName())==false) {
					continue;
				}
				final RadiateDose ss = (RadiateDose)stp;				
				if(ss.cmb_stng.getValue()==stng) {
					lst.add(ss);
				}				
			}
			return lst.toArray(new RadiateDose[0]); 
		}
		
		private void dump_step_result(
			final Workbook wb,
			final String name,
			final RadiateDose[] lst
		) {
			final Sheet sh = wb.createSheet(name);
			get_cell(sh,"A1").setCellValue("輻射偵檢儀校正實驗室輻射場強度標定紀錄表");
			
			get_cell(sh,"B2").setCellValue("電量計：");
			get_cell(sh,"C2").setCellValue(
				RadiateStep.at5350.Identify[0].get()+" "+
				RadiateStep.at5350.Identify[1].get()
			);
			
			get_cell(sh,"F2").setCellValue("游離腔：");
			get_cell(sh,"G2").setCellValue("PTW TM32002(s/n: 0298)");
			
			get_cell(sh,"J2").setCellValue("校正報告：");
			get_cell(sh,"K2").setCellValue("（NRSL-104140，2015/05/15，INER）90");

			get_cell(sh,"B3").setCellValue("3Ci標定");
			get_cell(sh,"C3").setCellValue("標定日期：");
			get_cell(sh,"D3").setCellValue(mark_day);
						
			get_cell(sh,"A5").setCellValue("now (μSv/hr)");
			get_cell(sh,"A6").setCellValue("1年後(μSv/hr)");
			get_cell(sh,"A7").setCellValue("距離 (cm)");
			get_cell(sh,"A8").setCellValue("新距離(cm)");
			get_cell(sh,"A9").setCellValue("個數 (n)");
			get_cell(sh,"A10").setCellValue("平均 (μSv/hr)");
			get_cell(sh,"A11").setCellValue("Sigma");
			get_cell(sh,"A12").setCellValue("%Sigma");
			get_cell(sh,"A13").setCellValue("計讀值 (μSv/hr)");
			
			
			char s_col = 'D';//skip 2 column~~~
			get_cell(sh,"B4").setCellValue("1");
			get_cell(sh,"C4").setCellValue("2");
			for(int i=0; i<lst.length; i++) {
				get_cell(sh,String.format("%C4", s_col+i)).setCellValue(String.format("%d", 3+i));
			}
			
			for(int s=0; s<lst.length; s++) {
				final RadiateDose stp = lst[s];
				final String last_loca = (String)stp.box_loca.getUserData();
				final String last_meas = (String)stp.txt_desc.getUserData();
				final String[] vals = last_meas.split("\n");
				
				final char cc = (char)(s_col+s);
				
				get_cell(sh,String.format("%C5", cc))
				.setCellFormula(String.format("%C10",cc));
				
				get_cell(sh,String.format("%C6", cc))
				.setCellFormula(String.format("%C10*0.977",cc));
				
				get_cell(sh,String.format("%C7", cc))
				.setCellValue(last_loca.replace("cm", "").trim());
				
				get_cell(sh,String.format("%C8", cc))
				.setCellFormula(String.format("((%C7+90)*0.988)-90", cc));
				
				get_cell(sh,String.format("%C10", cc))
				.setCellFormula(String.format("AVERAGE(%C13:%C32)", cc, cc));
				
				get_cell(sh,String.format("%C11", cc))
				.setCellFormula(String.format("STDEV(%C13:%C32)", cc, cc));
				
				get_cell(sh,String.format("%C12", cc))
				.setCellFormula(String.format("(%C11/%C10)*100", cc, cc));
				
				
				for(int i=0; i<vals.length; i++) {
					String txt = vals[i];
					final int pos = txt.indexOf("#");
					if(pos>=0) {
						txt = txt.substring(0,pos);
					}
					txt = txt.replace('"',' ').trim();
					txt = UtilPhysical.convertScale(txt,"μSv/hr");
					Cell aa = get_cell(sh,String.format("%C%d", cc, 13+i));
					aa.setCellType(CellType.NUMERIC);
					aa.setCellValue(Float.valueOf(txt));		
				}
			}
		}
		
		private void dump_metrix_vals(
			final Workbook wb,
			final RadiateDose[][] lst_all
		) {
			final Sheet sh = wb.createSheet("標定表");
			get_cell(sh,"A1").setCellValue("3Ci");
			get_cell(sh,"A2").setCellValue("距離(cm)");
			get_cell(sh,"B2").setCellValue("劑量(μSv/hr)");
			
			get_cell(sh,"D1").setCellValue("0.5Ci");
			get_cell(sh,"D2").setCellValue("距離(cm)");
			get_cell(sh,"E2").setCellValue("劑量(μSv/hr)");
			
			get_cell(sh,"G1").setCellValue("0.05Ci");
			get_cell(sh,"G2").setCellValue("距離(cm)");
			get_cell(sh,"H2").setCellValue("劑量(μSv/hr)");
			
			final char[][] cols = {
				{'A','B'},
				{'D','E'},
				{'G','H'},
			};
			
			for(int i=0; i<3; i++) {				
				RadiateDose[] lst = lst_all[i];
				for(int j=0; j<lst.length; j++) {
					//final String[] summ = lst[j].lastSummary.split("@");
					final String[] summ = lst[j].txt_desc.getText().split("@");
					if(summ.length<2) {
						continue;
					}
					get_cell(sh,String.format("%C%d", cols[i][0], j+5)).setCellValue(summ[0].trim());
					final String[] vals = summ[1].split("\\s");//avg unit ± dev
					get_cell(sh,String.format("%C%d", cols[i][1], j+5)).setCellValue(vals[0].trim());
				}
			}
		}
	};
}
