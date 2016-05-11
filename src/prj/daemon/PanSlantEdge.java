package prj.daemon;

import narl.itrc.CamBundle;
import narl.itrc.ImgRender;
import narl.itrc.Misc;

public class PanSlantEdge extends PanCamera implements 
	ImgRender.Filter
{
	public PanSlantEdge(){
		imgCtrl.addAction(this,1).setText("分析SFR");
	}

	@Override
	public void initData() {
	}
	@Override
	public void procData(CamBundle bnd, long ptrMat0, long patMat1) {
	}
	@Override
	public void markData(CamBundle bnd, long ptrMat1) {
	}
	@Override
	public void showData(CamBundle bnd, int count) {
	}
}
