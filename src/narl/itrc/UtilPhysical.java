package narl.itrc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.UnknownFormatConversionException;

/**
 * convenient functions for changing all units.<p>
 * Physical Number is combined with digital and unit.<p>
 * This static class will parse unit and convert SI-scale.<p>
 * Unit is jointed with name and SI-scale.<p>
 * @author qq
 *
 */
public class UtilPhysical {
	
	/**
	 * Split physical number into value and unit part.<p>
	 * Physical number contain digital and unit.<p>
	 * There is at least one space between digital and unit.<p>
	 * @param txt - physical number (value and unit)
	 * @return
	 */
	public static String[] split(String txt){
		//number and unit(SI scale + name)		
		txt =txt.trim();
		String[] col = txt.split("[\\s]+");
		if(col.length!=2) {
			throw new NumberFormatException("no way to distinguish between digital and unit");
		}
		col[0] = col[0].trim();//digital number
		col[1] = col[1].trim();//unit name
		if(col[0].matches("[+-]?[\\d.]+(?:[E|e]-?\\d+)?")==false) {
			throw new NumberFormatException("invalid digital");
		}
		return col;
	}	
	
	
	//some special word~~~~
	//'/', '·','∘∘×','∙',×÷,⁻¹²³,U+2219,U+00B1±,kg−1⋅m−2⋅s4⋅A2,
	//Ȧ U+0226, °C, Å U+00C5
	
	/**
	 * 分出單位裏可辨識的名子，切出分子和分母
	 * @param unit - SI unit text
	 * @return two string array, first is numerator, second is denominator. 
	 */
	private static String[][] tokenize_unit(final String si_unit){
		final String txt = si_unit.replaceAll("\\s+", "");//remove all space~~~
		
		final ArrayList<String>	numer = new ArrayList<String>();//numerator
		final ArrayList<String>	denom = new ArrayList<String>();//denominator
		ArrayList<String> lst = numer;
		short flg = 0;
		String tkn = "";
		for(int i=0; i<txt.length(); i++) {
			char cc = txt.charAt(i);
			if(cc=='*' || cc=='×' || cc=='∙' || cc=='\u22C5') {
				lst.add(tkn);
				tkn = "";//for next turn~~~~
			}else if(cc=='/' || cc=='÷') {
				lst.add(tkn);
				tkn = "";//for next turn~~~~
				//swap list~~~~
				flg+=1;
				if(flg%2==0) {
					lst = numer;
				}else {
					lst = denom;
				}
			}else if(cc=='(' || cc==')') {
				continue;//skip brackets ???
			}else {
				tkn = tkn + cc;
			}
		}
		//add the last unit~~~
		lst.add(tkn);
		
		final String[][] res = {null, null};
		res[0] = numer.toArray(new String[0]);
		res[1] = denom.toArray(new String[0]);
		return res;
	}
	
	private static class PREFIX_SCALE {
		//the elements in two arrays are mapping one by one
		final char[] prefix = {'p','n','u','μ','m','c','k','M','G','T' };
		final byte[] base10 = {-12, -9, -6, -6, -3, -2,  3,  6,  9, 12 };
		
		byte prefix2base(final String txt) {
			if(txt.length()==1) {
				return 0;
			}
			//Here!!, we can skip some special unit name
			if(txt.toLowerCase().equals("mil")==true) {
				return 0;
			}
			if(txt.toLowerCase().equals("min")==true) {
				return 0;
			}
			char cc = txt.charAt(0);
			for(int i=0; i<prefix.length; i++) {
				if(cc==prefix[i]) {
					return base10[i];
				}
			}
			return 0;
		}
		int get(
			final String[] aa,
			final String[] bb
		) {
			if(aa.length!=bb.length) {
				throw new NumberFormatException("the number of unit no match");
			}
			byte base = 0;
			for(int i=0; i<aa.length; i++) {
				if(aa[i].equals(bb[i])==true) {
					continue;
				}
				//the problem is 'meter', it's abbreviation is 'm'....
				byte va = prefix2base(aa[i]);
				byte vb = prefix2base(bb[i]);
				base = (byte) (base + (va-vb));
			}
			return (int)base;
		}
	};
	private static final PREFIX_SCALE prefix_scale = new PREFIX_SCALE();
		
	private static class TIME_SCALE {
		//final BigDecimal I360 = V001.divide(V360,6,RoundingMode.HALF_UP);
		
		//one value include numerator and denominator
		final int[] I360 = {  1, 360};
		final int[] I060 = {  1,  60};
		final int[] VONE = {  1,   1};
		final int[] V060 = { 60,   1};
		final int[] V360 = {360,   1};
		final int[][][] scale = {
			//------// second, minute, hour
			/*second*/{VONE  ,I060   , I360},
			/*minute*/{V060  ,VONE   , I060},
			/*hour  */{V360  ,V060   , VONE},	
		};
		int name2index(final String name) {
			if(name.equals("sec") || name.equals("s")) {
				return 0;
			}else if(name.equals("min") || name.equals("m")) {
				return 1;
			}else if(name.equals("hr") || name.equals("hour") || name.equals("h")) {
				return 2;
			}
			return -1;
		}
		int[] get(
			final String[] aa, 
			final String[] bb
		) {
			int[] val = {1,1};
			for(int i=0; i<aa.length; i++) {
				final int ii = name2index(aa[i]);
				final int jj = name2index(bb[i]);
				if(ii<0 || jj<0) {
					continue;
				}
				int[] vv = scale[ii][jj];
				val[0] = val[0] * vv[0];
				val[1] = val[1] * vv[1];
			}
			return val;
		}
	};
	private static final TIME_SCALE time_scale = new TIME_SCALE();
	
	//test sample code
	//String ans;
	//ans = UtilPhysical.convert_scale("13 mm", "cm");
	//ans = UtilPhysical.convert_scale("256  um", "mm");
	//ans = UtilPhysical.convert_scale("67  Pix/mm", "Pix/um");
	//ans = UtilPhysical.convert_scale("0.256 1/um", "1/mm");
	//ans = UtilPhysical.convert_scale("60 sec", "min", 0);
	//ans = UtilPhysical.convert_scale("0.01666666 Sv/sec", "Sv/min");
	
	public static String convertScale(
		final String phy_numb, 
		final String dst_unit,
		final int running_size,
		final RoundingMode running_mode
	) {
		final String[] col = split(phy_numb);
		final String val_numb = col[0];
		final String src_unit = col[1];
		if(val_numb.length()==0||src_unit.length()==0) {
			return "";
		}
		//check unit~~~
		final String[][] src_u = tokenize_unit(src_unit);
		final String[][] dst_u = tokenize_unit(dst_unit);
		try {
			BigDecimal val = new BigDecimal(val_numb);
			//difference the prefix of unit name
			int base = 0;
			base = prefix_scale.get(src_u[0], dst_u[0]);
			val = val.movePointRight(base);
			base = prefix_scale.get(src_u[1], dst_u[1]);
			val = val.movePointLeft(base);//trick, it is inverse
			//process some special time unit.			
			int[] ts_a = time_scale.get(src_u[0], dst_u[0]);
			int[] ts_b = time_scale.get(dst_u[1], src_u[1]);
			int[] ts_c = {ts_a[0]*ts_b[0], ts_a[1]*ts_b[1]};			
			if((ts_c[0]*ts_c[1])!=1) {
				val = val.multiply(new BigDecimal(ts_c[0]));
				if(ts_c[1]>1) {
					val = val.divide(new BigDecimal(ts_c[1]), running_size, running_mode);
				}
			}			
			return val.toString();
		}catch(UnknownFormatConversionException e) {
			Misc.loge("[mismatch] %s!=%s",src_unit,dst_unit);
			return "";
		}catch(NumberFormatException e) {
			Misc.loge("[format error] %s",val_numb);
			return "";
		}
	}
	public static String convertScale(
		final String phy_numb, 
		final String dst_unit
	) {
		return convertScale(phy_numb,dst_unit,3,RoundingMode.HALF_UP);
	}
	public static String convertScale(
		final String phy_numb, 
		final String dst_unit,
		final int running_size
	) {
		return convertScale(phy_numb,dst_unit,running_size,RoundingMode.HALF_UP);
	}
	//-------------------------------------------------//
	
	
	public static double getDouble(final String txt){
		try {
			return Double.valueOf(split(txt)[0]);
		}catch(NumberFormatException e) {
			Misc.loge("Wrong physcial value --> %s", txt);			
		}
		return 0.;
	}
	public static int getInteger(final String txt){
		try {
			return Integer.valueOf(split(txt)[0]);
		}catch(NumberFormatException e) {
			Misc.loge("Wrong physcial value --> %s", txt);			
		}
		return 0;
	}
	public static String getUnit(final String txt){
		return split(txt)[1];//SI scale + name
	}
	
	public static double convert(
		double srcValue,
		String srcUnit,
		String dstUnit
	) throws NumberFormatException {
		srcUnit = srcUnit.trim();//for safety~~~
		dstUnit = dstUnit.trim();
		if(srcUnit.equals(dstUnit)==true){
			return srcValue;//we don't need convert value~~~~
		}
		double srcScale = findScale(srcUnit);
		if(srcScale!=1.){
			srcUnit = srcUnit.substring(1);
		}
		double dstScale = findScale(dstUnit);
		if(dstScale!=1.){
			dstUnit = dstUnit.substring(1);
		}
		double scale = (srcScale/dstScale);		
		double ratio = findRatio(srcUnit,dstUnit); 
		return srcValue*scale*ratio;
	}
	public static double convert(
		String srcValue,
		String srcUnit,
		String dstUnit
	) throws NumberFormatException {
		return convert(
			Double.valueOf(srcValue),
			srcUnit,
			dstUnit
		);
	}	
	public static double convert(
		String srcValueUnit,
		String dstUnit
	) throws NumberFormatException {
		String[] srcTxt = split(srcValueUnit);
		return convert(
			srcTxt[0],
			srcTxt[1],
			dstUnit
		);
	}
	
	public static String num2scale(double number){
		String txt = String.format("%G",number);
		int pos = txt.indexOf('E');
		if(pos<0){
			return txt;
		}
		double sss = Math.pow(10.,Integer.valueOf(txt.substring(pos+1)));
		int idx = 0;
		for(idx=0; idx<si_value.length; idx++){
			if(si_value[idx]==sss){
				break;
			}			
		}
		if(idx>=si_value.length){
			return txt;
		}
		return txt.substring(0,pos)+si_scale[idx];
	}	
	
	
	private static final char[] p_prefix = {' ','k','M','G','T','P','E'};		
	private static final char[] n_prefix = {' ','m','μ','n','p','f','a'};
	private static final double[] p_scale = {1., 1e-3, 1e-6, 1e-9, 1e-12, 1e-15, 1e-18};
	private static final double[] n_scale = {1., 1e3, 1e6, 1e9, 1e12, 1e15, 1e18};
	
	/**
	 * Add prefix notation to value.<p>
	 * @param number - value
	 * @return
	 */
	public static String addPrefix(final double number){
		return num2prefix(number,3);
	}
	public static String num2prefix(
		final double number, 
		final int dots
	){		
		String[] txt = String.format("%E",number).split("E");
		int prx = Integer.valueOf(txt[1]);
		int mtx = Math.abs(prx)/3;
		if(mtx>=p_prefix.length){
			return String.format("%E",number);//no change~~~
		}
		char[] prefix;
		double[] scale;
		if(prx>=0){
			prefix= p_prefix;
			scale = p_scale;
		}else{
			prefix= n_prefix;
			scale = n_scale;
		}
		return String.format(
			"%."+dots+"f%c", 
			number * scale[mtx],
			prefix[mtx]
		);
	}
	
	//private static final double l0_254 = 1./25.4;
	private static final double l1_254 = 10./25.4;
	private static final double l3_254 = 1000./25.4;
	private static final double l4_254 = 1e4/25.4;
	private static final double l6_254 = 1e6/25.4;
	private static final double D5_254 = 2.54e-5;
	
	private static final String[] unitLength ={
		"mil","cm","inch","m"
	};
	private static final double[][] ratioLength = {
		//destination (直排) ||  source (橫排) 
		{  1.0000 , l4_254, 1000.0, l6_254 },
		{  0.00254, 1.0000, 2.5400, 100.00 },
		{  0.00100, l1_254, 1.0000, l3_254 },
		{  D5_254 , 0.0100, 0.0254, 1.0000 },
	};
	
	private static final double BASE_60_1 = 60.;
	private static final double BASE_1_60 = 1./60.;
	private static final double BASE_36_1 = 3600.;
	private static final double BASE_1_36 = 1./3600.;
	
	private static final String[] unitTime ={
		"s", "sec", "min", "hr"
	};	
	private static final double[][] ratioTime = {
		//destination || source 
		{1.       ,1.       ,BASE_60_1,BASE_36_1},
		{1.       ,1.       ,BASE_60_1,BASE_36_1},
		{BASE_60_1,BASE_1_60,1        ,BASE_60_1},
		{BASE_1_36,BASE_1_36,BASE_60_1,1.       },
	};
	
	private static final String[][] unitAll = {
		unitLength,unitTime
	};//the order is important~~~
	
	private static final char[] si_scale = {
		'p','μ','u','m','k','M','G'
	};//the order is important~~~
	private static final double[] si_value = {
		1e-9,1e-6,1e-6,1e-3,1e3,1e6,1e9
	};//the order is important~~~
	
	private static double findScale(String unit){
		if(unit.length()==1){
			return 1.;//it must be no scale signature
		}
		if(unit.equalsIgnoreCase("mil")==true){
			return 1.;//skip this unit, it shouldn't append scale signature
		}
		char ss = unit.charAt(0);
		for(int i=0; i<si_scale.length; i++){
			if(si_scale[i]==ss){
				return si_value[i];
			}
		}
		return 1.;
	}
	
	private static double findRatio(String srcUnit,String dstUnit){
		if(srcUnit.equalsIgnoreCase(dstUnit)==true){
			return 1.;
		}
		for(int k=0; k<unitAll.length; k++){
			String[] unit = unitAll[k];
			for(int i=0; i<unit.length; i++){
				if(unit[i].equalsIgnoreCase(srcUnit)==true){
					for(int j=0; j<unit.length; j++){
						if(unit[j].equalsIgnoreCase(dstUnit)==true){
							switch(k){
							case 0:
								return ratioLength[j][i];
							case 1:
								return ratioTime[j][i];
							}
						}
					}
				}
			}
		}
		return 0.;//??? what is going on ???
	}
}
