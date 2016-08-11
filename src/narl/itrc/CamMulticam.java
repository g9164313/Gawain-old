package narl.itrc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.scene.Parent;

public class CamMulticam extends CamBundle {

	public CamMulticam(String nameConfig){
		super(nameConfig);		
		indxMcBoard = 0;//how to pack this in configure name
		File fs = null;
		if(Misc.isPOSIX()==true){
			final String dir = "/.euresys/multicamstudio/MultiCamStudio.settings";
			fs = new File("/home/qq"+dir);			
		}else{
			//where is configuration file??
		}
		if(fs.exists()==false){
			return;
		}
		parse_source(fs);
	}

	
	private int indxMcBoard = 0;	
	
	private int sizeX,sizeY,sizePitch;//update by native code
	
	private boolean staOK = false;//update by native code
	
	private native void implSetup(CamBundle cam,int[] id,String[] txt);
	private native long implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
		
	@Override
	public void setup() {
		staOK = false;//it will update by native code~~~
		int[] id = flatParamId();
		String[] txt = flatParamText();
		if(id==null||txt==null){
			return;
		}
		implSetup(this,id,txt);
	}

	@Override
	public void fetch() {
		if(staOK==false){
			return;
		}
		implFetch(this);
	}

	@Override
	public void close() { 
		implClose(this);
	}

	@Override
	public Parent genPanelSetting(PanBase pan) {
		return null;
	}	
	//----------------------------------------//
	
	class Param{
		public String name,text;
		public int id;
		Param(String _name,String _id,String _text){
			name = _name;
			text = _text;
			id = Integer.valueOf(_id);
		}
	}
	
	private ArrayList<Param> lstParam = new ArrayList<Param>();
	
	private int[] flatParamId(){
		if(lstParam.isEmpty()==true){
			return null;
		}
		int cnt = lstParam.size();
		int[] res = new int[cnt];
		for(int i=0; i<cnt; i++){
			res[i] = lstParam.get(i).id;
		}
		return res;
	}
	
	private String[] flatParamText(){
		if(lstParam.isEmpty()==true){
			return null;
		}
		int cnt = lstParam.size();
		String[] res = new String[cnt];
		for(int i=0; i<cnt; i++){
			res[i] = lstParam.get(i).text;
		}
		return res;
	}
	
	private void parse_source(File fs) {
		
		lstParam.clear();
		
		String srcName = txtConfig;
		
		 try {
			Document doc = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(fs);
			
			doc.getDocumentElement().normalize();
			
			NodeList lst = doc.getElementsByTagName("source");
			
			for(int i=0; i<lst.getLength(); i++){
				
				Node nd = lst.item(i);
				
				if(nd.getNodeType()==Node.ELEMENT_NODE){
					
					Element ee = (Element) nd;
					
					String txt = ee.getAttribute("name");
					
					if(txt.equalsIgnoreCase(srcName)==true){
						
						parse_param(ee.getElementsByTagName("config").item(0));
						
						parse_param(ee.getElementsByTagName("parameters").item(0));
						
						return;
					}
				}
			}
		} catch (SAXException e) {			
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	private void parse_param(Node root){

		NodeList lst = ((Element)root).getElementsByTagName("param");
		
		for(int i=0; i<lst.getLength(); i++){
			
			Node nd = lst.item(i);
						
			if(nd.getNodeType()==Node.ELEMENT_NODE){
				
				Element ee = (Element) nd;
				
				String name = ee.getAttribute("name");
				
				if(name.equalsIgnoreCase("BoardIdentifier")==true){
					//skip this parameter, it is useless~~~
					continue;
				}
				
				lstParam.add(new Param(
					name,
					ee.getAttribute("paramid"),
					ee.getTextContent()
				));				
			}
		}
	}
}
