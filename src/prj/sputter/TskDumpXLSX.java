package prj.sputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.concurrent.Task;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.UtilPhysical;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;

public class TskDumpXLSX extends Task<Integer> {
	
	final String prefix;
	
	public TskDumpXLSX(final String uuid) {
		prefix = uuid;
	}
	
	@Override
	protected Integer call() throws Exception {
		
		File fs = new File(StepAnalysis.pathLogCache+"0BADEB-0001.obj");
		Mesg[] msg = LogStream.read(fs);
		
		/*
		updateMessage("掃瞄紀錄");
		File[] lstFs = Gawain.dirSock.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix);
			}
		});
		if(lstFs.length==0) {
			return -1;
		}
		prepare_book();
		for(File fs:lstFs) {
			updateMessage("解析 "+fs.getName().split("-")[0]);
			Object obj = Misc.deserializeFile(fs.getAbsolutePath());
			Mesg[] lstMsg = (Mesg[])obj;
			//TODO:Outlier Detection!!!
			write_book(lstMsg);
		}
		close_book();*/
		return 0;
	}
	
	Workbook wb;
	DataFormat fmt;
	CellStyle styl;
	private void prepare_book() throws IOException {
		wb = new XSSFWorkbook();
		fmt = wb.createDataFormat();
		styl = wb.createCellStyle();
		styl.setDataFormat(fmt.getFormat("0.000"));
	}
	private void close_book() throws IOException {
		FileOutputStream dst = new FileOutputStream(String.format(
			"monitor-%s.xlsx",
			Misc.getDateName()
		));			
		wb.write(dst);		
		wb.close();
		dst.close();
	}
	private void write_book(final Mesg[] lst) {			
		//prepare sheet
		String s_title = "???";//default sheet name
		for(int i=0; i<lst.length; i++){
			final Mesg msg = lst[i];
			final String txt = msg.getText();
			if(txt.matches("^[\\>\\>].+[\\<\\<]$")==true){					
				s_title = txt.substring(2,txt.length()-2).trim();
				break;
			}
		}
		Sheet sh = wb.getSheet(s_title);
		if(sh==null) {
			sh = wb.createSheet(s_title);
		}
		updateMessage("寫入 "+s_title);
		
		//writing data~~
		int row = 0;
		get_cell(sh, 0, row).setCellValue("時間");
		get_cell(sh, 1, row).setCellValue("電壓");
		get_cell(sh, 2, row).setCellValue("電流");
		get_cell(sh, 3, row).setCellValue("功率");
		get_cell(sh, 4, row).setCellValue("速率");
		get_cell(sh, 5, row).setCellValue("厚度");
		row+=1;
		for(int i=0; i<lst.length; i++){
			updateProgress(i+1, lst.length);
			final Mesg msg = lst[i];
			String txt = msg.getText();
			if(txt.startsWith(StepWatcher.TAG_WATCH)==false) {
				continue;
			}
			//put time stamp~~
			get_cell(sh, 0, row).setCellValue(
				msg.getTickText("")
			);
			//put other records~~~
			String[] val = txt
				.substring(txt.indexOf(":")+1)
				.split(",");
			for(int col=1; col<=5; col++) {
				Cell cc = get_cell(sh, col, row);
				cc.setCellStyle(styl);
				double _val = UtilPhysical.getDouble(val[col-1]);
				//TODO: mark data label~~~
				cc.setCellValue(_val);
			}
			row+=1;
		}
		return;
	}
	Cell get_cell(
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
}
