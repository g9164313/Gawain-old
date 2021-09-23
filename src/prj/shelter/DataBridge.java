package prj.shelter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.sun.glass.ui.Application;
import javafx.concurrent.Task;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class DataBridge{
	
	public class Owner {
		public String[] inf;
	};
	public class Tenur {
		public String[] inf;
	};
	public class Prodx {
		public String[] inf;
	};
	
	public static class Stuff {
		UUID uuid;
		String[] info;
		String[] appx;//product scribble
		/**
		 * FMT_F2 = 2;//
		 * FMT_F3 = 3;//γ反應報告
		 * FMT_F4 = 4;//效率報告
		 * FMT_F5 = 5;//活度報告
		 */
		int fmt = -1;//product format
		
		private Stuff getInfo(final ResultSet rs){
			try {
				uuid = rs.getObject(1, UUID.class);
				info = (String[])(rs.getArray(2).getArray());
			} catch (SQLException e) {
				e.printStackTrace();
			}			
			return this;
		}
		private Stuff getProdx(final ResultSet rs){
			try {
				uuid= rs.getObject(1, UUID.class);
				fmt = rs.getInt(2);
				info= (String[])(rs.getArray(3).getArray());
				appx= (String[])(rs.getArray(4).getArray());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return this;
		}
		public String getFormatText(){
			switch(fmt){
			case 2: return "F2 報告";
			case 3: return "γ反應報告";
			case 4: return "效率報告";
			case 5: return "活度報告";
			}
			return "???";
		}
	};	
	public static interface Callback1{
		void getResult(final Stuff[] lst);
	};
	public static interface Callback2{
		void getResult(final Stuff[][] lst);
	};
	
	public void list_tenur_by_name(
		final String name,
		final Callback1 event		
	){
		final Task<Stuff[]> tsk = new Task<Stuff[]>(){
			@Override
			protected Stuff[] call() throws Exception {
				ArrayList<Stuff> lst = new ArrayList<Stuff>();
				//retrieve list of tenure by name.
				s_list_tenur_by_name.setString(1, name);
				if(s_list_tenur_by_name.execute()==false){
					return null;//WTF!!!
				}				
				ResultSet rs = s_list_tenur_by_name.getResultSet();		
				while(rs.next()){
					Stuff itm = new Stuff().getInfo(rs);
					lst.add(itm);
					updateMessage("搜尋 "+itm.info[0]);					
				}
				return lst.toArray(new Stuff[0]);
			}
		};
		tsk.setOnSucceeded(e->{
			try {
				event.getResult(tsk.get());
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		});
		Gawain.mainPanel.notifyTask(tsk);
	}
	
	class FetchLastProdx extends Task<Stuff[][]>{
		/**
		 * retrieve last product with tenure.<p>
		 * The array content will be :
		 * {
		 * 	{TENUR, PRODX, PRODX...},
		 *  {TENUR, PRODX, PRODX...}
		 * }
		 */
		PreparedStatement sta1 = null;
		PreparedStatement sta2 = s_list_prodx_by_tid;
		FetchLastProdx(String... arg){
			try {
				if(arg.length==1){
					sta1 = s_list_tenur_by_name;
					sta1.setString(1, arg[0]);
				}else if(arg.length==2){
					sta1 = s_list_tenur_by_code;
					s_list_tenur_by_code.setString(1, arg[0]);
					s_list_tenur_by_code.setString(2, arg[1]);
					s_list_tenur_by_code.setString(3, arg[0]);
					s_list_tenur_by_code.setString(4, arg[1]);
				}
			} catch (SQLException e) {
				sta1 = null;
				e.printStackTrace();
			}	
		}		
		@Override
		protected Stuff[][] call() throws Exception {
			if(sta1==null){
				updateMessage("內部錯誤 -1");
				throw new Exception();
			}
			ArrayList<Stuff[]> lst1 = new ArrayList<Stuff[]>();
			if(sta1.execute()==false){
				updateMessage("內部錯誤 -2");
				throw new Exception();//WTF
			}
			ResultSet rs1 = sta1.getResultSet();	
			while(rs1.next()){
				Stuff itm1 = new Stuff().getInfo(rs1);					
				updateMessage("搜尋 "+itm1.info[0]);
				
				ArrayList<Stuff> lst2 = new ArrayList<Stuff>();
				lst2.add(itm1);					
				sta2.setObject(1, itm1.uuid);
				if(sta2.execute()==false){
					continue;
				}
				ResultSet rs2 = sta2.getResultSet();
				while(rs2.next()){
					Stuff itm2 = new Stuff().getProdx(rs2);
					lst2.add(itm2);
					updateMessage("配對 "+itm2.info[0]);
				}
				
				lst1.add(lst2.toArray(new Stuff[0]));
			}
			return lst1.toArray(new Stuff[0][]);
		}
	};

	public void list_last_prodx(
		final String name,
		final Callback2 event
	){
		final Task<Stuff[][]> tsk = new FetchLastProdx(name);
		tsk.setOnSucceeded(e->{
			try {
				event.getResult(tsk.get());
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		});
		Gawain.mainPanel.notifyTask(tsk);
	}
	public void list_last_prodx(
		final String code1,
		final String code2,
		final Callback2 event
	){
		final Task<Stuff[][]> tsk = new FetchLastProdx(code1, code2);
		tsk.setOnSucceeded(e->{
			try {
				event.getResult(tsk.get());
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		});
		Gawain.mainPanel.notifyTask(tsk);
	}

	private Optional<Connection> link = Optional.empty();
	
	private PreparedStatement s_list_tenur_by_name, s_list_tenur_by_code;
	private PreparedStatement s_list_prodx_by_tid;

	private final static String LIST_TENUR_BY_NAME = 
		"SELECT  "+
		"DISTINCT ON (tenure.info[1]) "+
		"tenure.id, tenure.info "+
		"FROM tenure WHERE  "+
		"tenure.info[1] LIKE ? "+
		"ORDER BY tenure.info[1] ASC LIMIT 100";
	private final static String LIST_TENUR_BY_CODE = 
		"SELECT  "+
		"DISTINCT ON (tenure.info[1]) "+
		"tenure.id, tenure.info "+
		"FROM tenure WHERE  "+
		"(REPLACE(LOWER(tenure.info[3]),' ','') LIKE ? "+
		"OR "+
		"REPLACE(LOWER(tenure.info[3]),' ','') LIKE ?) "+
		"AND "+
		"(REPLACE(LOWER(tenure.info[4]),' ','') LIKE ? "+
		"OR "+
		"REPLACE(LOWER(tenure.info[4]),' ','') LIKE ?) "+
		"ORDER BY tenure.info[1] ASC LIMIT 100";
	
	private final static String LIST_PRODX_BY_TID =
		"SELECT "+
		"DISTINCT ON (product.format) "+
		"product.id, product.format, "+
		"product.info, product.scribble "+
		"FROM product WHERE "+
		"product.tid=? "+
		"ORDER BY product.format,product.info[1] DESC LIMIT 100";
			
	/*private final static String ss1 = 
		"SELECT "+ 
		"DISTINCT ON (tenure.info[1])  "+ 
		"tenure.id, tenure.info, product.info"+ 
		"FROM tenure "+ 
		"INNER JOIN product  "+ 
		"ON tenure.id=product.tid "+ 
		"WHERE  "+ 
		"(REPLACE(LOWER(tenure.info[3]),' ','') LIKE ? "+ 
		"OR "+ 
		"REPLACE(LOWER(tenure.info[3]),' ','') LIKE ?) "+ 
		"AND "+ 
		"(REPLACE(LOWER(tenure.info[4]),' ','') LIKE ? "+ 
		"OR "+ 
		"REPLACE(LOWER(tenure.info[4]),' ','') LIKE ?) "+
		"ORDER BY tenure.info[1], product.info[1] DESC ";*/
	
	/**
	 * connect to data base~~~
	 * @param database
	 */
	public void connect(){
		
		if(link.isPresent()==true){
			return;
		}
		
		final String ADDR_DEF = "localhost:8765";
		
		final String addr = Gawain.prop().getProperty("database", ADDR_DEF);
				
		final Task<Integer> tsk = new Task<Integer>(){
			boolean connecting(String address){
				updateMessage("連線至 "+address);
				try {
					Connection conn = DriverManager.getConnection(
						"jdbc:postgresql://"+address+"/bookkeeping",
						"qq","qq"
					);
					link = Optional.of(conn);
				} catch (SQLException e2) {
					link = Optional.empty();
					return false;
				}
				return true;
			} 
			@Override
			protected Integer call() throws Exception {
				try{
					Class.forName("org.postgresql.Driver");
					DriverManager.setLoginTimeout(3);
				} catch(ClassNotFoundException e){
					Application.invokeAndWait(()->PanBase.notifyError(
						"連線", 
						"內部錯誤(no postgresql)"
					));
					return -1;
				}
				if(connecting(addr)==false){
					if(connecting(ADDR_DEF)==false){
						Application.invokeAndWait(()->PanBase.notifyError(
							"連線", 
							"無法建立連線"
						));
						return -1;
					}else{
						//notify user whether we use local data-base
						Application.invokeAndWait(()->PanBase.notifyWarning(
							"連線", 
							"使用本地資料庫!!"
						));
					}
				}
				//prepare statement~~~
				s_list_tenur_by_name= link.get().prepareStatement(LIST_TENUR_BY_NAME);
				s_list_tenur_by_code= link.get().prepareStatement(LIST_TENUR_BY_CODE);
				s_list_prodx_by_tid = link.get().prepareStatement(LIST_PRODX_BY_TID);					
				return 0;
			}
		};
		Gawain.mainPanel.notifyTask(tsk);
	}
	
	private static Optional<DataBridge> self = Optional.empty();
	
	public static DataBridge getInstance(){
		DataBridge obj;
		if(self.isPresent()==true){
			obj = self.get();
		}else{
			obj = new DataBridge();
			self = Optional.of(obj);
		}
		obj.connect();
		return obj;
	}
}


