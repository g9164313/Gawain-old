package prj.scada;

import java.util.Hashtable;

import javafx.scene.image.Image;
import narl.itrc.Misc;

public class PID_Const {

	public static final int GRID_SIZE = 24;
	
	//The direction of pipe mean the flow output 
	
	public static final int CURSOR_SELECT = 0x0001;
	public static final int CURSOR_DELETE = 0x0002;
	
	public static final int Valve = 0x1000;
	
	public static final int Pipe1LF= 0x1011;
	public static final int Pipe1RH= 0x1012;
	public static final int Pipe1UP= 0x1013;
	public static final int Pipe1DW= 0x1014;
		
	public static final int PipeL1a= 0x1021;
	public static final int PipeL1b= 0x1022;
	public static final int PipeL2a= 0x1023;
	public static final int PipeL2b= 0x1024;
	public static final int PipeL3a= 0x1025;
	public static final int PipeL3b= 0x1026;
	public static final int PipeL4a= 0x1027;
	public static final int PipeL4b= 0x1028;
	
	public static final int PipeT1= 0x1031;
	public static final int PipeT2= 0x1032;
	public static final int PipeT3= 0x1033;
	public static final int PipeT4= 0x1034;
	
	public static final int PipeXX= 0x1041;
	
	public static final int WALL_1 = 0x1051;
	public static final int WALL_2 = 0x1052;
	public static final int WALL_3 = 0x1053;
	public static final int WALL_4 = 0x1054;
	public static final int WALL_5 = 0x1055;
	public static final int WALL_6 = 0x1056;
	public static final int WALL_7 = 0x1057;
	public static final int WALL_8 = 0x1058;
	
	public static final int TYPE_INVALID = -1;
	
}

