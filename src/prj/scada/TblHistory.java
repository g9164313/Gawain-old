package prj.scada;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXToggleButton;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import narl.itrc.DevModbus;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class TblHistory extends VBox {

	private final TableView<Record> table = new TableView<Record>();
	
	private final JFXToggleButton starter = new JFXToggleButton();
	
	private final ToggleGroup period = new ToggleGroup();
	
	private Optional<Timeline> time = Optional.empty();
	
	public TblHistory(final PanBase pan) {
		
		init_table();
		
		final JFXRadioButton[] rad = {
			new JFXRadioButton(" 1 秒"),
			new JFXRadioButton("10 秒"),
			new JFXRadioButton("30 秒"),
		};
		rad[0].setUserData(Duration.seconds(1.));
		rad[1].setUserData(Duration.seconds(10.));
		rad[2].setUserData(Duration.seconds(30.));
		for(int i=0; i<rad.length; i++) {
			rad[i].setToggleGroup(period);
		}
		period.selectToggle(rad[0]);
		
		starter.setText("紀錄");
		starter.setOnAction(e->{
			if(time.isPresent()==false) {
				startRecord();
			}else {
				stopRecord();
			}
		});
		
		final JFXButton btnDump = new JFXButton("匯出");
		btnDump.disableProperty().bind(starter.selectedProperty());
		btnDump.getStyleClass().add("btn-raised-1");
		btnDump.setMinWidth(120.);
		btnDump.setOnAction(e->{
			String name = pan.saveAsFile("record.xlsx");
			if(name.length()==0) {
				return;
			}
			pan.notifyTask(dumpRecord(name));
		});
		
		final HBox lay0 = new HBox(); 
		lay0.getStyleClass().addAll("box-pad");
		lay0.setAlignment(Pos.BASELINE_LEFT);
		lay0.getChildren().addAll(
			starter,
			new Label("週期:"), 
			rad[0], rad[1], rad[2],
			new Label("    "),
			btnDump
		);
				
		getChildren().addAll(table,lay0);
	}
	
	public void startRecord() {Misc.exec_gui(()->{
		
		if(time.isPresent()==true) {
			return;
		}
		table.getItems().clear();
		
		Object obj = period.getSelectedToggle().getUserData();
		final KeyFrame kfm = new KeyFrame(
			(Duration)obj, e->{				
			//simulation();	
			Record rec = new Record(vals);
			table.getItems().add(rec);
			table.scrollTo(rec);
		});		
		Timeline tt = new Timeline(kfm);
		tt.setCycleCount(Animation.INDEFINITE);
		tt.play();
		time = Optional.of(tt);
		//time.getKeyFrames().get(0).getOnFinished().handle(null);
		
		starter.setSelected(true);
	});}
	public void stopRecord() {Misc.exec_gui(()->{
		
		if(time.isPresent()==false) {
			return;
		}
		time.get().stop();
		time = Optional.empty();
		
		starter.setSelected(false);
	});}
	//--------------------------------//
	
	@SuppressWarnings("unchecked")
	private void init_table() {
		
		final TableColumn<Record,String> col0 = new TableColumn<>("時間");
		final TableColumn<Record,String> col1 = new TableColumn<>("電壓");
		final TableColumn<Record,String> col2 = new TableColumn<>("電流");
		final TableColumn<Record,String> col3 = new TableColumn<>("功率");
		//final TableColumn<Record,String> col4 = new TableColumn<>("焦耳"),
		final TableColumn<Record,String> col5 = new TableColumn<>("速率");
		final TableColumn<Record,String> col6 = new TableColumn<>("厚度");

		col0.setCellValueFactory(new PropertyValueFactory<Record,String>("stmp"));
		col1.setCellValueFactory(new PropertyValueFactory<Record,String>("volt"));
		col2.setCellValueFactory(new PropertyValueFactory<Record,String>("amps"));
		col3.setCellValueFactory(new PropertyValueFactory<Record,String>("watt"));
		//col4.setCellValueFactory(new PropertyValueFactory<Record,String>("joul"));
		col5.setCellValueFactory(new PropertyValueFactory<Record,String>("rate"));
		col6.setCellValueFactory(new PropertyValueFactory<Record,String>("high"));
		
		DoubleBinding col_w = widthProperty().subtract(col0.prefWidthProperty()).divide(5f);
		col0.setPrefWidth(100);
		col1.prefWidthProperty().bind(col_w);
		col2.prefWidthProperty().bind(col_w);
		col3.prefWidthProperty().bind(col_w);
		col5.prefWidthProperty().bind(col_w);
		col6.prefWidthProperty().bind(col_w);
		
		final TableColumn<Record,String> colA = new TableColumn<>("總輸出");		
		final TableColumn<Record,String> colB = new TableColumn<>("薄膜");
		colA.getColumns().addAll(col1,col2,col3);
		colB.getColumns().addAll(col5,col6);
		
		table.setEditable(false);
		table.getColumns().addAll(col0,colA,colB);
		
		VBox.setVgrow(table, Priority.ALWAYS);
	}
	
	private FloatProperty[] vals = null;
	
	public void bindProperty(final FloatProperty... values) {
		vals = values;
	}	
	public void bindProperty(
		final DevModbus coup,
		final DevSQM160 sqm1
	) {
		vals = new FloatProperty[6];
		vals[0] = new SimpleFloatProperty();
		vals[1] = new SimpleFloatProperty();
		vals[2] = new SimpleFloatProperty();
		vals[3] = new SimpleFloatProperty();

		IntegerProperty prop;
		prop = coup.register(8001);
		if(prop!=null) {
			vals[0].bind(prop.multiply(0.20f));
		}
		prop = coup.register(8002);
		if(prop!=null) {
			vals[2].bind(prop.multiply(1.06f));
		}
		vals[1].bind(vals[2].divide(vals[0].add(Float.MIN_VALUE)));
		vals[4] = sqm1.rate[0];
		vals[5] = sqm1.high[0];
	}
	
	public Task<?> dumpRecord(final String name) {
		
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				
				XSSFWorkbook workbook = new XSSFWorkbook();
		        XSSFSheet sheet = workbook.createSheet("Datatypes in Java");
				
		        //create title~~~~
		        Row row = sheet.createRow(0);
		        row.createCell(0).setCellValue("時間");					
				row.createCell(1).setCellValue("電壓");
				row.createCell(2).setCellValue("電流");
				row.createCell(3).setCellValue("功率");
				row.createCell(4).setCellValue("焦耳");
				row.createCell(5).setCellValue("速率");
				row.createCell(6).setCellValue("厚度");
				
				ObservableList<Record> lst = table.getItems();
				for(int i=0; i<lst.size(); i++) {
					row = sheet.createRow(i+1);
					updateMessage(String.format("處理項目: %d/%d", i, lst.size()));
					Record itm = lst.get(i);
					row.createCell(0).setCellValue(itm.getStmp());					
					row.createCell(1).setCellValue(itm.getValue(1));
					row.createCell(2).setCellValue(itm.getValue(2));
					row.createCell(3).setCellValue(itm.getValue(3));
					row.createCell(4).setCellValue(itm.getValue(4));
					row.createCell(5).setCellValue(itm.getValue(5));
					row.createCell(6).setCellValue(itm.getValue(6));
				}
				
				try {
					updateMessage("匯出檔案中...");
		            workbook.write(new FileOutputStream(name));
		            workbook.close();
		        } catch (FileNotFoundException e) {
		            e.printStackTrace();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
				return null;
			}
		};
	}
}
