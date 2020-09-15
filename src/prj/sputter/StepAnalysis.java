package prj.sputter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.Stepper;
import narl.itrc.UtilPhysical;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;
import narl.itrc.init.Terminal;

abstract class StepAnalysis extends Stepper {
	
	protected DevSQM160 sqm;
	protected DevDCG100 dcg;
	
	protected int log_indx = 0;
	protected Label[] log_text = {null,null};

	protected static final String pathLogStock= Gawain.pathSock+"監控紀錄"+File.separatorChar;
	protected static final String pathLogCache= pathLogStock+"cache"+File.separatorChar;
	
	private void check_path(final String path) throws Exception {
		File fs = new File(path);
		if(fs.exists()==false) {
			if(fs.mkdirs()==false) {
				throw new Exception("Fail to create "+path);
			}
		}
	}
	private void operation(final boolean flush) {
		final Task<?> tsk = new Task<Void>() {		
			@Override
			protected Void call() throws Exception {
				check_path(pathLogStock);
				check_path(pathLogCache);								
				Mesg[] mesg = (flush==true)?(
					LogStream.getInstance().flushPool()
				):(
					LogStream.getInstance().fetchPool()
				);
				int idx = Math.abs(++log_indx);
				try {
					FileWriter fs = new FileWriter(String.format(
						"%s%s.txt",
						pathLogStock,Misc.getDateName()
					));
					for(int i=0; i<mesg.length; i++){
						updateMessage(String.format("%3d/%3d", i,mesg.length));
						fs.write(String.format(
							"%s    %s\r\n",
							mesg[i].getTickText(""),
							mesg[i].getText()
						));				
					}
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(flush==true) {
					updateMessage("快取中...");
					Misc.serialize2file(String.format(
						"%s%s-%03d.obj",
						pathLogCache,ladder.uuid(),idx
					),mesg);
				}
				unbind_log_text();
				next_work();
				return null;
			}
		};
		bind_log_text(tsk);
		waiting_async(tsk);
	}
	
	protected final Runnable op_save  = ()-> operation(false);
	protected final Runnable op_export= ()-> operation(true);
	
	private void bind_log_text(final Task<?> tsk) {
		if(log_text[0]!=null) {
			log_text[0].setText("匯出紀錄");
		}
		if(log_text[1]!=null) {
			log_text[1].textProperty().bind(tsk.messageProperty());
		}
	}
	private void unbind_log_text() {
		if(log_text[1]!=null) {
			log_text[1].textProperty().unbind();
		}
	}
	
	protected void record_info(final String TAG) {
		
		final float volt = dcg.volt.get();		
		final float amps = dcg.amps.get();				
		final int   watt = (int)dcg.watt.get();
		
		final float rate = sqm.rate[0].get();
		final String unit1 = sqm.unitRate.get();
		
		final float high = sqm.thick[0].get();
		final String unit2 = sqm.unitThick.get();
		
		Misc.logv(
			"%s: %.3f V, %.3f A, %d W, %.3f %s, %.3f %s",
			TAG, 
			volt, amps, watt,
			rate, unit1,
			high, unit2
		);
	}
	
	private static final File tmp_fs = new File(Gawain.pathSock+"temp.csv");
		
	public static Task<?> task_dump(final String uuid){ return new Task<Integer>() {
		@Override
		protected Integer call() throws Exception {			
			updateMessage("掃瞄快取");
			File[] lstFs = new File(pathLogCache).listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if(uuid.length()==0){
						return false;
					}
					return name.contains(uuid);
				}
			});
			if(lstFs.length==0) {
				updateMessage("無快取資料");
				return -1;
			}
			
			//debug~~~~
			//File fs = new File(pathLogCache+"0BADEB-0001.obj");
			//Mesg[] msg = LogStream.read(fs);
			
			//dump2temp("輸出",msg);
			//study_temp();
			
			prepare_book();			
			for(File fs:lstFs) {
				//list all cache files~~~
				Object obj = Misc.deserializeFile(fs.getAbsolutePath());
				Mesg[] msg = (Mesg[])obj;
				writing_book(msg);
			}
			export_book();
			return lstFs.length;
		}
		private void study_temp() {
			String res = Terminal.exec("/usr/bin/python3","bound.py");
			Misc.logv(res);
		}
		private void dump2temp(
			final String tag,
			final Mesg[] msg
		) {
			updateMessage("提取資料中");
			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(tmp_fs,false));
				int row_idx = 0;
				for(Mesg m:msg) {					
					String txt = m.getText();
					if(txt.startsWith(tag)==false) {
						continue;
					}
					updateProgress(++row_idx, msg.length);
					String[] _txt = txt
						.substring(txt.indexOf(":")+1)
						.split(",");
					String[][] col = new String[2][_txt.length];
					for(int i=0; i<_txt.length; i++) {
						String[] val = UtilPhysical.split(_txt[i]);
						col[0][i] = val[0];
						col[1][i] = val[1];
					}
					if(row_idx==1) {
						pw.write(String.join(",",col[1])+"\r\n");
					}
					pw.write(String.join(",",col[0])+"\r\n");
				}
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		Workbook wb;
		DataFormat fmt;
		CellStyle styl;
		
		private void writing_book(final Mesg[] msg) {
			Sheet sh = find_sheet(msg);
			get_cell(sh, 0, 0).setCellValue("清洗過程");
			get_cell(sh, 8, 0).setCellValue("鍍膜過程");
			//dump_message(sh,0,"輸出",msg_k);//debug!!!
			dump_message(sh,0,StepKindler.TAG_CLEAN,msg);			
			dump_message(sh,8,StepWatcher.TAG_WATCH,msg);
		}
		private Sheet find_sheet(final Mesg[] msg) {
			String name = "???";//default sheet name
			for(Mesg m:msg) {
				final String txt = m.getText();
				if(txt.matches("^[\\>\\>].+[\\<\\<]$")==true){					
					name = txt.substring(2,txt.length()-2).trim();
					break;
				}
			}
			Sheet sh = wb.getSheet(name);
			if(sh==null) {
				sh = wb.createSheet(name);
			}
			return sh;
		}
		private void dump_message(
			final Sheet sh,
			final int start_column,
			final String tag,
			final Mesg[] msg
		) {
			int row = 1;
			get_cell(sh, start_column+0, row).setCellValue("時間");
			get_cell(sh, start_column+1, row).setCellValue("電壓");
			get_cell(sh, start_column+2, row).setCellValue("電流");
			get_cell(sh, start_column+3, row).setCellValue("功率");
			get_cell(sh, start_column+4, row).setCellValue("速率");
			get_cell(sh, start_column+5, row).setCellValue("厚度");
			row+=1;
			for(int i=0; i<msg.length; i++){
				updateProgress(i+1, msg.length);
				Mesg m = msg[i];
				String txt = m.getText();
				if(txt.startsWith(tag)==false) {
					continue;
				}
				//put time stamp~~
				get_cell(sh, start_column+0, row).setCellValue(m.getTickText(""));
				//put other records~~~
				String[] val = txt
					.substring(txt.indexOf(":")+1)
					.split(",");
				for(int col=1; col<=5; col++) {
					Cell cc = get_cell(sh, start_column+col, row);
					cc.setCellStyle(styl);
					double _val = UtilPhysical.getDouble(val[col-1]);
					//TODO: check outline~~~
					cc.setCellValue(_val);
				}
				row+=1;
			}
		}		
		private void prepare_book() throws IOException {
			updateMessage("準備中");
			wb = new XSSFWorkbook();
			fmt = wb.createDataFormat();
			styl = wb.createCellStyle();
			styl.setDataFormat(fmt.getFormat("0.000"));
		}
		private void export_book() throws IOException {
			updateMessage("匯出中");
			FileOutputStream dst = new FileOutputStream(String.format(
				"製程紀錄-%s.xlsx",
				Misc.getDateName()
			));			
			wb.write(dst);		
			wb.close();
			dst.close();
		}		
		private Cell get_cell(
			final Sheet sheet,
			final int xx,
			final int yy
		){
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
	};}
}

