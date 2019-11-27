package prj.daemon;


import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

@SuppressWarnings("unused")
public class DevShapeoko extends DevTTY {

	public DevShapeoko() {
		super("dev-shapeoko");
	}
	
	/*@Override
	protected void afterLoop() {
		doing(new FastFetch( 1,   ""     , (cmd,txt)->dummy(cmd,txt)));//got caption
		doing(new FastFetch( 2,   "$X\n" , (cmd,txt)->dummy(cmd,txt)));//unlock device
		doing(new FastFetch( 3,   "M05\n", (cmd,txt)->dummy(cmd,txt)));//spindle off
		doing(new FastFetch( 4,   "G21\n", (cmd,txt)->dummy(cmd,txt)));//Set Units to Millimeters
		doing(new FastFetch( 5,   "~\n"  , (cmd,txt)->dummy(cmd,txt)));//start cycle
		doing(new FastFetch(-6, 7,"?\n"  , (cmd,txt)->update_property(txt)));
	}*/
	
	private boolean debug = true;
	//private boolean debug = false;

	private void dummy(
		final String cmd,
		final String txt
	) {
		if(debug==true) {
			Misc.logv("%s: %s", cmd, txt);
		}else {
			if(cmd.length()==0) {
				return;//this is caption~~~
			}
			if(txt.contains("ok")==false) {
				Misc.loge("fail to execute: %s (%s)", cmd, txt);
			}
		}
	}
	
	public final StringProperty State= new SimpleStringProperty("Reset"); 
	
	public final FloatProperty MPosX = new SimpleFloatProperty();
	public final FloatProperty MPosY = new SimpleFloatProperty();
	public final FloatProperty MPosZ = new SimpleFloatProperty();
	
	public final IntegerProperty Bf1 = new SimpleIntegerProperty();
	public final IntegerProperty Bf2 = new SimpleIntegerProperty();
	
	public final IntegerProperty Fs1 = new SimpleIntegerProperty();
	public final IntegerProperty Fs2 = new SimpleIntegerProperty();
	
	public final FloatProperty WCO1 = new SimpleFloatProperty();
	public final FloatProperty WCO2 = new SimpleFloatProperty();
	public final FloatProperty WCO3 = new SimpleFloatProperty();
	
	public final IntegerProperty Ov1= new SimpleIntegerProperty();
	public final IntegerProperty Ov2= new SimpleIntegerProperty();
	

	private void update_property(final String txt) {
		
		//if(debug==true) {
		//	Misc.logv("Info) %s", txt);
		//}
		
		int p1 = txt.indexOf('<');
		int p2 = txt.lastIndexOf('>');
		if(p1<0 || p2<=0 || p1>=p2) {
			return;
		}
		String[] col = txt
			.substring(p1+1,p2)
			.split("[|]");
		
		Application.invokeAndWait(()->{
			State.set(col[0]);		
			for(int i=1; i<col.length; i++){
				String itm = col[i];
				try{
					String[] val;
					if(itm.startsWith("MPos:")==true){
						val = itm.substring(5).split(",");
						MPosX.set(Float.valueOf(val[0]));
						MPosY.set(Float.valueOf(val[1]));
						MPosZ.set(Float.valueOf(val[2]));
					}else if(itm.startsWith("Bf:")==true){
						val = itm.substring(3).split(",");
						Bf1.set(Integer.valueOf(val[0]));
						Bf2.set(Integer.valueOf(val[1]));
					}else if(itm.startsWith("FS:")==true){
						val = itm.substring(3).split(",");
						Fs1.set(Integer.valueOf(val[0]));
						Fs2.set(Integer.valueOf(val[1]));
					}else if(itm.startsWith("WCO:")==true){
						val = itm.substring(4).split(",");
						WCO1.set(Float.valueOf(val[0]));
						WCO2.set(Float.valueOf(val[1]));
						WCO3.set(Float.valueOf(val[2]));
					}else if(itm.startsWith("Ov:")==true){
						val = itm.substring(3).split(",");
						Ov1.set(Integer.valueOf(val[0]));
						Ov2.set(Integer.valueOf(val[1]));
					}else if(itm.startsWith("Pn:")==true){
						//touch to limit!!!
					}else {
						Misc.loge("Wrong Item: %s",itm);
					}
				}catch(NumberFormatException e){
					Misc.loge("Wrong Fromat: %s",itm);
				}
			}
		});
	}
	
	public void exec(String... commands) {		
		for(int i=0; i<commands.length; i++) {
			String _cmd = commands[i];
			if(_cmd.endsWith("\n")==false) {
				_cmd = _cmd + "\n";
			}
			int ends = (i==commands.length-1)?(-6):(7+i+1);			
			//doing(new FastFetch(
			//	7+i, ends, _cmd,
			//	(cmd,txt)->dummy(cmd,txt)
			//));
		}		
	}
	
	public void move(
		final float xx, 
		final float yy,
		final float zz,
		final boolean abs
	){
		String cmd1 = (abs==true)?("G90\n"):("G91\n");
		String cmd2 = String.format("G00X%.1fY%.1fZ%.1f\n",xx,yy,zz);
		//doing(new FastFetch( 7,   cmd1,(cmd,txt)->dummy(cmd,txt)));
		//doing(new FastFetch( 8,-6,cmd2,(cmd,txt)->dummy(cmd,txt)));
	}
	public void moveAbs(
		float xx, 
		float yy,
		float zz
	){
		move(xx,yy,zz,true);
	}
	public void moveRel(
		float xx, 
		float yy, 
		float zz
	){
		move(xx,yy,zz,false);
	}
}
