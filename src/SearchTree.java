import java.util.ArrayList;
import java.util.List;

public class SearchTree {	
	//abcdefghijklmnopqrstuvwxyz01234566789 SYMBOL
	static final int NUMBER_OF_CHARS = 'Z' - 'A' + '9' - '0' + 3;
	static final int SYMBOL = NUMBER_OF_CHARS - 1;
	
	static int translate(int c) {
		c = Character.toUpperCase(c);
		
		if(Character.isDigit(c))
			return c - '0' + 'Z' - 'A' + 1;
		
		if(c <= 'Z' && c >= 'A')
			return c - 'A';
		
		return SYMBOL;
	}
	
	SearchTree[] options = new SearchTree[NUMBER_OF_CHARS];
	String word;
	
	void addTag(String tag, int layer) {
		int index = translate(tag.codePointAt(layer));
		SearchTree currentTree = options[index];
		
		//add new subtree
		if(currentTree == null) 
			options[index] = new SearchTree();
		
		//populate tree with word or new subtree
		if(tag.length() > layer + 1)
			options[index].addTag(tag, layer + 1);
		else
			options[index].setWord(tag);
	}
	
	@Override
	public String toString() {
		List<String> tags = getTags();
		if(tags.isEmpty())
			return "Empty";
		
		String s = "[";
		for(int i = 0; i < tags.size() && i < 10; i++) {
			s += (i == 0 ? "" : ", ") + tags.get(i);
		}
		s += "]";
		
		if(tags.size() > 10)
			s += "(" + tags.size() + " more )";
		
		return s;
	}
	
	//used for debug
	public List<String> getTags() {
		ArrayList<String> l = new ArrayList<>();
		
		if(getWord() != null) l.add(getWord());
		
		for(SearchTree tree : options) {
			if(tree != null)
				l.addAll(tree.getTags());
		}
		
		return l;
	}
	
	public void setWord(String word) {
		this.word = word;
	}
	
	SearchTree() {}
	
	public SearchTree(List<String> input) {
		for(String tag : input)
			addTag(tag.trim(), 0);
	}
	
	public String getWord() {
		return word;
	}
	
	public SearchTree getTree(int c) {
		return options[translate(c)];
	}
}
