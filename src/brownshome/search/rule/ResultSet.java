package brownshome.search.rule;

import java.util.Arrays;
import java.util.List;

public class ResultSet {
	private final String[] data;
	private final List<String> categories;

	public ResultSet(List<String> categories) {
		this.categories = categories;
		data = new String[categories.size()];
		Arrays.fill(data, "No Data");
	}

	public void add(String paramater, String value) {
		if(value == null) {
			value = "No Data";
		}
		
		data[categories.indexOf(paramater)] = value;
	}
	
	public String getData(int i) {
		return data[i];
	}
}