package narl.itrc;

import narl.itrc.WidTextSheet.RowVector;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

public class WidTextSheet extends TableView<RowVector>{

	public WidTextSheet(){
		setEditable(true);
		add_column(0,"項目\\項次");		
	}
	
	private void add_column(int idx, String title){
		TableColumn<RowVector,String> col = new TableColumn<RowVector,String>(title);
		col.setSortable(false);
		col.setCellValueFactory(new PropertyValueFactory<RowVector,String>("col"+idx));
		col.setCellFactory(TextFieldTableCell.forTableColumn());
		getColumns().add(col);
	}
	
	public WidTextSheet setTitle(String... names){
		ObservableList<RowVector> lst = getItems();
		int cnt = lst.size() - names.length;
		if(cnt<0){
			cnt = cnt * -1;
			for(int i=0; i<cnt; i++){
				lst.add(new RowVector());
			}
		}
		for(int i=0; i<names.length; i++){
			lst.get(i).col[0].set(names[i]);
		}
		return this;
	}
	
	public WidTextSheet setValue(int colIdx, int rowIdx, String txt){
		
		if(rowIdx<=0){
			return this;			
		}
		rowIdx = rowIdx - 1;
		
		if(colIdx>100){
			return this;			
		}
		
		ObservableList<RowVector> lstRow = getItems();
		int cnt = 0;
		cnt = lstRow.size() - rowIdx - 1;
		if(cnt<0){
			cnt = cnt * -1;
			for(int i=0; i<cnt; i++){
				lstRow.add(new RowVector());
			}
		}
		
		ObservableList<TableColumn<RowVector, ?>> lstCol = getColumns();
		cnt = lstCol.size() - colIdx - 1;
		if(cnt<=0){
			cnt = cnt * -1;
			int beg = lstCol.size();
			for(int i=0; i<cnt; i++){
				add_column(beg+i,String.format("%d",beg+i));
			}
		}
		
		lstRow.get(rowIdx).col[colIdx].set(txt);
		return this;
	}
	
	public static class RowVector{
		public SimpleStringProperty[] col = {
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
				new SimpleStringProperty(),
		};
		public void setCol0(String val){ col[0].set(val); }
		public String getCol0(){ return col[0].get(); }
		public void setCol1(String val){ col[1].set(val); }
		public String getCol1(){ return col[1].get(); }
		public void setCol2(String val){ col[2].set(val); }
		public String getCol2(){ return col[2].get(); }
		public void setCol3(String val){ col[3].set(val); }
		public String getCol3(){ return col[3].get(); }
		public void setCol4(String val){ col[4].set(val); }
		public String getCol4(){ return col[4].get(); }
		public void setCol5(String val){ col[5].set(val); }
		public String getCol5(){ return col[5].get(); }
		public void setCol6(String val){ col[6].set(val); }
		public String getCol6(){ return col[6].get(); }
		public void setCol7(String val){ col[7].set(val); }
		public String getCol7(){ return col[7].get(); }
		public void setCol8(String val){ col[8].set(val); }
		public String getCol8(){ return col[8].get(); }
		public void setCol9(String val){ col[9].set(val); }
		public String getCol9(){ return col[9].get(); }
		public void setCol10(String val){ col[10].set(val); }
		public String getCol10(){ return col[10].get(); }
		public void setCol11(String val){ col[11].set(val); }
		public String getCol11(){ return col[11].get(); }
		public void setCol12(String val){ col[12].set(val); }
		public String getCol12(){ return col[12].get(); }
		public void setCol13(String val){ col[13].set(val); }
		public String getCol13(){ return col[13].get(); }
		public void setCol14(String val){ col[14].set(val); }
		public String getCol14(){ return col[14].get(); }
		public void setCol15(String val){ col[15].set(val); }
		public String getCol15(){ return col[15].get(); }
		public void setCol16(String val){ col[16].set(val); }
		public String getCol16(){ return col[16].get(); }
		public void setCol17(String val){ col[17].set(val); }
		public String getCol17(){ return col[17].get(); }
		public void setCol18(String val){ col[18].set(val); }
		public String getCol18(){ return col[18].get(); }
		public void setCol19(String val){ col[19].set(val); }
		public String getCol19(){ return col[19].get(); }
		public void setCol20(String val){ col[20].set(val); }
		public String getCol20(){ return col[20].get(); }
		public void setCol21(String val){ col[21].set(val); }
		public String getCol21(){ return col[21].get(); }
		public void setCol22(String val){ col[22].set(val); }
		public String getCol22(){ return col[22].get(); }
		public void setCol23(String val){ col[23].set(val); }
		public String getCol23(){ return col[23].get(); }
		public void setCol24(String val){ col[24].set(val); }
		public String getCol24(){ return col[24].get(); }
		public void setCol25(String val){ col[25].set(val); }
		public String getCol25(){ return col[25].get(); }
		public void setCol26(String val){ col[26].set(val); }
		public String getCol26(){ return col[26].get(); }
		public void setCol27(String val){ col[27].set(val); }
		public String getCol27(){ return col[27].get(); }
		public void setCol28(String val){ col[28].set(val); }
		public String getCol28(){ return col[28].get(); }
		public void setCol29(String val){ col[29].set(val); }
		public String getCol29(){ return col[29].get(); }
		public void setCol30(String val){ col[30].set(val); }
		public String getCol30(){ return col[30].get(); }
		public void setCol31(String val){ col[31].set(val); }
		public String getCol31(){ return col[31].get(); }
		public void setCol32(String val){ col[32].set(val); }
		public String getCol32(){ return col[32].get(); }
		public void setCol33(String val){ col[33].set(val); }
		public String getCol33(){ return col[33].get(); }
		public void setCol34(String val){ col[34].set(val); }
		public String getCol34(){ return col[34].get(); }
		public void setCol35(String val){ col[35].set(val); }
		public String getCol35(){ return col[35].get(); }
		public void setCol36(String val){ col[36].set(val); }
		public String getCol36(){ return col[36].get(); }
		public void setCol37(String val){ col[37].set(val); }
		public String getCol37(){ return col[37].get(); }
		public void setCol38(String val){ col[38].set(val); }
		public String getCol38(){ return col[38].get(); }
		public void setCol39(String val){ col[39].set(val); }
		public String getCol39(){ return col[39].get(); }
		public void setCol40(String val){ col[40].set(val); }
		public String getCol40(){ return col[40].get(); }
		public void setCol41(String val){ col[41].set(val); }
		public String getCol41(){ return col[41].get(); }
		public void setCol42(String val){ col[42].set(val); }
		public String getCol42(){ return col[42].get(); }
		public void setCol43(String val){ col[43].set(val); }
		public String getCol43(){ return col[43].get(); }
		public void setCol44(String val){ col[44].set(val); }
		public String getCol44(){ return col[44].get(); }
		public void setCol45(String val){ col[45].set(val); }
		public String getCol45(){ return col[45].get(); }
		public void setCol46(String val){ col[46].set(val); }
		public String getCol46(){ return col[46].get(); }
		public void setCol47(String val){ col[47].set(val); }
		public String getCol47(){ return col[47].get(); }
		public void setCol48(String val){ col[48].set(val); }
		public String getCol48(){ return col[48].get(); }
		public void setCol49(String val){ col[49].set(val); }
		public String getCol49(){ return col[49].get(); }
		public void setCol50(String val){ col[50].set(val); }
		public String getCol50(){ return col[50].get(); }
		public void setCol51(String val){ col[51].set(val); }
		public String getCol51(){ return col[51].get(); }
		public void setCol52(String val){ col[52].set(val); }
		public String getCol52(){ return col[52].get(); }
		public void setCol53(String val){ col[53].set(val); }
		public String getCol53(){ return col[53].get(); }
		public void setCol54(String val){ col[54].set(val); }
		public String getCol54(){ return col[54].get(); }
		public void setCol55(String val){ col[55].set(val); }
		public String getCol55(){ return col[55].get(); }
		public void setCol56(String val){ col[56].set(val); }
		public String getCol56(){ return col[56].get(); }
		public void setCol57(String val){ col[57].set(val); }
		public String getCol57(){ return col[57].get(); }
		public void setCol58(String val){ col[58].set(val); }
		public String getCol58(){ return col[58].get(); }
		public void setCol59(String val){ col[59].set(val); }
		public String getCol59(){ return col[59].get(); }
		public void setCol60(String val){ col[60].set(val); }
		public String getCol60(){ return col[60].get(); }
		public void setCol61(String val){ col[61].set(val); }
		public String getCol61(){ return col[61].get(); }
		public void setCol62(String val){ col[62].set(val); }
		public String getCol62(){ return col[62].get(); }
		public void setCol63(String val){ col[63].set(val); }
		public String getCol63(){ return col[63].get(); }
		public void setCol64(String val){ col[64].set(val); }
		public String getCol64(){ return col[64].get(); }
		public void setCol65(String val){ col[65].set(val); }
		public String getCol65(){ return col[65].get(); }
		public void setCol66(String val){ col[66].set(val); }
		public String getCol66(){ return col[66].get(); }
		public void setCol67(String val){ col[67].set(val); }
		public String getCol67(){ return col[67].get(); }
		public void setCol68(String val){ col[68].set(val); }
		public String getCol68(){ return col[68].get(); }
		public void setCol69(String val){ col[69].set(val); }
		public String getCol69(){ return col[69].get(); }
		public void setCol70(String val){ col[70].set(val); }
		public String getCol70(){ return col[70].get(); }
		public void setCol71(String val){ col[71].set(val); }
		public String getCol71(){ return col[71].get(); }
		public void setCol72(String val){ col[72].set(val); }
		public String getCol72(){ return col[72].get(); }
		public void setCol73(String val){ col[73].set(val); }
		public String getCol73(){ return col[73].get(); }
		public void setCol74(String val){ col[74].set(val); }
		public String getCol74(){ return col[74].get(); }
		public void setCol75(String val){ col[75].set(val); }
		public String getCol75(){ return col[75].get(); }
		public void setCol76(String val){ col[76].set(val); }
		public String getCol76(){ return col[76].get(); }
		public void setCol77(String val){ col[77].set(val); }
		public String getCol77(){ return col[77].get(); }
		public void setCol78(String val){ col[78].set(val); }
		public String getCol78(){ return col[78].get(); }
		public void setCol79(String val){ col[79].set(val); }
		public String getCol79(){ return col[79].get(); }
		public void setCol80(String val){ col[80].set(val); }
		public String getCol80(){ return col[80].get(); }
		public void setCol81(String val){ col[81].set(val); }
		public String getCol81(){ return col[81].get(); }
		public void setCol82(String val){ col[82].set(val); }
		public String getCol82(){ return col[82].get(); }
		public void setCol83(String val){ col[83].set(val); }
		public String getCol83(){ return col[83].get(); }
		public void setCol84(String val){ col[84].set(val); }
		public String getCol84(){ return col[84].get(); }
		public void setCol85(String val){ col[85].set(val); }
		public String getCol85(){ return col[85].get(); }
		public void setCol86(String val){ col[86].set(val); }
		public String getCol86(){ return col[86].get(); }
		public void setCol87(String val){ col[87].set(val); }
		public String getCol87(){ return col[87].get(); }
		public void setCol88(String val){ col[88].set(val); }
		public String getCol88(){ return col[88].get(); }
		public void setCol89(String val){ col[89].set(val); }
		public String getCol89(){ return col[89].get(); }
		public void setCol90(String val){ col[90].set(val); }
		public String getCol90(){ return col[90].get(); }
		public void setCol91(String val){ col[91].set(val); }
		public String getCol91(){ return col[91].get(); }
		public void setCol92(String val){ col[92].set(val); }
		public String getCol92(){ return col[92].get(); }
		public void setCol93(String val){ col[93].set(val); }
		public String getCol93(){ return col[93].get(); }
		public void setCol94(String val){ col[94].set(val); }
		public String getCol94(){ return col[94].get(); }
		public void setCol95(String val){ col[95].set(val); }
		public String getCol95(){ return col[95].get(); }
		public void setCol96(String val){ col[96].set(val); }
		public String getCol96(){ return col[96].get(); }
		public void setCol97(String val){ col[97].set(val); }
		public String getCol97(){ return col[97].get(); }
		public void setCol98(String val){ col[98].set(val); }
		public String getCol98(){ return col[98].get(); }
		public void setCol99(String val){ col[99].set(val); }
		public String getCol99(){ return col[99].get(); }
		public void setCol100(String val){ col[100].set(val); }
		public String getCol100(){ return col[100].get(); }
	};
}
