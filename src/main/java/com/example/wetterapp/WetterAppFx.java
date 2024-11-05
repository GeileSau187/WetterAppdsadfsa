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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WetterAppFx extends Application {

    private static final String API_KEY = "5e4d858d5dc3bff71d4b900e5360bd46"; // API-Schlüssel für OpenWeatherMap
    private String ort = "Matrei am Brenner"; // Initialer Ort für die Wetterabfrage

    // UI-Elemente für die Anzeige von Wetterinformationen
    private Label titleLabel;
    private Label temperaturLabel;
    private Label luftfeuchtigkeitLabel;
    private ImageView weatherIcon;
    private HBox forecastBox;

    public static void main(String[] args) {
        launch(args); // Start der JavaFX-Anwendung
    }

    @Override
    public void start(Stage primaryStage) {
        setupUI(primaryStage); // UI-Elemente einrichten
        fetchWeatherData(); // Wetterdaten abrufen
    }

    // Methode zur Einrichtung der Benutzeroberfläche
    private void setupUI(Stage primaryStage) {
        // Titel-Label mit initialem Ortsnamen
        titleLabel = createLabel("Temperatur in " + ort, 24);

        // Labels für Temperatur und Luftfeuchtigkeit
        temperaturLabel = createLabel("Lade Temperatur...", 20);
        luftfeuchtigkeitLabel = createLabel("Lade Luftfeuchtigkeit...", 20);

        // Wettericon
        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(100);
        weatherIcon.setFitHeight(100);

        // Prognosebox für die Wettervorhersage
        forecastBox = new HBox(10);
        forecastBox.setStyle("-fx-alignment: center;");

        // Suchfeld für die Eingabe eines neuen Ortes
        TextField searchField = new TextField();
        searchField.setPromptText("Ort eingeben...");

        // Suchbutton, der die Wetterdaten für den eingegebenen Ort abruft
        Button searchButton = new Button("Suchen");
        searchButton.setOnAction(e -> {
            ort = searchField.getText(); // Neuen Ort setzen
            titleLabel.setText("Temperatur in " + ort); // Titel aktualisieren
            fetchWeatherData(); // Wetterdaten abrufen
        });

        // Suchbox, die das Suchfeld und den Button enthält
        HBox searchBox = new HBox(10, searchField, searchButton);
        searchBox.setStyle("-fx-alignment: center;");

        // Hauptlayout mit allen UI-Elementen
        VBox root = new VBox(20, searchBox, titleLabel, temperaturLabel, luftfeuchtigkeitLabel, weatherIcon, forecastBox);
        root.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #D3D3D3;");

        // Szene erstellen und dem Hauptfenster zuweisen
        Scene scene = new Scene(root, 400, 600);
        primaryStage.setTitle("Wetter in " + ort);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Methode zum Abrufen der aktuellen Wetterdaten
    private void fetchWeatherData() {
        new Thread(() -> {
            String[] wetterDaten = getWetterDaten(); // Wetterdaten abrufen
            String[] vorhersageDaten = getWetterVorhersage(); // Wettervorhersage abrufen

            // UI-Updates müssen auf dem JavaFX-Anwendungsthread erfolgen
            Platform.runLater(() -> {
                if (wetterDaten[0].startsWith("Fehler")) {
                    showErrorWindow(); // Fehlerfenster anzeigen
                } else {
                    // Wetterdaten in die UI-Elemente setzen
                    temperaturLabel.setText("Aktuelle Temperatur: " + wetterDaten[0] + "°C");
                    luftfeuchtigkeitLabel.setText("Luftfeuchtigkeit: " + wetterDaten[1] + "%");
                    weatherIcon.setImage(new Image(wetterDaten[2])); // Wettericon setzen
                }

                // Vorhersagebox leeren und neue Daten hinzufügen
                forecastBox.getChildren().clear();
                for (String tag : vorhersageDaten) {
                    String[] tagDaten = tag.split(","); // Wetterdaten für jeden Tag aufteilen
                    VBox dayBox = new VBox(5); // VBox für jeden Tag

                    String wochentag = getWochentag(tagDaten[0]); // Wochentag ermitteln
                    Label dayLabel = new Label(wochentag);
                    ImageView forecastIcon = new ImageView(tagDaten[1]);
                    forecastIcon.setFitWidth(50);
                    forecastIcon.setFitHeight(50);
                    Label forecastTemp = new Label(tagDaten[2] + "°C");

                    // Tagesbox mit Wochentag, Icon und Temperatur hinzufügen
                    dayBox.getChildren().addAll(dayLabel, forecastIcon, forecastTemp);
                    forecastBox.getChildren().add(dayBox);
                }
            });
        }).start(); // Neuen Thread starten
    }

    // Methode zum Abrufen der aktuellen Wetterdaten
    private String[] getWetterDaten() {
        try {
            String encodedOrt = URLEncoder.encode(ort, "UTF-8"); // Ort URL-encodieren
            String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedOrt + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // API-Daten empfangen
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine); // Daten sammeln
            }
            in.close(); // Reader schließen
            connection.disconnect(); // Verbindung trennen

            // JSON-Daten parsen
            JSONObject json = new JSONObject(content.toString());
            double temperatur = json.getJSONObject("main").getDouble("temp");
            double luftfeuchtigkeit = json.getJSONObject("main").getDouble("humidity");
            String wetterIconId = json.getJSONArray("weather").getJSONObject(0).getString("icon");

            // URL des Wettericons
            String wetterIconUrl = "https://openweathermap.org/img/wn/" + wetterIconId + "@2x.png";

            return new String[]{String.valueOf(temperatur), String.valueOf(luftfeuchtigkeit), wetterIconUrl};
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Fehler beim Abrufen der Temperatur", "0", ""};
        }
    }

    // Methode zum Abrufen der Wettervorhersage für die nächsten 5 Tage
    private String[] getWetterVorhersage() {
        try {
            String encodedOrt = URLEncoder.encode(ort, "UTF-8"); // Ort URL-encodieren
            String apiUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + encodedOrt + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // API-Daten empfangen
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine); // Daten sammeln
            }
            in.close(); // Reader schließen
            connection.disconnect(); // Verbindung trennen

            // JSON-Daten parsen
            JSONObject json = new JSONObject(content.toString());
            JSONArray list = json.getJSONArray("list");
            String[] vorhersageDaten = new String[5];

            for (int i = 0; i < 5; i++) {
                JSONObject tag = list.getJSONObject(i * 8); // Alle 3 Stunden
                double temperatur = tag.getJSONObject("main").getDouble("temp");
                String wetterIconId = tag.getJSONArray("weather").getJSONObject(0).getString("icon");
                String wetterIconUrl = "https://openweathermap.org/img/wn/" + wetterIconId + "@2x.png";
                String wochentag = tag.getString("dt_txt");

                // Vorhersagedaten speichern
                vorhersageDaten[i] = wochentag + "," + wetterIconUrl + "," + temperatur;
            }
            return vorhersageDaten;
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Fehler bei der Wettervorhersage"};
        }
    }

    // Methode zur Ermittlung des Wochentags anhand eines Datums
    private String getWochentag(String datum) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(datum); // Datum parsen
            SimpleDateFormat wochentagSdf = new SimpleDateFormat("EEEE", Locale.getDefault()); // Wochentag-Format
            return wochentagSdf.format(date); // Wochentag zurückgeben
        } catch (Exception e) {
            e.printStackTrace();
            return "Unbekannt";
        }
    }

    // Methode zum Anzeigen eines Fehlerfensters bei Problemen
    private void showErrorWindow() {
        // Implementierung des Fehlerfensters kann hier erfolgen
        System.out.println("Ein Fehler ist aufgetreten!"); // Platzhalter für Fehlerbehandlung
    }

    // Hilfsmethode zur Erstellung von Labels mit spezifischer Schriftgröße
    private Label createLabel(String text, int fontSize) {
        Label label = new Label(text);
        label.setFont(new Font(fontSize));
        return label;
    }
}
