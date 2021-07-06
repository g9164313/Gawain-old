package prj.shelter;

import java.io.Serializable;
import java.time.LocalDate;

public class DataRadiation 
  implements Serializable
{
	public class Mark implements Serializable {
		private static final long serialVersionUID = 6061524050057088702L;
		public String meas = "";//AT5350測量結果
		public String loca = "";//HusiIO載台位置
		public String actv = "";//Activity Unit: Ci, Bq
	};
	
	private static final long serialVersionUID = -8809212022245413541L;
	
	public final LocalDate day = LocalDate.now();//校正日期
	

	
}
