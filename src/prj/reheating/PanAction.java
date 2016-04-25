package prj.reheating;

import narl.itrc.PropBundle;
import narl.itrc.Misc;
import narl.itrc.PanListAction;

public class PanAction extends PanListAction {

	private Entry e = null;
	
	private final String CMD_HEAT_TUBE = "HEAT_TUBE";
	private final String CMD_HEAT_BUCK = "HEAT_BUCK";
	private final String CMD_COOL_TUBE = "COOL_TUBE";
	private final String CMD_COOL_BUCK = "COOL_BUCK";
	private final String CMD_VACCUM = "VACCUM";
	private final String CMD_HOOK_A = "HOOK_A";
	private final String CMD_HOOK_B = "HOOK_B";
	private final String CMD_HOOK_O = "HOOK_O";
	
	public PanAction(Entry parent){		
		e = parent;
		lstToken.add(new Token(
			CMD_HEAT_TUBE,
			"加熱爐管至?ºC",
			"範例：100 表示加熱爐管至100ºC"
		));
		lstToken.add(new Token(
			CMD_HEAT_BUCK,
			"加熱油桶至?ºC",
			"範例：100 表示加熱油桶至100ºC"
		));
		lstToken.add(new Token(
			CMD_COOL_TUBE,
			"冷卻爐管至?ºC",
			"範例：100 表示冷卻爐管低於100ºC"
		));
		lstToken.add(new Token(
			CMD_COOL_BUCK,
			"冷卻油桶至?ºC",
			"範例：100 表示冷卻油桶低於100ºC"
		));
		lstToken.add(new Token(
			CMD_VACCUM,
			"抽真空至?BAR",
			"範例：0.1 表示氣壓降至0.1BAR"
		));
		lstToken.add(new Token(
			CMD_HOOK_A,
			"移動掛勾至爐管",
			"忽略參數"
		));
		lstToken.add(new Token(
			CMD_HOOK_B,
			"移動掛勾至油桶",
			"忽略參數"
		));
		lstToken.add(new Token(
			CMD_HOOK_O,
			"收回掛勾",
			"忽略參數"
		));
	}

	@Override
	protected int eventStage(PropBundle itm,String[] parm){
		int res = 1;//always keep going~~~
		if(parm[0].equals(CMD_HEAT_TUBE)==true){
			Misc.logv("EXE: CMD_HEAT_TUBE");
		}else if(parm[0].equals(CMD_HEAT_BUCK)==true){
			Misc.logv("EXE: CMD_HEAT_BUCK");
		}else if(parm[0].equals(CMD_COOL_TUBE)==true){
			Misc.logv("EXE: CMD_COOL_TUBE");
		}else if(parm[0].equals(CMD_COOL_BUCK)==true){
			Misc.logv("EXE: CMD_COOL_BUCK");
		}else if(parm[0].equals(CMD_VACCUM)==true){
			Misc.logv("EXE: CMD_VACCUM");
		}else if(parm[0].equals(CMD_HOOK_A)==true){
			Misc.logv("EXE: CMD_HOOK_A");
		}else if(parm[0].equals(CMD_HOOK_B)==true){
			Misc.logv("EXE: CMD_HOOK_B");
		}else if(parm[0].equals(CMD_HOOK_O)==true){
			Misc.logv("EXE: CMD_HOOK_O");
		}
		return res;
	}
	
	@Override
	protected void eventStart() {
		e.panCtrl.setDisable(true);
	}

	@Override
	protected void eventStop() {
		e.panCtrl.setDisable(false);
	}
}
