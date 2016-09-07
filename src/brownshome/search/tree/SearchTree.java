package brownshome.search.tree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchTree {
	public static class Match {
		public final String tag;
		public final int from;
		public final int to;
		
		public Match(int from, int to, String tag) {
			this.from = from;
			this.to = to;
			this.tag = tag;
		}
	}
	
	public static SearchTree rootTree;
	
	public static SearchTree createTree(List<String> readAllLines) {
		return new SearchTree(readAllLines);
	}
	
	public static List<Match> getMatches(String line) {
		List<SearchTree> subTrees = new ArrayList<>();
		List<Match> matches = new ArrayList<>();
		
		for(int ci = 0; ci < line.length(); ci++) {
			int c = line.codePointAt(ci);
			
			//take every subtree and traverse one step lower
			for(int i = 0; i < subTrees.size(); i++) {
				SearchTree t = subTrees.get(i);

				if(t != null) {
					subTrees.set(i, t = t.getTree(c));
					if(t != null) {
						if(t.getWord() != null) {
							matches.add(new Match(ci - t.getWord().length() + 1, ci + 1, t.getWord()));
						}
					}
				}
			}

			//add new subtrees from root tree
			SearchTree subTree = rootTree.getTree(c);
			if(subTree != null) {
				int index = -1;
				for(int i = 0; i < subTrees.size(); i++) {
					if(subTrees.get(i) == null)
						index = i;
				}

				//try to conserver space by reusing indices
				if(index != -1) {
					subTrees.set(index, subTree);
				} else {
					subTrees.add(subTree);
				}

				if(subTree.getWord() != null) {
					matches.add(new Match(ci - subTree.getWord().length() + 1, ci + 1, subTree.getWord()));
				}
			}
		}
		
		return matches;
	}
	
	Map<Integer, SearchTree> options = new HashMap<>();
	String word;
	
	void addTag(String tag, int layer) {
		int index = tag.codePointAt(layer);
		SearchTree currentTree = options.get(index);
		
		//add new subtree
		if(currentTree == null) 
			options.put(index, currentTree = new SearchTree());
		
		//populate tree with word or new subtree
		if(tag.length() > layer + 1)
			currentTree.addTag(tag, layer + 1);
		else
			currentTree.setWord(tag);
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
		
		for(SearchTree tree : options.values()) {
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
		for(String tag : input) {
			if(Thread.interrupted()) 
				return;
			
			addTag(tag.trim(), 0);
		}
	}
	
	public String getWord() {
		return word;
	}
	
	public SearchTree getTree(int c) {
		return options.get(c);
	}
}
