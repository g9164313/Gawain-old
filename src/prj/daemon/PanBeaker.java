package prj.daemon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.neuralnet.twod.NeuronSquareMesh2D;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.google.common.primitives.Doubles;
import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import narl.itrc.ButtonEx;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanBeaker extends PanBase {
	
	private final static DecimalFormat fmt = new DecimalFormat("###,###,###,###,###");

	private StringProperty[] info = {
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty()
	};
		
	private class Ballot {
		String txt = "";
		int[] val;
		Ballot(int[] data){
			val = data;
		}
		int getDistance(){			
			int sum = 0;
			for(int i=0; i<val.length-1; i++){
				sum += Math.abs(val[i+1] - val[i]);
			}
			return sum-val.length-1;
		}
	}; 
	private int compare(final Ballot aa, final Ballot bb){
		int cnt = 0;
		for(int i=0; i<aa.val.length; i++){
			for(int j=0; j<bb.val.length; j++){
				if(aa.val[i]==bb.val[j]){
					cnt+=1;
					break;
				}
			}
		}
		return cnt;
	}
	
	private ArrayList<Ballot> lstComb = new ArrayList<Ballot>();
	
	private class TaskMakeCombo extends Task<Void> {
		Combinations binc;
		long work,size;
		TaskMakeCombo(int n, int k){			
			binc = new Combinations(n,k);
			size = CombinatoricsUtils.binomialCoefficient(n, k);
			lstComb.clear();		
			lstComb.ensureCapacity((int)size);
			set_column(k);
			info[0].set(String.format("%d choose %d ", n, k));
			info[1].set(String.format("size=%s ", fmt.format(size)));
		}
		@Override
		protected Void call() throws Exception {
			binc.forEach(val->{
				lstComb.add(new Ballot(val));
				updateProgress(++work, size);
			});
			Application.invokeAndWait(()->list2table());
			return null;
		}
	};
		
	public PanBeaker(){
	}

	void trival_event(){
		double[][] val = {
			{1.1},
			{1.3},
			{1.2},
			{1.4},
			{3.5},
		};
		ArrayList<DoublePoint> data = new ArrayList<DoublePoint>();
		data.add(new DoublePoint(val[0]));
		data.add(new DoublePoint(val[1]));
		data.add(new DoublePoint(val[2]));
		data.add(new DoublePoint(val[3]));
		data.add(new DoublePoint(val[4]));
		DBSCANClusterer<DoublePoint> cc = new DBSCANClusterer<DoublePoint>(2,1);
		List<Cluster<DoublePoint>> res = cc.cluster(data);
		for(Cluster<DoublePoint> itm:res){
			System.out.println("----cluster----");
			List<DoublePoint> lst = itm.getPoints();
			for(DoublePoint v:lst){
				System.out.format("%.1f\n", v.getPoint()[0]);
			}
		}		
	}
	
	private void drill(){
		
		int[] hist = new int[49];
		for(int i=lstComb.size()-2; i>=0; --i){
			Ballot aa = lstComb.get(i);
			int d = aa.getDistance();
			hist[d]+=1;
		}
		
		for(int i=0; i<hist.length; i++){
			System.out.format("%02d]", i);
			for(int j=0; j<hist[i]; j++){
				System.out.print("*");
			}
			System.out.println();
		}
		
		/*int tail = 200;
		double[] lst = new double[tail];
		for(int i=0; i<lst.length; i++){
			lst[i] = lstComb.get(i).val[0];
		}
		EmpiricalDistribution dist = new EmpiricalDistribution(100);		
		dist.load(lst);
		int aa = lstComb.get(tail+1).val[0];
		int bb = 0;
		int cnt = 0;
		do{
			bb = (int)dist.getNextValue();
			Misc.logv("%02d) --> %02d", aa, bb); 
			cnt++;
		}while(aa!=bb);
		Misc.logv("guess=%d", cnt);*/ 
		return;
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		//information bar
		final Label[] txtInfo = new Label[info.length];
		for(int i=0; i<info.length; i++){
			Label txt = new Label();
			txt.textProperty().bind(info[i]);
			txtInfo[i] = txt;			
		}
		final HBox lay1 = new HBox(7);
		lay1.getChildren().addAll(txtInfo);
		
		final ButtonEx btnMake = new ButtonEx(
			"製造","coffee-to-go.png"
		).setOnClick(event->{
		});
		
		final ButtonEx btnPrv = new ButtonEx(
			"上一頁","chevron-left.png"
		).setOnClick(e->{
			int p = pageIdx - 1;
			if(p<0){
				return;
			}
			pageIdx = p;
			list2table();
		});		
		final ButtonEx btnNxt = new ButtonEx(
			"下一頁","chevron-right.png"
		).setOnClick(e->{
			int p = pageIdx + 1;
			if(p>=pageMax){
				return;
			}
			pageIdx = p;
			list2table();
		});

		final ButtonEx btn1 = new ButtonEx(
			"蒸餾-1","coffee-to-go.png"
		).setOnClick(e->{
			doDuty(()->drill());
		});
		final ButtonEx btn2 = new ButtonEx(
			"remove-2","coffee-to-go.png"
		).setOnClick(e->{
			final TextField box1 = new TextField();
			final TextField box2 = new TextField();
			final GridPane lay = new GridPane();
			lay.addRow(0, new Label("max-rank："), box1);
			lay.addRow(1, new Label("ballot  ："), box2);
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.getDialogPane().setContent(lay);
			if(dia.showAndWait().get()==ButtonType.OK){
				//int rank = Integer.valueOf(box1.getText().trim());
				//int[] target = text2int(box2.getText().trim());
			}
		});
		final ButtonEx btn3 = new ButtonEx(
			"group","coffee-to-go.png"
		).setOnClick(e->{
		});
		
		final ToolBar bar = new ToolBar(
			btnMake,
			new Separator(Orientation.VERTICAL),
			btnPrv,
			btnNxt,
			new Separator(Orientation.VERTICAL),
			btn1,
			btn2,
			btn3
		);		
		final BorderPane lay0 = new BorderPane();
		lay0.setTop(bar);
		lay0.setCenter(tabComb);
		lay0.setBottom(lay1);
		
		set_column(6);
		
		stage().setOnShown(e->{
			//read_from_csv(new File("/home/qq/桌面/lotter.txt"));
			//refresh();
			trival_event();
		});
		return lay0;
	}
	
	private void read_from_csv(final File fs){
		if(fs==null){
			return;
		}
		try {
			BufferedReader rd = new BufferedReader(new FileReader(fs));
			String txt;
			while((txt = rd.readLine())!=null){
				txt = txt.trim();
				if(txt.charAt(0)=='#'){
					continue;
				}
				String[] args = txt.trim().split("[,\\s]+");
				int[] val = new int[6];
				for(int i=0; i<val.length; i++){
					val[i] = Misc.txt2int(args[i]);
				}
				lstComb.add(new Ballot(val));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	//----------------------------------//
	
	private class ColValue implements 
		Callback<CellDataFeatures<Ballot,String>,ObservableValue<String>>
	{
		int idx = 0;
		final SimpleStringProperty txt = new SimpleStringProperty();
		ColValue(int i){
			idx = i;
		}
		@Override
		public ObservableValue<String> call(CellDataFeatures<Ballot, String> param) {
			final Ballot itm = param.getValue();
			if(idx==0){
				txt.set(itm.txt);
			}else{
				txt.set(String.format(
					"%02d", 
					itm.val[idx-1]
				));
			}
			return txt;
		}
	};
	private TableView<Ballot> tabComb = new TableView<Ballot>();
		
	private void set_column(int k){
		tabComb.getColumns().clear();
		for(int i=0; i<=k; i++){
			final String title = (i==0)?("-"):("");
			TableColumn<Ballot, String> col = new TableColumn<>(title);			
			col.setCellValueFactory(new ColValue(i));
			tabComb.getColumns().add(col);
		}
	}
	private int pageIdx, pageMax;
	private final int ONE_PAGE_SIZE = 200;
	private void list2table(){
		tabComb.getItems().clear();
		int cnt = lstComb.size();
		int beg = (int)(pageIdx * ONE_PAGE_SIZE);
		int end = beg + ONE_PAGE_SIZE;
		if(end>=cnt){
			end = cnt;
		}
		tabComb.getItems().addAll(lstComb.subList(beg, end));
		info[2].set(String.format(
			"page %d/%d (%s)", 
			pageIdx+1, pageMax, fmt.format(lstComb.size())
		));
	}
	private void refresh(){
		pageIdx = 0;
		pageMax = 1 + (int)(lstComb.size() / ONE_PAGE_SIZE);
		list2table();
	}
}
