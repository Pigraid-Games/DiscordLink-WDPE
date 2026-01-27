package gg.pigraid.discordlink.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.pigraid.accountadapter.models.AccountDto;
import gg.pigraid.discordlink.api.models.*;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for communicating with AccountService Discord linking API
 */
public class DiscordLinkServiceClient {
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String apiKey;
    private final boolean debugRequests;

    public DiscordLinkServiceClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, false);
    }

    public DiscordLinkServiceClient(String baseUrl, String apiKey, boolean debugRequests) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.debugRequests = debugRequests;
        this.gson = new Gson();

        // PERFORMANCE: Connection pooling for efficient HTTP requests
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
                .retryOnConnectionFailure(false)
                .build();
    }

    /**
     * Test connection to AccountService API
     */
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                .url(baseUrl + "/api/health")
                .header("X-Api-Key", apiKey)
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                // Accept any response (even 401/404) as "connected"
                // Just checking if the service is reachable
                return response.code() > 0;
            }
        } catch (Exception e) {
            if (debugRequests) {
                System.err.println("Failed to test connection to AccountService: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Generate a Discord verification code for a player (async)
     *
     * @param xuid Player's XUID
     * @param username Player's username
     * @return CompletableFuture with GenerateCodeResponse
     */
    public CompletableFuture<GenerateCodeResponse> generateVerificationCode(String xuid, String username) {
        CompletableFuture<GenerateCodeResponse> future = new CompletableFuture<>();

        try {
            GenerateCodeRequest requestObj = new GenerateCodeRequest(xuid, username);

            RequestBody body = RequestBody.create(
                gson.toJson(requestObj),
                MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                .url(baseUrl + "/api/accounts/discord/generate-code")
                .header("X-Api-Key", apiKey)
                .post(body)
                .build();

            if (debugRequests) {
                System.out.println("Sending request to generate-code for xuid: " + xuid);
            }

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (debugRequests) {
                        System.err.println("Failed to generate verification code: " + e.getMessage());
                    }
                    GenerateCodeResponse errorResponse = new GenerateCodeResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Failed to connect to account service");
                    future.complete(errorResponse);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        String responseBody = response.body() != null ? response.body().string() : "";

                        if (debugRequests) {
                            System.out.println("Generate-code response: " + response.code() + " - " + responseBody);
                        }

                        GenerateCodeResponse result = gson.fromJson(responseBody, GenerateCodeResponse.class);
                        if (result == null) {
                            result = new GenerateCodeResponse();
                            result.setSuccess(false);
                            result.setMessage("Invalid response from server");
                        }
                        future.complete(result);
                    } catch (Exception e) {
                        if (debugRequests) {
                            System.err.println("Error parsing generate-code response: " + e.getMessage());
                        }
                        GenerateCodeResponse errorResponse = new GenerateCodeResponse();
                        errorResponse.setSuccess(false);
                        errorResponse.setMessage("Error processing response");
                        future.complete(errorResponse);
                    }
                }
            });
        } catch (Exception e) {
            if (debugRequests) {
                System.err.println("Exception in generateVerificationCode: " + e.getMessage());
            }
            GenerateCodeResponse errorResponse = new GenerateCodeResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Unexpected error: " + e.getMessage());
            future.complete(errorResponse);
        }

        return future;
    }

    /**
     * Unlink Discord account (called from in-game, requires Discord ID from Account)
     * Note: This requires fetching the account first to get the Discord ID
     *
     * @param discordId Discord user ID
     * @return CompletableFuture with UnlinkResponse
     */
    public CompletableFuture<UnlinkResponse> unlinkDiscordAccount(String discordId) {
        CompletableFuture<UnlinkResponse> future = new CompletableFuture<>();

        try {
            UnlinkRequest requestObj = new UnlinkRequest(discordId);

            RequestBody body = RequestBody.create(
                gson.toJson(requestObj),
                MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                .url(baseUrl + "/api/accounts/discord/unlink")
                .header("X-Api-Key", apiKey)
                .post(body)
                .build();

            if (debugRequests) {
                System.out.println("Sending unlink request for Discord ID: " + discordId);
            }

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (debugRequests) {
                        System.err.println("Failed to unlink Discord account: " + e.getMessage());
                    }
                    UnlinkResponse errorResponse = new UnlinkResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Failed to connect to account service");
                    future.complete(errorResponse);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        String responseBody = response.body() != null ? response.body().string() : "";

                        if (debugRequests) {
                            System.out.println("Unlink response: " + response.code() + " - " + responseBody);
                        }

                        UnlinkResponse result = gson.fromJson(responseBody, UnlinkResponse.class);
                        if (result == null) {
                            result = new UnlinkResponse();
                            result.setSuccess(false);
                            result.setMessage("Invalid response from server");
                        }
                        future.complete(result);
                    } catch (Exception e) {
                        if (debugRequests) {
                            System.err.println("Error parsing unlink response: " + e.getMessage());
                        }
                        UnlinkResponse errorResponse = new UnlinkResponse();
                        errorResponse.setSuccess(false);
                        errorResponse.setMessage("Error processing response");
                        future.complete(errorResponse);
                    }
                }
            });
        } catch (Exception e) {
            if (debugRequests) {
                System.err.println("Exception in unlinkDiscordAccount: " + e.getMessage());
            }
            UnlinkResponse errorResponse = new UnlinkResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Unexpected error: " + e.getMessage());
            future.complete(errorResponse);
        }

        return future;
    }

    /**
     * Get account by XUID (to retrieve Discord link info)
     *
     * @param xuid Player's XUID
     * @return CompletableFuture with AccountDto
     */
    public CompletableFuture<AccountDto> getAccountByXuid(String xuid) {
        CompletableFuture<AccountDto> future = new CompletableFuture<>();

        try {
            Request request = new Request.Builder()
                .url(baseUrl + "/api/accounts/" + xuid)
                .header("X-Api-Key", apiKey)
                .get()
                .build();

            if (debugRequests) {
                System.out.println("Fetching account for xuid: " + xuid);
            }

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (debugRequests) {
                        System.err.println("Failed to fetch account: " + e.getMessage());
                    }
                    future.complete(null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            AccountDto account = gson.fromJson(responseBody, AccountDto.class);
                            future.complete(account);
                        } else {
                            if (debugRequests) {
                                System.err.println("Failed to fetch account: " + response.code());
                            }
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        if (debugRequests) {
                            System.err.println("Error parsing account response: " + e.getMessage());
                        }
                        future.complete(null);
                    }
                }
            });
        } catch (Exception e) {
            if (debugRequests) {
                System.err.println("Exception in getAccountByXuid: " + e.getMessage());
            }
            future.complete(null);
        }

        return future;
    }

    /**
     * Close the HTTP client and release resources
     */
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
