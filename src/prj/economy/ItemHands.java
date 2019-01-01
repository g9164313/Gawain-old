package prj.economy;


public class ItemHands {

	/**
	 * operator name.<p>
	 */
	public String name = "";

	/**
	 * operator contact information.<p>
	 * This variable is also identify-serial-number.<p>
	 */
	public String info = "";

	/**
	 * Where operator can take care.<p>
	 */
	public String zone ="";

	/**
	 * other information.<p>
	 */
	public String memo ="";

	/**
	 * It means which day is holiday.<p>
	 * Format: [year]-[month]-[day]{T[hour]:[minute]~[hour]:[minute]}.<p>
	 * Example: 2018-03-20;2018-07-19;
	 * Example: 2019-01-04T09:00~12:00;2019-01-07T14:00~16:00
	 */
	public String holiday="";

	private static final String SEPARATOR = ";";

	public ItemHands addHoliday(final String stamp){
		if(holiday.contains(stamp)==true){
			return this;
		}
		holiday = holiday + stamp + SEPARATOR;
		return this;
	}
	public ItemHands delHoliday(final String stamp){
		String[] arg = holiday.split(SEPARATOR);
		String tmp = "";
		for(String txt:arg){
			if(txt.startsWith(stamp)==true){
				continue;
			}
			tmp = tmp + txt + SEPARATOR;
		}
		holiday = tmp;//reassign again!!!!
		return this;
	}

	public ItemHands(){		
	}
}
