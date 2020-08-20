package prj.sputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;
import narl.itrc.UtilPhysical;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;

public class StepWatcher extends Stepper {

	private DevSQM160 sqm;
	private DevDCG100 dcg;
		
	public StepWatcher(
		final DevSQM160 dev1,
		final DevDCG100 dev2 
	){
		sqm = dev1;
		dcg = dev2;
		set(
			op_1,op_2,op_3,op_4,
			op5_1, op5_2,
			op_6
		);
		//set(op5_1, op5_2);
	}
	
	private final static String init_txt = "厚度監控";
	private final static String TAG_MONITOR = "監控中";
	
	private Label msg1 = new Label(init_txt);
	private Label msg2 = new Label("");

	private Label msg3 = new Label("");
	private Label msg4 = new Label("");
	private Label msg5 = new Label("");
	
	long tick_beg = -1L, tick_end = -1L;
	
	final Runnable op_1 = ()->{
		//open shutter
		msg1.setText("歸零");
		msg2.setText("");
		waiting_async();
		sqm.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick
			try {
				sqm.exec("S");
				Thread.sleep(250);
				sqm.exec("T");
				Thread.sleep(250);
				sqm.exec("U1");
			} catch (InterruptedException e) {
			}
			tick_beg = System.currentTimeMillis();
			next.set(LEAD);
		});
	};
	final Runnable op_2 = ()->{
		msg1.setText("等待檔板");
		msg2.setText(String.format(
			"%s",
			Misc.tick2time(waiting_time(5000),true)
		));		
	};
	final Runnable op_3 = ()->{
		//monitor shutter
		tick_end = System.currentTimeMillis();
		
		msg1.setText(TAG_MONITOR);
		msg2.setText("");
		
		msg3.setText(String.format(
			"%5.3f%s",
			sqm.rate[0].get(), sqm.unitRate.get()
		));
		msg4.setText(String.format(
			"%5.3f%s",
			sqm.thick[0].get(), sqm.unitThick.get()
		));	
		msg5.setText(Misc.tick2time(tick_end-tick_beg,true));
				
		print_info(TAG_MONITOR);
		
		if(sqm.shutter.get()==false){
			next_work();
		}else{
			next_hold();
		}
	};
	final Runnable op_4 = ()->{
		//extinguish plasma		
		msg1.setText("關閉高壓");
		msg2.setText("");
		
		waiting_async();
		dcg.asyncBreakIn(()->{
			if(dcg.exec("OFF").endsWith("*")==false) {
				next_abort();
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法關閉!!"));
				return;
			}else {
				next_work();
			}
		});
	};
	
		
	final Runnable op5_1 = ()->{		
		Task<?> tsk = waiting_async(new TskStoreData());		
		msg1.setText("儲存數據");
		msg2.textProperty().bind(
			tsk.progressProperty().multiply(100f).asString("%.0f%%")
		);		
	};	
	final Runnable op5_2 = ()->{
		int vv = (int)dcg.volt.get();
		int ww = (int)dcg.watt.get();
		if(vv>=30 && ww>=1){
			next.set(HOLD);
		}else{
			next.set(LEAD);
		}
		msg1.setText("放電中");
		msg2.setText(String.format("%3dV %3dW",vv,ww));
	};
	
	final Runnable op_6 = ()->{
		msg1.setText(init_txt);
		msg2.setText("");
		msg5.setText(Misc.tick2time(tick_end-tick_beg,true));
	};
	
	private void print_info(final String TAG) {
		final float volt = dcg.volt.get();
		final float amps = dcg.amps.get();		
		final int watt = (int)dcg.watt.get();
		final float rate = sqm.rate[0].get();
		final String unit_rate = sqm.unitRate.get();
		final float high = sqm.thick[0].get();
		final String unit_high = sqm.unitThick.get();
		Misc.logv(
			"%s: %.2f V, %.2f A, %d W, %.3f %s, %.3f %s",
			TAG, 
			volt, amps, watt,
			rate, unit_rate,
			high, unit_high
		);
	}
	private class TskStoreData extends Task<Integer> {
		final File fs_temp = new File("__tmp__.xlsx");
		final File fs_book = new File("monitor.xlsx");
		Workbook wb;
		DataFormat fmt;
		CellStyle styl;
		private void prepare_book() throws IOException {
			//In MS windows, POS can't over write self.
			//So, create a temporary file to store previous data
			if(fs_book.exists()==false){				
				wb = new XSSFWorkbook();
			}else{
				Files.copy(fs_book.toPath(),fs_temp.toPath());
				wb = WorkbookFactory.create(fs_temp);
			}
			fmt = wb.createDataFormat();
			styl = wb.createCellStyle();
			styl.setDataFormat(fmt.getFormat("0.000"));
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
				if(txt.startsWith(TAG_MONITOR)==false) {
					continue;
				}
				//put time~~
				get_cell(sh, 0, row).setCellValue(
					msg.getTickText("")
				);
				String[] val = txt
					.substring(txt.indexOf(":")+1)
					.split(",");
				for(int col=1; col<=5; col++) {
					Cell cc = get_cell(sh, col, row);
					cc.setCellStyle(styl);
					cc.setCellValue(UtilPhysical.getDouble(val[col-1]));
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
		private void close_book() throws IOException {
			FileOutputStream dst;
			dst = new FileOutputStream(fs_book);			
			wb.write(dst);			
			dst.close();
			wb.close();
			Files.deleteIfExists(fs_temp.toPath());
		}		
		@Override
		protected Integer call() throws Exception {
			Mesg[] msg = LogStream.getInstance().flushPool();
			//ArrayList<Mesg> lst = (ArrayList<Mesg>)Misc.deserializeFile("monitor.obj");
			//Mesg[] msg = lst.toArray(new Mesg[0]);
			///--------///			
			updateProgress(0L,100L);
			prepare_book();
			write_book(msg);
			close_book();
			///--------///
			next_work();
			msg2.textProperty().unbind();
			return 0;
		}
	};

	@Override
	public Node getContent(){
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		
		msg3.setMinWidth(100);
		msg4.setMinWidth(100);
		msg4.setMinWidth(100);

		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg1, msg2);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addColumn(2,new Label("速率"),msg3);
		lay.addColumn(3,new Label("厚度"),msg4);
		lay.addColumn(4,new Label("時間"),msg5);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return "";
	}
	@Override
	public void expand(String txt) {
	}
}
