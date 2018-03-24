package brownshome.search.rule;

import java.util.List;
import com.google.re2j.Pattern;

public class SwitchOff implements SwitchRule {
	Pattern regex;
	
	public SwitchOff(List<String> lines) {
		regex = Pattern.compile(lines.get(0));
	}
	
	@Override
	public String getDescription() {
		return "Switches off the search after reaching " + regex;
	}

	@Override
	public boolean isOn(String line, boolean isCurrentlyOn) {
		return isCurrentlyOn && !regex.matcher(line).find();
	}
}
