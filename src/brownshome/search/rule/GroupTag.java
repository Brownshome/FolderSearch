package brownshome.search.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brownshome.search.rule.RuleSet.ResultSet;

public class GroupTag implements Rule, DataRule {
	List<String> groupCategories;
	Pattern regex;
	
	List<String> currentGroup;
	
	public GroupTag(List<String> lines) {
		regex = Pattern.compile(lines.get(0));
		groupCategories = Arrays.asList(lines.get(1).split(","));
		
		if(lines.get(1).equals(","))
			groupCategories = Collections.emptyList();
	}

	@Override
	public String getDescription() {
		return "Looking for " + regex + " as a heading.";
	}

	@Override
	public List<String> getDataHeadings() {
		return groupCategories;
	}

	public void reset() {
		currentGroup = null;
	}
	
	public void processLine(String line) {
		Matcher matcher = regex.matcher(line);
		if(matcher.find()) {
			currentGroup = new ArrayList<>();
			for(int i = 0; i < groupCategories.size(); i++) {
				currentGroup.add(matcher.group(groupCategories.get(i)));
			}
		}
	}

	public void fillResultSet(ResultSet result) {
		if(currentGroup != null) {
			for(int i = 0; i < groupCategories.size(); i++) {
				if(currentGroup.get(i) != null) {
					result.add(groupCategories.get(i), currentGroup.get(i));
				} else {
					result.add(groupCategories.get(i), "No Data");
				}
			}
		} else {
			for(String s : groupCategories) {
				result.add(s, "no Data");
			}
		}
	}
}
