package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class GettingStartedDocs {
    public static final DocTypes.DocCategory CATEGORY = new DocTypes.DocCategory("getting-started", "Getting Started", List.of(
            new DocPage("first-connection", "First Connection",
                    "Connect to your first MySQL database. Enter your host, port, username, and password, "
                            + "then Free My Query will read the schema and render it as an interactive diagram.",
                    "assets/docs/getting-started/first-connection.png"),
            new DocPage("troubleshoot-first-connection", "Troubleshooting First Connection",
                    "Common causes of connection failure: incorrect host/port, firewall rules blocking the "
                            + "MySQL port, or a user account that doesn't have remote-login privileges.",
                    "assets/docs/getting-started/troubleshoot-first-connection.png")
    ));
}
