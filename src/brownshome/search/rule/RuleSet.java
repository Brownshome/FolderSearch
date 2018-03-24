package brownshome.search.rule;

import java.io.IOException;
import java.lang.Character.UnicodeScript;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import brownshome.search.FileUtils;
import brownshome.search.tree.SearchTree;
import brownshome.search.tree.SearchTree.Match;
import brownshome.search.ui.GUIController;

// FILE - always on
// LINE - only on if file is active
// OTHER - only on if file and line is active

public class RuleSet {
	public List<String> catagories;

	public class ResultSet {
		public String[] data;

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
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);
		
		for(Path path : paths) {
			final Path file = path;
			executor.execute(() -> {
				try {
					List<Rule> listOfRules = setFileName(file.getFileName().toString());

					boolean[] isSwitchedOn = { true };
					for(Rule rule : listOfRules) {
						if(rule instanceof StartState) {
							isSwitchedOn[0] = ((StartState) rule).state;
						}
					}

					if(!listOfRules.isEmpty()) {
						System.out.println("Starting " + file);

						int lineNo = 1;
						for(String line : FileUtils.readAllLines(file)) {
							if(Thread.interrupted()) {
								return;
							}

							List<ResultSet> result = processLine(listOfRules, line, caseSensitive, isSwitchedOn);

							int l = lineNo++;
							result.forEach((ResultSet r) -> {
								r.add("File", file.toString());
								r.add("Line No", String.valueOf(l));
							});

							results.addAll(result);
						}
					}

					double percent = ((double) done.addAndGet(Files.size(file))) / size[0];
					Platform.runLater(() -> GUIController.INSTANCE.setProgress(percent));
				} catch(IOException e) {
					System.out.println("Malformed input file " + file.toString() + ": " + e.toString());
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

	/** Returns true if the file needs to be processed, this also sets up the rule list */
	public List<Rule> setFileName(String fileName) {
		List<Rule> filteredSet = new ArrayList<>(rules);

		boolean isCurrentlyValid = true;
		for(Iterator<Rule> it = filteredSet.iterator(); it.hasNext();) {
			Rule rule = it.next();

			if(rule instanceof GroupTag) {
				((GroupTag) rule).reset();
			}
			
			if(rule instanceof FileRule) {
				isCurrentlyValid = ((FileRule) rule).isValid(fileName, isCurrentlyValid);
				it.remove();
			} else if(!isCurrentlyValid)
				it.remove();
		}

		return filteredSet;
	}

	public List<ResultSet> processLine(List<Rule> filteredSet, String line, boolean caseSensitive, boolean[] isSwitchedOn) {
		boolean isCurrentlyValid = true;
		List<ResultSet> results = new ArrayList<>();

		for(Rule rule : filteredSet) {
			if(rule instanceof SwitchRule) {
				isSwitchedOn[0] = ((SwitchRule) rule).isOn(line, isSwitchedOn[0]);
			} else if(isSwitchedOn[0]) {
				if(rule instanceof LineRule) {
					isCurrentlyValid = ((LineRule) rule).isValid(line, isCurrentlyValid);
				} else {
					if(!isCurrentlyValid) continue;

					if(rule instanceof GroupTag) {
						((GroupTag) rule).processLine(line);
					} else if(rule instanceof SearchMatch) {
						for(Match match : SearchTree.getMatches(line, caseSensitive)) {
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
			}
		}

		return results;
	}

	public List<TableColumn<ResultSet, ?>> getColumns() {
		return IntStream.range(0, catagories.size()).mapToObj(index -> { 
			if(catagories.get(index).equals("Line No")) {
				TableColumn<ResultSet, Integer> column = new TableColumn<>(catagories.get(index));
				column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(Integer.parseInt(cellData.getValue().data[index])));
				column.setId(String.valueOf(index));
				return column;
			} else {
				TableColumn<ResultSet, String> column = new TableColumn<>(catagories.get(index));
				column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().data[index]));
				column.setId(String.valueOf(index));
				return column;
			}
		}).collect(Collectors.toList());
	}
}