package net.runelite.client.plugins.projectx;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSite;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client to fetch random facts from the projectx API.
 */
@Slf4j
public class RandomFactClient {
    private static final String PROJECTX_API_URL = ProjectXSite.baseUrl() + "api";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * Fetches a random fact from the projectx API.
     * @return random fact string or error message
     */
    public static String getRandomFact() {
        try {
            Request request = new Request.Builder()
                    .url(PROJECTX_API_URL + "/fact/random")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return "Failed to fetch random fact";
                }
                return response.body().string();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fetches a random fact asynchronously and invokes the callback on the Swing EDT.
     * @param callback
     */
    public static void getRandomFactAsync(Consumer<String> callback) {
        if (ProjectX.isTelemetryDisabled()) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                return getRandomFact();
            } catch (Exception e) {
                log.error("Error in async fact fetching", e);
            }
            return null;
        }).thenAccept(fact -> {
            SwingUtilities.invokeLater(() -> callback.accept(fact));
        }).exceptionally(throwable -> {
            log.error("Async fact fetching failed", throwable);
            return null;
        });
    }
}