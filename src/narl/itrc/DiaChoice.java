package narl.itrc;

import com.sun.javafx.scene.control.skin.resources.ControlResources;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class DiaChoice<T> extends Dialog<T> {

	private final GridPane grid;
	private final Label label;
	public final ComboBox<T> comboBox;

	public DiaChoice() {

		final DialogPane dialogPane = getDialogPane();

		// -- grid
		this.grid = new GridPane();
		this.grid.setHgap(10);
		this.grid.setMaxWidth(Double.MAX_VALUE);
		this.grid.setAlignment(Pos.CENTER_LEFT);

		// -- label
		label = new Label(dialogPane.getContentText());
		label.setMaxWidth(Double.MAX_VALUE);
		label.setMaxHeight(Double.MAX_VALUE);
		label.getStyleClass().add("content");
		label.setWrapText(true);
		label.setPrefWidth(360);

		label.setPrefWidth(Region.USE_COMPUTED_SIZE);
		label.textProperty().bind(dialogPane.contentTextProperty());

		dialogPane.contentTextProperty().addListener(o -> updateGrid());

		setTitle(ControlResources.getString("Dialog.confirm.title"));
		dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
		dialogPane.getStyleClass().add("choice-dialog");
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		final double MIN_WIDTH = 150;

		comboBox = new ComboBox<T>();
		comboBox.setMinWidth(MIN_WIDTH);
		comboBox.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(comboBox, Priority.ALWAYS);
		GridPane.setFillWidth(comboBox, true);

		updateGrid();
		
        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? getSelectedItem() : null;
        });
	}

	private void updateGrid() {

		grid.getChildren().clear();

		grid.add(label, 0, 0);
		grid.add(comboBox, 1, 0);
		getDialogPane().setContent(grid);

		Platform.runLater(() -> comboBox.requestFocus());
	}

	/**************************************************************************
	 *
	 * Public API
	 *
	 **************************************************************************/

	/**
	 * Returns the currently selected item in the dialog.
	 */
	public final T getSelectedItem() {
		return comboBox.getSelectionModel().getSelectedItem();
	}

	/**
	 * Returns the property representing the currently selected item in the
	 * dialog.
	 */
	public final ReadOnlyObjectProperty<T> selectedItemProperty() {
		return comboBox.getSelectionModel().selectedItemProperty();
	}

	/**
	 * Sets the currently selected item in the dialog.
	 * 
	 * @param item
	 *            The item to select in the dialog.
	 */
	public final void setSelectedItem(T item) {
		comboBox.getSelectionModel().select(item);
	}

	/**
	 * Returns the list of all items that will be displayed to users. This list
	 * can be modified by the developer to add, remove, or reorder the items to
	 * present to the user.
	 */
	public final ObservableList<T> getItems() {
		return comboBox.getItems();
	}
}
