package brownshome.search.rule;

import java.util.List;
import com.google.re2j.Pattern;

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
	public boolean isValid(String file, boolean isCurrentlyValid) {
		return isCurrentlyValid && !fileRegex.matcher(file).find();
	}
}
