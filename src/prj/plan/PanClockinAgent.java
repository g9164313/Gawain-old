package prj.plan;

import java.io.IOException;
import java.io.InputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.jfoenix.controls.JFXTabPane;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import narl.itrc.nat.Loader;

public class PanClockinAgent extends PanBase {

	public PanClockinAgent(){
		//Loader.hooker = init;
	}
	
	private Runnable init = new Runnable(){
		@Override
		public void run() {			
			try {
				InputStream stm = Gawain.class.getResourceAsStream("/narl/itrc/res/clockin.json");
				FirebaseOptions opt;
				opt = new FirebaseOptions.Builder()
					.setCredentials(GoogleCredentials.fromStream(stm))
					.setDatabaseUrl("https://clockin-base.firebaseio.com")
					.build();
				FirebaseApp.initializeApp(opt);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}			
		}
	};
	
	@Override
	public Node eventLayout(PanBase self) {
		//final PanDispatch nod1 = new PanDispatch();
		
		final FragForm frag1 = new FragForm(); 
		final FragAuth frag2 = new FragAuth(frag1);
		final HBox lay1 = new HBox(
			frag1,
			new Separator(Orientation.VERTICAL),
			frag2
		);
		
		final Tab[] tabs = {
			new Tab("分派器",lay1),
			new Tab("查詢/報表"),
		};
		final JFXTabPane lay0 = new JFXTabPane();		
		lay0.getTabs().addAll(tabs);
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
		
		/*DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
		
		ref.addValueEventListener(new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				Object doc = arg0.getValue();
				System.out.println(doc);
			}
		});*/
	}
}
