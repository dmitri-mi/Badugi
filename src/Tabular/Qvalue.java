package Tabular;

public class Qvalue {
    private double value;
    private int counter; // counter - number of times we got into this (state,action)
    public Qvalue(double value, int counter){this.value=value; this.counter=counter;}
    public final void setValue(double v){value = v;}
    public final double getValue(){return value;}
    public final int getCounter(){return counter;}
    public final void incrementCounter(){counter++;}
}
