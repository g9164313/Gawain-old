package prj.sputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;
import narl.itrc.UtilPhysical;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;

public class StepCollect extends Stepper {

	public StepCollect() {
		set(op0,op1,op2,end);
	}
	
	private final static String init_txt = "EXP.";

	private Label msg1 = new Label(init_txt);
	private Label msg2 = new Label(init_txt);
	private TextField beg_zfac = new TextField("0.5");//Z-factor begin
	private TextField end_zfac = new TextField("3.5");//Z-factor end
	private TextField beg_watt = new TextField("80");//Watt begin
	private TextField end_watt = new TextField("160");//Watt end
	
	private final File fs_temp = new File("temp.xlsx");
	private File fs_book = null;//template and data 
	private int  col_idx = 0;
	
	private boolean init_watt = false;
	private boolean init_zfac = false;
	private int   cur_watt = -1;
	private float cur_zfac = -1f;
	
	private Runnable op0 = ()->{
		read_watt();
		read_zfac();
		Task<?> tsk = waiting_async(new TskStoreData());		
		msg1.setText("儲存數據");	
		msg2.textProperty().bind(tsk.progressProperty()
			.multiply(100f).asString("%.0f%%")
		);
	};
	private Runnable op1 = ()->{
		int beg = Integer.valueOf(beg_watt.getText().trim());
		int end = Integer.valueOf(end_watt.getText().trim());
		int dff = Math.abs(end - beg) / 5;
		
		msg1.setText("調整功率");
		if(init_watt==false) {
			msg2.setText("init...");
			cur_watt = beg;
			init_watt= true;
			next_jump(-1);
		}else{
			if(cur_watt<end){
				msg2.setText("step...");
				cur_watt+= dff;
				next_jump(-1);
			}else {
				msg2.setText("final...");
				init_watt= false;
				next_work();
			}
		} 
		Stepper stp = get(-1);
		if(stp instanceof StepKindler) {
			((StepKindler)stp).boxValue.setText(""+cur_watt);
		}else {
			PanBase.notifyWarning("", "不合法的執行規則！！");
			next_abort();
		}
	};
	private Runnable op2 = ()->{
		float beg = Float.valueOf(beg_zfac.getText().trim());
		float end = Float.valueOf(end_zfac.getText().trim());
		float dff = Math.abs(end - beg) / 5f;
		
		msg1.setText("調整 Z-Fac");
		cur_watt = -1;		
		if(init_zfac==false) {
			msg2.setText("init...");
			cur_zfac = beg;
			init_zfac= true;
			next_jump(-2);
		}else{
			 if(cur_zfac<end){
				msg2.setText("step...");
				cur_zfac+= dff;
				next_jump(-2);
			}else {
				msg2.setText("final...");
				init_zfac= false;
				next_work();
			}
		}
		Stepper stp = get(-2);
		if(stp instanceof StepSetFilm) {
			((StepSetFilm)stp).boxZFactor.setText(""+cur_zfac);
		}else {
			PanBase.notifyWarning("", "不合法的執行規則！！");
			next_abort();
		}
	};
	private Runnable end = ()->{
		msg1.setText(init_txt);
		msg2.setText("");
		next.set(ABORT);
		//for next turn~~~~
		fs_book = null;
		init_watt= false;
		init_zfac= false;		
	};
		
	private void read_watt(){
		Stepper obj = get(-1);
		if(obj instanceof StepKindler) {
			StepKindler stp = (StepKindler)obj;
			cur_watt = Integer.valueOf(stp.boxValue.getText());
		}
	}
	private void read_zfac(){
		Stepper obj = get(-2);
		if(obj instanceof StepSetFilm) {
			StepSetFilm stp = (StepSetFilm)obj;
			cur_zfac = Float.valueOf(stp.boxZFactor.getText());
		}
	}
	
	private class TskStoreData extends Task<Integer> {
		
		Workbook wb;
		DataFormat fmt;
		
		private void prepare_book() throws EncryptedDocumentException, IOException {
			if(fs_book==null) {
				col_idx = 0;
				fs_book = new File(Misc.getDateName()+".xlsx");
				wb = new XSSFWorkbook();
				wb.createSheet("values");
			}else {
				wb = WorkbookFactory.create(fs_temp);
			}
			fmt = wb.createDataFormat();
		}
		final String TAG1 = StepKindler.TAG_FIRE;
		
		private void writing_book(final Mesg[] lst) {			
			updateProgress(0, lst.length);
			
			Sheet sh = wb.getSheetAt(0);
	
			int col = col_idx;
			int row = 1;
			
			get_cell(sh, col, row).setCellValue(
				String.format("Z-Fac=%.2f", cur_zfac)
			);
			get_cell(sh,col+1, row).setCellValue(
				String.format("Watt=%d", cur_watt)
			);
			row+=1;
			
			get_cell(sh,col  , row).setCellValue("時間");
			get_cell(sh,col+1, row).setCellValue("速率");
			row+=1;
			
			CellStyle styl = wb.createCellStyle();
			styl.setDataFormat(fmt.getFormat("0.000"));
			
			for(int i=0; i<lst.length; i++) {
				updateProgress(i+1, lst.length);
				final Mesg msg = lst[i];
				String txt = msg.getText();
				if(txt.startsWith(TAG1)==false) {
					continue;
				}
								
				get_cell(sh, col, row).setCellValue(msg.getTickText(""));
				
				txt = txt.substring(txt.indexOf(":")+1);
				String[] vals = txt.split(",");
				String[] nota;
				nota = UtilPhysical.split(vals[3]);
				
				Cell cc = get_cell(sh, col+1, row);
				cc.setCellStyle(styl);
				cc.setCellValue(Double.valueOf(nota[0]));
				
				row+=1;
			}
			//for next turn~~
			//we will use 3 column for data present
			col_idx = col+3;
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
			dst = new FileOutputStream(fs_temp);
			wb.write(dst);			
			dst.close();
			wb.close();
		}
		@Override
		protected Integer call() throws Exception {
			//dummy for debug~~~~
			//ArrayList<Mesg> lst = (ArrayList<Mesg>) Misc.deserializeFile("dummy-log1.obj");
			//Mesg[] msg = lst.toArray(new Mesg[0]);
			Mesg[] msg = LogStream.getInstance().flushPool();
			
			prepare_book();
			writing_book(msg);
			close_book();
			next_work();
			msg2.textProperty().unbind();
			return 0;
		}
	};
	//-------------------------------//
	
	@Override
	public Node getContent() {
		
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		beg_zfac.setMaxWidth(80);
		end_zfac.setMaxWidth(80);
		beg_watt.setMaxWidth(80);
		end_watt.setMaxWidth(80);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg1, msg2);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.add(new Label("開始"), 2, 1);
		lay.add(new Label("結束"), 2, 2);
		lay.addColumn(3, new Label("Z 因子"), beg_zfac, end_zfac);
		lay.addColumn(4, new Label("Watt")   , beg_watt, end_watt);
		return lay;
	}
	@Override
	public void eventEdit() {
		//no support
	}
	@Override
	public String flatten() {
		return "";//no support
	}
	@Override
	public void expand(String txt) {
		//no support
	}
}
