package prj.economy;


public class ItemHands {

	/**
	 * operator name
	 */
	public String name = "";	
	/**
	 * operator phone, this is also key or UUID.<p>
	 */
	public String phone= "";	
	/**
	 * Where operator can take care.<p>
	 */
	public String zone ="";
	/**
	 * Where operator is.<p>
	 * This will be updated by operator phone.<p>
	 */
	public String geom ="";
	/**
	 * other information.<p>
	 */
	public String memo ="";
	
	public ItemHands(){		
	}
}
