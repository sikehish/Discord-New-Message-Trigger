package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class DiscordBot {
    private static final String DISCORD_API_BASE_URL = "https://discord.com/api/v10";
    private static final String CHANNEL_ID = "1292190708186222739";
    private static String botToken = System.getenv("DISCORD_BOT_TOKEN");
    private static boolean isInitialized = false;
    private static String lastMessageId = null;
    private static final int FETCH_INTERVAL = 2000;

    public static void main(String[] args) {
        System.out.println(botToken);
        System.out.println("Bot is initialized and ready to fetch new messages!");
        while (true) {
            fetchMessages();
            try {
                Thread.sleep(FETCH_INTERVAL);
            } catch (InterruptedException e) {
                System.err.println("Error during sleep: " + e.getMessage());
            }
        }
    }

    private static void fetchMessages() {
        try {
            String url = DISCORD_API_BASE_URL + "/channels/" + CHANNEL_ID + "/messages";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bot " + botToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                String responseBody = readResponse(connection);
                JSONObject jsonResponse = new JSONObject(responseBody);
                int retryAfter = jsonResponse.getInt("retry_after");
                System.out.println("Rate limited. Try again in " + retryAfter + " ms");
                Thread.sleep(retryAfter);
            } else if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error fetching messages: " + responseCode + " " + connection.getResponseMessage());
                return;
            }

            String responseBody = readResponse(connection);
            JSONArray messages = new JSONArray(responseBody);

            System.out.println(isInitialized + ", " + messages.length());
            if (isInitialized && messages.length() > 0) {
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject message = messages.getJSONObject(i);
                    if (lastMessageId != null && message.getString("id").equals(lastMessageId)) {
                        break;
                    }
                    System.out.println(lastMessageId + ": " + message.getString("id") + "  " + message.getString("content"));
                }
                lastMessageId = messages.getJSONObject(0).getString("id");
            } else if (!isInitialized) {
                isInitialized = true;
                lastMessageId = messages.length() > 0 ? messages.getJSONObject(0).getString("id") : null;
                System.out.println("Init: " + lastMessageId);
            }
        } catch (Exception e) {
            System.err.println("Error fetching messages: " + e.getMessage());
        }
    }

    private static String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }
}
