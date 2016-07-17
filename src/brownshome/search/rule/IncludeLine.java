package brownshome.search.rule;

import java.util.List;
import java.util.regex.Pattern;

public class IncludeLine implements LineRule {
	Pattern lineRegex;
	
	public IncludeLine(List<String> lines) {
		lineRegex = Pattern.compile(lines.get(0));
	}

	@Override
	public String getDescription() {
		return "Include lines matching " + lineRegex;
	}

	@Override
	public boolean isValid(String line, boolean isCurrentlyValid) {
		return isCurrentlyValid || lineRegex.matcher(line).find();
	}
}
