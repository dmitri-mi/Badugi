import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

enum Action{
    Fold(ActionHelper.FoldAction),
    Call(ActionHelper.CallAction),
    MinRaise(ActionHelper.MinRaiseAction),
    MiddleRaise(ActionHelper.MiddleRaiseAction),
    MaxRaise(ActionHelper.MaxRaiseAction),

    DrawZero(ActionHelper.DrawZeroAction),
    DrawOne(ActionHelper.DrawOneAction),
    DrawTwo(ActionHelper.DrawTwoAction),
    DrawThree(ActionHelper.DrawThreeAction),
    DrawFour(ActionHelper.DrawFourAction);

    private int index;

    public static int NumBetActions = 5;
    public static int NumDrawActions = 5;

    Action(int index){this.index=index;}

    public final int toInt(){return index;}

    public static Action fromInt(int index){
        if(index == ActionHelper.FoldAction) return Fold;
        if(index == ActionHelper.CallAction) return Call;
        if(index == ActionHelper.MinRaiseAction) return MinRaise;
        if(index == ActionHelper.MiddleRaiseAction) return MiddleRaise;
        if(index == ActionHelper.MaxRaiseAction) return MaxRaise;

        if(index == ActionHelper.DrawZeroAction ) return DrawZero;
        if(index == ActionHelper.DrawOneAction  ) return DrawOne;
        if(index == ActionHelper.DrawTwoAction  ) return DrawTwo;
        if(index == ActionHelper.DrawThreeAction) return DrawThree;
        if(index == ActionHelper.DrawFourAction ) return DrawFour;

        throw new IllegalArgumentException("Enum Action cannot be created from integer: "+index);
    }

    public final boolean isBet() {
        return index <= ActionHelper.MaxRaiseAction;
    }

    public static List<Action> allActions(boolean isBet) {

        List<Action> selectedActions = new ArrayList<>();
        Action[] all = Action.values();

        if(isBet) { for(Action a : all) {if( a.isBet()) selectedActions.add(a);}}
        else      { for(Action a : all) {if(!a.isBet()) selectedActions.add(a);}}

        return selectedActions;
    }

    // list that has Q-value for each action of the given state.
// action a: 0,1,2,3,4 : 0=fold, 1=toCall, 2=minRaise, 3=(maxRaise+minRaise)/2, 4=maxRaise
//           5,6,7,8,9 : 5=draw-0,6=draw-1,7=draw-2,8=draw-3,9=draw-4
    class ActionHelper {
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
    }
}

class State{
    int position;// 0 if the agent is the dealer or 1 - otherwise
    int drawsRemaining; // 3,2,1,0
    int raises;         // 0,1,2,3,4
    int opponentDrew;       // -1,0,1,2,3,4
    int agentDrew;    // -1,0,1,2,3,4
    int pot; // 2..3*10^6

    int handActiveLength; // 1,2,3,4
    int handActiveFirstRank; // 1,2,..,13
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

    public static final boolean nextActionShouldBeRandom(double epsilon){
        double d = random.nextDouble(); // next random double from interval [0..1]
        if(d<=epsilon) return true; // make epsilon greedy only epsilon amount of time
        return false; // otherwise use regular algorithm for action
    }

    public static Action getRandomAction(boolean isBet) {
        if(isBet) return getRandomBetAction();
        return getRandomDrawAction();
    }
}

class Vector {
    private double[] weight;

    public Vector(int length){
        weight = new double[length];

        for (int i=0; i<length; i++){
            weight[i]=0.0;
        }
    }

    public Vector(double[] values){
        if (values==null){
            throw new IllegalArgumentException("vector length is incompatible");
        }

        weight = new double[values.length];

        for (int i=0; i<weight.length; i++){
            weight[i]=values[i];
        }
    }

    public void add(final Vector other){
        validate(other);

        for (int i=0; i<weight.length; i++){
            weight[i] += other.weight[i];
        }
    }

    public void multiply(double scalar){
        for (int i=0; i<weight.length; i++){
            weight[i] *= scalar;
        }
    }

    public double innerProduct(Vector other){
        validate(other);

        double sum = 0.0;
        for (int i=0; i<weight.length; i++){
            sum += weight[i] * other.weight[i];
        }
        return sum;
    }

    private final void validate(Vector other){
        if(other == null || other.weight.length != this.weight.length) {
            throw new IllegalArgumentException("vector length is incompatible");
        }
    }

    public final double norm1(){
        double sum = 0.0;
        for (double v: this.weight) {
            sum += Math.abs(v);
        }
        return sum;
    }

    public final double norm2(){
        double sum = 0.0;
        for (double v: this.weight) {
            sum += v*v;
        }
        sum = Math.sqrt(sum);
        return sum;
    }
}

class Sarsa {

    private double alpha; // learning rate
    private double gamma; // discount factor
    private double epsilon; // epsilon greedy parameter for exploration vs exploitation
    private double epsilonZero; // starting value of epsilon
    private int handsToGo;
    private int episodeCounter; // counts number of episodes done => used for changing hyper-parameters

    private State prevState;   // the previous state of Q-value: S
    private Action prevAction; // the action that was taken to go from prevState to newState: A
    private Vector theta; // value function weights vector for features
    private static final int FeatureLength = 10;

    public final State getPrevState(){ return prevState;}
    public final void setPrevState(State s){ prevState = s;}
    public final Action getPrevAction(){ return prevAction;}
    public final void setPrevAction(Action action){ prevAction = action;}

    public Sarsa(){
        resetRate();
        theta = new Vector(FeatureLength);
    }

    private final void resetRate() {
        // reset learning for new match
        alpha = 0.3;
        gamma = 0.99;
        epsilon = epsilonZero = 0.3;
        episodeCounter = 0;
    }

    public final void startEpisode(State initialState, int episodesLeft) {
        episodeCounter++;
        this.handsToGo = episodesLeft;
        prevState = initialState;
        prevAction = RandomHelper.getRandomBetAction();
        epsilon *= 0.99993;// * episodesLeft/(10*(episodeCounter+episodesLeft));
        alpha *= 0.99993; // 10^(-3/10^5), so that after 10^5 episodes alpha will be divided by 10^-3
    }

    public final Action nextAction(State newState, boolean isBet){

        // Q(S,A,T) = sum( Ti * Fi ), Fi = Qi(S,A) - feature i,
        // action = e-greedy argmax( Q(S,A,T), S=newState )

        if(RandomHelper.nextActionShouldBeRandom(epsilon)){
            return RandomHelper.getRandomAction(isBet);
        }

        Action bestAction = null;
        double max = Double.NEGATIVE_INFINITY;
        boolean isFirst = true;

        for(Action action: Action.allActions(isBet)) {

            double q = q(newState, action, theta);

            if(isFirst){
                isFirst = false;
                max = q;
                bestAction = action;
            }

            if (q > max) {
                max = q;
                bestAction = action;
            }
        }

        return bestAction;
    }

    public final void update(State newState, double reward, Action newAction){

        double scalar = alpha * (reward + gamma * q(newState,newAction,theta) - q(prevState,prevAction,theta));
        Vector gradient = getFeatures(prevState, prevAction);
        gradient.multiply(scalar);
        theta.add(gradient);
    }

    public final void updateTerminal(double reward) {

        double scalar = alpha * (reward - q(prevState,prevAction,theta));
        Vector gradient = getFeatures(prevState, prevAction);
        gradient.multiply(scalar);
        theta.add(gradient);
    }

    private double q(State state, Action action, Vector theta) {
        Vector v = getFeatures(state, action);
        double s = v.innerProduct(theta);
        return s;
    }

    private Vector getFeatures(State state, Action action) {
        double[] feature = new double[FeatureLength];

        feature[0] = 0.1;
        feature[1] = (state.position - 0.5) / 10.0;
        feature[3] = state.drawsRemaining / 10.0;
        feature[4] = state.raises / 10.0;
        feature[5] = state.opponentDrew / 20.0;
        feature[6] = state.agentDrew / 20.0;
        feature[7] = state.pot / 1e8;
        feature[8] = state.handActiveLength / 10.0;
        feature[9] = state.handActiveFirstRank / 20.0;

        return new Vector(feature);
    }
}

public class PLBadugi500877176 implements PLBadugiPlayer {

    private static int instanceCounter = 0;
    private String name = "Obi-Wan-";

    private Sarsa qtable;
    private State newState;

    public PLBadugi500877176() {
        instanceCounter++;
        name += instanceCounter;

        qtable = new Sarsa();
    }

    @Override
    public void startNewMatch(int handsToGo) { }

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

        newState = null;
        State s = getInitialState(position);
        qtable.startEpisode(s, handsToGo);
    }

    private final State getInitialState(int position){
        State s = new State();

        s.position = position;
        s.opponentDrew = 0;
        s.agentDrew = 0;
        s.raises = 0;
        s.drawsRemaining = 3;
        s.pot = 0;
        s.handActiveLength = 0;
        s.handActiveFirstRank = 13;

        return s;

        // new PLBadugiHand(Arrays.asList(new Card(0,12))));
        // int myScore = position==0 ?  currentScore : -currentScore;
    }

    private final void prepareUpdate(State newState){
        this.newState = newState;
    }

    private final void applyUpdate(boolean isBet){
        // when episode (hand) starts there is no newState
        if(newState == null) return;

        Action nextAction = qtable.nextAction(newState, isBet);

        qtable.update(newState,0.0, nextAction);

        qtable.setPrevState(newState);
        qtable.setPrevAction(nextAction);
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

        applyUpdate(true);

        Action prevAction = qtable.getPrevAction();
        int chips = convertToChips(prevAction, toCall,  minRaise,  maxRaise);;

        int[] active = hand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        final State prevState = qtable.getPrevState();

        State newState = new State();
        newState.position = prevState.position;
        newState.opponentDrew = prevState.opponentDrew + (opponentDrew == -1 ? 0: opponentDrew);
        newState.agentDrew = prevState.agentDrew;
        newState.raises = raises;
        newState.drawsRemaining = drawsRemaining;
        newState.pot = pot;
        newState.handActiveLength = handActiveLength;
        newState.handActiveFirstRank = handActiveFirstRank;

        prepareUpdate(newState);

        return chips;
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

        applyUpdate(false);

        Action prevAction = qtable.getPrevAction();
        List<Card> replaceCards = convertToDraw(prevAction, hand);

        int[] active = hand.getActiveRanks();
        int handActiveLength = active.length; // [1,2,3,4]
        int handActiveFirstRank = active[0]; // from 1 (ace) to 13 (king)

        final State prevState = qtable.getPrevState();

        State newState = new State();
        newState.position = prevState.position;
        newState.opponentDrew = prevState.opponentDrew + (dealerDrew == -1 ? 0: dealerDrew);
        newState.agentDrew = prevState.agentDrew + replaceCards.size();
        newState.raises = prevState.raises;
        newState.drawsRemaining = drawsRemaining;
        newState.pot = pot;
        newState.handActiveLength = handActiveLength;
        newState.handActiveFirstRank = handActiveFirstRank;

        prepareUpdate(newState);

        return replaceCards;
    }

    private final List<Card> convertToDraw(Action action, PLBadugiHand hand){

        List<Card> inactiveCards = hand.getInactiveCards();
        int countInactive = inactiveCards.size();
        int cardsToDraw = action.toInt() - Action.DrawZero.toInt();

        if(cardsToDraw < 0 || cardsToDraw > 4) {
            throw new IllegalArgumentException("No support for draw action: "+action);
        }

        int count = Math.min(cardsToDraw, countInactive); // limit the number of cards to draw by count of inactive
        return inactiveCards.subList(0, count);
    }

    @Override
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {

        qtable.updateTerminal(result);
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
