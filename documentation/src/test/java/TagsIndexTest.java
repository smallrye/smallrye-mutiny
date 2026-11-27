import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The "Tags" section of tags-index.adoc lists the pages per tag, from the {@code :topics:} each page declares.
 * MkDocs used to generate it; this test keeps it in sync, and prints the section to paste when it is not.
 */
public class TagsIndexTest {

    private static final Path DOCS = Path.of("src/main/asciidoc");
    private static final String TAGS_HEADING = "== Tags";

    @Test
    public void tagsIndexListsThePagesOfEachTag() throws IOException {
        String index = Files.readString(DOCS.resolve("tags-index.adoc"), StandardCharsets.UTF_8);
        int start = index.indexOf(TAGS_HEADING);
        assertThat(start).as("tags-index.adoc has a '%s' section", TAGS_HEADING).isNotNegative();

        String expected = expectedTagsSection();
        assertThat(index.substring(start))
                .as("The '%s' section of tags-index.adoc does not match the pages' :topics:. Replace it with:%n%n%s",
                        TAGS_HEADING, expected)
                .isEqualTo(expected);
    }

    private static String expectedTagsSection() throws IOException {
        // tag -> (title -> page)
        Map<String, Map<String, String>> pagesByTag = new TreeMap<>();
        List<Path> pages;
        try (Stream<Path> files = Files.walk(DOCS)) {
            pages = files.filter(p -> p.toString().endsWith(".adoc")).toList();
        }
        for (Path page : pages) {
            List<String> lines = Files.readAllLines(page, StandardCharsets.UTF_8);
            String title = lines.stream().filter(l -> l.startsWith("= ")).findFirst().map(l -> l.substring(2).trim())
                    .orElse(null);
            String topics = lines.stream().filter(l -> l.startsWith(":topics:")).findFirst()
                    .map(l -> l.substring(":topics:".length()).trim()).orElse(null);
            if (title == null || topics == null) {
                continue;
            }
            String xref = DOCS.relativize(page).toString().replace('\\', '/');
            for (String tag : topics.split(",")) {
                pagesByTag.computeIfAbsent(tag.trim(), t -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(title, xref);
            }
        }

        StringBuilder section = new StringBuilder(TAGS_HEADING).append("\n\n")
                .append("// Pages declare their tags with :topics: in their header.\n");
        pagesByTag.forEach((tag, pagesOfTag) -> {
            section.append("\n=== ").append(tag).append("\n\n");
            pagesOfTag.forEach((title, xref) -> section.append("* xref:").append(xref).append('[').append(title)
                    .append("]\n"));
        });
        return section.toString();
    }
}
