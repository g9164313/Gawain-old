package prj.letterpress;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import eu.hansolo.enzo.flippanel.FlipEvent;
import eu.hansolo.enzo.flippanel.FlipPanel;

public class PanTest extends FlipPanel {

	private StackPane frontPanel;
	
	public PanTest(){
		super(Orientation.VERTICAL);
		initLayout();		
	}
	
	public void initLayout() {
		
        frontPanel = new StackPane();
        frontPanel.setBackground(new Background(
        	new BackgroundFill(
        		Color.AQUAMARINE, 
        		CornerRadii.EMPTY, 
        		Insets.EMPTY
        	)
        ));
        
       getFront().getChildren().add(initFront(this, frontPanel));
       getBack().getChildren().add(initBack(this, frontPanel));

       addEventHandler(FlipEvent.FLIP_TO_FRONT_FINISHED, event ->{ System.out.println("Flip to front finished"); });
       addEventHandler(FlipEvent.FLIP_TO_BACK_FINISHED, event -> System.out.println("Flip to back finished"));
    }
	
	private Pane initFront(final FlipPanel flipPanel, final StackPane FRONT_PANEL) {                
        //Region settingsButton = new Region();
        //settingsButton.getStyleClass().add("settings-button");
        //settingsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, EVENT -> FLIP_PANEL.flipToBack());
		
		Button settingsButton = new Button("**");
		settingsButton.setOnAction(EVENT -> flipPanel.flipToBack());
		
        VBox componentsFront = new VBox(settingsButton, FRONT_PANEL);
        componentsFront.setSpacing(10);
        VBox.setVgrow(FRONT_PANEL, Priority.ALWAYS);

        /*StackPane front = new StackPane();
        front.setPadding(new Insets(20, 20, 20, 20));
        front.getStyleClass().add("panel");
        front.getChildren().addAll(componentsFront);
        return front;*/
        return componentsFront;
    }
	
    private Pane initBack(final FlipPanel flipPanel, final StackPane FRONT_PANEL) {
    	
        //Region backButton = new Region();
        //backButton.getStyleClass().add("back-button");
       // backButton.addEventHandler(MouseEvent.MOUSE_CLICKED, EVENT -> flipPanel.flipToFront());

        Button backButton = new Button("**");
        backButton.setOnAction(EVENT -> flipPanel.flipToFront());
		
        ToggleGroup group = new ToggleGroup();

        final RadioButton standardGreen = new RadioButton("Green");
        standardGreen.setToggleGroup(group);
        standardGreen.setSelected(true);
        standardGreen.setOnAction(event -> FRONT_PANEL.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY))));

        final RadioButton amber         = new RadioButton("Red");
        amber.setToggleGroup(group);
        amber.setOnAction(event -> FRONT_PANEL.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY))));

        final RadioButton blueDarkBlue  = new RadioButton("Blue");
        blueDarkBlue.setToggleGroup(group);
        blueDarkBlue.setOnAction(event -> FRONT_PANEL.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY))));

        VBox componentsBack = new VBox(backButton, standardGreen, amber, blueDarkBlue);
        componentsBack.setSpacing(10);

        StackPane back = new StackPane();
        back.setPadding(new Insets(20, 20, 20, 20));
        back.getStyleClass().add("panel");
        back.getChildren().addAll(componentsBack);
        return back;
    }
}
