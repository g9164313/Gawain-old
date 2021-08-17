package prj.shelter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDatePicker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;


public class LayAbacus extends BorderPane
{
	//private final static DateTimeFormatter fmt_day = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	
	private final static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	
	public static class Mark implements Serializable {
		static final long serialVersionUID = 6061524050057088702L;
		final LocalDateTime date;
		final String loca;//HusiIO載台位置
		final String meas;//AT5350測量結果	
		public Mark(
			final String location, 
			final String measurement
		) {
			this(LocalDateTime.now(),location,measurement);
		}
		public Mark(
			final LocalDateTime markday_sec,
			final String location, 
			final String measurement
		) {
			date = markday_sec;
			loca = location;
			meas = measurement;
		}
		double getX(final String unit) {
			return Double.valueOf(UtilPhysical.convertScale(loca,unit));
		}
		double getY(final String unit) {
			if(meas.length()==0) {
				return 0f;
			}
			SummaryStatistics ss = new SummaryStatistics();
			for(String itm:meas.split(",")) {
				String txt = UtilPhysical.convertScale(itm,unit);
				if(txt.length()==0) {
					continue;
				}
				ss.addValue(Float.valueOf(txt));
			}
			return ss.getMean();
		}
	};
	
	public static class Model extends ArrayList<Mark> 
		implements Serializable, ParametricUnivariateFunction
	{
		static final long serialVersionUID = 7859312449362122438L;
		
		public static final String LOCA_UNIT = "cm";
		public static final String DOSE_UNIT = "μSv/hr";
		
		//Casium-137 --> 30.17 year --> 952 Msec
		//see WIKI: radioactive nuclides by half-life
		private long half_life_nuclide = 952000000;//unit is second
		
		private double getDecay(
			final LocalDateTime start,
			final LocalDateTime endof
		) {
			final long elapse = Duration.between(start,endof).toMillis() / 1000L;
			if(elapse<0) {
				return 1.;
			}
			return Math.exp((-0.693*elapse)/half_life_nuclide);
		}
		
		public Model fitting() {
			return fitting(LocalDateTime.now());
		}
		public Model fitting(final LocalDateTime endof) {
			
			final WeightedObservedPoints obv = new WeightedObservedPoints();			
			for(Mark mm:this) {
				double decay = getDecay(mm.date,endof);//RADIOACTIVE DECAY EQUATION				
				double xx = mm.getX(LOCA_UNIT);
				double yy = mm.getY(DOSE_UNIT) * decay; 
				obv.add(xx, yy);
			}
			
			double[] starter = {1, 1, 1, 1};		
			coeff = SimpleCurveFitter
				.create(this, starter)
				.fit(obv.toList());		
			return this;
		}
		
		public Description[] getLocaDesc(double... loca) {
			Description[] lst = new Description[loca.length];
			for(int i=0; i<loca.length; i++) {
				final double xx = loca[i];
				final double yy = getDose(xx);
				if(Double.isNaN(xx)==true) {
					continue;
				}
				final Description obj = new Description();
				obj.loca = String.format("%8.2f", xx);
				obj.dose = String.format("%8.2f", yy);
				lst[i] = obj; 
			}
			return lst;
		}
		public Description[] getDoseDesc(double... dose) {
			Description[] lst = new Description[dose.length];
			for(int i=0; i<dose.length; i++) {
				final double yy = dose[i];
				final double xx = getLoca(yy);
				if(Double.isNaN(xx)==true) {
					continue;
				}
				final Description obj = new Description();
				obj.loca = String.format("%8.2f", xx);
				obj.dose = String.format("%8.2f", yy);
				lst[i] = obj; 
			}
			return lst;
		}
		public Description[] getMarkDesc() {
			Description[] lst = new Description[size()];

			for(int i=0; i<size(); i++) {
				final Mark mark = get(i);
				
				final SummaryStatistics ss = new SummaryStatistics();
				for(String itm:mark.meas.split(",")) {
					String txt = UtilPhysical.convertScale(itm,"uSv/hr");
					if(txt.length()==0) {
						continue;
					}
					ss.addValue(Double.valueOf(txt));
				}
				
				final Description obj = new Description();
				obj.loca= UtilPhysical.convertScale(mark.loca,"cm");
				obj.dose= String.format("%8.2f", ss.getMean());
				obj.dev = String.format("%8.2f", ss.getStandardDeviation());
				obj.stmp= dtf.format(mark.date);
				lst[i] = obj; 
			}
			return lst;
		}
		
		private double[] coeff;

		public double getDose(double loca) {
			if(coeff==null) { return -1.; }
			return value(loca,coeff[0],coeff[1],coeff[2],coeff[3]);
		}
		public double[] getDose(double[] loca) {
			double[] v = new double[loca.length];
			for(int i=0; i<loca.length; i++) {
				if(coeff==null) {
					v[i] = -1.;
				}else {
					v[i] = value(loca[i],coeff[0],coeff[1],coeff[2],coeff[3]);
				}
			}
			return v;
		}
	
		public double getLoca(double dose) {
			return inv_value(dose);
		}
		public double[] getLoca(double[] dose) {
			double[] v = new double[dose.length];
			for(int i=0; i<dose.length; i++) {
				v[i] = inv_value(dose[i]);
			}
			return v;
		}
		
		public double inv_value(double y) {
			if(coeff==null) { return -1.; }
			final double a = coeff[0];
			final double b = coeff[1];
			final double c = coeff[2];
			final double d = coeff[3];
			return c * Math.pow(((a-d)/(y-d))-1.,1./b);
		}		
		@Override
		public double value(double x, double... parameters) {
			// 4PL - module function
			final double a = parameters[0];
			final double b = parameters[1];
			final double c = parameters[2];
			final double d = parameters[3];
			return d + ((a - d) / (1 + Math.pow(x / c, b)));
		}
		@Override
		public double[] gradient(double x, double... parameters) {
			// 4PL - model partial derivative
			final double a = parameters[0];
			final double b = parameters[1];
			final double c = parameters[2];
			final double d = parameters[3];
			
			double[] gradients = new double[4];
			double den = 1 + Math.pow(x / c, b);
			gradients[0] = 1 / den; //  Yes  a  Derivation 
			gradients[1] = -((a - d) * Math.pow(x / c, b) * Math.log(x / c)) / (den * den); //  Yes  b  Derivation 
			gradients[2] = (b * Math.pow(x / c, b - 1) * (x / (c * c)) * (a - d)) / (den * den); //  Yes  c  Derivation 
			gradients[3] = 1 - (1 / den); //  Yes  d  Derivation 
			return gradients;
		}
	};
	
	public static class Description {
		String stmp="----";
		String loca="0.", dose="0.", decay="0."; 
		String dev ="----", min="----", max="----";		
		public String getLocation() { return loca; }
		public String getDoseRate() { return dose; }
		public String getYearDecay() { return decay; }
		public String getDeviation(){ return dev; }
		public String getMinimum()  { return min; }
		public String getMaximum()  { return max; }
		public String getTimeStamp()  { return stmp; }
	};
	
	private Model[] model = {
		null, null, null	
	};//3Ci, 0.5Ci, 0.05Ci
	
	private static final String BY_LOCA = "依位置";
	private static final String BY_DOSE = "依劑量";
	private static final String BY_PLOT = "圖表顯示";
		
	private final ObjectProperty<LocalDate> endof;
	private final ReadOnlyObjectProperty<String> showby;
	private final ReadOnlyObjectProperty<String> activity;
	private ObservableList<Description> lst_fore;
	private ObservableList<Description> lst_mark;
	
	private void update_table() {
		lst_fore.clear();
		lst_mark.clear();
		
		Model mod = null;
		String opt_actv = activity.get();
		if(opt_actv.equals(DevHustIO.ACT_NAME_3Ci)) {
			mod = model[0];
		}else if(opt_actv.equals(DevHustIO.ACT_NAME_0_5Ci)) {
			mod = model[1];
		}else if(opt_actv.equals(DevHustIO.ACT_NAME_0_05Ci)) {
			mod = model[2];
		}
		if(mod==null) {
			return;
		}
		
		final double[] loca = {
			0.,10.,50.,100.,
			150.,200.,250.,
			300.,350.,400.,
			450.,500.	
		};
		final double[] dose = {
			   5.,
			  10.,  20.,  25.,  40.,  50.,  80.,
			 100., 200., 250., 400., 500., 800.,
			1000.,2000.,2500.,4000.,5000.,8000.,
			10000.,
		};
		lst_mark.addAll(mod.getMarkDesc());
		final String opy_showby = showby.get();
		if(opy_showby.equals(BY_LOCA)) {			
			lst_fore.addAll(mod.getLocaDesc(loca));
		}else if(opy_showby.equals(BY_DOSE)) {
			lst_fore.addAll(mod.getDoseDesc(dose));
		}
	}
	
	@SuppressWarnings("unchecked")
	public LayAbacus() {
		
		//final LineChart<Number,Number> cht = new LineChart<Number,Number>(
		//	new NumberAxis(),
		//	new NumberAxis()
		//);
		
		final JFXDatePicker pck_endofday = new JFXDatePicker();
		pck_endofday.setValue(LocalDate.now());
		pck_endofday.setPrefWidth(160.);
		pck_endofday.valueProperty().addListener((obv,oldVal,newVal)->{			
			update_table();
		});
		endof = pck_endofday.valueProperty();
		
		final JFXComboBox<String> cmb_showby = new JFXComboBox<String>();
		cmb_showby.getItems().addAll(
			BY_LOCA,
			BY_DOSE,
			BY_PLOT
		);
		cmb_showby.getSelectionModel().select(1);
		cmb_showby.setOnAction(e->update_table());
		showby = cmb_showby.getSelectionModel().selectedItemProperty();
		
		final JFXComboBox<String> cmb_activity = new JFXComboBox<String>();
		cmb_activity.getItems().addAll(
			DevHustIO.ACT_NAME_3Ci,
			DevHustIO.ACT_NAME_0_5Ci,
			DevHustIO.ACT_NAME_0_05Ci
		);
		cmb_activity.getSelectionModel().select(0);
		cmb_activity.setOnAction(e->update_table());
		activity = cmb_activity.getSelectionModel().selectedItemProperty();

		/*final JFXComboBox<String> cmb_doseunit = new JFXComboBox<String>();
		cmb_doseunit.getItems().addAll(
			Model.DOSE_UNIT
		);
		cmb_doseunit.getSelectionModel().select(0);		
		cmb_doseunit.setOnAction(e->update_table());
		dose_unit= cmb_doseunit.getSelectionModel().selectedItemProperty();*/
		
		final JFXButton btn = new JFXButton("test.1");
		btn.getStyleClass().add("btn-raised-1");
		btn.setMaxWidth(Double.MAX_VALUE);
		
		final HBox lay_option = new HBox(
			pck_endofday,
			cmb_showby,
			cmb_activity,
			btn
		);
		lay_option.getStyleClass().addAll("box-pad");
		lay_option.setAlignment(Pos.CENTER_LEFT);
		
		setCenter(new HBox(
			gen_table_forecast(),
			gen_table_marking()
		));
		//setLeft(lay_option);
		setBottom(lay_option);
		
		//update_table();
	}
	//-------------------------------//
	
	@SuppressWarnings("unchecked")
	private TableView<Description> gen_table_forecast() {
		
		@SuppressWarnings("rawtypes")
		final TableColumn col[] = {
			new TableColumn<Description,String>("位置("+Model.LOCA_UNIT+")"),
			new TableColumn<Description,String>("劑量("+Model.DOSE_UNIT+")"),
		};
		col[0].setMinWidth(100);
		col[1].setMinWidth(133);
		
		col[0].setCellValueFactory(new PropertyValueFactory<Description,String>("Location"));
		col[1].setCellValueFactory(new PropertyValueFactory<Description,String>("DoseRate"));

		final TableView<Description> table = new TableView<Description>();
		table.getStyleClass().addAll("font-console");
		table.setEditable(false);
		table.getColumns().addAll(col);
		lst_fore = table.getItems();
		return table;
	}
	@SuppressWarnings("unchecked")
	private TableView<Description> gen_table_marking() {
		
		@SuppressWarnings("rawtypes")
		final TableColumn col[] = {
			new TableColumn<Description,String>("位置("+Model.LOCA_UNIT+")"),
			new TableColumn<Description,String>("劑量("+Model.DOSE_UNIT+")"),
			new TableColumn<Description,String>("標準差 σ"),
			new TableColumn<Description,String>("時間戳記"),
		};
		col[0].setMinWidth(100);
		col[1].setMinWidth(133);
		col[2].setMinWidth(70);
		col[3].setMinWidth(180);
		
		col[0].setCellValueFactory(new PropertyValueFactory<Description,String>("Location"));
		col[1].setCellValueFactory(new PropertyValueFactory<Description,String>("DoseRate"));
		col[2].setCellValueFactory(new PropertyValueFactory<Description,String>("Deviation"));
		col[3].setCellValueFactory(new PropertyValueFactory<Description,String>("TimeStamp"));
		
		final TableView<Description> table = new TableView<Description>();
		table.getStyleClass().addAll("font-console");
		table.setEditable(false);
		table.getColumns().addAll(col);
		lst_mark = table.getItems();
		return table;
	}
	//-------------------------------//
	
	public void reload(final File fs) {
		if(fs.exists()==false) {
			return;
		}
		final Task<Integer> tsk = new Task<Integer>() {
			
			DataFormatter fmt;
			FormulaEvaluator eva;
			
			String get_cell_text(
				final Sheet sheet,
				final String address
			){
				CellReference ref = new CellReference(address);
				int yy = ref.getRow();
				int xx = ref.getCol();
				Cell cc = sheet.getRow(yy).getCell(xx);
				String res = fmt.formatCellValue(cc, eva);
				if(res.startsWith("#DIV")==true){
					return "";
				}
				return res;
			}
			
			Model collect_mark(final Sheet sh) {
				updateMessage("從 "+sh.getSheetName()+" 收集標定點");

				//the date of experiment~~~
				final LocalDateTime exp_day = LocalDateTime.parse(
					get_cell_text(sh,"D3").trim()+" 12:00:00", 
					dtf
				);
				
				Model mm = new Model();

				//for(char i='B'; i<='U'; i+=1) {
				for(char i='D'; i<='U'; i+=1) {
					String loca = get_cell_text(sh,String.format("%C%d",i,7));
					if(loca.length()==0) {
						continue;
					}
					loca = loca + " cm";
					String meas = "";
					for(int j=13; j<=32; j+=1){
						String val = get_cell_text(sh,String.format("%C%d",i,j));
						if(val.length()==0) {
							continue;
						}
						val = val + " μSv/min";
						meas = meas + val;
						if(j!=32) {
							meas = meas + ",";
						}
					}
					if(meas.length()==0) {
						continue;
					}
					mm.add(new Mark(exp_day,loca,meas));
				}
				//return mm.fitting();
				return mm;
			}
			
			@Override
			protected Integer call() throws Exception {
				try {
					Workbook wb = WorkbookFactory.create(fs);
					eva = wb.getCreationHelper().createFormulaEvaluator();
					fmt = new DataFormatter();
					model[0] = collect_mark(wb.getSheetAt(0));
					model[1] = collect_mark(wb.getSheetAt(1));
					model[2] = collect_mark(wb.getSheetAt(2));
					wb.close();
				} catch (IOException e) {
					updateMessage("無法讀取試算表："+e.getMessage());
				}
				return 0;
			}
		};
		tsk.setOnSucceeded(e->{
			final LocalDateTime day = endof.getValue().atTime(12, 0);
			model[0].fitting(day);
			model[1].fitting(day);
			model[2].fitting(day);
			update_table();
		});
		PanBase.self(this).notifyTask("讀取 "+fs.getName(),tsk);
	}
	public void reload(final String name) {
		reload(new File(name));
	}
	public void reloadLast() {
		reload(Gawain.pathSock+"mark-2018.xlsx");
	}
}
