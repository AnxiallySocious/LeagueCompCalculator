/**
 *
 * Updates the game's version on-file so it can accurately get data from present game version
 *
 */


import org.json.JSONArray;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionInfo {
    private static final String VERSION_FILE_PATH = "version_info";

    public VersionInfo() {

    }

    public static String getUpdatedVersionInfo() {
        String url = "https://ddragon.leagueoflegends.com/api/versions.json";
        String version = "";
        try {
            String jsonResponse = getJsonResponse(url);
            version = getVersion(jsonResponse);
            return version;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    public static void saveVersionToFile(String version) {
        try (FileWriter writer = new FileWriter(VERSION_FILE_PATH)) {
            writer.write(version);
        } catch (IOException e) {
            System.out.println("Error saving version to file: " + e.getMessage());
        }
    }

    public static String readVersionFromFile() {
        File versionFile = new File(VERSION_FILE_PATH);
        if (versionFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(VERSION_FILE_PATH))) {
                return reader.readLine();
            } catch (IOException e) {
                System.out.println("Error reading version from file: " + e.getMessage());
            }
        }
        return null;
    }

    private static String getJsonResponse(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder jsonResponse = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            jsonResponse.append(line);
        }
        reader.close();

        return jsonResponse.toString();
    }

    private static String getVersion(String jsonResponse) {
        JSONArray jsonArray = new JSONArray(jsonResponse);
        return jsonArray.getString(0);
    }
}