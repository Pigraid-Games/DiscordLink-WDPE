package gg.pigraid.discordlink.forms;

import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import gg.pigraid.discordlink.DiscordLinkPlugin;
import xxAROX.WDForms.forms.elements.Button;
import xxAROX.WDForms.forms.types.MenuForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Form UI for Discord linking
 */
public class LinkForms {

    /**
     * Show the verification code form to the player
     *
     * @param player Player to show form to
     * @param language Player's language code
     * @param code Verification code
     * @param expiresInSeconds Seconds until code expires
     * @param plugin Plugin instance
     */
    public static void showLinkCodeForm(ProxiedPlayer player, String language, String code, int expiresInSeconds, DiscordLinkPlugin plugin) {
        // Format expiry time
        String expiryText;
        if (expiresInSeconds >= 60) {
            int minutes = expiresInSeconds / 60;
            int seconds = expiresInSeconds % 60;
            expiryText = minutes + "m " + seconds + "s";
        } else {
            expiryText = expiresInSeconds + "s";
        }

        // Get Discord info from config
        String discordUrl = plugin.getConfiguration().getString("discord.invite_url", "https://discord.gg/pigraid");
        String botChannel = plugin.getConfiguration().getString("discord.bot_command_channel", "#bot-commands");

        // Build form content
        StringBuilder content = new StringBuilder();
        content.append("§l§6").append(code).append("§r\n\n");
        content.append(plugin.getI18n().tr(language, "link.form.instructions", code)).append("\n\n");
        content.append(plugin.getI18n().tr(language, "link.form.channel", botChannel)).append("\n");
        content.append(plugin.getI18n().tr(language, "link.form.expires", expiryText)).append("\n\n");
        content.append("§7").append(discordUrl);

        // Create buttons
        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button(
            plugin.getI18n().tr(language, "forms.button.ok"),
            null,
            button -> {
                // Form closed - do nothing
            }
        ));

        // Build and send form
        MenuForm.menu()
            .title(plugin.getI18n().tr(language, "link.form.title"))
            .content(content.toString())
            .buttons(buttons)
            .build()
            .sendTo(player);
    }
}
