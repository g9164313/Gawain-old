package prj.refuge;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.virtualbox_4_3.Holder;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IDisplay;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.VirtualBoxManager;

import com.sun.glass.events.KeyEvent;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import narl.itrc.Gawain;

public class TaskVBoxMonitor extends Task<Integer>{

	private Thread thread = null;
	
	private VirtualBoxManager vman;
	
	private IMachine vbox = null;	
	private ISession sess = null;
	private IConsole cons = null;
	
	private Holder<Long> screenWidth = new Holder<Long>();
	private Holder<Long> screenHeight= new Holder<Long>();
	private Holder<Long> screenBitPix= new Holder<Long>();

	private BlockingQueue<String> quee = new ArrayBlockingQueue<String>(25);
	
	public TaskVBoxMonitor(VirtualBoxManager m){
		vman = m;
		setOnFailed(e->{
			sess = null;
			cons = null;
			final Alert dia = new Alert(AlertType.ERROR);
			dia.setTitle("內部錯誤");
			dia.setContentText("Task fail...");
			dia.showAndWait();
		});
	}	
	
	public void launch(IMachine m){
		vbox = m;		
		thread = new Thread(this,"Task-VBoxMonitor");
		thread.start();
	}
	
	public void send(final String... msg){
		try{
			quee.addAll(Arrays.asList(msg));
		}catch(IllegalStateException e){
			final Alert dia = new Alert(AlertType.ERROR);
			dia.setTitle("內部錯誤");
			dia.setContentText("忙碌中...");
			dia.showAndWait();
		}
	}
	
	@Override
	protected Integer call() throws Exception {
		
		sess = vman.getSessionObject();
		
		IProgress prog = vbox.launchVMProcess(sess, "gui", "");
		while(prog.getCompleted()==false){
			prog.waitForCompletion(50);
			if(this.isCancelled()==true){
				return -2;
			}
		}
		
		cons = sess.getConsole();
		
		cons.getDisplay().getScreenResolution(
			0L, 
			screenWidth, screenHeight, 
			screenBitPix, 
			null, null
		);

		while(Gawain.isExit()==false){
			
			if(quee.isEmpty()==true){
				Thread.sleep(100);
				continue;
			}
			String[] tkn = quee.take().split(":");			
			String cmd = tkn[0];
			String arg = (tkn.length>=2)?(tkn[1]):(null);
			
			if(cmd.startsWith("snap")==true){
				snapshot(arg);			
			}else if(cmd.startsWith("key")==true){
				map_vkey(arg);
			}else if(cmd.startsWith("pts")==true){
				move_pts(arg);
			}else if(cmd.startsWith("script")==true){
				
			}
		}	
		return 0;
	}
	
	private void snapshot(String name){
		if(name==null){
			name = "snap.png";
		}
		try {
			FileOutputStream stm = new FileOutputStream(name);
			byte[] buf = cons.getDisplay().takeScreenShotPNGToArray(
				0L, 
				screenWidth.value, screenHeight.value
			);
			stm.write(buf);
			stm.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static final int vkey[] = {
		KeyEvent.VK_UNDEFINED/*NULL*/, KeyEvent.VK_SPACE            /*SP */, KeyEvent.VK_AT           /* @ */, KeyEvent.VK_BACK_QUOTE/* ` */,
		KeyEvent.VK_HOME     /*SOH */, KeyEvent.VK_EXCLAMATION      /* ! */, KeyEvent.VK_A            /* A */, KeyEvent.VK_A         /* a */,
		KeyEvent.VK_HOME     /*STX */, KeyEvent.VK_UNDEFINED        /* " */, KeyEvent.VK_B            /* B */, KeyEvent.VK_B         /* b */,
		KeyEvent.VK_END      /*ETX */, KeyEvent.VK_NUMBER_SIGN      /* # */, KeyEvent.VK_C            /* C */, KeyEvent.VK_C         /* c */,
		KeyEvent.VK_END      /*EOT */, KeyEvent.VK_DOLLAR           /* $ */, KeyEvent.VK_D            /* D */, KeyEvent.VK_D         /* d */,
		KeyEvent.VK_UNDEFINED/*ENQ */, KeyEvent.VK_UNDEFINED        /* % */, KeyEvent.VK_E            /* E */, KeyEvent.VK_E         /* e */,
		KeyEvent.VK_UNDEFINED/*ACK */, KeyEvent.VK_UNDEFINED        /* & */, KeyEvent.VK_F            /* F */, KeyEvent.VK_F         /* f */,
		KeyEvent.VK_ESCAPE   /*bell*/, KeyEvent.VK_QUOTE            /* ' */, KeyEvent.VK_G            /* G */, KeyEvent.VK_G         /* g */,
		KeyEvent.VK_BACKSPACE/*back*/, KeyEvent.VK_LEFT_PARENTHESIS /* ( */, KeyEvent.VK_H            /* H */, KeyEvent.VK_H         /* h */,
		KeyEvent.VK_TAB      /*TAB */, KeyEvent.VK_RIGHT_PARENTHESIS/* ) */, KeyEvent.VK_I            /* I */, KeyEvent.VK_I         /* i */,
		KeyEvent.VK_ENTER    /* LF */, KeyEvent.VK_MULTIPLY         /* * */, KeyEvent.VK_J            /* J */, KeyEvent.VK_J         /* j */,
		KeyEvent.VK_UNDEFINED/* VT */, KeyEvent.VK_ADD              /* + */, KeyEvent.VK_K            /* K */, KeyEvent.VK_K         /* k */,
		KeyEvent.VK_UNDEFINED/* FF */, KeyEvent.VK_UNDEFINED        /*   */, KeyEvent.VK_L            /* L */, KeyEvent.VK_L         /* l */,
		KeyEvent.VK_ENTER    /* CR */, KeyEvent.VK_MINUS            /* - */, KeyEvent.VK_M            /* M */, KeyEvent.VK_M         /* m */,
		KeyEvent.VK_UNDEFINED/* SO */, KeyEvent.VK_PERIOD           /* . */, KeyEvent.VK_N            /* N */, KeyEvent.VK_N         /* n */,
		KeyEvent.VK_UNDEFINED/* SI */, KeyEvent.VK_SLASH            /* / */, KeyEvent.VK_O            /* O */, KeyEvent.VK_O         /* o */,
		KeyEvent.VK_ESCAPE   /*DLE */, KeyEvent.VK_0                /* 0 */, KeyEvent.VK_P            /* P */, KeyEvent.VK_P         /* p */,
		KeyEvent.VK_F1       /*DC1 */, KeyEvent.VK_1                /* 1 */, KeyEvent.VK_Q            /* Q */, KeyEvent.VK_Q         /* q */,
		KeyEvent.VK_F2       /*DC2 */, KeyEvent.VK_2                /* 2 */, KeyEvent.VK_R            /* R */, KeyEvent.VK_R         /* r */,
		KeyEvent.VK_F3       /*DC3 */, KeyEvent.VK_3                /* 3 */, KeyEvent.VK_S            /* S */, KeyEvent.VK_S         /* s */,
		KeyEvent.VK_F4       /*DC4 */, KeyEvent.VK_4                /* 4 */, KeyEvent.VK_T            /* T */, KeyEvent.VK_T         /* t */,
		KeyEvent.VK_F5       /*NAK */, KeyEvent.VK_5                /* 5 */, KeyEvent.VK_U            /* U */, KeyEvent.VK_U         /* u */,
		KeyEvent.VK_F6       /*SYN */, KeyEvent.VK_6                /* 6 */, KeyEvent.VK_V            /* V */, KeyEvent.VK_V         /* v */,
		KeyEvent.VK_F7       /*ETB */, KeyEvent.VK_7                /* 7 */, KeyEvent.VK_W            /* W */, KeyEvent.VK_W         /* w */,
		KeyEvent.VK_F8       /*CAN */, KeyEvent.VK_8                /* 8 */, KeyEvent.VK_X            /* X */, KeyEvent.VK_X         /* x */,
		KeyEvent.VK_F9       /* EM */, KeyEvent.VK_9                /* 9 */, KeyEvent.VK_Y            /* Y */, KeyEvent.VK_Y         /* y */,
		KeyEvent.VK_F10      /*SUB */, KeyEvent.VK_COLON            /* : */, KeyEvent.VK_Z            /* Z */, KeyEvent.VK_Z         /* z */,
		KeyEvent.VK_F11      /*ESC */, KeyEvent.VK_UNDEFINED        /* ; */, KeyEvent.VK_OPEN_BRACKET /* [ */, KeyEvent.VK_BRACELEFT /* { */,
		KeyEvent.VK_F12      /* FS */, KeyEvent.VK_LESS             /* < */, KeyEvent.VK_BACK_SLASH   /* \ */, KeyEvent.VK_UNDEFINED /* | */,
		KeyEvent.VK_F13      /* GS */, KeyEvent.VK_UNDEFINED        /* = */, KeyEvent.VK_CLOSE_BRACKET/* ] */, KeyEvent.VK_BRACERIGHT/* } */,
		KeyEvent.VK_F14      /* RS */, KeyEvent.VK_GREATER          /* > */, KeyEvent.VK_CIRCUMFLEX   /* ^ */, KeyEvent.VK_UNDEFINED /* ~ */,
		KeyEvent.VK_F15      /* US */, KeyEvent.VK_UNDEFINED        /* ? */, KeyEvent.VK_UNDERSCORE   /* _ */, KeyEvent.VK_DELETE    /*DEL*/,
	};
	
	private void map_vkey(String txt) throws InterruptedException{
		if(txt==null){
			return;
		}
		char[] txts = txt.toCharArray();
		for(int cc:txts){			
			if(0<=cc && cc<=127){
				cc = (cc%4)*32 + (cc/4);
				cons.getKeyboard().putScancode(vkey[cc]);
				Thread.sleep(10);
			}
		}
	}
	
	private void move_pts(String prop){
		if(prop==null){
			return;
		}
		//if(abs==true){
		//}else{
		//}
		cons.getMouse().putMouseEvent(0, 0, 0, 0, 0);
		cons.getMouse().putMouseEventAbsolute(0, 0, 0, 0, 0);
	}
}
