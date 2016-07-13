package brownshome.search.rule;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class RuleSet {
	List<FileRule> fileRules;
	List<LineRule> lineRules;
	List<SearchRule> searchRules;
	List<ExtraDataRule> extraData;
	
	public RuleSet(Path path) throws IOException {
		Iterator<String> lines = Files.readAllLines(path).iterator();
		while(lines.hasNext()) {
			
		}
	}
}