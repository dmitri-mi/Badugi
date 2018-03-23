import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Qvalue{
    public double value;
    public int counter; // counter - number of times we got into this (state,action)
    public Qvalue(double value, int counter){this.value=value; this.counter=counter;}
}

class Q{
    // for each action a: 0,1,2,3,4 : 0=fold, 1=toCall, 2=minRaise, 3=(maxRaise+minRaise)/2, 4=maxRaise
    private static int ActionLength = 5;
    private Qvalue[] Qvalues;

    public static final int FoldAction=0;
    public static final int CallAction=1;
    public static final int MinRaiseAction=2;
    public static final int MiddleRaiseAction=3;
    public static final int MaxRaiseAction=4;

    public static void setActionValue(Q self, int actionIndex, double v) { // make it static in order to conserve memory

        if (self.Qvalues == null) self.Qvalues = new Qvalue[ActionLength];

        if (self.Qvalues[actionIndex] == null)
            self.Qvalues[actionIndex] = new Qvalue(v, 0);

        self.Qvalues[actionIndex].value = v;
        self.Qvalues[actionIndex].counter++;
    }
}


class Qtable{

    private HashMap<Integer,Q> Qmap = new HashMap<Integer,Q>();
    private double alpha = 0.1; // learning rate
    private double gamma = 0.9; // discount factor

    public void resetRate() {
        // reset learning for new match
        alpha = 0.1;
        gamma = 0.9;
    }
}

public class PLBadugi500877176 implements PLBadugiPlayer {

    private static int instanceCounter = 0;
    private String name = "Obi-Wan-";
    private int instaceIndex;

    private Qtable qtable;

    public PLBadugi500877176() {
        instanceCounter++;
        name += instanceCounter;
        instaceIndex = instanceCounter;

        qtable = new Qtable();
    }

    @Override
    public void startNewMatch(int handsToGo) {

        qtable.resetRate();
    }

    @Override
    public void finishedMatch(int finalScore) { }

    /**
     * The method to inform the agent that a new hand is starting.
     * @param position 0 if the agent is the dealer in this hand, 1 if the opponent.
     * @param handsToGo The number of hands left to play in this heads-up tournament.
     * @param currentScore The current score of the tournament.
     */
    @Override
    public void startNewHand(int position, int handsToGo, int currentScore) {

        int myScore = position==0 ?  currentScore : -currentScore;
    }

    /**
     * The method to ask the agent what betting action it wants to perform.
     * @param drawsRemaining How many draws are remaining after this betting round: 3,2,1,0
     * @param hand The current hand held by this player.
     * @param pot The current size of the pot.
     * @param raises The number of raises made in this round: 0,1,2,3,4
     * @param toCall The cost to call to stay in the pot.
     * @param minRaise The minimum allowed raise to make, if the agent wants to raise.
     * @param maxRaise The maximum allowed raise to make, if the agent wants to raise.
     * @param opponentDrew How many cards the opponent drew in the previous drawing round. In the
     * first betting round, this argument will be -1. 0,1,2,3,4
     * @return The amount of chips that the player pushes into the pot. Putting in less than
     * toCall means folding. Any amount less than minRaise becomes a call, and any amount between
     * minRaise and maxRaise, inclusive, is a raise. Any amount greater than maxRaise is clipped at
     * maxRaise.
     */
    @Override
    public int bettingAction(int drawsRemaining, PLBadugiHand hand, int pot, int raises,
                             int toCall, int minRaise, int maxRaise, int opponentDrew) {

        int[] active = hand.getActiveRanks();
        int activeCount = active.length; // [1,2,3,4]
        int activeFirstRank = active[0]; // from 1 (ace) to 13 (king)

        return maxRaise;
        // return instaceIndex==2 ?  toCall : minRaise;
    }

    @Override
    public List<Card> drawingAction(int drawsRemaining, PLBadugiHand hand, int pot, int dealerDrew) {
        return new ArrayList<Card>();
    }

    @Override
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {


    }

    @Override
    public String getAgentName() {
        return name;
    }

    @Override
    public String getAuthor() {
        return "Dmitri Minkin";
    }
}
