package prj.daemon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Hashtable;

import narl.itrc.Misc;

public class ZipcodeTW {

	private static final int DEPTH_CITY = 0;
	private static final int DEPTH_ZONE = 1;
	private static final int DEPTH_ROAD = 2;
	private static final int DEPTH_SIGN = 3;
	private static final int DEPTH_CODE = 4;
	private static final int MAX_DEPTH = 5;
	
	@SuppressWarnings("rawtypes")
	private static Hashtable<String,Hashtable> tree = new Hashtable<String,Hashtable>();

	/**
	 * read lines from CSV file one by one~~~~
	 * @param pathCSV - CSV fila full path name....
	 */
	public static void buid(String pathCSV){
		tree.clear();
		Path path = Paths.get(pathCSV);
		try {
			BufferedReader fs = Files.newBufferedReader(path);
			String txt = fs.readLine();//skip the first line, this is just title~~
			while((txt = fs.readLine())!=null){
				//three or four segments-->
				//zipcode, city, zone, road, sign
				String[] arg = txt.split(",");
				if(arg.length!=MAX_DEPTH){
					Misc.logw("-->"+txt);
					continue;
				}
				//reorder arguments~~~
				String[] _arg = {
					arg[0].replaceAll("\\s", ""),/*zipcode*/	
					arg[3].replaceAll("\\s", ""),/*road*/
					arg[2].replaceAll("\\s", ""),/*zone*/
					arg[1].replaceAll("\\s", ""),/*city*/
				};
				build_tree(
					tree,
					_arg, _arg.length-1
				);
			}
			fs.close();
			Misc.logw("!!Done!!");
		} catch (IOException e) {	
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes","unchecked"})
	private static void build_tree(
		Hashtable<String,Hashtable> leaf,
		final String[] arg,
		final int idx
	){
		String name = arg[idx];
		Hashtable<String,Hashtable> node = leaf.get(name);
		if(node==null){
			node = new Hashtable<String,Hashtable>();
			leaf.put(name, node);
		}
		if(idx==0){
			//Misc.logv("==> %s] %s] %s] %s]", arg[3], arg[2], arg[1], arg[0]);
			return;
		}
		build_tree(node,arg,idx-1);
	}
	//-------------------------------//
	
	public static void flatten(String pathFlat){
		
		String txt = flatten();
		
		Path path = Paths.get(pathFlat);
		try {
			BufferedWriter fs = Files.newBufferedWriter(path);
			fs.write(txt);
			fs.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String flatten(){
		String txt = "";
		return flatten_tree(0,txt,tree);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String flatten_tree(
		int depth,
		String cntxt,
		Hashtable<String,Hashtable> leaf
	){
		Enumeration<String> set = leaf.keys();
	
		String txt = "";
		
		int idx=0;
		
		while(set.hasMoreElements()==true){
			
			String name = set.nextElement();

			Hashtable<String,Hashtable> nxt = leaf.get(name);
			
			if(nxt!=null){
				txt = txt + flatten_tree(depth+1,cntxt,nxt);
			}
				
			txt = txt +String.format(
				"node%d_%d.put(\"%s\",node%d);\n",
				depth,idx,name,depth+1
			);
			
			idx++;
		}
		
		if(idx==0){
			return String.format(
				"node%d = new Hashtable<String,Hashtable>();\n",
				depth
			);
		}
		return cntxt + txt;
	}
	//-------------------------------//
	
	public static String parse(String address){
		
		return trace_leaf(tree,address,DEPTH_CITY);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String trace_leaf(
		Hashtable<String,Hashtable> root,
		String address,
		int depth
	){
		Enumeration<String> set = root.keys();

		while(set.hasMoreElements()==true){
		
			String name = set.nextElement();
			
			int pos = address.indexOf(name);
			if(pos<0){
				continue;
			}
			address = address.substring(pos+name.length());
			
			root = root.get(name);
			if(root==null){
				return "error --> "+name;//WTF
			}
			if(depth<DEPTH_ROAD){
				return trace_leaf(root,address,depth+1);
			}
			return checkout(root,address);
		}
		return "";
	}
	
	@SuppressWarnings("rawtypes")
	private static String checkout(
		Hashtable<String,Hashtable> root,
		String remain
	){
		return root.keys().nextElement();
	}
}
