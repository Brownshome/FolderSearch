package brownshome.search.rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

import brownshome.search.tree.SearchTree;
import brownshome.search.tree.SearchTree.Match;
import brownshome.search.ui.GUIController;

// FILE - always on
// LINE - only on if file is active
// OTHER - only on if file and line is active

public class RuleSet {
	List<String> catagories;

	public class ResultSet {
		String[] data;

		public ResultSet() {
			data = new String[catagories.size()];
			Arrays.fill(data, "No Data");
		}

		public void add(String paramater, String value) {
			data[catagories.indexOf(paramater)] = value;
		}
		
		String getData(int i) {
			return data[i];
		}
	}

	List<Rule> rules = new ArrayList<>();
	List<Rule> filteredSet;

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
			lines = Files.readAllLines(path);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read rule file " + path, e);
		}


		catagories = new ArrayList<>();

		catagories.add("File");
		catagories.add("Line No");
		catagories.add("Match");

		for(int i = 0; i < lines.size(); i++) {
			RuleType type = RuleType.valueOf(lines.get(i));
			List<String> sublist = lines.subList(++i, i = i + type.lines);
			Rule rule = type.constructor.apply(sublist);
			rules.add(rule);

			if(rule instanceof DataRule) {
				catagories.addAll(((DataRule) rule).getDataHeadings());
			}

			if(rule instanceof GroupTag) {
				groups.add((GroupTag) rule);
			}
		}
	}

	public List<ResultSet> searchPaths(Collection<Path> items) {
		List<ResultSet> results = new ArrayList<>();

		List<Path> paths = new ArrayList<>();
		long[] size = new long[] {0};

		for(Path folder : items) {
			try {
				Files.walk(folder).forEach(file -> {
					if(!Files.isReadable(file) || !Files.isRegularFile(file))
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

		long done = 0;
		for(Path file : paths) {
			try {
				done += Files.size(file);
				
				double percent = ((double) done) / size[0];
				Platform.runLater(() -> GUIController.INSTANCE.setProgress(percent));
				
				if(setFileName(file.getFileName().toString())) {
					int lineNo = 1;
					for(String line : Files.readAllLines(file)) {
						List<ResultSet> result = processLine(line);

						int l = lineNo++;
						result.forEach((ResultSet r) -> {
							r.add("File", file.getFileName().toString());
							r.add("Line No", String.valueOf(l));
						});

						results.addAll(result);
					}
				}
			} catch(IOException e) {
				//malformed UTF-8 expression
			}
		}

		return results;
	}

	/** Returns true if the file needs to be processed, this also sets up the
	 * rule list */
	public boolean setFileName(String fileName) {
		filteredSet = new ArrayList<>(rules);

		boolean isCurrentlyValid = true;
		for(Iterator<Rule> it = filteredSet.iterator(); it.hasNext();) {
			Rule rule = it.next();

			if(rule instanceof FileRule) {
				isCurrentlyValid = ((FileRule) rule).isValid(fileName, isCurrentlyValid);
				it.remove();
			} else {
				if(!isCurrentlyValid)
					it.remove();
			}
		}

		return !filteredSet.isEmpty();
	}

	public List<ResultSet> processLine(String line) {
		boolean isCurrentlyValid = true;
		List<ResultSet> results = new ArrayList<>();

		for(Rule rule : filteredSet) {
			if(rule instanceof LineRule) {
				isCurrentlyValid = ((LineRule) rule).isValid(line, isCurrentlyValid);
			} else if(rule instanceof GroupTag) {
				((GroupTag) rule).processLine(line);
			} else if(rule instanceof SearchMatch) {
				for(Match match : SearchTree.getMatches(line)) {
					ResultSet result = new ResultSet();
					result.add("Match", match.tag);

					if(((SearchMatch) rule).match(line, match, result)) {
						for(GroupTag tag : groups)
							tag.fillResultSet(result);

						results.add(result);
					}
				}
			}
		}

		return results;
	}

	public List<TableColumn<ResultSet, ?>> getColumns() {
		return IntStream.range(0, catagories.size()).mapToObj(index -> { 
			if(catagories.get(index).equals("Line No")) {
				TableColumn<ResultSet, Integer> column = new TableColumn<>(catagories.get(index));
				column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(Integer.parseInt(cellData.getValue().data[index])));
				return column;
			} else {
				TableColumn<ResultSet, String> column = new TableColumn<>(catagories.get(index));
				column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().data[index]));
				return column;
			}
		}).collect(Collectors.toList());
	}
}