package brownshome.search.rule;

import java.util.List;
import java.util.regex.Pattern;

public class ExcludeLine implements LineRule {
	Pattern lineRegex;
	
	public ExcludeLine(List<String> lines) {
		lineRegex = Pattern.compile(lines.get(0));
	}

	@Override
	public String getDescription() {
		return "Excludes lines matching " + lineRegex;
	}

	@Override
	public boolean isValid(String line, boolean isCurrentlyValid) {
		return isCurrentlyValid && !lineRegex.matcher(line).find();
	}
}
