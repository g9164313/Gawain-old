package prj.refuge;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.jfoenix.controls.JFXTextField;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import narl.itrc.BoxLogger;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * A utility change routine for calibrate radiation 
 * @author qq
 *
 */
public class PanEntry1 extends PanBase {

	private DevCDR06  cdr  = new DevCDR06();
	private DevHustIO hust = new DevHustIO();
	private DevAT5350 atom = new DevAT5350();
	
	private WidMarkTable mark = new WidMarkTable(
		DevHustIO.ISOTOPE_NAME[0],
		DevHustIO.ISOTOPE_NAME[1],
		DevHustIO.ISOTOPE_NAME[2]
	);
	
	public PanEntry1(){
		firstAction = FIRST_MAXIMIZED;
	}
	//-------------------------------//
	
	private Node layHust, layAtom;
	
	@Override
	public Node eventLayout(PanBase self) {		
		BorderPane root = new BorderPane();
		root.setCenter(mark);
		root.setRight(layout_ctrl());
		root.setBottom(layout_actn());
		return root;
	}
	
	private class CoffConsole extends GridPane {
		public CoffConsole(){
			getStyleClass().add("table-col-grid");
			
			JFXTextField boxDelta = new JFXTextField(String.format("%.1f", coffDelta));
			boxDelta.setPrefWidth(70);
			boxDelta.textProperty().addListener(event->{
				try{
					coffDelta = Double.valueOf(boxDelta.getText());					
				}catch(NumberFormatException e){
				}			
			});
			
			JFXTextField boxBound = new JFXTextField(String.format("%.5f", coffBound));
			boxBound.setPrefWidth(70);
			boxBound.textProperty().addListener(event->{
				try{
					coffBound = Double.valueOf(boxBound.getText());					
				}catch(NumberFormatException e){
				}			
			});
			
			JFXTextField boxStdCV = new JFXTextField(String.format("%.2f", coffStdCV));
			boxStdCV.setPrefWidth(70);
			boxStdCV.textProperty().addListener(event->{
				try{
					coffStdCV = Double.valueOf(boxBound.getText());					
				}catch(NumberFormatException e){
				}			
			});
			
			this.addRow(0, new Label("相對距離修正（mm）"), boxDelta);
			this.addRow(1, new Label("邊界值範圍"), boxBound);
			this.addRow(2, new Label("相對標準差要求（%）"), boxStdCV);
		}
	};
	
	private Node layout_ctrl(){
		VBox lay0 = new VBox();
		layHust = hust.eventLayout(this);
		layAtom = atom.eventLayout(this);
		lay0.getStyleClass().add("vbox-one-direction");
		lay0.getChildren().addAll(			
			new TitledPane("HUST-IO", layHust),
			new TitledPane("AT5350" , layAtom),
			new TitledPane("環境參數",new CoffConsole())
		);
		return lay0;
	}
	
	private String moveDecPoint(String txt, int step, boolean isRight){
		try{
			BigDecimal dec = new BigDecimal(txt);
			if(isRight==true){
				dec = dec.movePointRight(step);
			}else{
				dec = dec.movePointLeft(step);
			}			
			return dec.toString();
		}catch(NumberFormatException e){
			return txt;
		}
	}	
	
	//private static double coffDecay = 0.97716;	
	private static double coffDelta = 700.;//unit is "mm"
	private static double coffBound = 0.025;
	private static double coffStdCV = 3.5;//unit is '%'
	
	class TskAutoMEasure extends Task<Void>{
		
		private double predict1(double posVal, double pinVal, double radi){			
			double val = pinVal / radi;
			val = val * (posVal+coffDelta) * (posVal+coffDelta);
			posVal = Math.sqrt(val) - coffDelta;
			return posVal;
		}
		@Override
		protected Void call() throws Exception {
			
			int cntCompensate = 0;
			
			for(int ss=0; ss<mark.getSheetSize(); ss++){
				final int IDX_SHEET = ss;				
				
				final ArrayList<String> lstLoca = new ArrayList<String>();	
				final ArrayList<Double> lstRadi = new ArrayList<Double>();				
				Misc.invoke(prepareList->{					
					mark.getSheetCurLoca(IDX_SHEET,lstLoca);
					mark.getSheetCurRadi(IDX_SHEET,lstRadi);
				});

				Misc.invoke( stpFlip->{
					Misc.logv("HustIO) 開始測試%s", DevHustIO.ISOTOPE_NAME[IDX_SHEET]);
					mark.getSelectionModel().select(IDX_SHEET);
				});
				
				for(int rr=0; rr<lstLoca.size(); rr++){

					String txtLoca = lstLoca.get(rr);
					try{
						if(Double.valueOf(txtLoca)<=10.01){
							Misc.logv("忽略第 %d 筆位置",rr+1);
							continue;
						}
					}catch(NumberFormatException e){
						Misc.logw("不合法的數字格式 - %s", txtLoca);
						continue;
					}
					
					//check whether we need compensation!!!!
					if((cntCompensate%10)==0){
						Misc.logv("HustIO) 歸零中...");
						hust.syncMoveTo(-1);				
						atom.syncCompensate();
					}
					cntCompensate++;
					
					double radi = lstRadi.get(rr);
					double rad1 = radi - radi * coffBound;
					double rad2 = radi + radi * coffBound;

					double curLoca = Double.valueOf(txtLoca)*10.;//input unit is 'cm'
					
					double finStdv = Double.MAX_VALUE;//final best CV stddev;
					
					Misc.logv("第 %d 筆起始位置 %s mm",rr+1,txtLoca);
					int maxAdjust = 20;
					do{
						curLoca = hust.syncMoveTo(curLoca);//Strange problem, why padding zero????
						
						Misc.logv("移動至 %.4f mm，開啟射源，等待穩定中",curLoca);
						hust.radiStart(ss);
						Misc.delay(10*1000);//let it stable!!!

						cdr.updateLastValue();
						String temp = cdr.lastValue[2];
						String press= cdr.lastValue[0];
						
						Misc.logv("開始量測，溫度：%s，壓力：%s",temp,press);
						atom.syncMeasure(temp,press);

						Misc.logv("結束量測");
						hust.syncRadiStop();
												
						double curRadi = atom.measAverage * 60.;//unit is uSv/min
						double curStdv = (atom.measStddev/atom.measAverage)*100;
						Misc.logv("確認劑量 %.4f mm @ [%.3f, %.3f, %.3f], CV:%.2f%%",
							curLoca,
							rad1, curRadi, rad2,
							curStdv
						);
						if(curStdv<finStdv){
							finStdv = curStdv;
							update_table(ss,rr,curLoca);
						}
						if(rad1<=curRadi && curRadi<=rad2 && curStdv<coffStdCV){
							break;
						}
						maxAdjust--;
						
						curLoca = predict1(curLoca, curRadi, radi);
						if(Double.valueOf(curLoca)<=0.){
							break;
						}
						Misc.logv("下次預估位置：%.4f mm",curLoca);
					}while(maxAdjust>0);
					
					Misc.logv("--------------------------");
				}				
			}
			
			Misc.logv("========================");
			Misc.invoke(stpFinal->{
				mark.saveExcel(new File(Gawain.pathHome+"report.xls"));
				tskMeas = null;//reset it!!!
				layHust.disableProperty().set(false);
				layAtom.disableProperty().set(false);
			});
			return null;
		}
		
		private void update_table(
			final int IDX_SHEET, 
			final int IDX_RECORD, 
			final double loca_mm
		){
			Misc.invoke(stpGetList->{
				mark.setCurLoca(
					IDX_SHEET, IDX_RECORD,
					String.format("%.4f", loca_mm/10.)
				);
				mark.clearValue(IDX_SHEET, IDX_RECORD);						
				for(String txt:atom.measValue){
					try{
						mark.addValue(
							IDX_SHEET, IDX_RECORD,
							moveDecPoint(txt,6,true)
						);
					}catch(NumberFormatException e){
						Misc.logw("Wrong format: %s ",txt);
					}							
				}
				mark.updateValue(IDX_SHEET, IDX_RECORD);
			});
		}
	};
	
	private Task<Void> tskMeas;
	
	private Node layout_actn(){

		Node nd1 = new TitledPane("溫溼度計", cdr.layout_grid());		
		Node nd2 = new TitledPane("訊息紀錄",new BoxLogger());
		HBox.setHgrow(nd2, Priority.ALWAYS);
		
		final Button btnLoadRec = PanBase.genButton2("匯入","toc.png");
		btnLoadRec.setMinWidth(93);
		btnLoadRec.setOnAction(event->{
			FileChooser dia = new FileChooser();
			dia.setTitle("匯入 Excel");
			dia.setInitialDirectory(Gawain.dirHome);
			File fs = dia.showOpenDialog(Misc.getParent(event));
            if(fs!=null){
            	mark.loadExcel(fs);
            }
		});
		
		final Button btnSaveRec = PanBase.genButton2("匯出","toc.png");
		btnSaveRec.setMinWidth(93);
		btnSaveRec.setOnAction(event->{
			FileChooser dia = new FileChooser();
			dia.setTitle("匯出 Excel");
			dia.setInitialDirectory(Gawain.dirHome);
            File fs = dia.showSaveDialog(Misc.getParent(event));
            if(fs!=null){
            	mark.saveExcel(fs);
            }
		});
		
		final Button btnKickOff = PanBase.genButton3("量測","arrow-right-drop-circle-outline.png");
		btnKickOff.setMinWidth(93);
		btnKickOff.setMinHeight(93);		
		btnKickOff.setOnAction(event->{
			if(tskMeas!=null){
				if(tskMeas.isRunning()==true){
					Misc.logw("忙碌中");
					return;
				}
			}else{
				tskMeas = new TskAutoMEasure();
			}
			layHust.disableProperty().set(true);
			layAtom.disableProperty().set(true);
			new Thread(tskMeas,"Auto-Measure").start();
		});
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("hbox-medium");
		lay1.getChildren().addAll(
			btnLoadRec,
			btnSaveRec,
			btnKickOff
		);
		lay0.getChildren().addAll(nd1, nd2, lay1);
		return lay0;
	}

	@Override
	public void eventShown(Object[] args) {
		//cdr.connect("");
		//cdr.layout_grid();//re-layout again!!!
		//cdr.update_auto(true);
	}
}
