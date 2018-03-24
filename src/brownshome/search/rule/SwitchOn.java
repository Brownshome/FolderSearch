package brownshome.search.rule;

import java.util.List;
import com.google.re2j.Pattern;

public class SwitchOn implements SwitchRule {
	Pattern regex;
	
	public SwitchOn(List<String> lines) {
		regex = Pattern.compile(lines.get(0));
	}
	
	@Override
	public String getDescription() {
		return "Switches on the search after reaching " + regex;
	}

	@Override
	public boolean isOn(String line, boolean isCurrentlyOn) {
		return isCurrentlyOn || regex.matcher(line).find();
	}
}
