package GUI.Settings.Documentation;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class SettingsAppDocs {
    public static final DocCategory CATEGORY = new DocCategory("app-settings", "Settings", List.of(

            new DocPage("account-settings", "Account Settings",
                    "Reach Account Settings from the gear icon at the bottom of the sidebar, then the "
                            + "\"Account\" tab in the Settings panel. This screen covers your Free My Query "
                            + "account itself, distinct from any MySQL connection profiles managed elsewhere.\n\n"
                            + "The Account card shows the email address your account is registered under. The "
                            + "Subscription card shows your current plan Status (e.g. Active) and the date it "
                            + "next Renews. Select \"Manage billing\" to open your billing portal to update a "
                            + "payment method or change plans, or \"Refresh status\" if you've just made a "
                            + "billing change and want the app to re-check your current subscription state "
                            + "immediately rather than waiting for its normal sync interval.\n\n"
                            + "The rest of the Settings panel — Theme, Docs, Give Feedback, and Version News — "
                            + "is reachable from the same left-hand list without leaving this screen.",
                    "assets/docs/settings/account.png"),

            new DocPage("theme-settings", "Theme",
                    "Open Settings → Theme to choose a colour palette for the whole application. Palettes are "
                            + "grouped by mood — DARK and MELLOW sections are shown, with more groups available "
                            + "by scrolling — and each row previews its palette as a strip of swatches so you "
                            + "can compare options at a glance before committing.\n\n"
                            + "Select any palette row (for example, \"Neon Tokyo\", \"Dust and Rust\", or "
                            + "\"Moss and Pine\") to apply it immediately across every screen in the app, "
                            + "including the schema diagram, sidebar, and documentation viewer you're reading "
                            + "right now. Several palettes have a matching \"(Dark)\" or \"(Darker)\" variant, "
                            + "which keep the same accent colours but shift the background toward a deeper "
                            + "black for lower-light environments.\n\n"
                            + "Theme changes are applied live and take effect instantly — there is no separate "
                            + "save step, and your selection is remembered the next time you open the app.",
                    "assets/docs/settings/theme.png")
    ));
}
