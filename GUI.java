import javax.swing.*;
import java.awt.*;
import java.io.IOException;


public class GUI extends JFrame {

    private JButton calculateButton;
    private JButton recalculateButton;
    private JButton processDataButton;
    private JTextArea outputTextArea;

    public GUI() {
        setTitle("Game Data Processor");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeComponents();
        layoutComponents();
        addListeners();

        setAlwaysOnTop(true); // Make the window appear on top
        setVisible(true); // It's important to set visible after setting always on top
        setAlwaysOnTop(false); // Set back to default behavior

    }

    private void initializeComponents() {
        calculateButton = new JButton("Calculate with present data");
        recalculateButton = new JButton("Recalculate the roles");
        processDataButton = new JButton("Process new data (requires an API key)");
        outputTextArea = new JTextArea(10, 30);
    }

    private void layoutComponents() {
        JPanel panel = new JPanel();
        panel.add(calculateButton);
        panel.add(recalculateButton);
        panel.add(processDataButton);

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        this.add(new JScrollPane(outputTextArea), BorderLayout.SOUTH);
    }



    private void addListeners() {
        calculateButton.addActionListener(e -> {
            try {
                performCalculation();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        recalculateButton.addActionListener(e -> {
            try {
                performRecalculation();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        processDataButton.addActionListener(e -> processNewData());
    }

    private void performCalculation() throws IOException {
        // Call ChampionDataFetcher methods and display results
        outputTextArea.setText("Calculating with present data...");
        ChampionDataFetcher fetcher = new ChampionDataFetcher();
        // Assume displayWinrate & getBestChamps methods update the UI or return String
        fetcher.displayWinrate(fetcher.getTopWinrates());
        fetcher.getBestChamps();
    }

    private void performRecalculation() throws IOException {
        // Call RoleGenerator methods and display results
        outputTextArea.setText("Recalculating the roles...");
        RoleGenerator generator = RoleGenerator.getInstance();
        generator.generate();
        generator.changeChampRoles();
    }

    private void processNewData() {
        // Instead of reading from the console, use JOptionPane for input
        String apiKey = JOptionPane.showInputDialog(this, "Enter your API Key:", "API Key Required", JOptionPane.PLAIN_MESSAGE);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            outputTextArea.setText("Processing new data with API Key...");
            try {
                RiotAPI.setApiKey(apiKey); // Assuming constructor sets the API key for later use
                RiotAPI.fetchAndProcessGameData();
            } catch (Exception e) {
                e.printStackTrace(); // This now redirects to the JTextArea
            }
        } else {
            JOptionPane.showMessageDialog(this, "API Key is required to process new data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            GUI frame = new GUI();
//            frame.setVisible(true);
//        });
//    }
}