package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class TablesDocs {
    public static final DocCategory CATEGORY = new DocCategory("tables", "Tables", List.of(

            new DocPage("make-table", "Make a Table",
                    "New tables are defined on a card that starts with a Table Name field and a required "
                            + "primary key. Give the table a name, then name the primary key column and choose "
                            + "its data type from the dropdown on the right (INT is the default, and covers "
                            + "most auto-incrementing identifier columns).\n\n"
                            + "Select \"Add Field\" to append additional columns one at a time. For each field "
                            + "you'll set a name and a type; if a column should reference another table, mark "
                            + "it as a foreign key and pick the target table and column so Free My Query can "
                            + "draw the relationship on the diagram and enforce it at the database level. A "
                            + "table can be removed from the pending list entirely with the \"x\" in its "
                            + "top-right corner before it's saved.\n\n"
                            + "Tables can be added either while creating a brand-new schema, or afterwards from "
                            + "an existing schema's \"+ Add Table\" button — the same card-based form is used in "
                            + "both places. Nothing is written to the database until the enclosing \"Save "
                            + "Schema\" or equivalent save action is selected, so you can freely add, edit, or "
                            + "remove tables while composing the schema.",
                    "assets/docs/tables/make-table.png"),

            new DocPage("edit-table", "Edit a Table",
                    "Select a table's \"≡\" menu on the diagram, or open it from the schema edit screen, to "
                            + "reach \"Edit Table\". The panel on the right lists every column: the primary key "
                            + "is shown locked at the top with a blue \"PK\" badge, and each remaining column has "
                            + "its own row where you can change the data type and, if applicable, point it at a "
                            + "foreign key using the \"No reference\" dropdown (for example, setting "
                            + "edited_department_id to reference departments(id)).\n\n"
                            + "Select \"+ Add Field\" at the bottom of the panel to introduce a new column, or "
                            + "\"Remove\" beside an existing field to drop it — both actions stage the change "
                            + "without touching the database until you save. The lower half of the screen shows "
                            + "a live preview of the table's current data across tabs for every table in the "
                            + "schema, so you can confirm a column's real-world contents before deciding to "
                            + "change its type or remove it.\n\n"
                            + "Select \"Save Changes\" to apply the column additions, type changes, and "
                            + "reference updates to the live table via an ALTER TABLE operation. Because type "
                            + "changes and column removals can be destructive to existing data, review the data "
                            + "preview carefully beforehand — this is treated as a schema-altering action, not a "
                            + "data edit.",
                    "assets/docs/tables/edit-table.png")
    ));
}
