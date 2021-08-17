package prj.sputter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;

public class LayLogger extends BorderPane {

	public LayLogger() {
		
		Node n1 = LogStream.getInstance().genViewer();
		Node n2 = layout_gauge();
		
		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton r_n1 =	new JFXRadioButton("訊息");
		final JFXRadioButton r_n2 =	new JFXRadioButton("儀錶");
		r_n1.setToggleGroup(grp);
		r_n2.setToggleGroup(grp);
		r_n1.setSelected(true);
		
		n1.visibleProperty().bind(r_n1.selectedProperty());
		n2.visibleProperty().bind(r_n2.selectedProperty());
				
		final StackPane lay_view = new StackPane(n1,n2);
		
		final JFXButton btn_review = new JFXButton("調閱");
		btn_review.getStyleClass().add("btn-raised-1");
		btn_review.setOnAction(e->{
			final FileChooser diag = new FileChooser();
			diag.setTitle("");
			diag.setInitialDirectory(Gawain.dirSock);
			diag.getExtensionFilters().add(new FileChooser.ExtensionFilter("紀錄檔","*.txt","*.obj"));
			final File fs = diag.showOpenDialog(Misc.getParent(e));
			if(fs==null) {
				return;
			}
			PanBase.self(this).notifyTask("輸出紀錄",new Dumping(fs));
		});
		final JFXButton btn_rever = new JFXButton("test");
		btn_rever.getStyleClass().add("btn-raised-1");
		btn_rever.setOnAction(e->{
			//if(badge.isPresent()==false) {
			//	show_progress();
			//}else {
			//	done_progress();
			//}			
		});
		
		final HBox lay_ctrl = new HBox(
			r_n1,r_n2,
			btn_review,btn_rever
		);
		lay_ctrl.getStyleClass().addAll("tool-pad");
		setCenter(lay_view);	
		setBottom(lay_ctrl);
	}	
	//-----------------------------------------------//
	
	/*private class Badge extends HBox {
		final Label txt = new Label("紀錄中");
		final ProgressBar bar = new ProgressBar();
		Badge() {
			bar.setProgress(-1.f);
			//bar.setPrefWidth(100);
			setStyle("-fx-hgap: 7;");
			setAlignment(Pos.CENTER);
			getChildren().addAll(txt,bar);			
		}
	};
	private Optional<Badge> badge = Optional.empty();
	*/

	private static final String pathLogStock = Gawain.pathSock+"監控紀錄"+File.separatorChar;
	private static final String pathLogCache = pathLogStock+"cache"+File.separatorChar;	
	private static final SimpleDateFormat fmt_time = new SimpleDateFormat("HH:mm:ss.SSS");
	
	private class Dumping extends Task<Void>{
		final File fs;
		Dumping(){
			fs = null;//source is from stdio~~~
		}
		Dumping(final File src){
			fs = src;//source is from file~~~
		}
		private void check_path(final String path) throws Exception {
			updateMessage("確認路徑： "+path);
			File fs = new File(path);
			if(fs.exists()==false) {
				if(fs.mkdirs()==false) {
					updateMessage("無效的路徑："+path);
					throw new Exception("Fail to create "+path);
				}
			}			
		}
		private void dump_text(final Mesg[] msg) {
			if(msg==null){ return; }
			try {
				updateMessage("資料輸出");
				FileWriter fs = new FileWriter(String.format(
					"%s%s.txt",
					pathLogStock, Misc.getDateName()
				));
				for(int i=0; i<msg.length; i++) {
					updateProgress(i,msg.length);					
					fs.write(String.format(
						"%s] %s\r\n",
						msg[i].getTickText(""),
						msg[i].getText()
					));
				}
				fs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		private Mesg[] plain_restore(final File fs) {
			final ArrayList<Mesg> lst = new ArrayList<Mesg>();
			try {
				//Scanner scn = new Scanner(fs,Charset.forName("big5").name());
				Scanner scn = new Scanner(fs);
				while(scn.hasNextLine()){
					String flat = scn.nextLine();
					if(flat.length()==0) {
						continue;
					}
					try {
						int i = flat.indexOf(']');
						if(i<0) {
							continue;
						}
						String[] arg = {
							flat.substring(0,i),
							flat.substring(i+1).trim(),
						};
						Mesg mm = new Mesg(
							fmt_time.parse(arg[0]).getTime(),
							arg[1]
						);
						lst.add(mm);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				scn.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return lst.toArray(new Mesg[0]);
		}
		@Override
		protected Void call() throws Exception {
			check_path(pathLogStock);
			check_path(pathLogCache);
			updateMessage("讀取資料");
			Mesg[] msg = null;
			if(fs==null) {
				msg = LogStream.getInstance().flushPool();
				dump_text(msg);
			}else if(fs.getName().matches(".*[.]obj$")==true){
				msg = (Mesg[]) Misc.deserializeFile(fs);
			}else if(fs.getName().matches(".*[.]txt$")==true){
				msg = plain_restore(fs);
			}
			//Misc.serialize2file(msg, pathLogCache+"temp.obj");
			Workbook wb = new XSSFWorkbook();
			export_mesg(wb,msg);
			export_book(wb);
			updateMessage("完成匯出");
			return null;
		}
		
		int[] row_idx = {0,0};
		
		private void export_mesg(
			final Workbook wb,
			final Mesg[] msg
		) throws IOException {
			if(msg==null) {
				return;
			}
			updateMessage("工作中");			
			Sheet sh0 = wb.createSheet("時間表");
			get_cell(sh0, 0, 0).setCellValue("時間");
			get_cell(sh0, 1, 0).setCellValue("標記");
			Sheet shx = null;
			for(int i=0; i<msg.length; i++) {
				updateProgress(i,msg.length);
				flush_action(sh0,msg[i]);
				shx = flush_recipe(shx,wb,msg[i]);	
			}
		}
		private Sheet flush_recipe(
			Sheet shx,
			final Workbook wb,
			final Mesg msg
		) {
			final String txt = msg.getText();			
			if(txt.matches("^[\\>\\>].+[\\<\\<]$")==true) {
				//create a new sheet~~~
				shx = wb.createSheet(txt.substring(2,txt.length()-2).trim());
				init_column_name(shx,0);
				init_column_name(shx,1);
				row_idx[0] = row_idx[1] = 1;
				return shx;
			}
			if(shx==null) {
				return shx;
			}
			if(txt.startsWith(StepKindler.TAG_KINDLE)==true) {
				set_column_value(shx,0,msg);
			}else if(txt.startsWith(StepWatcher.TAG_WATCH)==true) {
				set_column_value(shx,1,msg);
			}			
			return shx;
		}
		private void init_column_name(final Sheet sh,int page) {
			page = page * 10;
			get_cell(sh, page+0, 0).setCellValue("時間");
			get_cell(sh, page+1, 0).setCellValue("電壓(V)");
			get_cell(sh, page+2, 0).setCellValue("電流(A)");
			get_cell(sh, page+3, 0).setCellValue("功率(W)");
			get_cell(sh, page+4, 0).setCellValue("mfc-1(sccm)");
			get_cell(sh, page+5, 0).setCellValue("mfc-2(sccm)");
			get_cell(sh, page+6, 0).setCellValue("mfc-3(sccm)");
			get_cell(sh, page+7, 0).setCellValue("速率(Å/s)");
			get_cell(sh, page+8, 0).setCellValue("厚度(kÅ)");
		}
		private void set_column_value(
			final Sheet sh, 
			int page, 
			final Mesg msg
		) {
			//put time stamp~~
			get_cell(sh, page*10+0, row_idx[page]).setCellValue(msg.getTickText(""));
			String txt = msg.getText();
			String[] val = txt
				.substring(txt.indexOf(":")+1)
				.split(",");
			//put other records~~
			int col = 0, sub_col = 3;
			for(int i=0; i<val.length; i++) {
				//According unit, set column index~~~
				//How to convert unit?
				String v = val[i].trim();				
				if(v.contains("A/s")==true) {
					col = 6;
					v = v.substring(0,v.length()-3).trim();
				}else if(v.contains("kA")==true) {
					col = 7;
					v = v.substring(0,v.length()-2).trim();
				}else if(v.contains("V")==true) {
					col = 0;
					v = v.substring(0,v.length()-1).trim();
				}else if(v.contains("A")==true) {
					col = 1;//don'y mix with 'A/s' and 'kA'
					v = v.substring(0,v.length()-1).trim();
				}else if(v.contains("W")==true) {
					col = 2;
					v = v.substring(0,v.length()-1).trim();
				}else if(v.contains("sccm")==true) {
					col = sub_col; sub_col+=1;
					v = v.substring(0,v.length()-4).trim();
				}else {
					Misc.logw("未知的數據（%s）", v);
					continue;
				}
				get_cell(sh, page*10+1+col, row_idx[page]).setCellValue(v);
			}
			row_idx[page]+=1;
		}
		private void flush_action(final Sheet sh,final Mesg msg) {
			int row = sh.getLastRowNum() + 1;
			final String txt = msg.getText();
			if(
				txt.startsWith(StepKindler.TAG_KINDLE)==true ||
				txt.startsWith(StepWatcher.TAG_WATCH)==true
			) {				
				return;
			}
			//put time stamp~~
			get_cell(sh, 0, row).setCellValue(msg.getTickText(""));
			//put action text~~~
			get_cell(sh, 1, row).setCellValue(txt);
		}
		private void export_book(final Workbook wb) throws IOException {
			updateMessage("匯出試算表");
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
			if(rr==null){ rr = sheet.createRow(yy); }
			Cell cc = rr.getCell(xx);
			if(cc==null){ cc = rr.createCell(xx); }
			return cc;
		}
		/*private void prepare_book() {
			DataFormat fmt;
			CellStyle styl_norm, styl_upper, styl_lower;
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
		}*/
	};

	public void show_progress() {
		/*Badge node;
		if(badge.isPresent()==false) {
			node = new Badge();
			badge = Optional.of(node);
		}else {
			node = badge.get();
		}
		lay_status.getChildren().add(node);		
		AnchorPane.setTopAnchor(node, 0.);
		AnchorPane.setRightAnchor(node, 0.);*/
		LogStream.getInstance().usePool(true);
	}
	
	public void done_progress() {
		LogStream.getInstance().usePool(false);
		final PanBase pan = PanBase.self(this);
		final Task<?> tsk = new Dumping();
		//TODO: how to remove badge??
		/*if(badge.isPresent()==true) {
			Badge node = badge.get();
			node.bar.progressProperty().bind(tsk.progressProperty());
			node.txt.textProperty().bind(tsk.messageProperty());
			tsk.setOnSucceeded(e->{
				lay_status.getChildren().remove(node);
			});
		}*/
		pan.notifyTask("輸出紀錄",tsk);
	}
	//-----------------------------------------------//
	
	private final Tile gag[] = new Tile[9];

	public static Optional<Tile> gag_thick = Optional.empty();
	
	private Pane layout_gauge() {
		
		//gauge for DCG-100
		gag[0] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("電壓")
			.unit("Volt")
			.maxValue(1000)			
			.build();
		gag[0].setDecimals(1);
		gag[0].setId("v_volt");
						
		gag[1] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("電流")			
			.unit("Amp")
			.maxValue(10)
			.build();
		gag[1].setDecimals(2);
		gag[1].setId("g_amps");
					
		gag[2] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("功率")			
			.unit("Watt")
			.maxValue(5000)				
			.build();
		gag[2].setId("g_watt");
					
		gag[3] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("焦耳")			
			.unit("Joules")
			.maxValue(5000)			
			.build();
		gag[3].setId("g_joul");
				
		//gauge for SQM-160
		gag[4] = TileBuilder.create()			
			.skinType(SkinType.GAUGE)
			.title("薄膜速率")	
			.build();
		gag[4].setDecimals(3);
		gag[4].setId("g_rate");
			
		gag[5] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("薄膜厚度")
			.build();
		gag[5].setDecimals(3);
		gag[5].setId("g_high");
		gag_thick = Optional.of(gag[5]);
		
		gag[6] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("Ar")
			.unit("SCCM")
			.build();
		gag[6].setDecimals(2);
		gag[7] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("O2")
			.unit("SCCM")
			.build();
		gag[7].setDecimals(2);
		gag[8] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("N2")
			.unit("SCCM")
			.build();
		gag[8].setDecimals(2);

		for(Tile obj:gag) {
			obj.setMaxSize(178.,178.);
		}
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addRow(0, gag[0],gag[1],gag[2]);
		lay.addRow(1, gag[3],gag[4],gag[5]);
		lay.addRow(2, gag[6],gag[7],gag[8]);
		return lay;
	}
	
	public void bindProperty(final DevDCG100 dev) {		
		gag[0].valueProperty().bind(dev.volt);
		gag[1].valueProperty().bind(dev.amps);
		gag[2].valueProperty().bind(dev.watt);
		gag[3].valueProperty().bind(dev.joul);
	}
	public void bindProperty(final DevSQM160 dev) {
		
		gag[4].valueProperty().bind(dev.rate[0]);
		gag[4].setMinValue(dev.rateRange[0].doubleValue());
		set_max_limit(
			gag[4],
			dev.rateRange[1].doubleValue()
		);
		gag[4].setUnit(dev.unitRate.get());
		
		gag[5].valueProperty().bind(dev.thick[0]);
		gag[5].setMinValue(dev.thickRange[0].doubleValue());
		set_max_limit(
			gag[5],
			dev.thickRange[1].doubleValue()
		);
		gag[5].setUnit(dev.unitThick.get());
	}
	public void bindProperty(final ModCouple dev) {
		gag[6].valueProperty().bind(dev.PV_FlowAr);
		gag[7].valueProperty().bind(dev.PV_FlowO2);
		gag[8].valueProperty().bind(dev.PV_FlowN2);
	}
	
	private void set_max_limit(
		final Tile obj,
		final double val
	) {
		obj.setMaxValue(val);
		obj.setThreshold(val-0.7);
	}
}
