package brownshome.search.rule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

public class GroupTag implements Rule, DataRule {
	private final List<NamedGroup> namedGroups;
	private final Pattern regex;
	
	public GroupTag(List<String> lines) {
		regex = Pattern.compile(lines.get(0));
		
		if(lines.get(1).equals(",")) {
			namedGroups = Collections.emptyList();
		} else {
			String[] rawNamedGroups = lines.get(1).split(",");
			NamedGroup[] namedGroupsArray = new NamedGroup[rawNamedGroups.length];
		
			for(int i = 0; i < rawNamedGroups.length; i++) {
				namedGroupsArray[i] = new NamedGroup(rawNamedGroups[i]);	
			}
			
			namedGroups = Arrays.asList(namedGroupsArray);
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
	
	public void processLine(String line, Map<String, String> groups) {
		Matcher matcher = regex.matcher(line);
		if(matcher.find()) {
			for(NamedGroup category : namedGroups) {
				groups.put(category.name, matcher.group(category.index));
			}
		}
	}
}
