import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimplePlayer implements PLBadugiPlayer  {

    double pCall = 0.1;
    double pRaise = 0.2;

    private Random rng = new Random();

    @Override
    public void startNewHand(int position, int handsToGo, int currentScore) {

    }

    @Override
    public int bettingAction(int drawsRemaining, PLBadugiHand hand, int pot, int raises, int toCall, int minRaise, int maxRaise, int opponentDrew) {

        if(drawsRemaining == 0) return toCall;

        double d = rng.nextDouble();
        if(d<pCall) return toCall; // call
        if(d<=pCall+pRaise) {
            int amount = (int)(maxRaise - (maxRaise - minRaise) * (rng.nextDouble() * 0.7 + 0.3));
            return amount; // raise
        }
        return 0; // fold
    }

    @Override
    public List<Card> drawingAction(int drawsRemaining, PLBadugiHand hand, int pot, int dealerDrew) {
        List<Card> allCards = hand.getAllCards();
        List<Card> inactiveCards = hand.getInactiveCards();
        List<Card> pitch = new ArrayList<Card>();
        // Don't break a made badugi when the opponent is drawing.
        if(inactiveCards.size() == 0 && drawsRemaining < 2 && dealerDrew > 0) { return pitch; }

        // Pitch the inactive cards and also the active cards that are too high in rank.
        for(Card c: allCards) {
            if(c.getRank() > 12 + dealerDrew - drawsRemaining || inactiveCards.contains(c)) {
                pitch.add(c);
            }
        }

        return pitch;
    }

    @Override
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {

    }

    @Override
    public String getAgentName() {
        return "Probability-Call";
    }

    @Override
    public String getAuthor() {
        return "Probability-Call";
    }
}
