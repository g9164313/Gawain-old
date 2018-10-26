package narl.itrc;

/**
 * convenient functions for changing all units. 
 * @author qq
 *
 */
public class UtilPhysical {
	
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
	
	public static final String[][] unitAll = {
		unitLength,unitTime
	};//the order is important~~~
	
	public static final char[] si_scale = {
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

	public static String[] split_value_unit(String txt){
		txt = txt.replaceAll("\\s+", "");		
		int i = 0;
		for(i=0; i<txt.length(); i++){
			char cc = txt.charAt(i);
			if( !(('0'<=cc && cc<='9') || cc=='.') ){
				break;
			}
		}
		String[] arg = new String[2];
		arg[0] = txt.substring(0, i);
		arg[1] = txt.substring(i);
		return arg;
	}
	
	public static double convert(
		double srcVal,
		String srcUnit,
		String dstUnit
	) throws NumberFormatException {
		srcUnit = srcUnit.trim();//for safety~~~
		dstUnit = dstUnit.trim();
		if(srcUnit.equals(dstUnit)==true){
			return srcVal;//we don't need convert value~~~~
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
		return srcVal*scale*ratio;
	}
	
	public static double convert(
		String srcValUnit,
		String dstUnit
	) throws NumberFormatException {
		double srcVal=0.;
		String[] srcTxt = split_value_unit(srcValUnit);
		srcVal = Double.valueOf(srcTxt[0]);
		return convert(srcVal,srcTxt[1],dstUnit);
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
	
	public static String num2prefix(double number){
		return num2prefix(number,0);
	}
	public static String num2prefix(double number,int power){
		
		String[] txt = String.format("%E",number).split("E");
		
		double val = Double.valueOf(txt[0]);
		
		int prx = Integer.valueOf(txt[1]);
		
		int cnt = Math.abs(prx);
		
		if(cnt==0){			
			return String.format("%."+power+"f",val);//special case~~~
		}

		final char[] p_prefix = {' ','k','M','G','T','P','E'};	
		final char[] n_prefix = {' ','m','μ','n','p','f','a'};
		char mtrx = ' ';
		if(prx>=0){
			mtrx = p_prefix[cnt/3];
			val = val * Math.pow(10.,(cnt%3));
		}else{
			int scale = 1+(cnt-1)/3;
			mtrx = n_prefix[scale];
			val = number * Math.pow(10.,scale*3);
		}
		if(mtrx!=' '){
			return String.format("%."+power+"f%c",val,mtrx);
		}
		return String.format("%."+power+"f",val);
	}
}
