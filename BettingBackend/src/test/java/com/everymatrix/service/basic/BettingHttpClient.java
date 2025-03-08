package com.everymatrix.service.basic;

import com.everymatrix.exception.HttpServerException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BettingHttpClient {
    private String baseUrl;

    public BettingHttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSession(Long customerId) throws IOException {
        String urlString = baseUrl + "/" + customerId + "/session";
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return readResponse(connection);
            } else {
                throw new HttpServerException(responseCode, "", null);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void postStake(Long betOfferId, String sessionKey, int stake) throws IOException {
        String urlString = baseUrl + "/" + betOfferId + "/stake?sessionkey=" + sessionKey;
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String requestBody = String.valueOf(stake);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new HttpServerException(responseCode, connection.getResponseMessage(), null);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String getHighestStake(Long betOfferId) throws IOException {
        String urlString = baseUrl + "/" + betOfferId + "/highstakes";
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(connection);
                return response.trim();
            } else {
                throw new IOException("Failed to get highest stake. HTTP code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}