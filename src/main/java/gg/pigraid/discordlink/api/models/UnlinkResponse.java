package gg.pigraid.discordlink.api.models;

/**
 * Response model for unlink requests
 */
public class UnlinkResponse {
    private boolean success;
    private String unlinkedFrom;
    private String message;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getUnlinkedFrom() {
        return unlinkedFrom;
    }

    public void setUnlinkedFrom(String unlinkedFrom) {
        this.unlinkedFrom = unlinkedFrom;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
