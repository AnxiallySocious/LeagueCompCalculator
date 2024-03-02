import java.io.IOException;
import java.util.*;

/**
 * Fetches and processes champion data to identify top performing champions based on win rates and matchups.
 */
public class ChampionDataFetcher {

    DataStorage dataStorage;

    // Global win rates for all champions
    static Map<String, Double> globalChampionWinrate = new HashMap<>();
    // Champion data with good performance against selected enemy champions
    static final Map<String, ChampionMatchupData> topChampList = new HashMap<>();
    // Win rates of top performing champions
    static Map<String, Double> topChampWinrates = new HashMap<>();
    // Detailed champion matchup data
    static Map<String, Map<String,ChampionMatchupData>> championData;
    // Role generator for filtering champions by role
    static RoleGenerator roles;

    /**
     * Initializes the ChampionDataFetcher by loading necessary data from the DataStorage instance.
     * @throws IOException If an I/O error occurs during data loading.
     */
    public ChampionDataFetcher() throws IOException {
        dataStorage = DataStorage.getInstance();
        roles = RoleGenerator.getInstance();
        championData = dataStorage.getChampionData();
        globalChampionWinrate = getTopWinrates();
    }

    /**
     * Fetches the top global win rates from DataStorage.
     * @return A map of champion names to their global win rates.
     */
    public Map<String, Double> getTopWinrates() {
        return (convertWinrates(dataStorage.getGlobalChampionWinrate()));
    }

    /**
     * Converts a given winrate list to a map of champion names to their win rates.
     * @param winrateList The list of champion matchup data to convert.
     * @return A map of champion names to win rates.
     */
    public Map<String, Double> getTopWinrates(Map<String, ChampionMatchupData> winrateList) {
        return (convertWinrates(winrateList));
    }

    /**
     * Interactively queries the user for enemy champions and calculates the best champions to counter them.
     */
    public void getBestChamps() {
        Scanner sc = new Scanner(System.in);
        Map<String, ChampionMatchupData> goodChampData = new HashMap<>();
        System.out.println();
        System.out.println("Now you will enter the enemy team one at a time.");
        for (int i = 0; i < 5; i++) {
            System.out.println("Enter an enemy champion: ");
            String enemy = sc.nextLine().replaceAll("[^A-Za-z]", "").toLowerCase();
            if (enemy.isEmpty()) {
                break; // Exit if user inputs an empty response
            }
            if (championData.containsKey(enemy)) {
                //Collects the winrate each champ has against these champions and outputs it as topChampList
                goodChampData = scourData(enemy);
                for (String champ : goodChampData.keySet()) {
                    if (topChampList.containsKey(champ)) {
                        topChampList.get(champ).addGames(goodChampData.get(champ).getGames());
                        topChampList.get(champ).addWins(goodChampData.get(champ).getWins());
                    } else {
                        topChampList.put(champ, goodChampData.get(champ));
                    }
                }
            }
        }
        if (!goodChampData.isEmpty()) {
            topChampWinrates = getTopWinrates(topChampList); // Assuming you meant to pass globalChampionWinrate here
        }
        System.out.println();
        System.out.println("Enter your role('top','jungle','mid','adc','sup', or 'any' is fine): ");
        String inputRole = sc.nextLine().replaceAll("[^A-Za-z]", "").toLowerCase().trim();
        while (inputRole.isEmpty()) {
            inputRole = sc.nextLine().replaceAll("[^A-Za-z]", "").toLowerCase().trim(); // Loop if user inputs an empty response
        }
        // Filter champions by role
        Map<String, Double> filteredWinrates = new HashMap<>();
        if (!inputRole.equals("any")) {
            // Filter champions by role
            for (String champ : topChampWinrates.keySet()) {
                String[] champRoles = roles.getRoles().get(champ); // Assuming roles map is accessible
                if (champRoles != null && Arrays.asList(champRoles).contains(inputRole)) {
                    filteredWinrates.put(champ, topChampWinrates.get(champ));
                }
            }
            // Display winrate after normalizing data
//            System.out.println("Total Winrate (for highest win chance)");
//            displayWinrate((filteredWinrates));
//            System.out.println();
            System.out.println("Normalized against general winrate (for best comp counter)");
            displayWinrate(normalizeData(filteredWinrates));
        } else {
//            System.out.println("Default Winrate");
//            displayWinrate((globalChampionWinrate));
//            Display winrate after normalizing data
//            System.out.println("Total Winrate (for highest win chance)");
//            displayWinrate((topChampWinrates));
//            System.out.println();
            System.out.println("Normalized against general winrate (for best comp counter)");
            displayWinrate(normalizeData(topChampWinrates));
        }
    }

    /**
     * Displays the win rates of champions in descending order, limited to the top 10.
     * @param map The map containing champion names and their corresponding win rates.
     */
    public void displayWinrate(Map<String,Double> map) {
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // Sort by win rate in descending order
                .limit(10) // Limit to top 10
                .forEach(e -> System.out.println(e.getKey() + ": " + String.format("%.2f%%", e.getValue() * 100))); // Display the win rate as a percentage
    }

    /**
     * Displays the worst win rates of champions, limited to the top 10.
     * @param map The map containing champion names and their corresponding win rates.
     */
    public void displayWorstWinrate(Map<String,Double> map) {
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()) // Sort by win rate in descending order
                .limit(10) // Limit to top 10
                .forEach(e -> System.out.println(e.getKey() + ": " + String.format("%.2f%%", e.getValue() * 100))); // Display the win rate as a percentage
    }

    /**
     * Converts a map of ChampionMatchupData to a map of champion names to their win rates.
     * @param winrateList The map of champion matchup data.
     * @return A map of champion names to win rates.
     */
    public Map<String, Double> convertWinrates(Map<String, ChampionMatchupData> winrateList) {
        Map<String, Double> convertedWinrateData = new HashMap<>();
        for (String champ : winrateList.keySet()) {
            convertedWinrateData.put(champ,winrateList.get(champ).getWinRate());
        }
        return (convertedWinrateData);
    }

    /**
     * Analyzes champion data to find champions with good performance against a specified enemy champion.
     * @param enemy The enemy champion name.
     * @return A map of champions to their matchup data against the specified enemy.
     */
    public Map<String, ChampionMatchupData> scourData(String enemy) {
        Map<String, ChampionMatchupData> goodChampData = new HashMap<>();
        for (String champ : championData.keySet()) {
            ChampionMatchupData newData = new ChampionMatchupData(0,0);
            goodChampData.put(champ, newData);
            if (!champ.equals(enemy)) {
                goodChampData.get(champ).addGames(championData.get(champ).get(enemy).getGames());
                goodChampData.get(champ).addWins(championData.get(champ).get(enemy).getWins());
            }
        }
        // Remove the entry if it has fewer than 300 games
        goodChampData.entrySet().removeIf(entry -> entry.getValue().getGames() < 300);
        return goodChampData;
    }

    /**
     * Normalizes the win rates of champions by comparing them to the global win rates.
     * @param data The map containing champion names and their win rates to normalize.
     * @return A map of champion names to their normalized win rates.
     */
    public Map<String, Double> normalizeData(Map<String, Double> data) {
        Map<String, Double> normalizedData = new HashMap<>();
        for(String champ : data.keySet()) {
            Double globalRate = globalChampionWinrate.get(champ);
            Double champRate = data.get(champ);
            if (globalRate != null) {
                normalizedData.put(champ, champRate - globalRate);
            } else {
                // Handle missing global rate, e.g., by copying the original rate
                normalizedData.put(champ, champRate);
            }
        }
        return normalizedData;
    }


}
