package brownshome.search.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import brownshome.search.rule.Rule;
import brownshome.search.rule.RuleSet;
import brownshome.search.rule.RuleSet.ResultSet;
import brownshome.search.rule.RuleType;
import brownshome.search.tree.SearchTree;

public class GUIController {
	public static GUIController INSTANCE;
	
	final static Path RULE_PATH = Paths.get("rules");
	
	Stage primaryStage;
	ObservableList<RuleSet> sharedRuleList = FXCollections.observableArrayList();
	
	@FXML ListView<Rule> ruleList;
	@FXML ComboBox<RuleSet> selectRuleSet;
	@FXML ProgressBar progressBar;
	@FXML TableView<ResultSet> resultTable;
	@FXML ComboBox<RuleType> ruleTypeBox;
	@FXML ComboBox<RuleSet> editRuleSet;
	@FXML Label progressLabel;
	@FXML ListView<Path> fileList;
	@FXML TextField regexBox;
	@FXML Button selectSearch;
	
	Path tagList;
	
	public static void initializeUI(Stage primaryStage) {
		INSTANCE = new GUIController(primaryStage);
	}
	
	public GUIController(Stage primaryStage) {
		this.primaryStage = primaryStage;
		
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
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
		
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if(event.getCode() == KeyCode.F5) 
				refreshRuleSet();
		});
		
		editRuleSet.setItems(sharedRuleList);
		selectRuleSet.setItems(sharedRuleList);
		selectRuleSet.getSelectionModel().selectedItemProperty().addListener(this::updateTable);
		fileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		refreshRuleSet();
		
		primaryStage.show();
	}
	
	void errorImpl(String message) {
		if(!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> errorImpl(message));
			return;
		}
		
		Alert error = new Alert(AlertType.ERROR, message);
		error.setHeaderText(null);
		error.show();
	}
	
	@FXML void addRule() {
		assert false : "Not implemented";
	}
	
	@FXML void addFolder() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Choose a folder to add");
		File folder = dirChooser.showDialog(primaryStage);
		fileList.getItems().add(folder.toPath());
	}
	
	@FXML void removeFolder() {
		ObservableList<Path> list = fileList.getSelectionModel().getSelectedItems();
		fileList.getItems().removeAll(list);
	}
	
	void refreshRuleSet() {
		if(!Files.isDirectory(RULE_PATH)) {
			try {
				Files.createDirectory(RULE_PATH);
			} catch (IOException e) {
				throw new RuntimeException("Rule directory not found and unable to create.", e);
			}
		}
		
		try(Stream<Path> stream = Files.list(RULE_PATH)) {
			sharedRuleList.clear();
			stream.filter(p -> p.getFileName().toString().endsWith(".search")).map(RuleSet::new).forEachOrdered(sharedRuleList::add);
		} catch (IOException e) {
			throw new RuntimeException("Error reading searches", e);
		}
	}
	
	@FXML void selectSearchList() {
		FileChooser fileChooser = new FileChooser();
		tagList = fileChooser.showOpenDialog(primaryStage).toPath();
	
		progressBar.setDisable(false);
		progressBar.setProgress(-1);
		progressLabel.setText("Generating Search Tree");
		selectSearch.setText(tagList.toString());
		
		Thread treeCreationThread = new Thread(() -> {
			try {
				SearchTree.createTree(Files.readAllLines(tagList));
				
				Platform.runLater(GUIController.INSTANCE::finishedMakingTree);
			} catch (IOException e) {
				throw new RuntimeException("Error reading tag list", e);
			}
		}, "TREE THREAD");
		
		treeCreationThread.setDaemon(true);
		treeCreationThread.start();
	}
	
	void finishedMakingTree() {
		progressBar.setDisable(true);
		progressBar.setProgress(0);
		progressLabel.setText("Idle");
	}
	
	@FXML void editRuleSet() {
		
	}
	
	@FXML void search() {
		RuleSet ruleSet = selectRuleSet.getSelectionModel().getSelectedItem();
		Collection<Path> paths = fileList.getItems();
		
		progressBar.setDisable(false);
		progressBar.setProgress(0);
		progressLabel.setText("Searching...");
		
		Thread thread = new Thread(() -> {
			List<ResultSet> results = ruleSet.searchPaths(paths);
			
			Platform.runLater(() -> {
				finishSearch(results);
				progressBar.setDisable(true);
				progressBar.setProgress(0);
				progressLabel.setText("Idle");
			});
		}, "SEARCH THREAD");
		
		thread.setDaemon(true);
		thread.start();
	}
	
	void finishSearch(List<ResultSet> results) {
		resultTable.getItems().setAll(results);
	}

	void updateTable(ObservableValue<? extends RuleSet> observable, RuleSet old, RuleSet current) {
		RuleSet currentSet = selectRuleSet.getSelectionModel().getSelectedItem();
		
		resultTable.getColumns().setAll(currentSet.getColumns());
	}
	
	@FXML void export() {
		
	}

	public void setProgress(double p) {
		if(!progressBar.isDisabled()) {
			progressBar.setProgress(p);
		}
	}
}