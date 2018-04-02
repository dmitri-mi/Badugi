package Tabular;

/*
 list that has Q-value for each action of the given state.
 action a: 0,1,2,3,4 : 0=fold, 1=toCall, 2=minRaise, 3=(maxRaise+minRaise)/2, 4=maxRaise
 */
public class Qaction {
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
