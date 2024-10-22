package com.example.wetterapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

    // API Key und Ortsname für die Wetterdaten
    private static final String API_KEY = "5e4d858d5dc3bff71d4b900e5360bd46";
    private static final String ORT = "Matrei am Brenner";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Labels für die Anzeige der Temperatur und Luftfeuchtigkeit
        Label titleLabel = new Label("Temperatur in " + ORT);
        titleLabel.setFont(new Font("Arial", 24)); // Überschrift Schriftart

        Label temperaturLabel = new Label("Lade Temperatur...");
        temperaturLabel.setFont(new Font("Arial", 20)); // Schriftart für Temperatur

        Label luftfeuchtigkeitLabel = new Label("Lade Luftfeuchtigkeit...");
        luftfeuchtigkeitLabel.setFont(new Font("Arial", 20)); // Schriftart für Luftfeuchtigkeit

        // Wettericon initialisieren
        ImageView weatherIcon = new ImageView();
        weatherIcon.setFitWidth(100);
        weatherIcon.setFitHeight(100);

        // HBox für Wettervorhersage erstellen
        HBox forecastBox = new HBox(10); // Abstand zwischen den Elementen
        forecastBox.setStyle("-fx-alignment: center;");

        // Das Layout für die Anzeige
        VBox root = new VBox(20, titleLabel, temperaturLabel, luftfeuchtigkeitLabel, weatherIcon, forecastBox);
        root.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #D3D3D3;"); // Hellgrauer Hintergrund

        // Die Wetterdaten in einem separaten Thread abrufen
        new Thread(() -> {
            String[] wetterDaten = getWetterDaten();
            String[] vorhersageDaten = getWetterVorhersage();

            // Zugriff auf die UI im JavaFX-Anwendungsthread
            Platform.runLater(() -> {
                if (wetterDaten[0].startsWith("Fehler")) {
                    showErrorWindow();
                } else {
                    temperaturLabel.setText("Aktuelle Temperatur: " + wetterDaten[0] + "°C");
                    luftfeuchtigkeitLabel.setText("Luftfeuchtigkeit: " + wetterDaten[1] + "%");
                    weatherIcon.setImage(new Image(wetterDaten[2])); // Wettericon setzen
                }

                // Vorhersage für die nächsten 5 Tage anzeigen
                for (String tag : vorhersageDaten) {
                    HBox tagHBox = new HBox(5);
                    String[] tagDaten = tag.split(",");
                    VBox dayBox = new VBox(5); // VBox für jeden Tag

                    // Wochentag berechnen
                    String wochentag = getWochentag(tagDaten[0]);
                    Label dayLabel = new Label(wochentag);
                    ImageView forecastIcon = new ImageView(tagDaten[1]);
                    forecastIcon.setFitWidth(50);
                    forecastIcon.setFitHeight(50);
                    Label forecastTemp = new Label(tagDaten[2] + "°C");

                    dayBox.getChildren().addAll(dayLabel, forecastIcon, forecastTemp); // Wochentag, Icon und Temperatur
                    forecastBox.getChildren().add(dayBox);
                }
            });
        }).start();

        // Szene erstellen
        Scene scene = new Scene(root, 400, 600);
        primaryStage.setTitle("Wetter in " + ORT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Diese Methode holt die aktuelle Temperatur und Luftfeuchtigkeit aus der OpenWeatherMap API.
     */
    private String[] getWetterDaten() {
        try {
            // Ort URL-encodieren
            String encodedOrt = URLEncoder.encode(ORT, "UTF-8");
            String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedOrt + "&appid=" + API_KEY + "&units=metric";

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Daten von der API empfangen
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();

            // JSON-Daten parsen
            JSONObject json = new JSONObject(content.toString());
            double temperatur = json.getJSONObject("main").getDouble("temp");
            double luftfeuchtigkeit = json.getJSONObject("main").getDouble("humidity");
            String wetterIconId = json.getJSONArray("weather").getJSONObject(0).getString("icon");

            // URL des Icons von OpenWeatherMap
            String wetterIconUrl = "https://openweathermap.org/img/wn/" + wetterIconId + "@2x.png";

            return new String[]{String.valueOf(temperatur), String.valueOf(luftfeuchtigkeit), wetterIconUrl};

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Fehler beim Abrufen der Temperatur", "0", ""};
        }
    }

    /**
     * Diese Methode holt die Wettervorhersage für die nächsten 5 Tage.
     */
    private String[] getWetterVorhersage() {
        try {
            // Ort URL-encodieren
            String encodedOrt = URLEncoder.encode(ORT, "UTF-8");
            String apiUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + encodedOrt + "&appid=" + API_KEY + "&units=metric";

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Daten von der API empfangen
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();

            // JSON-Daten parsen
            JSONObject json = new JSONObject(content.toString());
            JSONArray list = json.getJSONArray("list");
            String[] vorhersageDaten = new String[5];

            for (int i = 0; i < 5; i++) {
                JSONObject tag = list.getJSONObject(i * 8); // alle 3 Stunden
                double temperatur = tag.getJSONObject("main").getDouble("temp");
                String wetterIconId = tag.getJSONArray("weather").getJSONObject(0).getString("icon");

                // URL des Icons von OpenWeatherMap
                String wetterIconUrl = "https://openweathermap.org/img/wn/" + wetterIconId + "@2x.png";

                // Wochentag speichern
                String wochentag = tag.getString("dt_txt");
                vorhersageDaten[i] = wochentag + "," + wetterIconUrl + "," + temperatur; // Temperatur und Icon URL
            }

            return vorhersageDaten;

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Fehler bei der Wettervorhersage"};
        }
    }

    /**
     * Diese Methode gibt den Wochentag basierend auf einem Datum zurück.
     */
    private String getWochentag(String datum) {
        try {
            // Datum im Format "YYYY-MM-DD HH:MM:SS"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
            Date date = sdf.parse(datum);
            SimpleDateFormat wochentagFormat = new SimpleDateFormat("EEEE", Locale.GERMANY);
            return wochentagFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Unbekannt";
        }
    }

    // Methode zum Anzeigen eines Fehlers
    private void showErrorWindow() {
        Stage errorStage = new Stage();
        Label errorLabel = new Label("Fehler beim Abrufen der Wetterdaten");
        errorLabel.setFont(new Font("Arial", 16)); // Schriftart für Fehleranzeige
        VBox errorLayout = new VBox(20, errorLabel);
        errorLayout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #D3D3D3;"); // Hellgrauer Hintergrund für Fehlerfenster

        Scene errorScene = new Scene(errorLayout, 300, 100);
        errorStage.setTitle("Fehler");
        errorStage.setScene(errorScene);
        errorStage.show();
    }
}
