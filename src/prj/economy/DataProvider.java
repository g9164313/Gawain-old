package prj.economy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

import narl.itrc.Gawain;

public class DataProvider {

	private static FirebaseDatabase base = null;
	
	public static boolean isReady(){
		return (base==null)?(false):(true);
	}

	public static void refer(
		final String path, 
		final ValueEventListener event
	){
		if(base==null){
			return;
		}
		base.getReference(path)
			.orderByKey()
			.addValueEventListener(event);
	}
	
	public static void refer_once(
			final String path, 
			final ValueEventListener event
	){
		if(base==null){
			return;
		}
		base.getReference(path)
			.orderByKey()
			.addListenerForSingleValueEvent(event);
	}
	
	public static void refer_once_last(
			final String path,
			final int size,
			final ValueEventListener event			
	){
		if(base==null){
			return;
		}
		base.getReference(path)
			.orderByKey()
			.limitToLast(size)
			.addListenerForSingleValueEvent(event);
	}
	
	public final static IntegerProperty propIndex = new SimpleIntegerProperty();
	
	private static void prepare_order_index(){
		
		final DatabaseReference ref = base.getReference("/var/index");
		
		ref.addListenerForSingleValueEvent(new ValueEventListener(){
			Number num = null;
			final Runnable event = new Runnable(){
				@Override
				public void run() {
					if(num==null){
						propIndex.set(1);
					}else{
						propIndex.setValue(num);
					}
				}
			};
			@Override
			public void onCancelled(DatabaseError arg0) {
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				if(arg0.exists()==false){
					num = null;
				}else{
					num = (Number)(arg0.getValue());
				}
				Application.invokeAndWait(event);
			}	
		});
		propIndex.addListener((obv,_old,_new)->{
			ref.setValueAsync((int)_new);
		});
	}
	
	public static void push_bills(
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
	
	public static void push_hands(final ItemHands itm){
		if(base==null){
			return;
		}
		DataProvider.base
		.getReference("/hands/"+itm.info)
		.setValueAsync(itm);
	}
	
	public static void delete(final String path){
		if(base==null){
			return;
		}
		DataProvider.base
		.getReference(path)
		.setValueAsync(null);
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

		prepare_order_index();
	}	
}
