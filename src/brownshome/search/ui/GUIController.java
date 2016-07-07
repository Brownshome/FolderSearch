package brownshome.search.ui;

import java.io.IOException;
import java.nio.file.Path;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import brownshome.search.rule.Rule;
import brownshome.search.rule.RuleSet;

public class GUIController {
	public static GUIController INSTANCE;
	
	@FXML ListView<Rule> ruleList;
	@FXML ProgressBar progressBar;
	@FXML TableView<Result> resultTable;
	@FXML ComboBox<Class<? extends Rule>> ruleTypeBox;
	@FXML ComboBox<RuleSet> editRuleSet;
	@FXML Label progressLabel;
	@FXML ListView<Path> fileList;
	@FXML TextField regexBox;
	
	public static void initializeUI(Stage primaryStage) {
		INSTANCE = new GUIController(primaryStage);
	}
	
	public GUIController(Stage primaryStage) {
		Thread.currentThread().setUncaughtExceptionHandler((thread, exception) -> {
			errorImpl("Unhandled " + exception.getClass().getSimpleName() + " : " + exception.getMessage() + "\nSee the console for more details.");
			exception.printStackTrace();
		});
		
		primaryStage.setTitle("Search Tool 2.0");
		
		FXMLLoader loader = new FXMLLoader(GUIController.class.getResource("GUI.fxml"));
		loader.setController(this);
		
		try {
			primaryStage.setScene(new Scene(loader.load()));
		} catch (IOException e) {
			throw new RuntimeException("Error loading fxml document", e);
		}
		
		primaryStage.show();
	}
	
	void errorImpl(String message) {
		Alert error = new Alert(AlertType.ERROR, message);
		error.setHeaderText(null);
		error.show();
	}
	
	@FXML void addRule() {
		assert false : "Not implemented";
	}
	
	@FXML void rulesRuleSet(ActionEvent e) {
		
	}
	
	@FXML void addFolder() {
		
	}
	
	@FXML void removeFolder() {
		
	}
	
	@FXML void searchRuleSet(ActionEvent e) {
		
	}
	
	@FXML void editRuleSet() {
		
	}
	
	@FXML void search() {
		
	}
	
	@FXML void export() {
		
	}
}