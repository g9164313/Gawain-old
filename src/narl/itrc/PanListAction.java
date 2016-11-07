package narl.itrc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.jfoenix.controls.JFXButton;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;

public abstract class PanListAction extends VBox {
	
	private final String STR_RUN="執行";
	private final String STR_STOP="停止";
	
	//private ObservableList<ListItem> datAction = FXCollections.observableArrayList();
	//private TableView<ListItem> lstAction = new TableView<ListItem>(datAction);
	private TableView<PropBundle> lstAction = new TableView<PropBundle>();
	
	private JFXButton btnUp = new JFXButton("上移");
	private JFXButton btnDw = new JFXButton("下移");
	private JFXButton btnAdd = new JFXButton("＋新增");	
	private JFXButton btnKick = new JFXButton(STR_RUN);
	
	public PanListAction(){
		
		getStyleClass().add("vbox-small");
		setAlignment(Pos.TOP_LEFT);
		
		TableColumn<PropBundle,String> col;

		col = new TableColumn<PropBundle,String>("項次");
		col.setCellValueFactory(new PropertyValueFactory<>("desc2"));
		col.prefWidthProperty().set(50);
		lstAction.getColumns().add(col);
		
		col = new TableColumn<PropBundle,String>("動作");
		col.setCellValueFactory(new PropertyValueFactory<>("value"));
		col.prefWidthProperty().set(170);
		lstAction.getColumns().add(col);

		HBox lay0 = new HBox();
		lay0.getStyleClass().add("hbox-small");
		
		btnUp.setOnAction(eventMoveItem);
		btnUp.getStyleClass().add("btn-raised");
		btnUp.prefWidthProperty().bind(lay0.widthProperty().divide(2));
		
		btnDw.setOnAction(eventMoveItem);
		btnDw.getStyleClass().add("btn-raised");
		btnDw.prefWidthProperty().bind(lay0.widthProperty().divide(2));
		
		lay0.prefWidthProperty().bind(lstAction.widthProperty());
		lay0.getChildren().addAll(btnUp,btnDw);
		
		btnAdd.setOnAction(eventAddItem);
		btnAdd.getStyleClass().add("btn-raised");
		btnAdd.prefWidthProperty().bind(lstAction.widthProperty());
		
		btnKick.setOnAction(eventKickItem);
		btnKick.getStyleClass().add("btn-raised");
		btnKick.prefWidthProperty().bind(lstAction.widthProperty());

		init_table();
		init_token();
		
		getChildren().addAll(lstAction,lay0,btnAdd,btnKick);
	}

	private final int MENU_SAVE=0;
	private final int MENU_LOAD=1;
	
	private void init_table(){
		ContextMenu menu = new ContextMenu();
		MenuItem itm;
		
		itm = new MenuItem("編輯");
		itm.setOnAction(eventEditItem);
		menu.getItems().add(itm);
		
		itm = new MenuItem("刪除");
		itm.setOnAction(eventDeleteItem);
		menu.getItems().add(itm);

		itm = new MenuItem("儲存");
		itm.setOnAction(eventSaveLoad);
		itm.setUserData(MENU_SAVE);
		menu.getItems().add(itm);
		
		itm = new MenuItem("讀取");
		itm.setOnAction(eventSaveLoad);
		itm.setUserData(MENU_LOAD);
		menu.getItems().add(itm);
		
		lstAction.setContextMenu(menu);
		lstAction.setRowFactory(new Callback<TableView<PropBundle>,TableRow<PropBundle>>(){
			@Override
			public TableRow<PropBundle> call(TableView<PropBundle> param) {
				final TableRow<PropBundle> row = new TableRow<>();
				row.setOnMouseClicked(new EventHandler<MouseEvent>(){
					@Override
					public void handle(MouseEvent event) {
						if(event.getClickCount() == 2 && (!row.isEmpty())){
							eventEditItem.handle(null);
						}
					}
				});
				return row;
			}
		});
	}

	private EventHandler<ActionEvent> eventEditItem = new EventHandler<ActionEvent>(){
		
		class PanEditAction extends GridPane {
			private ComboBox<Token> cmbDesc;
			private TextField txtParm;
			public PanEditAction(){
				getStyleClass().add("grid-small");
				cmbDesc = new ComboBox<Token>();
				cmbDesc.setPrefWidth(200);
				cmbDesc.setEditable(false);			
				txtParm = new TextField();			
				txtParm.setPrefWidth(200);
				//GridPane.setHgrow(cmbDesc,Priority.ALWAYS);
				//GridPane.setHgrow(txtParm,Priority.ALWAYS);
				addRow(0,new Label("名稱："),cmbDesc);
				addRow(1,new Label("參數："),txtParm);
			}
			public void itm2box(PropBundle itm){
				if(cmbDesc.getItems().size()==0){
					cmbDesc.getItems().addAll(lstToken);
				}
				String t1 = itm.desc1.get();
				for(int i=0; i<lstToken.size(); i++){				
					String t2 = cmbDesc.getItems().get(i).desc;
					if(t1.equalsIgnoreCase(t2)==true){
						cmbDesc.getSelectionModel().select(i);
						txtParm.setTooltip(new Tooltip(lstToken.get(i).usage));
						t1 = itm.name.get();
						int pos = t1.indexOf(',');
						if(pos<0){
							txtParm.setText("");
						}else{
							txtParm.setText(t1.substring(pos+1));
						}					
						return;
					}
				}
				txtParm.setText("???");
			}
			public void box2itm(PropBundle itm){
				Token tkn = cmbDesc.getSelectionModel().getSelectedItem();
				String name = tkn.name;
				String parm = txtParm.getText().trim();
				if(parm.length()!=0){
					name = name + "," + parm;
				}
				itm.name.set(name);
				itm.desc1.set(tkn.desc);
				itm.value.set(evaluate(name,tkn.desc));
			}
		}
		private final PanEditAction panEdit = new PanEditAction();
		
		@Override
		public void handle(ActionEvent event) {
			PropBundle itm = lstAction.getSelectionModel().getSelectedItem();
			if(itm==null){
				return;
			}
			panEdit.itm2box(itm);
			Dialog<Void> dlg = new Dialog<Void>();			
			dlg.getDialogPane().setContent(panEdit);
			dlg.getDialogPane().getButtonTypes().addAll(
				ButtonType.APPLY,
				ButtonType.CANCEL
			);
			dlg.setResultConverter(new Callback<ButtonType,Void>(){
				@Override
				public Void call(ButtonType param) {
					if(param==ButtonType.CANCEL){
						return null;
					}
					panEdit.box2itm(itm);//apply data~~~
					lstAction.refresh();
					return null;
				}
			});
			dlg.showAndWait();
		}
	};
	
	private EventHandler<ActionEvent> eventDeleteItem = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			ObservableList<PropBundle> lst = lstAction.getItems();
			int cnt = lstAction.getSelectionModel().getSelectedItems().size();
			if(cnt==0){
				//delete all item!!!
			}else{
				for(PropBundle itm:lstAction.getSelectionModel().getSelectedItems()){
					lst.remove(itm);
				}
				for(int i=0; i<lst.size(); i++){
					lst.get(i).desc2.set(String.format("%d",i+1));
				}
			}
			lstAction.getSelectionModel().clearSelection();
		}
	};
	
	private EventHandler<ActionEvent> eventMoveItem = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			int cnt = lstAction.getSelectionModel().getSelectedItems().size();
			if(cnt==0){
				return;
			}
			//TODO: use sort method to replace index, it should work~~~~
			JFXButton btn = (JFXButton)(event.getSource());
			if(btn==btnUp){
				
			}else if(btn==btnDw){
				
			}
			//ObservableList<ListItem> lst = lstAction.getItems();
			//lst.sort(c);
		}
	};

	private EventHandler<ActionEvent> eventAddItem = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			ObservableList<PropBundle> lst = lstAction.getItems();
			Token tkn = lstToken.get(0);//use first token as default action~~~
			PropBundle itm = new PropBundle(
				tkn.name,
				tkn.desc,
				evaluate(tkn.name,tkn.desc)
			);			
			lst.add(itm);
			itm.desc2.set(String.format("%d",lst.size()));
		}
	};
	
	private EventHandler<ActionEvent> eventSaveLoad = new EventHandler<ActionEvent>(){
		class TskSave extends TskDialog{
			public File target = null;
			public TskSave(){
				setName("儲存工作清單");
			}
			@Override
			public int looper(Task<Integer> tsk) {				
				try {
					FileWriter fs = new FileWriter(target);
					ObservableList<PropBundle> lst = lstAction.getItems();
					int cnt = lst.size();
					for(int i=0; i<cnt; i++){
						PropBundle itm = lst.get(i);
						fs.write(itm.name.get()+";"+itm.desc1.get()+";"+itm.value.get()+"\n");
						log("寫入："+itm.name.get());
						setProgress(i,cnt);
					}
					fs.close();
				} catch (IOException e) {					
					log(e.getMessage());
					tsk.cancel();
					return -1;
				}							
				return 1;
			}
		};
		class TskLoad extends TskDialog{
			public File target = null;
			public TskLoad(){
				setName("讀取工作清單");
			}
			@Override
			public int looper(Task<Integer> tsk) {
				ObservableList<PropBundle> lst = lstAction.getItems();
				lst.clear();
				try {
					FileReader fs = new FileReader(target);
					BufferedReader bf = new BufferedReader(fs);
					String txt = null;
					while((txt=bf.readLine())!=null){
						String[] parm = txt.split(";");
						if(parm.length!=3){
							continue;
						}
						//how to check whether token is valid??
						PropBundle itm = new PropBundle(parm[0],parm[1],parm[2]);
						lst.add(itm);
						itm.desc2.set(String.format("%d",lst.size()));
						log("讀取："+parm[0]);
					}
					bf.close();
					fs.close();
				} catch (IOException e) {					
					log(e.getMessage());
					tsk.cancel();
					return -1;
				}
				return 1;
			}
		};
		private FileChooser fch = new FileChooser();
		@Override
		public void handle(ActionEvent event) {
			int cmd = (int) ((MenuItem)event.getSource()).getUserData();
			//fc.getExtensionFilters().add(new ExtensionFilter("Text Files","*.txt"));
			Window ww = PanListAction.this.getScene().getWindow();
			switch(cmd){
			case MENU_SAVE:
				TskSave tskSave = new TskSave();
				fch.setTitle("儲存工作清單");
				tskSave.target = fch.showSaveDialog(ww);		
				if(tskSave.target==null){
					return;
				}
				tskSave.appear();
				break;
			case MENU_LOAD:
				TskLoad tskLoad = new TskLoad();
				fch.setTitle("讀取工作清單");				
				tskLoad.target = fch.showOpenDialog(ww);
				if(tskLoad.target==null){
					return;
				}
				tskLoad.appear();
				break;
			}
		}
	};

	private EventHandler<ActionEvent> eventKickItem = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			if(btnAdd.isDisabled()==false){
				idxLooper = resLooper = 0;//reset it~~~
				btnKick.setText(STR_STOP);
				btnAdd.setDisable(true);				
				eventStart();
			}else{
				eventStop();
				btnAdd.setDisable(false);
				btnKick.setText(STR_RUN);
				idxLooper = -1;//reset it~~~
			}
		}
	};
	//---------------------------//
	
	//protected abstract ListItem eventAddItem();
	protected abstract int  eventStage(PropBundle itm,String[] parm);//don't block this method!!!
	protected abstract void eventStart();//don't block this method!!!
	protected abstract void eventStop();//don't block this method!!!

	private final String CMD_DELAY= "DELAY";
	private final String CMD_JREP = "JUMP_REPEAT";
	private final String CMD_JEQU = "JUMP_EQUAL";
	private final String CMD_JNEQ = "JUMP_NOT_EQUAL";
	
	protected int idxLooper = -1;
	protected int resLooper =  0;//keep the previous result~~
	public boolean isRunning(){
		return (idxLooper>=0)?(true):(false);
	}	
	public void eventLooper(){
		if(isRunning()==false){
			return;
		}
		ObservableList<PropBundle> lst = lstAction.getItems();
		checkIndicator(lst,idxLooper);
		int total = lst.size();
		if(idxLooper>=total){
			//finally we done~~~
			btnKick.fire();
			return;
		}else if(idxLooper<0){
			return;//WTF???
		}
		
		PropBundle itm = lst.get(idxLooper);
		String[] parm = itm.name.get().split(",");
		
		if(parm[0].equals(CMD_DELAY)==true){
			resLooper = cmdDelay(itm,parm);
		}else if(parm[0].equals(CMD_JREP)==true){
			resLooper = cmdJumpRepeat(itm,parm);
		}else if(parm[0].equals(CMD_JEQU)==true){
			resLooper = cmdJumpEQ_NEQ(itm,parm,true);
		}else if(parm[0].equals(CMD_JNEQ)==true){
			resLooper = cmdJumpEQ_NEQ(itm,parm,false);
		}else{
			resLooper = eventStage(itm,parm);
		}		
		if(resLooper>0){
			idxLooper++;
		}else if(resLooper==0){
			return;
		}else if(resLooper<0){			
			btnKick.fire();//escape!!!
		}
	}
	
	private void checkIndicator(ObservableList<PropBundle> lst,int idx){
		PropBundle itemPre=null,itemCur=null;
		String lineNum;		
		if(0<=(idxLooper-1)){
			itemPre = lst.get(idxLooper-1);
		}
		if(idxLooper<lst.size()){
			itemCur = lst.get(idxLooper);
		}
		//remove previous indicator
		if(itemPre!=null){
			lineNum = itemPre.desc2.get();
			if(lineNum.charAt(0)=='-'){
				lineNum = lineNum.substring(2);
				itemPre.desc2.setValue(lineNum);
			}
		}
		//mark a new indicator
		if(itemCur!=null){
			lineNum = itemCur.desc2.get();
			if(lineNum.charAt(0)!='-'){
				itemCur.desc2.setValue("->"+lineNum);
			}
		}
	}
	
	private int cmdDelay(PropBundle itm,String[] parm){
		int preTick = itm.arg1.get();
		if(preTick==0){
			//this is the first initialization
			if(parm.length<2){
				return 1;//still work, but no function				
			}
			itm.arg1.set((int)System.currentTimeMillis());
			itm.arg2.set((int)Misc.phyConvert(parm[1],"ms"));			
		}else{
			int curTick = (int)System.currentTimeMillis();
			int diff = itm.arg2.get();
			if((curTick-preTick)>=diff){
				itm.arg1.set(0);//reset this for the next turn~~~
				return 1;//goto next stage~~~
			}
		}
		return 0;//stay to the origin stage~~~
	}
	
	private int cmdJumpRepeat(PropBundle itm,String[] parm){
		int cnt = itm.arg1.get();
		if(cnt==0){
			//this is the first initialization
			if(parm.length<3){
				return 1;//still work, but no function
			}
			try{
				itm.arg1.set(1);
				itm.arg2.set(Integer.valueOf(parm[2]));//repeat count
				itm.arg3.set(Integer.valueOf(parm[1])-1);//jump to this line~~
			}catch(NumberFormatException e){
				return -1;//we fail, so just skip all actions!!!
			}
		}else{
			if(cnt>=itm.arg2.get()){
				itm.arg1.set(0);//reset it for next turn~~~
				return 1;
			}
			itm.arg1.set(cnt+1);
		}
		//trick, keeping the origin stage will not change looper index(0-base)~~~
		idxLooper = itm.arg3.get();
		return 0;
	}

	private int cmdJumpEQ_NEQ(PropBundle itm,String[] parm,boolean eq_ne){
		try{
			if(parm.length<3){
				return -1;//the number of parameter is not enough!!!
			}
			int result = Integer.valueOf(parm[1]);//previous value
			int line_num = Integer.valueOf(parm[2]);//jump to this line~~
			//trick, keeping the origin stage will not change looper index(0-base)~~~
			if(eq_ne==true){
				if(result==resLooper){
					idxLooper = line_num - 1;
					return 0;
				}
			}else{
				if(result!=resLooper){
					idxLooper = line_num - 1;
					return 0;
				}
			}	
		}catch(NumberFormatException e){
			return -1;//we fail, so just skip all actions!!!
		}		
		return 1;
	}

	public class Token {
		public String name;//this introduces token and parameter~~
		public String desc;//evaluate item by this description~~
		public String usage;
		public Token(String txt1,String txt2,String txt3){
			name = txt1.toUpperCase();
			desc = txt2;
			usage = txt3;
		}
		@Override public String toString(){
			return desc;
		}
	}

	protected String evaluate(String name,String desc){
		String[] pam = name.split(",");
		if(pam.length<=1){
			return desc;
		}
		String[] txt = desc.split("\\?");
		String val = "";
		for(int i=0; i<txt.length; i++){
			val = val + txt[i];
			if((i+1)<pam.length){
				val = val + pam[i+1];
			}else if((i+1)<txt.length){
				val = val + '?';//keep this~~~
			}
		}
		return val;
	}

	protected ArrayList<Token> lstToken = new ArrayList<Token>();
	private void init_token(){
		//these are default tokens~~~
		lstToken.add(new Token(
			CMD_DELAY,
			"延遲?",
			"範例：5sec 或 3min"
		));
		lstToken.add(new Token(
			CMD_JREP,
			"跳至?項，重複?次",
			"範例：5,3 表示跳至第5項並重複3次此項跳躍"
		));
		lstToken.add(new Token(
			CMD_JEQU,
			"前次回傳值等於?時，跳至?項",
			"範例：5,3 回傳值等於5時，跳至第3項，不然繼續"
		));
		lstToken.add(new Token(
			CMD_JNEQ,
			"前次回傳值不等於?時，跳至?項",
			"範例：5,3 回傳值不等於5時，跳至第3項，不然繼續"
		));
	}
	
	public void bindHeight(ReadOnlyDoubleProperty prop){
		lstAction.prefHeightProperty().bind(prop
			.subtract(btnAdd.heightProperty())
			.subtract(btnKick.heightProperty())
		);
	}
	
	public void bindWidth(ReadOnlyDoubleProperty prop){
		lstAction.prefWidthProperty().bind(prop);
	}
}

