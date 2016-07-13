package brownshome.search.rule;

import java.util.List;
import java.util.function.Function;

public enum RuleType {
	EXCLUDE_FILE(ExcludeFile::new, 1),
	INCLUDE_FILE(IncludeFile::new, 1),
	EXCLUDE_LINE(ExcludeLine::new, 1),
	INCLUDE_LINE(IncludeLine::new, 1),
	SEARCH_MATCH(SearchMatch::new, 1),
	EXTRA_DATA(ExtraData::new, 3),
	GROUP_TAG(GroupTag::new, 3);
	
	Function<List<String>, ? extends Rule> constructor;
	int lines;
	
	RuleType(Function<List<String>, ? extends Rule> function, int lines) {
		this.constructor = function;
		this.lines = lines;
	}
}
