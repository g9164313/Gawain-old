package prj.shelter;

import java.util.Calendar;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXListView;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import prj.shelter.DataBridge.Stuff;

public class LayQuery extends BorderPane {

	class Card extends VBox {
		final String[] t_info;
		Card(Stuff[] itm){
			//map data-value to graphic node			
			getStyleClass().addAll("box-pad","box-border");
			t_info = itm[0].info;
			show_tenure(itm[0]);
			for(int i=1; i<itm.length; i++){
				show_prodx(itm[i]);
			}
		}
		void show_tenure(Stuff itm){
			//GridPane lay = new GridPane();
			Label[] txt = {
				new Label("廠牌：")     ,
				new Label(itm.info[1]),
				new Label("型號：")     ,
				new Label(itm.info[2]),
				new Label("序號：")     ,
				new Label(itm.info[3])
			};
			HBox lay = new HBox(txt);
			getChildren().add(lay);
			
		}
		void show_prodx(Stuff itm){
			//skip the first~~~~
			GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad","font-console");
			
			lay.add(new Label(String.format(
				"%s (%s)",
				itm.info[0], itm.getFormatText()
			)), 0, 0, 15, 1);
			
			lay.add(new Separator(Orientation.VERTICAL), 2, 1, 1, itm.appx.length+1);
			lay.add(new Separator(Orientation.VERTICAL), 8, 1, 1, itm.appx.length+1);
			lay.add(new Separator(Orientation.VERTICAL),14, 1, 1, itm.appx.length+1);
			
			Label[] name = {
				new Label("＊＊＊"), 
				new Label("＊＊＊"), 
				new Label("＊＊＊")
			};
			switch(itm.fmt){
			case 2:
			case 3://γ反應報告
				name[0].setText("刻度");
				name[1].setText("實際值");
				name[2].setText("器示值");
				break;
			case 4://效率報告
			case 5://活度報告
				name[0].setText("刻度");
				name[1].setText("背景值");
				name[2].setText("器示值");				
				break;
			}
			lay.add(name[0], 1, 1, 1, 1);
			lay.add(name[1], 3, 1, 5, 1);
			lay.add(name[2], 9, 1, 5, 1);
			
			for(int i=0; i<itm.appx.length; i++){				
				String[] col = itm.appx[i].split("@");
				{
					Label txt = new Label(col[0]);
					//txt.setPickOnBounds(true);
					lay.add(txt, 1, 2+i);
				}
				String[] val;
				if(col.length>=2){
					val = col[1].split(",");
					for(int j=0; j<val.length; j++){
						Label txt = new Label(padding(val[j]));
						lay.add(txt, 3+j, 2+i);
					}
				}
				if(col.length>=3){
					val = col[2].split(",");
					for(int j=0; j<val.length; j++){
						Label txt = new Label(padding(val[j]));
						lay.add(txt, 9+j, 2+i);
					}
				}				
			}
			getChildren().add(lay);
		}
		String padding(String txt){
			int pos = txt.lastIndexOf('.');
			if(pos<0){
				return String.format("%"+4+"s", txt);
			}else{
				String hh = txt.substring(0,pos);
				String tt = txt.substring(pos+1);
				if(hh.length()<4){
					hh = String.format("%"+4+"s", hh);//right-padding
				}
				if(tt.length()<3){
					tt = String.format("%-"+3+"s", tt);//left-padding
				}
				txt = hh + "." + tt;
			}			
			return txt;
		}
	};	
	
	private final TextField box_code = new TextField();
	private final TextField box_date = new TextField();	
	private final TextField box_memo = new TextField();
	private final JFXComboBox<String> cmb_employ = new JFXComboBox<String>();
	
	private final JFXListView<Card> lst_card = new JFXListView<Card>();
	
	public LayQuery(){
		
		init_label_setting();
		
		lst_card.getStyleClass().addAll("font-size3");
		lst_card.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);		
		lst_card.setUserData("");//default value for tenure ID.
		lst_card.setOnKeyPressed(e->{
			KeyCode cc = e.getCode();
			if(cc==KeyCode.F1 || cc==KeyCode.ESCAPE){
				box_code.requestFocus();
				e.consume();
			}else if(cc==KeyCode.F3){
				print_label();
				e.consume();
			}			
		});
		
		box_code.setOnAction(e->searchy_text());
		box_code.setOnKeyPressed(e->{
			KeyCode cc = e.getCode();
			if(cc==KeyCode.F2 || cc==KeyCode.DOWN){
				//when user DOWN key, focus list view.
				lst_card.requestFocus();
				lst_card.getSelectionModel().select(0);
				e.consume();
			}else if(cc==KeyCode.F3){
				print_label();
				e.consume();
			}
			//just propagate event~~~
		});		
		HBox.setHgrow(box_code, Priority.ALWAYS);
		
		JFXButton btn_print = new JFXButton("列印-F3");
		btn_print.getStyleClass().add("btn-raised-1");
		btn_print.setOnAction(e->print_label());
		
		HBox lay1 = new HBox(
			new Label("搜尋:"),
			box_code			
		);
		lay1.getStyleClass().addAll("box-pad");
		lay1.setAlignment(Pos.CENTER_LEFT);
		
		
		HBox lay2 = new HBox(
			new Label("日期:"), box_date,
			new Label("人員:"), cmb_employ,
			new Label("備註:"), box_memo,
			btn_print
		);
		lay2.getStyleClass().addAll("box-pad");
		lay2.setAlignment(Pos.CENTER_LEFT);
		
		//getStyleClass().addAll("font-size1");
		setMinSize(500,500);
		setTop(lay1);
		setCenter(lst_card);
		setBottom(lay2);
	}
	
	void init_label_setting(){
		Calendar cc = Calendar.getInstance();
		box_date.setPrefWidth(150);
		box_date.setText(String.format(
			"%d/%d/%d",
			cc.get(Calendar.YEAR)-1911,
			cc.get(Calendar.MONTH)+1,
			cc.get(Calendar.DATE)
		));	
		box_memo.setPrefWidth(150);
				
		final String vals = Gawain.prop().getProperty("employ", "");
		if(vals.length()!=0){
			String[] v = vals.split("[,]|[@]");
			for(int i=0; i<v.length; i++){
				cmb_employ.getItems().add(v[i]);
			}
		}		
		cmb_employ.getItems().add("none");
		cmb_employ.getSelectionModel().select(0);
	}
	
	String get_tenure_id(){
		String txt = "";
		if(lst_card.getSelectionModel().getSelectedIndex()>=0){
			txt = lst_card.getSelectionModel().getSelectedItem().t_info[0];
		}else{
			txt = ((String)lst_card.getUserData()).trim();
		}
		return txt;
	}
	
	void print_label(){
		/**
		 * Label format is below:
		 * [date] @ [tenure ID] @ [employ] {@ [memo]}
		 */		
		final String arg0 =	box_date.getText().trim();
		final String arg1 =	get_tenure_id();
		final String arg2 =	cmb_employ.getSelectionModel().getSelectedItem().trim();
		final String arg3 =	box_memo.getText().trim();
		//check whether arguments are valid. 
		if(arg0.matches("\\d+/\\d+/\\d+")==false){
			PanBase.notifyError("", "錯誤的日期格式");
			return;
		}
		if(arg1.length()==0){
			PanBase.notifyError("", "非法的儀器ID");
			return;
		}		
		final Task<Integer> tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				updateMessage("列印中");
				String args = arg0+"@"+arg1+"@"+arg2;
				if(arg3.length()!=0){
					args = args+"@"+arg3;
				}
				//TODO:execute bash script~~
				return 0;
			}
		};
		((PanBase)(getParent().getUserData())).notifyTask(tsk);
	}
	
	void searchy_text(){
		String txt = box_code.getText()
			.replace("\\r", "")
			.replace("\\n", "")
			.trim();
		if(txt.matches("\\d+/\\d+/\\d+@(\\p{Graph}+)+")==true){
			/**
			 * Label format is below:
			 * [date] @ [tenure ID] @ [employ] @ [memo]
			 */
			String[] val = txt.split("@");			
			switch(val.length){
			default:
			case 4:
				box_memo.setText(val[3]);
				break;
			case 3:
				break;
			case 2:
				PanBase.notifyError("","錯誤的標籤格式!!");
				return;
			}			
			box_date.setText(val[0]);
			lst_card.setUserData(val[1]);
			cmb_employ.getSelectionModel().select(val[2]);
			
			String[] idf = val[1].split("_");
			DataBridge.getInstance()
				.list_last_prodx(
					idf[0], idf[1], 
					lst->gen_cards(lst)
				);
		}else{
			txt = "%"+txt+"%";
			lst_card.setUserData("");//trick!!!
			DataBridge.getInstance()
				.list_last_prodx(txt, lst->gen_cards(lst));
		}
	}
	
	void gen_cards(final Stuff[][] lst){
		ObservableList<Card> obv = lst_card.getItems();
		obv.clear();
		for(int i=0; i<lst.length; i++){
			obv.add(new Card(lst[i]));
		}
	}
}
