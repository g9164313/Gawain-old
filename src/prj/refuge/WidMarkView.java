package prj.refuge;

import javafx.scene.layout.StackPane;
import narl.itrc.WidTextSheet;

public class WidMarkView extends StackPane {
		
	public WidTextSheet sheet = new WidTextSheet();
	
	public WidMarkView(){
		sheet.setTitle(
			"測量後劑量 (μSv/hr)",
			"一年後劑量 (μSv/hr)",
			"距離 (cm)",
			"新距離 (cm)",
			"量測次數 (n)",
			"平均劑量 (μSv/min)",
			"Sigma",
			"%Sigma"
		);
		getChildren().add(sheet);
	}
}
