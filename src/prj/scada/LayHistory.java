package prj.scada;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import javafx.util.Duration;
import javafx.util.StringConverter;

public class LayHistory extends BorderPane {

	private final TableView<Record> tbl = new TableView<>();
	
	@SuppressWarnings("unchecked")
	public LayHistory() {
		
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
			Duration.seconds(10.)
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

		tbl.setEditable(false);		
		tbl.getColumns().addAll(col0,colA,colB);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().addAll("box-pad");
		lay1.getChildren().addAll(
			tgl,cmb,
			btn[0],btn[1],btn[2]
		);
		
		getStyleClass().addAll("box-pad");
		setLeft(lay1);
		setCenter(tbl);
	}
	
	private FloatProperty[] vals = null;
	
	public LayHistory(final FloatProperty... values) {
		this();
		vals = values;
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
		tbl.getItems().clear();
		
		final KeyFrame kfm = new KeyFrame(
			flagDura.getSelectedItem(), e->{				
			//simulation();			
			tbl.getItems().add(new Record(vals));
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
	
	private static final SimpleDateFormat s_fmt = new SimpleDateFormat("HH:mm:ss");
	
	public static class Record {
		
		final StringProperty stmp = new SimpleStringProperty();
		final StringProperty volt = new SimpleStringProperty();
		final StringProperty amps = new SimpleStringProperty();
		final StringProperty watt = new SimpleStringProperty();
		final StringProperty joul = new SimpleStringProperty();
		final StringProperty rate = new SimpleStringProperty();
		final StringProperty high = new SimpleStringProperty();
		
		public String getStmp() { return stmp.get(); }
		public String getVolt() { return volt.get(); }
		public String getAmps() { return amps.get(); }
		public String getWatt() { return watt.get(); }
		public String getjoul() { return joul.get(); }
		public String getRate() { return rate.get(); }
		public String getHigh() { return high.get(); }

		public Record() {
			final Timestamp ss = new Timestamp(System.currentTimeMillis());
			stmp.set(s_fmt.format(ss));
		}
		
		public Record(final FloatProperty[] arg) {
			this();
			if(arg==null) {
				return;
			}
			volt.set(String.format("%.1f", arg[0].get()));
			amps.set(String.format("%.2f", arg[1].get()));
			watt.set(String.format("%.0f", arg[2].get()));
			joul.set(String.format("%.0f", arg[3].get()));
			rate.set(String.format("%.3f", arg[4].get()));
			high.set(String.format("%.3f", arg[5].get()));
		}
	};
}
