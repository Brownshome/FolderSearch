package brownshome.search.rule;

import java.util.List;
import java.util.regex.Pattern;

import brownshome.search.tree.SearchTree;

public class SearchMatch implements Rule {
	String regex;
	
	public SearchMatch(List<String> lines) {
		regex = lines.get(0);
	}
	
	@Override
	public String getDescription() {
		return "Matching " + regex;
	}
	
	public boolean matches(String line, String tag) {
		return line.matches(regex.replace("%search%", tag));
	}
}
