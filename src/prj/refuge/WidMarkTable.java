package prj.refuge;

import java.util.ArrayList;

import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class WidMarkTable extends StackPane {

	public WidMarkTable(String... name){		
		if(name.length==0){
			getChildren().add(new Label("無設定"));
			return;
		}
		
		ScrollPane lay1 = new ScrollPane();
		lay1.setPrefSize(800, 800);
		lay1.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		lay1.setVbarPolicy(ScrollBarPolicy.ALWAYS);

		JFXTabPane lay2= new JFXTabPane();
		lay2.prefWidthProperty().bind(lay1.widthProperty());
		lay2.prefHeightProperty().bind(lay1.heightProperty().multiply(1.5));
		
		sheet = new Sheet[name.length];
		for(int i=0; i<sheet.length; i++){
			sheet[i] = new Sheet(name[i]);			
			Tab tab = new Tab();
			tab.setText(name[i]);
			tab.setContent(sheet[i]);
			lay2.getTabs().add(tab);
		}
		
		lay1.setContent(lay2);
		getChildren().add(lay1);		
	}

	public Record addRecord(int idx){
		return sheet[idx].addRecord();
	}
	
	private static final int ROW_INDEX   = 0;
	private static final int ROW_CUR_RADI= 1;
	private static final int ROW_NXT_RADI= 2;
	private static final int ROW_CUR_LOCA= 3;
	private static final int ROW_NXT_LOCA= 4;
	private static final int ROW_COUNT   = 5;
	private static final int ROW_AVERAGE = 6;
	private static final int ROW_STDDEV_1= 7;
	private static final int ROW_STDDEV_2= 8;
	private static final int ROW_INFO_MAX= 9;

	private Sheet[] sheet;
	
	public class Sheet extends HBox {
		
		public Sheet(String name){
			getStyleClass().add("table-row-hbox");

			HBox lay1 = new HBox();
			lay1.setStyle("-fx-spacing: 13;");
			Button btnAdd = new Button("+");
			btnAdd.setOnAction(e->{
				addRecord();
			});
			Button btnDel = new Button("-");
			btnDel.setOnAction(e->{
				delRecord(-1);
			});
			lay1.getChildren().addAll(btnAdd, btnDel);
			
			VBox lay2 = new VBox();
			lay2.getStyleClass().add("table-col-vbox");
			lay2.getChildren().addAll(
				lay1,
				new Label("now μSv/hr"),
				new Label("1 y after μSv/hr"),
				new Label("距離 (cm)"),
				new Label("新距離(cm)"),
				new Label("個數 (n)"),
				new Label("平均 (μSv/min)"),
				new Label("Sigma"),
				new Label("%Sigma"),
				new Label("計讀值")
			);
			getChildren().add(lay2);
		}
		
		public Record addRecord(){
			int cnt = getChildren().size();
			Record rec = new Record(cnt);
			getChildren().add(rec);
			return rec;
		}
		
		public void delRecord(int col){
			int cnt = getChildren().size();
			if(cnt==0){
				return;
			}
			if(col<=0){
				col = cnt;//the last one~~~
			}else if(cnt<col){
				return;
			}
			getChildren().remove(col);
			//re-index again~~~
			for(int i=0; i<cnt-1; i++){
				Record rec = (Record)(getChildren().get(i));
				rec.txtInfo[0].setText(String.format("%d", i));
			}
		}
	};
	
	public class Record extends GridPane {
		
		private Label[] txtInfo = new Label[9];
		private ArrayList<JFXTextField> lstBox = new ArrayList<JFXTextField>();
		
		public Record(int col){
			getStyleClass().add("table-col-grid");
			
			txtInfo[ROW_INDEX] = new Label();//this is special item~~~~
			txtInfo[ROW_INDEX].setPrefWidth(30);
			txtInfo[ROW_INDEX].setStyle("-fx-alignment: center;");
			add(txtInfo[ROW_INDEX], 1, 0, 1, 1);
			GridPane.setValignment(txtInfo[ROW_INDEX], VPos.CENTER);
			
			for(int i=1; i<txtInfo.length; i++){
				txtInfo[i] = new Label();
				GridPane.setFillWidth(txtInfo[i], true);
				add(txtInfo[i], 0, i, 3, 1);
			}
						
			Button btnAdd = new Button("+");
			btnAdd.setOnAction(e->{
				addValue("0.00");
			});
			add(btnAdd, 0, 0);
			Button btnDel = new Button("-");
			btnDel.setOnAction(e->{
				delValue();
			});
			add(btnDel, 2, 0);
			
			this.setOnMouseEntered(e->{
				setStyle("-fx-border-color: black;-fx-padding: 0 7 0 7;");
			});		
			setOnMouseExited(e->{
				setStyle("");
			});
			
			txtInfo[ROW_INDEX   ].setText(String.format("%d", col));
			txtInfo[ROW_CUR_RADI].setText("0.00");
			txtInfo[ROW_NXT_RADI].setText("0.00");
			txtInfo[ROW_CUR_LOCA].setText("0.00");
			txtInfo[ROW_NXT_LOCA].setText("0.00");
			txtInfo[ROW_COUNT   ].setText("0");
			txtInfo[ROW_AVERAGE ].setText("0.00");
			txtInfo[ROW_STDDEV_1].setText("0.00");
			txtInfo[ROW_STDDEV_2].setText("0.00");
		}
		
		public int addValue(String... val){
			int off = ROW_INFO_MAX + lstBox.size();			
			for(int i=0; i<val.length; i++){
				JFXTextField box = new JFXTextField();
				box.setPrefWidth(70);
				box.setText(val[i]);
				box.setOnAction(e->{
					update_info();
				});
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
			getChildren().remove(lstBox.get(cnt-1));
			lstBox.remove(cnt-1);
			update_info();
		}
		
		/**
		 * set the current location.
		 * @param loca - unit is 'cm'
		 */
		public void setCurLoca(String loca_cm){
			txtInfo[ROW_CUR_LOCA].setText(loca_cm);
			try{
				double val = Double.valueOf(loca_cm);
				val =((val+0.977)*0.988)-0.977;
				txtInfo[ROW_NXT_LOCA].setText(String.format("%.3f", val));
			}catch(NumberFormatException e){
				System.out.println("invalid numner:"+loca_cm);
			}
		}
		
		private int update_info(){
			ArrayList<Double> lstVal = new  ArrayList<Double>();
			for(JFXTextField box:lstBox){
				String txt = box.getText();
				try{
					lstVal.add(Double.valueOf(txt));
				}catch(NumberFormatException e){
					System.out.println("invalid numner:"+txt);
				}
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
			
			txtInfo[ROW_CUR_RADI].setText(String.format("%.3f", avg*60.));
			txtInfo[ROW_NXT_RADI].setText(String.format("%.3f", avg*60.*0.977));
			txtInfo[ROW_COUNT   ].setText(String.format("%d", lstVal.size()));
			txtInfo[ROW_AVERAGE ].setText(String.format("%.3f", avg));			
			txtInfo[ROW_STDDEV_1].setText(String.format("%.3f", dev));
			if(avg==0.){
				txtInfo[ROW_STDDEV_2].setText("???");
			}else{
				txtInfo[ROW_STDDEV_2].setText(String.format("%.3f", (dev/avg)*100.));
			}
			return lstVal.size();
		}
	};
}
