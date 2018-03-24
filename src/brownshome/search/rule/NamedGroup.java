package brownshome.search.rule;

class NamedGroup {
	final String name;
	final int index;
	
	NamedGroup(String description) {
		try {
			String[] parts = description.split("=");
			name = parts[0];
			index = Integer.parseInt(parts[1]);
		} catch(ArrayIndexOutOfBoundsException | NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid creation string: " + description + " the correct format is NAME=INDEX,NAME=INDEX,...", ex);
		}
	}
}
