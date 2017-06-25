package prj.daemon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Hashtable;


public class ZipcodeTW {

	private static final int DEPTH_CITY = 0;
	//private static final int DEPTH_ZONE = 1;
	//private static final int DEPTH_ROAD = 2;
	private static final int DEPTH_SIGN_CODE = 3;
	private static final int MAX_COLUMN = 5;//CSV file column
	
	private static final char TKN_UNKNOW = '?';
	
	@SuppressWarnings("rawtypes")
	private static Hashtable<String,Hashtable> treeLeaf = new Hashtable<String,Hashtable>();
	
	@SuppressWarnings("rawtypes")
	private static Hashtable<String,Hashtable> treeRoot = new Hashtable<String,Hashtable>();

	//有些政府單位的郵遞號碼是唯一，沒地址就查這表
	private static Hashtable<String,String> nameBase = new Hashtable<String,String>();
	
	/**
	 * read lines from CSV file one by one~~~~
	 * @param pathCSV - CSV fila full path name....
	 */
	public static void buid(String pathCSV){
		treeRoot.clear();
		Path path = Paths.get(pathCSV);
		try {
			BufferedReader fs = Files.newBufferedReader(path);
			String txt = fs.readLine();//skip the first line, this is just title~~
			while((txt = fs.readLine())!=null){
				//three or four segments-->
				//zipcode, city, zone, road, sign
				String[] arg = txt.split(",");
				if(arg.length!=MAX_COLUMN){
					System.err.println("-->"+txt);
					continue;
				}
				//first triming~~~
				for(int i=0; i<arg.length; i++){
					arg[i] = cook_word(arg[i]);
				}
				//reorder arguments~~~
				String[] _arg = {
					arg[1],/*city*/
					arg[2],/*zone*/
					arg[3],/*road*/
					arg[0]+notation(arg[4],arg[0])/*zipcode + sign*/
				};
				//System.out.println(arg[4]+" ==> "+_arg[3]);
				build_tree(treeRoot,_arg, DEPTH_CITY);
			}
			fs.close();
			System.out.println("!!build done!!");
		} catch (IOException e) {	
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes","unchecked"})
	private static void build_tree(
		Hashtable<String,Hashtable> node,
		final String[] arg,
		final int depth
	){
		String name = arg[depth];
		if(depth==DEPTH_SIGN_CODE){
			node.put(name, treeLeaf);
			return;
		}
		Hashtable<String,Hashtable> nd = node.get(name);
		if(nd==null){
			nd = new Hashtable<String,Hashtable>();
			node.put(name, nd);
		}
		build_tree(nd,arg,depth+1);
	}
	//-------------------------------//
	
	public static void flatten(String full_name){
		String txt = flatten();
		System.out.println("!!flatten done!!");
		try {
			Path path = Paths.get(full_name);
			BufferedWriter fs = Files.newBufferedWriter(path);
			fs.write(txt);
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String flatten(){
		return 
			"static{\n\t Hashtable<String,Hashtable> node = treeRoot;\n" + 
			flatten_tree(treeRoot,"node")+
			"}\n";
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String flatten_tree(
		Hashtable<String,Hashtable> node,
		final String nodeName
	){
		String txt = "";
		
		int index = 0;
		
		Enumeration<String> set = node.keys();
		
		while(set.hasMoreElements()==true){
			
			String name = set.nextElement();

			Hashtable<String,Hashtable> nxt = node.get(name);
			
			if(nxt.isEmpty()==true){
				txt += String.format(
					"\t%s.put(\"%s\",treeLeaf);\n",
					nodeName, name
				);
			}else{
				String nxt_name = nodeName+"_"+index;
				
				txt += "\tHashtable<String,Hashtable> " + nxt_name + " = new Hashtable<String,Hashtable>();\n" +
					flatten_tree(nxt, nxt_name) + 
					"\t"+nodeName + ".put(\""+name+"\","+nxt_name+");\n" + 					
					"\t/*----------------*/\n"; 
			}
			
			index++;
		}
		return txt;
	}
	//-------------------------------//
	
	public static String parse(String address){
		address = cook_word(address);
		return trace_tree(treeRoot,address,DEPTH_CITY);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String trace_tree(
		Hashtable<String,Hashtable> root,
		String address,
		int depth
	){
		Enumeration<String> set = root.keys();

		while(set.hasMoreElements()==true){
		
			String notation = set.nextElement();
			
			if(depth>=DEPTH_SIGN_CODE){
				String zcode = decide_zipcode(address,notation);
				if(zcode.length()!=0){
					return zcode;
				}			
			}else{			
				int pos = address.indexOf(notation);
				if(pos<0){
					continue;
				}
				address = address.substring(pos+notation.length());
				
				root = root.get(notation);
				if(root==null){
					return "?????";//WTF!!!
				}			
				return trace_tree(root,address,depth+1);
			}
		}
		return "?????";//WTF!!!
	}
	
	private static String decide_zipcode(
		String address,
		String notation	
	){
		int[]  num = find_digi(address);
		//notation format: {zip-code}[token]{vcode}
		String zip = notation.substring(0,5);
		int    tkn = notation.charAt(5);
		String cod = notation.substring(6);
		int[]  bnd1 = vcode2mark(cod);
		
		
		Boolean res = check_even_odd(tkn,num);
		if(res!=null){
			return (res)?(zip):("");
		}
		res = check_bound(tkn,bnd1,num);
		if(res!=null){
			return (res)?(zip):("");
		}
		
		int[] bnd2 =  vcode2mark_sec(cod);
		res = check_bound(tkn,bnd1,bnd2,num);
		if(res!=null){
			return (res)?(zip):("");
		}
		
		return "";
	}
	
	private static Boolean check_even_odd(int tkn, int[] num){
		switch(tkn){
		case 0x10://雙
		case 0x11://單
			for(int v:num){
				if(v==0){
					continue;
				}
				if(v%2==(tkn-0x10)){
					return true;
				}else{
					return false;
				}
			}
			break;
		case 0x12://全
			return true;
		}
		return null;
	}
	
	private static Boolean check_bound(int tkn, int[] rng, int[] num){
		if(tkn<0x13 || 0x1A<tkn){
			return null;
		}
		for(int i=rng.length-1; i>=0; i--){
			if(rng[i]!=0){
				if(num[i]==0){
					return false;
				}
				switch(tkn){	
				case 0x14://以上-雙
				case 0x15://以上-單
					if(rng[i]<=num[i]){
						if(num[i]%2==(tkn-0x14)){
							return true;
						}
					}
					return false;
				case 0x13://以上
				case 0x16://以上-連
					if(rng[i]<=num[i]){
						return true;
					}
					return false;
				
				case 0x18://以下-雙
				case 0x19://以下-單
					if(num[i]<=rng[i]){
						if(num[i]%2==(tkn-0x18)){
							return true;
						}
					}
					return false;
				case 0x1A://以下-連
				case 0x17://以下
					if(num[i]<=rng[i]){
						return true;
					}
					return false;
				}
				return false;
			}
		}
		return false;
	}

	private static Boolean check_bound(
		int tkn, 
		int[] rng1, int[] rng2, 
		int[] num
	){
		int aa = rng1[fst_nonzero_index(rng1)];
		int bb = rng2[fst_nonzero_index(rng2)];
		int cc = rng1[fst_nonzero_index(num)];
		if(aa>=bb || aa==0 || bb==0){
			System.err.println("impossible, check it!!!");
			return false;
		}
		if(aa<=cc && cc<=bb){
			if(tkn==0x1B || tkn==0x1C){
				if(cc%2==(tkn-0x1B)){
					return true;
				}else{
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	private static int fst_nonzero_index(int[] val){
		for(int i=0; i<val.length; i++){
			if(val[i]!=0){
				return i;
			}
		}
		return -1;
	}
	//-------------------------------//

	private static String cook_word(String txt){
		//使用者可能會用全行字，或空白隔開，消滅牠們!!!
		txt = txt
			.replaceAll("\\s", "")
			.replaceAll("\u3000","")
			.replace('（', '(')
			.replace('）', ')')
			.replace('０', '0')
			.replace('１', '1')
			.replace('２', '2')
			.replace('３', '3')
			.replace('４', '4')
			.replace('５', '5')
			.replace('６', '6')
			.replace('７', '7')
			.replace('８', '8')
			.replace('９', '9');
		//資料庫裡的路名跟街名用國字，巷弄號則用阿拉伯數字，統一他們???
		return txt;
	}
	

	private static int[] find_digi(String txt){
		//矩陣內順序依序為 '巷','弄','號','之','樓', 0 表示沒有此編號
		int[] digi = new int[5];
		if(txt.length()==0){
			return digi;//they are all zero!!!
		}

		String num = "";
		char[] tkn = txt.toCharArray();
		//first, process a special word....
		int pos = txt.indexOf('之');
		if(pos>0){
			for(int i=pos+1; i<tkn.length; i++){
				char cc = tkn[i];
				if( cc<'0' || '9'<cc ){
					String fore = txt.substring(0,pos);
					String post = txt.substring(i);
					txt = fore+post;
					tkn = txt.toCharArray();
					break;
				}
				num = num + cc;
			}
			digi[3] = Integer.valueOf(num);
			num = "";//reset again~~~
		}
		//second, gather all numbers~~~
		for(char cc:tkn){
			if('0'<=cc && cc<='9'){
				num = num + cc;
				continue;
			}
			switch(cc){
			case '巷':
				digi[0] = Integer.valueOf(num);
				break;
			case '弄':
				digi[1] = Integer.valueOf(num);
				break;
			case '號':
				if(num.length()==0){
					break;//忽略 "含附號","及以上附號"
				}
				digi[2] = Integer.valueOf(num);
				break;
			case '樓':
				digi[4] = Integer.valueOf(num);
				break;
			}
			num = "";//reset!!;
		}
		return digi;
	} 
	
	private final static int BASE_VAL=96;
	private final static int BASE_OFF=32;
		
	public static String mark2vcode(String txt){
		//format:[length][no-zero, 96-base][no-zero, 96-base]...
		int[] digi = find_digi(txt);
		String code = "";
		char len = 0x0;
		if(digi[0]!=0){ len = (char)(len+0b00001); code=code+int2txt(digi[0]); }
		if(digi[1]!=0){ len = (char)(len+0b00010); code=code+int2txt(digi[1]); }
		if(digi[2]!=0){ len = (char)(len+0b00100); code=code+int2txt(digi[2]); }
		if(digi[3]!=0){ len = (char)(len+0b01000); code=code+int2txt(digi[3]); }
		if(digi[4]!=0){ len = (char)(len+0b10000); code=code+int2txt(digi[4]); }
		len = (char)(len | BASE_OFF);
		if(len==BASE_OFF){
			return "";
		}
		return ""+len+code;
	}

	private static String int2txt(int val){
		String txt = "";
		while(val!=0){
			char rem = (char)(val%BASE_VAL + BASE_OFF);//skip EOF character....
			txt = "" + rem +txt;
			val = val / BASE_VAL;
		}
		return txt;
	}
	
	public static int[] vcode2mark(String txt){
		//format:[length][no-zero, 64-base][no-zero, 64-base]...
		//check length bit, then parse value~~~
		int[] digi = new int[5];
		if(txt.length()==0){
			return digi;//they are all zero!!!
		}
		char[] code = txt.toCharArray();
		int i=0;
		for(; i<code.length; i++){
			code[i] = (char)(code[i] - BASE_OFF);
		}
		i=1;//reset this for counting....
		if( (code[0]&0b00001)!=0 ){ digi[0] = code[i]*BASE_VAL + code[i+1]; i+=2; }
		if( (code[0]&0b00010)!=0 ){ digi[1] = code[i]*BASE_VAL + code[i+1]; i+=2; }
		if( (code[0]&0b00100)!=0 ){ digi[2] = code[i]*BASE_VAL + code[i+1]; i+=2; }
		if( (code[0]&0b01000)!=0 ){ digi[3] = code[i]*BASE_VAL + code[i+1]; i+=2; }
		if( (code[0]&0b10000)!=0 ){ digi[4] = code[i]*BASE_VAL + code[i+1]; i+=2; }
		return digi;
	}
	
	public static int[] vcode2mark_sec(String txt){
		char tkn = txt.charAt(0);
		int len = 0;
		if( (tkn&0b00001)!=0 ){ len+=2; }
		if( (tkn&0b00010)!=0 ){ len+=2; }
		if( (tkn&0b00100)!=0 ){ len+=2; }
		if( (tkn&0b01000)!=0 ){ len+=2; }
		if( (tkn&0b10000)!=0 ){ len+=2; }
		return vcode2mark(txt.substring(len+1));
	}
	
	private static String dbg_txt = null; 
	
	public static String notation(String txt,final String code){		
		dbg_txt = txt;		
		//特殊地點
		if(txt.matches(".*[(].*[)]")==true){
			int pos = txt.indexOf("(");
			String name = txt.substring(pos);
			name = name.substring(1,name.length()-1);
			if(code!=null){
				nameBase.put(name, code);
			}else{
				System.err.print("X--> "+name+" without zipcode\n");
				return "?";//沒給郵遞區號，直接放棄這資料
			}
			//只剩門號的邏輯判斷
			return "?";
		}
		
		char tkn = TKN_UNKNOW;
		//有門牌號碼，判斷範圍，決定是單，雙，連號，至，以上或以下
		if(txt.matches(".*[單雙]?[全]")==true){
			
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.endsWith("雙全")==true){
				tkn=0x10;
			}else if(txt.endsWith("單全")==true){
				tkn=0x11;
			}else if(txt.endsWith("全")==true){
				tkn=0x12;
			}
			txt = mark2vcode(txt);	
			return String.format("%c%s",tkn,txt);
			
		}else if(txt.matches(".*[單雙連]?.+以[上下]")==true){
			
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.endsWith("以上")==true){
				tkn=0x13;//0x13, 0x14(雙), 0x15(單), 0x16(連) 
			}else if(txt.endsWith("以下")==true){
				tkn=0x17;//0x17, 0x18(雙), 0x19(單), 0x1A(連) 
			}
			if(txt.indexOf('雙')>=0){
				tkn =(char)(tkn+1);
			}else if(txt.indexOf('單')>=0){
				tkn =(char)(tkn+2);
			}else if(txt.indexOf('連')>=0){
				tkn =(char)(tkn+3);
			}
			txt = mark2vcode(txt);	
			return String.format("%c%s",tkn,txt);
			
		}else if(txt.matches(".*[單雙連]?.+至.+")==true){
			
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.indexOf('雙')>=0){
				tkn = 0x1C;
			}else if(txt.indexOf('單')>=0){
				tkn = 0x1B;
			}else if(txt.indexOf('連')>=0){
				tkn = 0x1D;
			}else{
				System.err.printf("[INVALID]-->%s\n",dbg_txt);//checking
				return ""+tkn;
			}
			int pos = txt.lastIndexOf('至');
			String beg = txt.substring(0,pos);
			String end = txt.substring(pos+1);
			beg = mark2vcode(beg);	
			end = mark2vcode(end);
			return String.format("%c%s%s",tkn,beg,end);
			
		}else if(txt.endsWith("含附號")==true){
			txt = mark2vcode(txt);
			return String.format("%c%s",0x1E,txt);
			
		}else if(txt.endsWith("及以上附號")==true){
			txt = mark2vcode(txt);
			return String.format("%c%s",0x1F,txt);
		}
		
		System.err.printf("[NO PROCESS]-->%s\n",dbg_txt);//checking
		return ""+tkn;
	}
}
