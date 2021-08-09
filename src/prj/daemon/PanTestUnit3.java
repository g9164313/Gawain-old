package prj.daemon;

import java.io.File;
import java.io.IOException;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.regression.GLSMultipleLinearRegression;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanTestUnit3 extends PanBase {
	
	public PanTestUnit3(final Stage stg) {
		try {
			double[][] val = test();
			test2(val);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void test2(final double[][] val) {
		//time(0), volt(1), curr(2), watt(3), mfc-1(4), mfc-2(5), mfc-3(6), rate(7)
		
		//SimpleRegression regression = new SimpleRegression();
		//GLSMultipleLinearRegression regression = new GLSMultipleLinearRegression();
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		
		int idx = 0;
		for(int t=0; t<380; t++) {
			double rate = val[t][7];
			if(rate<=0.) {
				continue;
			}
			idx+=1;
		}
		double[] yy = new double[idx];
		double[][] xx = new double[idx][];
		idx = 0;
		for(int t=0; t<380; t++) {
			double volt = val[t][1];
			double amp  = val[t][2];
			double watt = val[t][3];
			double mfc1 = val[t][4];
			double mfc2 = val[t][5];
			double mfc3 = val[t][6];
			double rate = val[t][7];
			double thick= val[t][8];
			if(rate<=0.) {
				continue;
			}
			yy[idx] = rate;
			xx[idx] = new double[] {volt, amp, mfc1, mfc2, mfc3};
			idx+=1;
		}				
		regression.newSampleData(yy, xx);
		
		double[] beta = regression.estimateRegressionParameters();       
		double[] residuals = regression.estimateResiduals();
		double[][] parametersVariance = regression.estimateRegressionParametersVariance();
		double regressandVariance = regression.estimateRegressandVariance();
		double rSquared = regression.calculateRSquared();
		double sigma = regression.estimateRegressionStandardError();
		
		//---------------------
		
		RealMatrix A = MatrixUtils.createRealMatrix(7, 7);//state
		RealMatrix B = MatrixUtils.createRealIdentityMatrix(7);//control
		RealMatrix Q = MatrixUtils.createRealIdentityMatrix(7);//error covariance
		
		RealMatrix H = MatrixUtils.createRealIdentityMatrix(7);//measurement covariance
		RealMatrix R = MatrixUtils.createRealIdentityMatrix(7);//noise covariance
		
		KalmanFilter filter = new KalmanFilter(
			new DefaultProcessModel(A, B, Q, null, null), 
			new DefaultMeasurementModel(H, R)
		);
		
		int hit_number = 0;
		for(int i=0; i<(val.length-2); i++) {
			
			double volt = val[i][11];
			double amp  = val[i][12];
			double watt = val[i][13];
			double mfc1 = val[i][14];
			double mfc2 = val[i][15];
			double mfc3 = val[i][16];
			double rate0 = val[i+0][17];
			double rate1 = val[i+1][17];
			
			double r_Val = beta[0] + 
				volt*beta[1] + 
				amp *beta[2] + 
				mfc1*beta[3] + 
				mfc2*beta[4] +
				mfc3*beta[5];
			if(Math.abs(rate0-r_Val)<0.05) {
				hit_number+=1;
			}
		}
		
		double hit_rate = (double)hit_number / (val.length-2);
		
		return;
	}
	
	private double[][] test() throws IOException {
		DataFormatter fmt = new DataFormatter();
		File fs = new File(Gawain.pathSock+"製程紀錄-20210625-1416.xlsx");
		Workbook wb = WorkbookFactory.create(fs);
		FormulaEvaluator eva = wb.getCreationHelper().createFormulaEvaluator();
		Sheet sh = wb.getSheetAt(1);
		final int COL_K=0;//10;
		final int COL_S=18;
		double[][] val = new double[sh.getLastRowNum()][];
		for(int j=1; j<=sh.getLastRowNum(); j++) {
			Row rr = sh.getRow(j);
			val[j-1] = new double[COL_S-COL_K+1];
			for(int i=COL_K; i<=COL_S; i++) {
				Cell cc = rr.getCell(i);
				String txt = fmt.formatCellValue(cc, eva);
				String[] col;
				if(txt.length()==0) {
					continue;
				}
				if(i==0 || i==10) {
					col = txt.split(":");
					int hh = Integer.valueOf(col[0]);
					int mm = Integer.valueOf(col[1]);
					double ss = Double.valueOf(col[2]);
					val[j-1][i-COL_K] = hh*60*60 + mm*60 + ss;
				}else{
					txt = txt.replaceAll("[^\\d.]", "");
					val[j-1][i-COL_K] = Double.valueOf(txt);
				}
				//Misc.logv("res=%s", txt);
			}
		}
		wb.close();
		return val;
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final BorderPane lay = new BorderPane();
		return lay;
	}

}
