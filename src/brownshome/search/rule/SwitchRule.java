package brownshome.search.rule;

public interface SwitchRule extends Rule {
	boolean isOn(String line, boolean isCurrentlyOn);
}
