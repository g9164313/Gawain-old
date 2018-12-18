package prj.economy;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ItemBills {
	
	private static final DateFormat fmtDate= new SimpleDateFormat("yyyy-MM-dd HH:mm");  
	
	/**
	 * key in data-base.<p>
	 * It is also time-stamp when creating this object.<p>
	 */
	private long serial = -1L;
	
	/**
	 * the text format of serial number.<p>
	 */
	public String order = "";
	
	/**
	 * time-stamp for open account, meet custom, and close account.<p>
	 */
	public String stampMeet="", stampClose="";
	
	/**
	 * custom information, contact name
	 */
	public String name = "";	
	/**
	 * custom information, contact information
	 */
	public String info = "";	
	/**
	 * custom information, contact address
	 */
	public String addr ="";	
	
	public ItemBills(){
		serial = new Date().getTime();
	}
	
	public void hangSerial(final String val){
		serial = Long.valueOf(val, 16);
	}
	public String takeSerial(){
		return String.format("%08X", serial);
	}	
	
	//public String takeSerialTxt(){
	//	return fmtDate.format(new Date(serial));
	//}
	
	public void stampOpen(){
		stampClose = "";
	}
	public void stampClose(){
		stampClose = fmtDate.format(new Date());
	}
	
	public boolean isClosed(){
		if(stampClose.length()==0){
			return false;
		}
		return true;
	}
	
	/**
	 * Price and name of item.<p>
	 * It include 3 format.Fee can't contain delimiter.<p>
	 * Format:<p>
	 * [name,fee];...<p>
	 * [name,fee,count];...<p>
	 * [name,fee,count,tick];...<p>
	 * Example:<p>
	 * 折扣,-100;...<p>
	 * 品項,23.3,78;...<p>
	 * 時段(秒計),1.23,100,13:21:15;...<p>
	 */
	public String listCart = "";
	
	//---below lines are processing cart things---//

	public static class Thing {
		public String name="";
		public float  fee=0.f;
		public float  cnt=1.f;
		public String memo="";
		public Thing(){			
		}
		public Thing(final String txt){
			unflatten(txt);
		}
		public Thing unflatten(final String txt){
			String[] arg = txt.split(",");
			if(arg.length>=1){ name = arg[0]; }
			if(arg.length>=2){ fee = Float.valueOf(arg[1]); }
			if(arg.length>=3){ cnt = Float.valueOf(arg[2]); }
			if(arg.length>=4){
				memo = arg[3];
				for(int i=4; i<arg.length; i++){
					memo = memo + "," + arg[i];
				}
			}
			return this;
		}
		public String flatten(){
			return String.format(
				"%s,%03f,%03f,%s;", 
				name, fee, cnt, memo
			);
		}
		public String toString(){
			return flatten();
		}
		public int amount(){
			return (int)Math.ceil(fee * cnt);
		}
		public String amountText(){
			return fmtDeci.format(amount());
		}
	};
	
	private static final String TXT_PERIOD_SEC = "時段(秒計)";
	private static final String TXT_PERIOD_MIN = "時段(分鐘)";
	private static final String TXT_PERIOD_HUR = "時段(小時)";
	
	public ItemBills addPeriodHour(
		float price,
		float count
	){
		return addThing(TXT_PERIOD_HUR,price,count,"");
	}
	public ItemBills addPeriodMinute(
		float price,
		float count
	){
		return addThing(TXT_PERIOD_MIN,price,count,"");
	}	
	public ItemBills addPeriodSecond(
		float price,
		float count
	){
		return addThing(TXT_PERIOD_SEC,price,count,"");
	}	
	public ItemBills addThing(
		final String name,
		float price
	){
		return addThing(name,price,1,"");
	}
	public ItemBills addThing(
		final String name,
		float price,
		float count
	){
		return addThing(name,price,count,"");
	}
	public ItemBills addThing(
		final String name,
		float price,
		float count,
		final String memo
	){
		Thing itm = new Thing();
		itm.name= name;
		itm.fee = price;
		itm.cnt = count;
		itm.memo= memo;
		listCart = listCart + itm.flatten();
		return this;
	}
	
	public ArrayList<ItemBills.Thing> takeThing(){
		ArrayList<ItemBills.Thing> lst = new ArrayList<ItemBills.Thing>();
		String[] arg = listCart.split(";");
		for(String txt:arg){
			lst.add(new Thing(txt));
		}
		return lst;
	}
	
	public static String thing2cart(final List<ItemBills.Thing> lst){
		String txt = "";
		for(ItemBills.Thing itm:lst){
			txt = txt + itm.flatten() + ";";
		}
		return txt;
	}
	
	private static final DecimalFormat fmtDeci = new DecimalFormat("###,###,###");
	
	public String amountText(){		
		return fmtDeci.format(amount());
	}
	public int amount(){
		int total = 0;
		ArrayList<Thing> lst = takeThing();
		for(Thing itm:lst){
			total = total + (int)Math.ceil(itm.fee * itm.cnt);
		}
		return total;
	}
}
