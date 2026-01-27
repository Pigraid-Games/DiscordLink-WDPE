package gg.pigraid.discordlink;

import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.utils.config.YamlConfig;
import gg.pigraid.accountadapter.AccountAdapter;
import gg.pigraid.discordlink.api.DiscordLinkServiceClient;
import gg.pigraid.discordlink.commands.LinkCommands;
import gg.pigraid.waterdogi18n.WaterdogI18n;

import java.io.File;

/**
 * DiscordLink-WDPE Plugin
 * Enables Discord-Minecraft account linking at the proxy level
 *
 * Features:
 * - /link command to generate verification codes
 * - /link unlink to unlink Discord account (via Discord bot recommended)
 * - /link status to check link status
 * - Multi-language support via WaterdogI18n
 * - Form UI or chat message display
 * - Integration with NotificationDispatcher-WDPE
 */
public class DiscordLinkPlugin extends Plugin {

    private static DiscordLinkPlugin instance;
    private static AccountAdapter accountAdapter;
    private static WaterdogI18n i18n;
    private DiscordLinkServiceClient serviceClient;
    private YamlConfig config;
    private Object notificationDispatcher;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config if it doesn't exist
        this.saveResource("config.yml");

        // Load config
        this.config = new YamlConfig(new File(this.getDataFolder(), "config.yml").toPath());

        // Initialize Discord Link Service Client
        String baseUrl = config.getString("account_service.base_url", "http://localhost:5005");
        String apiKey = config.getString("account_service.api_key", "");
        boolean debugRequests = config.getBoolean("account_service.debug_requests", false);

        // SECURITY: Validate that API key is configured
        if (apiKey.isEmpty()) {
            this.getLogger().error("SECURITY ERROR: API key not configured in config.yml!");
            this.getLogger().error("Please configure account_service.api_key");
            this.getLogger().error("Plugin will not function properly without an API key.");
            return;
        }

        this.serviceClient = new DiscordLinkServiceClient(baseUrl, apiKey, debugRequests);

        // Test connection to AccountService
        this.getProxy().getScheduler().scheduleAsync(() -> {
            if (this.serviceClient.testConnection()) {
                this.getLogger().info("Successfully connected to AccountService at " + baseUrl);
            } else {
                this.getLogger().warn("Failed to connect to AccountService at " + baseUrl);
            }
        });

        // Initialize AccountAdapter for multi-language support
        try {
            accountAdapter = new AccountAdapter(baseUrl, apiKey, debugRequests);

            if (accountAdapter.testConnection()) {
                this.getLogger().info("AccountAdapter initialized successfully");
            } else {
                this.getLogger().warn("AccountAdapter connection test failed - language preferences may be unavailable");
            }
        } catch (Exception e) {
            this.getLogger().warn("Failed to initialize AccountAdapter: " + e.getMessage());
        }

        // Initialize WaterdogI18n for translations
        File langFolder = new File(this.getDataFolder(), "language");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Save all language files from resources (only those that exist)
        String[] languages = {"id_ID", "da_DK", "de_DE", "en_GB", "en_US", "es_ES", "es_MX",
                              "fr_CA", "fr_FR", "it_IT", "hu_HU", "nl_NL", "nb_NO", "pl_PL",
                              "pt_BR", "pt_PT", "sk_SK", "fi_FI", "sv_SE", "tr_TR", "cs_CZ",
                              "el_GR", "bg_BG", "ru_RU", "uk_UA", "ja_JP", "zh_CN", "zh_TW",
                              "ko_KR", "th_TH"};
        int savedLanguages = 0;
        for (String lang : languages) {
            try {
                this.saveResource("language/" + lang + ".lang");
                savedLanguages++;
            } catch (Exception e) {
                // Language file doesn't exist in JAR - skip it
                this.getLogger().debug("Language file not found: " + lang + ".lang (using fallback)");
            }
        }
        this.getLogger().info("Saved " + savedLanguages + " language files");

        i18n = new WaterdogI18n("DiscordLink", langFolder, accountAdapter);
        i18n.loadAllLanguages();
        i18n.setDefaultLanguage("en_US");

        this.getLogger().info("Multi-language support enabled! Loaded languages: " + i18n.getLoadedLanguages());

        // Get NotificationDispatcher plugin (optional)
        try {
            for (Plugin plugin : this.getProxy().getPluginManager().getPlugins()) {
                if (plugin.getName().equals("NotificationDispatcher-WDPE")) {
                    this.notificationDispatcher = plugin;
                    break;
                }
            }
            if (this.notificationDispatcher != null) {
                this.getLogger().info("NotificationDispatcher-WDPE integration enabled");
            } else {
                this.getLogger().warn("NotificationDispatcher-WDPE not found - notifications disabled");
            }
        } catch (Exception e) {
            this.getLogger().warn("Failed to load NotificationDispatcher-WDPE: " + e.getMessage());
        }

        // Register commands
        this.getProxy().getCommandMap().registerCommand(new LinkCommands(this));

        this.getLogger().info("DiscordLink-WDPE Plugin has been enabled!");
        this.getLogger().info("Players can now use /link to connect their Discord accounts");
    }

    @Override
    public void onDisable() {
        if (this.serviceClient != null) {
            this.serviceClient.close();
        }
        if (accountAdapter != null) {
            accountAdapter.shutdown();
        }
        this.getLogger().info("DiscordLink-WDPE Plugin has been disabled!");
    }

    public static DiscordLinkPlugin getInstance() {
        return instance;
    }

    public DiscordLinkServiceClient getServiceClient() {
        return serviceClient;
    }

    public YamlConfig getConfiguration() {
        return config;
    }

    public static WaterdogI18n getI18n() {
        return i18n;
    }

    public Object getNotificationDispatcher() {
        return notificationDispatcher;
    }

    public static AccountAdapter getAccountAdapter() {
        return accountAdapter;
    }

    /**
     * Send a notification to a player if they're online and NotificationDispatcher is available
     * Can be used when Discord linking is completed (called from external webhook/API)
     *
     * @param targetXuid The XUID of the target player
     * @param notificationType The notification type (e.g., "Success")
     * @param message The message to send
     */
    public void sendNotification(String targetXuid, String notificationType, String message) {
        if (notificationDispatcher == null) {
            return; // NotificationDispatcher not available
        }

        try {
            // Find player by XUID
            dev.waterdog.waterdogpe.player.ProxiedPlayer targetPlayer = null;
            for (dev.waterdog.waterdogpe.player.ProxiedPlayer player : this.getProxy().getPlayers().values()) {
                if (player.getLoginData().getXuid().equals(targetXuid)) {
                    targetPlayer = player;
                    break;
                }
            }

            if (targetPlayer == null) {
                return; // Player not online
            }

            // Get NotificationDispatcher's class loader
            ClassLoader notifClassLoader = ((Plugin) notificationDispatcher).getClass().getClassLoader();

            // Use reflection to load NotificationDispatcher classes
            Class<?> notificationAPIClass = Class.forName("dev.pigraid.notificationdispatcher.NotificationAPI", true, notifClassLoader);
            Class<?> notificationTypeClass = Class.forName("dev.pigraid.notificationdispatcher.models.NotificationType", true, notifClassLoader);
            Class<?> notificationClass = Class.forName("dev.pigraid.notificationdispatcher.models.Notification", true, notifClassLoader);

            // Get the notification type enum value
            Object typeValue = null;
            for (Object enumConstant : notificationTypeClass.getEnumConstants()) {
                if (enumConstant.toString().equals(notificationType)) {
                    typeValue = enumConstant;
                    break;
                }
            }

            if (typeValue == null) {
                this.getLogger().warn("Unknown notification type: " + notificationType);
                return;
            }

            // Create a Notification object using reflection (it's a record)
            java.lang.reflect.Constructor<?> notificationConstructor = notificationClass.getConstructor(String.class, notificationTypeClass);
            Object notification = notificationConstructor.newInstance(message, typeValue);

            // Call sendMessage method
            java.lang.reflect.Method sendMethod = notificationAPIClass.getMethod(
                "sendMessage",
                dev.waterdog.waterdogpe.player.ProxiedPlayer.class,
                notificationClass
            );
            sendMethod.invoke(null, targetPlayer, notification);

        } catch (Exception e) {
            this.getLogger().warn("Failed to send notification: " + e.getMessage());
        }
    }
}
