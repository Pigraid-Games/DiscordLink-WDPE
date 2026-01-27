# DiscordLink-WDPE

Discord-Minecraft account linking plugin for WaterdogPE proxy.

## Overview

This plugin enables players to link their Minecraft Bedrock accounts with Discord accounts using a verification code flow. The linking happens at the proxy level, making it consistent across all backend servers.

## Features

- `/link` command to generate verification codes
- `/link unlink` to unlink Discord account (via Discord bot recommended)
- `/link status` to check current link status
- Multi-language support (30 languages via WaterdogI18n)
- Form UI or chat message display (configurable)
- Integration with NotificationDispatcher-WDPE
- Secure API communication with AccountService

## Dependencies

### Required
- **WaterdogPE** 2.0.4+ (provided)
- **AccountService** (must be running with Discord linking API)

### Optional (Soft Dependencies)
- **AccountAdapter-WDPE** - For language detection and account access
- **WaterdogI18n** - For multi-language support
- **NotificationDispatcher-WDPE** - For in-game notifications when linking completes

### Bundled
- **WD-Forms** - For Form UI (bundled in plugin)
- **OkHttp3** - For HTTP requests (shaded)
- **Gson** - For JSON processing (not shaded, WaterdogPE provides it)

## Installation

1. Build the plugin:
   ```bash
   ./deploy.sh
   ```

2. The JAR will be copied to `Nukkit-Network/WaterdogPE/plugins/`

3. Configure `config.yml` (see Configuration section)

4. Restart WaterdogPE proxy

## Configuration

Edit `plugins/DiscordLink-WDPE/config.yml`:

```yaml
account_service:
  base_url: "http://account-service:5005"
  api_key: "YOUR_API_KEY_HERE"  # REQUIRED
  timeout: 15
  debug_requests: false

discord:
  invite_url: "https://discord.gg/pigraid"
  bot_command_channel: "#bot-commands"

settings:
  use_form_ui: true
  enable_notifications: true

debug: false
```

## Usage

### For Players

1. Join the server
2. Run `/link` command
3. A 6-character verification code will be displayed (e.g., A7B3C9)
4. Go to Discord and use `/verify A7B3C9` in the bot commands channel
5. Accounts are now linked!

### Commands

- `/link` - Generate a verification code
- `/link status` - Check if your account is linked
- `/link unlink` - Unlink Discord account (Discord bot method recommended)

Aliases: `/discord`, `/discordlink`

## Integration with Discord Bot

This plugin works together with the Discord bot (ModerationService) which provides:
- `/verify <code>` - Complete the linking by entering the code
- `/unlink` - Unlink Discord account from Minecraft
- `/linkstatus` - Check linking status from Discord

See `docs/planning/discord-minecraft-linking-plan.md` for full architecture.

## API Communication

The plugin communicates with AccountService endpoints:
- `POST /api/accounts/discord/generate-code` - Generate verification code
- `POST /api/accounts/discord/unlink` - Unlink account
- `GET /api/accounts/{xuid}` - Get account info for status checks

All requests include `X-Api-Key` header for authentication.

## Multi-Language Support

The plugin supports 30 languages via WaterdogI18n. Language files are automatically extracted to `plugins/DiscordLink-WDPE/language/`.

To add a new language:
1. Copy `language/en_US.lang`
2. Translate all strings
3. Save as `language/<locale>.lang` (e.g., `es_ES.lang`)
4. Restart the plugin

## Development

### Build Requirements
- Java 17+
- Maven 3.8+

### Build Commands
```bash
# Build plugin
mvn clean package

# Build and deploy
./deploy.sh
```

### Project Structure
```
DiscordLink-WDPE/
├── pom.xml
├── deploy.sh
├── src/main/
│   ├── java/gg/pigraid/discordlink/
│   │   ├── DiscordLinkPlugin.java      # Main plugin class
│   │   ├── commands/
│   │   │   └── LinkCommands.java       # Command handlers
│   │   ├── api/
│   │   │   ├── DiscordLinkServiceClient.java  # HTTP client
│   │   │   └── models/                 # API DTOs
│   │   └── forms/
│   │       └── LinkForms.java          # Form UI
│   └── resources/
│       ├── plugin.yml
│       ├── config.yml
│       └── language/                   # Language files
└── lib/
    └── WD-Forms-Plugin-2.0-SNAPSHOT.jar
```

## Security Considerations

- All API requests require authentication via API key
- Verification codes expire after 5 minutes (configurable in AccountService)
- Codes are cryptographically random and one-time use
- API key must be configured or plugin will not function

## Troubleshooting

### Plugin doesn't load
- Check WaterdogPE version (requires 2.0.4+)
- Verify all dependencies are installed
- Check console for error messages

### "Failed to connect to AccountService"
- Verify AccountService is running
- Check `base_url` in config.yml
- Verify API key is correct
- Check network connectivity

### Verification codes not working
- Verify AccountService has Discord linking API endpoints
- Check AccountService logs for errors
- Verify Discord bot is running and connected

### Language not working
- Check if language file exists in `language/` folder
- Verify WaterdogI18n plugin is installed
- Check player's client language code

## License

Copyright (c) 2026 Pigraid Team. All rights reserved.

## Support

For issues, feature requests, or questions:
- GitHub: https://github.com/pigraid
- Discord: https://discord.gg/pigraid
