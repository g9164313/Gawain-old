package prj.refuge;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.virtualbox_4_3.Holder;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IDisplay;
import org.virtualbox_4_3.IKeyboard;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.VirtualBoxManager;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.util.StringConverter;
import narl.itrc.DiaChoice;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class TaskSandbox extends Task<Integer> {
	
	//private static final int VBOX_RUNNING = 5;
	//private static final int VBOX_PAUSE = 6;
	private static final int VBOX_POWEREDOFF = 1;
	
	private static final Hashtable<Character,List<Integer>> scode = new Hashtable<Character,List<Integer>>();
	
	private static final VirtualBoxManager vman = VirtualBoxManager.createInstance("Sandbox");
	
	private IMachine vbox = null;	
	private ISession sess = null;
	private IConsole cons = null;

	private BlockingQueue<String> quee = new ArrayBlockingQueue<String>(25);
		
	public TaskSandbox(){
		init_scancode();
		setOnFailed(e->{			
			final Alert dia = new Alert(AlertType.ERROR);
			dia.setTitle("內部錯誤");
			dia.setContentText("Task fail...");
			dia.showAndWait();
			clear_session();
		});
		setOnSucceeded(e->{
			clear_session();		
		});
	}	

	private void clear_session(){
		//vman.closeMachineSession(sess);
		vman.cleanup();
		vbox = null;
		sess = null;
		cons = null;
	}
	
	/**
	 * Make a task instance, to provide message looper for communication with virtual-machine.<p>
	 * Important!! this function must be executed by GUI-thread.<p>
	 * This is because it will pop a dialog to ask user launch which machine.<p> 
	 * @return a task instance
	 */
	public static TaskSandbox factory(TaskSandbox ptr){
		if(ptr!=null){
			if(ptr.isRunning()==true){
				Alert dia = new Alert(AlertType.WARNING);
				dia.setTitle("內部警告");
				dia.setContentText("工作執行中");
				dia.showAndWait();
				return ptr;
			}
		}
		ptr = new TaskSandbox();
		ptr.choose_vbox();
		if(ptr.vbox==null){
			return null;
		}
		new Thread(ptr,"Task-VBoxMonitor").start();
		return ptr;
	}
	
	private void choose_vbox(){
		List<IMachine> lst = vman.getVBox().getMachines();			
		if(lst.size()==1){
			vbox = lst.get(0);
		}else{
			final DiaChoice<IMachine> dia = new DiaChoice<IMachine>();
			dia.setTitle("啟動虛擬機器");
			dia.setHeaderText(null);
			dia.setContentText("機器名稱：");
			dia.getItems().addAll(lst);
			dia.setSelectedItem(lst.get(0));
			dia.setConverter(strconv);
			final Optional<IMachine> opt = dia.showAndWait();
			if(opt.isPresent()==false){
				return;
			}
			vbox = opt.get();
		}
	}
	
	private static StringConverter<IMachine> strconv = new StringConverter<IMachine>() {
		@Override
		public String toString(IMachine object) {
			return (object==null)?(null):(object.getName());
		}
		@Override
		public IMachine fromString(String string) {
			return null;
		}
    };

	public void send(final String... msg){
		try{
			quee.addAll(Arrays.asList(msg));
		}catch(IllegalStateException e){
			Alert dia = new Alert(AlertType.ERROR);
			dia.setTitle("內部錯誤");
			dia.setContentText("忙碌中...");
			dia.showAndWait();
		}
	}

	/**
	 * Core, main looper for send command to virtual machine.
	 */
	@Override
	protected Integer call() throws Exception {

		ScriptEngine script = new ScriptEngineManager().getEngineByName("nashorn");
		script.put("m", this);
		script.put("man", this);
		script.put("b", this);
		script.put("box", this);
		
		sess = vman.getSessionObject();
		
		//phase.1 - wait for session.
		IProgress prog = vbox.launchVMProcess(sess, "gui", "");
		while(prog.getCompleted()==false){
			prog.waitForCompletion(50);
			if(isCancelled()==true){
				return -2;
			}
		}
		//setup console~~~
		cons = sess.getConsole();
		//phase.2 - wait control message.
		while(Gawain.isExit()==false && vbox.getState().value()!=VBOX_POWEREDOFF){
			
			if(quee.isEmpty()==true){
				Thread.sleep(1000);
				//Misc.logv("vbox=%s(%d)",
				//	vbox.getState().name(),
				//	vbox.getState().value()
				//);
				continue;
			}
			String[] tkn = quee.take().split(":");			
			String cmd = tkn[0];
			String arg = (tkn.length>=2)?(tkn[1]):(null);
			
			if(cmd.startsWith("snap")==true){
				snap_shot(arg);
			}else if(cmd.startsWith("ocr")==true){
				
			}else if(cmd.startsWith("key")==true){
				keyin_text(arg);
			}else if(cmd.startsWith("pts")==true){
				mouse_ctrl(arg);
			}else if(cmd.startsWith("script")==true){
				try{
					if(arg==null){
						continue;
					}
					script.eval(arg);
				}catch(ScriptException e){
					Misc.logv(e.getMessage());
				}
			}
		}
		Misc.logv("vbox is closed!!");
		return 0;
	}
	
	/**
	 * this method will be invoked by script engine
	 * @param name
	 */
	public void snap(String name){
		snap_shot(name);
	}
	
	/**
	 * this method will be invoked by script engine
	 * @param txt - send ASCII character one by one
	 * @throws InterruptedException 
	 */
	public void keyin(String txt) throws InterruptedException{
		keyin_text(txt);
	}
	
	/**
	 * Take a screen shot from virtual machine.
	 * @param name - image file name, if no, use default name: "snap.png".
	 */
	private void snap_shot(String name){
		if(name==null){
			name = "snap.png";
		}
		IDisplay dp = cons.getDisplay();
		
		Holder<Long> sw = new Holder<Long>();
		Holder<Long> sh = new Holder<Long>();
		Holder<Long> bsp= new Holder<Long>();
		Holder<Integer> ox = new Holder<Integer>();
		Holder<Integer> oy = new Holder<Integer>();
		dp.getScreenResolution(0L, sw, sh, bsp, ox, oy);
		
		try {
			FileOutputStream stm = new FileOutputStream(name);
			stm.write(dp.takeScreenShotPNGToArray(0L, sw.value, sh.value));
			stm.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Map ASCII code to scan-code, then send it to the machine.
	 * @param txt - text
	 * @throws InterruptedException - thread sleep, it simulate typing speed.
	 */
	private void keyin_text(String txt) throws InterruptedException{
		if(txt==null){
			return;
		}
		IKeyboard kb = cons.getKeyboard();
		char[] lst = txt.toCharArray();
		for(char cc:lst){
			List<Integer> code = scode.get(cc);
			ArrayList<Integer> buf = new ArrayList<Integer>();
			buf.addAll(code);
			buf.addAll(code);
			int id = buf.size() - 1;
			buf.set(id, buf.get(id) + 0x80);
			kb.putScancodes(buf);
		}
	}
	
	/**
	 * send mouse event to virtual machine.
	 * @param prop
	 */
	private void mouse_ctrl(String prop){
		if(prop==null){
			return;
		}
		//if(abs==true){
		//}else{
		//}
		cons.getMouse().putMouseEvent(0, 0, 0, 0, 0);
		cons.getMouse().putMouseEventAbsolute(0, 0, 0, 0, 0);
	}
	
	private void init_scancode(){
		if(scode.size()!=0){
			return;
		}
		//the first initialize......		
		scode.put('a' , Arrays.asList(0x1e));
		scode.put('b' , Arrays.asList(0x30));
		scode.put('c' , Arrays.asList(0x2e));
		scode.put('d' , Arrays.asList(0x20));
		scode.put('e' , Arrays.asList(0x12));
		scode.put('f' , Arrays.asList(0x21));
		scode.put('g' , Arrays.asList(0x22));
		scode.put('h' , Arrays.asList(0x23));
		scode.put('i' , Arrays.asList(0x17));
		scode.put('j' , Arrays.asList(0x24));
		scode.put('k' , Arrays.asList(0x25));
		scode.put('l' , Arrays.asList(0x26));
		scode.put('m' , Arrays.asList(0x32));
		scode.put('n' , Arrays.asList(0x31));
		scode.put('o' , Arrays.asList(0x18));
		scode.put('p' , Arrays.asList(0x19));
		scode.put('q' , Arrays.asList(0x10));
		scode.put('r' , Arrays.asList(0x13));
		scode.put('s' , Arrays.asList(0x1f));
		scode.put('t' , Arrays.asList(0x14));
		scode.put('u' , Arrays.asList(0x16));
		scode.put('v' , Arrays.asList(0x2f));
		scode.put('w' , Arrays.asList(0x11));
		scode.put('x' , Arrays.asList(0x2d));
		scode.put('y' , Arrays.asList(0x15));
		scode.put('z' , Arrays.asList(0x2c));
		scode.put('0' , Arrays.asList(0x0b));
		scode.put('1' , Arrays.asList(0x02));
		scode.put('2' , Arrays.asList(0x03));
		scode.put('3' , Arrays.asList(0x04));
		scode.put('4' , Arrays.asList(0x05));
		scode.put('5' , Arrays.asList(0x06));
		scode.put('6' , Arrays.asList(0x07));
		scode.put('7' , Arrays.asList(0x08));
		scode.put('8' , Arrays.asList(0x09));
		scode.put('9' , Arrays.asList(0x0a));
		scode.put(' ' , Arrays.asList(0x39));
		scode.put('-' , Arrays.asList(0x0c));
		scode.put('=' , Arrays.asList(0x0d));
		scode.put('[' , Arrays.asList(0x1a));
		scode.put(']' , Arrays.asList(0x1b));
		scode.put(';' , Arrays.asList(0x27));
		scode.put('\'', Arrays.asList(0x28));
		scode.put(',' , Arrays.asList(0x33));
		scode.put('.' , Arrays.asList(0x34));
		scode.put('/' , Arrays.asList(0x35));
		scode.put('\t', Arrays.asList(0x0f));
		scode.put('`' , Arrays.asList(0x29));
		scode.put('\n', Arrays.asList(0x1c));//ENTER		
		scode.put('\b', Arrays.asList(0x0e));//Backspace
		scode.put(' ' , Arrays.asList(0x39));//SPACE
		scode.put('\r', Arrays.asList(0x1c));//ENTER
		scode.put('\u8001', Arrays.asList(0x01));//ESC
		scode.put('\u8002', Arrays.asList(0x3a));//CapsLock
		scode.put('\u8011', Arrays.asList(0x2a));//Left  Shift
		scode.put('\u8012', Arrays.asList(0x36));//Right Shift
		scode.put('\u8021', Arrays.asList(0x1d      ));//Left  CTRL
		scode.put('\u8022', Arrays.asList(0xe0, 0x1d));//Right CTRL
		scode.put('\u8031', Arrays.asList(0x38      ));//Left  ALT
		scode.put('\u8032', Arrays.asList(0xe0, 0x38));//Right ALT
		scode.put('\u4001', Arrays.asList(0xe0, 0x52));//insert
		scode.put('\u4002', Arrays.asList(0xe0, 0x53));//delete
		scode.put('\u4003', Arrays.asList(0xe0, 0x4f));//end
		scode.put('\u4004', Arrays.asList(0xe0, 0x47));//home
		scode.put('\u4005', Arrays.asList(0xe0, 0x49));//PageUp
		scode.put('\u4006', Arrays.asList(0xe0, 0x51));//PageDown
		scode.put('\uA001', Arrays.asList(0xe0, 0x5b));//Left  GUI, Win or Apple key
		scode.put('\uA002', Arrays.asList(0xe0, 0x5c));//Right GUI, Win or Apple key
		scode.put('\uA003', Arrays.asList(0xe0, 0x5d));//???
		scode.put('\u2001', Arrays.asList(0x3b));//F1 - function key
		scode.put('\u2002', Arrays.asList(0x3c));//F2 - function key
		scode.put('\u2003', Arrays.asList(0x3d));//F3 - function key
		scode.put('\u2004', Arrays.asList(0x3e));//F4 - function key
		scode.put('\u2005', Arrays.asList(0x3f));//F5 - function key
		scode.put('\u2006', Arrays.asList(0x40));//F6 - function key
		scode.put('\u2007', Arrays.asList(0x41));//F7 - function key
		scode.put('\u2008', Arrays.asList(0x42));//F8 - function key
		scode.put('\u2009', Arrays.asList(0x43));//F9 - function key
		scode.put('\u200A', Arrays.asList(0x44));//F10 - function key
		scode.put('\u200B', Arrays.asList(0x57));//F11 - function key
		scode.put('\u200C', Arrays.asList(0x58));//F12 - function key
		scode.put('\u1001', Arrays.asList(0xe0, 0x48));//up - arrows pad
		scode.put('\u1002', Arrays.asList(0xe0, 0x4b));//left - arrows pad
		scode.put('\u1003', Arrays.asList(0xe0, 0x50));//down - arrows pad
		scode.put('\u1004', Arrays.asList(0xe0, 0x4d));//right - arrows pad
	}
}
