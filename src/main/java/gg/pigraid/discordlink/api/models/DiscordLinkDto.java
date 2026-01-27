package gg.pigraid.discordlink.api.models;

/**
 * DTO for Discord link information from AccountService
 */
public class DiscordLinkDto {
    private String discordId;
    private String discordUsername;
    private String discordDisplayName;
    private String linkedAt;

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getDiscordUsername() {
        return discordUsername;
    }

    public void setDiscordUsername(String discordUsername) {
        this.discordUsername = discordUsername;
    }

    public String getDiscordDisplayName() {
        return discordDisplayName;
    }

    public void setDiscordDisplayName(String discordDisplayName) {
        this.discordDisplayName = discordDisplayName;
    }

    public String getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(String linkedAt) {
        this.linkedAt = linkedAt;
    }
}
