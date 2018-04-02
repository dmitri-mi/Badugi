package Tabular;

public enum Action{
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

        throw new IllegalArgumentException("Enum Tabular.Action cannot be created from integer: "+index);
    }

    public final boolean isBet() {
        return index <= Qaction.MaxRaiseAction;
    }
}
