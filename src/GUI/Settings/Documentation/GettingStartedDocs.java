package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class GettingStartedDocs {
    public static final DocCategory CATEGORY = new DocCategory("getting-started", "Getting Started", List.of(

            new DocPage("sign-in", "Sign In",
                    "Every session begins at the sign-in screen. Enter the email address and password "
                            + "associated with your Free My Query account and select \"Sign In\" to continue. "
                            + "If you don't have an account yet, select \"Need an account? Sign Up\" below the "
                            + "button to create one — sign-up only requires an email address and a password, "
                            + "and no database details are collected at this stage.\n\n"
                            + "Your Free My Query account is separate from any MySQL credentials you connect "
                            + "with later: this login controls access to the application itself (your saved "
                            + "connection profiles, theme preferences, and subscription), while the database "
                            + "credentials you enter on the next screen control access to your actual data. "
                            + "Once signed in, you'll be taken straight to the local credentials screen so you "
                            + "can connect to a MySQL server.",
                    "assets/docs/getting-started/sign-in.png"),

            new DocPage("first-connection", "First Connection",
                    "After signing in, connect to your first MySQL database from the \"Login with local "
                            + "Credentials\" screen. This is used for direct, non-tunneled connections — if you "
                            + "need to reach a database that sits behind a bastion host, see the SSH section "
                            + "instead.\n\n"
                            + "Fill in the following fields:\n"
                            + "• Connection Profile — choose an existing saved profile from the dropdown, or "
                            + "select the \"+\" button to create a new one.\n"
                            + "• Host — the address of your MySQL server (use \"localhost\" for a database "
                            + "running on your own machine).\n"
                            + "• Port — the MySQL port, typically 3306.\n"
                            + "• MySQL Username / MySQL Password — the credentials for the MySQL account you "
                            + "want to connect as. These are passed straight through to the server and are "
                            + "never sent anywhere else.\n"
                            + "• User Initials — a short tag (e.g. \"EB\") used elsewhere in the app to label "
                            + "who made a given change, useful when a profile is shared across a team.\n\n"
                            + "Check \"Remember details for this profile\" to have Free My Query securely store "
                            + "these values locally so you don't have to re-enter them next time you pick this "
                            + "profile. Select \"Connect\" once every field is filled in. On success, Free My "
                            + "Query reads the schema over the connection and renders every table it finds as "
                            + "an interactive ER diagram.",
                    "assets/docs/getting-started/first-connection.png"),

            new DocPage("credentials", "Managing Saved Credentials",
                    "Saved connection profiles live under Settings → Credentials, and can also be reached "
                            + "directly from the key icon in the left-hand rail. This screen is the central place "
                            + "to review, edit, or remove any profile you've previously saved, without having to "
                            + "reconnect first.\n\n"
                            + "Use the dropdown at the top to switch between profiles, the \"+\" button to add a "
                            + "new blank profile, and the \"x\" button to delete the profile currently shown. "
                            + "Each profile stores:\n"
                            + "• Host URL — the full JDBC connection string, including any driver options such "
                            + "as allowMultiQueries, useSSL, and allowPublicKeyRetrieval.\n"
                            + "• User — the MySQL username for this profile.\n"
                            + "• Password — stored securely; left blank here means you'll be prompted at connect "
                            + "time.\n"
                            + "• Initials — the short tag shown alongside changes made under this profile.\n\n"
                            + "Select \"Test & Save\" to verify the connection details are valid before they're "
                            + "written to disk. If the test fails, Free My Query will tell you whether the "
                            + "problem was the host, the credentials, or the network, so you can correct just "
                            + "that field rather than re-entering everything.",
                    "assets/docs/getting-started/credentials.png"),

            new DocPage("troubleshoot-first-connection", "Troubleshooting First Connection",
                    "If a connection attempt fails, work through the following in order before assuming the "
                            + "database itself is misconfigured:\n\n"
                            + "1. Incorrect host or port — double-check that the host is reachable from your "
                            + "machine and that the port matches what the server is actually listening on "
                            + "(3306 is the MySQL default, but administrators frequently change it).\n"
                            + "2. Firewall rules — a firewall on either the client or the server side can block "
                            + "the MySQL port even when the host is reachable for other traffic. Confirm the "
                            + "port is open in both directions.\n"
                            + "3. Remote-login privileges — the MySQL user account must be granted permission "
                            + "to log in from your IP address or host name, not just from \"localhost\". Ask a "
                            + "database administrator to confirm the account's host grants (e.g. "
                            + "'user'@'%' vs 'user'@'localhost').\n"
                            + "4. Driver options — if the server requires a specific SSL or authentication mode, "
                            + "make sure the corresponding option (useSSL, allowPublicKeyRetrieval, etc.) is set "
                            + "correctly in the saved profile's Host URL.\n\n"
                            + "If all of the above check out and the connection still fails, the error message "
                            + "returned from \"Test & Save\" will include the raw driver exception, which is the "
                            + "fastest way to identify the exact point of failure.",
                    null)
    ));
}
