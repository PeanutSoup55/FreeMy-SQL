package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class SshDocs {
    public static final DocCategory CATEGORY = new DocCategory("ssh", "SSH", List.of(

            new DocPage("ssh-first-connection", "First Connection",
                    "Use the SSH tab (the icon showing a monitor with an arrow, in the left rail) to reach a "
                            + "MySQL server that isn't directly exposed to your machine, by tunnelling through "
                            + "an intermediate host. The screen is split into two panels that together describe "
                            + "the full path your connection takes: Your Machine → SSH Server → MySQL.\n\n"
                            + "Under \"SSH Tunnel — the server gate\", enter the Host/IP and Port of the bastion "
                            + "or jump box you can reach directly, along with the Username and Password (or key, "
                            + "where supported) used to authenticate to that machine over SSH. Under \"MySQL "
                            + "Database — inside the server\", enter the DB Host, DB Port, DB Username, DB "
                            + "Password, and Database Name exactly as MySQL sees them from the far side of the "
                            + "tunnel — this is often \"127.0.0.1\" and \"3306\" if MySQL runs on the same "
                            + "machine as the SSH server.\n\n"
                            + "Before connecting, you can save time on future connections: use \"Saved "
                            + "Connections\" at the top to load a previously saved profile from the dropdown, or "
                            + "give the current details a name in \"Save as\" so they can be reloaded later. "
                            + "Select \"Connect\" to open the tunnel for this session only, or \"Connect & Save\" "
                            + "to open it and store the profile at the same time.",
                    "assets/docs/ssh/first-connection.png"),

            new DocPage("ssh-connected-status", "Connection Status",
                    "Once a tunnel is established, Free My Query switches to a live status view confirming "
                            + "\"Connected — <database> @ <host>\" at the top, with the three-stage path (Your "
                            + "Machine → SSH Server → MySQL) now shown in green to indicate every hop is up.\n\n"
                            + "Three summary cards report on the tunnel itself: Tunnel Uptime (how long the "
                            + "session has been open), Bridge Port (the local port Free My Query is forwarding "
                            + "through), and Setup Time (how long the handshake took to establish). Below that, "
                            + "\"Server Info\" is pulled live from the connected MySQL instance and includes its "
                            + "version, overall server uptime, active thread count, configured max connections, "
                            + "open table count, slow query count, character set, and the size and table count "
                            + "of the specific database you connected to.\n\n"
                            + "A green status line at the bottom confirms the tunnel is secured and the "
                            + "connection is live. Select \"Disconnect\" in the top-right at any time to close "
                            + "the tunnel; the remote schema will then move from the REMOTE section back to "
                            + "being unreachable until you reconnect.",
                    "assets/docs/ssh/connected-status.png"),

            new DocPage("ssh-troubleshoot", "Troubleshoot",
                    "Most SSH tunnel failures fall into one of a few categories:\n\n"
                            + "1. Wrong port — the SSH port defaults to 22, but many hardened servers move it "
                            + "elsewhere; confirm the port with whoever administers the bastion host before "
                            + "assuming the tunnel itself is broken.\n"
                            + "2. Key permissions — if you're authenticating with a private key rather than a "
                            + "password, most SSH servers will silently reject a key file that has overly "
                            + "permissive filesystem permissions. Ensure the key is only readable by your user.\n"
                            + "3. The remote MySQL user not being allowed to connect from the tunnel's bind "
                            + "address — because traffic arrives at MySQL from the SSH server's local address "
                            + "(often 127.0.0.1) rather than your real IP, the MySQL account must have a host "
                            + "grant that permits logins from that address, not just from your original machine.\n"
                            + "4. The DB Host/DB Port pair being entered from the wrong perspective — remember "
                            + "these describe how the SSH server reaches MySQL, not how your own machine would.\n\n"
                            + "If the tunnel opens but the MySQL step fails, the error returned will indicate "
                            + "whether the SSH hop or the database hop was the point of failure, which tells you "
                            + "which half of the two-panel form to revisit.",
                    null),

            new DocPage("ssh-cloning", "Cloning Schemas / Early Version Control",
                    "Once connected to a remote schema over SSH, it appears under a REMOTE section in the "
                            + "sidebar alongside your LOCAL schemas. From here you can clone a remote schema "
                            + "locally to snapshot its structure before making changes — a lightweight stand-in "
                            + "for proper version control until migrations are supported natively.\n\n"
                            + "Cloning copies the remote schema's table structure into a local schema you can "
                            + "freely edit and experiment on without touching production data, and gives you a "
                            + "known-good reference point to diff against if you need to understand what changed "
                            + "after editing tables in place. This is particularly useful before a risky "
                            + "operation like a column type change or table removal on a remote database, where "
                            + "recovering from a mistake would otherwise require a full database restore.\n\n"
                            + "Because cloning only captures structure, remember to pair it with your own backup "
                            + "process if the data itself also needs to be preserved.",
                    null)
    ));
}
