import java.util.Scanner;

/*
* Maps to COURT_SHARE, the system composition,
* that creates the gangs with 'us' and 'others' parameters
* and linked the court, handkerchiefs, and turn indicator to both gangs.
* The system status is printed each time an action changes something.
*/
public class CourtShare {
    public static void main(String[] args) {
        Court court = new Court();
        Handkerchiefs handkerchiefs = new Handkerchiefs();
        Indicator turnIndicator = new Indicator();

        // Each gang takes its name, the other gang's name, and the court, handkerchiefs, and turn indicator used
        Gang sharks = new Gang("sharks", "jets", court, handkerchiefs, turnIndicator);
        Gang jets = new Gang("jets", "sharks", court, handkerchiefs, turnIndicator);

        sharks.start(); // Initial turn set to JETS by SHARKS who start first
        jets.start(); // JETS follow
    }

    // Print the status of the court
    public static void printStatus(Court.State state) {
        System.out.print("  ->  Court: " + state + '\n');
    }

    // Print the status of the handkerchiefs
    public static void printStatus(Handkerchiefs.State state) {
        System.out.print("  ->  Red Handkerchiefs: " + state + '\n');
    }

    // Print status of the turn indicator
    public static void printStatus(Indicator.State state) {
        System.out.print("  ->  Turn: " + state + '\n');
    }
}

/*
* Maps to GANG, the users of the court,
* with multiple stages and two recursive calls (untie -> beginning, wait -> check),
* behaving similarly to the FSP model.
*/
class Gang extends Thread {
    private final String us; // This gang's name
    private final String others; // Other gang's name
    private final Court court;
    private final Handkerchiefs handkerchiefs;
    private final Indicator turnIndicator;
    private final int tryLimit; // Maximum amount of tries
    private int tryCount = 0; // Try counter

    public Gang(String us, String others, Court court, Handkerchiefs handkerchiefs, Indicator turnIndicator) {
        this.us = us;
        this.others = others;
        this.court = court;
        this.handkerchiefs = handkerchiefs;
        this.turnIndicator = turnIndicator;

        // Choose the maximum amount of tries to access the court for this gang
        System.out.print("Enter the try limit to access the court for " + this.us.toUpperCase() + " (try 5): ");
        Scanner scanner = new Scanner(System.in);
        tryLimit = scanner.nextInt();
    }

    @Override
    public void run() {
        begin();
    }

    // Tie a red handkerchief, set the turn to the other gang and proceed, or wait
    private void begin() {
        if (tryCount++ >= tryLimit) return; // Stop if the try limit is reached
        handkerchiefs.tie(us);
        if (!turnIndicator.checkTurn(us).equals(others)) {
            turnIndicator.setTurn(us, others);
            check();
        } else {
            waitTurn();
        }
    }

    // If the other handkerchief is white, enter the court, otherwise wait or untie
    private void check() {
        if (turnIndicator.checkTurn(us).equals(others)) {
            if (handkerchiefs.see(us).equals("red")) {
                waitTurn();
                check(); // Recursive loop to check again
            } else if (handkerchiefs.see(us).equals("white")) {
                useCourt();
            }
        } else if (turnIndicator.checkTurn(us).equals(us)) {
            if (handkerchiefs.see(us).equals("red")) {
                finish();
            } else {
                useCourt();
            }
        }
    }

    // Wait the turn
    private void waitTurn() {
        System.out.println(us + ".wait");
    }

    // Go to the court, play, leave the court, and finish
    private void useCourt() {
        court.go(us);
        System.out.println(us + ".play");
        court.leave(us);
        finish();
    }

    // Untie the red handkerchief and start again
    private void finish() {
        handkerchiefs.untie(us);
        begin(); // Recursive loop to next try
    }
}

/*
 * Maps to COURT, a shared resource that the gangs access.
 */
class Court {
    // * Maps to the 2 states in COURT *
    public enum State {
        EMPTY,
        TAKEN
    }

    private State state = State.EMPTY; // To print in terminal

    // Enter the court
    public synchronized void go(String us) {
        state = State.TAKEN;
        System.out.print(us + ".court.go");
        CourtShare.printStatus(state);
    }

    // Leave the court
    public synchronized void leave(String us) {
        state = State.EMPTY;
        System.out.print(us + ".court.leave");
        CourtShare.printStatus(state);
    }
}

/*
 * Maps to HANDKERCHIEFS, the signals to each gang.
 */
class Handkerchiefs {
    // * Maps to the 3 states in HANDKERCHIEFS *
    public enum State {
        NONE, // There are no red handkerchiefs
        ONE, // One side has a red handkerchief tied
        BOTH // Both sides have a red handkerchief tied
    }

    private State state = State.NONE;

    // Tie own red handkerchief
    public synchronized void tie(String us) {
        switch (state) {
            case NONE: state = State.ONE; break;
            case ONE: state = State.BOTH; break;
            case BOTH: return;
        }
        System.out.print(us + ".tie");
        CourtShare.printStatus(state);
    }

    // Untie own red handkerchief
    public synchronized void untie(String us) {
        switch (state) {
            case NONE: return;
            case ONE: state = State.NONE; break;
            case BOTH: state = State.ONE; break;
        }
        System.out.print(us + ".untie");
        CourtShare.printStatus(state);
    }

    // See the other handkerchief being either white or red
    public synchronized String see(String us) {
        String colour = "";
        switch (state) {
            case NONE:
            case ONE: colour = "white"; break;
            case BOTH: colour = "red"; break;
        }
        System.out.println(us + ".see." + colour);
        return colour;
    }
}

/*
 * Maps to INDICATOR, the turn indicator for fairness.
 */
class Indicator {
    // * Maps to the 3 states in INDICATOR *
    public enum State {
        NONE,
        SHARKS,
        JETS
    }

    private State state = State.NONE;

    // Set the turn to the other gang
    public synchronized void setTurn(String us, String others) {
        switch (state) {
            // Sharks only allowed to set to SHARKS, and Jets only allowed to set to JETS
            case NONE: state = State.valueOf(others.toUpperCase()); break;
            // If turn SHARKS, only Jets can change it
            case SHARKS: if (us.equals("jets")) state = State.valueOf(others.toUpperCase()); else return; break;
            // If turn JETS, only Sharks can change it
            case JETS: if (us.equals("sharks")) state = State.valueOf(others.toUpperCase()); else return; break;
        }
        System.out.print(us + ".setTurn." + state.toString().toLowerCase());
        CourtShare.printStatus(state);
    }

    // Check the indicator for the next turn
    public synchronized String checkTurn(String us) {
        if (state != State.NONE) System.out.println(us + ".checkTurn." + state.toString().toLowerCase());
        return state.toString().toLowerCase();
    }
}