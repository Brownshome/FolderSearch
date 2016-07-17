package brownshome.search.rule;

public interface FileRule extends Rule {
	boolean isValid(String file, boolean isCurrentlyValid);
}
