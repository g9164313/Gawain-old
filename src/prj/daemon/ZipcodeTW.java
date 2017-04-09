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
		int[] mark = find_mark(address);
		
		String zipcode = clue.substring(0,6);
		String notation= clue.substring(6);
		
		char tkn = notation.charAt(0);
		notation = notation.substring(1);
		int[] grp0 = null;
		int[] grp1 = null;
		if(notation.length()!=0){
			String[] grp = notation.split("#");
			if(grp.length==1){
				grp0 = text2mark(grp[0]);
			}else if(grp.length==2){
				grp0 = text2mark(grp[0]);
				grp1 = text2mark(grp[1]);
			}
		}
		
		switch(tkn){
		case '?'://ignore this~~~
			break;
		case '*'://全
			if(isMatch(grp0,mark)==false){
				break;
			}
			return zipcode;
		case 'A'://單全
			if(mark[2]%2==1){
				if(isMatch(grp0,mark)==false){
					break;
				}
				return zipcode;
			}
			break;
		case 'B'://雙全
			if(mark[2]%2==0){
				if(isMatch(grp0,mark)==false){
					break;
				}
				return zipcode;
			}
			break;
		
		case 'W'://TODO: '含附號'
		case 'S'://TODO: '及以上附號'
		case '@'://只有 '號'
			if(grp0[2]==mark[2]){
				return zipcode;
			}
			break;
		}
		return "";
	}
	
	private static boolean isMatch(int[] group,int[] mark){
		if(group==null){
			return true;
		}
		for(int i=0; i<group.length; i++){
			if(group[i]!=0 && mark[i]!=group[i]){
				return false;
			}
		}
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
	
	private static int[] text2mark(String text){
		int[] mark = new int[4];
		String[] val = text.split(",");
		for(int i=0; i<val.length; i++){
			mark[i] = Integer.valueOf(val[i]);
		}
		return mark;
	}
	
	private static String mark2text(int[] val){
		return String.format(
			"%d,%d,%d,%d,%d", 
			val[0],val[1],val[2],val[3],val[4]
		);//依序為'巷','弄','號','之','樓'
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
			txt = txt.substring(0, pos);//只剩門號就好
		}
		
		//有門牌號碼，判斷範圍，決定是單，雙，連號，至，以上或以下
		if(txt.matches(".*[單雙]?[全]")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
			
			String cmd = "*";//至少一定是'全'
			if(txt.equalsIgnoreCase("全")==true){
				return cmd;
			}else if(txt.endsWith("單全")==true){
				txt = txt.replace("單全", "");
				cmd = "A";	
			}else if(txt.endsWith("雙全")==true){
				txt = txt.replace("雙全", "");
				cmd = "B";
			}
			if(txt.length()==0){
				return cmd;
			}
			return cmd+mark2text(find_mark(txt));
			
		}else if(txt.matches(".*[單雙連]?.+以[上下]")==true){
			System.out.printf("%s\n",dbg_notation);//checking
			
		}else if(txt.matches(".*[單雙連]?.+至.+")==true){
			//System.out.printf("%s\n",dbg_notation);//checking
				
		}else{
			//System.out.printf("%s\n",dbg_notation);//checking
			//只剩"門號"+[含附號][及以上附號]
			String cmd = null;
			if(txt.endsWith("含附號")==true){
				txt = txt.replace("含附號", "");
				cmd = "W";
			}else if(txt.endsWith("及以上附號")==true){
				txt = txt.replace("及以上附號", "");
				cmd = "S";
			}else{
				cmd = "@";
			}
			return cmd+mark2text(find_mark(txt));
		}
		return "?";
	}
}
