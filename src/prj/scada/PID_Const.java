package prj.scada;


public class PID_Const {

	public static final int GRID_SIZE = 24;
	
	//The direction of pipe mean the flow output 
	
	public static final int CURSOR_SELECT = 0x0001;
	public static final int CURSOR_DELETE = 0x0002;
	
	public static final int Pipe1LF=0x1011;
	public static final int Pipe1RH=0x1012;
	public static final int Pipe1UP=0x1013;
	public static final int Pipe1DW=0x1014;		
	public static final int PipeL1a=0x1021;
	public static final int PipeL1b=0x1022;
	public static final int PipeL2a=0x1023;
	public static final int PipeL2b=0x1024;
	public static final int PipeL3a=0x1025;
	public static final int PipeL3b=0x1026;
	public static final int PipeL4a=0x1027;
	public static final int PipeL4b=0x1028;	
	public static final int PipeT1= 0x1031;
	public static final int PipeT2= 0x1032;
	public static final int PipeT3= 0x1033;
	public static final int PipeT4= 0x1034;	
	public static final int PipeXX= 0x1041;
	
	public static final int Wall1 = 0x2011;
	public static final int Wall2 = 0x2012;
	public static final int Wall3 = 0x2013;
	public static final int Wall4 = 0x2014;
	public static final int Wall5 = 0x2015;
	public static final int Wall6 = 0x2016;
	public static final int Wall7 = 0x2017;
	public static final int Wall8 = 0x2018;
	public static final int Join1 = 0x2021;
	public static final int Join2 = 0x2022;
	public static final int Join3 = 0x2023;
	public static final int Join4 = 0x2024;
	
	public static final int Gauge = 0x3000;
	public static final int Valve1= 0x3011;
	public static final int Valve2= 0x3012;		
	public static final int Pump  = 0x3020;
	public static final int Cryo  = 0x3021;
	public static final int Sputer= 0x3030;
	public static final int Strata= 0x3040;
	
	public static final int TYPE_INVALID = -1;
	
}

