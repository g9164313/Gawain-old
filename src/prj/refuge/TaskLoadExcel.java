package prj.refuge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import narl.itrc.WidTextSheet;


public class TaskLoadExcel extends Task<Void>{

	public File file = null;
		
	public WidTextSheet[] sheet = null;
	
	public ButtonType nmark = null;
	
	private final int begCol = 1;
	private final int endCol = 20;
	
	public TaskLoadExcel(){
	}
	
	@Override
	protected Void call() throws Exception {
		
		updateProgress(0, 3*20);
		
		try {

			FileInputStream stm = new FileInputStream(file);
			
			HSSFWorkbook bok = new HSSFWorkbook(stm);
			
			DataFormatter fmt = new DataFormatter();
			
			FormulaEvaluator eva = bok.getCreationHelper().createFormulaEvaluator();
			
			for(int s=0; s<3; s++){
				
				HSSFSheet sht = bok.getSheetAt(s);

				for(int i=begCol, dstCol=1; i<=endCol; i++, dstCol++){
					
					updateProgress(s*20+i, 3*20);
					
					//first reset all values...
					sheet[s].asynSetValue(dstCol, 1, "");
					sheet[s].asynSetValue(dstCol, 2, "");
					sheet[s].asynSetValue(dstCol, 3, "");
					sheet[s].asynSetValue(dstCol, 4, "");
					
					//start to reload values~~~
					HSSFCell celDose, celLoca;
					
					if(nmark==ButtonType.OK){
						celDose = sht.getRow(5).getCell(i);// 1 year dose rate  
						celLoca = sht.getRow(7).getCell(i);// 1 year location
					}else{
						celDose = sht.getRow(4).getCell(i);
						celLoca = sht.getRow(6).getCell(i);
					}
					if(celDose==null || celLoca==null){						
						continue;
					}
					
					String txtDose = fmt.formatCellValue(celDose, eva); 
					String txtLoca = fmt.formatCellValue(celLoca, eva);
					
					if(txtDose.startsWith("#") || txtLoca.startsWith("#")){
						continue;
					}
					sheet[s].asynSetValue(dstCol, 1, txtDose);
					sheet[s].asynSetValue(dstCol, 3, txtLoca);
					
					double val;
					
					val = PanEntry2.formularNextYearDose(celDose.getNumericCellValue());
					if(val>=0.){
						sheet[s].asynSetValue(
							dstCol, 2, 
							String.format("%.4f", val)
						);
					}
					
					val = PanEntry2.formularNextYearLoca(celLoca.getNumericCellValue());
					if(val>=0.){
						sheet[s].asynSetValue(
							dstCol, 4, 
							String.format("%.4f", val)
						);
					}			
				}				
			}
			
			bok.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
