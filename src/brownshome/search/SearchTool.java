package brownshome.search;

import javafx.application.Application;
import javafx.stage.Stage;

import brownshome.search.ui.GUIController;

public class SearchTool extends Application {
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		GUIController.initializeUI(primaryStage);
	}
}
