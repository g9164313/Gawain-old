package prj.scada;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.util.Duration;
import javafx.util.StringConverter;
import narl.itrc.DevModbus;

public class TblHistory extends TableView<Record> {

	@SuppressWarnings("unchecked")
	public TblHistory() {
		
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
		
		final JFXToggleButton tgl = new JFXToggleButton();
		flagTime = tgl.selectedProperty();
		flagTime.addListener(e->recording());
		
		final JFXComboBox<Duration> cmb = new JFXComboBox<>();
		cmb.setConverter(new StringConverter<Duration>() {
			@Override
			public Duration fromString(String arg0) {
				return null;
			}
			@Override
			public String toString(Duration arg0) {
				int val = (int)arg0.toSeconds();
				return String.format("%2d秒", val);
			}
		});
		cmb.getItems().addAll(
			Duration.seconds(1.),
			Duration.seconds(5.),
			Duration.seconds(10.),
			Duration.seconds(15.),
			Duration.seconds(20.),
			Duration.seconds(25.),
			Duration.seconds(30.)
		);
		flagDura = cmb.getSelectionModel();
		flagDura.select(0);
		
		final JFXButton[] btn = {
			new JFXButton("test-1"),
			new JFXButton("test-2"),
			new JFXButton("test-3"),
		};
		for(int i=0; i<btn.length; i++) {
			btn[i].setMaxWidth(Double.MAX_VALUE);
			btn[i].getStyleClass().add("btn-raised-1");
		}
		btn[0].setOnAction(e->{
			boolean flag = flagTime.get();
			flagTime.set(!flag);	
		});
		
		setEditable(false);
		getColumns().addAll(col0,colA,colB);
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
	
	private BooleanProperty flagTime = null;
	private SingleSelectionModel<Duration> flagDura = null;
	private Timeline time = null;
	
	private void recording() {
		
		if(flagTime.get()==false) {
			time.stop();
			time = null;
			return;
		}

		//do we need dump???
		getItems().clear();
		
		final KeyFrame kfm = new KeyFrame(
			flagDura.getSelectedItem(), e->{				
			//simulation();	
			Record rec = new Record(vals);
			getItems().add(rec);
			scrollTo(rec);
		});
		
		time = new Timeline(kfm);
		time.setCycleCount(Animation.INDEFINITE);
		time.play();
		time.getKeyFrames().get(0).getOnFinished().handle(null);
	} 
	
	private void press_timer(final boolean flag) {
		if(Application.isEventThread()==true) {
			flagTime.set(flag);
		}else {
			Application.invokeAndWait(()->flagTime.set(flag));
		}
	}
	
	public void startRecord() {
		press_timer(true);
	}
	
	public void stopRecord() {
		press_timer(false);
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
				
				ObservableList<Record> lst = getItems();
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
