package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class DataDocs {
    public static final DocCategory CATEGORY = new DocCategory("data", "Data", List.of(

            new DocPage("view-data", "View Data",
                    "Open a table from the diagram or the schema edit screen and select \"View Data\" (or open "
                            + "its data tab directly) to browse its rows in a searchable, sortable grid without "
                            + "writing any SQL. The header at the top shows the table name alongside the schema "
                            + "it belongs to, so it's always clear which database you're looking at.\n\n"
                            + "The \"Existing Data\" panel lists every row currently in the table, with columns "
                            + "laid out exactly as they're defined in the schema — including foreign key columns "
                            + "showing the raw referenced value (e.g. edited_department_id) and NULL values "
                            + "displayed explicitly rather than left blank. Select the refresh icon above the "
                            + "grid at any time to re-pull the latest data from the server, which is useful "
                            + "after making changes elsewhere in the app or from another client.\n\n"
                            + "A row count and last-loaded indicator (e.g. \"5 rows loaded\") appears at the "
                            + "bottom of the grid so you always know how much data is currently in view.",
                    "assets/docs/data/view-data.png"),

            new DocPage("crud-data", "CRUD Data",
                    "The \"Insert New Row\" panel on the right of a table's data view lets you add, and the "
                            + "grid on the left lets you remove, rows directly through the GUI — all changes are "
                            + "validated against the table's schema before being committed, so you can't "
                            + "accidentally submit a value of the wrong type or leave a required field empty.\n\n"
                            + "To insert a row, fill in a value for each field shown; each field is labelled "
                            + "with its column name and underlying type (e.g. varchar, date) directly beneath "
                            + "it. Foreign key fields are presented as a \"Select from <table>.<column>...\" "
                            + "dropdown rather than a free-text box, so you can only link to rows that actually "
                            + "exist in the referenced table. Select \"Insert Row\" to commit the new row, or "
                            + "\"Clear\" to reset the form without saving. Select \"+ New Row\" to start filling "
                            + "in another row immediately after inserting one.\n\n"
                            + "To delete a row, select the red \"x\" at the end of its line in the \"Existing "
                            + "Data\" grid on the left; this removes the row from the underlying table "
                            + "immediately, so make sure you've selected the correct row before confirming, "
                            + "especially on tables referenced by foreign keys elsewhere in the schema. Editing "
                            + "an existing value is done by selecting the cell directly in the grid.",
                    "assets/docs/data/crud-data.png"),

            new DocPage("generate-login-function", "Generate Login Function",
                    "The Generate Login Function tool produces a ready-to-use authentication code snippet "
                            + "scoped to a specific table's credentials structure, so you don't have to hand-write "
                            + "boilerplate login and signup logic for every project.\n\n"
                            + "Under Configuration, choose the table that stores your users (its full column "
                            + "list is shown for reference), then set:\n"
                            + "• Identifier Field — the column used to look a user up at login time (e.g. an "
                            + "email column such as editedEmail).\n"
                            + "• Password Field — the column holding the hashed password (e.g. password).\n\n"
                            + "Select \"Generate\" to produce a complete class in the Generated Code tab, "
                            + "including a strongly-typed User model matching every column in the table, a "
                            + "login(...) method that looks the user up by the identifier field and verifies "
                            + "the password with BCrypt.checkpw against the stored hash, and a MakeUser(...) "
                            + "method that hashes a new password and inserts a new row. A companion BCrypt.java "
                            + "tab is provided alongside the generated code so the snippet compiles without "
                            + "hunting down the dependency yourself; the required Maven/Gradle coordinate "
                            + "(org.mindrot:jbcrypt) is called out directly in the generated import comment.\n\n"
                            + "Select \"Copy\" in the top-right corner to copy the full generated file to your "
                            + "clipboard and paste it straight into your project. Because the generated queries "
                            + "use parameterized PreparedStatements throughout, the snippet is safe against SQL "
                            + "injection out of the box.",
                    "assets/docs/data/generate-login-function.png")
    ));
}
