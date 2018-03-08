package brownshome.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {
	//Be safe, here be garbage input, ISO_8859_1 is safe for any input
	private static final CharsetDecoder defaultDecoder = StandardCharsets.ISO_8859_1.newDecoder();

	/**
	 * Reads the file into a list of strings, looking for byte order marks.
	 */
	public static List<String> readAllLines(Path path) throws IOException {
		try (InputStream in = Files.newInputStream(path)) {
			int first = in.read();

			switch(first) {
			default:
				return Files.readAllLines(path, StandardCharsets.UTF_8);
			case 0xef:
				//UTF-8 BOM or malformed
				if(in.read() == 0xbb && in.read() == 0xbf) {
					//We have already read the BOM so this works well
					return read(new InputStreamReader(in, StandardCharsets.UTF_8));
				} else {
					//Who knows what this is
					return read(in);
				}
			case 0xfe:
				if(in.read() == 0xff) {
					//we are UTF-16 BE
					return Files.readAllLines(path, StandardCharsets.UTF_16); //UTF_16 checks the BOM itself
				} else {
					return read(in);
				}

			case 0xff:
				if(in.read() == 0xfe) {
					return Files.readAllLines(path, StandardCharsets.UTF_16);
				} else {
					return read(in);
				}
			}
		} catch(MalformedInputException mie) {
			try (InputStream in = Files.newInputStream(path)) {
				return read(in);
			}
		}
	}

	private static List<String> read(InputStreamReader reader) {
		return new BufferedReader(reader).lines().collect(Collectors.toList());
	}

	private static List<String> read(InputStream in) {
		return new BufferedReader(new InputStreamReader(in, defaultDecoder)).lines().collect(Collectors.toList());
	}
}
