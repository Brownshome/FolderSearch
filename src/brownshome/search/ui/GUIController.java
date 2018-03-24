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
import java.util.ListIterator;
import java.util.stream.Stream;

import brownshome.search.FileUtils;
import brownshome.search.rule.Rule;
import brownshome.search.rule.RuleSet;
import brownshome.search.rule.RuleSet.ResultSet;
import brownshome.search.rule.RuleType;
import brownshome.search.tree.SearchTree;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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

public class GUIController {
	public static final String VERSION = "2.6.2-re2j";
	
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
	@FXML CheckBox caseSensitive;
	@FXML Button removeButton;
	
	ObjectProperty<Path> tagList = new SimpleObjectProperty<>();
	BooleanBinding isSeachButtonValid;
	BooleanProperty isSearchDone = new SimpleBooleanProperty(false);
	BooleanProperty currentlySearching = new SimpleBooleanProperty(false);
	BooleanProperty treeBeingMade = new SimpleBooleanProperty(false);
	BooleanBinding buttonIsCancel;
	
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
		
		primaryStage.setTitle("Search Tool " + VERSION);
		
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
		removeButton.disableProperty().bind(fileList.getSelectionModel().selectedItemProperty().isNull());

		buttonIsCancel = currentlySearching.or(treeBeingMade);
		StringExpression searchButtonText = Bindings.createStringBinding(
				() -> buttonIsCancel.get() ? "Cancel" : "Search", 
				buttonIsCancel
			);
		
		searchButton.disableProperty().bind(isSeachButtonValid.not().and(currentlySearching.not()).and(treeBeingMade.not()));
		searchButton.textProperty().bind(searchButtonText);
		
		EventHandler<ActionEvent> search = ae -> search();
		EventHandler<ActionEvent> cancel = ae -> cancel();
		
		searchButton.onActionProperty().bind(Bindings.createObjectBinding(() -> buttonIsCancel.get() ? cancel : search, buttonIsCancel));
		
		exportButton.disableProperty().bind(isSearchDone.not());
		selectSearch.disableProperty().bind(currentlySearching);
		selectRuleSet.disableProperty().bind(currentlySearching);
		
		caseSensitive.disableProperty().bind(currentlySearching.or(treeBeingMade).or(tagListSelected.not()));
		caseSensitive.selectedProperty().addListener((obs, old, isCaseSensitive) -> {
			if(isCaseSensitive) {
				if(SearchTree.caseSensitiveRootTree == null) {
					createTree();
				}
			} else {
				if(SearchTree.caseInsensitiveRootTree == null) {
					createTree();
				}
			}
		});
		
		primaryStage.show();
		
		Platform.runLater(this::refreshRuleSet);
	}
	
	public void errorImpl(String message) {
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
	
	Path initialDir;
	@FXML void addFolder() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		
		if(initialDir != null)
			dirChooser.setInitialDirectory(initialDir.toFile());
		
		dirChooser.setTitle("Choose a folder to add");
		File folder = dirChooser.showDialog(primaryStage);
		
		if(folder == null)
			return;
		
		initialDir = folder.toPath().getParent();
		if(initialDir == null)
			initialDir = folder.toPath();
		
		fileList.getItems().add(folder.toPath());
	}
	
	@FXML void displayHelp() {
		ButtonType wikiButton = new ButtonType("Visit Wiki");
		Alert alert = new Alert(AlertType.INFORMATION, "A folder search tool made by James Brown.\nFor information on usage please visit the wiki.", ButtonType.CLOSE, wikiButton);
		alert.setTitle("About Folder Search " + VERSION);
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
	
	Path initialDirSearchList;
	Thread treeCreationThread;
	@FXML void selectSearchList() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select the file to search with");
		
		if(initialDirSearchList != null)
			fileChooser.setInitialDirectory(initialDirSearchList.toFile());
		
		File p = fileChooser.showOpenDialog(primaryStage);
		if(p == null)
			return;
		
		initialDirSearchList = p.toPath().getParent();
		if(initialDirSearchList == null) 
			initialDirSearchList = p.toPath();
		
		tagList.setValue(p.toPath());
	
		SearchTree.caseInsensitiveRootTree = null;
		SearchTree.caseSensitiveRootTree = null;
		
		createTree();
	}

	private void createTree() {
		progressBar.setDisable(false);
		progressBar.setProgress(-1);
		
		resultTable.getItems().clear();
		isSearchDone.set(false);
		
		progressLabel.setText("Generating Search Tree");
		selectSearch.setText(tagList.get().toString());
		treeBeingMade.set(true);
		
		boolean caseFlag = caseSensitive.isSelected();
		treeCreationThread = new Thread(() -> {
			try {
				List<String> searchTerms = FileUtils.readAllLines(tagList.get());
				
				System.out.println("Started to create tree, case: " + caseFlag);
				if(!caseFlag) {
					for(ListIterator<String> it = searchTerms.listIterator(); it.hasNext(); ) {
						String term = it.next();
						it.set(term.toLowerCase());
					}
				}
				
				SearchTree tree = new SearchTree(searchTerms);
				
				Thread t = Thread.currentThread();
				Platform.runLater(() -> finishedMakingTree(t, tree));
			} catch (IOException e) {
				throw new RuntimeException("Error reading tag list", e);
			}
		}, "TREE THREAD");
		
		treeCreationThread.setDaemon(true);
		treeCreationThread.start();
	}
	
	void finishedMakingTree(Thread t, SearchTree tree) {
		if(t != treeCreationThread) {
			return; //we aborted, just die
		}
		
		boolean caseFlag = caseSensitive.isSelected();
		System.out.println("Finished creating tree, case: " + caseFlag);
		if(caseFlag) {
			SearchTree.caseSensitiveRootTree = tree;
		} else {
			SearchTree.caseInsensitiveRootTree = tree;
		}
		
		progressBar.setDisable(true);
		progressBar.setProgress(0);
		progressLabel.setText("Idle");
		treeBeingMade.set(false);
	}
	
	@FXML void editRuleSet() {
		
	}
	
	void cancel() {
		if(currentlySearching.get()) {
			searchThread.interrupt();
			searchThread = null;
			finishSearch(null, null);
		}
		
		if(treeBeingMade.get()) {
			treeCreationThread.interrupt();
			treeCreationThread = null;
			finishedMakingTree(null, null);
			tagList.setValue(null);
			selectSearch.setText("Select Search List");
		}
	}
	
	Thread searchThread;
	void search() {
		RuleSet ruleSet = selectRuleSet.getSelectionModel().getSelectedItem();
		Collection<Path> paths = fileList.getItems();
		
		progressBar.setDisable(false);
		progressBar.setProgress(0);
		progressLabel.setText("Searching...");
		isSearchDone.set(false);
		currentlySearching.set(true);
		
		boolean caseFlag = caseSensitive.isSelected();
		
		searchThread = new Thread(() -> {
			List<ResultSet> results = ruleSet.searchPaths(paths, caseFlag);
		
			Thread t = Thread.currentThread();
			Platform.runLater(() -> {
				finishSearch(t, results);
			});
		}, "SEARCH THREAD");
		
		searchThread.setDaemon(true);
		searchThread.start();
	}
	
	void finishSearch(Thread t, List<ResultSet> results) {
		if(t != searchThread)
			return;
		
		if(results != null) resultTable.getItems().setAll(results);
		
		progressBar.setDisable(true);
		progressBar.setProgress(0);
		progressLabel.setText("Idle");
		isSearchDone.set(true);
		currentlySearching.set(false);
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
	
	Path initialExportDir;
	@FXML void export() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select the file to export to");
		fileChooser.getExtensionFilters().setAll(new ExtensionFilter("Csv", "*.csv"));
		
		if(initialExportDir != null)
			fileChooser.setInitialDirectory(initialExportDir.toFile());
		
		Path output = fileChooser.showSaveDialog(primaryStage).toPath();
		
		if(output == null)
			return;
		
		initialExportDir = output.getParent();
		if(initialExportDir == null) initialExportDir = output;
		
		List<String> catagories = selectRuleSet.getSelectionModel().getSelectedItem().catagories;
		String header = "";	
		for(TableColumn<ResultSet, ?> c : resultTable.getColumns()) {
			header += "\"" + catagories.get(Integer.parseInt(c.getId())).replace("\"", "\"\"") + "\",";
		}
		header = header.substring(0, header.length() - 1);
		
		Iterable<String> iterable = Stream.concat(
			Stream.of(header), 
				
			resultTable.getItems().stream().map(r -> {
				String s = "";
			
				for(TableColumn<ResultSet, ?> c : resultTable.getColumns()) {
					s += "\"" + r.data[Integer.parseInt(c.getId())].replace("\"", "\"\"") + "\",";
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