package GUI.Settings.Documentation;

import java.util.List;

public class SchemasDocs {
    public static final DocTypes.DocCategory CATEGORY = new DocTypes.DocCategory("schemas", "Schemas", List.of(
            new DocTypes.DocPage("add-schema", "Add a Schema",
                    "Create a new schema from the sidebar. Free My Query will scaffold an empty canvas you "
                            + "can start adding tables to, or connect it to an existing database.",
                    "assets/docs/schemas/add-schema.png"),
            new DocTypes.DocPage("edit-schema", "Edit a Schema",
                    "Rename, reorganize, or delete a schema from the schema card grid. Deleting a schema does "
                            + "not drop the underlying database — it only removes it from the app.",
                    "assets/docs/schemas/edit-schema.png")
    ));
}
