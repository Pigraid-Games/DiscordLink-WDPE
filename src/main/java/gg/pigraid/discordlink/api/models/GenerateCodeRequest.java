package gg.pigraid.discordlink.api.models;

/**
 * Request model for generating Discord verification codes
 */
public class GenerateCodeRequest {
    private String xuid;
    private String username;

    public GenerateCodeRequest(String xuid, String username) {
        this.xuid = xuid;
        this.username = username;
    }

    public String getXuid() {
        return xuid;
    }

    public void setXuid(String xuid) {
        this.xuid = xuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
