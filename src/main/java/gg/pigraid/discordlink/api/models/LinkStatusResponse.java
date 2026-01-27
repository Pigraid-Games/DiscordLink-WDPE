package gg.pigraid.discordlink.api.models;

/**
 * Response model for link status queries
 */
public class LinkStatusResponse {
    private boolean linked;
    private String xuid;
    private String username;
    private String linkedAt;

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
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

    public String getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(String linkedAt) {
        this.linkedAt = linkedAt;
    }
}
