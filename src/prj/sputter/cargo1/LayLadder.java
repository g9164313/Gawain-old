package prj.sputter.cargo1;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import narl.itrc.Gawain;
import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;
import prj.sputter.StepRunSPIK;
import prj.sputter.StepSetFilm;
import prj.sputter.StepSetSPIK;

public class LayLadder extends Ladder {

	public LayLadder(){
		super(Orientation.HORIZONTAL);
		recipe.setMinWidth(200);
		prelogue = event_prelogue;
		epilogue = event_epilogue;		
		addStep("分隔線", Stepper.Sticker.class);
		addStep(StepSetFilm.action_name, StepSetFilm.class, PanMain.sqm1);
		addStep(StepMassFlow.action_name, StepMassFlow.class);		
		addStep(StepGunsHub.action_name, StepGunsHub.class);
		addStep(StepCleanPls.action_name, StepCleanPls.class);
		addStep(StepIgniteRF.action_name, StepIgniteRF.class);
		addStep(StepSetSPIK.action_name, StepSetSPIK.class, PanMain.spik);
		addStep(StepRunSPIK.action_name, StepRunSPIK.class, PanMain.spik);		
		addStep(StepMonitor.action_name, StepMonitor.class);
		
		/*genButton("--layer--",e->{			
			genStep(Stepper.Sticker.class);
			genStep(StepIgnite1.class);
			genStep(StepIgnite2.class);
			genStep(StepperMonitor.class);
		});*/
	}
	
	private final Runnable event_prelogue = ()->{
		LogStream.getInstance().usePool(true);
	};
	
	private final Runnable event_epilogue = ()->{
		LogStream.getInstance().usePool(false);
		
		final Dumping tsk = new Dumping();
		tsk.TAG1 = StepMonitor.action_name;
		
		PanBase.self(this).notifyTask(tsk);
	};
	//-----------------------------------------
	
	private class Dumping extends Task<Void>{
		
		String TAG1 = null;

		@Override
		protected Void call() throws Exception {
			final Mesg[] msg = LogStream.getInstance().flushPool();
			try {
				dump_text(msg);
				dump_xssf(msg);
				updateMessage("完成匯出");
			}catch(IOException e) {
				updateMessage(e.getMessage());
				failed();
			}			
			return null;
		}
		
		private CellStyle sty_num3;
		
		private void dump_xssf(final Mesg[] msg) throws IOException {
			if(msg==null){ return; }
			
			updateMessage("匯出試算表");
				
			Workbook wb = new XSSFWorkbook();
			
			DataFormat fmt = wb.createDataFormat();
			sty_num3 = wb.createCellStyle();
			sty_num3.setDataFormat(fmt.getFormat("0.00"));
			
			Sheet sh0 = wb.createSheet("log");
			int row0 = 0;
			
			Sheet sh1 = wb.createSheet("預設");			
			int row1 = 1;
			
			updateMessage("製作表格");
			for(int i=0; i<msg.length; i++) {
				updateProgress(i,msg.length);				
				//check whether text match special item(">> <<"),
				final String name = is_sticker(msg[i].getText());
				if(name!=null) {
					sh1 = wb.createSheet(name);
					row1= 0;//reset row-index counter!!!
					continue;//put record in new one sheet, after next turn~~~
				}
				if(has_tag(msg[i].getText())==false) {
					get_cell(sh0, 0, row0).setCellValue(msg[i].getTickText(""));
					get_cell(sh0, 1, row0).setCellValue(msg[i].getText());	
					row0+=1;
				}else {
					//print item for special TAG, split text into multiple-columns~~~~
					get_cell(sh1, 0, row1).setCellValue(msg[i].getTickText(""));
					//split, split, split~~~~
					String[] lst = del_tag(msg[i].getText()).trim().split(",");
					int j=1;
					for(String itm:lst) {
						if(itm.length()==0) {
							continue;
						}
						String[] col = itm.trim().split(":");
						if(col.length==1) {
							get_cell(sh1, j, row1).setCellValue(col[0]);
						}else {
							final String v_txt = col[1];
							Object obj = Misc.txt2num(v_txt);
							if(obj==null) {
								get_cell(sh1, j, row1).setCellValue(v_txt);
							}else if(obj instanceof Integer) {
								get_cell(sh1, j, row1).setCellValue(v_txt);
							}else if(obj instanceof Float) {
								Cell cc = get_cell(sh1, j, row1);
								cc.setCellStyle(sty_num3);
								cc.setCellValue((Float)obj);
							}
						}
						j+=1;
					}					
					row1+=1;
				}				
			}
								
			FileOutputStream dst = new FileOutputStream(String.format(
				"%s製程紀錄-%s.xlsx",
				Gawain.getSockPath(),Misc.getDateName()
			));
			wb.write(dst);		
			wb.close();
			dst.close();
		}
		
		private String is_sticker(final String txt) {
			if(txt.startsWith(">>") && txt.endsWith("<<")) {
				return txt.replace(">>", "").replace("<<", "").trim();
			}
			return null;			
		} 
		private boolean has_tag(String txt) {
			txt = txt.trim();
			if(txt.charAt(0)=='[') {
				txt = txt.substring(1);
			}
			return txt.startsWith(TAG1);
		}
		private String del_tag(String txt) {
			txt = txt.trim().replace(TAG1, "");
			if(txt.charAt(0)=='[') {
				txt = txt.substring(1);
			}
			if(txt.charAt(0)==']') {
				txt = txt.substring(1);
			}
			return txt;
		}
				
		private Cell get_cell(
			final Sheet sheet,
			final int xx,
			final int yy
		){
			Row rr = sheet.getRow(yy);
			if(rr==null){ rr = sheet.createRow(yy); }
			Cell cc = rr.getCell(xx);
			if(cc==null){ cc = rr.createCell(xx); }
			return cc;
		}		
		//-----------------------------------------------
		
		private void dump_text(final Mesg[] msg) throws IOException {
			if(msg==null){ return; }			
			FileWriter fs = new FileWriter(String.format(
				"%s製程紀錄-%s.txt",
				Gawain.getSockPath(),Misc.getDateName()
			));
			updateMessage("匯出資料");
			for(int i=0; i<msg.length; i++) {
				updateProgress(i,msg.length);					
				fs.write(String.format(
					"{%s} %s\r\n",
					msg[i].getTickText(""),
					msg[i].getText()
				));
			}
			fs.close();
		}
	};
}
