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
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
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
import narl.itrc.BoxLogger;
import narl.itrc.ButtonEx;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanBeaker extends PanBase {
	
	private final static DecimalFormat fmt = new DecimalFormat("###,###,###,###,###");

	public PanBeaker(){

	}
	
	private void eventShown(){
	}
	
	private void generate_data(){
		final int n = 49;
		final int k = 6;
		Combinations comb = new Combinations(n,k);
		duty.initProgress(
			0, 1,
			CombinatoricsUtils.binomialCoefficient(n,k)
		);
		for(int[] cc:comb){
			
			duty.incProgress();
		}
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		stage().setOnShown(e->eventShown());
		
		//information bar
		//final Label[] txtInfo = new Label[info.length];
		//final HBox lay1 = new HBox(7);
		//lay1.getChildren().addAll(txtInfo);
		
		final ButtonEx btn1 = new ButtonEx(
			"製造","coffee-to-go.png"
		).setOnClick(e1->doDuty(()->generate_data()));
		
		final ButtonEx btn2 = new ButtonEx(
			"process.1","coffee-to-go.png"
		).setOnClick(e->{
		});
		final ButtonEx btn3 = new ButtonEx(
			"process.2","coffee-to-go.png"
		).setOnClick(e->{
		});
		
		final ToolBar bar = new ToolBar(			
			btn1,
			btn2,
			btn3,
			new Separator(Orientation.VERTICAL)
		);
		return new BorderPane(
			null,
			bar,null,
			null,null
		);
	}
	//----------------------------------//
	
	private void read_data(final File fs){
		try {
			BufferedReader rd = new BufferedReader(new FileReader(fs));
			String txt;
			while((txt = rd.readLine())!=null){
				txt = txt.trim();
				if(txt.charAt(0)=='#'){
					continue;
				}
				if(txt.matches("[\\d]+[)]\\s*([\\d]+\\s*)(\\g<1>)*")==true){
					continue;
				}
				String[] args = txt.trim().split("[,\\s]+");
				int[] val = new int[7];
				for(int i=0; i<val.length; i++){
					val[i] = Misc.txt2int(args[i]);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
