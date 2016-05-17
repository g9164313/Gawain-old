package narl.itrc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.scene.Node;
import javafx.scene.Parent;

public class CamVidcap extends CamBundle {
	
	private static final String CMD_V4L2 = "v4l2-ctl";
	private static Boolean haveV4L2 = null;
	
	public CamVidcap(){
		checkV4L2();
	}
	
	public CamVidcap(int index,String config){
		super(index,config);
		checkV4L2();
	}
	
	private native void implSetup(CamBundle cam,int id);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	@Override
	public void setup(int idx,String configName) {
		implSetup(this,idx);
	}
	
	@Override
	public void fetch() { 
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}
	
	private static boolean checkDevList(int idx){
		if(haveV4L2==false){
			return false;
		}
		return true;
	}
	
	private static void checkV4L2(){
		if(haveV4L2!=null){			
			return;//we already had this information~~
		}
		String txt = Misc.syncExec(CMD_V4L2,"--help");		
		if(txt.contains("General/Common options")==true){
			haveV4L2 = true;
		}else{
			haveV4L2 = false;
		}
	}

	class PanSetting extends PanOption {
		private String arg1;
		
		public PanSetting(int idx){
			title = "設定V4L2";
			arg1="--device=/dev/video"+idx;
			parse_info(Misc.syncExec(CMD_V4L2,arg1,"--list-ctrls-menus"));
		}
		
		private void parse_info(String txt){			
			String ln = null;
			String[] pm = {"","",""};//type,name,parameters
			try {
				BufferedReader br = new BufferedReader(
					new InputStreamReader(
						new ByteArrayInputStream(txt.getBytes())
					)
				);				
				while((ln=br.readLine())!=null){					
					parse_line(ln,pm);
					if(pm[0].equals("int")==true){						
						create_slide(pm[1],pm[2]);						
					}else if(pm[0].equals("bool")==true){
						create_checkbox(pm[1],pm[2]);
					}else if(pm[0].equals("menu")==true){						
						create_combobox(pm[1],pm[2],br);//it is special!!!!
					}
				}				
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void parse_line(String txt,String[] res){
			res[0] = res[1] = res[2] = "";//reset data~~~
			int p1 = txt.indexOf("(");
			int p2 = txt.indexOf(")");
			int p3 = txt.indexOf(":");
			if(p1<0 || p2<0 || p3<0){
				return;
			}
			res[0] = txt.substring(p1+1,p2);
			res[1] = txt.substring(0,p1).trim();
			res[2] = txt.substring(p3+1).trim();
		}
		
		private void create_slide(String name,String txt){
			int param[]={
				0/* 0:default */,
				0/* 1:value   */,
				0/* 2:minimum */,
				0/* 3:maximum */,
				0/* 4:step    */,
				0/* 5:flags   */
			};
			split_param(txt,param);
			addSlide(name,param[2],param[3],param[4],param[1]);
		}
		
		private void create_checkbox(String name,String txt){
			int param[]={
				0/* 0:default */,
				0/* 1:value   */,
				0/* 2:minimum */,
				0/* 3:maximum */,
				0/* 4:step    */,
				0/* 5:flags   */
			};
			split_param(txt,param);
			addBoxCheck(name,(param[1]!=0)?(true):(false));
		}
		
		private String create_combobox(String name,String txt,BufferedReader br){
			int param[]={
				0/* 0:default */,
				0/* 1:value   */,
				0/* 2:minimum */,
				0/* 3:maximum */,
				0/* 4:step    */,
				0/* 5:flags   */
			};
			split_param(txt,param);
			String[] lst = new String[param[3]-param[2]+1];
			for(int i=0; i<lst.length; i++){
				lst[i] = String.format("??? - %d",i);
			}			
			try {
				int idx = -1;
				while(idx<param[3]){
					txt = br.readLine().replace("\t","").trim();
					if(txt.matches("\\p{Digit}*:\\p{ASCII}*")==false){
						//TODO: how to feedback paragraph???						
						addBoxCombo(name,param[1],lst);
						return txt;//we have problems!!!
					}
					String[] itm = txt.split(":");
					idx = Integer.valueOf(itm[0].trim());
					lst[idx] = itm[1].trim();
				}
			}catch(NumberFormatException e){
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();				
			}			
			addBoxCombo(name,param[1],lst);
			return null;
		}
		
		private void split_param(String txt,int[] param){
			String[] lstTkn = txt.split(" ");
			for(String tkn:lstTkn){
				try{
					if(tkn.startsWith("default")==true){
						param[0] = Integer.valueOf(tkn.substring(8).trim());
					}else if(tkn.startsWith("value")==true){
						param[1] = Integer.valueOf(tkn.substring(6).trim());
					}else if(tkn.startsWith("min")==true){
						param[2] = Integer.valueOf(tkn.substring(4).trim());
					}else if(tkn.startsWith("max")==true){
						param[3] = Integer.valueOf(tkn.substring(4).trim());
					}else if(tkn.startsWith("step")==true){
						param[4] = Integer.valueOf(tkn.substring(5).trim());
					}else if(tkn.startsWith("flags")==true){
						//param[4] = Integer.valueOf(tkn.substring(5).trim());
					}
				}catch(NumberFormatException e){
					Misc.loge("fail to parse - %s",tkn);
					continue;
				}
			}
		}
		
		public void updateBox(){		
		}
		@Override
		public void slider2value(String name,int newValue) {
			String txt = Misc.syncExec(CMD_V4L2,arg1,"--set-ctrl="+name+"="+newValue);
			if(txt.length()!=0){
				Misc.loge(txt);
			}
		}
		@Override
		public void boxcheck2value(String name,boolean newValue) {
			int val = (newValue==true)?(1):(0);			
			String txt = Misc.syncExec(CMD_V4L2,arg1,"--set-ctrl="+name+"="+val);
			if(txt.length()!=0){
				Misc.loge(txt);
			}
		}
		@Override
		public void boxcombo2value(String name, int newValue,String newTitle) {
			String txt = Misc.syncExec(CMD_V4L2,arg1,"--set-ctrl="+name+"="+newValue);
			if(txt.length()!=0){
				Misc.loge(txt);
			}
		}
		@Override
		public void boxinteger2value(String name, int newValue) {
		}
		@Override
		public Parent rootLayout() {
			return null;//just layout a default panel~~
		}
	};
	
	@Override
	public Node getPanSetting() {
		return null;
	}
}
