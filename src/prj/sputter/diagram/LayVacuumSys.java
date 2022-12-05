package prj.sputter.diagram;

import javafx.scene.layout.GridPane;

public class LayVacuumSys extends GridPane {

	private static final String s_txt = 
		"-fx-padding: 27px";
	
	public final ValveSrc[] mfc = new ValveSrc[8];//mass flow control
	
	public final BrickDst[] pump = new BrickDst[4];
		
	public final Valve[] valve = new Valve[16];
	
	public Brick chamber = null;
	
	public LayVacuumSys LayoutForm1() {
		
		setStyle(s_txt);
		
		mfc[0] = new ValveSrc();
		mfc[1] = new ValveSrc();
		mfc[2] = new ValveSrc();
				
		valve[0] = new Valve(PixDir.HORI);
		valve[1] = new Valve(PixDir.VERT);
		
		Pipe p0 = new Pipe(PixDir.TP_LF).connect(valve[1].flowIn);
		
		chamber = new Brick();//chamber
		pump[0] = new BrickDst();//machine pump
		pump[1] = new BrickDst().connect(valve[0].flowOut, valve[1].flowOut);//turbo pump

		add(mfc[0], 0, 0);
		add(mfc[1], 0, 1);
		add(mfc[2], 0, 2);
		
		add(chamber, 1, 0, 1, 3);
		add(pump[0], 2, 2);
		add(pump[1], 3, 0);
		
		add(valve[0], 2, 0);
		add(valve[1], 3, 1);
		add(p0, 3, 2);

		/*mfc[0].createInfo()
		.insert("PV", "0.000 psi")
		.insert("SV", "0.000 psi");
		mfc[1].createInfo()
		.insert("PV", "0.000 psi")
		.insert("SV", "0.000 psi");
		chamber.info
		.insert("氣壓-1", "0.000 psi")
		.insert("氣壓-2", "0.000 psi")
		.insert("氣壓-3", "0.000 psi");*/
		
		//setGridLinesVisible(true);
		return this;
	}	
}
