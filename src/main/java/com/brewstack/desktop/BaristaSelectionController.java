package com.brewstack.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class BaristaSelectionController {

    @FXML private FlowPane baristaPane;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        fetchBaristas();
    }

    private void fetchBaristas() {
        Task<List<Barista>> task = new Task<>() {
            @Override
            protected List<Barista> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/baristas"))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return mapper.readValue(response.body(), new TypeReference<>() {});
            }
        };

        task.setOnSucceeded(e -> populateCards(task.getValue()));
        task.setOnFailed(e -> {
            Label error = new Label("Could not load baristas: " + task.getException().getMessage());
            error.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            baristaPane.getChildren().add(error);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void populateCards(List<Barista> baristas) {
        for (Barista barista : baristas) {
            VBox card = buildCard(barista);
            baristaPane.getChildren().add(card);
        }
    }

    private VBox buildCard(Barista barista) {
        String baseStyle =
            "-fx-background-color: #ffffff; -fx-background-radius: 16; " +
            "-fx-cursor: hand; -fx-border-color: #ece6fa; -fx-border-radius: 16; -fx-border-width: 2; " +
            "-fx-effect: dropshadow(gaussian, rgba(100,80,160,0.10), 12, 0, 0, 3);";
        String hoverStyle =
            "-fx-background-color: #f0eaff; -fx-background-radius: 16; " +
            "-fx-cursor: hand; -fx-border-color: #d4bef5; -fx-border-radius: 16; -fx-border-width: 2; " +
            "-fx-effect: dropshadow(gaussian, rgba(100,80,160,0.18), 16, 0, 0, 4);";

        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(175);
        card.setPrefHeight(130);
        card.setStyle(baseStyle);

        Label nameLabel = new Label(barista.getName());
        nameLabel.setStyle("-fx-text-fill: #1e1830; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label levelLabel = new Label("Level " + barista.getLevel());
        levelLabel.setStyle("-fx-text-fill: #7a5cb0; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label xpLabel = new Label(barista.getTotalXp() + " XP");
        xpLabel.setStyle("-fx-text-fill: #9b8fc0; -fx-font-size: 11px;");

        card.getChildren().addAll(nameLabel, levelLabel, xpLabel);
        card.setOnMouseClicked(e -> selectBarista(barista));
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    private void selectBarista(Barista barista) {
        AppState.setCurrentBarista(barista);
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("MainView.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 720);
            Stage stage = (Stage) baristaPane.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
