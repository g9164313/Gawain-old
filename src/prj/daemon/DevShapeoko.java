package prj.daemon;


import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

public class DevShapeoko extends DevTTY {

	public DevShapeoko() {
		super("dev-shapeoko");
	}

	protected void afterOpen() {
		//wait caption.....
		readTxt(
			"unlock]\r\n",
			(act,txt)->{
			Misc.logv("%s",txt);
		});
		
		fetchTxt(
			"$X\n",
			"", "\r\n", (act,txt)->{
			if(txt.contains("Unlocked")==true) {
				act.repeat = 1;
			}else {
				Misc.logv("Unlock is %s", txt);
				act.repeat = 0;
			}			
		});
		
		exec("$X");//~~unlock~~
		exec("M05");//Spindle off
		exec("G21");//Set Units to Millimeters
		exec("~");//start cycle
		
		take(-1, 100, monitor);
	}
	
	
	//private final AtomicReference<String> state = new AtomicReference<String>();
	
	public final StringProperty State= new SimpleStringProperty("Prepare"); 
	
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
	
	private final Action monitor = new Action()
		.writeData("?\n")
		.indexOfData(
			"<", ">\r\nok\r\n", 
			(act,txt)->{
			//Misc.logv("%s", txt);
			String[] col = txt
				.substring(1, txt.length()-7)
				.split("[|]");
			State.set(col[0]);		
			for(String atm:col){
				try{
					String[] val;
					if(atm.startsWith("MPos:")==true){
						val = atm.substring(5).split(",");
						MPosX.set(Float.valueOf(val[0]));
						MPosY.set(Float.valueOf(val[1]));
						MPosZ.set(Float.valueOf(val[2]));
					}else if(atm.startsWith("Bf:")==true){
						val = atm.substring(3).split(",");
						Bf1.set(Integer.valueOf(val[0]));
						Bf2.set(Integer.valueOf(val[1]));
					}else if(atm.startsWith("FS:")==true){
						val = atm.substring(3).split(",");
						Fs1.set(Integer.valueOf(val[0]));
						Fs2.set(Integer.valueOf(val[1]));
					}else if(atm.startsWith("WCO:")==true){
						val = atm.substring(4).split(",");
						WCO1.set(Float.valueOf(val[0]));
						WCO2.set(Float.valueOf(val[1]));
						WCO3.set(Float.valueOf(val[2]));
					}else if(atm.startsWith("Ov:")==true){
						val = atm.substring(3).split(",");
						Ov1.set(Integer.valueOf(val[0]));
						Ov2.set(Integer.valueOf(val[1]));
					}
				}catch(NumberFormatException e){
					Misc.loge("Wrong Fromat --> %s",atm);
				}
			}
		});
	
	public void exec(final String cmd){
		fetchTxt(
			(cmd.endsWith("\n")==false)?(cmd+"\n"):(cmd),
			"", "\r\n",
			null);
			//(act,txt)->{
			//Misc.logv("exec: %s-->%s",cmd,txt);
		//});
	}
	
	private Task<?> tskMove; 
	
	public void move(
		final float xx, 
		final float yy, 
		final boolean abs
	){
		if(tskMove!=null) {
			if(tskMove.isDone()==false) {
				Misc.logw("shapeoko is busy~~");
				return;
			}
		}
		tskMove = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				while(State.get().equals("Idle")==false) {
					//updateMessage("wait shapeoko");
					Thread.sleep(100);
				}
				//updateMessage("command shapeoko");
				if(abs==true){
					exec("G90\n");
				}else {
					exec("G91\n");
				}
				exec(String.format("G00X%.1fY%.1f\n", xx,yy));
				return null;
			}
		};
		Thread thr = new Thread(
			tskMove,
			"shapeoko-move"
		);
		thr.setDaemon(true);
		thr.start();		
	}
	public void moveAbs(float xx, float yy){
		move(xx,yy,true);
	}
	public void moveRel(float xx, float yy){
		move(xx,yy,false);
	}
}
