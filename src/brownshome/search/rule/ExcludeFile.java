package brownshome.search.rule;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class ExcludeFile implements FileRule {
	Pattern fileRegex;
	
	public ExcludeFile(List<String> lines) {
		fileRegex = Pattern.compile(lines.get(0));
	}

	@Override
	public String getDescription() {
		return "Exclude files matching " + fileRegex;
	}

	@Override
	public boolean isValid(File file) {
		return file.isFile() && !fileRegex.matcher(file.getName()).find();
	}
}
