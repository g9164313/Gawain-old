package narl.itrc;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

public class ChartLine extends LineChart<Number, Number> {

	public ChartLine(){
		super(new NumberAxis(), new NumberAxis());
		setAnimated(false);
		setCreateSymbols(false);
		
		final MenuItem itm11 = new MenuItem("X軸");
		itm11.setOnAction(event->axis_setting_show(getXAxis()));
		final MenuItem itm12 = new MenuItem("Y軸");
		itm12.setOnAction(event->axis_setting_show(getYAxis()));
		final Menu itm1 = new Menu("設定邊界", null, itm11, itm12);

		final ToggleGroup itm20 = new ToggleGroup();
		final RadioMenuItem itm21 = new RadioMenuItem("無");
		itm21.setToggleGroup(itm20);
		final RadioMenuItem itm22 = new RadioMenuItem("最大值");//max(abs())
		itm22.setToggleGroup(itm20);
		final RadioMenuItem itm23 = new RadioMenuItem("平均值");//sum(abs())
		itm23.setToggleGroup(itm20);
		final RadioMenuItem itm24 = new RadioMenuItem("均方值");//sqrt(sum(pow2()))
		itm24.setToggleGroup(itm20);
		itm20.selectToggle(itm21);
		final Menu itm2 = new Menu("正規化", null, itm21, itm22, itm23, itm24);
		
		final Menu itm3 = new Menu("刪除標記");
		
		final MenuItem itm4 = new MenuItem("新增標記");
		itm4.setOnAction(event->mark_setting_show(itm3));
		
		final ContextMenu menu = new ContextMenu(itm1, itm2, itm3, itm4);

		setOnContextMenuRequested(event->{
			menu.show(getParent().getScene().getWindow());
		});
	}
	
	private void mark_setting_show(final Menu menu){
		
		TextInputDialog dia = new TextInputDialog();
		dia.setTitle("標記");
		dia.setHeaderText(null);
		dia.setContentText("X軸位置：");
		
		final Optional<String> opt = dia.showAndWait();
		
		float xx = Float.valueOf(opt.get());
		
		final int idx = menu.getItems().size();
		
		final String name = String.format("標記-%d", idx);
		
		final XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
		series.setName(name);
		series.getData().add(new XYChart.Data<Number,Number>(xx, 0));
		series.getData().add(new XYChart.Data<Number,Number>(xx, 1));
		getData().add(1+idx, series);
		
		final MenuItem itm = new MenuItem(name);
		itm.setOnAction(event->{
			menu.getItems().remove(itm);
			getData().remove(series);
		});
		menu.getItems().add(itm);
	}
	
	private void axis_setting_show(final Axis<Number> axis){
		
		NumberAxis axs = (NumberAxis)axis;
		
		final BooleanProperty flag = axs.autoRangingProperty();
		
		final CheckBox box =new CheckBox("自動邊界");		
		box.selectedProperty().bind(flag);
		
		final TextField[] txt = {
			new TextField(),
			new TextField(),
			new TextField()
		};
		txt[0].setText(String.format("%.3f", axs.getUpperBound()));		
		txt[1].setText(String.format("%.3f", axs.getTickUnit()));
		txt[2].setText(String.format("%.3f", axs.getLowerBound()));
		for(int i=0; i<txt.length; i++){
			txt[i].disableProperty().bind(flag);
			txt[i].setPrefWidth(80);
		}

		final GridPane lay0 = new GridPane();
		lay0.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");	
		lay0.add(box, 0, 0, 2, 1);
		lay0.addRow(1, new Label("上限："), txt[0]);
		lay0.addRow(2, new Label("刻度："), txt[1]);
		lay0.addRow(3, new Label("下限："), txt[2]);
				
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setOnCloseRequest(event->{
			ButtonType res = dia.getResult();
			if(res==ButtonType.OK){
				axs.setUpperBound(Double.valueOf(txt[0].getText()));
				axs.setTickUnit  (Double.valueOf(txt[1].getText()));
				axs.setLowerBound(Double.valueOf(txt[2].getText()));
			}
		});
		dia.getDialogPane().setContent(lay0);
		dia.showAndWait();
	}
	
	
	public XYChart.Series<Number,Number> update(float head, float step, float[] value){
		if(getData().size()>=1){
			getData().remove(0);
		}
		XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();		
		for(int i=0; i<value.length; i++){			
			float xx = head + step * i;			
			float yy = value[i];			
			series.getData().add(
				new XYChart.Data<Number,Number>(xx,yy)
			);
		}		
		getData().add(0, series);		
		return series;
	}
	
	public ChartLine setRangeX(double lower, double upper){
		return setRangeX(lower,upper,(upper-lower)/10.);
	}	
	public ChartLine setRangeX(double lower, double upper, double tick){
		return set_range(
			(NumberAxis)getXAxis(), 
			lower,
			upper, 
			tick
		);
	}
	
	public ChartLine setRangeY(double lower, double upper){
		return setRangeY(lower,upper,(upper-lower)/10., true);
	}
	public ChartLine setRangeY(double lower, double upper, boolean norm){
		return setRangeY(lower,upper,(upper-lower)/10., norm);
	}
	public ChartLine setRangeY(double lower, double upper, double tick, boolean norm){
		//use_norm_axis.set(norm);
		return set_range(
			(NumberAxis)getYAxis(), 
			lower,
			upper, 
			tick
		);
	}

	private ChartLine set_range(
		final NumberAxis axs, 
		final double lower, 
		final double upper, 
		final double tick
	){
		axs.setAutoRanging(false);
		axs.setLowerBound(lower);
		axs.setUpperBound(upper);
		axs.setTickUnit(tick);
		return this;
	}
	
}
