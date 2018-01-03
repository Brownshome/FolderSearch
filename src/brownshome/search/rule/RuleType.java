package brownshome.search.rule;

import java.util.List;
import java.util.function.Function;

public enum RuleType {
	EXCLUDE_FILE(ExcludeFile::new, 1),
	INCLUDE_FILE(IncludeFile::new, 1),
	EXCLUDE_LINE(ExcludeLine::new, 1),
	INCLUDE_LINE(IncludeLine::new, 1),
	SEARCH_MATCH(SearchMatch::new, 2),
	START_SEARCH_STATE(StartState::new, 1),
	SWITCH_ON(SwitchOn::new, 1),
	SWITCH_OFF(SwitchOff::new, 1),
	GROUP_TAG(GroupTag::new, 2);
	
	Function<List<String>, ? extends Rule> constructor;
	int lines;
	
	RuleType(Function<List<String>, ? extends Rule> function, int lines) {
		this.constructor = function;
		this.lines = lines;
	}
}
