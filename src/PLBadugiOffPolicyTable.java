import java.util.Arrays;
import java.util.List;

public class PLBadugiOffPolicyTable implements PLBadugiPlayer {

    private static int instanceCounter = 0;
    private String name = "Obi-Wan-";

    private int handsToGo;
    private int position;
    private int drawsRemaining;
    private int raises;
    private int opponentDrew;
    private int agentDrew;
    private Tabular.State prevState; // the previous state of Q-value: s
    private Tabular.Action prevAction; // the action that was taken to go from prevState to newState
    private Tabular.Qtable qtable;

    public PLBadugiOffPolicyTable() {
        instanceCounter++;
        name += instanceCounter;

        qtable = new Tabular.Qtable();
    }

    @Override
    public void startNewMatch(int handsToGo) {

        this.handsToGo = handsToGo;
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

        this.position = position;
        this.handsToGo = handsToGo;
        this.prevAction = Tabular.RandomHelper.getRandomBetAction();
        this.opponentDrew = 0;
        this.agentDrew = 0;
        this.raises = 0;
        this.drawsRemaining = 3;
        // int myScore = position==0 ?  currentScore : -currentScore;

        prevState = encodeToState(new PLBadugiHand(Arrays.asList(new Card(0,12))));
    }

    private final Tabular.State encodeToState(PLBadugiHand hand){

        int[] active = hand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        return Tabular.State.Encode(
                position,
                drawsRemaining,
                raises,
                handActiveLength,
                handActiveFirstRank,
                opponentDrew,
                agentDrew
        );
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

        this.drawsRemaining = drawsRemaining;
        this.raises = raises;
        this.opponentDrew = opponentDrew;

        int[] active = hand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        Tabular.State newState = encodeToState(hand);

        qtable.update(prevState, newState,0.0, prevAction);

        Tabular.Action bestAction = qtable.bestAction(newState, true);
        this.prevAction = bestAction;
        this.prevState = newState;

        return convertToChips(bestAction, toCall,  minRaise,  maxRaise);

        // return instaceIndex==2 ?  toCall : minRaise;
    }

    private int convertToChips(Tabular.Action action, int toCall, int minRaise, int maxRaise) {
        if(action == Tabular.Action.Fold) return toCall-1;
        if(action == Tabular.Action.Call) return toCall;
        if(action == Tabular.Action.MinRaise) return minRaise;
        if(action == Tabular.Action.MaxRaise) return maxRaise;
        if(action == Tabular.Action.MiddleRaise){
            if(minRaise == maxRaise) return minRaise;
            return (maxRaise - minRaise)/2+ minRaise;
        }

        throw new IllegalArgumentException("Action is not supported: "+action);
    }

    @Override
    public List<Card> drawingAction(int drawsRemaining, PLBadugiHand hand, int pot, int dealerDrew) {
        this.drawsRemaining = drawsRemaining;

        if(dealerDrew != -1){
            this.opponentDrew = dealerDrew;
        }

        Tabular.State newState = encodeToState(hand);

        qtable.update(prevState, newState,0.0, prevAction);

        Tabular.Action bestAction = qtable.bestAction(newState, false);

        List<Card> replaceCards = convertToDraw(bestAction, hand);

        this.agentDrew += replaceCards.size();
        this.prevState = newState;
        this.prevAction = bestAction;

        return replaceCards;
    }

    private final List<Card> convertToDraw(Tabular.Action action, PLBadugiHand hand){

        List<Card> inactiveCards = hand.getInactiveCards();
        int countInactive = inactiveCards.size();
        int cardsToDraw = action.toInt() - Tabular.Action.DrawZero.toInt();

        if(cardsToDraw < 0 || cardsToDraw > 4) throw new IllegalArgumentException("No support for draw action: "+action);

        int count = Math.min(cardsToDraw, countInactive); // limit the number of cards to draw by count of inactive
        return inactiveCards.subList(0, count);
    }

    @Override
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {

        int[] active = yourHand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        Tabular.State newState = Tabular.State.Encode(
                this.position,
                this.drawsRemaining,
                this.raises,
                handActiveLength,
                handActiveFirstRank,
                this.opponentDrew,
                this.agentDrew
        );

        qtable.update(prevState, newState, result, prevAction);

        this.prevState = newState; // don't need this at the end of the hand ?
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
