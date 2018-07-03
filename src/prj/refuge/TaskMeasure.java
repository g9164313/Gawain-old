package prj.refuge;

import java.util.ArrayList;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.WidTextSheet;

public class TaskMeasure extends Task<Void> {
	
	private static final int ISOTOPE_UNKNOW= -1;
	private static final int ISOTOPE_3CI   = 0;
	private static final int ISOTOPE_0_5CI = 1;
	private static final int ISOTOPE_0_05CI= 2;
	
	private static class Mark {
		/**
		 * code name for isotope
		 * 0 --> 3Ci
		 * 1 --> 0.5Ci
		 * 2 --> 0.05Ci
		 */
		public int isotope = ISOTOPE_3CI;
		/**
		 * move platform to this location, and accept radiation.<p>
		 * Each location apply to one dose text.<p>
		 */
		public ArrayList<String> location = new ArrayList<String>();
		/**
		 * raw data from the instrument, AT5350.<p>
		 * Each raw data apply to one location.<p>
		 */
		public ArrayList<String> dose_txt = new ArrayList<String>();
		/**
		 * target value, we must try to meet this value.
		 */
		public String goal_val;
	};
	
	private ArrayList<Mark> lstMark = new ArrayList<Mark>();
	
	private TaskSandbox sandbox;
	
	public TaskMeasure(WidTextSheet[] sheet, TaskSandbox sbox){
		
		sandbox = sbox;
		
		prepare_flat_list(sheet);
	}
	
	@Override
	protected Void call() throws Exception {
		
		int total_mark = lstMark.size();
		int cur_isotope = ISOTOPE_UNKNOW;
		
		//process all mark point~~~
		for(int i=0; i<total_mark; i++){
			
			updateProgress(i, total_mark);
		
			Mark itm = lstMark.get(i);
		
			//select radiation source
			if(cur_isotope!=itm.isotope){
				switch(itm.isotope){
				case ISOTOPE_3CI:
					sandbox.sendScript("m.keyin(\"\\u2002\\u2002\\u2002\");\n",null);
					break;
				case ISOTOPE_0_5CI:
					sandbox.sendScript("m.keyin(\"\\u2003\\u2003\\u2003\");\n",null);
					break;
				case ISOTOPE_0_05CI:
					sandbox.sendScript("m.keyin(\"\\u2004\\u2004\\u2004\");\n",null);
					break;
				}
				itm.isotope = cur_isotope;//update it, for next turn~~~
			}			
			if(itm.goal_val.length()==0){
				Misc.logv("%s) 忽略第 %d 筆標記", idx2name(itm.isotope), i+1);
				continue;
			}
			
			hit_mark(itm);
		}		
		return null;
	}
	
	/**
	 * try to get a good measurement ~~~
	 * @param itm - data mark 
	 */
	private void hit_mark(Mark itm){
		int cnt = 0;
		do{
			String txt = itm.location.get(cnt);
			if(txt.length()==0){
				break;
			}			
			
			//step.1 - wait motor to start, and move platform.
			sandbox.sendScript(
				"m.ketin(\""+txt+"\");\n"+
				"m.ketin(\""+"\");\n", 
				null)
			.waitForDone();
			
			//step.2 - dig up the information of temperature and moisture
			//step.2 - start radiation
			//step.3 - kick AT5350 to measure dose rate.
			//step.4 - stop radiation
			//step.5 - if not meet the goal, guess the next location.
			itm.location.add("");
			cnt++;
		}while(cnt<=10);
	}

	private void prepare_flat_list(WidTextSheet[] sheet){
		
		for(int i=0; i<sheet.length; i++){
			
			int cnt = sheet[i].getSizeColumn();
			
			for(int j=0; j<cnt; j++){
				
				Mark itm = new Mark();
				itm.isotope = i;
				itm.goal_val = sheet[i].getValue(j+1,1);
				itm.location.add(sheet[i].getValue(j+1,3));

				lstMark.add(itm);
			}
		}
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
