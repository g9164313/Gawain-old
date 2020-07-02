package narl.itrc.init;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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
import narl.itrc.PanBase;

public class LogStream {

	private static final SimpleDateFormat F_STAMP = new SimpleDateFormat("MM/dd  HH:mm:ss.SSS");
	
	public interface Hooker {
		void callback(long tick, char level, String text);
	};
	
	public class Mesg {		
		final Timestamp stm;
		final char tkn;
		final ByteArrayOutputStream txt = new ByteArrayOutputStream();	
		public Mesg(final char level){
			stm = new Timestamp(System.currentTimeMillis());
			tkn = level;
		}
		public String getCol0() { return F_STAMP.format(stm); }
		public String getCol1() { return ""+tkn; }
		public String getCol2() { return txt.toString(); }
		public long   getTick() { return stm.getTime(); }
		public String getText() { return txt.toString(); }
		
		private void write(int b){
			//character may be UTF-8 word.
			//Keep them all, then convert all to one string.
			txt.write(b);
		}
	};
	
	private class Pipe extends OutputStream {
		Optional<Mesg> box = Optional.empty();
		FileOutputStream fid;
		PrintStream p_in;
		PrintStream p_out;
		
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
				commit(box.get());
				box = Optional.empty();
				break;
			case 021://verbose message token
				box = Optional.of(new Mesg('V'));
				break;
			case 022://warning message token
				box = Optional.of(new Mesg('W'));
				break;
			case 023://error message token
				box = Optional.of(new Mesg('E'));		
				break;
			default:
				p_out.write(b);
				if(fid!=null){ fid.write(b); }
				break;
			}
			if(box.isPresent()==true){
				box.get().write(b);
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
	public ArrayList<Mesg> getPool(){
		usePool.set(false);
		return pooler;
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
	
	private synchronized void commit(final Mesg msg){
		//upload message~~~
		//1.logger always need it
		if(logger.size()>=200){
			logger.remove(0, 100);
		}else{
			logger.add(msg);
		}
		//2.Should we put it in pool?
		if(usePool.get()==true){
			pooler.add(msg);
		}
		//3.check hooker
		if(useHook.get()==true){
			hooker.callback(
				msg.getTick(), 
				msg.tkn, 
				msg.getText()
			);
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
