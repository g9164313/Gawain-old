package narl.itrc.vision;

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

import narl.itrc.Gawain;
import narl.itrc.Misc;

public class CamMulticam extends CamBundle {

	public CamMulticam(String nameConfig){
		//super(nameConfig);		
		indxMcBoard = 0;//how to pack this in configure name
		String path = "";
		if(Gawain.isPOSIX==true){
			final String dir = "/.euresys/multicamstudio/MultiCamStudio.settings";
			path = "/home/qq"+dir;//how to get home directory???		
		}else{
			//where is configuration file??
		}
		parseSetting(this,path,nameConfig);
	}

	
	private int indxMcBoard = 0;	
	
	private int sizeX,sizeY,sizePitch;//update by native code
	
	private boolean staOK = false;//update by native code
	
	private native void implSetup(
		CamBundle cam,
		String topology,
		String connect,
		String camFile,
		int[] parmId,
		String[] parmTxt
	);
	private native long implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
		
	@Override
	public void setup() {
		staOK = false;//it will update by native code~~~
		if(confCamFile.length()==0){
			//fail to parse setting file, it is an important file.
			return;
		}
		implSetup(
			this,
			confTopology,
			confConnect,
			confCamFile,
			flatParamId(),
			flatParamText()
		);
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
	//----------------------------------------//
	
	private String confTopology= "";
	private String confConnect = "";
	private String confCamFile = "";
	
	static class Parm{
		public String name,text;
		public int id;
		Parm(String _name,String _id,String _text){
			name = _name;
			text = _text;
			id = Integer.valueOf(_id);
		}
	}
	
	private ArrayList<Parm> lstParm = new ArrayList<Parm>();
	
	private int[] flatParamId(){
		if(lstParm.isEmpty()==true){
			return null;
		}
		int cnt = lstParm.size();
		int[] res = new int[cnt];
		for(int i=0; i<cnt; i++){
			res[i] = lstParm.get(i).id;
		}
		return res;
	}
	
	private String[] flatParamText(){
		if(lstParm.isEmpty()==true){
			return null;
		}
		int cnt = lstParm.size();
		String[] res = new String[cnt];
		for(int i=0; i<cnt; i++){
			res[i] = lstParm.get(i).text;
		}
		return res;
	}
	
	/**
	 * Just override parseSetting().<p>
	 * @param cam - instance
	 * @param fs - XML file location
	 * @param name - source name
	 */
	public static void parseSetting(CamMulticam cam,String fs,String name) {
		parseSetting(cam, new File(fs), name);
	}
	
	/**
	 * This will parse MultiCamStudio setting file.It is XML style document.<p>
	 * This file is usually located in home directory. 
	 * @param cam - instance
	 * @param fs - XML file
	 * @param name - source name
	 */
	public static void parseSetting(CamMulticam cam,File fs,String name) {
				
		cam.confTopology= "";
		cam.confConnect = "";
		cam.confCamFile = "";
		cam.lstParm.clear();
		
		if(fs.exists()==false){
			return;
		}
		
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
					
					if(txt.equalsIgnoreCase(name)==true){
						
						parse_conf(cam,ee.getElementsByTagName("config").item(0));
						
						parse_parm(cam,ee.getElementsByTagName("parameters").item(0));
						
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
	
	private static void parse_conf(CamMulticam cam,Node root){

		NodeList lst = ((Element)root).getElementsByTagName("param");
		
		for(int i=0; i<lst.getLength(); i++){
			
			Node nd = lst.item(i);
						
			if(nd.getNodeType()==Node.ELEMENT_NODE){
				
				Element ee = (Element) nd;
				
				String name = ee.getAttribute("name");

				if(name.equalsIgnoreCase("CamFile")==true){
					
					cam.confCamFile = ee.getTextContent();
					
				}else if(name.equalsIgnoreCase("Connector")==true){
				
					cam.confConnect = ee.getTextContent();
					
				}else if(name.equalsIgnoreCase("BoardTopology")==true){
					
					cam.confTopology= ee.getTextContent();
					
				}else{
					Misc.logw("no support tag: ", name);
				}
			}
		}
	}
	
	private static void parse_parm(CamMulticam cam,Node root){

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
				
				cam.lstParm.add(new Parm(
					name,
					ee.getAttribute("paramid"),
					ee.getTextContent()
				));				
			}
		}
	}
}
