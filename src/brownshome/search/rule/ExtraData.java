package brownshome.search.rule;

import java.util.List;
import java.util.regex.*;

public class ExtraData implements Rule {
	int matchNumber;
	String dataName;
	String regex;
	
	/* Lines are:
	 * 		regex
	 * 		matchNumber
	 * 		data
	 */
	public ExtraData(List<String> list) {
		regex = list.get(0);
		
		try {
			matchNumber = Integer.parseInt(list.get(1));
		} catch(NumberFormatException nfe) {
			throw new RuntimeException("Invalid rule input");
		}
		
		dataName = list.get(2);
	}
	
	public String getData(String line, String tag) {
		Matcher matcher = Pattern.compile(regex.replace("%search%", tag)).matcher(line);
		for(int i = 0; i < matchNumber && matcher.find(); i++);
		return matcher.group();
	}

	@Override
	public String getDescription() {
		return "Adds the extra data " + dataName + " to the table, pulled from " + regex;
	}
}
