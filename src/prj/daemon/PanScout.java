package prj.daemon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import narl.itrc.ButtonEx;
import narl.itrc.PanBase;

public class PanScout extends PanBase {
	
	private final static DecimalFormat fmt = new DecimalFormat("###,###,###,###,###");

	private ArrayList<int[]> lstComb = new ArrayList<int[]>();
	
	private StringProperty propState = new SimpleStringProperty();
	
	private ObservableList<Label> lstResult; 
	
	private int pageIndex = 0;
	private final int PAGE_SIZE = 100;
	
	private void list_result(){
		lstResult.clear();
		int size = lstComb.size();
		if(size>=PAGE_SIZE){
			size = PAGE_SIZE;
		}
		for(int i=pageIndex; i<(pageIndex+size); i++){
			int[] val = lstComb.get(i);
			String txt = "";
			for(int j=0; j<val.length; j++){
				txt = txt + String.format("%02d, ", val[j]);
			}
			lstResult.add(new Label(txt));
		}
	}
	
	private class TaskCreateCombo extends Task<Void> {
		Combinations binc;
		long size,indx;
		TaskCreateCombo(int n, int k){
			binc = new Combinations(n,k);
			size = CombinatoricsUtils.binomialCoefficient(n, k);
			lstComb.clear();		
			lstComb.ensureCapacity((int)size);
			propState.set(String.format(
				"%d choose %d, total=%s", 
				n, k, fmt.format(size) 
			));
		}
		@Override
		protected Void call() throws Exception {
			binc.forEach(val->{			
				for(int i=0; i<val.length; i++){
					val[i] = val[i] + 1;//bias~~~~
				}			
				lstComb.add(val);				
				updateProgress(++indx, size);
			});
			Application.invokeAndWait(()->{
				pageIndex = 0;//reset it~~~
				list_result();
			});
			return null;
		}
	};
	
	public PanScout(){
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		//show_all_coeff();
		
		final Label txtState = new Label();
		txtState.getStyleClass().add("layout-small");
		txtState.textProperty().bind(propState);
		
		final ListView<Label> lst2 = new ListView<Label>();
		lstResult = lst2.getItems();
		
		final ListView<TextField> lst1 = new ListView<TextField>();
		//lst.setMinSize(200, 480);
		lst1.setOnMouseClicked(e->{
			if(e.getButton()!=MouseButton.PRIMARY){
				return;
			}
			if(e.getClickCount()<2){
				return;
			}
			lst1.getItems().add(new TextField());
		});

		final HBox lay1 = new HBox();
		lay1.getStyleClass().add("layout-small");
		lay1.getChildren().addAll(lst1, lst2);
		
		final ButtonEx btn1 = new ButtonEx(
			"test","file-search-outline.png"
		).setOnClick(event->{
			spin.kick('p', new TaskCreateCombo(49,6));
		});
		
		final ButtonEx btnPrv = new ButtonEx(
			"<<","file-search-outline.png"
		).setOnClick(event->{
			int delta = pageIndex - PAGE_SIZE;
			if(delta<0){
				return;
			}
			pageIndex = delta;
			list_result();
		});		
		final ButtonEx btnNxt = new ButtonEx(
			">>","file-search-outline.png"
		).setOnClick(event->{
			int delta = pageIndex + PAGE_SIZE;
			if(delta>=lstComb.size()){
				return;
			}
			pageIndex = delta;
			list_result();
		});
		
		final ToolBar bar = new ToolBar(
			btn1,
			new Separator(Orientation.VERTICAL),
			btnPrv,
			btnNxt
		);		
		final BorderPane lay0 = new BorderPane();
		lay0.setTop(bar);
		lay0.setCenter(lay1);
		lay0.setBottom(txtState);
		return lay0;
	}

	@Override
	public void eventShown(Object[] args) {
		//spin.kick('p', new TaskCreateCombo(49,6));
	}
}
