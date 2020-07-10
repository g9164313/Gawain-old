package narl.itrc.init;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXToggleButton;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class LogStream {

	private static final SimpleDateFormat F_STAMP = new SimpleDateFormat("MM/dd  HH:mm:ss.SSS");
	
	public interface Hooker {
		void callback(long tick, char level, String text);
	};
	
	public static class Mesg implements Serializable {		

		private static final long serialVersionUID = 3532735768645537118L;
		
		final Timestamp stm;
		final char tkn;
		String txt;

		public Mesg(final char level){
			stm = new Timestamp(System.currentTimeMillis());
			tkn = level;
		}
		public String getCol0() { return F_STAMP.format(stm); }
		public String getCol1() { return ""+tkn; }
		public String getCol2() { return txt; }		
		public long   getTick() { return stm.getTime(); }
		public String getText() { return txt; }
		public Mesg   setText(final String text) { txt = text; return this; }
		public void callback(Hooker hook) {
			hook.callback(stm.getTime(), tkn, txt);
		}
		/*@Override
		public String toString() {
			return String.format("[%s][%c] %s",getCol0(),tkn,txt);
		}*/
	};
	
	private class Pipe extends OutputStream {
		Optional<Mesg> msg = Optional.empty();
		FileOutputStream fid;
		PrintStream p_in;
		PrintStream p_out;		
		ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
		
		Pipe(final PrintStream stm){
			p_in = new PrintStream(this);
			p_out= stm;
		}		
		PrintStream getNode(){
			return p_in;
		}
		Pipe setFile(final String name){
			try {
				fid = new FileOutputStream(name);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return this;
		}
		
		@Override
		public void write(int b) throws IOException {
			switch(b){
			case 020://end of log message
				//upload message~~~
				if(msg.isPresent()==false) {
					return;
				}
				Mesg mm = msg.get().setText(buf.toString());				
				//1.logger always need it
				if(logger.size()>=200){
					logger.remove(0, 100);
				}else{
					logger.add(mm);
				}
				//2.Should we put it in pool?
				if(usePool.get()==true){
					pooler.add(mm);
				}
				//3.check hooker
				if(useHook.get()==true){
					mm.callback(hooker);
				}
				//4.clear buffer for next turn
				msg = Optional.empty();
				buf.reset();
				break;
			case 021://verbose message token
				msg = Optional.of(new Mesg('V'));
				break;
			case 022://warning message token
				msg = Optional.of(new Mesg('W'));
				break;
			case 023://error message token
				msg = Optional.of(new Mesg('E'));		
				break;
			default:
				p_out.write(b);//pipe to the origin stream
				if(fid!=null){ 
					fid.write(b);
				}
				if(msg.isPresent()==true) {
					buf.write(b);
				}
				break;
			}
		}
		@Override
		public void flush() throws IOException {
			p_out.flush();
			if(fid!=null){ fid.flush(); }
		}
		@Override
		public void close() throws IOException {
			p_out.close();
			if(fid!=null){ 
				fid.close(); fid=null;
			}
		}
	};
	
	private Pipe[] pip = {
		new Pipe(System.out),
		new Pipe(System.err),
	};
	
	private static Optional<LogStream> self = Optional.empty();
	private static Optional<PanBase> pane = Optional.empty();
	
	private LogStream(){
		pip[0].setFile(Gawain.pathSock+"stdout.txt");
		pip[1].setFile(Gawain.pathSock+"stderr.txt");
		System.setOut(pip[0].getNode());
		System.setErr(pip[1].getNode());
	}
	
	public void close(){		
		try {
			pip[0].close();
			pip[1].close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ObservableList<Mesg> logger = FXCollections.observableArrayList();
	private AtomicBoolean usePool = new AtomicBoolean(false);
	private ArrayList<Mesg> pooler = null;
	
	private AtomicBoolean useHook = new AtomicBoolean(false);		
	private Hooker hooker = null;
	
	public void setPool(){
		pooler = new ArrayList<Mesg>();
		usePool.set(true);
	}
	public Mesg[] getPool(final String file_name){
		usePool.set(false);
		Misc.asynSerialize2file(file_name, pooler);
		return pooler.toArray(new Mesg[0]);
	}
	public Mesg[] getPool(){
		return getPool("");
	}	
	@SuppressWarnings("unchecked")
	public static Mesg[] getPoolFromFile(final String file_name) {
		Object obj = Misc.deserializeFile(file_name);
		if(obj!=null) {
			return ((ArrayList<Mesg>)obj).toArray(new Mesg[0]);
		}
		return null;		
	} 
	
	public void setHook(final Hooker event){
		if(event==null){
			hooker = null;
			useHook.set(false);
		}else{
			hooker = event;
			useHook.set(true);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Pane layout_console(PanBase self){
		
		final TableColumn<Mesg,String> col0 = new TableColumn<>("時間");
		final TableColumn<Mesg,String> col1 = new TableColumn<>("層級");
		final TableColumn<Mesg,String> col2 = new TableColumn<>("訊息");
		
		col0.setCellValueFactory(new PropertyValueFactory<Mesg,String>("col0"));
		col0.setMinWidth(80);
		col1.setCellValueFactory(new PropertyValueFactory<Mesg,String>("col1"));
		col2.setCellValueFactory(new PropertyValueFactory<Mesg,String>("col2"));		
		col2.setMinWidth(200);
		
		final TableView<Mesg> tbl = new TableView<Mesg>();		
		tbl.setEditable(false);
		tbl.getColumns().addAll(col0,col1,col2);
		tbl.setItems(logger);
		
		final Timeline timer = new Timeline(new KeyFrame(
			Duration.seconds(1), 
			e->tbl.scrollTo(tbl.getItems().size()-1)
		));
		timer.setCycleCount(Animation.INDEFINITE);
		
		final JFXToggleButton btnScroll = new JFXToggleButton();
		btnScroll.setSelected(true);
		btnScroll.setOnAction(e->{
			if(btnScroll.isSelected()==true){
				timer.play();
			}else{
				timer.pause();
			}
		});
		btnScroll.getOnAction().handle(null);
		
		final JFXButton btnClear = new JFXButton("清除");
		btnClear.getStyleClass().add("btn-raised-1");
		btnClear.setMaxWidth(Double.MAX_VALUE);
		btnClear.setOnAction(e->tbl.getItems().clear());
				
		final JFXButton btnSave = new JFXButton("保存");
		btnSave.getStyleClass().add("btn-raised-1");
		btnSave.setMaxWidth(Double.MAX_VALUE);
		
		final VBox lay1 = new VBox(
			btnScroll,
			btnClear,
			btnSave	
		);
		lay1.getStyleClass().addAll("box-pad");
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(tbl);
		lay0.setLeft(lay1);
		
		self.stage().setOnCloseRequest(e->{
			pane=Optional.empty();
			timer.stop();
		});
		return lay0;
	}
	
	public PanBase showConsole(){
		if(pane.isPresent()==true){
			return pane.get();
		}
		PanBase obj = new PanBase(){
			@Override
			public Pane eventLayout(PanBase self) {
				return layout_console(self);
			}
		};
		pane = Optional.of(obj.appear());
		return obj;
	}

	public static LogStream getInstance() {
		if(self.isPresent()==false){
			self = Optional.of(new LogStream());
		}
		return self.get();
	}
}
