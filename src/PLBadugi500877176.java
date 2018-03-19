import java.util.ArrayList;
import java.util.List;

public class PLBadugi500877176 implements PLBadugiPlayer {

    private static int instanceCounter = 0;
    private String name = "Obi-Wan-";
    private int instaceIndex;


    public PLBadugi500877176() {
        instanceCounter++;
        name += instanceCounter;
        instaceIndex = instanceCounter;
    }

    @Override
    public void startNewHand(int position, int handsToGo, int currentScore) {

    }

    @Override
    public int bettingAction(int drawsRemaining, PLBadugiHand hand, int pot, int raises, int toCall, int minRaise, int maxRaise, int opponentDrew) {
        // if(instaceIndex == 1) return minRaise;
        // else return toCall;
        return minRaise;
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
