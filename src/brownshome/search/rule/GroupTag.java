package brownshome.search.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import brownshome.search.rule.RuleSet.ResultSet;

public class GroupTag implements Rule, DataRule {
	List<NamedGroup> namedGroups;
	Pattern regex;
	
	List<String> currentGroup;
	
	public GroupTag(List<String> lines) {
		regex = Pattern.compile(lines.get(0));
		
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
		return "Looking for " + regex + " as a heading.";
	}

	@Override
	public List<String> getDataHeadings() {
		return namedGroups.stream().map(g -> g.name).collect(Collectors.toList());
	}

	public void reset() {
		currentGroup = null;
	}
	
	public void processLine(String line) {
		Matcher matcher = regex.matcher(line);
		if(matcher.find()) {
			currentGroup = new ArrayList<>();
			for(NamedGroup category : namedGroups) {
				currentGroup.add(matcher.group(category.index));
			}
		}
	}

	public void fillResultSet(ResultSet result) {
		if(currentGroup != null) {
			for(int i = 0; i < namedGroups.size(); i++) {
				if(currentGroup.get(i) != null) {
					result.add(namedGroups.get(i).name, currentGroup.get(i));
				} else {
					result.add(namedGroups.get(i).name, "No Data");
				}
			}
		} else {
			for(NamedGroup s : namedGroups) {
				result.add(s.name, "no Data");
			}
		}
	}
}
