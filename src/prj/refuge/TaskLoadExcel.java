package prj.refuge;

import java.io.File;

import javafx.concurrent.Task;
import narl.itrc.WidTextSheet;

public class TaskLoadExcel extends Task<Void>{

	public File file = null;
	
	public int rowIndex = 0;
	
	public WidTextSheet[] sheet = null;
		
	public TaskLoadExcel(){
	}
	
	@Override
	protected Void call() throws Exception {
		
		return null;
	}
}
