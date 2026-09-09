package docs;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

import java.util.Comparator;
import java.util.List;

@DataMapping(value = "versions", type = DataMapping.Type.ARRAY_DIR)
public record Versions(List<Version> list) {

    public List<Version> sorted() {
        return list.stream()
                .sorted(Comparator.comparingInt(Version::sortOrder))
                .toList();
    }

    public record Version(
            String label,
            String path,
            int sortOrder,
            boolean defaultVersion,
            boolean devVersion,
            List<Section> sections) {

        public record Section(
                String name,
                List<MenuItem> items) {
        }

        public record MenuItem(
                String title,
                String path,
                String icon,
                String target) {
        }
    }
}
