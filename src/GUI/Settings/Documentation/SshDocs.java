package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class SshDocs {
    public static final DocCategory CATEGORY = new DocCategory("ssh", "SSH", List.of(
            new DocPage("ssh-first-connection", "First Connection",
                    "Set up an SSH tunnel to a remote MySQL instance using a host, port, and private key or "
                            + "password authentication.",
                    "assets/docs/ssh/first-connection.png"),
            new DocPage("ssh-troubleshoot", "Troubleshoot",
                    "Common SSH tunnel issues: wrong port, key permissions, or the remote MySQL user not "
                            + "being allowed to connect from the tunnel's bind address.",
                    "assets/docs/ssh/troubleshoot.png"),
            new DocPage("ssh-cloning", "Cloning Schemas / Early Version Control",
                    "Clone a remote schema locally to snapshot its structure before making changes — a "
                            + "lightweight stand-in for version control until proper migrations are supported.",
                    "assets/docs/ssh/cloning.png")
    ));
}
