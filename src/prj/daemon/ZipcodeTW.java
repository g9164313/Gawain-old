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

	private static int[] find_mark(String txt){
		//每一欄代表 '巷','弄','(之)號(樓)'
		//'(之)號(樓)'為的固定點，也就是連除 40000跟 200，用來表示'之'跟'樓'
		int[] digi = new int[3];
		if(txt.length()==0){
			return digi;//they are all zero!!!
		}
		
		return digi;
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
			txt = txt.substring(0, pos);//只剩門號的邏輯判斷
		}
		
		char tkn = '?';
		//有門牌號碼，判斷範圍，決定是單，雙，連號，至，以上或以下
		if(txt.matches(".*[單雙]?[全]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.endsWith("單全")==true){
				tkn=10;
			}else if(txt.endsWith("雙全")==true){
				tkn=11;
			}else if(txt.endsWith("全")==true){
				tkn=12;
			}
			return String.format("%c",tkn);
			
		}else if(txt.matches(".*[單雙連]?.+以[上下]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			if(txt.endsWith("以上")==true){
				tkn=20; 
			}else if(txt.endsWith("以下")==true){
				tkn=25;
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
				tkn = 31;
			}else if(txt.indexOf('雙')>=0){
				tkn = 32;
			}else if(txt.indexOf('連')>=0){
				tkn = 33;
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
