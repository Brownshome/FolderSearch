package brownshome.search.rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import brownshome.search.FileUtils;
import brownshome.search.SearchState;
import brownshome.search.ui.GUIController;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

// FILE - always on
// LINE - only on if file is active
// OTHER - only on if file and line is active

public class RuleSet {
	public List<String> catagories;

	List<Rule> rules = new ArrayList<>();

	List<GroupTag> groups = new ArrayList<>();
	String name;

	@Override
	public String toString() {
		return name;
	}

	public RuleSet(Path path) {
		name = path.getFileName().toString();

		List<String> lines;
		try {
			lines = FileUtils.readAllLines(path);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read rule file " + path, e);
		}

		catagories = new ArrayList<>();
		catagories.add("File");
		catagories.add("Line No");
		catagories.add("Match");

		for(int i = 0; i < lines.size(); i++) {
			RuleType type = null;
			List<String> subList = null;
			try {
				type = RuleType.valueOf(lines.get(i));
				subList = lines.subList(++i, i = i + type.lines);
				Rule rule = type.constructor.apply(subList);
				rules.add(rule);

				if(rule instanceof DataRule) {
					for(String s : ((DataRule) rule).getDataHeadings()) {
						if(!catagories.contains(s))
							catagories.add(s);
					}
				}
			
				if(rule instanceof GroupTag) {
					groups.add((GroupTag) rule);
				}
			} catch(IllegalArgumentException iae) {
				if(type == null)
					throw new RuntimeException("Invalid rule type found (" + path.getFileName() + ":" + i + ")");
				else
					throw new RuntimeException("Error reading rule file (" + path.getFileName() + ":" + (i - type.lines - 1) + "-" + i + ")", iae);
			} catch(IndexOutOfBoundsException ioobe) {
				if(subList == null)
					throw new RuntimeException("Reached the end of file while reading (" + path.getFileName() + ":end)");
				else
					throw new RuntimeException("Error reading rule file (" + path.getFileName() + ":" + (i - type.lines - 1) + "-" + i + ")", ioobe);
			} catch(Exception e) {
				throw new RuntimeException("Error reading rule file (" + path.getFileName() + ":" + (i - type.lines - 1) + "-" + i + ")", e);
			}
		}
	}
	
	public List<ResultSet> searchPaths(Collection<Path> items, boolean caseSensitive) {
		List<Path> paths = new ArrayList<>();
		long[] size = new long[] {0};

		for(Path folder : items) {
			try {
				Files.walk(folder).forEach(file -> {
					if(!Files.isRegularFile(file))
						return;

					paths.add(file);
					try {
						size[0] += Files.size(file);
					} catch (Exception e) {
						throw new RuntimeException("Unable to read files", e);
					}
				});
			} catch (IOException e) {
				throw new RuntimeException("Unable to read files", e);
			}
		}

		AtomicLong done = new AtomicLong();
		List<ResultSet> results = Collections.synchronizedList(new ArrayList<>());
		ExecutorService executor = Executors.newFixedThreadPool(Math.max(10, Runtime.getRuntime().availableProcessors() * 3));
		
		for(Path path : paths) {
			executor.execute(() -> {
				try {
					SearchState state = new SearchState(rules, catagories, path, caseSensitive);
					
					if(state.shouldSearch()) {
						results.addAll(state.processFile(FileUtils.readAllLines(path)));
					}

					double percent = ((double) done.addAndGet(Files.size(path))) / size[0];
					Platform.runLater(() -> GUIController.INSTANCE.setProgress(percent));
				} catch(IOException e) {
					System.out.println("Malformed input file " + path.toString() + ": " + e.toString());
				}
			});
		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			return null;
		}

		return results;
	}

	public List<TableColumn<ResultSet, ?>> getColumns() {
		return IntStream.range(0, catagories.size()).mapToObj(index -> { 
			if(catagories.get(index).equals("Line No")) {
				TableColumn<ResultSet, Integer> column = new TableColumn<>(catagories.get(index));
				column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(Integer.parseInt(cellData.getValue().getData(index))));
				column.setId(String.valueOf(index));
				return column;
			} else {
				TableColumn<ResultSet, String> column = new TableColumn<>(catagories.get(index));
				column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getData(index)));
				column.setId(String.valueOf(index));
				return column;
			}
		}).collect(Collectors.toList());
	}
}