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
	private static final int DEPTH_ZONE = 1;
	private static final int DEPTH_ROAD = 2;
	private static final int DEPTH_SIGN_CODE = 3;
	private static final int MAX_COLUMN = 5;//CSV file column

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
					arg[i] = arg[i]
						.replaceAll("\\s", "")
						.replaceAll("\u3000","")
						.replace('（', '(')
						.replace('）', ')');
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
			// TODO Auto-generated catch block
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
		int[] mark = find_mark(address);
		
		String zcode = notation.substring(0,6);
		String badge = notation.substring(6);
		
		
		return "";
	}
	//-------------------------------//

	private static int gather_num(String txt,char tkn,boolean isTail){
		int pos = txt.indexOf(tkn);
		if(pos<0){
			return 0;
		}
		if(isTail==true){
			txt = txt.substring(0, pos);
		}else{
			txt = txt.substring(pos+1);
		}
		if(txt.length()==0){
			return 0;
		}
		char[] cc = txt.toCharArray();
		txt = "";//reset this~~~
		for(int i=0; i<cc.length; i++){
			if('0'<=cc[i] && cc[i]<='9'){
				txt = txt + cc[i];
			}
			if(isTail==false){
				break;//In this condition, we reach a non-digital, just jump out!!!!
			}
		}
		return Integer.valueOf(txt);
	}
	
	private static int[] find_mark(String txt){
		//每一欄代表 '巷','弄','(之)號(樓)'
		//矩陣內順序依序為 '巷','弄','號','之','樓', 0 表示沒有此編號
		int[] digi = new int[5];
		if(txt.length()==0){
			return digi;//they are all zero!!!
		}

		String num = "";
		char[] tkn = txt.toCharArray();
		//first, process a special word....
		int pos = txt.indexOf('之');
		if(pos!=0){
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
		//format:[length][no-zero, 64-base][no-zero, 64-base]...
		int[] mrk = find_mark(txt);
		String val = "";
		char len = 0x0;
		if(mrk[0]!=0){ len = (char)(len+0b00001); val=val+int2txt(mrk[0]); }
		if(mrk[1]!=0){ len = (char)(len+0b00010); val=val+int2txt(mrk[1]); }
		if(mrk[2]!=0){ len = (char)(len+0b00100); val=val+int2txt(mrk[2]); }
		if(mrk[3]!=0){ len = (char)(len+0b01000); val=val+int2txt(mrk[3]); }
		if(mrk[4]!=0){ len = (char)(len+0b10000); val=val+int2txt(mrk[4]); }
		len = (char)(len | BASE_OFF);
		if(len==BASE_OFF){
			return "";
		}
		return ""+len+val;
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
		//check lenth bit, then parse value~~~
		int[] mrk = new int[5];
		if(txt.length()==0){
			return mrk;//they are all zero!!!
		}
		char[] tkn = txt.toCharArray();
		int i=0;
		for(; i<tkn.length; i++){
			tkn[i] = (char)(tkn[i] - BASE_OFF);
		}
		i=1;//reset this for counting....
		if( (tkn[0]&0b00001)!=0 ){ mrk[0] = tkn[i]*BASE_VAL + tkn[i+1]; i+=2; }
		if( (tkn[0]&0b00010)!=0 ){ mrk[1] = tkn[i]*BASE_VAL + tkn[i+1]; i+=2; }
		if( (tkn[0]&0b00100)!=0 ){ mrk[2] = tkn[i]*BASE_VAL + tkn[i+1]; i+=2; }
		if( (tkn[0]&0b01000)!=0 ){ mrk[3] = tkn[i]*BASE_VAL + tkn[i+1]; i+=2; }
		if( (tkn[0]&0b10000)!=0 ){ mrk[4] = tkn[i]*BASE_VAL + tkn[i+1]; i+=2; }
		return mrk;
	}
	
	@SuppressWarnings("unused")
	private static String dbg_notation = null; 
	
	public static String notation(String txt,final String code){
		
		dbg_notation = txt;
				
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
		
		char tkn = '?';
		//有門牌號碼，判斷範圍，決定是單，雙，連號，至，以上或以下
		if(txt.matches(".*[單雙]?[全]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.endsWith("單全")==true){
				tkn=0x10;
			}else if(txt.endsWith("雙全")==true){
				tkn=0x11;
			}else if(txt.endsWith("全")==true){
				tkn=0x12;
			}
			return String.format("%c",tkn);
			
		}else if(txt.matches(".*[單雙連]?.+以[上下]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.endsWith("以上")==true){
				tkn=0x13;//0x13, 0x14(單), 0x15(雙), 0x16(連) 
			}else if(txt.endsWith("以下")==true){
				tkn=0x17;//0x17, 0x18(單), 0x19(雙), 0x1A(連) 
			}
			if(txt.indexOf('單')>=0){
				tkn =(char)(tkn+1);
			}else if(txt.indexOf('雙')>=0){
				tkn =(char)(tkn+2);
			}else if(txt.indexOf('連')>=0){
				tkn =(char)(tkn+3);
			}
			return String.format("%c",tkn);
			
		}else if(txt.matches(".*[單雙連]?.+至.+")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.indexOf('單')>=0){
				tkn = 0x1B;
			}else if(txt.indexOf('雙')>=0){
				tkn = 0x1C;
			}else if(txt.indexOf('連')>=0){
				tkn = 0x1D;
			}else{
				//Is this possible???
				System.out.printf("warning-->%s\n",dbg_notation);//checking
			}
			int pos = txt.lastIndexOf('至');
			String sta = txt.substring(0,pos);
			String off = txt.substring(pos+1);
			return String.format("%c",tkn);
			
		}else{
			//System.out.printf("%s\n",dbg_notation);//checking
			//只剩"門號"+[含附號][及以上附號]
			if(txt.endsWith("含附號")==true){
				
			}else if(txt.endsWith("及以上附號")==true){
				
			}else{
				
			}
		}
		return "?";
	}
}
