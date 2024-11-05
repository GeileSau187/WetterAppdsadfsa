package com.example.wetterapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.*;

public class WetterAppFx extends Application {

    private static final String API_KEY = "5e4d858d5dc3bff71d4b900e5360bd46";
    private String ort = "Matrei am Brenner"; // Initialer Ort

    private Label titleLabel;
    private Label temperaturLabel;
    private Label luftfeuchtigkeitLabel;
    private ImageView weatherIcon;
    private HBox forecastBox;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        titleLabel = new Label("Temperatur in " + ort);
        titleLabel.setFont(new Font("Arial", 24));

        temperaturLabel = new Label("Lade Temperatur...");
        temperaturLabel.setFont(new Font("Arial", 20));

        luftfeuchtigkeitLabel = new Label("Lade Luftfeuchtigkeit...");
        luftfeuchtigkeitLabel.setFont(new Font("Arial", 20));

        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(100);
        weatherIcon.setFitHeight(100);

        forecastBox = new HBox(10);
        forecastBox.setStyle("-fx-alignment: center;");

        TextField searchField = new TextField();
        searchField.setPromptText("Ort eingeben...");

        Button searchButton = new Button("Suchen");
        searchButton.setOnAction(e -> {
            ort = searchField.getText();
            titleLabel.setText("Temperatur in " + ort);
            fetchWeatherData();
        });

        HBox searchBox = new HBox(10, searchField, searchButton);
        searchBox.setStyle("-fx-alignment: center;");

        VBox root = new VBox(20, searchBox, titleLabel, temperaturLabel, luftfeuchtigkeitLabel, weatherIcon, forecastBox);
        root.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #D3D3D3;");

        Scene scene = new Scene(root, 400, 600);
        primaryStage.setTitle("Wetter in " + ort);
        primaryStage.setScene(scene);
        primaryStage.show();

        fetchWeatherData();
    }

    private void fetchWeatherData() {
        new Thread(() -> {
            String[] wetterDaten = getWetterDaten();
            String[] vorhersageDaten = getWetterVorhersage();

            Platform.runLater(() -> {
                if (wetterDaten[0].startsWith("Fehler")) {
                    showErrorWindow();
                } else {
                    temperaturLabel.setText("Aktuelle Temperatur: " + wetterDaten[0] + "°C");
                    luftfeuchtigkeitLabel.setText("Luftfeuchtigkeit: " + wetterDaten[1] + "%");
                    weatherIcon.setImage(new Image(wetterDaten[2]));
                }

                forecastBox.getChildren().clear();
                for (String tag : vorhersageDaten) {
                    String[] tagDaten = tag.split(",");
                    VBox dayBox = new VBox(5);
                    String wochentag = getWochentag(tagDaten[0]);
                    Label dayLabel = new Label(wochentag);
                    ImageView forecastIcon = new ImageView(tagDaten[1]);
                    forecastIcon.setFitWidth(50);
                    forecastIcon.setFitHeight(50);
                    Label forecastTemp = new Label(tagDaten[2] + "°C");

                    dayBox.getChildren().addAll(dayLabel, forecastIcon, forecastTemp);
                    forecastBox.getChildren().add(dayBox);
                }
            });
        }).start();
    }

    private String[] getWetterDaten() {
        try {
            String encodedOrt = URLEncoder.encode(ort, "UTF-8");
            String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedOrt + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();

            JSONObject json = new JSONObject(content.toString());
            double temperatur = json.getJSONObject("main").getDouble("temp");
            double luftfeuchtigkeit = json.getJSONObject("main").getDouble("humidity");
            String wetterIconId = json.getJSONArray("weather").getJSONObject(0).getString("icon");
            String wetterIconUrl = "https://openweathermap.org/img/wn/" + wetterIconId + "@2x.png";

            return new String[]{String.valueOf(temperatur), String.valueOf(luftfeuchtigkeit), wetterIconUrl};

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Fehler beim Abrufen der Temperatur", "0", ""};
        }
    }

    private String[] getWetterVorhersage() {
        try {
            String encodedOrt = URLEncoder.encode(ort, "UTF-8");
            String apiUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + encodedOrt + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();

            JSONObject json = new JSONObject(content.toString());
            JSONArray list = json.getJSONArray("list");
            String[] vorhersageDaten = new String[5];

            for (int i = 0; i < 5; i++) {
                JSONObject tag = list.getJSONObject(i * 8);
                double temperatur = tag.getJSONObject("main").getDouble("temp");
                String wetterIconId = tag.getJSONArray("weather").getJSONObject(0).getString("icon");
                String wetterIconUrl = "https://openweathermap.org/img/wn/" + wetterIconId + "@2x.png";
                String wochentag = tag.getString("dt_txt");
                vorhersageDaten[i] = wochentag + "," + wetterIconUrl + "," + temperatur;
            }
            return vorhersageDaten;

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Fehler bei der Wettervorhersage"};
        }
    }

    private String getWochentag(String datum) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
            Date date = sdf.parse(datum);
            SimpleDateFormat wochentagFormat = new SimpleDateFormat("EEEE", Locale.GERMANY);
            return wochentagFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Unbekannt";
        }
    }

    private void showErrorWindow() {
        Stage errorStage = new Stage();
        Label errorLabel = new Label("Fehler beim Abrufen der Wetterdaten");
        errorLabel.setFont(new Font("Arial", 16));
        VBox errorLayout = new VBox(20, errorLabel);
        errorLayout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #D3D3D3;");
        Scene errorScene = new Scene(errorLayout, 300, 100);
        errorStage.setTitle("Fehler");
        errorStage.setScene(errorScene);
        errorStage.show();
    }
}
