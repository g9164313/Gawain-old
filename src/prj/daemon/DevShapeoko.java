package prj.daemon;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import narl.itrc.DevBase;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

public class DevShapeoko extends DevBase {

	private DevTTY tty = new DevTTY();
	
	public DevShapeoko() {
		super("dev-shapeoko");
	}

	private String path;
	
	public void link(final String ttyPath){
		path = ttyPath;
		link();
	}
	
	@Override
	protected boolean eventLink() {
		return tty.open(path);
	}

	@Override
	protected boolean afterLink() {
		tty.createStream();
		String txt = tty.getStreamTail("\r\n",2);
		Misc.logv(txt);
		if(txt.contains("Grbl")==false){
			Misc.loge("Not Valid Grbl firmware!!");
			return false;
		}
		_exec("$X");//~~unlock~~
		_exec("M05");//Spindle off
		_exec("G21");//Set Units to Millimeters
		_exec("~");//start cycle		
		createLooper(50);
		return true;
	}

	private String state = "none";
	private float[] mpos= {0.f, 0.f, 0.f};
	private int  [] bf  = {0, 0};
	private int  [] fs  = {0, 0};	
	private float[] wco = {0.f, 0.f, 0.f};
	private int  [] ov  = {0, 0};

	@Override
	protected int core_looper(Work obj, int pass){		
		tty.writeTxt('?');		
		//parse state information, for example:
		//<Alarm|MPos:0.000,0.000,0.000|Bf:14,128|FS:0,0>		
		String txt = tty.getStreamTail("\n",1).trim();		
		if(txt.matches("[<].+[>]")==false){
			return 0;
		}
		String[] col = txt
			.substring(1, txt.length()-1)
			.split("[|]");
		
		state = col[0];		
		for(String atm:col){
			try{
				String[] val;
				if(atm.startsWith("MPos:")==true){
					val = atm.substring(5).split(",");
					mpos[0] = Float.valueOf(val[0]);
					mpos[1] = Float.valueOf(val[1]);
					mpos[2] = Float.valueOf(val[2]);
				}else if(atm.startsWith("Bf:")==true){
					val = atm.substring(3).split(",");
					bf[0] = Integer.valueOf(val[0]);
					bf[1] = Integer.valueOf(val[1]);
				}else if(atm.startsWith("FS:")==true){
					val = atm.substring(3).split(",");
					fs[0] = Integer.valueOf(val[0]);
					fs[1] = Integer.valueOf(val[1]);
				}else if(atm.startsWith("WCO:")==true){
					val = atm.substring(4).split(",");
					wco[0] = Float.valueOf(val[0]);
					wco[1] = Float.valueOf(val[1]);
					wco[2] = Float.valueOf(val[2]);
				}else if(atm.startsWith("Ov:")==true){
					val = atm.substring(3).split(",");
					ov[0] = Integer.valueOf(val[0]);
					ov[1] = Integer.valueOf(val[1]);
				}
			}catch(NumberFormatException e){
				Misc.loge("Wrong Fromat --> %s",atm);
			}
		}	
		return 1; 
	}

	
	public final StringProperty State= new SimpleStringProperty(state); 
	
	public final FloatProperty MPosX = new SimpleFloatProperty(mpos[0]);
	public final FloatProperty MPosY = new SimpleFloatProperty(mpos[1]);
	public final FloatProperty MPosZ = new SimpleFloatProperty(mpos[2]);		
	public final IntegerProperty Bf1 = new SimpleIntegerProperty(bf[0]);
	public final IntegerProperty Bf2 = new SimpleIntegerProperty(bf[1]);
	public final IntegerProperty Fs1 = new SimpleIntegerProperty(fs[0]);
	public final IntegerProperty Fs2 = new SimpleIntegerProperty(fs[1]);
	
	public final FloatProperty WCO1 = new SimpleFloatProperty(wco[0]);
	public final FloatProperty WCO2 = new SimpleFloatProperty(wco[1]);
	public final FloatProperty WCO3 = new SimpleFloatProperty(wco[2]);	
	public final IntegerProperty Ov1= new SimpleIntegerProperty(ov[0]);
	public final IntegerProperty Ov2= new SimpleIntegerProperty(ov[1]);
	
	@Override
	protected int core_event(Work obj, int pass){
		State.set(state);			
		MPosX.set(mpos[0]);
		MPosY.set(mpos[1]);
		MPosZ.set(mpos[2]);
		Bf1.set(bf[0]);
		Bf2.set(bf[1]);
		Fs1.set(fs[0]);
		Fs2.set(fs[1]);
		WCO1.set(wco[0]);
		WCO2.set(wco[1]);
		WCO3.set(wco[2]);
		Ov1.set(ov[0]);
		Ov2.set(ov[1]);
		if(state.contains("Idle")==true){
			return 0;//for executing next work
		}
		return 2;
	}
	
	private void _exec(String cmd){		
		Misc.logv("CMD-->%s", cmd);//debug!!!
		if(cmd.endsWith("\n")==false){
			cmd = cmd + "\n";
		}
		tty.writeTxt(cmd);
		String res;
		do{
			res = tty.getStreamTail("\r\n",1);
			Misc.logv("RES-->%s", res);//debug!!!
			if(
				res.contains("ok")==true || 
				res.contains("error")==true
			){
				break;
			}				
		}while(isCanceled()==false);
	}
	
	public void exec(final String cmd){
		offer(work->_exec(cmd));
	}
	
	public void exec_atom(final String cmd){
		if(countWork()>1){
			return;
		}
		offer(work->_exec(cmd));
	}
	
	public void move(
		final float xx, 
		final float yy, 
		final boolean abs
	){		
		offer(work->{
			if(abs==true){
				_exec("G90\n");
			}else{
				_exec("G91\n");
			}
			_exec(String.format("G00X%.1fY%.1f\n", xx,yy));
		});
	}
	
	public void moveAbs(float xx, float yy){
		move(xx,yy,true);
	}
	public void moveRel(float xx, float yy){
		move(xx,yy,false);
	}
	
	public void move_atom(float xx, float yy, boolean abs){
		if(countWork()>1){
			return;
		}
		move(xx,yy,abs);
	}
	
	public boolean isIdle(){
		if(state.contains("Idle")==true){
			return true;
		}
		return false;
	}
	
	@Override
	protected void beforeUnlink() {
		tty.close();
	}

	@Override
	protected void eventUnlink() {
		path = null;
	}
}
