package prj.sputter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.Stepper;
import narl.itrc.UtilPhysical;
import narl.itrc.init.LogStream.Mesg;
import narl.itrc.init.Terminal;

abstract class StepExtender extends Stepper {
	
	protected static DevSQM160 sqm;
	protected static DevDCG100 dcg;
	protected static DevCouple cup;
	protected static DevSPIK2k spk;
	
	//protected static ScriptEngineManager sc_man = new ScriptEngineManager();
	//protected static ScriptEngine sc_eng = sc_man.getEngineByName("nashorn");
	
	//------------------------------//
	
	protected final TextField ar_sccm = new TextField("30");
	protected final TextField n2_sccm = new TextField();
	protected final TextField o2_sccm = new TextField("0");
	
	protected final Runnable op_give_mass_flow = ()->{
		wait_async();
		final float sv_ar_sccm = sccm2value(ar_sccm);
		final float sv_n2_sccm = sccm2value(n2_sccm);
		final float sv_o2_sccm = sccm2value(o2_sccm);
		cup.asyncBreakIn(()->{
			cup.set_all_mass_flow(
				sv_ar_sccm, 
				sv_n2_sccm, 
				sv_o2_sccm
			);
			notify_async();
		});
		set_info("注入 sccm");
	};
	private float sccm2value(final TextField box) {
		final String txt = box.getText();
		if(txt.length()==0) {
			return -1f;
		}
		try{
			return Float.valueOf(txt);
		}catch(NumberFormatException e) {		
		}
		return -1f;
	}
	
	private float[] old_sccm = {-1.f, -1.f, -1.f};
	
	protected final Runnable op_wait_mass_flow = ()->{
		
		/*final float[] sv = {
			sccm2value(ar_sccm),
			sccm2value(n2_sccm),
			sccm2value(o2_sccm)
		};*/
		final float[] pv = {
			cup.PV_FlowAr.get(),
			cup.PV_FlowN2.get(),
			cup.PV_FlowO2.get()
		};		
		set_info(
			String.format("Ar:%2.1f/%2.1f",pv[0],old_sccm[0]),
			String.format("N2:%2.1f/%2.1f",pv[1],old_sccm[1]),
			String.format("O2:%2.1f/%2.1f",pv[2],old_sccm[2])
		);		
		boolean flg = false;
		for(int i=0; i<3; i++) {
			if(old_sccm[i]<0.f) {
				old_sccm[i] = pv[i];
				continue;//skip this item~~~~
			}
			if(Math.abs(old_sccm[i]-pv[i])<=0.1f) {
				flg = flg | true;
			}else {
				flg = false;
				old_sccm[i] = pv[i];
			}
		}		
		if(flg==false) {
			hold_step();
		}else {
			//reset old value~~~
			old_sccm[0] = -1.f;
			old_sccm[1] = -1.f;
			old_sccm[2] = -1.f;
			next_step();
		}		
	};
	
	protected final Runnable op_shutter_open = ()->{
		wait_async();
		sqm.asyncBreakIn(()->{
			sqm.cmd_U(true);
			notify_async();
		});
		set_info("打開擋板");
	};
	protected final Runnable op_shutter_open_zero = ()->{
		wait_async();
		sqm.asyncBreakIn(()->{
			sqm.cmd_S_T();
			sqm.cmd_U(true);
			notify_async();
		});
		set_info("打開擋板");
	};
	protected final Runnable op_shutter_close= ()->{		
		wait_async();
		sqm.asyncBreakIn(()->{
			sqm.cmd_U(false);
			notify_async();
		});
		set_info("關閉擋板");
	};
	
	protected final Runnable op_high_pin_open = ()->{
		if(spk.isAsyncDone()==false) {
			return;
		}
		hold_step();
		spk.asyncBreakIn(()->{
			spk.high_pin();
			next_step();
		});
		set_info("打開電極");
	};
	
	protected final TextField spw = new TextField("100");
	protected final TextField spr = new TextField("5");///ramp time
	protected final TextField w_time = new TextField("30");///wait for stable
	
	protected final Runnable op_power_trg = ()->{
		if(dcg.watt.intValue()!=0) {
			next_step();
			return;
		}
		wait_async();
		final int pow_val = Integer.valueOf(spw.getText().trim());
		final int ramp_ms = (int) Misc.text2tick (spr.getText().trim());
		dcg.asyncBreakIn(()->{
			//ensure that 'high-pin' is on!! 
			spk.asyncHighPin();
			spk.blockWaiting();
			//set power~~~~
			dcg.exec(String.format("SPW=%d",pow_val));
			dcg.exec(String.format("SPR=%d",ramp_ms));
			dcg.exec("CHL=W");
			dcg.exec("CHT=C");
			dcg.exec("TRG");//go!!!!
			notify_async();
		});
		set_info("開啟高壓電");
	};	
	protected final Runnable op_wait_fire = ()->{
		final int ramp_ms = (int) Misc.text2tick (spr.getText().trim());
		final int wait_ms = (int) Misc.text2tick (w_time.getText().trim());
		waiting_time(ramp_ms+wait_ms);
	};
			
	protected final Runnable op_power_off = ()->{
		wait_async();
		dcg.asyncBreakIn(()->{
			dcg.exec("OFF");			
			notify_async();
		});
		set_info("關閉高壓電");
	};
	protected final Runnable op_calm_down = ()->{
		int vv = (int)dcg.volt.get();
		int ww = (int)dcg.watt.get();
		set_info(
			"放電中",
			String.format("%3d Volt",vv),
			String.format("%3d Watt",ww)
		);
		if(vv<200 && ww<5) {
			next_step();
		}else {
			hold_step();
		}
	};
	
	//------------------------------//
	
	protected Label[] info = {
		new Label(), new Label(), new Label(),
		new Label(), new Label(), new Label(),
	};
	
	public StepExtender() {
		for(Label obj:info) {
			obj.setPrefWidth(150);
		}
		
		final TextField[] lst = {spw,spr,w_time, ar_sccm,n2_sccm,o2_sccm  };
		for(TextField box:lst) {
			box.setMaxWidth(80);
		}
		
		ar_sccm.setUserData(DevCouple.ar_max_sccm);
		n2_sccm.setUserData(DevCouple.n2_max_sccm);
		o2_sccm.setUserData(DevCouple.o2_max_sccm);
	}
	protected void set_info(final String... txt) {
		if(txt.length==0) {
			for(int i=0; i<info.length; i++) {
				info[i].setText("");
			}
			return;
		}
		for(int i=0; i<info.length; i++) {
			if(i>=txt.length) {
				info[i].setText("");
			}else {
				info[i].setText(txt[i]);
			}
		}
	}
	
	protected void log_data(final String TAG) {
		
		final float volt = dcg.volt.get();		
		final float amps = dcg.amps.get();				
		final int   watt = (int)dcg.watt.get();
		
		final float rate = sqm.meanRate.get();
		final String unit1 = sqm.unitRate.get();
		
		final float high = sqm.meanThick.get();
		final String unit2 = sqm.unitThick.get();
		
		final float mfc1 = cup.PV_FlowAr.get();
		final float mfc2 = cup.PV_FlowN2.get();
		final float mfc3 = cup.PV_FlowO2.get();
		
		Misc.logv(
			"%s: %.3f V, %.3f A, %d W, "+
			"%.3f sccm, %.3f sccm, %.3f sccm, "+
			"%.3f %s, %.3f %s",
			TAG, 
			volt, amps, watt,
			mfc1, mfc2, mfc3,
			rate, unit1, high, unit2
		);
	}
	//------------------------------//
	
	private static final File study_fs = new File(Gawain.getRootPath()+"temp.csv");
	
	protected static final String pathLogStock= Gawain.getRootPath()+"監控紀錄"+File.separatorChar;
	protected static final String pathLogCache= pathLogStock+"cache"+File.separatorChar;
	
	/*private void check_path(final String path) throws Exception {
		File fs = new File(path);
		if(fs.exists()==false) {
			if(fs.mkdirs()==false) {
				throw new Exception("Fail to create "+path);
			}
		}
	}
	
	private void flush_record(final boolean flag) {
		final Task<?> tsk = new Task<Void>() {		
			@Override
			protected Void call() throws Exception {
				check_path(pathLogStock);
				check_path(pathLogCache);								
				Mesg[] mesg = (flag==true)?(
					LogStream.getInstance().flushPool()
				):(
					LogStream.getInstance().fetchPool()
				);
				try {
					FileWriter fs = new FileWriter(String.format(
						"%s%s.txt",
						pathLogStock,Misc.getDateName()
					));
					for(int i=0; i<mesg.length; i++){
						updateMessage(String.format(
							"%3d/%3d", 
							i,mesg.length
						));
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
				if(flag==true) {
					updateMessage("快取中...");
					Misc.serialize2file(mesg,String.format(
						"%s%s+%s.obj",
						pathLogCache,uuid,Misc.getTickName()
					));
				}
				info[2].textProperty().unbind();
				next_step();
				return null;
			}
		};
		info[1].setText("匯出紀錄");
		info[2].textProperty().bind(tsk.messageProperty());
		waiting_async(tsk);
	}*/

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
					return name.startsWith(uuid);
				}
			});
			if(lstFs.length==0) {
				updateMessage("無快取資料");
				return -1;
			}
			//dump data as Excel file~~~
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
		
		private double[][] range;
		
		private void study_message(
			final String tag,
			final Mesg[] msg
		) {
			String study_tool =  Gawain.prop().getProperty("USE_PYTHON","");
			if(study_tool.length()==0) {
				range = null;//clear
				return;
			}
			updateMessage("分析數據");
			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(study_fs,false));
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
			String res = Terminal.exec(study_tool,"bound.py");
			String[] col = res.replace("\r\n", "\n").split("\n");
			range = new double[col.length][];
			for(int i=0; i<col.length; i++) {
				if(col[i].startsWith("range")==true) {
					String[] val = col[i].split(":");
					if(val.length<=3) {
						range[i] = new double[2];
						range[i][0] = Double.valueOf(val[1]);
						range[i][1] = Double.valueOf(val[2]);
						continue;
					}
				}
				range[i] = null;
			}
			return;
		}
		
		Workbook wb;
		DataFormat fmt;
		CellStyle styl_norm, styl_upper, styl_lower;
		
		private void writing_book(final Mesg[] msg) {
			Sheet sh = find_sheet(msg);
			get_cell(sh, 0, 0).setCellValue("清洗過程");
			get_cell(sh, 8, 0).setCellValue("鍍膜過程");

			//study_message(StepKindler.TAG_CLEAN,msg);			
			//dump_message(sh,0,StepKindler.TAG_CLEAN,msg);
			
			study_message(StepWatcher.TAG_WATCH,msg);
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
					int v_idx = col - 1;
					double _val = UtilPhysical.getDouble(val[v_idx]);
					//check outline~~~
					cc.setCellStyle(styl_norm);
					if(range!=null && v_idx<range.length && v_idx!=4) {						
						double[] outline = range[v_idx];
						if(outline!=null) {							
							if(_val<=outline[0]) {								
								cc.setCellStyle(styl_lower);
							}else if(outline[1]<=_val){
								cc.setCellStyle(styl_upper);							
							}						
						}
					}
					cc.setCellValue(_val);
				}
				row+=1;
			}
		}		
		private void prepare_book() throws IOException {
			updateMessage("準備中");
			wb = new XSSFWorkbook();
			
			fmt = wb.createDataFormat();
			short f_id = fmt.getFormat("0.000");
			
			styl_norm = wb.createCellStyle();
			styl_norm.setDataFormat(f_id);
			
			Font fnt_red = wb.createFont();
			fnt_red.setColor(IndexedColors.RED.getIndex());
			
			Font fnt_blue = wb.createFont();
			fnt_blue.setColor(IndexedColors.BLUE.getIndex());
			
			styl_upper= wb.createCellStyle();
			styl_upper.setDataFormat(f_id);
			//styl_upper.setFillBackgroundColor(IndexedColors.RED.getIndex());
			//styl_upper.setFillPattern(FillPatternType.NO_FILL);			
			styl_upper.setFont(fnt_red);
			
			styl_lower= wb.createCellStyle();
			styl_lower.setDataFormat(f_id);
			//styl_lower.setFillBackgroundColor(IndexedColors.BLUE.getIndex());
			//styl_lower.setFillPattern(FillPatternType.NO_FILL);
			styl_lower.setFont(fnt_blue);
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

