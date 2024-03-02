import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class RiotAPI {

    //Rate limiters
    private static final int RATE_LIMIT_PER_SECOND = 20;
    private static final int RATE_LIMIT_PER_TWO_MINUTES = 100;

    //Urls for API
    private static final String baseUrl = "https://na1.api.riotgames.com";
    private static final String baseUrl2 = "https://americas.api.riotgames.com";
    private static String apiKey = "";
    private static final OkHttpClient httpClient = new OkHttpClient();

    public RiotAPI() {

    }

    public static void setApiKey(String apiKey) {
        RiotAPI.apiKey = apiKey;
    }

    //Fetches the game data from Riot, looping through top ranks then the top 10 pages of lower ranks.
    public static void fetchAndProcessGameData() {
        String[] tiers = {"CHALLENGER", "GRANDMASTER", "MASTER", "DIAMOND", "EMERALD", "PLATINUM", "GOLD", "SILVER"};
        String[] divisions = {"I", "II", "III", "IV"};
        try {
            for (String tier : tiers) {
                if (tier.equals("CHALLENGER") || tier.equals("GRANDMASTER") || tier.equals("MASTER")) {
                    try {
                        List<String> accountIds = getPUUIDsByTierAndDivision(tier);
                        for (String accountId : accountIds) {
                            List<String> matchIds = getMatchIdsByPUUID(accountId);
                            for (String matchId : matchIds) {
                                String matchData = getMatchDataByMatchId(matchId);
                                DataStorage.processGameData(matchData);
                                DataStorage.writeProcessedMatchIdToFile(matchId);
                            }
                        }
                    } catch (IOException | RiotApiException e) {
                        e.printStackTrace();
                        throw new RiotApiException(e.getMessage());
                    }
                    System.out.println("Processed " + tier + " games");
                } else { //If not challenger, grandmaster, or master
                    for (String division : divisions) {
                        try {
                            for (int i = 0; i < 10; i++) {
                                List<String> accountIds = getPUUIDsByTierAndDivision(tier, division, i);
                                for (String accountId : accountIds) {
                                    List<String> matchIds = getMatchIdsByPUUID(accountId);
                                    for (String matchId : matchIds) {
                                        String matchData = getMatchDataByMatchId(matchId);
                                        DataStorage.processGameData(matchData);
                                        DataStorage.writeProcessedMatchIdToFile(matchId);
                                    }
                                }
                            }
                        } catch (IOException | RiotApiException e) {
                            e.printStackTrace();
                            throw new RiotApiException(e.getMessage());
                        }
                        System.out.println("Processed " + tier + " games");
                    }
                }
            }

        } catch (RiotApiException e) {
            e.printStackTrace();
        }
        System.out.println("We did it?");
    }

    //This is for single-page tiers.
    private static List<String> getPUUIDsByTierAndDivision(String tier) throws IOException, RiotApiException {
        String url = baseUrl + "/lol/league/v4/" + tier.toLowerCase() + "leagues/by-queue/RANKED_SOLO_5x5?api_key=" + apiKey;
        System.out.println(baseUrl);
        System.out.print("Getting all PUUID'S: ");
        String jsonResponse = makeRequest(url);
        System.out.println("getPUUIDsByTierAndDivision JSON Response: " + Objects.requireNonNull(jsonResponse).substring(0,100) + "...");// Initialize the JSONArray to process
        JSONArray jsonArray;

// Try to parse the JSON response as an object to check for the old format
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            jsonArray = jsonObject.getJSONArray("entries"); // Old format
        } catch (Exception e) {
            // If parsing as an object fails, try parsing it as an array for the new format
            jsonArray = new JSONArray(jsonResponse);
        }

        List<String> puuids = new ArrayList<>();
        //CHANGED. TURN BACK TO JSONARRAY.LENGTH.
        int summonerCount = jsonArray.length();
        for (int i = 0; i < summonerCount; i++) {
            JSONObject playerObject = jsonArray.getJSONObject(i);
            String summonerId = playerObject.getString("summonerId");

            // Get the summoner's data using summonerId
            String summonerUrl = baseUrl + "/lol/summoner/v4/summoners/" + summonerId + "?api_key=" + apiKey;
            System.out.println("Getting summoner data " + (i+1) + "/" + summonerCount + ": ");
            String summonerJsonResponse = makeRequest(summonerUrl);
            JSONObject summonerJsonObject = new JSONObject(Objects.requireNonNull(summonerJsonResponse));

            // Get the PUUID from summoner's data
            String puuid = summonerJsonObject.getString("puuid");
            puuids.add(puuid);
        }
        return puuids;
    }


    //This is for multi-page tiers
    private static List<String> getPUUIDsByTierAndDivision(String tier, String division, int page) throws IOException, RiotApiException {
        page++;
        String url = baseUrl + "/lol/league-exp/v4/entries/RANKED_SOLO_5x5/" + tier + "/" + division + "?page=" + page + "&api_key=" + apiKey;
        System.out.println(baseUrl);
        System.out.print("Getting all PUUID'S: ");
        String jsonResponse = makeRequest(url);
        System.out.println("getPUUIDsByTierAndDivision JSON Response: " + Objects.requireNonNull(jsonResponse).substring(0,100) + "...");// Initialize the JSONArray to process
        JSONArray jsonArray;

// Try to parse the JSON response as an object to check for the old format
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            jsonArray = jsonObject.getJSONArray("entries"); // Old format
        } catch (Exception e) {
            // If parsing as an object fails, try parsing it as an array for the new format
            jsonArray = new JSONArray(jsonResponse);
        }

        List<String> puuids = new ArrayList<>();
        System.out.println("Btw, jsonArray length was: " + jsonArray.length());
        int summonerCount = jsonArray.length();
        for (int i = 0; i < summonerCount; i++) {
            JSONObject playerObject = jsonArray.getJSONObject(i);
            String summonerId = playerObject.getString("summonerId");

            // Get the summoner's data using summonerId
            String summonerUrl = baseUrl + "/lol/summoner/v4/summoners/" + summonerId + "?api_key=" + apiKey;
            System.out.println("Getting summoner data " + (i+1) + "/" + summonerCount + ": ");
            String summonerJsonResponse = makeRequest(summonerUrl);
            JSONObject summonerJsonObject = new JSONObject(Objects.requireNonNull(summonerJsonResponse));

            // Get the PUUID from summoner's data
            String puuid = summonerJsonObject.getString("puuid");
            puuids.add(puuid);
        }
        return puuids;
    }

    private static List<String> getMatchIdsByPUUID(String puuid) throws IOException, RiotApiException {
        String url = baseUrl2 + "/lol/match/v5/matches/by-puuid/" + puuid + "/ids??start=0&count=100&api_key=" + apiKey;
        System.out.print("Getting Match ID'S: ");
        String jsonResponse = makeRequest(url);
        System.out.println("getMatchIdsByPUUID JSON Response: " + jsonResponse);
        JSONArray jsonArray = new JSONArray(Objects.requireNonNull(jsonResponse));

        // Load the set of processed match IDs directly from DataStorage
        // This operation assumes that the set is already loaded into memory and up-to-date
        Set<String> processedMatchIds = DataStorage.processedMatchIds;

        List<String> matchIds = new ArrayList<>();
        System.out.println("Processing matches");
        for (int i = 0; i < jsonArray.length(); i++) {
            String matchId = jsonArray.getString(i);
            // Only add match IDs that have not been processed
            if (!processedMatchIds.contains(matchId)) {
                matchIds.add(matchId);
                System.out.println("Match processed!");
            } else {
                System.out.println("Redundant match Id");
            }
        }

        return matchIds;
    }

    private static String getMatchDataByMatchId(String matchId) throws IOException, RiotApiException {
        String url = baseUrl2 + "/lol/match/v5/matches/" + matchId + "?api_key=" + apiKey;
        return makeRequest(url);
    }

    public static String getJsonResponse(String urlString) throws Exception {
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

    public static Map<String, String> getChampionNamesAndIds(String jsonResponse) {
        Map<String, String> championMap = new HashMap<>();
        //System.out.println("getChampionNamesAndIds JSON Response: " + jsonResponse);
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject data = jsonObject.getJSONObject("data");
        for (String key : data.keySet()) {
            JSONObject championJson = data.getJSONObject(key);
            String id = championJson.getString("id").toLowerCase();
            String name = championJson.getString("name").toLowerCase();
            championMap.put(id, name);
        }
        return championMap;
    }

    private static String makeRequest(String url) throws IOException, RiotApiException {
        waitForRateLimit();
        Request request = new Request.Builder().url(url).build();
        int maxRetries = 10; // Maximum number of retries
        int retryDelay = 2000; // Initial delay between retries in milliseconds
        for (int i = 0; i <= maxRetries; i++) {
            try (Response response = httpClient.newCall(request).execute()) {
                int statusCode = response.code();
                if (statusCode == 200) {
                    return Objects.requireNonNull(response.body()).string();
                } else if (statusCode == 500 && i < maxRetries) {
                    // If it's an internal server error, and we have retries left, log and wait to retry
                    System.err.println("Internal Server Error. Retrying... Attempt " + (i + 1)); //I can't figure out why this doesn't increment and just displays 'Attempt 1'
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } else {
                    // For all other errors or no retries left, handle as usual
                    System.out.println("-------------------------------------");
                    System.out.println("ERROR: Request for " + url + " failed");
                    handleHttpError(statusCode, response.message());
                    return null; // This line will not be reached if handleHttpError throws an exception
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new RiotApiException("Interrupted during retry delay");
            }
        }
        return null; // In case all retries fail, though this line should never be reached due to exception throwing
    }

    private static void handleHttpError(int statusCode, String message) throws RiotApiException {
        String errorMessage;
        switch (statusCode) {
            case 400 -> {
                errorMessage = "Bad Request: " + message;
                System.err.println("Bad Request: " + message);
            }
            case 401 -> {
                errorMessage = "Unauthorized: " + message;
                System.err.println("Unauthorized: " + message);
            }
            case 403 -> {
                errorMessage = "Forbidden: " + message;
                System.err.println("Forbidden: " + message);
            }
            case 404 -> {
                errorMessage = "Not Found: " + message;
                System.err.println("Not Found: " + message);
            }
            case 429 -> {
                errorMessage = "Rate Limit Exceeded: " + message;
                System.err.println("Rate Limit Exceeded: " + message);
            }
            case 500 -> {
                errorMessage = "Internal Server Error: " + message;
                System.err.println("Internal Server Error: " + message);
            }
            case 503 -> {
                errorMessage = "Service Unavailable: " + message;
                System.err.println("Service Unavailable: " + message);
            }
            default -> {
                errorMessage = "Unexpected HTTP error: " + message;
                System.err.println("Unexpected HTTP error: " + statusCode + " " + message);
            }
        }
        throw new RiotApiException(errorMessage);
    }

    private static void waitForRateLimit() {
        try {
            Thread.sleep(120000/RATE_LIMIT_PER_TWO_MINUTES); // Calculates each action to a portion of the 2-minute rate limit
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
