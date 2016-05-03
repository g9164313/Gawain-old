package narl.itrc;

import javafx.scene.Parent;
import javafx.stage.WindowEvent;

abstract class BtnPopping extends BtnToggle {

	private PanBase pane = new PanBase(){
		@Override
		protected void eventShowing(WindowEvent e){
			//eventShowing(e);
		}
		@Override
		protected void eventShown(WindowEvent e){
			//eventShown(e);
		}
		@Override
		protected void eventClose(WindowEvent e){
			BtnPopping.this.change(false);
			//eventClose(e);
		}
		@Override
		public Parent layout() {
			return eventLayout();
		}
	};
	
	public BtnPopping(String title,String... name){
		super(name);
		pane.title = title;
	}
	
	protected void eventShowing(WindowEvent event){		
	}
	protected void eventShown(WindowEvent event){		
	}
	protected void eventClose(WindowEvent event){
	}
	abstract Parent eventLayout();

	@Override
	protected void eventStart() {
		pane.makeStage(getScene().getWindow());
		pane.appear();
	}

	@Override
	protected void eventFinal() {
		pane.dismiss();
	}
}
