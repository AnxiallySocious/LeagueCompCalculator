import javax.swing.*;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        //GUI CURRENTLY DISABLED. It is more efficient to showcase this project using the console, as-is. I will leave the code in for the future.
//        System.out.println("Do you want to use the GUI or the console?");
//        System.out.println("1. GUI");
//        System.out.println("2. Console");
//        String input = sc.next().toLowerCase();
//        while (!input.equals("gui") && !input.equals("console") && !input.equals("1") && !input.equals("2")) {
//            input = sc.next().toLowerCase();
//        }
//        if (input.equals("gui") || input.equals("1")) {
//            GUI();
//        } else {
//            console(sc);
//        }
        console(sc);
    }

    //Unused presently.
    public static void GUI() {
        SwingUtilities.invokeLater(() -> {
            GUI frame = new GUI();
            frame.setVisible(true);
        });
    }

    public static void console(Scanner sc) throws IOException {
        System.out.println("Select a task:");
        System.out.println("1. Calculate with present data");
        System.out.println("2. Recalculate the roles");
        System.out.println("3. Process new data (requires an API key)");
        int input = sc.nextInt();
        while ((input!=1 && input!=2) && input!=3) {
            input = sc.nextInt();
        }
        switch(input) {
            case 1:
                ChampionDataFetcher fetcher = new ChampionDataFetcher();
                fetcher.displayWinrate(fetcher.getTopWinrates());
                fetcher.getBestChamps();
                break;
            case 2:
                RoleGenerator generator = RoleGenerator.getInstance();
                generator.generate();
                generator.changeChampRoles();
                break;
            case 3:
                System.out.println("Please input the API key you've been provided: ");
                String apiKey = sc.next();
                RiotAPI.setApiKey(apiKey);
                DataStorage dataStorage = DataStorage.getInstance();
                RiotAPI.fetchAndProcessGameData();
            default:
                System.out.println("How");
                break;
        }
    }

}