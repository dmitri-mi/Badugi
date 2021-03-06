// VERSION MARCH 6, 2018

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static easyjcckit.QuickPlot.*;

public class PLBadugiRunner {

    // The initial ante posted by both players in the hand.
    private static final int ANTE = 1;
    // How many bets and raises are allowed during one betting round.
    public static final int MAX_RAISES = 4;
    // Number of hands in each heads-up match.
    public static final int HANDS_PER_MATCH = 1_00_000;
    // How often to print out the current hand even when silent (-1 means never)
    private static final int SAMPLE_OUTPUT = 0;//HANDS_PER_MATCH;//(int)2e8;
    // Minimum raise in each betting round.
    private static final int[] MIN_RAISE = {4, 2, 2, 1};
    // Whether two agent objects of same type will play against each other in the tournament.
    private static boolean SAME_TYPE_PLAY = false;
    // How many hands have been played so far in this entire tournament.
    private static long handCount = 0;

    private static int[] numFolds = new int[2];
    private static int[] numCalls = new int[2];
    private static int[] numRaises = new int[2];

    // A utility method to output a message to the given PrintWriter, forcing it to flush() after the message.
    private static void message(PrintWriter out, String msg) {
        if(out != null) {
            out.println(msg);
            out.flush();
        }
    }
    
    /**
     * Play one hand of badugi, with both players first placing an ANTE, after which the betting takes place in fixed size
     * increments depending on the street. For simplicity, both players are assumed to have deep enough stacks so that the
     * concept of all-in does not emerge.
     * @param handSize How many cards each player receives to his hand.
     * @param deck The deck of cards used to play this hand.
     * @param players The two-element array of the players in this hand, in the order (dealer, opponent).
     * @param out The PrintWriter used to write the verbose messages about the events in this hand. If null, there is no output.
     * @param handsToGo How many hands are left in the current heads-up match.
     * @param currentScore The current score of player 0.
     * @return The result of the hand, as indicated by the amount won by player 0 from player 1. A negative result
     * therefore means that the player 0 lost the hand.
     */
    public static int playOneHand(int handSize, EfficientDeck deck, PLBadugiPlayer[] players, PrintWriter out, PrintWriter err, int handsToGo, int currentScore) {

        message(out, "\n----\nHand #" + handCount + " for " + players[0].getAgentName() + " vs. " + players[1].getAgentName()
        + ". Both players ante " + ANTE + ".");
        int pot = 2 * ANTE;
        deck.restoreCards();
        int[] totalBets = new int[2];
        totalBets[0] = totalBets[1] = ANTE;
        int[] drawCounts = new int[2];
        drawCounts[0] = drawCounts[1] = -1;
        PLBadugiHand[] hands = new PLBadugiHand[2];
        hands[0] = deck.drawBadugiHand(handSize);
        hands[1] = deck.drawBadugiHand(handSize);
        
        try {
            players[0].startNewHand(0, handsToGo, currentScore);
        } catch(Exception e) {message(err, e.toString()); return -1000; }
        try {
            players[1].startNewHand(1, handsToGo, -currentScore);
        } catch(Exception e) {message(err, e.toString()); return +1000; }

        // A single badugi hand consists of four betting streets and three draws.
        for(int drawsRemaining = 3; drawsRemaining >= 0; drawsRemaining--) {
            message(out, "Pot is " + pot + " chips, " + drawsRemaining + " draws remain.");
            message(out, players[0].getAgentName() + 
            " has " + hands[0] + ", " + players[1].getAgentName() + " has " + hands[1] + ".");

            int currPlayer = 0; // Dealer starts the betting on each street
            int calls = -1; // Number of consecutive calls made in this betting round.
            int raises = 0; // The number of bets and raises made in this betting round.
            int action; // How many chips the current player pushes into the pot in his turn to bet.
            int highestRaise = ANTE * MIN_RAISE[drawsRemaining]; // The highest raise made so far in this betting round.
 
            // Betting action for the current street
            while(calls < 1) { // Betting ends when there is a call, or when both players call in the beginning.
                int otherPlayer = 1 - currPlayer;
                int toCall = totalBets[otherPlayer] - totalBets[currPlayer];
                int minRaise, maxRaise;
                if(raises < MAX_RAISES) {
                    minRaise = Math.max(highestRaise, 2 * toCall);
                    maxRaise = Math.max(highestRaise, pot + 2 * toCall);
                }
                else { // no more raises allowed this betting round
                    minRaise = maxRaise = toCall;
                }
                try {
                    action = players[currPlayer].bettingAction(
                       drawsRemaining, hands[currPlayer], pot, raises, toCall,
                       minRaise, maxRaise, drawCounts[otherPlayer]
                    );
                    String agentName = players[currPlayer].getAgentName();
                    if(action > toCall && action < minRaise) { action = toCall; }
                    if(action > maxRaise) { action = maxRaise; }
                    message(out, agentName + " " +
                        (action < toCall ? "FOLDS" : (maxRaise > toCall && action >= minRaise ? (toCall == 0 ? "BETS" : (raises > 1 ? "RERAISES" : "RAISES")) 
                        + " " + (action - toCall) + ((toCall > 0) ? " MORE" : ""):
                        (toCall == 0 ? "CHECKS" : "CALLS " + toCall) )) + "." );
                } catch(Exception e) { // Any failure is considered a checkfold.
                    message(out, players[currPlayer].getAgentName() + " bettingAction method failed! " + e);
                    message(err, e.toString());
                    action = toCall-1;
                }
                if(action < toCall) { // current player folds, the hand is finished
                    numFolds[currPlayer]++;

                    message(out, players[otherPlayer].getAgentName() + " won " + totalBets[currPlayer] + " chips.");
                    try { players[currPlayer].handComplete(hands[currPlayer], null, -totalBets[currPlayer]); }
                    catch(Exception e)  {
                        message(out, players[currPlayer].getAgentName() + " handComplete method failed! " + e);
                        message(err, e.toString());
                    }
                    try { players[otherPlayer].handComplete(hands[otherPlayer], null, totalBets[currPlayer]); }
                    catch(Exception e) {
                        message(out, players[currPlayer].getAgentName() + " handComplete method failed! " + e);
                        message(err, e.toString());
                    }
                    return totalBets[currPlayer] * (currPlayer == 1 ? +1 : -1);
                }
                else if(action == toCall) { // current player merely calls
                    numCalls[currPlayer]++;
                    calls++;
                }
                else { // current player raises

                    numRaises[currPlayer]++;
                    raises++;
                    calls = 0;
                    // update the highest raise made on this betting round
                    if(action - toCall > highestRaise) { highestRaise = action - toCall; }
                }
                pot += action;
                totalBets[currPlayer] += action;
                currPlayer = 1 - currPlayer; // and it is now opponent's turn to act
            }
            
            if(drawsRemaining > 0) { // Drawing action for the current street.
                for(currPlayer = 0; currPlayer <= 1; currPlayer++) {
                    List<Card> cards = hands[currPlayer].getAllCards();
                    List<Card> toReplace;
                    try {
                        toReplace = players[currPlayer].drawingAction(drawsRemaining, hands[currPlayer], pot,
                        currPlayer == 0 ? -1: drawCounts[0]);
                        if(toReplace.size() > 4) {
                            message(err,"Trying to replace too many cards");
                            throw new IllegalArgumentException("Trying to replace too many cards.");
                        }
                        message(out, players[currPlayer].getAgentName() + " replaces cards " + toReplace + ".");
                        for(Card c: toReplace) {
                            if(!cards.contains(c)) {
                                message(err,"Trying to replace nonexistent card");
                                throw new IllegalArgumentException("Trying to replace nonexistent card " + c);
                            }
                            hands[currPlayer].replaceCard(c, deck);
                        }
                        drawCounts[currPlayer] = toReplace.size();
                    } catch(Exception e) {
                        message(out, players[currPlayer].getAgentName() + ": drawingAction method failed: " + e);
                        message(err, e.toString());
                        numFolds[currPlayer]++;
                        return totalBets[currPlayer] * (currPlayer == 1 ? +1 : -1);
                    }
                }
            }
        }
        
        message(out, "The hand has reached the showdown.");
        message(out, players[0].getAgentName() + " has " + hands[0] + ".");
        message(out, players[1].getAgentName() + " has " + hands[1] + ".");
        
        // Bug found and fix provided by Alex Ladd March 6 2018
        int showdown = hands[0].compareTo(hands[1]);
        int result = showdown < 0 ? -totalBets[0] : (showdown > 0 ? totalBets[1] : 0);
        if(showdown != 0) {
            message(out, players[showdown > 0 ? 0 : 1].getAgentName() +" won " + totalBets[1] + " chips.");
            try { players[0].handComplete(hands[0], hands[1], showdown > 0 ? totalBets[0] : -totalBets[0]); }
            catch(Exception e) {
                message(out, players[0].getAgentName() + " handComplete method failed! " + e);
                message(err, e.toString());
            }
            try { players[1].handComplete(hands[1], hands[0], showdown < 0 ? totalBets[1] : -totalBets[1]); }
            catch(Exception e) {
                message(out, players[1].getAgentName() + " handComplete method failed! " + e);
                message(err, e.toString());
            }
        }
        else {
            message(out, "Both players brought equal badugi hands to showdown.");
            try { players[0].handComplete(hands[0], hands[1], 0); }
            catch(Exception e) {
                message(out, players[0].getAgentName() + " handComplete method failed! " + e);
                message(err, e.toString());
            }
            try { players[1].handComplete(hands[1], hands[0], 0); }
            catch(Exception e) {
                message(out, players[1].getAgentName() + " handComplete method failed! " + e);
                message(err, e.toString());
            }
        }
        return result;
    }
    
    /**
     * Play the given number of hands of heads-up badugi between the two players, alternating the dealer position
     * between each round.
     * @param players The two players participating in this heads-up match.
     * @param out The PrintWriter used to write the verbose messages about the events in this hand. To silence this output,
     * use e.g. new FileWriter("/dev/null") as this argument in an Unix system.
     * @param hands How many hands to play in this heads-up match.
     * @return The result of the match, as indicated by the amount won by player 0 from player 1. A negative result
     * therefore means that the player 0 lost the match.
     */
    public static int playHeadsUp(EfficientDeck deck, PLBadugiPlayer[] players, PrintWriter out, PrintWriter err, int hands) {
        int score = 0;
        PLBadugiPlayer[] thisRoundPlayers = new PLBadugiPlayer[2];
        players[0].startNewMatch(hands);
        players[1].startNewMatch(hands);

        PLBadugi500877176 me = players[0] instanceof PLBadugi500877176 ? (PLBadugi500877176) players[0] : null;
        if(me==null)      me = players[1] instanceof PLBadugi500877176 ? (PLBadugi500877176) players[1] : null;

        numFolds = new int[players.length];
        numCalls = new int[players.length];
        numRaises = new int[players.length];

        for (int i = 0; i < players.length; i++) {
            numFolds [i]=0;
            numCalls [i]=0;
            numRaises[i]=0;
        }

        int[] scoresPerMatch = new int[hands];
        double[] thetaPerMatch = new double[hands];
        int handsInMatch = 0;

        while(--hands >= 0) {
            if(hands % 2 == 0) { thisRoundPlayers[0] = players[0]; thisRoundPlayers[1] = players[1]; }
            else { thisRoundPlayers[0] = players[1]; thisRoundPlayers[1] = players[0]; }
            int sign = (hands % 2 == 0 ? +1 : -1);
            handCount++;

            PrintWriter out2=null;
            if(SAMPLE_OUTPUT > 0 && handCount % SAMPLE_OUTPUT == 0 && out == null) {
                out2 = new PrintWriter(System.out);
            }
            else if(out != null) out2=out;

            int matchScore = sign * playOneHand(4, deck, thisRoundPlayers, out2, err, hands, sign * score);

            score += matchScore; // total score

            scoresPerMatch[handsInMatch] = matchScore; // current match score
            thetaPerMatch[handsInMatch] = me!=null ? me.thetaNorm() : 0.0;
            handsInMatch++;
        }
        players[0].finishedMatch(score);
        players[1].finishedMatch(-score);

        showProgress(new PrintWriter(System.out), scoresPerMatch, thetaPerMatch, players);
        return score;
    }

    public static void showProgress(PrintWriter out, int[] scores, double[] thetaPerMatch, PLBadugiPlayer[] players) {

        final int N = 100; // length of running average
        final int Shift = 50; // difference to next score average since we can't show all million scores on graph
        final int stopIndex = Math.min(100000, scores.length);

        int newLen = (scores.length - N + 1) / Shift;
        if(newLen <= 0) return;

        double[] xEpisode = new double[newLen];
        double[] yScore = new double[newLen];
        double[] yTheta = new double[newLen];

        int k=0;
        for (int i = N - 1; i < stopIndex && k<xEpisode.length; i = i + Shift, k++) {
            xEpisode[k] = i;

            double sum = 0.0;
            for (int j = i - N + 1; j <= i; j++) {
                sum += scores[j];
            }
            yScore[k] = sum / N;
            yTheta[k] = thetaPerMatch[i];
        }

        // cut off zeros at the end
        xEpisode = Arrays.copyOfRange(xEpisode,0, k-1);
        yScore = Arrays.copyOfRange(yScore,0, k-1);
        yTheta = Arrays.copyOfRange(yTheta,0, k-1);

        if(out !=null) {
            // message(out, "score max: " + max(scores));
            // message(out, "score min: " + min(scores));
            message(out, " " );
            message(out, "score avg: " + avg(scores));
            message(out, "theta avg: " + avg(yTheta));

            for(int i=0;i<players.length; i++) {

                message(out, " " );
                PLBadugiPlayer p = players[i];
                message(out, ""+p.getAgentName());
                message(out, "Folds  : " + numFolds[i]);
                message(out, "Calls  : " + numCalls[i]);
                message(out, "Raises : " + numRaises[i]);
                message(out, "Total : " + (numFolds[i]+numCalls[i] +numRaises[i]));
                message(out, "Aggression : " + numRaises[i]/(double)numCalls[i]);
                message(out, "Aggression2 : " + (numRaises[i]+numCalls[i])/(double)numFolds[i]);
            }
        }
        plot( xEpisode, yScore ); // a plot for score average
        //addLine( xEpisode, yTheta ); // a plot for theta average
    }

    private static double max(int[] array){
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if(max < array[i]) max = array[i];
        }
        return max;
    }

    private static double min(int[] array){
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            if(min > array[i]) min = array[i];
        }
        return min;
    }

    private static double avg(int[] array){
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum/array.length;
    }

    private static double avg(double[] array){
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum/array.length;
    }
    
    /**
     * Play the entire multiagent Badugi tournament, one heads-up match between every possible pair of agents.
     * @param agentClassNames A string array containing the names of agent subclasses.
     * @param out A PrintWriter to write the results of the individual heads-up matches into.
     * @param results A PrintWriter to write the tournament results into.
     */
    public static void badugiTournament(String[] agentClassNames, PrintWriter out, PrintWriter results) {
        
        // Create the list of player agents.
        List<PLBadugiPlayer> players = createPlayers(agentClassNames);

        if (players == null) return;
        int[] scores = new int[players.size()];
        Random rng;
        String seed = "This string is to be used as seed of secure random number generator " + System.currentTimeMillis();
        try { rng = new SecureRandom(seed.getBytes()); } 
        catch(Exception e) { 
            message(out, "Unable to create secure RNG: " + e);
            message(out, "Using system Random class instead.");

            rng = new Random();
        }
        // One and the same deck object is reused through the entire tournament.
        EfficientDeck deck = new EfficientDeck(rng);

        PrintWriter err = new PrintWriter(System.out);

        // Play and score the individual heads-up matches.
        for(int i = 0; i < players.size(); i++) {
            for(int j = i+1; j < players.size(); j++) {

                if(!SAME_TYPE_PLAY && agentClassNames[i].equals(agentClassNames[j])) { continue; }

                PLBadugiPlayer[] playersArr = { players.get(i), players.get(j) };

                int result = playHeadsUp(deck, playersArr, null, err, HANDS_PER_MATCH);

                if(result < 0) { scores[j] += 2; }
                else if(result > 0) { scores[i] += 2; }
                else { scores[j]++; scores[i]++; }

                out.print("["+players.get(i).getAgentName() + "] vs. [" + players.get(j).getAgentName() + "]: ");
                out.println(result);
                out.flush();
            }
        }

        updateScores(results, players, scores);
    }

    private static void updateScores(PrintWriter results, List<PLBadugiPlayer> players, int[] scores) {
        for(int i = 0; i < players.size(); i++) {
            int max = 0;
            for(int j = 1; j < players.size(); j++) {
                if(scores[j] > scores[max]) { max = j; }
            }
            String name = players.get(max).getAgentName();
            results.println((i+1) + " : " + name + " : " + scores[max]);
            scores[max] = -scores[max];
        }

        for(int i = 0; i < players.size(); i++) {
            scores[i] = -scores[i];
        }

        results.println("\n\n");

        for(int i = 0; i < players.size(); i++) {
            int max = 0;
            for(int j = 1; j < players.size(); j++) {
                if(scores[j] > scores[max]) { max = j; }
            }
            int pos = players.size() - i;
            results.println((i + 1) + ": " + players.get(max).getAuthor());
            scores[max] = -scores[max];
        }
    }

    private static List<PLBadugiPlayer> createPlayers(String[] agentClassNames) {
        List<PLBadugiPlayer> players = new ArrayList<PLBadugiPlayer>(agentClassNames.length);
        for(String agent: agentClassNames) {
            Class c = null;
            try {
                c = Class.forName(agent);
            } catch(Exception e) {
                System.out.println("Unable to load class bytecode for [" + agent + "]. Exiting.");
                return null;
            }
            PLBadugiPlayer bp = null;
            try {
                bp = (PLBadugiPlayer)(c.newInstance());
            } catch(Exception e) {
                System.out.println("Unable to instantiate class [" + agent + "]. Exiting.");
                return null;
            }
            players.add(bp);
        }
        return players;
    }

    /**
     * Play three hands in the verbose mode. Suitable for watching your agents play.
     */
    public static void playThreeHandTournament() throws IOException {
        SAME_TYPE_PLAY = true;

        PLBadugiPlayer[] players = { 
            // Replace these with some suitable objects.
                new PLBadugi500877176(),//new IlkkaPlayer3(),
                new PLBadugi500877176()
        };
        Random rng;
        String seed = "This string is to be used as seed of secure random number generator"+System.currentTimeMillis();
        try { rng = new SecureRandom(seed.getBytes()); } 
        catch(Exception e) { 
            System.out.println("Unable to create a secure RNG. Using java.util.Random instead.");
            rng = new Random();
        }
        EfficientDeck deck = new EfficientDeck(rng);

        PrintWriter out = new PrintWriter(System.out);
        PrintWriter err = new PrintWriter(System.out);

        int result = playHeadsUp(deck, players, out, err, 3);
        System.out.println("\n\nMatch result is " + result + ".");
    }
    
    /**
     * Run the entire badugi tournament between agents from classes listed inside this method.
     */
    public static void main2(String[] args) throws IOException {
        playThreeHandTournament();
    }

    public static void main(String[] args) throws IOException {

        // playThreeHandTournament();

        /* Modify this array to include the player classes that participate in the tournament. */
        String[] playerClasses = {

                //"SimplePlayer"
                "IlkkaPlayer3" ,
                "PLBadugi500877176",
        };
        
        PrintWriter out = new PrintWriter(System.out);
        PrintWriter result = new PrintWriter(new FileWriter("results.txt"));

        final int Replay = 1;
        for (int i = 0; i < Replay; i++) {
            badugiTournament(playerClasses, out, result);
        }

        result.close();
    }
}
