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
						.replaceAll("\u3000","");
				}
				//reorder arguments~~~
				String[] _arg = {
					arg[1],/*city*/
					arg[2],/*zone*/
					arg[3],/*road*/
					arg[0]+notation(arg[4])/*zipcode + sign*/
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
				
				String zipcode = checkout(address,clue);
				
				if(zipcode.length()!=0){
					return zipcode;
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
	
	private static String checkout(
		String address,
		String clue	
	){
		int mark[] = find_mark(address);
		
		String zipcode = clue.substring(0,6);
		String notation= clue.substring(6);
		
		
		
		return zipcode;
	}
	//-------------------------------//
	
	private static int markHead, markTail;
	
	private static int[] find_mark(String txt){
		
		int[] digi = new int[5];
		
		final char[] tkn = {'巷','弄','之','號','樓'};
		
		markHead = txt.length();//reset position~~~
		markTail = -1;//reset position~~
		
		for(int i=0; i<tkn.length; i++){
			
			digi[i] = find_digital(txt,tkn[i]);
			
			if(digiTail>=0 && markTail<digiTail){
				
				markTail = digiTail;
				
				if(digiHead<markHead){
					
					markHead = digiHead;
				}
			}
		}
		return digi;
	} 
	
	private static String replace_mark(String txt){
		
		if(markTail<0){
			return txt;
		}
		String tail = txt.substring(markTail+1);
		
		String head = null;
		if(markHead<0){
			head = "";
		}else{
			head = txt.substring(0, markHead+1);
		}
		
		return head + "#" + tail;
	}
	
	private static int digiHead, digiTail;
	
	private static int find_digital(String txt,char tkn){
		
		int value=0;
		
		digiTail = txt.indexOf(tkn);
		digiHead = digiTail - 1;
		if(digiTail<0 || digiHead<0){
			return 0;
		}
		
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
	
	private static String dbg_notation = null; 
	 
	public static String notation(String txt){
		
		dbg_notation = txt;
		
		int[] mark1 = find_mark(txt);
		txt = replace_mark(txt);
		
		int[] mark2 = find_mark(txt);
		txt = replace_mark(txt);
		
		System.out.printf("%s --> %s\n", txt, dbg_notation);
		
		return "";
	}
	
	/*private static String dbg_notation = null;
	
	private static String notation(String txt){
		dbg_notation = txt;
		String nota = "";
		String appx = "";
		//First, skip some special case or prefix notation~~~
		if(txt.matches("^\\d+號")==true){
			return "!"+decide_mark(txt);
		}
		if(txt.matches("^\\d+巷\\d+弄.*")==true){
			int pos = txt.indexOf('弄');
			appx = ","+txt.substring(0, pos+1);
			txt = txt.substring(pos+1);
		}
		if(txt.matches("^\\d+巷.*")==true){
			int pos = txt.indexOf('巷');
			appx = ","+txt.substring(0, pos+1);
			txt = txt.substring(pos+1);
		}
		//Second, create logic for regular mark~~~ 
		char cc = txt.charAt(0);
		if(cc=='單'){
			if(txt.charAt(1)=='全'){
				nota = "W";//special case...
			}else{
				nota = "@"+decide_range(txt.substring(1));//odd token is '@'
			}
		}else if(cc=='雙'){
			if(txt.charAt(1)=='全'){
				nota = "R";//special case...
			}else{
				nota = "$"+decide_range(txt.substring(1));//even token is '$'
			}
		}else if(cc=='連'){
			nota = "#"+decide_range(txt.substring(1));//continue token is "#";
		}else if(cc=='全'){
			nota = "*";
		}else{
			System.err.println("unknown.1 - "+dbg_notation);
			return "???";
		}
		return nota+appx;
	}
	
	private static String decide_range(String txt){
		int pos = txt.indexOf("以下");
		if(pos>=0){
			return "<"+decide_mark(txt.substring(0,pos));
		}
		pos = txt.indexOf("以上");
		if(pos>=0){
			return ">"+decide_mark(txt.substring(0,pos));
		}
		pos = txt.indexOf("至");
		if(pos>=0){
			return "~"+decide_mark(txt.substring(0,pos))+
				" "+decide_mark(txt.substring(pos+1));
		}
		System.err.println("unknown.2 - "+dbg_notation);
		return "???";
	}*/
}
