package brownshome.search.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import brownshome.search.tree.SearchTree.Match;

public class SearchMatch implements Rule, DataRule {
	public final static String SENTINEL = "\ufdd0"; //a non-character
	
	Pattern regex;
	List<NamedGroup> namedGroups = new ArrayList<>();
	
	public SearchMatch(List<String> lines) {
		regex = Pattern.compile(lines.get(0).replace("%search%", Pattern.quote(SENTINEL)));
		
		if(lines.get(1).equals(",")) {
			namedGroups = Collections.emptyList();
		} else {
			String[] rawNamedGroups = lines.get(1).split(",");
			NamedGroup[] namedGroupsArray = new NamedGroup[rawNamedGroups.length];
		
			for(int i = 0; i < rawNamedGroups.length; i++) {
				namedGroupsArray[i] = new NamedGroup(rawNamedGroups[i]);
				namedGroups = Arrays.asList(namedGroupsArray);
			}
		}
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
		
		for(NamedGroup namedGroup : namedGroups) {
			String group = matcher.group(namedGroup.index);
			
			result.add(namedGroup.name, group == null ? "No Data" : group.replace(SENTINEL, match.tag));
		}
		
		return true;
	}

	@Override
	public List<String> getDataHeadings() {
		return namedGroups.stream().map(group -> group.name).collect(Collectors.toList());
	}
}
