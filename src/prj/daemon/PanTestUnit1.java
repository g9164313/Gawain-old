package prj.daemon;

import java.io.File;
import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanTestUnit1 extends PanBase {

	public PanTestUnit1(final Stage stg) {
		super(stg);
		File fs = new File(Gawain.pathSock + "monitor-20200905-1127.xlsx");
		try {
			DataFormatter fmt = new DataFormatter();
			
			Workbook wb = WorkbookFactory.create(fs);
			
			Sheet sh0 = wb.getSheetAt(0);

			for(int rr=1; rr<sh0.getLastRowNum(); rr++) {
				String val = get_cell(sh0, 1, 0, fmt);
			}

			wb.close();
		} catch (EncryptedDocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String get_cell(
		final Sheet sheet,
		final int xx,
		final int yy,
		final DataFormatter fmt
	){
		Row rr = sheet.getRow(yy);
		if(rr==null){ rr = sheet.createRow(yy); }
		Cell cc = rr.getCell(xx);
		if(cc==null){ cc = rr.createCell(xx); }
		return fmt.formatCellValue(cc);
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		final BorderPane lay0 = new BorderPane();
		return lay0;
	}
}
