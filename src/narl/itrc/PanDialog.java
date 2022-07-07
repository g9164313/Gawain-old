package narl.itrc;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.ButtonBar.ButtonData;

public abstract class PanDialog<T> extends Dialog<T> {


	protected void init(final Pane layout) {

		DialogPane pan = new DialogPane() {
			@Override
			protected Node createButton(ButtonType buttonType) {
				final JFXButton button = new JFXButton(buttonType.getText());				
				button.setPrefSize(48.*3., 48);				
				if(buttonType==ButtonType.OK) {
					button.getStyleClass().addAll("btn-raised-3","font-console");
				}else if(buttonType==ButtonType.CANCEL) {
					button.getStyleClass().addAll("btn-raised-1","font-console");
				}
				final ButtonData buttonData = buttonType.getButtonData();
				ButtonBar.setButtonData(button, buttonData);
		        button.setDefaultButton(buttonType != null && buttonData.isDefaultButton());
		        button.setCancelButton(buttonType != null && buttonData.isCancelButton());
		        button.addEventHandler(ActionEvent.ACTION, ae -> {
		            if (ae.isConsumed()) return;
		            if(set_result_and_close(buttonType)==true) {
		            	close();
		            }
		        });
				return button;
			}			
		};
		pan.getStylesheets().add(Gawain.sheet);
		pan.getButtonTypes().addAll(
			ButtonType.CANCEL,
			ButtonType.OK			
		);
		pan.setContent(layout);
		
		setDialogPane(pan);
	} 
		
	public interface EventOption<T> {
		void callback(T txt);
	}
	
	@SuppressWarnings("unchecked")
	public void popup(final EventOption<T> event) {
		Optional<T> opt = showAndWait();
		if(opt.isPresent()==false) {
			return;
		}
		Object obj = opt.get();
		if(obj instanceof ButtonType) {
			//if user has no result converter, dialog will send ButtonType!!!
			return;
		}
		event.callback((T)obj);
	}
	
	abstract boolean set_result_and_close(ButtonType type);
}
