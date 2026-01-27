package gg.pigraid.discordlink.api.models;

/**
 * Request model for unlinking Discord account
 */
public class UnlinkRequest {
    private String discordId;

    public UnlinkRequest(String discordId) {
        this.discordId = discordId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }
}
