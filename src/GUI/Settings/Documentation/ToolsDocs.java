package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class ToolsDocs {
    public static final DocCategory CATEGORY = new DocCategory("tools", "Tools", List.of(

            new DocPage("sql-query", "SQL Query",
                    "The SQL Query tool (the </> icon in the left rail) gives you a direct, free-form SQL "
                            + "console against the currently selected connection, for anything the visual "
                            + "schema, table, and data tools don't cover.\n\n"
                            + "Type or paste a statement into the Query box and select \"Run\" — or use the "
                            + "Ctrl+Enter shortcut — to execute it against the active connection. Results, or "
                            + "any error returned by the server, are printed into the Output panel below. Use "
                            + "\"Clear Input/Output\" to reset both panels for a fresh statement.\n\n"
                            + "Every statement you run is recorded in the History panel on the right; step "
                            + "backward and forward through it with the \"↓ Prev\" and \"↑ Next\" buttons (or the "
                            + "Ctrl+↑ / Ctrl+↓ shortcuts) to re-run or tweak a previous query without retyping "
                            + "it, and select \"Clear History\" to wipe the recorded list for the current "
                            + "session.\n\n"
                            + "Because this tool executes exactly what you type with no schema validation "
                            + "beforehand, destructive statements (DELETE, DROP, TRUNCATE, etc.) run "
                            + "immediately and unprompted — double-check a statement's WHERE clause before "
                            + "running it here.",
                    "assets/docs/tools/sql-query.png"),

            new DocPage("mysql-logs", "MySQL Logs",
                    "The Logs view streams the connected server's general query log, giving you a live audit "
                            + "trail of every connection, disconnection, and query the server is handling — "
                            + "useful for debugging unexpected behaviour or watching what other clients are "
                            + "doing against a shared database.\n\n"
                            + "Each row shows the Time the event was logged, the User/Host it came from, a "
                            + "Command type (CONNECT, QUERY, QUIT, and so on, colour-coded for quick scanning), "
                            + "and the full Query text where applicable. Use the Type dropdown to filter down to "
                            + "a single command type, and the Limit field to control how many rows are pulled "
                            + "per refresh (200 by default).\n\n"
                            + "Select \"Refresh\" to pull the latest rows on demand, or turn on \"Auto-refresh\" "
                            + "to have the view keep polling and update itself continuously — handy when you're "
                            + "actively reproducing an issue and want to watch queries arrive in real time. The "
                            + "row count and last-updated time are shown at the bottom-left of the table.\n\n"
                            + "Note that enabling the general query log has a performance cost on the server "
                            + "itself, since every statement must be written out; avoid leaving Auto-refresh "
                            + "running unattended against a busy production instance for extended periods.",
                    "assets/docs/tools/mysql-logs.png")
    ));
}
