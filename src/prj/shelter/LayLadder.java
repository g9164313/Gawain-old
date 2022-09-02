package prj.shelter;

import java.util.ArrayList;
import java.util.Optional;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import narl.itrc.Gawain;
import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class LayLadder extends Ladder {
	
	public LayLadder(){

		epilogue = event_finally;
		
		addStep("分隔線", Stepper.Sticker.class);
		addStep("原點補償", RadiateStep.Reset.class);
		addStep(RadiateLoca.NAME, RadiateLoca.class);
		addStep(RadiateDose.NAME, RadiateDose.class);
		genButton("校正程序",e->{
			final DevHustIO.Strength[] lst_ss = quest_strength();
			final String M_TIME = quest_meas_time();
			if(lst_ss==null||M_TIME==null) {
				return;
			}
			
			Stepper stp;
			for(DevHustIO.Strength ss:lst_ss) {
				
				stp = genStep(Stepper.Sticker.class);
				((Stepper.Sticker)stp).setValues("[  校正"+ss.toString()+"  ]");
				
				stp =genStep(RadiateStep.Reset.class);
				((RadiateStep.Reset)stp).setValue(true,true,false);
				
				for(String loca:def_location) {
					stp = genStep(RadiateLoca.class);
					((RadiateLoca)stp).setValues(loca, M_TIME, ss, true, true);
				}
				int cnt = 0;
				for(String dose:def_doserate(ss,true)) {
					if(cnt%2==0) { 
						stp = genStep(Stepper.Sticker.class);
						((Stepper.Sticker)stp).setValues(String.format("%02d", cnt+1));
					}
					cnt++;
					stp = genStep(RadiateDose.class);
					((RadiateDose)stp).setValues(dose, M_TIME, ss, true, true, true);					
				}
			}			
		});
		
		//main_kits.getChildren().addAll(	new Separator());
	}
	
	private DevHustIO.Strength[] quest_strength(){
		
		final ChoiceDialog<String> dia = new ChoiceDialog<String>("All","3Ci","0.05Ci","0.5Ci");
		dia.setTitle("選取劑量");
		dia.setHeaderText("");
		dia.setContentText("");
		final Optional<String> opt = dia.showAndWait(); 
		if(opt.isPresent()==false) {
			return null;
		}
		
		if(opt.get().equals("0.05Ci")) {
			return new DevHustIO.Strength[] { DevHustIO.Strength.V_005Ci };
		}else if(opt.get().equals("0.5Ci")) {
			return new DevHustIO.Strength[] { DevHustIO.Strength.V_05Ci };
		}else if(opt.get().equals("3Ci")) {
			return new DevHustIO.Strength[] { DevHustIO.Strength.V_3Ci };
		}else if(opt.get().equals("All")) {
			return DevHustIO.Strength.values();
		}
		return null;
	}
	private String quest_meas_time(){
		final TextInputDialog dia = new TextInputDialog("5:00");
		dia.setTitle("照射時間");
		dia.setHeaderText("");
		dia.setContentText("");
		final Optional<String> opt = dia.showAndWait();
		if(opt.isPresent()==false) {
			return null;
		}
		return opt.get();
	}
	
	private final static String[] def_location = {
		 "50 cm","100 cm","150 cm","200 cm",
		 "250 cm","300 cm","350 cm","400 cm",
	};	

	private final static String[] DEF_V_3CI = {
		"10250.3 uSv/hr", "9990.8 uSv/hr",
		 "8260.3 uSv/hr", "7990.8 uSv/hr",
		 "5165.3 uSv/hr", "4990.8 uSv/hr",
		 "4131.3 uSv/hr", "3980.8 uSv/hr",
		 "2075.3 uSv/hr", "1990.8 uSv/hr",
		 "1040.3 uSv/hr",  "990.8 uSv/hr",
		  "827.3 uSv/hr",  "790.8 uSv/hr",
		  "519.3 uSv/hr",  "490.8 uSv/hr",
		  "416.3 uSv/hr",  "390.8 uSv/hr",
	};
	private final static String[] DEF_V_05CI = {
		"1040.3 uSv/hr", "990.8 uSv/hr",
		 "827.3 uSv/hr", "790.8 uSv/hr",
		 "519.3 uSv/hr", "490.8 uSv/hr",
		 "416.3 uSv/hr", "390.8 uSv/hr",
		 "208.3 uSv/hr", "190.9 uSv/hr",
		 "104.3 uSv/hr",  "98.8 uSv/hr",
		  "84.3 uSv/hr",  "78.8 uSv/hr",
		  "53.3 uSv/hr",  "48.8 uSv/hr",
		  "42.3 uSv/hr",  "38.8 uSv/hr",
	};
	private final static String[] DEF_V_005CI = {
		"104.5 uSv/hr", "97.8 uSv/hr",
		 "84.3 uSv/hr", "77.8 uSv/hr",
		 "53.3 uSv/hr", "47.8 uSv/hr",
		 "42.3 uSv/hr", "37.8 uSv/hr",
		 "26.3 uSv/hr", "22.8 uSv/hr",
		 "21.3 uSv/hr", "18.8 uSv/hr",
		 "11.3 uSv/hr", " 8.8 uSv/hr", 
		  "5.5 uSv/hr", " 4.8 uSv/hr",
	};
	
	private final static String[] def_doserate(DevHustIO.Strength ss,boolean bound) {
		String[] lst = null;
		switch(ss) {		
		default:
		case V_005Ci: lst = gen_doserate("Mark_005Ci",DEF_V_005CI); break;
		case V_05Ci : lst = gen_doserate("Mark_05Ci",DEF_V_05CI); break;
		case V_3Ci  : lst = gen_doserate("Mark_3Ci",DEF_V_3CI); break;
		}
		return lst;
	}
	
	private final static String[] gen_doserate(
		final String name,
		final String[] def
	) {
		final String txt = (String) Gawain.prop().getOrDefault(name, "");
		if(txt.length()==0) {
			return def;
		}
		String[] res = txt.split("[,|;]");
		ArrayList<String> lst = new ArrayList<String>();
		for(String v:res) {
			v = v.trim();
			try {
				Float.parseFloat(v);
				lst.add(v+" uSv/hr");
			}catch(NumberFormatException e) {
			}
		}
		return lst.toArray(new String[0]);
	};
	//----------------------------------//

	private final Runnable event_finally = ()->{		
		RadiateStep.hustio.asyncHaltOn();//stop radiation~~~
		Misc.logv("[Ladder] Done!! :-)");
	};
}
