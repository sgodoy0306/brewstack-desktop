package com.brewstack.desktop.api;

import com.brewstack.desktop.api.model.Barista;
import com.brewstack.desktop.api.model.CreateRecipeRequest;
import com.brewstack.desktop.api.model.IngredientDTO;
import com.brewstack.desktop.api.model.OrderSummaryDTO;
import com.brewstack.desktop.api.model.Recipe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<Barista> getBaristas() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/baristas"))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch baristas. HTTP status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<Barista>>() {});
    }

    public Barista getBarista(Long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/baristas/" + id))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch barista " + id + ". HTTP status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), Barista.class);
    }

    public Barista createBarista(String name) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("name", name));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/baristas"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create barista. HTTP status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), Barista.class);
    }

    public List<IngredientDTO> getIngredients() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/stock"))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch ingredients. HTTP status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<IngredientDTO>>() {});
    }

    public Recipe createRecipe(CreateRecipeRequest request) throws Exception {
        String json = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/recipes"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create recipe. HTTP status: " + response.statusCode() + " — " + response.body());
        }

        return objectMapper.readValue(response.body(), Recipe.class);
    }

    public OrderSummaryDTO processOrder(List<Long> recipeIds, Long baristaId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipeIds", recipeIds);
        body.put("baristaId", baristaId);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/brew/order"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to process order. HTTP status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), OrderSummaryDTO.class);
    }
}
