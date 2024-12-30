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
    public static void printStatus(boolean inUse) {
        String state = inUse ? "Taken" : "Empty";
        System.out.println("\t| Court: " + state);
    }

    // Print the status of the handkerchiefs
    public static void printStatus(int numRed) {
        System.out.println("\t| Red Handkerchiefs: " + numRed);
    }

    // Print status of the turn indicator
    public static void printStatus(String turn) {
        System.out.println("\t| Next Turn: " + turn.toUpperCase());
    }
}

/*
* Maps to GANG, the users of the court,
* with multiple stages and two recursive calls (untie -> beginning, wait -> check),
* behaving similarly to the FSP model.
*/
class Gang extends Thread {
    public final static String SHARKS = "sharks"; // Constant for SHARKS name
    public final static String JETS = "jets"; // Constant for JETS name
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
            turnIndicator.setTurn(us);
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
 * Maps to COURT, the shared resource that the gangs access,
 * with two boolean-like states and go and leave to switch between them.
 */
class Court {
    // [Maps to the two states in COURT]
    private boolean inUse = false;

    // Enter the court
    public synchronized void go(String us) {
        inUse = true;
        System.out.println(us + ".court.go");
        CourtShare.printStatus(inUse);
    }

    // Leave the court
    public synchronized void leave(String us) {
        inUse = false;
        System.out.println(us + ".court.leave");
        CourtShare.printStatus(inUse);
    }
}

/*
 * Maps to HANDKERCHIEFS, the signals to each gang,
 * with the ability to be in three states like the FSP model.
 */
class Handkerchiefs {
    // [Maps to the three states in HANDKERCHIEFS]
    private int numRed = 0; // Invariant: 0 <= numRed <= 2

    // Tie own red handkerchief
    public synchronized void tie(String us) {
        if (numRed >= 2) return;
        numRed++;
        System.out.println(us + ".tie");
        CourtShare.printStatus(numRed);
    }

    // Untie own red handkerchief
    public synchronized void untie(String us) {
        if (numRed <= 0) return;
        numRed--;
        System.out.println(us + ".untie");
        CourtShare.printStatus(numRed);
    }

    // See the other handkerchief being either white or red
    public synchronized String see(String us) {
        String colour = (numRed < 2) ? "white" : "red";
        System.out.println(us + ".see." + colour);
        return colour;
    }
}

/*
 * Maps to INDICATOR, the turn indicator for fairness,
 * with the ability to be in three states like the FSP model.
 */
class Indicator {
    // [Maps to the three states in INDICATOR]
    private String turn = ""; // Invariant: turn = "" OR "sharks" OR "jets"

    // Set the turn to the other gang
    public synchronized void setTurn(String us) {
        if (turn.isEmpty()) {
            turn = (us.equals(Gang.SHARKS)) ? Gang.JETS : Gang.SHARKS;
        } else if (turn.equals(Gang.JETS)) {
            if (us.equals(Gang.SHARKS)) turn = Gang.JETS;
            else return;
        } else if (turn.equals(Gang.SHARKS)) {
            if (us.equals(Gang.JETS)) turn = Gang.SHARKS;
            else return;
        }
        System.out.println(us + ".setTurn." + turn);
        CourtShare.printStatus(turn);
    }

    // Check the indicator for the next turn
    public synchronized String checkTurn(String us) {
        if (!turn.isEmpty()) System.out.println(us + ".checkTurn." + turn);
        return turn;
    }
}