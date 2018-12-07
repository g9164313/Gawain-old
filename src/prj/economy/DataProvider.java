package prj.economy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.HBox;
import narl.itrc.Gawain;

public class DataProvider {

	private static FirebaseDatabase base = null;
	
	public static boolean isReady(){
		return (base==null)?(false):(true);
	}
	
	public static void test0(){
		DatabaseReference ref = base.getReference("/qq1/work1");
		ref.addValueEventListener(new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				Object doc = arg0.getValue();
				System.out.println(doc);
			}
		});
		//ref.setValueAsync("ggyy");
		//ref.removeValueAsync();
		//ref.push();
	}
	
	public final static IntegerProperty propIndex = new SimpleIntegerProperty();
	
	private static void ref2prop_int(
		final String dataPath, 
		final IntegerProperty prop,
		final int defaultValue
	){
		
		final DatabaseReference ref = base.getReference(dataPath);
		
		ref.addListenerForSingleValueEvent(new ValueEventListener(){
			int value;
			Runnable event_set_value = new Runnable(){
				@Override
				public void run() {
					prop.set(value);
				}
			};
			@Override
			public void onCancelled(DatabaseError arg0) {
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				Number num = (Number)(arg0.getValue());
				if(num==null){
					ref.setValueAsync(defaultValue);
					value = defaultValue;
				}else{
					value = num.intValue();
				}
				Application.invokeAndWait(event_set_value);
			}	
		});
		
		prop.addListener((obv,old,val)->{
			ref.setValueAsync((int)val);
		});
	}
	
	public static void put_item(
		final String path,
		final ItemBills itm
	){
		if(base==null){
			return;
		}
		DataProvider.base
		.getReference(path)
		.setValueAsync(itm);
	}
	
	public static void init(String key_file){
		
		final String KEY_NAME = "KEY_FILE";
		if(base!=null){
			return;
		}
		base = null;
		if(key_file==null){
			//load from default setting
			key_file = Gawain.getSetting().getProperty(KEY_NAME, null);
			if(key_file==null){				
				return;
			}
		}
		try {
			InputStream stm = new FileInputStream(key_file);
			FirebaseOptions opt = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(stm))
				.setDatabaseUrl("https://clockin-base.firebaseio.com")
				.build();
			FirebaseApp.initializeApp(opt);
			Gawain.getSetting().setProperty(KEY_NAME, key_file);			
			base = FirebaseDatabase.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//prepare variable
		ref2prop_int("/var/index",propIndex,1);
	}	
}
