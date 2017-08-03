package prj.refuge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;

public class WidMarkTable extends JFXTabPane {

	public WidMarkTable(String... name){		
		if(name.length==0){
			getChildren().add(new Label("無設定"));
			return;
		}
		sheet = new Sheet[name.length];
		for(int i=0; i<sheet.length; i++){
			sheet[i] = new Sheet(name[i]);			
			Tab tab = new Tab();
			tab.setText(name[i]);
			tab.setContent(sheet[i]);			
			getTabs().add(tab);
		}	
	}

	public Record addRecord(int idx){
		return sheet[idx].addRecord();
	}
	
	public void getSheetCurLoca(int idx, ArrayList<String> lst){
		lst.clear();
		int cnt = sheet[idx].layRecord.getChildren().size();
		for(int i=1; i<cnt; i++){
			Record rec = (Record)(sheet[idx].layRecord.getChildren().get(i));
			lst.add(rec.txtCurLoca.getText());
		}
	}
	
	public void getSheetCurRadi(int idx, ArrayList<Double> lst){
		lst.clear();
		int cnt = sheet[idx].layRecord.getChildren().size();
		for(int i=1; i<cnt; i++){
			Record rec = (Record)(sheet[idx].layRecord.getChildren().get(i));
			String val = rec.txtCurRadi.getText();
			lst.add(Double.valueOf(val));
		}
	}
	
	public void clearValue(int idx1, int idx2){
		sheet[idx1].getRecord(idx2).delValueAll();
	}
	
	public void setCurLoca(int idx1, int idx2, String txt){
		sheet[idx1].getRecord(idx2).setCurLoca(txt);
	}
	
	public void addValue(int idx1, int idx2, String txt){
		sheet[idx1].getRecord(idx2).addValue(txt);
	}
	
	public void updateValue(int idx1, int idx2){
		sheet[idx1].getRecord(idx2).update_info();
	}
	
	public int getSheetSize(){
		return sheet.length;
	}
	
	private class OptionLoad extends GridPane {
		public boolean flag = true;
		public int begCol = 1;
		public int endCol = 20;
	
		public OptionLoad(){
			getStyleClass().add("grid-medium");
			JFXCheckBox chkFlag = new JFXCheckBox("使用新距離");
			chkFlag.setOnAction(event->{
				flag = chkFlag.isSelected();
			});
			chkFlag.setSelected(flag);
			JFXTextField boxBegCol = new JFXTextField(""+begCol);
			boxBegCol.textProperty().addListener(event->{
				try{
					begCol = Integer.valueOf(boxBegCol.getText());					
				}catch(NumberFormatException e){
					begCol = 1;
				}			
			});
			boxBegCol.setPrefWidth(30);
			JFXTextField boxEndCol = new JFXTextField(""+endCol);
			boxEndCol.textProperty().addListener(event->{
				try{
					endCol = Integer.valueOf(boxEndCol.getText());					
				}catch(NumberFormatException e){
					endCol = 20;
				}			
			});
			boxEndCol.setPrefWidth(30);
			add(chkFlag,0,0,2,1);
			addRow(1, new Label("起始欄位"), boxBegCol);
			addRow(2, new Label("最後欄位"), boxEndCol);
		}
	};
	
	public void loadExcel(File fs){
		int locaRow = 7;
		int begCol = 1;
		int endCol = 20;
		//ask option???
		Alert dia = new Alert(AlertType.INFORMATION);
		final OptionLoad opt = new OptionLoad();
		dia.getDialogPane().setContent(opt);
		dia.showAndWait();
		if(opt.flag==true){
			locaRow = 7;
		}else{
			locaRow = 6;
		}
		begCol = opt.begCol;
		endCol = opt.endCol;
		try {
			DataFormatter fmt = new DataFormatter();
			HSSFWorkbook bok = new HSSFWorkbook(new FileInputStream(fs));
			FormulaEvaluator eval = bok.getCreationHelper().createFormulaEvaluator();
			for(int s=0; s<3; s++){
				sheet[s].delRecord();
				HSSFSheet sht = bok.getSheetAt(s);				
				for(int i=begCol; i<=endCol; i++){
					HSSFRow row = sht.getRow(locaRow);
					HSSFCell cel = row.getCell(i);
					Record rec = sheet[s].addRecord();
					double val;
					if(cel==null){
						rec.setCurLoca("-99.99");
					}else{
						val = cel.getNumericCellValue();
						rec.setCurLoca(String.format("%.4f",val));
					}
					cel = sht.getRow(4).getCell(i);
					if(cel!=null){
						rec.txtCurRadi.setText(fmt.formatCellValue(cel, eval));
					}
				}				
			}
			bok.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveExcel(File fs){
		Calendar cc = Calendar.getInstance();
		String day = String.format(
			"%d/%02d/%02d",
			cc.get(Calendar.YEAR),
			cc.get(Calendar.MONTH)+1,
			cc.get(Calendar.DATE)
		);
		String title1 = Gawain.prop.getProperty("title1","ATOMTEX AT5350/1(s/n:10527)");
		String title2 = Gawain.prop.getProperty("title2","PTW TM32002(s/n: 0298)");
		String title3 = Gawain.prop.getProperty("title3","NRSL-104140，2015/05/15，INER");
		try {
			HSSFWorkbook bok = new HSSFWorkbook();
			DataFormat fmt_num = bok.createDataFormat();
			
			CellStyle sty_num = bok.createCellStyle();
			sty_num.setDataFormat(fmt_num.getFormat("0.0000"));
			
			CellStyle sty_loca = bok.createCellStyle();
			sty_loca.setDataFormat(fmt_num.getFormat("0.00"));
			
			CellStyle sty_cnt = bok.createCellStyle();
			sty_cnt.setDataFormat(fmt_num.getFormat("00"));
			
			//save every record data....
			HSSFRow[] row = null;
			HSSFCell cel = null;
			for(int ss=0; ss<3; ss++){
				HSSFSheet sht = bok.createSheet(sheet[ss].name);
				row = new HSSFRow[37];
				for(int rr=0; rr<37; rr++){
					row[rr] = sht.createRow(rr);
				}
				row[0 ].createCell(0 ).setCellValue("輻射偵檢儀校正實驗室輻射場強度標定紀錄表");
				row[0 ].createCell(7 ).setCellValue("("+day+")");
				row[0 ].createCell(18).setCellValue("HPCLP-01-02");
				row[1 ].createCell(1 ).setCellValue("電量計：");
				row[1 ].createCell(2 ).setCellValue(title1);
				row[1 ].createCell(5 ).setCellValue("游離腔：");
				row[1 ].createCell(6 ).setCellValue(title2);
				row[1 ].createCell(9 ).setCellValue("校正報告：");
				row[1 ].createCell(10).setCellValue("（"+title3+"）90");
				row[2 ].createCell(1 ).setCellValue(sheet[ss].name+"標定");
				row[2 ].createCell(2 ).setCellValue("標定日期：");
				row[2 ].createCell(3 ).setCellValue(day);
				row[36].createCell(1 ).setCellValue("品質負責人：");
				row[36].createCell(5 ).setCellValue("操作人：");
				
				row[4 ].createCell(0 ).setCellValue("now μSv/hr");
				row[5 ].createCell(0 ).setCellValue("1 y after μSv/hr");
				row[6 ].createCell(0 ).setCellValue("距離 (cm)");
				row[7 ].createCell(0 ).setCellValue("新距離(cm)");
				row[8 ].createCell(0 ).setCellValue("個數 (n)");
				row[9 ].createCell(0 ).setCellValue("平均 (μSv/min)");
				row[10].createCell(0 ).setCellValue("Sigma");
				row[11].createCell(0 ).setCellValue("%Sigma");
				row[12].createCell(0 ).setCellValue("計讀值 (μSv/min)");
				
				int cnt = sheet[ss].layRecord.getChildren().size();				
				for(int i=1; i<cnt; i++){
					Record rec = (Record)(sheet[ss].layRecord.getChildren().get(i));
										
					row[3 ].createCell(i ).setCellValue(""+i);
					
					char col = (char)((int)'A' + i);
					
					cel = row[4 ].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellFormula(String.format("%C10*60",col));

					cel = row[5 ].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellFormula(String.format("%C5*0.977",col));
					
					cel = row[6 ].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellValue(Double.valueOf(rec.txtCurLoca.getText()));
					
					cel = row[7 ].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellFormula(String.format("((%C7+90)*0.988)-90",col));
					
					cel = row[8 ].createCell(i );
					cel.setCellStyle(sty_cnt);
					cel.setCellValue(Double.valueOf(rec.txtInfo[ROW_COUNT].getText()));
					
					cel = row[9 ].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellFormula(String.format("AVERAGE(%C13:%C32)",col,col));
					
					cel = row[10].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellFormula(String.format("STDEV(%C13:%C32)",col,col));
					
					cel = row[11].createCell(i );
					cel.setCellStyle(sty_num);
					cel.setCellFormula(String.format("(%C11/%C10)*100",col,col));
					
					for(int j=0; j<rec.lstBox.size(); j++){
						String txt = rec.lstBox.get(j).getText();
						try{							
							double val = Double.valueOf(txt);						
							row[12+j].createCell(i).setCellValue(val);
						}catch(NumberFormatException e){
							//we fail, just keep the original information~~~
							row[12+j].createCell(i).setCellValue(txt);
						}
					}					
				}
			}
			//append a transform table...
			HSSFSheet sht = bok.createSheet("標定表");
			int max_row = 0;
			for(int ss=0; ss<3; ss++){
				int cnt = sheet[ss].layRecord.getChildren().size();
				if(cnt>max_row){
					max_row = cnt;
				}				
			}			
			row = new HSSFRow[max_row+10];
			for(int rr=0; rr<row.length; rr++){
				row[rr] = sht.createRow(rr);
			}
			for(int ss=0; ss<3; ss++){
				
				int cnt = sheet[ss].layRecord.getChildren().size();
				
				cel = row[4].createCell(ss*2+0);
				cel.setCellValue(sheet[ss].name);
				
				for(int i=1; i<cnt; i++){
					
					Record rec = (Record)(sheet[ss].layRecord.getChildren().get(i));
					
					cel = row[4+i].createCell(ss*2+0);
					cel.setCellStyle(sty_loca);
					cel.setCellValue(Double.valueOf(rec.txtCurLoca.getText()));					
					
					cel = row[4+i].createCell(ss*2+1);
					cel.setCellStyle(sty_num);
					cel.setCellValue(Double.valueOf(rec.txtInfo[ROW_AVERAGE].getText()));
					/*String txt = rec.txtCurRadi.getText();					
					try{
						double val = Double.valueOf(txt);
						cel.setCellValue(val/60.);
					}catch(NumberFormatException e){
						cel.setCellValue(txt);
					}*/
				}
			}
			bok.write(fs);
			bok.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static final int ROW_TITLE   = 0;
	private static final int ROW_CUR_RADI= 1;//let user key in this value~~~
	private static final int ROW_NXT_RADI= 2;
	private static final int ROW_CUR_LOCA= 3;
	private static final int ROW_NXT_LOCA= 4;
	private static final int ROW_COUNT   = 5;
	private static final int ROW_AVERAGE = 6;
	private static final int ROW_STDDEV_1= 7;
	private static final int ROW_STDDEV_2= 8;
	private static final int ROW_INFO_MAX= 9;

	private Sheet[] sheet;
	
	public class Sheet extends ScrollPane {
		public String name;
		
		public Sheet(String name){
			this.name = name;
			setHbarPolicy(ScrollBarPolicy.ALWAYS);
			setVbarPolicy(ScrollBarPolicy.ALWAYS);
			
			Button btnAdd = new Button("+");
			btnAdd.setOnAction(e->{
				addRecord();
			});
			Button btnDel = new Button("-");
			btnDel.setOnAction(e->{
				delRecord(-1);
			});
			
			HBox layCtrl = new HBox();
			layCtrl.setStyle("-fx-spacing: 13;");
			layCtrl.getChildren().addAll(btnAdd, btnDel);
			
			VBox layTitle = new VBox();
			layTitle.getStyleClass().add("table-col-vbox");
			layTitle.getChildren().addAll(
				layCtrl,
				new Label("now μSv/hr"),
				new Label("1 y after μSv/hr"),
				new Label("距離 (cm)"),
				new Label("新距離(cm)"),
				new Label("個數 (n)"),
				new Label("平均 (μSv/min)"),
				new Label("Sigma"),
				new Label("%Sigma"),
				new Label("計讀值 (μSv/min)")
			);
			layRecord.getStyleClass().add("table-row-hbox");
			layRecord.getChildren().add(layTitle);
			
			setPrefSize(700, 700);
			setContent(layRecord);
		}
		
		private HBox layRecord = new HBox();
		
		public Record addRecord(){
			int cnt = layRecord.getChildren().size();//just one-base
			Record rec = new Record(cnt);
			layRecord.getChildren().add(rec);
			return rec;
		}
		
		public void delRecord(){
			int cnt = layRecord.getChildren().size();
			if(cnt==1){
				return;
			}
			layRecord.getChildren().remove(1, cnt);
		}
		
		public void delRecord(int col){
			int cnt = layRecord.getChildren().size();
			if(cnt==1){
				return;
			}else if(col<0){
				col = cnt -1;
			}
			layRecord.getChildren().remove(col);
			cnt = layRecord.getChildren().size();
			//re-index again~~~
			for(int i=1; i<cnt; i++){
				Record rec = (Record)(layRecord.getChildren().get(i));
				rec.txtInfo[0].setText(String.format("%d", i));
			}
		}
		
		public Record getRecord(int idx){
			return (Record)(layRecord.getChildren().get(idx+1));
		}
	};
	
	public class Record extends GridPane {
		
		private TextField txtCurRadi;
		private TextField txtCurLoca;
		private Label[] txtInfo = new Label[ROW_INFO_MAX];		
		private ArrayList<JFXTextField> lstBox = new ArrayList<JFXTextField>();
		
		public Record(int col){
			getStyleClass().add("table-col-grid");
			
			txtInfo[ROW_TITLE] = new Label();//this is special item~~~~
			txtInfo[ROW_TITLE].setStyle("-fx-alignment: center;");
			txtInfo[ROW_TITLE].setText(String.format("%d", col));			
			add(txtInfo[ROW_TITLE], 1, 0, 3, 1);
			GridPane.setValignment(txtInfo[ROW_TITLE], VPos.CENTER);
			
			for(int i=1; i<txtInfo.length; i++){
				Node itm = null;
				if(i==ROW_CUR_RADI){
					itm = txtCurRadi = new JFXTextField();
					txtCurRadi.setText("0.0000");
					txtCurRadi.setPrefWidth(80);
					txtCurRadi.textProperty().addListener(event->{
						setCurRadi(null);
					});
				}else if(i==ROW_CUR_LOCA){
					itm = txtCurLoca = new JFXTextField();
					txtCurLoca.setText("0.0000");
					txtCurLoca.setPrefWidth(80);
					txtCurLoca.textProperty().addListener(event->{
						setCurLoca(null);
					});
				}else{
					itm = txtInfo[i] = new Label();					
				}
				GridPane.setFillWidth(itm, true);
				GridPane.setHgrow(itm, Priority.ALWAYS);
				add(itm, 0, i, 3, 1);
			}
			txtInfo[ROW_NXT_RADI].setText("0.0000");
			//skip ROW_CUR_LOCA, it is a input field
			txtInfo[ROW_NXT_LOCA].setText("0.0000");
			txtInfo[ROW_COUNT   ].setText("0");
			txtInfo[ROW_AVERAGE ].setText("0.0000");
			txtInfo[ROW_STDDEV_1].setText("0.0000");
			txtInfo[ROW_STDDEV_2].setText("0.0000");
			
			setOnMouseEntered(e->{
				setStyle("-fx-border-color: black;-fx-padding: 0 7 0 7;");
			});		
			setOnMouseExited(e->{
				setStyle("");
			});
		}
		
		public int addValue(String... val){
			int off = ROW_INFO_MAX + lstBox.size();			
			for(int i=0; i<val.length; i++){
				JFXTextField box = new JFXTextField();
				box.setPrefWidth(70);
				box.setText(val[i]);
				box.setOnAction(e->update_info());
				lstBox.add(box);
				add(box, 0, off+i, 3, 1);
			}			
			return update_info();
		}
		
		public void delValue(){
			int cnt = lstBox.size();
			if(cnt==0){
				return;
			}
			int idx = cnt - 1;//the last one~~~
			getChildren().remove(lstBox.get(idx));
			lstBox.remove(idx);
			update_info();
		}
		
		public void delValueAll(){
			int cnt = lstBox.size();
			if(cnt==0){
				return;
			}
			getChildren().remove(ROW_INFO_MAX,ROW_INFO_MAX+cnt);
			lstBox.clear();
			update_info();
		}
		
		public Record setCurRadi(String val){
			if(val!=null){
				txtCurRadi.setText(val);				
			}else{
				val = txtCurRadi.getText().trim();
			}			
			try{
				double _v = Double.valueOf(val);
				_v =_v*0.977;
				if(_v<=0.){
					txtInfo[ROW_NXT_RADI].setText("0.0000");
				}else{
					txtInfo[ROW_NXT_RADI].setText(String.format("%.4f", _v));
				}				
			}catch(NumberFormatException e){
				//System.out.println("invalid numner:"+loca_cm);
			}
			return this;
		}
		
		/**
		 * set the current location.
		 * @param loca - unit is 'cm'
		 */
		public Record setCurLoca(String val){
			if(val!=null){
				txtCurLoca.setText(val);				
			}else{
				val = txtCurLoca.getText().trim();
			}			
			try{
				double _v = Double.valueOf(val);
				_v =((_v+90.)*0.988)-90.;
				if(_v<=0.){
					txtInfo[ROW_NXT_LOCA].setText("0.0000");
				}else{
					txtInfo[ROW_NXT_LOCA].setText(String.format("%.4f", _v));
				}				
			}catch(NumberFormatException e){
				//System.out.println("invalid numner:"+loca_cm);
			}
			return this;
		}
		
		private int update_info(){
			ArrayList<Double> lstVal = new  ArrayList<Double>();
			for(TextField box:lstBox){
				String txt = box.getText();
				try{
					lstVal.add(Double.valueOf(txt));
				}catch(NumberFormatException e){
					System.out.println("invalid numner:"+txt);
				}
			}
			if(lstVal.size()==0){
				//no data???
				txtCurRadi.setText("0.000");
				txtInfo[ROW_NXT_RADI].setText("0.000");
				txtInfo[ROW_COUNT   ].setText("0");
				txtInfo[ROW_AVERAGE ].setText("0.000");			
				txtInfo[ROW_STDDEV_1].setText("0.000");
				txtInfo[ROW_STDDEV_2].setText("0.000");
				return 0;
			}
			
			double cnt = lstVal.size();
			double avg = 0.;
			for(double v:lstVal){
				avg = avg + v;
			}
			avg = avg / cnt;
			
			double dev = 0.;
			for(double v:lstVal){
				dev = dev + (v-avg)*(v-avg);
			}
			dev = Math.sqrt(dev/(cnt-1.));
			
			txtCurRadi.setText(String.format("%.4f", avg*60.));
			txtInfo[ROW_NXT_RADI].setText(String.format("%.4f", avg*60.*0.97716));
			txtInfo[ROW_COUNT   ].setText(String.format("%d"  , lstVal.size()));
			txtInfo[ROW_AVERAGE ].setText(String.format("%.4f", avg));			
			txtInfo[ROW_STDDEV_1].setText(String.format("%.4f", dev));
			if(avg==0.){
				txtInfo[ROW_STDDEV_2].setText("???");
			}else{
				txtInfo[ROW_STDDEV_2].setText(String.format("%.3f", (dev/avg)*100.));
			}
			return lstVal.size();
		}
	};
}
