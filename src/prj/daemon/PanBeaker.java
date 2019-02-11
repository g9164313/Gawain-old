package prj.daemon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

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
	

	//private final File data_base = new File("c:\\\\Users\\qq\\Desktop\\lotter.csv");
	
	private final static DecimalFormat fmt = new DecimalFormat("###,###,###,###,###");

	private static class Ballot {
		String txt = "";
		int[] val;
		Ballot(int[] data){
			val = data;
			for(int i=0; i<val.length; i++){
				val[i] = val[i] + 1;//bias~~~~
			}
		}
		int getRepetition(){
			int max = 0;
			for(int i=0,j=0; i<val.length-1; i++){
				int cnt = 0;
				int aa = val[i];
				for(j=i+1; j<val.length; j++){
					int bb = val[j];
					if(Math.abs(aa-bb)!=1){		
						break;
					}
					cnt+=1;
					aa = bb;//for next turn~~~
				}
				i = j;
				if(cnt>max){
					max = cnt;
				}
			}
			return max;
		}
		//rank-0, it means "equality"
		int getRank(final int[] target){
			int cnt = 0;
			for(int i=0; i<target.length; i++){
				for(int j=0; j<val.length; j++){
					if(target[i]==val[j]){
						cnt++;
						break;
					}
				}
			}
			return val.length-cnt;
		}
	}; 
	
	private static class ColValue implements 
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
	
	private ArrayList<Ballot> lstComb = new ArrayList<Ballot>();
	
	private TableView<Ballot> tabComb = new TableView<Ballot>();
	
	private StringProperty[] info = {
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty()
	};
		
	private void init_table(int k, long size){
		pageIdx = 0;
		pageMax = 1 + (int)(size / ONE_PAGE_SIZE);
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
	
	
	private class TaskMakeCombo extends Task<Void> {
		Combinations binc;
		long work,size;
		TaskMakeCombo(int n, int k){			
			binc = new Combinations(n,k);
			size = CombinatoricsUtils.binomialCoefficient(n, k);
			lstComb.clear();		
			lstComb.ensureCapacity((int)size);
			init_table(k,size);
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
	
	private class TaskRedunType1 extends Task<Void> {
		int work = 0;
		int size = lstComb.size();
		TaskRedunType1(){
		}
		@Override
		protected Void call() throws Exception {
			updateProgress(0, size);
			final ArrayList<Ballot> ref = new ArrayList<Ballot>();
			lstComb.forEach(itm->{
				int[] diff = {
					itm.val[1] - itm.val[0],
					itm.val[2] - itm.val[1],
					itm.val[3] - itm.val[2],
					itm.val[4] - itm.val[3],
					itm.val[5] - itm.val[4],
				};
				Arrays.sort(diff);
				if( (diff[0]!=diff[1]) &&
					(diff[1]!=diff[2]) &&
					(diff[2]!=diff[3]) &&
					(diff[3]!=diff[4])
				){
					ref.add(itm);
				}
				updateProgress(++work, size);
			});
			lstComb = ref;
			Application.invokeAndWait(()->refresh());
			return null;
		}
	};
	
	private class TaskRedunType2 extends Task<Void> {
		int minRank = 0;
		int work = 0;
		int size = lstComb.size();
		int[] target;
		TaskRedunType2(int rank,int[] tar){
			minRank= rank;
			target = tar;
		}
		@Override
		protected Void call() throws Exception {
			final ArrayList<Ballot> ref = new ArrayList<Ballot>();			
			lstComb.forEach(itm->{
				if(itm.getRank(target)>=minRank){
					ref.add(itm);
				}
				updateProgress(++work, size);
			});
			lstComb = ref;
			Application.invokeAndWait(()->refresh());
			return null;
		}
	};
	
	private class TaskCluster extends Task<Void> {
		int work = 0;
		int size = lstComb.size();
		TaskCluster(){
		}
		@Override
		protected Void call() throws Exception {			
			lstComb.forEach(itm->{
				
				updateProgress(++work, size);
			});
			Application.invokeAndWait(()->refresh());
			return null;
		}
	};
	
	public PanBeaker(){
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
			final TextField box1 = new TextField();
			final TextField box2 = new TextField();
			final GridPane lay = new GridPane();
			lay.addRow(0, new Label("N："), box1);
			lay.addRow(1, new Label("K："), box2);
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.getDialogPane().setContent(lay);
			if(dia.showAndWait().get()==ButtonType.OK){
				int n = Integer.valueOf(box1.getText().trim());
				int k = Integer.valueOf(box2.getText().trim());
				spin.kick('p', new TaskMakeCombo(n,k));
			}
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
		/*final ButtonEx btnWork = new ButtonEx(
			"蒸餾","coffee-to-go.png"
		).setOnClick(e->{	
		});
		final ButtonEx btnText = new ButtonEx(
			"腳本","file-document.png"
		).setOnClick(e->{
			script = chooseFile("選取腳本");
			if(script!=null){
				getStage().setTitle("腳本："+script.getName());
			}else{
				getStage().setTitle("");
			}
		});*/

		final ButtonEx btn1 = new ButtonEx(
			"remove-1","coffee-to-go.png"
		).setOnClick(e->{
			/*final TextField box1 = new TextField();
			final GridPane lay = new GridPane();
			lay.addRow(0, new Label("repeat："), box1);
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.getDialogPane().setContent(lay);
			if(dia.showAndWait().get()==ButtonType.OK){
				int rr = Integer.valueOf(box1.getText().trim());
				spin.kick('p', new TaskRedunType1(rr));
			}*/
			spin.kick('p', new TaskRedunType1());
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
				int rank = Integer.valueOf(box1.getText().trim());
				int[] target = text2int(box2.getText().trim());
				spin.kick('p', new TaskRedunType2(rank,target));
			}
		});
		final ButtonEx btn3 = new ButtonEx(
			"group","coffee-to-go.png"
		).setOnClick(e->{
			spin.kick('p', new TaskCluster());
		});
		
		final ToolBar bar = new ToolBar(
			btnMake,
			new Separator(Orientation.VERTICAL),
			btnPrv,
			btnNxt,
			//new Separator(Orientation.VERTICAL),
			//btnWork,
			//btnText,
			new Separator(Orientation.VERTICAL),
			btn1,
			btn2,
			btn3
		);		
		final BorderPane lay0 = new BorderPane();
		lay0.setTop(bar);
		lay0.setCenter(tabComb);
		lay0.setBottom(lay1);
		return lay0;
	}

	@Override
	public void eventShown(Object[] args) {
		spin.kick('p', new TaskMakeCombo(49,6));
		//spin.kick('p', new TaskMakeCombo(16,4));
		//spin.kick('p', new TaskRedunType1(data_base));
	}
	
	private int[] text2int(String txt){
		String[] arg = txt.trim().split("[,\\s]");
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		for(int i=0; i<arg.length; i++){
			try{
				tmp.add(Integer.valueOf(arg[i]));
			}catch(NumberFormatException e){
				continue;
			}
		}
		int[] res = new int[tmp.size()];
		for(int i=0; i<res.length; i++){
			res[i] = tmp.get(i).intValue();
		}
		return res;
	}	
}
