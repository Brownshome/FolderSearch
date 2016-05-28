import java.util.List;

public class SearchTree {	
	//abcdefghijklmnopqrstuvwxyz01234566789 SYMBOL
	static final int NUMBER_OF_CHARS = 'Z' - 'A' + '9' - '0' + 1;
	static final int SYMBOL = NUMBER_OF_CHARS - 1;
	
	static int translate(int c) {
		c = Character.toUpperCase(c);
		
		if(Character.isDigit(c))
			return c - '0' + 'Z' - 'A';
		
		if(c <= 'Z' && c >= 'A')
			return c - 'A';
		
		return SYMBOL;
	}
	
	SearchTree[] options = new SearchTree[NUMBER_OF_CHARS];
	String word;
	
	void addTag(String tag, int layer) {
		int index = translate(tag.codePointAt(layer));
		SearchTree currentTree = options[index];
		
		if(currentTree == null) 
			options[index] = new SearchTree();
		
		if(tag.length() > layer + 1)
			options[index].addTag(tag, layer + 1);
		else
			options[index].setWord(tag);
	}
	
	@Override
	public String toString() {
		return word == null ? "empty" : word;
	}
	
	public void setWord(String word) {
		this.word = word;
	}
	
	SearchTree() {}
	
	public SearchTree(List<String> input) {
		for(String tag : input)
			addTag(tag, 0);
	}
	
	public String getWord() {
		return word;
	}
	
	public SearchTree getTree(int c) {
		return options[translate(c)];
	}
}
