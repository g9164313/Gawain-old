package prj.scada;

import narl.itrc.Misc;

public class ApiMapPID extends WidMapPID {

	//Basic API
	public void delay(long ms){
		Misc.delay(ms);
	}
	
	//About sputter machine....
	public void setLampRed(){
		if(PanSputter.DBG==true){
			Misc.logv("Simulation - red lamp on!!!");
			return;
		}
		towerLamp.setRed();
	}
	public void setLampYellow(){
		if(PanSputter.DBG==true){
			Misc.logv("Simulation - yellow lamp on!!!");
			return;
		}
		towerLamp.setYellow();
	}
	public void setLampGreen(){
		if(PanSputter.DBG==true){
			Misc.logv("Simulation - green lamp on!!!");
			return;
		}
		towerLamp.setGreen();
	}
	public void setLamp(boolean green, boolean yellow, boolean red){
		if(PanSputter.DBG==true){
			Misc.logv("Simulation - lamp green(%B), yellow(%B), reg(%B) ",green);
			return;
		}
		towerLamp.set(green, yellow, red);
	}
	
	public double getBunner1Watt(){
		return burner_dc1.attr[0].prop.getValue().doubleValue();
	}
	public double getBunner1Volt(){
		return burner_dc1.attr[1].prop.getValue().doubleValue();
	}
	public double getBunner1Curr(){
		return burner_dc1.attr[2].prop.getValue().doubleValue();
	}
	
	public double getMP1Curr(){		
		return ampMP1.prop.getValue().doubleValue();
	}
	public double getHeaterCurr(){		
		return ampHeater.prop.getValue().doubleValue();
	}

	public double getPressChamber1(){		
		return presChamber1.prop.getValue().doubleValue();
	}
	public double getPressChamber2(){		
		return presChamber2.prop.getValue().doubleValue();
	}
	public double getPressAr(){		
		return presAr.prop.getValue().doubleValue();
	}
	public double getPressO2(){		
		return presO2.prop.getValue().doubleValue();
	}
	public double getPressN2(){		
		return presN2.prop.getValue().doubleValue();
	}
	public double getPressCP1(){		
		return presCP1.prop.getValue().doubleValue();
	}
	public double getPressCP2(){		
		return presCP2.prop.getValue().doubleValue();
	}
	
	public double getTempCP1(){		
		return tempCP1.prop.getValue().doubleValue();
	}
	public double getTempCP2(){		
		return tempCP2.prop.getValue().doubleValue();
	}
	public double getTempChamber2(){		
		return tempChamber2.prop.getValue().doubleValue();
	}
	public double getTempChiller(){		
		return tempChiller.prop.getValue().doubleValue();
	}
	
	public double get(String name){
		double val =0.;
		switch(name.toLowerCase().trim()){
		case "ar": val = presAr.prop.getValue().doubleValue(); break;
		}
		return val;
	}
	
	public int apcGetStep(){
		return apc.getStep();
	}
	public void apcSetStep(int pos){
		apc.setStep(pos);
	}
	public double apcGetPressure(){
		return apc.getPress();
	}
	public void apcSetPressure(double Torr){
		apc.setPress(Torr);
	}
	
	public int chuckGetRPM(){
		return chuck.getRPM();
	}
	public void chuckSetRPM(int val){
		chuck.setRPM(val);
	}

	public void toggle(String name,boolean onoff){
		switch(name.toLowerCase().trim()){
		case "ar":  Arv.flick(onoff); break;
		case "o2":  O2v.flick(onoff); break;
		case "n2":  N2v.flick(onoff); break;
		case "gv1": gv1.flick(onoff); break;
		case "gv2": gv2.flick(onoff); break;
		case "gv3": gv3.flick(onoff); break;
		case "vv": vv.flick(onoff); break;
		case "rv": rv.flick(onoff); break;
		case "mv1": mv1.flick(onoff); break;
		case "fv1": fv1.flick(onoff); break;
		case "mv2": mv2.flick(onoff); break;
		case "chuck": fv2.flick(onoff);	break;
		case "baffle":
		case "擋板":
			baffle.flick(onoff); 
			break;
		case "cp1": cp1.flick(onoff); break;
		case "cp2": cp2.flick(onoff); break;
		case "mp1": mp1.flick(onoff); break;
		default:
			Misc.logw("未知的物件："+name);
			return;
		}
		Misc.logv("Valve: %s - (%B)",name, onoff); 
	}
}
