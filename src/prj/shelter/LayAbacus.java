package prj.shelter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXRadioButton;

import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
	
	//Casium-137 --> 30.17 year --> 952 Msec
	//see WIKI: radioactive nuclides by half-life
	private static long half_life_nuclide = 952000000;//unit is second
	
	private static double calculate_decay(
		final LocalDateTime start,
		final LocalDateTime endof
	) {
		final long elapse = Duration.between(start,endof).toMillis() / 1000L;
		if(elapse<0) {
			return 1.;
		}
		return Math.exp((-0.693*elapse)/half_life_nuclide);
	}
	//----------------------------------------
	
	public static class Mark implements Serializable {
		static final long serialVersionUID = 6061524050057088702L;
		
		final LocalDateTime time;
		final String loca;//HusiIO載台位置
		final String meas;//AT5350測量結果
		final SummaryStatistics stat;
		
		public Mark(
			final String location, 
			final DevAT5350 dev
		) {
			this(LocalDateTime.now(),location,dev);
		}
		public Mark(
			final LocalDateTime markday_sec,
			final String location, 
			final DevAT5350 dev
		) {
			time = markday_sec;
			loca = location;
			meas = dev.lastMeasure();
			stat = dev.lastSummary();
		}
		double getX(final String unit) {
			return Double.valueOf(UtilPhysical.convertScale(loca,unit));
		}
		double getY(final String unit) {
			if(meas.length()==0) {
				return 0f;
			}
			return stat.getMean();
		}

		public String getStamp() {
			String txt = time.toString();
			return txt.substring(0,txt.lastIndexOf(':'));
		}
		public String getLocation() {
			return loca;
		}
		public String getAvgDose() {
			return String.format("%.2f", stat.getMean());
		}
		public String getDecDose() {
			return String.format("%.2f", stat.getMean() * calculate_decay(time,LocalDateTime.now()));
		}
		public String get1YAfter() {
			return String.format("%.2f", stat.getMean()*0.977305);
		}
		public String getStdDev() {
			return String.format("%.5f", stat.getStandardDeviation());
		}
	};

	public static class Model extends ArrayList<Mark> 
		implements Serializable, ParametricUnivariateFunction
	{
		static final long serialVersionUID = 7859312449362122438L;

		public Model fitting() {
			return fitting(LocalDateTime.now());
		}
		public Model fitting(final LocalDateTime endOfDay) {
			final WeightedObservedPoints obv = new WeightedObservedPoints();			
			for(Mark mm:this) {				
				double dd = calculate_decay(mm.time, endOfDay);//RADIOACTIVE DECAY EQUATION				
				double xx = mm.getX(MODEL_LOCA_UNIT);
				double yy = mm.getY(MODEL_DOSE_UNIT) * dd; 
				obv.add(xx, yy);
			}
			if(size()<=2) {
				return this;
			}
			double[] init = {1., 1., 1., 1.};
			if(coeff!=null) {
				init = coeff;
			}
			coeff = SimpleCurveFitter				
				.create(this, init)
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
			return inv_value(dose,coeff);
		}
		public String getText() {
			if(coeff==null) {
				return "y = d + (a-d) / (1+b^(x/c))";
			}			
			return String.format(
				"y = %.3f + %.3f / (1+(%.3f)^(x/%.3f))", 
				coeff[3], coeff[0]-coeff[3], coeff[1], coeff[2]
			);
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
	public void dumpModel() {
		final String path = Gawain.getSockPath();
		final File fs = new File(path+MODEL_NAME);
		if(fs.exists()==true) {
			final DateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");			
			final File tmp = new File(
				path+
				MODEL_NAME+"."+
				fmt.format(Calendar.getInstance().getTime())+
				"-bak"
			);
			try {
				Files.move(
					Paths.get(fs.getAbsolutePath()), 
					Paths.get(tmp.getAbsolutePath())
				);
			} catch (IOException e) {
				Misc.loge(e.getMessage());
				return;
			}
		}
		mmap.forEach((ss,mm)->mm.fitting());
		Misc.serialize2file(mmap, fs);
	}
	
	public String predictDoseRate(
		final Strength ss,
		final String location
	) {
		return predictDoseRate(ss,location,LocalDateTime.now());
	}
	public String predictDoseRate(
		final Strength ss,
		final String location,
		final LocalDateTime endOfDay
	) {
		final Model mm = mmap.get(ss);
		if(mm==null) { return "?"; }
		if(mm.coeff==null) { return "??"; }
		mm.fitting(endOfDay);
		
		final double loca = UtilPhysical.convert(location, MODEL_LOCA_UNIT);
		final double dose = mm.getDose(loca);
		return String.format("%.3f %s", dose, MODEL_DOSE_UNIT);
	};
	
	public String predictLocation(
		final Strength ss,
		final String doseRate
	) {
		return predictLocation(ss,doseRate,LocalDateTime.now());
	}
	public String predictLocation(
		final Strength ss,
		final String doseRate,
		final LocalDateTime endOfDay
	) {
		final Model mm = mmap.get(ss);
		if(mm==null) { return "?"; }
		if(mm.coeff==null) { return "??"; }
		mm.fitting(endOfDay);

		final double dose = UtilPhysical.convert(doseRate, MODEL_DOSE_UNIT);
		final double loca = mm.getLoca(dose);
		return String.format("%.3f %s", loca, MODEL_LOCA_UNIT);
	};

	public void removeLastMark(final Strength ss) {
		final Model md = mmap.get(ss);
		if(md==null) { return; }
		final int len = md.size();
		if(len<=4) {
			return;
		}
		md.remove(len-1);
	}
	
	public void addMark(
		final Strength ss,
		final String loca, 
		final DevAT5350 dev
	) {
		final Model md = mmap.get(ss);
		if(md==null) { return; }
		final Mark mk = new Mark(loca,dev);
		md.add(mk);
		md.fitting();
		add_table_item(ss,mk);
		txt_info.setText(md.getText());
	}
	
	public void clearAllMark() {
		mmap.forEach((ss,mm)->mm.clear());
		tab_mark.getItems().clear();
	}
	public void applyFitting() {
		mmap.forEach((ss,mm)->{
			if(mm.size()>=3) {
				mm.fitting();
			}
		});
	}
	//----------------------------------------------------
	
	final TableView<Mark> tab_mark = gen_table_mark();
	final ToggleGroup grp_stng = new ToggleGroup();
	final Label txt_info = new Label();
	
	public LayAbacus() {
		
		reloadLast();
		
		final JFXDatePicker pck_endofday = new JFXDatePicker();
		pck_endofday.setValue(LocalDate.now());
		pck_endofday.setPrefWidth(160.);

		final TextField box_loca = new TextField("0 cm");		
		final TextField box_dose = new TextField("0 uSv/hr");
				
		box_loca.setOnAction(e->{
			final Strength ss = (Strength)grp_stng.getSelectedToggle().getUserData();
			final LocalDateTime ed = pck_endofday.getValue().atTime(0, 0);
			box_dose.setText(predictDoseRate(ss,box_loca.getText(),ed));
		});
		box_dose.setOnAction(e->{
			final Strength ss = (Strength)grp_stng.getSelectedToggle().getUserData();
			final LocalDateTime ed = pck_endofday.getValue().atTime(0, 0);
			box_loca.setText(predictLocation(ss,box_dose.getText(),ed));
		});
		
		final Strength[] lst_ss = Strength.values();
		final JFXRadioButton[] rad_stng = new JFXRadioButton[lst_ss.length];
		for(int i=0; i<rad_stng.length; i++) {
			final Strength ss = lst_ss[i];
			final JFXRadioButton obj = new JFXRadioButton(ss.toString());
			obj.setToggleGroup(grp_stng);
			obj.setUserData(ss);
			obj.setOnAction(e->{
				flush_screen(ss);
			});
			rad_stng[i] = obj;			
		}
		grp_stng.selectToggle(rad_stng[0]);
		flush_screen((Strength)rad_stng[0].getUserData());
		
		/*final JFXButton btn_refresh = new JFXButton("更新");
		btn_refresh.getStyleClass().add("btn-raised-1");
		btn_refresh.setMaxWidth(Double.MAX_VALUE);
		btn_refresh.setMaxHeight(Double.MAX_VALUE);
		btn_refresh.setOnAction(e->{
			final Strength ss = (Strength)grp_stng.getSelectedToggle().getUserData();			
			flush_screen(ss);
		});*/
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad","font-console");
		lay1.addColumn(0,rad_stng);
		lay1.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		lay1.addColumn(2,new Label("距離"), box_loca);
		lay1.addColumn(3,new Label("劑量"), box_dose);
		lay1.addColumn(4,new Label("衰退至"), pck_endofday);
		lay1.add(txt_info, 2, 2, 3, 1);
		//lay1.add(btn_refresh, 5, 0, 3, 3);
		
		setTop(lay1);
		setCenter(tab_mark);
	}
	//-------------------------------//

	@SuppressWarnings("unchecked")
	private TableView<Mark> gen_table_mark() {
		
		@SuppressWarnings("rawtypes")
		final TableColumn col[] = {
			new TableColumn<Mark,String>("戳記"),
			new TableColumn<Mark,String>("標定位置"),
			new TableColumn<Mark,String>("標定劑量("+MODEL_DOSE_UNIT+")"),
			new TableColumn<Mark,String>("衰退"),
			new TableColumn<Mark,String>("1年後"),
			new TableColumn<Mark,String>("標準差 σ"),
		};
		col[0].setMinWidth(140);
		col[1].setMinWidth(140);
		col[2].setMinWidth(150);
		col[3].setMinWidth(150);
		col[4].setMinWidth(150);
		col[5].setMinWidth(100);
		
		col[0].setCellValueFactory(new PropertyValueFactory<Mark,String>("Stamp"));
		col[1].setCellValueFactory(new PropertyValueFactory<Mark,String>("Location"));
		col[2].setCellValueFactory(new PropertyValueFactory<Mark,String>("AvgDose"));
		col[3].setCellValueFactory(new PropertyValueFactory<Mark,String>("DecDose"));
		col[4].setCellValueFactory(new PropertyValueFactory<Mark,String>("1YAfter"));
		col[5].setCellValueFactory(new PropertyValueFactory<Mark,String>("StdDev"));

		final TableView<Mark> table = new TableView<Mark>();
		table.getStyleClass().addAll("font-console");
		table.setEditable(false);
		table.getColumns().addAll(col);
		
		final MenuItem c_cls = new MenuItem("清除");
		c_cls.setOnAction(e->clearAllMark());
		final MenuItem c_fit = new MenuItem("計算");
		c_fit.setOnAction(e->{
			applyFitting();
			flush_screen((Strength)grp_stng.getSelectedToggle().getUserData());
		});
		table.setContextMenu(new ContextMenu(c_cls,c_fit));
		
		return table;
	}
	
	private void flush_screen(final Strength ss) {
		tab_mark.getItems().clear();
		txt_info.setText("");
		final Model mm = mmap.get(ss);
		if(mm==null) { 
			return; 
		}
		txt_info.setText(mm.getText());
		for(Mark mk:mm) {
			tab_mark.getItems().add(mk);
		}		
	}
	private void add_table_item(final Strength ss,final Mark mk) {
		final Strength dst = (Strength)grp_stng.getSelectedToggle().getUserData();
		if(ss==dst) {
			tab_mark.getItems().add(mk);
		}		
	}
}
