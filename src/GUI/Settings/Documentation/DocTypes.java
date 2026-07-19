package GUI.Settings.Documentation;

import java.util.List;

public class DocTypes {
    public record DocPage(String key, String title, String body, String imagePath) {}
    public record DocCategory(String key, String label, List<DocPage> pages) {}
}
