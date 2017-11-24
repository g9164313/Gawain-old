package prj.daemon;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import narl.itrc.Misc;

public class WidFringeMap extends WidFringeView {

	public WidFringeMap() {
		super();
		init();
	}

	public WidFringeMap(int width, int height) {
		super(width, height);
		init();
	}
	// ----------------------------//

	
	private static final int MAX_TERM = 42;
	
	private final JFXTextField[] boxCoff = new JFXTextField[MAX_TERM];
	private final JFXCheckBox[]  chkCoff = new JFXCheckBox[MAX_TERM];
	
	private DoubleProperty propMaxValue = new SimpleDoubleProperty();
	private DoubleProperty propMinValue = new SimpleDoubleProperty();
	//private DoubleProperty curValue = new SimpleDoubleProperty();
	
	private void init(){
		Insets marg = new Insets(3,7,3,7);
		for(int i=0; i<MAX_TERM; i++){			
			chkCoff[i] = new JFXCheckBox();
			GridPane.setMargin(chkCoff[i], marg);
			GridPane.setHalignment(chkCoff[i], HPos.CENTER);
			
			boxCoff[i] = new JFXTextField();
			boxCoff[i].disableProperty().bind(chkCoff[i].selectedProperty().not());
			boxCoff[i].setMaxWidth(50);
			boxCoff[i].setText("0.");
			boxCoff[i].setOnAction(e->update());
			GridPane.setHalignment(boxCoff[i], HPos.CENTER);
		}
		final Label txtMaxVal = new Label();
		txtMaxVal.textProperty().bind(propMaxValue.asString("%.4f"));
		
		final Label txtMinVal = new Label();
		txtMinVal.textProperty().bind(propMinValue.asString("%.4f"));
		
		layCtrl.add(new Label("RMS：" ), 0,  9);layCtrl.add(new Label(Misc.TXT_UNKNOW) , 1, 9);
		layCtrl.add(new Label("最大："), 0, 10);layCtrl.add(txtMaxVal, 1, 10);
		layCtrl.add(new Label("最小："), 0, 11);layCtrl.add(txtMinVal, 1, 11);
		//layCtrl.add(new Label("圖例："), 0, 10);
		//layCtrl.add(, 0, 11); layCtrl.add(, 0, 11);
	}
		
	public void calculate(){		
		Misc.logv("----[開始解算]----");
		
		int terms = getValidTerms();
		double[][] fng = getFringeDots();
		
		//BB = AA * x, 'x' are coefficients
		RealMatrix AA = new Array2DRowRealMatrix(terms,terms);
		RealVector BB = new ArrayRealVector(terms);
		for(int j=0; j<terms; j++){
			for(int i=0; i<terms; i++){
				double aa = 0.;
				double bb = 0.;
				for(int fd=0; fd<fng.length; fd++){
					double[] dot = fng[fd];
					for(int dd=0; dd<dot.length; dd+=2){
						double dot_x = dot[dd+0];
						double dot_y = dot[dd+1];
						aa = aa + Zernike(i,dot_x,dot_y) * Zernike(j,dot_x,dot_y);
						if(j==0){
							bb = bb + 1. * (fd+1) * Zernike(i,dot_x,dot_y);
						}
					}
				}
				AA.setEntry(j, i, aa);
				if(j==0){
					BB.setEntry(i, bb);
				}				
			}
		}
		try{
			RealVector xx = new LUDecomposition(AA).getSolver().solve(BB);
			for(int i=0; i<terms; i++){
				boxCoff[i].setText(String.format("%.4f", xx.getEntry(i)));
			}
			update();
		}catch(SingularMatrixException e){
			Misc.loge("無法解算");
		}		
	}
	
	public void update(){
		//calculate data~~~
		double max=-Double.MAX_VALUE, min=Double.MAX_VALUE;
		
		for(MaskData dat:cmask.lstData){
			double val = 0.;
			for(int idx=0; idx<MAX_TERM; idx++){
				if(boxCoff[idx].isDisable()==true){
					continue;
				}
				String txtCoff = boxCoff[idx].getText();
				try{					
					double coff = Double.valueOf(txtCoff);
					val = val + coff * Zernike(
						idx,
						dat.normX,dat.normY
					);
				}catch(NumberFormatException e){
					Misc.loge("第 %d 項係數輸入錯誤(%s)", idx, txtCoff);
				}
			}
			dat.model = val;
			if(max<val){
				max = val;
			}
			if(val<min){
				min = val;
			}
		}
		propMaxValue.setValue(max);
		propMinValue.setValue(min);
		Misc.logv("Model Intensity: [%.4f, %.4f]", min, max);
		
		//map intensity to color~~~
		PixelWriter img = getOverlayView().getPixelWriter();
		for(MaskData dat:cmask.lstData){
			int idx = (int)(((dat.model-min)/(max-min)) * 255); 
			dat.color = rainbow[idx];
			img.setArgb(
				dat.locaX, 
				dat.locaY,
				dat.color
			);
		}
	}	
	
	/**
	 * produce Zernike term value
	 * @param n - Zernike number as given by James Wyant
	 * @param X - location X (normalized 0..1)
	 * @param Y - location X (normalized 0..1)
	 * @return term value
	 */
	public static double Zernike(int n, double X, double Y) {

		double X2 = 0., X3 = 0., X4 = 0.;
		double Y2 = 0., Y3 = 0., Y4 = 0.;
		double R2 = 0.;//, R = 0.;
		//double last_x = 0.;
		//double last_y = 0.;
		//int cnt = 0;
		// if (last_x != X || last_y != Y)
		{
			X2 = X * X;
			X3 = X2 * X;
			X4 = X2 * X2;
			//last_x = X;
			Y2 = Y * Y;
			Y3 = Y2 * Y;
			Y4 = Y2 * Y2;
			R2 = X2 + Y2;
			//R = Math.sqrt(R2);
			//last_y = Y;
		}

		double d;
		switch (n) {
		case 0:
			// return(.01 * sin(3 * M2PI * sqrt(R2)));
			return (1.);
		case 1:
			return (X);
		case 2:
			return (Y);
		case 3:
			return (-1. + 2. * R2);
		case 4:
			return (X2 - Y2);
		case 5:
			return (2. * X * Y);
		case 6:
			return (-2. * X + 3. * X * R2);
		case 7:
			return (-2. * Y + 3. * Y * R2);
		case 8:
			return (1. + R2 * (-6. + 6. * R2));
		case 9:
			return (X3 - 3. * X * Y2);
		case 10:
			return (3. * X2 * Y - Y3);
		case 11:
			return (-3. * X2 + 4. * X4 + 3. * Y2 - 4. * Y4);
		case 12:
			return 2. * X * Y * (-3. + 4. * R2);
		case 13:
			return X * (3. + R2 * (-12. + 10. * R2));
		case 14:
			return Y * (3. + R2 * (-12. + 10. * R2));
		case 15:
			return (-1. + R2 * (12. + R2 * (-30. + 20. * R2)));
		case 16:
			return (X4 - 6. * X2 * Y2 + Y4);
		case 17:
			return 4. * X * Y * (X2 - Y2);
		case 18:
			return X * (5. * X4 + 3. * Y2 * (4. - 5. * Y2) - 2 * X2 * (2. + 5. * Y2));
		case 19:
			return Y * (15. * X4 + 4. * Y2 - 5. * Y4 + 2. * X2 * (-6. + 5. * Y2));
		case 20:
			return (X2 - Y2) * (6. + R2 * (-20. + 15. * R2));
		case 21:
			return 2. * X * Y * (6. + R2 * (-20. + 15. * R2));
		case 22:
			return X * (-4. + R2 * (30. + R2 * (-60. + 35. * R2)));
		case 23:
			return Y * (-4. + R2 * (30. + R2 * (-60. + 35. * R2)));
		case 24:
			return (1. + R2 * (-20. + R2 * (90. + R2 * (-140. + 70. * R2))));
		case 25:
			return X * (X4 - 10. * X2 * Y2 + 5. * Y4);
		case 26:
			d = Y * (5. * X4 - 10. * X2 * Y2 + Y4);
			break;
		case 27:
			d = 6. * X4 * X2 - (30. * X2 * Y2) * (-1. + Y2) + Y4 * (-5. + 6. * Y2) - 5 * X4 * (1. + 6. * Y2);
			break;
		case 28:
			d = X * (-20. * X2 * Y + 20. * Y3 + 24. * X2 * Y * R2 - 24 * Y3 * R2);
			break;
		case 29:
			d = X * (10. * X2 - 30. * Y2 + R2 * (-30. * X2 + 90. * Y2 + R2 * (21. * X2 - 63. * Y2)));
			break;
		case 30:
			d = Y * (-10. * Y2 + 30. * X2 + R2 * (30. * Y2 - 90. * X2 + R2 * (-21. * Y2 + 63. * X2)));
			break;
		case 31:
			d = (-10. + R2 * (60. + R2 * (-105. + 56. * R2))) * (X2 - Y2);
			break;
		case 32:
			d = X * Y * (-20. + R2 * (120. + R2 * (-210. + 112. * R2)));
			break;
		case 33:
			d = X * (5. + R2 * (-60. + R2 * (210. + R2 * (-280. + R2 * 126))));
			break;
		case 34:
			d = Y * (5. + R2 * (-60. + R2 * (210. + R2 * (-280. + 126. * R2))));
			break;
		case 35:
			d = -1. + R2 * (30. + R2 * (-210. + R2 * (560. + R2 * (-630. + 252. * R2))));
			break;
		case 36:
			d = X4 * X2 - 15. * X4 * Y2 + 15. * X2 * Y4 - Y4 * Y2;
			break;
		case 37:
			d = 6. * X4 * X * Y - 20. * X3 * Y3 + 6. * X * Y4 * Y;
			break;
		case 38:
			d = -6. * X4 * X + 60. * X3 * Y2 - 30. * X * Y4 + 7. * X4 * X * R2 - 70. * X3 * Y2 * R2 + 35. * X * Y4 * R2;
			break;
		case 39: // Spherical 5
			d = 1. + R2 * (-42. + R2 * (420. + R2 * (-1680. + R2 * (3150. + R2 * (-2772. + 924. * R2)))));
			break;
		case 40: // spherical 6
			d = -1. + R2 * (56.
					+ R2 * (-756. + R2 * (4200. + R2 * (-11550. + R2 * (16632. + R2 * (-12012. + 3432. * R2))))));
			break;
		case 41: // spherical 7
			d = 1. + R2 * (-72. + R2 * (1260.
					+ R2 * (-9240. + R2 * (34650. + R2 * (-72072. + R2 * (84084. + R2 * (-51480. + 12870. * R2)))))));
			// d = zpr(8,0,R);
			break;
		default:
			return (0.);
		}
		return d;
	}
	//---------------------------//
	
	private int getValidTerms(){
		for(int i=MAX_TERM-1; i>=0; --i){
			if(chkCoff[i].isSelected()==true){
				return i+1;
			}
		}
		return 0;
	}
	
	public Pane genPaneZernikePoly(){
		
		final int[] termHead = {0, 4, 9 , 16, 25, 36};
		final int[] termTail = {4, 9, 16, 25, 36, 42};
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("grid-small");		
				
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().add("grid-small");
		
		lay0.visibleProperty().bind(lay1.visibleProperty().not());
		lay1.setVisible(true);
		
		final ComboBox<String> cmbMode = new ComboBox<String>();
		cmbMode.setMaxWidth(Double.MAX_VALUE);
		cmbMode.getItems().addAll("1th-Sphere", "4th", "6th", "8th", "10th", "12th");
		cmbMode.getSelectionModel().select(0);
		cmbMode.setOnAction(e->{			
			int idx = cmbMode.getSelectionModel().getSelectedIndex();
			for(int i=0; i<42; i++){
				if(i<termTail[idx]){
					chkCoff[i].setSelected(true);
				}else{
					chkCoff[i].setSelected(false);
				}				
			}
		});		
		cmbMode.getOnAction().handle(null);//default setting!!!	
		cmbMode.setMaxWidth(113);
		StackPane.setMargin(cmbMode, new Insets(7,64,7,7));
		StackPane.setAlignment(cmbMode, Pos.TOP_RIGHT);
		
		final Button btnShow = new Button("自訂");
		btnShow.setOnAction(e->{
			boolean flag = lay1.isVisible();
			lay1.setVisible(!flag);			
		});
		StackPane.setMargin(btnShow, new Insets(7,7,7,7));
		StackPane.setAlignment(btnShow, Pos.TOP_RIGHT);
		
		//font --> １２３４５６７８９０				
		lay0.addColumn(0, 
			new Label(" 1："),
			new Label(" 5："),
			new Label("10："),
			new Label("17："), 
			new Label("26："),
			new Label("37：")
		);
		lay1.addColumn(0, 
			new Label(" 1："),
			new Label(" 5："),
			new Label("10："),
			new Label("17："), 
			new Label("26："),
			new Label("37：")
		);
		
		for(int row=0; row<termHead.length; row++){
			int pos = 1;//start from the first column
			for(int idx=termHead[row]; idx<termTail[row]; idx++,pos++){
				lay0.add(chkCoff[idx], pos, row);
				lay1.add(boxCoff[idx], pos, row);
			}			
		}
		
		final StackPane layRoot = new StackPane();
		layRoot.getChildren().addAll(lay0,lay1,cmbMode,btnShow);
		return layRoot;
	}
	//---------------------------//
	
	private int[] rainbow = {
			0x80AA00FF, 0x80A700FF, 0x80A300FF, 0x80A000FF, 0x809D00FF, 0x809900FF, 0x809600FF, 0x809300FF, 
			0x808F00FF, 0x808C00FF, 0x808900FF, 0x808500FF, 0x808200FF, 0x807F00FF, 0x807B00FF, 0x807800FF, 
			0x807500FF, 0x807100FF, 0x806E00FF, 0x806B00FF, 0x806700FF, 0x806400FF, 0x806100FF, 0x805D00FF, 
			0x805A00FF, 0x805700FF, 0x805300FF, 0x805000FF, 0x804D00FF, 0x804900FF, 0x804600FF, 0x804300FF, 
			0x803F00FF, 0x803C00FF, 0x803900FF, 0x803500FF, 0x803200FF, 0x802F00FF, 0x802B00FF, 0x802800FF, 
			0x802500FF, 0x802100FF, 0x801E00FF, 0x801B00FF, 0x801700FF, 0x801400FF, 0x801100FF, 0x800D00FF, 
			0x800A00FF, 0x800701FE, 0x800503FC, 0x800305FA, 0x800107F8, 0x80000AF5, 0x80000FF0, 0x800014EB, 
			0x800019E6, 0x80001EE1, 0x800023DC, 0x800028D7, 0x80002DD2, 0x800032CD, 0x800037C8, 0x80003CC3, 
			0x800041BE, 0x800046B9, 0x80004BB4, 0x800050AF, 0x800055AA, 0x80005AA5, 0x80005FA0, 0x8000649B, 
			0x80006996, 0x80006E91, 0x8000738C, 0x80007887, 0x80007D82, 0x8000827D, 0x80008778, 0x80008C73, 
			0x8000916E, 0x80009669, 0x80009B64, 0x8000A05F, 0x8000A55A, 0x8000AA55, 0x8000AF50, 0x8000B44B, 
			0x8000B946, 0x8000BE41, 0x8000C33C, 0x8000C837, 0x8000CD32, 0x8000D22D, 0x8000D728, 0x8000DC23, 
			0x8000E11E, 0x8000E619, 0x8000EB14, 0x8000F00F, 0x8000F50A, 0x8000FA05, 0x8003FC03, 0x8007FD02, 
			0x800BFE01, 0x800FFF00, 0x8014FF00, 0x8019FF00, 0x801EFF00, 0x8023FF00, 0x8028FF00, 0x802DFF00, 
			0x8032FF00, 0x8037FF00, 0x803CFF00, 0x8041FF00, 0x8046FF00, 0x804BFF00, 0x8050FF00, 0x8055FF00, 
			0x805AFF00, 0x805FFF00, 0x8064FF00, 0x8069FF00, 0x806EFF00, 0x8073FF00, 0x8078FF00, 0x807DFF00, 
			0x8082FF00, 0x8087FF00, 0x808CFF00, 0x8091FF00, 0x8096FF00, 0x809BFF00, 0x80A0FF00, 0x80A5FF00, 
			0x80AAFF00, 0x80AFFF00, 0x80B4FF00, 0x80B9FF00, 0x80BEFF00, 0x80C3FF00, 0x80C8FF00, 0x80CDFF00, 
			0x80D2FF00, 0x80D7FF00, 0x80DCFF00, 0x80E1FF00, 0x80E6FF00, 0x80EBFF00, 0x80F0FF00, 0x80F4FE00, 
			0x80F8FE00, 0x80FCFD00, 0x80FFFC00, 0x80FFFA00, 0x80FFF800, 0x80FFF500, 0x80FFF300, 0x80FFF000, 
			0x80FFEE00, 0x80FFEB00, 0x80FFE800, 0x80FFE600, 0x80FFE400, 0x80FFE100, 0x80FFDF00, 0x80FFDC00, 
			0x80FFDA00, 0x80FFD700, 0x80FFD400, 0x80FFD200, 0x80FFD000, 0x80FFCD00, 0x80FFCA00, 0x80FFC800, 
			0x80FFC600, 0x80FFC300, 0x80FFC000, 0x80FFBE00, 0x80FFBC00, 0x80FFB900, 0x80FFB600, 0x80FFB400, 
			0x80FFB200, 0x80FFAF00, 0x80FFAC00, 0x80FFAA00, 0x80FFA800, 0x80FFA500, 0x80FFA200, 0x80FFA000, 
			0x80FF9E00, 0x80FF9B00, 0x80FF9800, 0x80FF9600, 0x80FF9400, 0x80FF9100, 0x80FF8E00, 0x80FF8C00, 
			0x80FF8A00, 0x80FF8700, 0x80FF8400, 0x80FF8200, 0x80FF8000, 0x80FF7D00, 0x80FF7A00, 0x80FF7800, 
			0x80FF7500, 0x80FF7300, 0x80FF7000, 0x80FF6E00, 0x80FF6C00, 0x80FF6900, 0x80FF6600, 0x80FF6400, 
			0x80FF6200, 0x80FF5F00, 0x80FF5C00, 0x80FF5A00, 0x80FF5800, 0x80FF5500, 0x80FF5200, 0x80FF5000, 
			0x80FF4E00, 0x80FF4B00, 0x80FF4800, 0x80FF4600, 0x80FF4400, 0x80FF4100, 0x80FF3E00, 0x80FF3C00, 
			0x80FF3900, 0x80FF3700, 0x80FF3400, 0x80FF3200, 0x80FF3000, 0x80FF2D00, 0x80FF2A00, 0x80FF2800, 
			0x80FF2600, 0x80FF2300, 0x80FF2000, 0x80FF1E00, 0x80FF1B00, 0x80FF1900, 0x80FF1600, 0x80FF1400, 
			0x80FF1200, 0x80FF0F00, 0x80FF0C00, 0x80FF0A00, 0x80FF0800, 0x80FF0500, 0x80FF0200, 0x80FF0000, 
	};
}
