package brownshome.search.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
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
	@FXML Button searchButton;
	@FXML Button exportButton;
	
	ObjectProperty<Path> tagList = new SimpleObjectProperty<>();
	BooleanBinding isSeachButtonValid;
	BooleanProperty isSearchDone = new SimpleBooleanProperty(false);
	BooleanProperty currentlySearching = new SimpleBooleanProperty(false);
	BooleanProperty treeBeingMade = new SimpleBooleanProperty(false);
	
	public static void initializeUI(Stage primaryStage) {
		INSTANCE = new GUIController(primaryStage);
	}
	
	public GUIController(Stage primaryStage) {
		this.primaryStage = primaryStage;
		
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			Throwable root = exception;
			while(root.getClass() != RuntimeException.class && root.getCause() != null) root = root.getCause();
			
			errorImpl("Unhandled " + root.getClass().getSimpleName() + " : " + root.getMessage() + "\nSee the console for more details.");
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
		
		primaryStage.getIcons().add(new Image(GUIController.class.getResourceAsStream("icon.png")));
		
		editRuleSet.setItems(sharedRuleList);
		selectRuleSet.setItems(sharedRuleList);
		selectRuleSet.getSelectionModel().selectedItemProperty().addListener(this::updateTable);
		fileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		BooleanExpression ruleSetSelected = selectRuleSet.getSelectionModel().selectedItemProperty().isNotNull();
		BooleanExpression listHasFilesInIt = new SimpleListProperty<Path>(fileList.getItems()).emptyProperty().not();
		BooleanExpression tagListSelected = tagList.isNotNull();
		
		isSeachButtonValid = ruleSetSelected.and(listHasFilesInIt.and(tagListSelected));
		searchButton.disableProperty().bind(isSeachButtonValid.not().or(currentlySearching).or(treeBeingMade));
		exportButton.disableProperty().bind(isSearchDone.not());
		selectSearch.disableProperty().bind(currentlySearching);
		selectRuleSet.disableProperty().bind(currentlySearching);
		
		primaryStage.show();
		
		Platform.runLater(this::refreshRuleSet);
	}
	
	void errorImpl(String message) {
		if(!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> errorImpl(message));
			return;
		}
		
		Alert error = new Alert(AlertType.ERROR, message);
		error.initOwner(primaryStage);
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
		
		if(folder == null)
			return;
		
		fileList.getItems().add(folder.toPath());
	}
	
	@FXML void displayHelp() {
		ButtonType wikiButton = new ButtonType("Visit Wiki");
		Alert alert = new Alert(AlertType.INFORMATION, "A folder search tool made by James Brown.\nFor information on usage please visit the wiki.", ButtonType.CLOSE, wikiButton);
		alert.setTitle("About Folder Search 2.0");
		alert.setHeaderText(null);
		alert.showAndWait();
		
		if(alert.getResult() == wikiButton) {
			try {
				Desktop.getDesktop().browse(URI.create("https://github.com/Brownshome/FolderSearch/wiki"));
			} catch (IOException e) {
				throw new RuntimeException("Unable to open browser to https://github.com/Brownshome/FolderSearch/wiki", e);
			}
		}
	}
	
	@FXML void removeFolder() {
		ObservableList<Path> list = fileList.getSelectionModel().getSelectedItems();
		fileList.getItems().removeAll(list);
	}
	
	@FXML void refreshRuleSet() {
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
		fileChooser.setTitle("Select the file to search with");
		
		File p = fileChooser.showOpenDialog(primaryStage);
		if(p == null)
			return;
		
		tagList.setValue(p.toPath());
	
		progressBar.setDisable(false);
		progressBar.setProgress(-1);
		
		resultTable.getItems().clear();
		isSearchDone.set(false);
		
		progressLabel.setText("Generating Search Tree");
		selectSearch.setText(tagList.get().toString());
		treeBeingMade.set(true);
		
		Thread treeCreationThread = new Thread(() -> {
			try {
				SearchTree.createTree(Files.readAllLines(tagList.get()));
				
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
		treeBeingMade.set(false);
	}
	
	@FXML void editRuleSet() {
		
	}
	
	@FXML void search() {
		RuleSet ruleSet = selectRuleSet.getSelectionModel().getSelectedItem();
		Collection<Path> paths = fileList.getItems();
		
		progressBar.setDisable(false);
		progressBar.setProgress(0);
		progressLabel.setText("Searching...");
		isSearchDone.set(false);
		currentlySearching.set(true);
		
		Thread thread = new Thread(() -> {
			List<ResultSet> results = ruleSet.searchPaths(paths);
			
			Platform.runLater(() -> {
				finishSearch(results);
				progressBar.setDisable(true);
				progressBar.setProgress(0);
				progressLabel.setText("Idle");
				isSearchDone.set(true);
				currentlySearching.set(false);
			});
		}, "SEARCH THREAD");
		
		thread.setDaemon(true);
		thread.start();
	}
	
	void finishSearch(List<ResultSet> results) {
		resultTable.getItems().setAll(results);
	}

	void updateTable(ObservableValue<? extends RuleSet> observable, RuleSet old, RuleSet current) {
		resultTable.getItems().clear();
		isSearchDone.set(false);
		
		RuleSet currentSet = selectRuleSet.getSelectionModel().getSelectedItem();
		
		if(currentSet == null)
			resultTable.getItems().clear();
		else
			resultTable.getColumns().setAll(currentSet.getColumns());
	}
	
	@FXML void export() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select the file to export to");
		fileChooser.getExtensionFilters().setAll(new ExtensionFilter("Csv", "*.csv"));
		Path output = fileChooser.showSaveDialog(primaryStage).toPath();
		
		if(output == null)
			return;
		
		List<String> catagories = selectRuleSet.getSelectionModel().getSelectedItem().catagories;
		String header = "";	
		for(TableColumn<ResultSet, ?> c : resultTable.getColumns()) {
			header += catagories.get(Integer.parseInt(c.getId())) + ",";
		}
		header = header.substring(0, header.length() - 1);
		
		Iterable<String> iterable = Stream.concat(
			Stream.of(header), 
				
			resultTable.getItems().stream().map(r -> {
				String s = "";
			
				for(TableColumn<ResultSet, ?> c : resultTable.getColumns()) {
					s += r.data[Integer.parseInt(c.getId())] + ",";
				}
			
				return s.substring(0, s.length() - 1);
			})
		)::iterator;
		
		try {
			Files.write(output, iterable, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write to file");
		}
	}

	public void setProgress(double p) {
		if(!progressBar.isDisabled()) {
			progressBar.setProgress(p);
		}
	}
}