package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class DataDocs {
    public static final DocCategory CATEGORY = new DocCategory("data", "Data", List.of(
            new DocPage("view-data", "View Data",
                    "Browse table rows in a searchable, sortable grid without writing any SQL.",
                    "assets/docs/data/view-data.png"),
            new DocPage("crud-data", "CRUD Data",
                    "Insert, update, and delete rows directly through the GUI. Changes are validated against "
                            + "the table's schema before being committed.",
                    "assets/docs/data/crud-data.png"),
            new DocPage("generate-login-function", "Generate Login Function",
                    "Generate a ready-to-use login code snippet scoped to a specific table's credentials structure.",
                    "assets/docs/data/generate-login-function.png")
    ));
}
