package brownshome.search.rule;

import java.io.File;

public interface FileRule extends Rule {
	boolean isValid(File file);
}
