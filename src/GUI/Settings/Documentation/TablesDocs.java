package GUI.Settings.Documentation;

import java.util.List;

public class TablesDocs {
    public static final DocTypes.DocCategory CATEGORY = new DocTypes.DocCategory("tables", "Tables", List.of(
            new DocTypes.DocPage("make-table", "Make a Table",
                    "Add a new table to the canvas, define columns, types, and constraints, then push it "
                            + "live to your database.",
                    "assets/docs/tables/make-table.png"),
            new DocTypes.DocPage("edit-table", "Edit a Table",
                    "Modify columns, keys, and relationships on an existing table directly from the ER diagram.",
                    "assets/docs/tables/edit-table.png")
    ));
}
