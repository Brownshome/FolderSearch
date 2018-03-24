package brownshome.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import brownshome.search.rule.FileRule;
import brownshome.search.rule.GroupTag;
import brownshome.search.rule.LineRule;
import brownshome.search.rule.ResultSet;
import brownshome.search.rule.Rule;
import brownshome.search.rule.SearchMatch;
import brownshome.search.rule.StartState;
import brownshome.search.rule.SwitchRule;
import brownshome.search.tree.SearchTree;
import brownshome.search.tree.SearchTree.Match;

/** Keeps track of the current state of a search. This is re-created per file and is not shared between threads */
public class SearchState {
	//True if the search is switched on for this line.
	private boolean switchedOn = true;
	private List<Rule> enabledRules;
	private Map<String, String> groups = new HashMap<>();
	private final boolean caseSensitive;
	private final Path fileName;
	private final List<String> categories;
	
	public SearchState(List<Rule> rules, List<String> categories, Path fileName, boolean caseSensitive) {
		enabledRules = new ArrayList<>(rules);
		this.categories = categories;
		this.fileName = fileName;
		this.caseSensitive = caseSensitive;

		boolean isCurrentlyValid = true;
		for(Iterator<Rule> it = enabledRules.iterator(); it.hasNext();) {
			Rule rule = it.next();
			
			if(rule instanceof FileRule) {
				isCurrentlyValid = ((FileRule) rule).isValid(fileName.getFileName().toString(), isCurrentlyValid);
				it.remove();
			} else if(!isCurrentlyValid) {
				it.remove();
			} else if(rule instanceof StartState) {
				switchedOn = ((StartState) rule).state;
			}
		}
	}
	
	public boolean shouldSearch() {
		return !enabledRules.isEmpty();
	}
	
	public Collection<ResultSet> processFile(List<String> lines) {
		Collection<ResultSet> results = new ArrayList<>();
		
		if(!enabledRules.isEmpty()) {
			System.out.println("Starting " + fileName);

			int lineNo = 1;
			for(String line : lines) {
				if(Thread.interrupted()) {
					return Collections.emptyList();
				}

				Collection<ResultSet> result = processLine(line);

				int l = lineNo++;
				result.forEach((ResultSet r) -> {
					r.add("File", fileName.toString());
					r.add("Line No", String.valueOf(l));
				});

				results.addAll(result);
			}
		}
		
		return results;
	}
	
	private Collection<ResultSet> processLine(String line) {
		boolean isCurrentlyValid = true;
		Collection<ResultSet> results = new ArrayList<>();

		for(Rule rule : enabledRules) {
			if(rule instanceof SwitchRule) {
				switchedOn = ((SwitchRule) rule).isOn(line, switchedOn);
			} else if(switchedOn) {
				if(rule instanceof LineRule) {
					isCurrentlyValid = ((LineRule) rule).isValid(line, isCurrentlyValid);
				} else {
					if(!isCurrentlyValid) 
						continue;

					if(rule instanceof GroupTag) {
						((GroupTag) rule).processLine(line, groups);
					} else if(rule instanceof SearchMatch) {
						for(Match match : SearchTree.getMatches(line, caseSensitive)) {
							ResultSet result = new ResultSet(categories);
							result.add("Match", match.tag);

							if(((SearchMatch) rule).match(line, match, result)) {
								for(Entry<String, String> pair : groups.entrySet())
									result.add(pair.getKey(), pair.getValue());

								results.add(result);
							}
						}
					}
				}
			}
		}
		
		return results;
	}
}
