import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Search {
	public static void main(String[] args) throws IOException {
		if(args.length != 2)
			throw new IllegalArgumentException("Usage [tags] [searchdir]");

		List<String> tags = Files.readAllLines(Paths.get(args[0]));
		SearchTree tree = new SearchTree(tags);

		Files.newDirectoryStream(Paths.get(args[1])).forEach(path -> {
			try {
				int[] count = new int[] {0, 0};
				String[] name = new String[1];

				Files.lines(path).forEachOrdered(line -> {
					if(line.startsWith("&N ")) {
						name[0] = line.substring(3).trim();
					} else{ 
						if(!line.startsWith("{")) {
							List<SearchTree> subTrees = new ArrayList<>();
							line.codePoints().forEachOrdered(c -> {
								for(int i = 0; i < subTrees.size(); i++) {
									SearchTree t = subTrees.get(i);

									if(t != null) {
										subTrees.set(i, t = t.getTree(c));
										if(t != null) {
											if(t.getWord() != null) {
												found(t.getWord(), path, count[0], count[1] - t.getWord().length(), name[0]);
											}
										}
									}
								}

								SearchTree subTree = tree.getTree(c);
								if(subTree != null) {
									int index = -1;
									for(int i = 0; i < subTrees.size(); i++) {
										if(subTrees.get(i) == null)
											index = i;
									}

									if(index != -1) {
										subTrees.set(index, subTree);
									} else {
										subTrees.add(subTree);
									}

									if(subTree.getWord() != null) {
										found(subTree.getWord(), path, count[0], count[1] - subTree.getWord().length(), name[0]);
									}
								}

								count[1]++;
							});
							count[1] = 0;
							count[0]++;
						}
					}
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void found(String word, Path path, int line, int column, String pointName) {
		System.out.printf("Found %s in %s (%d, %d) p:%s%n", word, path.getFileName(), line + 1, column + 2, pointName);
	}
}
