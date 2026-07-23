package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class SchemasDocs {
    public static final DocCategory CATEGORY = new DocCategory("schemas", "Schemas", List.of(

            new DocPage("diagram-view", "Viewing the Schema Diagram",
                    "Once connected, every schema on the server appears in the LOCAL list in the left sidebar. "
                            + "Selecting a schema renders all of its tables on a canvas as an entity-relationship "
                            + "diagram: each card shows the table name, its primary key (PK) at the top, and any "
                            + "foreign keys (FK) below it in a distinct colour, followed by the remaining columns "
                            + "and their types.\n\n"
                            + "Relationship lines are drawn automatically between a foreign key and the primary "
                            + "key it references, so you can trace how tables connect — for example, following "
                            + "the line from loans.customer_id to customers.customer_id. Use the toolbar above "
                            + "the schema list to refresh the current layout, auto-arrange the cards, or re-sort "
                            + "them, and use \"Filter schemas...\" to quickly jump to a schema by name when the "
                            + "server hosts many databases.\n\n"
                            + "Selecting the \"≡\" menu on any table card opens quick actions for that table, "
                            + "including editing its structure or viewing its data, without leaving the diagram.",
                    "assets/docs/schemas/diagram-view.png"),

            new DocPage("add-schema", "Add a Schema",
                    "Create a new schema from the \"+ New schema\" link at the bottom of the sidebar. This opens "
                            + "the \"Create New Schema\" screen, which scaffolds an empty canvas you can build up "
                            + "one table at a time, rather than requiring you to write raw DDL.\n\n"
                            + "Start by giving the schema a name (e.g. \"inventory_db\") in the Schema Name field. "
                            + "By default a new schema starts with one blank table definition; give it a name, "
                            + "choose a data type for its primary key from the dropdown (INT is selected by "
                            + "default), and use \"Add Field\" to add additional columns beyond the primary key. "
                            + "Select \"+ Add Table\" to add further tables to the same schema before saving.\n\n"
                            + "When you're ready, select \"Save Schema\" to push the schema — and every table "
                            + "defined on this screen — live to the connected MySQL server in a single operation. "
                            + "Select \"← Back\" at any point to discard the in-progress schema and return to the "
                            + "diagram view.",
                    "assets/docs/schemas/add-schema.png"),

            new DocPage("edit-schema", "Edit a Schema",
                    "Open the \"≡\" menu on any schema, or select the schema in the sidebar and choose Edit, to "
                            + "reach the \"Edit Schema\" screen. This view lists every table in the schema as a "
                            + "card showing its full column list, giving you an at-a-glance summary of the "
                            + "schema's structure without opening each table individually.\n\n"
                            + "To rename the schema, edit the text field at the top and select \"Rename\" — this "
                            + "renames the underlying database on the server, so any external tools or "
                            + "connection strings pointing at the old name will need to be updated. Select "
                            + "\"+ Add Table\" to add a new table to the schema, or open a table's \"≡\" menu to "
                            + "edit or delete just that table.\n\n"
                            + "Selecting \"Delete Schema\" permanently drops the schema and every table inside "
                            + "it from the MySQL server — this action cannot be undone, so double-check you have "
                            + "selected the correct schema before confirming. Note the distinction from removing "
                            + "a schema from the sidebar list elsewhere in the app: \"Delete Schema\" here always "
                            + "drops the real underlying database.",
                    "assets/docs/schemas/edit-schema.png")
    ));
}
