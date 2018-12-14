package prj.economy;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.jfoenix.controls.JFXBadge;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class FaceHands extends GridPane {

	public final StringProperty name = new SimpleStringProperty();
	public final StringProperty phone= new SimpleStringProperty();
	public final StringProperty zone = new SimpleStringProperty();
	public final StringProperty memo = new SimpleStringProperty();
	
	private final IntegerProperty loading = new SimpleIntegerProperty();

	private ItemHands ref;
	
	public String descr_idx(){
		return ref.phone;
	}
	
	public FaceHands(final ItemHands itm){
		
		init_layout();		
		ref = itm;//make reference~~~		
		name.setValue(ref.name);
		phone.setValue(ref.phone);
		zone.setValue(ref.zone);
		
		setOnMouseClicked(event->{
			if(event.getClickCount()<2){
				return;
			}
			new PanEditHands(itm).appear();
		});
		
		DataProvider.refer("/pool-2/"+descr_idx(), new ValueEventListener(){
			int val = 0;
			Runnable event = new Runnable(){
				@Override
				public void run() {
					loading.set(val);
				}
			};
			@Override
			public void onCancelled(DatabaseError arg0) {	
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				if(arg0.exists()==false){
					val = 0;
				}else{
					val = (int)arg0.getChildrenCount();
				}
				//trick!! Property doesn't be changed when node is showing...
				Application.invokeLater(event);
			}
		});
	}

	private static Image img = Misc.getPicImage("account.png");
	
	private void init_layout(){
		
		final Label[] info = {
			new Label(),
			new Label(),
			new Label(),
			new Label()
		};
		info[0].textProperty().bind(name);
		info[1].textProperty().bind(phone);
		info[2].textProperty().bind(zone);
		info[3].textProperty().bind(memo);

		final ImageView icon = new ImageView(img);
		final JFXBadge badge = new JFXBadge(icon,Pos.TOP_LEFT);
		badge.getStyleClass().add("icons-badge");
		badge.textProperty().bind(loading.asString());

		add(badge,0,0,3,3);
		add(new Label("名稱："), 3, 0);
		add(new Label("電話："), 3, 1);
		add(new Label("位置："), 3, 2);
		add(info[0], 4, 0);
		add(info[1], 4, 1);
		add(info[2], 4, 2);
	}
}
