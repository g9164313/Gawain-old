package prj.shelter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;

import prj.shelter.DevHustIO.Strength;

public class ManBooker extends BorderPane {

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
	
	public static class Mark implements Serializable {
		private static final long serialVersionUID = -124375273973164461L;
		
		final LocalDateTime time;
		final String loca;//HusiIO載台位置
		final String meas;//AT5350測量結果
		final SummaryStatistics stat;
		
		public Mark(
			final DevHustIO hustio,
			final DevAT5350 at5350
		) {
			time = LocalDateTime.now();
			loca = hustio.locationText.get();
			meas = at5350.lastMeasure();
			stat = at5350.lastSummary();
		}
		public Mark(final String v_loca,final double... val) {
			String v_meas = "";
			for(int i=0; i<val.length; i++) {
				v_meas = v_meas + String.format("%.2f Sv/hr #%d, ", val[i], i);
			}			
			time = LocalDateTime.now();
			loca = v_loca;
			meas = v_meas;			
			stat = new SummaryStatistics();
			for(int i=0; i<val.length; i++) {
				stat.addValue(val[i]);
			}			
		}
		public double getMeanDecay() {
			return stat.getMean() * calculate_decay(time,LocalDateTime.now());
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
			//回傳現在的劑量值
			return String.format("%.2f", getMeanDecay());
		}
		public String get1YAfter() {
			//回傳 1年後劑量值（直接近似）
			return String.format("%.2f", stat.getMean()*0.977305);
		}
		public String getStdDev() {
			final double dev = stat.getStandardDeviation();
			if(Double.isNaN(dev)) {
				return "---";
			}
			return String.format("%.5f", stat.getStandardDeviation());
		}
		public String getPerSigma() {
			final double avg = stat.getMean() * calculate_decay(time,LocalDateTime.now());
			final double dev = stat.getStandardDeviation();
			if(Double.isNaN(dev)) {
				return "---";
			}
			return String.format("%.2f%%", (dev/avg)*100.);
		}
	};
	
	private static final String DATA_NAME = "book.obj";
	
	private static final File DATA_FILE = new File(Gawain.getSockPath()+DATA_NAME);
	
	//private HashMap<Strength,ArrayList<Mark>> database = new HashMap<Strength,ArrayList<Mark>>();
	
	public ManBooker insert(
		final DevHustIO hustio,
		final DevAT5350 at5350
	) {
		final Mark mm = new Mark(hustio,at5350);
		final Strength ss = hustio.activity.get();
		Misc.logv("[inset-mark][%s] %s @ %.2f", 
			ss.toString(),
			hustio.locationText.get(), 
			mm.stat.getMean()
		);
		return insert(ss,mm);
	}
	public ManBooker insert(
		final DevHustIO.Strength ss,
		final String loca,
		final double... val
	) {
		return insert(ss,new Mark(loca,val));		
	}
	private ManBooker insert(
		final DevHustIO.Strength ss,
		final Mark mm
	) {
		update_table(ss,mm);
		save_items();		
		return this;
	}
	
	public List<Mark> selectAll(final DevHustIO.Strength ss){
		final List<Mark> lst = tabs.get(ss).getItems();
		if(lst!=null) {
			Collections.sort(lst, (e1,e2)->{
				final double v1 = e1.stat.getMean();
				final double v2 = e2.stat.getMean();
				if(v1<v2) { 
					return -1; 
				}else if(v1>v2) { 
					return 1; 
				}
				return 0;
			});
		}		
		return lst;
	}
	
	public Mark[] selectRange(
		final DevHustIO.Strength ss,
		final double dose
	) {
		List<Mark> lst = tabs.get(ss).getItems();
		if(lst==null) {
			Misc.loge("[ManBook] (%s) no list??", ss.toString());
			return null;
		}		
		//put a test point, then sort list~~~
		final Mark pt = new Mark("", dose);
		lst.add(pt);		
		Collections.sort(lst, (e1,e2)->{
			final double v1 = e1.getMeanDecay();
			final double v2 = e2.getMeanDecay();
			if(v1<v2) { 
				return -1; 
			}else if(v1>v2) { 
				return 1; 
			}
			return 0;
		});
		
		//the left one is less than target, the right one is bigger than target. 
		Mark[] res = {null, null};		
		final int idx = lst.indexOf(pt);
		final int end = lst.size()-1;
		if(idx==0) {
			res[1] = lst.get(1);
		}else if(idx==end) {
			res[0] = lst.get(end-1);
		}else {
			res[0] = lst.get(idx-1);
			res[1] = lst.get(idx+1);
		}
		//Remember, clear test point~~~
		lst.remove(pt);
		return res;
	}
	public Mark[] selectRange(
		final DevHustIO.Strength stg,
		final String dose
	) {
		return selectRange(stg,UtilPhysical.convert(dose,"uSv/hr"));
	}
	
	public String selectHalfLocation(
		final DevHustIO.Strength stg,
		final String dose
	) {
		Mark[] mk = selectRange(stg,dose);
		if(mk==null) { 
			return ""; 
		}
		double loca0=0., loca1=600.;
		if(mk[0]!=null && mk[1]!=null) {
			loca0 = UtilPhysical.convert(mk[0].loca, "cm");
			loca1 = UtilPhysical.convert(mk[1].loca, "cm");
		}else if(mk[0]!=null && mk[1]==null) {
			loca0 = UtilPhysical.convert(mk[0].loca, "cm");
		}else if(mk[0]==null && mk[1]!=null) {
			loca1 = UtilPhysical.convert(mk[1].loca, "cm");
		}		
		return String.format("%.4f cm", (loca0+loca1)/2.);
	}
	
	public ManBooker reload() {
		if(DATA_FILE.exists()==true) {
			load_data();		
		}		
		return this;
	}
	public ManBooker restore() {
		//backup old data~~~
		if(DATA_FILE.exists()==true) {
			try {
				final DateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
				final String path = DATA_FILE.getAbsolutePath();
				Files.move(
					Paths.get(path), 
					Paths.get(path+"-bak-"+fmt.format(Calendar.getInstance().getTime()))
				);
			} catch (IOException e) {
				Misc.loge(e.getMessage());
				return this;
			}
		}		
		save_items();
		return this;
	}
	private void load_data() {
		Object[] lst = (Object[])Misc.deserializeFile(DATA_FILE);
		tabs.get(Strength.V_005Ci).getItems().addAll((Mark[])lst[0]);
		tabs.get(Strength.V_05Ci).getItems().addAll((Mark[])lst[1]);
		tabs.get(Strength.V_3Ci).getItems().addAll((Mark[])lst[2]);
	}	
	private void save_items() {
		Object[] lst = new Object[3];
		lst[0] = tabs.get(Strength.V_005Ci).getItems().toArray(new Mark[0]);
		lst[1] = tabs.get(Strength.V_05Ci).getItems().toArray(new Mark[0]);
		lst[2] = tabs.get(Strength.V_3Ci).getItems().toArray(new Mark[0]);				
		Misc.serialize2file(lst, DATA_FILE);	
	}
	//------------------------------------------

	private HashMap<Strength,TableView<Mark>> tabs = new HashMap<Strength,TableView<Mark>>();

	private void update_table(
		final Strength ss,
		final Mark mm
	) {
		@SuppressWarnings("unchecked")
		final TableView<Mark> tt = (TableView<Mark>) tabs.get(ss);
		if(tt==null) {
			return;
		}
		if(Application.isEventThread()) {
			tt.getItems().add(mm);
		}else {
			Application.invokeLater(()->tt.getItems().add(mm));
		}
	}
	private ObservableList<Mark> get_list(final Strength ss){
		@SuppressWarnings("unchecked")
		final TableView<Mark> tt = (TableView<Mark>) tabs.get(ss);
		if(tt==null) {
			return null;
		}
		return tt.getItems();
	}
	
	@SuppressWarnings("unchecked")
	public ManBooker(){
		Strength[] ss = Strength.values();
		
		tabs.put(ss[0], gen_table(ss[0]));
		tabs.put(ss[1], gen_table(ss[1]));
		tabs.put(ss[2], gen_table(ss[2]));
		
		ToggleGroup grp_stng = new ToggleGroup();
		
		final JFXRadioButton[] rad_stng = {null, null, null};
		for(int i=0; i<rad_stng.length; i++) {
			final JFXRadioButton obj = new JFXRadioButton(ss[i].toString());
			obj.setToggleGroup(grp_stng);
			obj.setUserData(ss[i]);
			obj.setOnAction(e->{
				
			});
			rad_stng[i] = obj;
		}
		grp_stng.selectToggle(rad_stng[0]);
		
		tabs.get(ss[0]).visibleProperty().bind(rad_stng[0].selectedProperty());
		tabs.get(ss[1]).visibleProperty().bind(rad_stng[1].selectedProperty());
		tabs.get(ss[2]).visibleProperty().bind(rad_stng[2].selectedProperty());
		
		JFXButton btn_import = new JFXButton("匯入");
		btn_import.getStyleClass().add("btn-raised-1");
		btn_import.setOnAction(e->{
			final FileChooser dia = new FileChooser();
			dia.setTitle("匯入標定");
			dia.setInitialDirectory(Gawain.getSockFile());
			final File fs = dia.showOpenDialog(getScene().getWindow());
			if(fs.isFile()==true) {
				//TODO:!!!!
			}
		});
		
		JFXButton btn_export = new JFXButton("試算表");
		btn_export.getStyleClass().add("btn-raised-1");
		btn_export.setOnAction(e->{
			final FileChooser dia = new FileChooser();
			dia.setTitle("匯出試算表");
			dia.setInitialDirectory(Gawain.getSockFile());
			final File fs = dia.showSaveDialog(getScene().getWindow());
			if(fs==null) {
				return;
			}
			final PanBase pan = PanBase.self(this);
			final TskExport tsk = new TskExport(fs,this);
			pan.notifyTask(tsk);
		});
		
		HBox lay1 = new HBox();
		lay1.getChildren().addAll(rad_stng);
		lay1.getChildren().addAll(btn_import, btn_export);
		lay1.getStyleClass().addAll("box-pad");
		lay1.setAlignment(Pos.BASELINE_LEFT);
		
		setTop(lay1);
		setCenter(new StackPane(
			tabs.get(ss[0]),
			tabs.get(ss[1]),
			tabs.get(ss[2])
		));
	}
	
	@SuppressWarnings("unchecked")
	private TableView<Mark> gen_table(final Strength ss){
		@SuppressWarnings("rawtypes")
		final TableColumn col[] = {
			new TableColumn<Mark,String>("時間戳記"),
			new TableColumn<Mark,String>("標定位置"),
			new TableColumn<Mark,String>("標定劑量(uSv/hr)"),
			new TableColumn<Mark,String>("衰退後"),
			new TableColumn<Mark,String>("1年後"),
			new TableColumn<Mark,String>("標準差σ"),
			new TableColumn<Mark,String>("%Sigma"),
		};
		col[0].setMinWidth(120);
		col[1].setMinWidth(100);
		col[2].setMinWidth(100);
		col[3].setMinWidth(100);
		col[4].setMinWidth(100);
		col[5].setMinWidth(70);
		col[6].setMinWidth(70);
		
		col[0].setCellValueFactory(new PropertyValueFactory<Mark,String>("Stamp"));
		col[1].setCellValueFactory(new PropertyValueFactory<Mark,String>("Location"));
		col[2].setCellValueFactory(new PropertyValueFactory<Mark,String>("AvgDose"));
		col[3].setCellValueFactory(new PropertyValueFactory<Mark,String>("DecDose"));
		col[4].setCellValueFactory(new PropertyValueFactory<Mark,String>("1YAfter"));
		col[5].setCellValueFactory(new PropertyValueFactory<Mark,String>("StdDev"));
		col[6].setCellValueFactory(new PropertyValueFactory<Mark,String>("PerSigma"));
		
		final TableView<Mark> table = new TableView<Mark>();
		table.getStyleClass().addAll("font-console");
		table.setEditable(false);
		table.getColumns().addAll(col);
		
		final MenuItem c_remove = new MenuItem("清除");
		c_remove.setOnAction(e->{
			final TableViewSelectionModel<Mark> slt = table.getSelectionModel();
			if(slt.isEmpty()==true) {
				//remove all items??
				table.getItems().clear();
			}else {
				//remove target
				for(Mark mm:slt.getSelectedItems()) {
					table.getItems().remove(mm);
				}
			}			
		});
				
		table.setContextMenu(new ContextMenu(
			c_remove
		));		
		return table;
	}
}
