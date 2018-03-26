import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

enum Action{
    Fold(Qaction.FoldAction),
    Call(Qaction.CallAction),
    MinRaise(Qaction.MinRaiseAction),
    MiddleRaise(Qaction.MiddleRaiseAction),
    MaxRaise(Qaction.MaxRaiseAction),

    DrawZero(Qaction.DrawZeroAction),
    DrawOne(Qaction.DrawOneAction),
    DrawTwo(Qaction.DrawTwoAction),
    DrawThree(Qaction.DrawThreeAction),
    DrawFour(Qaction.DrawFourAction);

    private int index;

    public static int NumBetActions = 5;
    public static int NumDrawActions = 5;

    Action(int index){this.index=index;}

    public final int toInt(){return index;}

    public static final Action fromInt(int index){
        if(index == Qaction.FoldAction) return Fold;
        if(index == Qaction.CallAction) return Call;
        if(index == Qaction.MinRaiseAction) return MinRaise;
        if(index == Qaction.MiddleRaiseAction) return MiddleRaise;
        if(index == Qaction.MaxRaiseAction) return MaxRaise;

        if(index == Qaction.DrawZeroAction ) return DrawZero;
        if(index == Qaction.DrawOneAction  ) return DrawOne;
        if(index == Qaction.DrawTwoAction  ) return DrawTwo;
        if(index == Qaction.DrawThreeAction) return DrawThree;
        if(index == Qaction.DrawFourAction ) return DrawFour;

        throw new IllegalArgumentException("Enum Action cannot be created from integer: "+index);
    }

    public final boolean isBet() {
        return index <= Qaction.MaxRaiseAction;
    }
}

class Qvalue {
    private double value;
    private int counter; // counter - number of times we got into this (state,action)
    public Qvalue(double value, int counter){this.value=value; this.counter=counter;}
    public final void setValue(double v){value = v;}
    public final double getValue(){return value;}
    public final int getCounter(){return counter;}
    public final void incrementCounter(){counter++;}
}

/*
 list that has Q-value for each action of the given state.
 action a: 0,1,2,3,4 : 0=fold, 1=toCall, 2=minRaise, 3=(maxRaise+minRaise)/2, 4=maxRaise
 */
class Qaction {
    private Qvalue[] qValues;

    // for each action a: 0,1,2,3,4 : 0=fold, 1=toCall, 2=minRaise, 3=(maxRaise+minRaise)/2, 4=maxRaise
    private static final int ActionLength = 5+5;

    // action index:
    public static final int FoldAction=0;
    public static final int CallAction=1;
    public static final int MinRaiseAction=2;
    public static final int MiddleRaiseAction=3;
    public static final int MaxRaiseAction=4;

    public static final int DrawZeroAction=5;
    public static final int DrawOneAction=6;
    public static final int DrawTwoAction=7;
    public static final int DrawThreeAction=8;
    public static final int DrawFourAction=9;

    public Qaction(){
        if (qValues == null) {
            qValues = new Qvalue[ActionLength];

            for (int i=0; i< ActionLength; i++){
                qValues[i] = new Qvalue(0,0);
            }
        }
    }

    public final void setActionValue(Action action, double v) {
        Qvalue qvalue = qValues[action.toInt()];
        qvalue.setValue(v);
        qvalue.incrementCounter();
    }

    public final double getActionValue(Action action){
        return qValues[action.toInt()].getValue();
    }

    public final int getActionCounter(Action action){
        return qValues[action.toInt()].getCounter();
    }

    public final MaxValue getMaxBetActionValue(){
        return getMaxActionValue(FoldAction, MaxRaiseAction);
    }

    public final MaxValue getMaxDrawActionValue(){
        return getMaxActionValue(DrawZeroAction, DrawFourAction);
    }

    private final MaxValue getMaxActionValue(int startIndex, int endIndex){
        double max = qValues[startIndex].getValue();
        int bestActionIndex = startIndex;

        for(int i=startIndex; i<=endIndex; i++) {
            Qvalue q = this.qValues[i];
            double otherActionValue = q.getValue();
            if (max < otherActionValue) {
                max = otherActionValue;
                bestActionIndex = i;
            }
        }

        return new MaxValue(Action.fromInt(bestActionIndex), max);
    }
}

class MaxValue {
    public Action action;
    public double value;
    public MaxValue(Action action, double value){
        this.action=action;
        this.value=value;
    }
}

class State implements Comparable<State>{

//    int position; // 0 if the agent is the dealer in this hand, 1 if the opponent.
//    int drawsRemaining;  // 3,2,1,0
//    int raises;          // 0,1,2,3,4
//    int handActiveLength;// 1,2,3,4
//    int handActiveFirstRank; // 1,2,..,13
//    int opponentDrew;        // -1 , 0,1,2,3,4
//    int agentDrew;      // -1,0,1,2,3,4
//    //int potSize; //pot 2..32*10^6 => int[log10(pot)]

    int encoded;

    private State(int encoded){this.encoded = encoded;}

    @Override
    public int compareTo(State o) {
        if(o==null)return 1;
        return Integer.compare(encoded, o.encoded);
    }
    
    public static State Encode(
            int position,            // 0 if the agent is the dealer, 1 - otherwise
            int drawsRemaining,      // 3,2,1,0
            int raises,              // 0,1,2,3,4
            int handActiveLength,    // 1,2,3,4        => every value is subtracted by 1
            int handActiveFirstRank, // 1,2,..,13      => every value is subtracted by 1
            int opponentDrew,        // -1,0,1,2,3,4 => -1 is converted to 0
            int agentDrew           // -1,0,1,2,3,4   => -1 is converted to 0
    )
    {
        if(position != 0 && position != 1) throw new IllegalArgumentException("position: "+position);
        if(drawsRemaining > 3 || drawsRemaining < 0) throw new IllegalArgumentException("drawsRemaining: "+drawsRemaining);
        if(raises > 4 || raises < 0) throw new IllegalArgumentException("raises: "+raises);
        if(handActiveLength > 4 || handActiveLength < 1)
            throw new IllegalArgumentException("handActiveLength: " + handActiveLength);
        if(handActiveFirstRank > 13 || handActiveFirstRank < 1)
            throw new IllegalArgumentException("handActiveFirstRank: "+handActiveFirstRank);

        if(opponentDrew > 4) opponentDrew = 4; // clip max
        if(opponentDrew < -1)
            throw new IllegalArgumentException("opponentDrew: "+opponentDrew);

        if(agentDrew > 4) agentDrew =4; // clip max
        if(agentDrew > 4 || agentDrew < -1)
            throw new IllegalArgumentException("agentDrew: "+ agentDrew);

        int res = 0;

        int shift = 0;
        res |= (position & 0b1); shift=shift+1;
        res |= (drawsRemaining & 0b11)<<shift; shift=shift+2;
        res |= (raises & 0b111)<<shift; shift = shift+3;
        res |= ((handActiveLength-1) & 0b11)<<shift; shift = shift + 2;
        res |= ((handActiveFirstRank-1) & 0b1111 )<<shift; shift = shift + 4;

        if(opponentDrew == -1) opponentDrew = 0;
        res |= (opponentDrew & 0b111)<<shift; shift = shift + 3;

        if(agentDrew == -1) agentDrew = 0;
        res |= (agentDrew & 0b111)<<shift; shift = shift + 3;

        return new State(res);
    }

    public static DecodedState Decode(State s){
        DecodedState d = new DecodedState();

        int encoded  = s.encoded;

        d.position = encoded & 0b1; encoded = encoded>>1;
        d.drawsRemaining = encoded & 0b11; encoded = encoded>>2;
        d.raises = encoded & 0b111; encoded = encoded>>3;
        d.handActiveLength = encoded & 0b11; encoded = encoded>>2;
        d.handActiveFirstRank = encoded & 0b1111; encoded = encoded>>4;
        d.opponentDrew = encoded & 0b111; encoded = encoded>>3;
        d.agentDrew = encoded & 0b111; encoded = encoded >> 3;

        return d;
    }
}

class DecodedState{
    int position;// 0 if the agent is the dealer or 1 - otherwise
    int drawsRemaining; // 3,2,1,0
    int raises;         // 0,1,2,3,4            
    int handActiveLength; // 1,2,3,4
    int handActiveFirstRank; // 1,2,..,13
    int opponentDrew;       // -1,0,1,2,3,4
    int agentDrew;    // -1,0,1,2,3,4
}

class RandomHelper{

    private static Random random = create();

    private static final Random create(){
        Random rng;
        String seed = "My string is to be used as seed of secure random number generator"+System.currentTimeMillis();
        try { rng = new SecureRandom(seed.getBytes()); }
        catch(Exception e) {
            //System.out.println("Unable to create a secure RNG. Using java.util.Random instead.");
            rng = new Random();
        }
        return rng;
    }

    public static final Action getRandomBetAction(){
        int index = random.nextInt(Action.NumBetActions) + Action.Fold.toInt();
        Action action = Action.fromInt(index);
        return action;
    }

    public static final Action getRandomDrawAction(){
        int index = random.nextInt(Action.NumDrawActions) + Action.DrawZero.toInt();
        Action action = Action.fromInt(index);
        return action;
    }
}

class Qtable{

    private HashMap<State, Qaction> Qmap = new HashMap<State, Qaction>();
    private double alpha = 0.1; // learning rate
    private double gamma = 0.9; // discount factor

    public Qtable(){
        resetRate();
    }

    public void resetRate() {
        // reset learning for new match
        alpha = 0.3;
        gamma = 0.99;
    }

    public Action bestAction(State currentState, boolean isBet){
        Qaction q = Qmap.getOrDefault(currentState,null);

        if(q == null){
            Action a = isBet ? RandomHelper.getRandomBetAction(): RandomHelper.getRandomDrawAction();
            return a;
        }
        else{
            return isBet ? q.getMaxBetActionValue().action : q.getMaxDrawActionValue().action;
        }
    }

    public void update(State prevState, State newState, double reward, Action action){
        Qaction q = Qmap.getOrDefault(prevState, null);

        double prevQ = 0.0;
        if(q==null){
            q = new Qaction();
            Qmap.put(prevState, q);
        }
        else{
            prevQ = q.getActionValue(action);
        }

        double maxQ = 0.0;// max Q(s-tag, action) , for all action
        Qaction qNew = Qmap.getOrDefault(newState,null);
        if(qNew != null) {
            MaxValue maxValue = action.isBet() ? q.getMaxBetActionValue() : q.getMaxDrawActionValue();
            maxQ = maxValue.value;
        }

        double newReward = (1-alpha) * prevQ + alpha * (reward + gamma * maxQ);

        q.setActionValue(action, newReward);
    }
}

public class PLBadugi500877176 implements PLBadugiPlayer {

    private static int instanceCounter = 0;
    private String name = "Obi-Wan-";

    private int handsToGo;
    private int position;
    private int drawsRemaining;
    private int raises;
    private int opponentDrew;
    private int agentDrew;
    private State prevState; // the previous state of Q-value: s
    private Action prevAction; // the action that was taken to go from prevState to newState
    private Qtable qtable;

    public PLBadugi500877176() {
        instanceCounter++;
        name += instanceCounter;

        qtable = new Qtable();
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
        this.prevAction = RandomHelper.getRandomBetAction();
        this.opponentDrew = 0;
        this.agentDrew = 0;
        this.raises = 0;
        this.drawsRemaining = 3;
        // int myScore = position==0 ?  currentScore : -currentScore;

        prevState = encodeToState(new PLBadugiHand(Arrays.asList(new Card(0,12))));
    }

    private final State encodeToState(PLBadugiHand hand){

        int[] active = hand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        return State.Encode(
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

        State newState = encodeToState(hand);

        qtable.update(prevState, newState,0.0, prevAction);

        Action bestAction = qtable.bestAction(newState, true);
        this.prevAction = bestAction;
        this.prevState = newState;

        return convertToChips(bestAction, toCall,  minRaise,  maxRaise);

        // return instaceIndex==2 ?  toCall : minRaise;
    }

    private int convertToChips(Action action, int toCall, int minRaise, int maxRaise) {
        if(action == Action.Fold) return toCall-1;
        if(action == Action.Call) return toCall;
        if(action == Action.MinRaise) return minRaise;
        if(action == Action.MaxRaise) return maxRaise;
        if(action == Action.MiddleRaise){
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

        State newState = encodeToState(hand);

        qtable.update(prevState, newState,0.0, prevAction);

        Action bestAction = qtable.bestAction(newState, false);

        List<Card> replaceCards = convertToDraw(bestAction, hand);

        this.agentDrew += replaceCards.size();
        this.prevState = newState;
        this.prevAction = bestAction;

        return replaceCards;
    }

    private final List<Card> convertToDraw(Action action, PLBadugiHand hand){

        List<Card> inactiveCards = hand.getInactiveCards();
        int countInactive = inactiveCards.size();
        int cardsToDraw = action.toInt() - Action.DrawZero.toInt();

        if(cardsToDraw < 0 || cardsToDraw > 4) throw new IllegalArgumentException("No support for draw action: "+action);

        int count = Math.min(cardsToDraw, countInactive); // limit the number of cards to draw by count of inactive
        return inactiveCards.subList(0, count);
    }

    @Override
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {

        int[] active = yourHand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        State newState = State.Encode(
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
