package brownshome.search.rule;

import java.util.List;

public class StartState implements Rule {

	public final boolean state;

	public StartState(List<String> lines) {
		state = lines.get(0).toUpperCase().equals("ON");
	}
	
	@Override
	public String getDescription() {
		return "Sets the search state for each file";
	}

}
