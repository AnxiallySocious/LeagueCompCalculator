import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Manages the roles of champions manually due to the absence of this information from the API.
 * Implements the Singleton pattern to ensure a single instance handles role data.
 */
public class RoleGenerator {

    private static RoleGenerator instance;

    // Stores the roles assigned to each champion
    Map<String,String[]> roles = new HashMap<>();
    DataStorage dataStorage;
    private static final String ROLES_FILE_PATH = "champion_roles.json";

    /**
     * Private constructor to prevent instantiation from outside the class.
     * Initializes the DataStorage instance and checks and updates the roles data file.
     * @throws IOException If an I/O error occurs during initialization.
     */
    private RoleGenerator() throws IOException {
        dataStorage = DataStorage.getInstance();
        checkAndUpdateDataFile();
    }

    /**
     * Provides access to the singleton instance of RoleGenerator, creating it if it does not exist.
     * @return The singleton instance of RoleGenerator.
     * @throws IOException If an I/O error occurs during instance creation.
     */
    public static synchronized RoleGenerator getInstance() throws IOException {
        if (instance == null) {
            instance = new RoleGenerator();
        }
        return instance;
    }

    //Getter for roles map
    public Map<String, String[]> getRoles() {
        return roles;
    }


    /**
     * Checks and updates the roles data file. Loads existing roles from the file or initializes the roles map if the file does not exist.
     * Updates the file if new champions are found in the DataStorage's champion list.
     */
    private void checkAndUpdateDataFile() {
        File dataFile = new File(ROLES_FILE_PATH);
        Gson gson = new Gson();
        if (dataFile.exists()) {
            // Load data from the file
            try (BufferedReader reader = new BufferedReader(new FileReader(ROLES_FILE_PATH))) {
                Type type = new TypeToken<Map<String, String[]>>() {
                }.getType();
                roles = gson.fromJson(reader, type);
            } catch (IOException e) {
                System.out.println("Error reading champion data file: " + e.getMessage());
            }
        } else {
            roles = new HashMap<>();
        }
        boolean dataUpdated = false;
        // Check for new champions and update roles accordingly
        for (String champion : dataStorage.championList.keySet()) {
            if (!roles.containsKey(champion)) {
                roles.put(champion, new String[]{});
                dataUpdated = true;
            }
        }
        // Save updated data to the file
        if (dataUpdated) {
            try (FileWriter writer = new FileWriter(ROLES_FILE_PATH, false)) {
                gson.toJson(roles, writer);
            } catch (IOException e) {
                System.out.println("Error updating champion data file: " + e.getMessage());
            }
        }
    }

    // Method to generate or initially set the roles for all champions
    public void generate() {
        roles.remove("all");
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter roles for champions. Separate multiple roles with a comma. Leave blank and press enter to finish.");
        for (String champ : DataStorage.championData.keySet()) {
            System.out.println(champ);
            if (roles.get(champ).length>0) {
                continue;
            }
            System.out.print(champ + ": ");
            String input = sc.nextLine();
            if (input.isEmpty()) {
                break; // Exit if user inputs an empty response
            }
            String[] roleArray = input.replaceAll("[^A-Za-z,]", "").toLowerCase().split(",");
            roles.put(champ, roleArray);
        }
        saveRolesToFile(); // Save changes to file
    }

    // Method to change the roles for a specific champion
    public void changeChampRoles() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the name of the champion whose roles you want to change:");
        String champName = sc.nextLine().replaceAll("[^A-Za-z]", "").toLowerCase().trim();
        if (!roles.containsKey(champName)) {
            System.out.println("Champion not found. Make sure you've entered the name correctly.");
            return;
        }
        System.out.println("Enter new roles for " + champName + ". Separate multiple roles with a comma:");
        String newRoles = sc.nextLine();
        String[] newRolesArray = newRoles.replaceAll("[^A-Za-z,]", "").toLowerCase().split(",");
        roles.put(champName, newRolesArray);
        System.out.println("Roles updated for " + champName + ".");
        saveRolesToFile(); // Save changes to file
    }

    // Optionally, add a method to display roles for testing
    public void displayRoles() {
        roles.forEach((champ, roleArray) -> {
            System.out.print(champ + " has roles: ");
            for (String role : roleArray) {
                System.out.print(role + " ");
            }
            System.out.println();
        });
    }

    // Saves the current roles map to a file.
    private void saveRolesToFile() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(ROLES_FILE_PATH, false)) {
            gson.toJson(roles, writer);
        } catch (IOException e) {
            System.out.println("Error writing roles to file: " + e.getMessage());
        }
    }


}
