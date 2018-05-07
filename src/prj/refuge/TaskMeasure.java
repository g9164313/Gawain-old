package prj.refuge;

import java.util.ArrayList;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.WidTextSheet;

public class TaskMeasure extends Task<Void> {


	private static class Mark{
		/**
		 * code name for isotope
		 * 0 --> 3Ci
		 * 1 --> 0.5Ci
		 * 2 --> 0.05Ci
		 */
		public int isotope = 0;
		/**
		 * keep the location, this value will be updated.
		 */
		public double location;
		/**
		 * measurement result, it is averaged value from raw data.
		 */
		public double dose_avg;
		/**
		 * measurement result, it is deviation value from raw data.
		 */
		public double dose_dev;
		/**
		 * target value, we must try to meet this value.
		 */
		public double goal_val;
		/**
		 * raw data from the instrument, AT5350.
		 */
		public String raw_data = "";
	};
	
	private ArrayList<Mark> lstMark = new ArrayList<Mark>();
	
	public TaskMeasure(WidTextSheet[] sheet){
		
		String txt = null;
		
		//create a flat list~~~
		for(int i=0; i<sheet.length; i++){
			
			int cnt = sheet[i].getSizeColumn();
			
			for(int j=0; j<cnt; j++){
				
				Mark itm = new Mark();
				
				itm.isotope = i;
				
				txt = sheet[i].getValue(j+1,1);
				if(txt.length()!=0){
					itm.goal_val = Double.valueOf(txt);
				}else{
					itm.goal_val = -1.;
				}
				
				txt = sheet[i].getValue(j+1,3);
				if(txt.length()!=0){
					itm.location = Double.valueOf(txt);
				}else{
					itm.location = -1.;
				}
				
				lstMark.add(itm);
			}
		}
	}
	
	@Override
	protected Void call() throws Exception {
		
		int total = lstMark.size();
				
		for(int i=0; i<total; i++){
			
			updateProgress(i, total);
		
			Mark itm = lstMark.get(i);
			
			if(itm.location<0.){
				Misc.logv("%s) 忽略第 %d 筆標記", idx2name(itm.isotope), i+1);
				continue;
			}
			
		}		
		return null;
	}
	
	public String idx2name(int code){
		switch(code){
		case 0: return "3Ci";
		case 1: return "0.5Ci";
		case 2: return "0.05Ci";
		}
		return "???";
	}
	
	public String simulation(){
		return "";
	}	
}
