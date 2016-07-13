package brownshome.search.rule;

import java.io.File;
import java.util.List;
import java.util.regex.*;

public class IncludeFile implements FileRule {
	Pattern fileRegex;
	
	public IncludeFile(List<String> lines) {
		fileRegex = Pattern.compile(lines.get(0));
	}

	@Override
	public String getDescription() {
		return "Include files matching " + fileRegex;
	}

	@Override
	public boolean isValid(File file) {
		return file.isFile() && fileRegex.matcher(file.getName()).find();
	}
}
