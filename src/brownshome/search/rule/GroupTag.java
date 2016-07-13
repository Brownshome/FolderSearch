package brownshome.search.rule;

import java.util.List;

public class GroupTag implements Rule {
	String tagname;
	String regex;
	
	String currentTag;

	public GroupTag(List<String> lines) {
		tagname = lines.get(0);
		regex = lines.get(1);
	}

	@Override
	public String getDescription() {
		return "Sets the " + tagname + " start tag to be " + regex;
	}
}
