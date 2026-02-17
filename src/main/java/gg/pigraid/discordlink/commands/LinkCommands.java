package gg.pigraid.discordlink.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import gg.pigraid.accountadapter.models.AccountDto;
import gg.pigraid.discordlink.DiscordLinkPlugin;
import gg.pigraid.discordlink.api.models.GenerateCodeResponse;
import gg.pigraid.discordlink.api.models.UnlinkResponse;
import gg.pigraid.discordlink.forms.LinkForms;
import gg.pigraid.feedbackutils.wdpe.SoundUtil;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

/**
 * Commands for Discord-Minecraft account linking
 * Handles /link, /unlink, and /linkstatus commands
 */
public class LinkCommands extends Command {

    private final DiscordLinkPlugin plugin;

    public LinkCommands(DiscordLinkPlugin plugin) {
        super("link", CommandSettings.builder()
                .setUsageMessage("/link - Link your Minecraft account to Discord")
                .setDescription("Link your account to Discord")
                .setAliases(new String[]{"discord", "discordlink"})
                .build());
        this.plugin = plugin;
    }

    @Override
    public boolean onExecute(CommandSender commandSender, String s, String[] args) {
        // Check if sender is a player
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(plugin.getI18n().tr("en_US", "command.onlyPlayers"));
            return false;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Get player's language
        String language = getPlayerLanguage(player);

        // Handle subcommands
        if (args.length > 0) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "unlink":
                    handleUnlinkCommand(player, language);
                    return true;
                case "status":
                    handleStatusCommand(player, language);
                    return true;
                default:
                    // Unknown subcommand, treat as main /link command
                    break;
            }
        }

        // Main /link command - generate verification code
        handleLinkCommand(player, language);
        return true;
    }

    /**
     * Handle /link command - generate verification code
     */
    private void handleLinkCommand(ProxiedPlayer player, String language) {
        String xuid = player.getLoginData().getXuid();
        String username = player.getName();

        // Show "generating..." message
        player.sendMessage(plugin.getI18n().tr(language, "link.command.generating"));

        // Call API to generate code
        plugin.getServiceClient().generateVerificationCode(xuid, username)
            .thenAccept(response -> {
                // Schedule response handling on proxy thread
                plugin.getProxy().getScheduler().scheduleAsync(() -> {
                    handleGenerateCodeResponse(player, language, response);
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().error("Error generating verification code: " + ex.getMessage());
                SoundUtil.playError(player);
                player.sendMessage(plugin.getI18n().tr(language, "link.command.error"));
                return null;
            });
    }

    /**
     * Handle the response from code generation API
     */
    private void handleGenerateCodeResponse(ProxiedPlayer player, String language, GenerateCodeResponse response) {
        if (!response.isSuccess()) {
            // Handle errors
            String error = response.getError();
            if ("ALREADY_LINKED".equals(error)) {
                // Player is already linked
                SoundUtil.playError(player);
                String message = response.getMessage();
                player.sendMessage(plugin.getI18n().tr(language, "link.command.already_linked", message));
            } else if ("CODE_EXISTS".equals(error)) {
                // Player has a pending code - show it again
                String code = response.getCode();
                int expiresInSeconds = response.getExpiresInSeconds();

                if (plugin.getConfiguration().getBoolean("settings.use_form_ui", true)) {
                    // Show form UI with existing code
                    LinkForms.showLinkCodeForm(player, language, code, expiresInSeconds, plugin);
                } else {
                    // Send chat message
                    player.sendMessage(plugin.getI18n().tr(language, "link.command.code_exists", code, String.valueOf(expiresInSeconds)));
                }
            } else {
                // Generic error
                SoundUtil.playError(player);
                player.sendMessage(plugin.getI18n().tr(language, "link.command.error"));
                if (plugin.getConfiguration().getBoolean("debug", false)) {
                    player.sendMessage("§cDebug: " + response.getMessage());
                }
            }
        } else {
            // Success - show verification code
            String code = response.getCode();
            int expiresInSeconds = response.getExpiresInSeconds();

            if (plugin.getConfiguration().getBoolean("settings.use_form_ui", true)) {
                // Show form UI
                LinkForms.showLinkCodeForm(player, language, code, expiresInSeconds, plugin);
            } else {
                // Send chat messages
                player.sendMessage(plugin.getI18n().tr(language, "link.command.success.title"));
                player.sendMessage(plugin.getI18n().tr(language, "link.command.success.code", code));
                player.sendMessage(plugin.getI18n().tr(language, "link.command.success.instructions", code));
                player.sendMessage(plugin.getI18n().tr(language, "link.command.success.expires", String.valueOf(expiresInSeconds)));
                String discordUrl = plugin.getConfiguration().getString("discord.invite_url", "https://discord.gg/pigraid");
                player.sendMessage(plugin.getI18n().tr(language, "link.command.success.discord", discordUrl));
            }
        }
    }

    /**
     * Handle /link unlink command - unlink Discord account
     */
    private void handleUnlinkCommand(ProxiedPlayer player, String language) {
        String xuid = player.getLoginData().getXuid();

        // First, get the player's account to retrieve Discord ID
        plugin.getServiceClient().getAccountByXuid(xuid)
            .thenAccept(account -> {
                if (account == null) {
                    SoundUtil.playError(player);
                    player.sendMessage(plugin.getI18n().tr(language, "link.command.no_account"));
                    return;
                }

                // Check if account has Discord link (we need to parse the settings)
                // For now, we'll show "not linked" if we can't get Discord info
                // This is a limitation - ideally AccountDto should expose DiscordLink
                player.sendMessage(plugin.getI18n().tr(language, "unlink.command.not_linked"));
                player.sendMessage("§eNote: Unlinking from Discord is currently only supported via Discord bot.");
                player.sendMessage("§eUse /unlink command in Discord to unlink your account.");

                // TODO: Once AccountDto exposes DiscordLink, implement proper unlinking
                // String discordId = account.getSettings().getDiscordLink().getDiscordId();
                // if (discordId == null) {
                //     player.sendMessage(plugin.getI18n().tr(language, "unlink.command.not_linked"));
                //     return;
                // }
                //
                // plugin.getServiceClient().unlinkDiscordAccount(discordId)
                //     .thenAccept(response -> {
                //         if (response.isSuccess()) {
                //             player.sendMessage(plugin.getI18n().tr(language, "unlink.command.success"));
                //         } else {
                //             player.sendMessage(plugin.getI18n().tr(language, "unlink.command.error"));
                //         }
                //     });
            })
            .exceptionally(ex -> {
                plugin.getLogger().error("Error fetching account for unlink: " + ex.getMessage());
                SoundUtil.playError(player);
                player.sendMessage(plugin.getI18n().tr(language, "unlink.command.error"));
                return null;
            });
    }

    /**
     * Handle /link status command - check link status
     */
    private void handleStatusCommand(ProxiedPlayer player, String language) {
        String xuid = player.getLoginData().getXuid();

        // Fetch account to check Discord link
        plugin.getServiceClient().getAccountByXuid(xuid)
            .thenAccept(account -> {
                plugin.getProxy().getScheduler().scheduleAsync(() -> {
                    if (account == null) {
                        player.sendMessage(plugin.getI18n().tr(language, "link.command.no_account"));
                        return;
                    }

                    // Check if Discord link exists
                    // For now, show "not linked" message
                    // This is a limitation until AccountDto exposes DiscordLink
                    player.sendMessage(plugin.getI18n().tr(language, "status.command.not_linked"));

                    // TODO: Once AccountDto exposes DiscordLink, show proper status
                    // DiscordLinkDto discordLink = account.getSettings().getDiscordLink();
                    // if (discordLink != null && discordLink.getDiscordId() != null) {
                    //     String discordUsername = discordLink.getDiscordUsername();
                    //     String linkedAt = discordLink.getLinkedAt();
                    //     player.sendMessage(plugin.getI18n().tr(language, "status.command.linked", discordUsername));
                    //     player.sendMessage(plugin.getI18n().tr(language, "status.command.linked_since", linkedAt));
                    // } else {
                    //     player.sendMessage(plugin.getI18n().tr(language, "status.command.not_linked"));
                    // }
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().error("Error fetching account for status: " + ex.getMessage());
                SoundUtil.playError(player);
                player.sendMessage(plugin.getI18n().tr(language, "status.command.error"));
                return null;
            });
    }

    /**
     * Get player's language from login data
     */
    private String getPlayerLanguage(ProxiedPlayer player) {
        try {
            JsonObject extraData = player.getLoginData().getClientData();
            if (extraData.has("LanguageCode")) {
                return extraData.get("LanguageCode").getAsString();
            }
        } catch (JsonParseException e) {
            // Fallback to en_US if error
        }
        return "en_US";
    }
}
