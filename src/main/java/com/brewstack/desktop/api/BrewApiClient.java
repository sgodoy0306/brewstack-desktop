package com.brewstack.desktop.api;

import com.brewstack.desktop.api.model.Recipe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class BrewApiClient {

    private static final String BASE_URL = "http://localhost:8181/api";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BrewApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<Recipe> getMenu() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/recipes"))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch menu. HTTP status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<Recipe>>() {});
    }

    public void processSale(Long recipeId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/brew/" + recipeId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to process sale for recipe " + recipeId + ". HTTP status: " + response.statusCode());
        }
    }
}
