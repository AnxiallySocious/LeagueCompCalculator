/**
 *
 * Represents the matchup data for a champion in a game, including the number of games played, wins, and win rate calculation.
 *
 */

public class ChampionMatchupData {

    private int games;
    private int wins;

    public ChampionMatchupData(int wins, int games) {
        this.games = games;
        this.wins = wins;
    }

    //Calculates and returns the winrate as a percentage, returning 0.0 if no games have been played to avoid a div0 error
    public double getWinRate() {
        return (games == 0) ? 0.0 : (double) wins / games;
    }

    public int getGames() {
        return games;
    }


    public int getWins() {
        return wins;
    }

    public void addGames(int num) {
        games = games + num;
    }

    public void addWins(int num) {
        wins = wins + num;
    }

    //Updates the winrate based on the game outcome. won is true if they won, and it updates to include the game being played either way.
    public void updateWinRate(boolean won) {
        games++;
        if (won) {
            wins++;
        }
    }
}