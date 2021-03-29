package prj.sputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXRadioButton;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.init.LogStream;
import narl.itrc.init.LogStream.Mesg;

public class LayLogger extends BorderPane {

	private final AnchorPane lay_status = new AnchorPane();
	
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
		lay_view.getStyleClass().addAll("box-pad");
		
		final JFXButton btn = new JFXButton("test~logger");
		btn.setOnAction(e->{
			//if(badge.isPresent()==false) {
			//	show_progress();
			//}else {
				done_progress();
			//}			
		});
		
		final HBox lay_ctrl = new HBox(r_n1,r_n2,btn);
		lay_ctrl.getStyleClass().addAll("box-pad");		
		AnchorPane.setTopAnchor(lay_ctrl, 0.);
		AnchorPane.setLeftAnchor(lay_ctrl, 0.);
		
		lay_status.getChildren().addAll(lay_ctrl);
		
		setCenter(lay_view);	
		setBottom(lay_status);
	}	
	//-----------------------------------------------//
	
	private class Badge extends HBox {
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
	
	private static final String pathLogStock = Gawain.pathSock+"監控紀錄"+File.separatorChar;
	private static final String pathLogCache = pathLogStock+"cache"+File.separatorChar;	
	
	private class Dumping extends Task<Void>{
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
			try {
				updateMessage("輸出文字");
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
		@Override
		protected Void call() throws Exception {
			check_path(pathLogStock);
			check_path(pathLogCache);
			//Mesg[] msg = LogStream.getInstance().flushPool();
			Mesg[] msg = (Mesg[]) Misc.deserializeFile(pathLogCache+"CEAA12-0001.obj");
			dump_text(msg);
			Workbook wb = new XSSFWorkbook();
			export_mesg(wb,msg);
			export_book(wb);			
			return null;
		}

		private void export_mesg(
			final Workbook wb,
			final Mesg[] msg
		) throws IOException {
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
			get_cell(sh, page+1, 0).setCellValue("電壓");
			get_cell(sh, page+2, 0).setCellValue("電流");
			get_cell(sh, page+3, 0).setCellValue("功率");
			get_cell(sh, page+4, 0).setCellValue("mfc-1");
			get_cell(sh, page+5, 0).setCellValue("mfc-2");
			get_cell(sh, page+6, 0).setCellValue("mfc-3");
			get_cell(sh, page+7, 0).setCellValue("速率");
			get_cell(sh, page+8, 0).setCellValue("厚度");
		}
		private void set_column_value(final Sheet sh,int page,final Mesg msg) {
			int row = sh.getLastRowNum() + 1;
			page = page * 10;
			//put time stamp~~
			get_cell(sh, page+0, row).setCellValue(msg.getTickText(""));
			String txt = msg.getText();
			String[] val = txt
				.substring(txt.indexOf(":")+1)
				.split(",");
			//put other records~~
			for(int i=0; i<val.length; i++) {
				get_cell(sh, page+i+1, row).setCellValue(val[i]);
			}
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
	
	private Optional<Badge> badge = Optional.empty();
	
	public void show_progress() {
		Badge node;
		if(badge.isPresent()==false) {
			node = new Badge();
			badge = Optional.of(node);
		}else {
			node = badge.get();
		}
		lay_status.getChildren().add(node);		
		AnchorPane.setTopAnchor(node, 0.);
		AnchorPane.setRightAnchor(node, 0.);
		LogStream.getInstance().usePool(true);
	}
	
	public void done_progress() {
		LogStream.getInstance().usePool(false);
		final PanBase pan = PanBase.self(this);
		final Task<?> tsk = new Dumping();
		if(badge.isPresent()==true) {
			Badge node = badge.get();
			node.bar.progressProperty().bind(tsk.progressProperty());
			node.txt.textProperty().bind(tsk.messageProperty());
			tsk.setOnSucceeded(e->lay_status.getChildren().remove(node));
		}
		pan.notifyTask("輸出紀錄",tsk);
	}
	//-----------------------------------------------//
	
	private final Tile gag[] = new Tile[9];
		
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

		final FlowPane lay = new FlowPane();
		lay.getStyleClass().addAll("box-pad");
		lay.setPrefWrapLength(800);
		lay.getChildren().addAll(gag);
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
		gag[5].setUnit(dev.unitHigh.get());
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
