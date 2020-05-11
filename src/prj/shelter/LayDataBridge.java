package prj.shelter;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.jfoenix.controls.JFXComboBox;
import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import prj.scada.Record;

public class LayDataBridge extends BorderPane {

	private TextField boxQuery = new TextField();
	private JFXComboBox<String> cmbType = new JFXComboBox<String>();
	
	private TableView<String[]> tblTenur = new TableView<>();
	private TableView<String[]> tblProdx = new TableView<>();
	
	@SuppressWarnings("unchecked")
	public LayDataBridge(){

		boxQuery.setOnAction(e->PanBase.self(boxQuery).notifyTask(new TaskList()));
		
		final TableColumn<String[],String> col0 = new TableColumn<>("廠牌");
		final TableColumn<String[],String> col1 = new TableColumn<>("型號");
		final TableColumn<String[],String> col2 = new TableColumn<>("序號");
		tblTenur.setEditable(false);
		tblTenur.getColumns().addAll(col0,col1,col2);
		
		cmbType.getItems().addAll("標籤","儀器","報告");
		cmbType.getSelectionModel().select(0);
		
		HBox lay1 = new HBox(cmbType, boxQuery);
		lay1.getStyleClass().addAll("box-pad");
		
		getStyleClass().addAll("box-pad");
		setTop(lay1);
		setCenter(tblTenur);
	}
		
	private class TaskList extends Task<Integer>{
		int typ;
		String txt;
		PreparedStatement sta;
		ArrayList<String[]> ans = new ArrayList<String[]>();
		TaskList(){
			typ = cmbType.getSelectionModel().getSelectedIndex();
			txt= boxQuery.getText().toLowerCase().trim();
			sta = stmt[typ];			
			setOnSucceeded(e->{
				
			});
		}
		@Override
		protected Integer call() throws Exception {
			updateMessage("開始搜尋");
			if(conn.isPresent()==false){
				return -1;
			}
			try {
				//搜尋方式~~~
				String[] arg, tag;
				switch(typ){
				case 0://標籤
					arg = txt.split("@");
					if(arg.length!=3){
						return -2;
					}
					tag = arg[1].split("_");
					if(tag.length!=2){
						return -3;
					}
					sta.setString(1,"%"+tag[0]+"%");
					sta.setString(2,"%"+tag[1]+"%");
					break;
				case 1://儀器
					sta.setString(1,"%"+txt+"%");
					sta.setString(2,"%"+txt+"%");
					sta.setString(3,"%"+txt+"%");
					break;
				case 2://報告
					break;
				}
				if(sta.execute()==false){
					return -4;//WTF!!!
				}
				int cnt = 0;
				ResultSet rs = sta.getResultSet();		
				while(rs.next()){
					cnt+=1;
					updateMessage("搜尋 "+cnt+"筆資料");
					ans.add(flatten(rs));
				}
			} catch (SQLException e) {
			}	
			return 0;
		}
		private String[] flatten(final ResultSet rs) throws SQLException {			
			
			UUID  uuid = (UUID)rs.getObject(1);
			String[] info = (String[])(rs.getArray(2).getArray());
			
			String[] dat = new String[1+info.length];
			dat[0] = uuid.toString();
			for(int i=0; i<info.length; i++){
				dat[i+1] = (info[i]==null)?(""):(info[i]);
			}
			return dat;
		}
		
	}
	
	private Optional<Connection> conn = Optional.empty();
	
	//查詢方式: "標籤","儀器","報告"
	private PreparedStatement[] stmt = new PreparedStatement[3];

	/**
	 * connect to data base~~~
	 * @param database
	 */
	public void connect(){
		
		final String DEF_ADDR = "localhost";
		
		final String addr = Gawain.prop().getProperty("database", DEF_ADDR);
				
		final Task<Integer> tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				updateMessage("連線至 "+addr);
				Connection cc;
				try{
					Class.forName("org.postgresql.Driver");
				} catch(ClassNotFoundException e){
					Application.invokeAndWait(()->PanBase.notifyError(
						"連線", 
						"內部錯誤(no postgresql)"
					));
					return -1;
				}				
				try {		
					DriverManager.setLoginTimeout(3);
					cc = DriverManager.getConnection(
						"jdbc:postgresql://"+addr+"/bookkeeping",
						"qq","qq"
					);					
				} catch (SQLException e1) {
					try {
						cc = DriverManager.getConnection(
							"jdbc:postgresql://"+DEF_ADDR+"/bookkeeping",
							"qq","qq"
						);
					} catch (SQLException e2) {
						Application.invokeAndWait(()->PanBase.notifyError(
							"連線", 
							"無資料庫!!"
						));
						return -1;
					}					
				}
				
				stmt[0] = cc.prepareStatement(
					"SELECT * FROM tenure"+
					"WHERE"+
					"LOWER(info[1]) SIMILAR TO ? "+
					"AND "+
					"LOWER(info[1]) SIMILAR TO ? "+
					"ORDER BY info[1] DESC" 
				);
				stmt[1] = cc.prepareStatement(
					"SELECT product.info, tenure.info FROM product "+
					"INNER JOIN tenure ON "+
					"product.tid = tenure.id "+
					"AND "+
					"LOWER(tenure.info[2]) LIKE ? "+
					"LOWER(tenure.info[3]) LIKE ? "+
					"LOWER(tenure.info[4]) LIKE ? "+
					"ORDER BY product.info[1] DESC "
				);
				stmt[2] = cc.prepareStatement(
					"SELECT * FROM product " + 
					"WHERE tid=? ORDER BY stamp DESC" 
				);
				conn = Optional.of(cc);
				
				if(addr.equals(DEF_ADDR)==true){
					Application.invokeAndWait(()->PanBase.notifyWarning(
						"連線", 
						"使用本機資料庫!!"
					));
				}				
				return 0;
			}
		};		
		PanBase.self(this).notifyTask(tsk);
	}	
}
