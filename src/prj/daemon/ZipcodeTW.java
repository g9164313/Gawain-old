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
		
			String clue = set.nextElement();
			
			if(depth>=DEPTH_SIGN_CODE){
				
				String zcode = parse_notation(address,clue);
				if(zcode.length()!=0){
					return zcode;
				}
				
			}else{
				
				int pos = address.indexOf(clue);
				if(pos<0){
					continue;
				}
				address = address.substring(pos+clue.length());
				
				root = root.get(clue);
				if(root==null){
					return "?????";//WTF!!!
				}
				
				return trace_tree(root,address,depth+1);
			}
		}
		return "?????";//WTF!!!
	}
	
	private static String parse_notation(
		String address,
		String notation	
	){
		int[] mark = find_mark(address);
		
		String zcode = notation.substring(0,6);
		String badge = notation.substring(6);
		
		char tkn1 = badge.charAt(0);
		char tkn2 = badge.charAt(1);
		String[] args = badge.substring(2).split(",");
	
		int[] val = {
			Integer.valueOf(args[0]),
			Integer.valueOf(args[1]),
			Integer.valueOf(args[2]),
			Integer.valueOf(args[3]),
			Integer.valueOf(args[4]),
		};
		
		switch(tkn1){
		case '?'://ignore this,always fail~~~
			break;
		case '*'://全

			break;
		case '>'://以上
			break;
		case '<'://以下
			break;
		case '~'://從某門號"至"某門號
			int[] vtx = {
				Integer.valueOf(args[0]),
				Integer.valueOf(args[1]),
				Integer.valueOf(args[2]),
				Integer.valueOf(args[3]),
				Integer.valueOf(args[4]),
			};
			break;
		case '$'://含附號
		case '%'://及以上附號
		case '@'://固定門 號
			break;
		}
		return "";
	}
	
	private boolean check_direct(int[] mark, char attr, int[] vals){
		return true;
	}
	
	//-------------------------------//
	
	private static int digiHead, digiTail;
	
	private static int find_digital(final String txt,char tkn){

		digiTail = txt.indexOf(tkn);
		digiHead = digiTail - 1;
		if(digiTail<0 || digiHead<0){
			return 0;//we fail~~~~
		}
		
		int value=0;
		
		while(digiHead>=0){
			char cc = txt.charAt(digiHead);
			if('0'<=cc && cc<='9'){
				int dd = (int)(cc - '0');
				value = value + dd*(int)Math.pow(10,digiTail-digiHead-1);
			}else{
				break;
			}
			digiHead--;
		}
		return value;
	}
	
	private static int markHead, markTail;
	
	private static void check_mark(){
		if(digiHead>=-1 && digiHead<markHead){
			markHead = digiHead;
		}
		if(digiTail>0 && markTail<digiTail){
			markTail = digiTail;
		}
	}
	
	private static int[] find_mark(String txt){
		
		int[] digi = new int[5];//每一欄代表 '巷','弄','號','之','樓'
		if(txt.length()==0){
			return digi;//they are all zero!!!
		}
		
		markHead = txt.length();
		markTail = -1;
		
		digi[0] = find_digital(txt,'巷');
		check_mark();

		digi[1] = find_digital(txt,'弄');
		check_mark();

		digi[2] = find_digital(txt,'號');
		check_mark();
		digi[3] = find_digital(txt,'之');//speical case~~~
		if(digiTail>=0 && digiTail<markTail){
			//此時的 digi[4]用來暫時存放，digi[2]與digi[3]互相交換
			digi[4] = digi[2];
			digi[2] = digi[3];
			digi[3] = digi[4];
			check_mark();
		}else{
			digi[3] = 0;//reset this sign, it is invalid~~~
		}

		digi[4] = find_digital(txt,'樓');
		check_mark();
		
		return digi;
	} 
	
	private static String text2badge(String text){
		String badge = "-,";
		//對於最後一組數字的限制
		if(text.indexOf('單')>=0){
			badge = "o";//odd
		}else if(text.indexOf('雙')>=0){
			badge = "e";//even
		}else if(text.indexOf('連')>=0){
			badge = "i";//integration
		}
		//依序為'巷','弄','號','之','樓'
		int[] val = find_mark(text);
		return badge+
			val[0]+","+
			val[1]+","+
			val[2]+","+
			val[3]+","+
			val[4];
	}
	
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
			txt = txt.substring(0, pos);//只剩門號的邏輯判斷
		}
		
		//有門牌號碼，判斷範圍，決定是單，雙，連號，至，以上或以下
		if(txt.matches(".*[單雙]?[全]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			txt = txt.substring(0, txt.length()-1);
			return "*"+text2badge(txt);
			
		}else if(txt.matches(".*[單雙連]?.+以[上下]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			int len = txt.length();
			char tkn = txt.charAt(len-1);
			txt = txt.substring(0,len-2);
			if(tkn=='上'){
				return ">"+text2badge(txt);
			}else if(tkn=='下'){
				return "<"+text2badge(txt);
			}
			
		}else if(txt.matches(".*[單雙連]?.+至.+")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			int pos = txt.indexOf('至');
			String prev = txt.substring(0,pos);
			String post = txt.substring(pos+1);
			int[] vtx = find_mark(post);
			return "~"+
				text2badge(prev)+","+
				vtx[0]+","+vtx[1]+","+vtx[2]+","+vtx[3]+","+vtx[4];
			
		}else{
			//System.out.printf("%s\n",dbg_notation);//checking
			//只剩"門號"+[含附號][及以上附號]
			String cmd = null;
			if(txt.endsWith("含附號")==true){
				
			}else if(txt.endsWith("及以上附號")==true){
				
			}else{
				return "@"+text2badge(txt);
			}
		}
		return "?";
	}
}
