package brownshome.search.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brownshome.search.rule.RuleSet.ResultSet;
import brownshome.search.tree.SearchTree.Match;

public class SearchMatch implements Rule, DataRule {
	public final static String SENTINEL = "\ufdd0"; //a non-character
	
	Pattern regex;
	List<String> namedGroups = new ArrayList<>();
	
	public SearchMatch(List<String> lines) {
		regex = Pattern.compile(lines.get(0).replace("%search%", Pattern.quote(SENTINEL)));
		namedGroups = Arrays.asList(lines.get(1).split(","));
		
		if(lines.get(1).equals(","))
			namedGroups = Collections.emptyList();
	}
	
	@Override
	public String getDescription() {
		return "Matching " + regex;
	}
	
	public boolean match(String line, Match match, ResultSet result) {
		//replace the match with a sentinel string
		line = line.substring(0, match.from) + SENTINEL + line.substring(match.to);
		
		//do the match
		//replace the sentinel with the match
		Matcher matcher = regex.matcher(line);
		
		if(!matcher.find())
			return false;
		
		for(String namedGroup : namedGroups) {
			String group = matcher.group(namedGroup);
			
			result.add(namedGroup, group == null ? "No Data" : group.replace(SENTINEL, match.tag));
		}
		
		return true;
	}

	@Override
	public List<String> getDataHeadings() {
		return namedGroups;
	}
}
