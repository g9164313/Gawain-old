package narl.itrc;

import javafx.scene.Parent;
import javafx.stage.WindowEvent;

abstract class BtnPopping extends BtnToggle {

	private PanBase pane = new PanBase(){
		@Override
		protected void eventShowing(WindowEvent e){
			BtnPopping.this.eventShowing(e);
		}
		@Override
		protected void eventShown(WindowEvent e){
			BtnPopping.this.eventShown(e);
		}
		@Override
		protected void eventClose(WindowEvent e){
			BtnPopping.this.change(false);//reflection~~~
			BtnPopping.this.eventClose(e);
		}
		@Override
		public Parent layout() {
			return BtnPopping.this.eventLayout();
		}
	};
	
	public BtnPopping(String title,String... name){
		super(name);
		pane.title = title;
	}
	
	public void dismiss(){
		pane.dismiss();
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
