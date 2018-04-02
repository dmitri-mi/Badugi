package Tabular;

import java.security.SecureRandom;
import java.util.Random;

public class RandomHelper{

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
