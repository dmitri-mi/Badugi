package Tabular;

import java.util.HashMap;

public class Qtable{

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
