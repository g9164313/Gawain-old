package prj.shelter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

import com.sun.glass.ui.Application;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import narl.itrc.Ladder;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class LayLadder extends Ladder {

	public LayLadder(
		final DevHustIO dev1,
		final DevAT5350 dev2,
		final DevCDR06 dev3
	){
		addStep("分隔線", Stepper.Sticker.class);
		addStep("核種。原點。補償", StepArrange.class, dev1,dev2);
		addStep("校正刻度", StepGraduate.class, dev1,dev2,dev3);
		addStep("照射劑量", StepRadiate.class , dev1);
	}
	
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
	
	private File mark_src = null;//template and data 
	
	private class TskImport extends Task<Void>{
		TskImport(){
			recipe.getItems().clear();
		}
		@Override
		protected Void call() throws Exception {			
			try {
				Workbook wb = WorkbookFactory.create(mark_src);
				eva = wb.getCreationHelper().createFormulaEvaluator();
				fmt = new DataFormatter();
				//collect_mark(wb.getSheetAt(0));
				//collect_mark(wb.getSheetAt(1));
				collect_mark(wb.getSheetAt(2));
				wb.close();
			} catch (IOException e) {
				updateMessage("無法讀取試算表："+e.getMessage());
			}
			return null;
		}
		void collect_mark(final Sheet sh){
			String name = sh.getSheetName();
			updateMessage("從 "+name+" 收集標定點");
			
			Application.invokeAndWait(()->{
				((Stepper.Sticker)genStep(Stepper.Sticker.class)).editValue(name);
				((StepArrange)genStep(StepArrange.class)).editValue(name,true,true);
			});
			//使用衰減過後的劑量跟距離當成初始值
			for(String[][] pin:pin_clip){
				String[] val = {
					//upper boundary
					getCellText(sh,pin[1][0]),//dose rate(μSv/hr)
					getCellText(sh,pin[1][1]),//location(cm)
					//lower boundary
					getCellText(sh,pin[1][2]),//dose rate('μSv/hr')
					getCellText(sh,pin[1][3]),//location(cm)
				};
				if(val[0].matches("[-]?\\d+[.]?\\d+")==false){
					continue;//skip
				}				
				updateMessage(String.format(
					"更新 %s校正刻度: %s-%s, %s-%s", 
					name,
					pin[1][0], pin[1][1],
					pin[1][2], pin[1][3]
				));				
				Application.invokeAndWait(()->{
					((StepGraduate)genStep(StepGraduate.class)).editValue(
						name, 
						val[0], val[1], pin[1][0],
						val[2], val[3], pin[1][2]
					);
				});
			}			
		}
	};
	
	private class TskExport extends Task<Void>{
		
		File mark_dst=null;		

		TskExport(){
			String name = mark_src.getAbsolutePath();
			int pos = name.lastIndexOf(File.separatorChar);
			if(pos<0){
				return;
			}
			Date day = Calendar.getInstance().getTime();  
			DateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			name = name.substring(0,pos+1);
			name = name + "標定-" + fmt.format(day)+".xlsx";
			mark_dst = new File(name);
		}
		
		@Override
		protected Void call() throws Exception {
			//updateMessage("複製檔案");
			//Files.copy(
			//	mark_src.toPath(), 
			//	mark_dst.toPath(), 
			//	StandardCopyOption.REPLACE_EXISTING
			//);
			try {
				Workbook wb = WorkbookFactory.create(mark_src);
				eva = wb.getCreationHelper().createFormulaEvaluator();
				fmt = new DataFormatter();
				//清除第一二項資料
				clear_old_mark(wb.getSheetAt(0));
				clear_old_mark(wb.getSheetAt(1));
				clear_old_mark(wb.getSheetAt(2));
				//重新填寫資料
				ObservableList<Stepper> lst = recipe.getItems();
				for(Stepper stp:lst){
					updateMessage("確認步驟；"+stp.getClass().getName());
					if(stp.getClass()!=StepGraduate.class){
						continue;
					}
					fill_mark_data(wb,(StepGraduate) stp);
				}
				//清除舊有的標定表
				wb.removeSheetAt(3);
				update_pin_table(wb);
				//export to file
				FileOutputStream dst = new FileOutputStream(mark_dst);
				wb.write(dst);
				dst.close();
			} catch (IOException e) {
				updateMessage("無法讀取試算表："+e.getMessage());
			}
			return null;
		}		
		private void clear_old_mark(final Sheet sh){			
			updateMessage(sh.getSheetName()+": 清除資料");
			final char[] lst = {
				'B','C','D','E','F','G','H','I','J','K',
				'L','M','N','O','P','Q','R','S','T','U'
			};
			for(char cc:lst){
				for(int rr=13; rr<33; rr++){
					get_cell(sh,""+cc+rr).setCellValue("");			
				}
			}
		}		
		private void fill_mark_data(
			final Workbook wb,
			final StepGraduate stp
		){
			Sheet sh = wb.getSheet(stp.ispt_name);
			fill_pin_value(sh,stp.pts[0]);
			fill_pin_value(sh,stp.pts[1]);			
		}
		private void fill_pin_value(
			final Sheet sh,
			final StepGraduate.GradValue pts
		){
			updateMessage(String.format(
				"%s: 更新欄位-%c", 
				sh.getSheetName(), pts.col_name
			));
			
			get_cell(sh,
				""+pts.col_name+"7"
			).setCellValue(
				pts.pin_loca
			);//distance
			
			double[] val = pts.pin_dose;
			if(val==null){
				return;
			}
			//the value of dose rate
			for(int rr=13, ii=0; rr<33; rr++, ii++){
				if(ii>=val.length){
					break;
				}
				get_cell(sh,
					""+pts.col_name+rr
				).setCellValue(
					val[ii]
				);
			}
		}
		private void update_pin_table(final Workbook wb){
			updateMessage("更新標定表");
			Sheet[] mrk = {
				wb.getSheetAt(0),//3Ci
				wb.getSheetAt(1),//0.5Ci
				wb.getSheetAt(2),//0.05Ci
			};
			Sheet pin = wb.createSheet("標定表");
			get_cell(pin,"A1").setCellValue("3Ci");
			get_cell(pin,"A2").setCellValue("距離(cm)");
			get_cell(pin,"B2").setCellValue("劑量(μSv/hr)");
			tranpose_mark(mrk[0],pin,'A','B');
			
			get_cell(pin,"C1").setCellValue("0.5Ci");
			get_cell(pin,"C2").setCellValue("距離(cm)");
			get_cell(pin,"D2").setCellValue("劑量(μSv/hr)");
			tranpose_mark(mrk[1],pin,'C','D');
			
			get_cell(pin,"E1").setCellValue("0.05Ci");
			get_cell(pin,"E2").setCellValue("距離(cm)");
			get_cell(pin,"F2").setCellValue("劑量(μSv/hr)");
			tranpose_mark(mrk[2],pin,'E','F');
		}
		private void tranpose_mark(
			final Sheet src,
			final Sheet dst,
			final char dst_col1,
			final char dst_col2
		){
			final char[] src_col = {
				'D','E','F','G','H','I','J','K','L',
				'M','N','O','P','Q','R','S','T','U'
			};
			int dst_row_idx = 5;//skip the first and second row.<p>
			for(char sc:src_col){
				String[] val = {
					getCellText(src,""+sc+"7"),
					getCellText(src,""+sc+"10"),
				};
				try{
					//trim location value
					float loc = Float.valueOf(val[0]);
					val[0] = String.format("%.2f", loc);
				}catch(NumberFormatException e){					
				}
				get_cell(dst,""+dst_col1+""+dst_row_idx).setCellValue(val[0]);
				get_cell(dst,""+dst_col2+""+dst_row_idx).setCellValue(val[1]);
				dst_row_idx+=1;
			}
		}
	};

	@Override
	protected void import_step(){
		PanBase pan = PanBase.self(this);
		mark_src = pan.loadFrom();
		if(mark_src==null){
			return;
		}
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
		pan.notifyTask(new TskImport());
	}
	
	@Override
	protected void export_step(){
		//mark_data = new File("C:\\Users\\qq\\mark-2017.xls");//debug~~
		if(mark_src==null){
			PanBase.notifyWarning("", "請先匯入表格!!");
			return;
		}
		PanBase.self(this).notifyTask(new TskExport());
	}
}
