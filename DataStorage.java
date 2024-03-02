import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class DataStorage {

    private static DataStorage instance;

    //File paths for storing data
    private static final String LIST_FILE_PATH = "champion_list.json";
    private static final String DATA_FILE_PATH = "champion_data.json";
    private static final String GLOBAL_WINRATE_FILE_PATH = "global_winrate_data.json";
    private static final String PROCESSED_FILE_PATH = "processed_match_ids.json";

    //Current game version. Is updated by VersionInfo. https://ddragon.leagueoflegends.com/api/versions.json
    private String gameVersion = "13.21.1";

    // Stores champion names and their IDs
    Map<String, String> championList;
    // Stores matchup data for each champion against other champions
    static Map<String, Map<String,ChampionMatchupData>> championData;

    // Stores global win rates for each champion
    static Map<String, ChampionMatchupData> globalChampionWinrate;
    // Stores IDs of matches that have been processed to avoid reprocessing
    static Set<String> processedMatchIds = new HashSet<>();

    // Constructor is private to prevent instantiation from outside and to facilitate the Singleton pattern
    private DataStorage() throws IOException {
        // Updates the game version and initializes data from files or API as necessary
        String updatedVersion = VersionInfo.getUpdatedVersionInfo();
        if (!Objects.equals(VersionInfo.readVersionFromFile(), updatedVersion)) {
            gameVersion = updatedVersion;
            VersionInfo.saveVersionToFile(gameVersion);
        }
        generateChampionList();
        checkAndUpdateDataFile();
        checkAndUpdateWinrateFile();
        readProcessedMatchIdsFromFile();
    }

    /**
     * Provides access to the singleton instance of DataStorage, creating it if it does not exist.
     * @return The singleton instance of DataStorage.
     * @throws IOException If there is an issue creating the instance or initializing data.
     */
    public static synchronized DataStorage getInstance() throws IOException {
        if (instance == null) {
            instance = new DataStorage();
        }
        return instance;
    }

    // Getters for accessing various types of stored data
    public Map<String, String> getChampionList() { return championList; }
    public Map<String, ChampionMatchupData> getGlobalChampionWinrate() { return globalChampionWinrate; }
    public Map<String, Map<String, ChampionMatchupData>> getChampionData() { return championData; }

    /**
     * Generates the champion list by fetching data from an external API and storing it in a file.
     * This is called if the game version updates or the list file does not exist.
     */
    private void generateChampionList() {
        String url = "https://ddragon.leagueoflegends.com/cdn/" + gameVersion + "/data/en_US/champion.json";
        championList = null;
        try {
            String jsonResponse = RiotAPI.getJsonResponse(url);
            championList = RiotAPI.getChampionNamesAndIds(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(LIST_FILE_PATH)) {
            gson.toJson(championList, writer);
        } catch (IOException e) {
            System.out.println("Error creating champion data file: " + e.getMessage());
        }
    }

    /**
     * Checks the existing data file for champion data, updates it with any new champions or matchups,
     * and writes the updated data back to the file.
     */
    private void checkAndUpdateDataFile() {
        File dataFile = new File(DATA_FILE_PATH);
        Gson gson = new Gson();
        if (dataFile.exists()) {
            // Load data from the file
            try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
                Type type = new TypeToken<Map<String, Map<String, ChampionMatchupData>>>() {
                }.getType();
                championData = gson.fromJson(reader, type);
            } catch (IOException e) {
                System.out.println("Error reading champion data file: " + e.getMessage());
            }
        } else {
            championData = new HashMap<>();
        }
        boolean dataUpdated = false;
        for (String champion : championList.keySet()) {
            if (championData.containsKey(champion)) {
                championData.get(champion).remove("all");
            }
            if (!championData.containsKey(champion)) {
                championData.put(champion, new HashMap<>());
                dataUpdated = true;
            }
            for (String otherChampion : championList.keySet()) {
                if (!champion.equals(otherChampion) && !championData.get(champion).containsKey(otherChampion)) {
                    championData.get(champion).put(otherChampion, new ChampionMatchupData(0, 0));
                    dataUpdated = true;
                }
            }
        }
        // Save updated data to the file
        if (dataUpdated) {
            try (FileWriter writer = new FileWriter(DATA_FILE_PATH, false)) {
                gson.toJson(championData, writer);
            } catch (IOException e) {
                System.out.println("Error updating champion data file: " + e.getMessage());
            }
        }
    }

    /**
     * Checks the global win rate file, updates it with new data as necessary,
     * and writes the updated data back to the file.
     */
    private void checkAndUpdateWinrateFile() {
        File dataFile = new File(GLOBAL_WINRATE_FILE_PATH);
        Gson gson = new Gson();
        if (dataFile.exists()) {
            // Load data from the file
            try (BufferedReader reader = new BufferedReader(new FileReader(GLOBAL_WINRATE_FILE_PATH))) {
                Type type = new TypeToken<Map<String, ChampionMatchupData>>() {
                }.getType();
                globalChampionWinrate = gson.fromJson(reader, type);
            } catch (IOException e) {
                System.out.println("Error reading champion data file: " + e.getMessage());
            }
        } else {
            globalChampionWinrate = new HashMap<>();
        }
        boolean dataUpdated = false;
        for (String champion : championList.keySet()) {
            if (!globalChampionWinrate.containsKey(champion)) {
                ChampionMatchupData newData = new ChampionMatchupData(0,0);
                for (String otherChampion : championList.keySet()) {
                    if (!champion.equals(otherChampion)) {
                        newData.addGames(championData.get(champion).get(otherChampion).getGames());
                        newData.addWins(championData.get(champion).get(otherChampion).getWins());
                    }
                }
                globalChampionWinrate.put(champion, newData);
                dataUpdated = true;
            }
        }
        // Save updated data to the file
        if (dataUpdated) {
            try (FileWriter writer = new FileWriter(GLOBAL_WINRATE_FILE_PATH, false)) {
                gson.toJson(globalChampionWinrate, writer);
            } catch (IOException e) {
                System.out.println("Error updating champion data file: " + e.getMessage());
            }
        }
    }

    /**
     * Processes new match data, updating champion matchup data and global win rates based on the results.
     * @param matchData The JSON string containing data about the match.
     */
    public static void processGameData(String matchData) {
        // Get the participants and their team ID
        Gson gson = new Gson();
        JsonObject matchDataJson = gson.fromJson(matchData, JsonObject.class);
        if (matchDataJson == null) {
            System.out.println("Error parsing match data: null JsonObject");
            return;
        }
        JsonObject info = matchDataJson.getAsJsonObject("info");
        if (info == null) {
            System.out.println("Error parsing match data: info field is null");
            return;
        }
        JsonArray participants = info.getAsJsonArray("participants");
        if (participants == null) {
            System.out.println("Error parsing match data: participants field is null");
            return;
        }
        Map<String, Integer> championIdToTeam = new HashMap<>();
        for (JsonElement participantElement : participants) {
            JsonObject participant = participantElement.getAsJsonObject();
            String championId = participant.get("championName").getAsString().toLowerCase();
            int teamId = participant.get("teamId").getAsInt();
            championIdToTeam.put(championId, teamId);
        }
        // Get the winning team ID
        JsonArray teams = info.getAsJsonArray("teams");
        int winningTeamId = -1;
        // Print the teams JsonArray
        for (JsonElement teamElement : teams) {
            JsonObject team = teamElement.getAsJsonObject();
            // Print the team JsonObject and the win value
            if (team.get("win").getAsBoolean()) {
                winningTeamId = team.get("teamId").getAsInt();
                break;
            }
        }
        // Update championData
        for (String championId : championIdToTeam.keySet()) {
            boolean won = championIdToTeam.get(championId) == winningTeamId;
            Map<String, ChampionMatchupData> winRateAgainst = championData.computeIfAbsent(championId, k -> new HashMap<>());

            for (String otherChampionId : championIdToTeam.keySet()) {
                if (!Objects.equals(championIdToTeam.get(otherChampionId), championIdToTeam.get(championId))) { // If champions are on opposite teams
                    ChampionMatchupData matchupData = winRateAgainst.get(otherChampionId);
                    //Adds winrate data if none exists
                    if (matchupData == null) {
                        matchupData = new ChampionMatchupData(0,0);
                        winRateAgainst.put(otherChampionId, matchupData);
                    }
                    //Updates the champ's global winrate vs 'all', all's winrate vs all, and vs the new champion
                    globalChampionWinrate.computeIfAbsent(championId, k -> new ChampionMatchupData(0, 0));
                    globalChampionWinrate.get(championId).updateWinRate(won);
                    matchupData.updateWinRate(won);
                }
            }
        }
        // Save the updated championData to the file
        try (FileWriter writer = new FileWriter(DATA_FILE_PATH, false)) {
            gson.toJson(championData, writer);
        } catch (IOException e) {
            System.out.println("Error updating champion data file: " + e.getMessage());
        }
    }

    //NEW ATTEMPT
    public static void readProcessedMatchIdsFromFile() throws IOException {
        File dataFile = new File(PROCESSED_FILE_PATH);
        Gson gson = new Gson();
        if (dataFile.exists()) {
            // Directly read the HashSet from the file
            try (Reader reader = new FileReader(PROCESSED_FILE_PATH)) {
                Type setType = new TypeToken<HashSet<String>>() {}.getType();
                processedMatchIds = gson.fromJson(reader, setType);
                if (processedMatchIds == null) {
                    processedMatchIds = new HashSet<>();
                }
            } catch (FileNotFoundException e) {
                System.err.println("Processed matches file not found: " + e.getMessage());
            }
        } else {
            processedMatchIds = new HashSet<>();
        }
    }

    public static void writeProcessedMatchIdToFile(String matchId) {
        // No need to check if the ID already exists, HashSet takes care of uniqueness
        boolean isNew = processedMatchIds.add(matchId);
        if (isNew) {
            // If the match ID was indeed new and added, then persist the updated set
            Gson gson = new Gson();
            try (Writer writer = new FileWriter(PROCESSED_FILE_PATH, false)) {
                gson.toJson(processedMatchIds, writer);
            } catch (IOException e) {
                System.err.println("Error writing processed match IDs to file: " + e.getMessage());
            }
        }
    }
}