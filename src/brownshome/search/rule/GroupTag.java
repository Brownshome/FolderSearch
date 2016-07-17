package brownshome.search.rule;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brownshome.search.rule.RuleSet.ResultSet;

public class GroupTag implements Rule, DataRule {
	String tagname;
	Pattern regex;
	
	String currentTag;
	
	public GroupTag(List<String> lines) {
		regex = Pattern.compile(lines.get(0));
		tagname = lines.get(1);
	}

	@Override
	public String getDescription() {
		return "Sets the " + tagname + " start tag to be " + regex;
	}

	@Override
	public List<String> getDataHeadings() {
		return Arrays.asList(tagname);
	}

	public void reset() {
		currentTag = null;
	}
	
	public void processLine(String line) {
		Matcher matcher = regex.matcher(line);
		if(matcher.find())
			currentTag = matcher.group(tagname);
	}

	public void fillResultSet(ResultSet result) {
		if(currentTag != null)
			result.add(tagname, currentTag);
	}
}
