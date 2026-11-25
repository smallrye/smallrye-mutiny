package docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension(namespace = "snippet")
public class SnippetExtension {

    private static final Path SNIPPET_ROOT = Path.of("../src/test");

    static String insert(String file) throws IOException {
        return insert(file, null);
    }

    static String insert(String file, String tag) throws IOException {
        Path path = SNIPPET_ROOT.resolve(file);
        if (!Files.exists(path)) {
            return "<!-- snippet not found: " + file + " -->";
        }
        if (tag == null || tag.isBlank()) {
            return Files.readString(path);
        }
        boolean[] recording = { false };
        String content = Files.readAllLines(path).stream()
                .filter(line -> {
                    if (!recording[0] && line.contains("<" + tag + ">")) {
                        recording[0] = true;
                        return false;
                    } else if (recording[0] && line.contains("</" + tag + ">")) {
                        recording[0] = false;
                        return false;
                    }
                    return recording[0];
                })
                .collect(Collectors.joining("\n"));
        if (content.isEmpty()) {
            return "<!-- tag '" + tag + "' not found in " + file + " -->";
        }
        return dedent(content);
    }

    private static String dedent(String text) {
        String[] lines = text.split("\n", -1);
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int indent = 0;
            for (char c : line.toCharArray()) {
                if (c == ' ') {
                    indent++;
                } else if (c == '\t') {
                    indent += 4;
                } else {
                    break;
                }
            }
            minIndent = Math.min(minIndent, indent);
        }
        if (minIndent == 0 || minIndent == Integer.MAX_VALUE) {
            return text;
        }
        int finalMinIndent = minIndent;
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) {
                sb.append("\n");
            } else {
                sb.append(line.substring(Math.min(finalMinIndent, line.length()))).append("\n");
            }
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
