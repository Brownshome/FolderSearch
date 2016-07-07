package brownshome.search.tree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Search {
	public static void main(String[] args) throws IOException {
		if(args.length != 2)
			throw new IllegalArgumentException("Usage [tags] [searchdir]");

		List<String> tags = Files.readAllLines(Paths.get(args[0]));
		
		//Home rolled suffix tree for the CL lookup.
		SearchTree tree = new SearchTree(tags);
		
		//Data structure for EB tag name lookups.
		Map<String, Integer> counts = new HashMap<>();
		for(String tag : tags) {
			counts.put(tag.trim().toUpperCase(), 0);
		}

		System.out.println("Reference,Path,File,Line,Tag,Paramater,Text");

		//for every file
		Files.walk(Paths.get(args[1])).forEach(path -> {
			int[] count = new int[] {0}; //pointer to a counter for the line number

			try {
				if(!Files.isDirectory(path)) {
					if(path.toString().endsWith(".EB")) {
						String[] name = new String[1];

						Files.lines(path).forEachOrdered(line -> {
							if(line.startsWith("&N"))
								name[0] = line.substring(3).trim();

							if(!line.startsWith("{") && line.contains("=")) {
								String t = line.split("=", 2)[1].trim().toUpperCase();

								if(t.contains(".")) {
									t = t.split("[.]")[0];
								}

								if(t.contains("\\"))
									t = t.split("\\\\")[1];

								if(counts.containsKey(t)) {
									String par = line.split("=", 2)[0].trim();
									found(t, path, count[0], name[0], par, line);
									counts.put(t, counts.get(t) + 1);
								}
							}

							count[0]++;
						});
					} else {
						Files.lines(path).forEachOrdered(line -> {
							if(!line.startsWith("--")) {
								List<SearchTree> subTrees = new ArrayList<>();
								line.codePoints().forEachOrdered(c -> {
									
									//take every subtree and traverse one step lower
									for(int i = 0; i < subTrees.size(); i++) {
										SearchTree t = subTrees.get(i);

										if(t != null) {
											subTrees.set(i, t = t.getTree(c));
											if(t != null) {
												if(t.getWord() != null) {
													found(t.getWord(), path, count[0], line);
													counts.put(t.getWord(), counts.get(t.getWord()) + 1);
												}
											}
										}
									}

									//add new subtrees from root tree
									SearchTree subTree = tree.getTree(c);
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
											found(subTree.getWord(), path, count[0], line);
											counts.put(subTree.getWord(), counts.get(subTree.getWord()) + 1);
										}
									}
								});
							}
							
							count[0]++;
						});
					}
				}
			} catch (Exception e) {
				//no error handling to ignore binary data files
			}
		});
	}

	private static void found(String reference, Path path, int line, String tag, String paramater, String text) {
		System.out.printf("%s,%s,%s,%d,%s,%s,%s%n", reference, path.getParent(), path.getFileName(), line + 1, tag, paramater, text.replace(',', ' ').trim());
	}

	private static void found(String reference, Path path, int line, String text) {
		found(reference, path, line, "--", "--", text);
	}
}
