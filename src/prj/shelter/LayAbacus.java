package prj.shelter;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXRadioButton;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.UtilPhysical;
import prj.shelter.DevHustIO.Strength;

public class LayAbacus extends BorderPane
{
	//private final static DateTimeFormatter fmt_day = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	//private final static DateTimeFormatter fmt_time= DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	
	public static class Mark implements Serializable {
		static final long serialVersionUID = 6061524050057088702L;
		
		final LocalDateTime time;
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
			time = markday_sec;
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
			SummaryStatistics ss = get_summary(unit);
			return ss.getMean();
		}
		
		public String getLocation() {
			return loca;
		}
		public String getAvgDose() {
			SummaryStatistics ss = get_summary(MODEL_DOSE_UNIT);
			return String.format("%.2f", ss.getMean());
		}
		public String getStdDev() {
			SummaryStatistics ss = get_summary(MODEL_DOSE_UNIT);
			return String.format("%.5f", ss.getStandardDeviation());
		}
		public String getStamp() { 
			return time.toString();
		}
		
		private SummaryStatistics get_summary(final String unit) {
			SummaryStatistics ss = new SummaryStatistics();
			for(String itm:meas.split(",")) {				
				final int pos = itm.indexOf("#");
				if(pos>=0) {
					itm = itm.substring(0,pos);
				}
				itm = itm.replace('"',' ').trim();
				
				String txt = UtilPhysical.convertScale(itm,unit);
				if(txt.length()==0) {
					continue;
				}
				ss.addValue(Float.valueOf(txt));
			}
			return ss;			
		}		
	};
		
	public static class Model extends ArrayList<Mark> 
		implements Serializable, ParametricUnivariateFunction
	{
		static final long serialVersionUID = 7859312449362122438L;
		
		//Casium-137 --> 30.17 year --> 952 Msec
		//see WIKI: radioactive nuclides by half-life
		private long half_life_nuclide = 952000000;//unit is second
		
		private LocalDateTime endof = LocalDateTime.now();
		
		private double calculate_decay(
			final LocalDateTime start,
			final LocalDateTime endof
		) {
			final long elapse = Duration.between(start,endof).toMillis() / 1000L;
			if(elapse<0) {
				return 1.;
			}
			return Math.exp((-0.693*elapse)/half_life_nuclide);
		}
		
		public Model fitting(final LocalDateTime current) {
			endof =current;//change end of time~~
			return fitting();
		}
		public Model fitting() {
			final WeightedObservedPoints obv = new WeightedObservedPoints();			
			for(Mark mm:this) {				
				double dd = calculate_decay(mm.time,endof);//RADIOACTIVE DECAY EQUATION				
				double xx = mm.getX(MODEL_LOCA_UNIT);
				double yy = mm.getY(MODEL_DOSE_UNIT) * dd; 
				obv.add(xx, yy);
			}
			if(size()<=2) {
				return this;
			}
			double[] starter = {1, 1, 1, 1};		
			coeff = SimpleCurveFitter
				.create(this, starter)
				.fit(obv.toList());		
			return this;
		}
		
		private double[] coeff;

		public double getDose(double loca) {
			if(coeff==null) { return -1.; }
			return value(loca,coeff);
		}
		public double getLoca(double dose) {
			if(coeff==null) { return -1.; }
			return inv_value(dose);
		}
		
		public double inv_value(double y, double... parameters) {
			// 4PL - inverse function
			final double a = parameters[0];
			final double b = parameters[1];
			final double c = parameters[2];
			final double d = parameters[3];
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
			//see: https://www.numberempire.com/derivativecalculator.php
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
	
	private HashMap<Strength,Model> mmap = new HashMap<Strength,Model>();
	
	public static final String MODEL_LOCA_UNIT = "cm";
	public static final String MODEL_DOSE_UNIT = "μSv/hr";
	public static final String MODEL_NAME = "radiation-model.obj";
	
	@SuppressWarnings("unchecked")
	public void reloadLast() {
		final String path = Gawain.getSockPath();
		final File fs = new File(path+MODEL_NAME);
		if(fs.exists()==true) {
			mmap = (HashMap<Strength, Model>)Misc.deserializeFile(fs);
		}else {
			//empty model~~~~
			mmap.put(Strength.V_005Ci, new Model());
			mmap.put(Strength.V_05Ci , new Model());
			mmap.put(Strength.V_3Ci  , new Model());
		}
		mmap.forEach((ss,mm)->mm.fitting());
	}
	
	public String predictDoseRate(final Strength ss,final String location) {
		final Model mm = mmap.get(ss);
		if(mm==null) { return ""; }
		final double loca = UtilPhysical.convert(location, MODEL_LOCA_UNIT);
		final double dose = mm.getDose(loca);
		return String.format("%.3f %s", dose, MODEL_DOSE_UNIT);
	};	
	public String predictLocation(final Strength ss,final String doseRate) {
		final Model mm = mmap.get(ss);
		if(mm==null) { return ""; }
		final double dose = UtilPhysical.convert(doseRate, MODEL_DOSE_UNIT);
		final double loca = mm.getLoca(dose);
		return String.format("%.3f %s", loca, MODEL_LOCA_UNIT);
	};
	public void addMark(
		final Strength ss,
		final String loca, 
		final String meas
	) {
		final Model mm = mmap.get(ss);
		if(mm==null) { return; }
		mm.add(new Mark(loca,meas));
		mm.fitting();
	}
	public void clearAllMark() {
		mmap.forEach((ss,mm)->mm.clear());		
	}
	//----------------------------------------------------
	
	/*public Description[] getLocaDesc(double... loca) {
	ArrayList<Description> lst = new ArrayList<Description>();
	for(int i=0; i<loca.length; i++) {
		final double xx = loca[i];
		final double yy = getDose(xx);
		if(Double.isNaN(xx)==true) {
			continue;
		}
		final Description obj = new Description();
		obj.loca = String.format("%8.2f", xx);
		obj.dose = String.format("%8.2f", yy);
		lst.add(obj); 
	}
	return lst.toArray(new Description[0]);
}
public Description[] getDoseDesc(double... dose) {
	ArrayList<Description> lst = new ArrayList<Description>();
	for(int i=0; i<dose.length; i++) {
		final double yy = dose[i];
		final double xx = getLoca(yy);
		if(Double.isNaN(xx)==true) {
			continue;
		}
		final Description obj = new Description();
		obj.loca = String.format("%8.2f", xx);
		obj.dose = String.format("%8.2f", yy);
		lst.add(obj); 
	}
	return lst.toArray(new Description[0]);
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
		obj.stmp= fmt_time.format(mark.date);
		lst[i] = obj; 
	}
	return lst;
}*/
	
	public LayAbacus() {
		
		final TableView<Mark> tab_mark = gen_table_mark();
		
		final JFXDatePicker pck_endofday = new JFXDatePicker();

		pck_endofday.setValue(LocalDate.now());
		pck_endofday.setPrefWidth(160.);
		pck_endofday.valueProperty().addListener((obv,oldVal,newVal)->{
		});

		final TextField box_loca = new TextField("0 cm");
		final TextField box_dose = new TextField("0 uSv/hr");
		
		final ToggleGroup grp_stng = new ToggleGroup();
		final JFXRadioButton[] rad_stng = {
			new JFXRadioButton("0.05Ci"),
			new JFXRadioButton("0.5Ci"),
			new JFXRadioButton("3Ci")			
		};
		rad_stng[0].setToggleGroup(grp_stng);
		rad_stng[1].setToggleGroup(grp_stng);
		rad_stng[2].setToggleGroup(grp_stng);		
		rad_stng[0].setOnAction(e->update_table(Strength.V_005Ci,tab_mark));
		rad_stng[1].setOnAction(e->update_table(Strength.V_05Ci ,tab_mark));
		rad_stng[2].setOnAction(e->update_table(Strength.V_3Ci  ,tab_mark));
		grp_stng.selectToggle(rad_stng[0]);
		
		final JFXButton btn_refresh = new JFXButton("更新");
		btn_refresh.getStyleClass().add("btn-raised-1");
		btn_refresh.setMaxWidth(Double.MAX_VALUE);
		btn_refresh.setMaxHeight(Double.MAX_VALUE);
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad","font-console");
		lay1.addColumn(0,rad_stng);
		lay1.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		lay1.addColumn(2,new Label("距離"), box_loca);
		lay1.addColumn(3,new Label("劑量"), box_dose);
		lay1.addColumn(4,new Label("衰退至"), pck_endofday);
		
		lay1.add(btn_refresh, 5, 0, 3, 3);
		
		setTop(lay1);
		setCenter(tab_mark);
	}
	//-------------------------------//

	@SuppressWarnings("unchecked")
	private TableView<Mark> gen_table_mark() {
		
		@SuppressWarnings("rawtypes")
		final TableColumn col[] = {
			new TableColumn<Mark,String>("位置"),
			new TableColumn<Mark,String>("平均劑量("+MODEL_DOSE_UNIT+")"),
			new TableColumn<Mark,String>("標準差 σ"),
			new TableColumn<Mark,String>("時間戳記"),
		};
		col[0].setMinWidth(170);
		col[1].setMinWidth(200);
		col[2].setMinWidth(170);
		col[3].setMinWidth(170);
		
		col[0].setCellValueFactory(new PropertyValueFactory<Mark,String>("getLocation"));
		col[1].setCellValueFactory(new PropertyValueFactory<Mark,String>("getAvgDose"));
		col[2].setCellValueFactory(new PropertyValueFactory<Mark,String>("getStdDev"));
		col[3].setCellValueFactory(new PropertyValueFactory<Mark,String>("getStamp"));
		
		final TableView<Mark> table = new TableView<Mark>();
		table.getStyleClass().addAll("font-console");
		table.setEditable(false);
		table.getColumns().addAll(col);
		return table;
	}
	
	private void update_table(
		final Strength ss,
		final TableView<Mark> tab
	) {
		tab.getItems().clear();
		final Model lst = mmap.get(ss);
		if(lst==null) {
			return;
		}
		for(Mark mm:lst) {
			tab.getItems().add(mm);
		}
	}	
	//-------------------------------//
	
	/*public void reload(final File fs) {
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
					fmt_time
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
				return mm.fitting(endofday.getValue().atTime(12, 0));
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
		tsk.setOnSucceeded(e->update_table());
		PanBase.self(this).notifyTask("讀取 "+fs.getName(),tsk);
	}*/
}
