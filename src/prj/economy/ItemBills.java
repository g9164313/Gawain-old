package prj.economy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	public String info= "";	
	/**
	 * custom information, contact address
	 */
	public String addr ="";	
	
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
	
	public ItemBills(){
		serial = new Date().getTime();
	}
	
	public void setSerial(long serial_number){
		serial = serial_number;
	}
	
	public long takeSerial(){
		return serial;
	}
	
	public String takeSerialTxt(){
		return fmtDate.format(new Date(serial));
	}
	
	public String stampOpen(){
		return fmtDate.format(new Date(serial));
	}
	
	public void stampClose(){
		stampClose = fmtDate.format(new Date());
	}
	
	//private static final DateFormat fmtTick = new SimpleDateFormat("HH:mm:ss");
	//private static final DecimalFormat fmtDeci = new DecimalFormat("###,###,###");
	
	/*public static String getCurrentOpenDate(){
		return fmtDate.format(new Date());
	}
	public static String toMeeting(final Date day){
		if(day==null){
			return "";
		}
		return fmtDate.format(day);
	}
	public static String toMeeting(final LocalDate day){
		if(day==null){
			return "";
		}
		final ZoneId id = ZoneId.systemDefault();
		return toMeeting(Date.from(day.atStartOfDay(id).toInstant()));
	}
	public static Date meeting2date(final ItemBills itm){
		try {
			if(itm.stampMeet.length()==0){
				return null;
			}
			return fmtDate.parse(itm.stampMeet);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static LocalDate meeting2local(final ItemBills itm){
		Date day = meeting2date(itm);
		if(day==null){
			return null;
		}
		final ZoneId id = ZoneId.systemDefault();
		return day.toInstant().atZone(id).toLocalDate();
	}*/
	
	//---below lines are processing cart things---//
	
	//private static final String ITEM_DELIMITER = ";";
	//private static final String ATTR_DELIMITER = ",";
	//private static final String TXT_PERIOD_SEC = "時段(秒計)";
	//private static final String TXT_PERIOD_MIN = "時段(分鐘)";
	//private static final String TXT_PERIOD_HUR = "時段(小時)";
		
	/*private int index_period = 1;
	
	public ItemBills addThingSec(
		final Date tick1, 
		final Date tick2,
		final String feeRate
	){
		return  _add_thing_time(TXT_PERIOD_SEC,tick1,tick2,feeRate);
	}	
	public ItemBills addThingMin(
		final Date tick1, 
		final Date tick2,
		final String feeRate
	){
		return  _add_thing_time(TXT_PERIOD_MIN,tick1,tick2,feeRate);
	}	
	public ItemBills addThingHour(
		final Date tick1, 
		final Date tick2,
		final String feeRate
	){
		return  _add_thing_time(TXT_PERIOD_HUR,tick1,tick2,feeRate);
	}
	
	private ItemBills _add_thing_time(
		final String tick_type,
		final Date tick1, 
		final Date tick2,
		final String feeRate
	){
		String txt = String.format(
			"%s%02d, %s~%s",
			tick_type,
			index_period++,
			fmtTick.format(tick1),
			fmtTick.format(tick2)
		);
		listCart = listCart + txt + ITEM_DELIMITER;
		return this;
	}
	
	public ItemBills delThing(int idx){
		final String[] arg = listCart.split(ITEM_DELIMITER);
		listCart = "";//reset context~~~
		for(int i=0; i<arg.length; i++){
			if(i==idx){
				continue;
			}
			listCart = listCart + arg[i] + ITEM_DELIMITER;
		}
		return this;
	}
	
	public ItemBills delThing(String name){
		final String[] arg = listCart.split(ITEM_DELIMITER);
		listCart = "";//reset context~~~
		for(int i=0; i<arg.length; i++){
			if(arg[i].startsWith(name)==true){
				continue;
			}
			listCart = listCart + arg[i] + ITEM_DELIMITER;
		}
		return this;
	}
	
	public String amountText(){		
		return fmtDeci.format(amount());
	}
	
	public int amount(){
		final String[] item = listCart.split(ITEM_DELIMITER);
		int total = 0;
		for(int i=0; i<item.length; i++){
			final String[] attr = item[i].split(ATTR_DELIMITER);
			switch(attr.length){
			case 2://[item-name,fee]
				total += get_price(attr[1]);
				break;
			case 3://[item-name,fee,count]
				total += get_price(attr[1],attr[2]);
				break;
			case 4://[time-name,fee,t1,t2]
				total += get_price(attr);
				break;
			default:
				Misc.logw("Ignore: %s", item[i]);
				break;
			}
		}
		return total;
	}

	private int get_price(String fee){
		if(fee.charAt(0)=='+'){
			fee = fee.substring(1);
		}
		try{
			return Integer.valueOf(fee);
		}catch(NumberFormatException e){
			Misc.logw("Ignore --> %s", fee);
		}
		return 0;
	}
	
	private int get_price(String fee, String cnt){
		if(fee.charAt(0)=='x'){
			fee = fee.substring(1);
		}
		try{
			float _fee = Float.valueOf(fee);
			int _cnt = Integer.valueOf(cnt);
			return (int)(_fee * _cnt);
		}catch(NumberFormatException e){
			Misc.logw("Ignore --> %s,%s", fee, cnt);
		}
		return 0;
	}
	
	private int get_price(String[] attr){
		int diff = 0;
		try {
			long t1 = fmtTick.parse(attr[2]).getTime();
			long t2 = fmtTick.parse(attr[3]).getTime();
			diff = (int)(t2 - t1)/1000;
		} catch (ParseException e) {
			Misc.logw("Ignore tick --> %s,%s",attr[2],attr[3]);
			return 0;
		}		
		if(attr[0].startsWith(TXT_PERIOD_SEC)==true){
			diff = diff / 1;
		}else if(attr[0].startsWith(TXT_PERIOD_MIN)==true){
			diff = diff / 60;
		}else if(attr[0].startsWith(TXT_PERIOD_HUR)==true){
			diff = diff / 3600;
		}
		return get_price(attr[1],""+diff);
	}*/
}
