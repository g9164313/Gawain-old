package narl.itrc;

import javafx.beans.property.IntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

public class TabPairBody extends TableView<TabPairItem> {

	private TableColumn<TabPairItem,String> col1, col2;
	
	@SuppressWarnings("unchecked")
	public TabPairBody(String nameCol1, String nameCol2,boolean flag){
		
		setEditable(flag);
		
		col1 = new TableColumn<TabPairItem,String>(nameCol1);
		col1.setCellValueFactory(new PropertyValueFactory<TabPairItem,String>("key"));
		//col1.setCellFactory(TextFieldTableCell.forTableColumn());
		
		col2 = new TableColumn<TabPairItem,String>(nameCol2);
		col2.setCellValueFactory(new PropertyValueFactory<TabPairItem,String>("val"));
		col2.setCellFactory(TextFieldTableCell.forTableColumn());
		if(flag==true){
			col2.setOnEditCommit(hook);
		}
		col2.setPrefWidth(100);
				
		setPrefWidth(170);
		getColumns().addAll(col1,col2);
	}

	private static final String NAME_COL1 = "名稱";
	private static final String NAME_COL2 = "內容";
	
	public TabPairBody(boolean flag){
		this(NAME_COL1,NAME_COL2,false);
	}
	
	public TabPairBody(){
		this(NAME_COL1,NAME_COL2,false);
	}
	
	public TabPairBody addProperty(String name,IntegerProperty value){
		return addProperty(name,value,null);
	}
	public TabPairBody addProperty(String name,IntegerProperty value,String format){
		TabPairItem itm = new TabPairItem(name);
		if(format==null){
			itm.val.bind(value.asString());
		}else{
			itm.val.bind(value.asString(format));
		}
		getItems().add(itm);
		return this;
	}
	
	public TabPairBody addRegList(String tkn,int addr,int size){
		getItems().clear();
		for(int off=0; off<size; off++){			
			String name = String.format("%s%d",tkn,addr+off);
			getItems().add(new TabPairItem(name));
		}
		return this;
	}
	
	public TabPairBody updateValue(int[] data){
		for(int i=0; i<data.length; i++){
			if(getItems().size()<=i){
				break;
			}
			TabPairItem itm = getItems().get(i);
			itm.setVal(String.format("%06d", data[i]));
		}
		return this;
	}
	
	private EventHandler<CellEditEvent<TabPairItem,String>> hook = new EventHandler<CellEditEvent<TabPairItem,String>>(){
		@Override
		public void handle(CellEditEvent<TabPairItem, String> event) {
			int row = event.getTablePosition().getRow();
			eventCommit(
				event.getTableView().getItems().get(row),
				event.getNewValue()
			);
		}
	};
	
	protected void eventCommit(TabPairItem itm, String newValue){}
}
