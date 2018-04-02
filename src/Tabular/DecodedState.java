package Tabular;

public class DecodedState{
    public int position;// 0 if the agent is the dealer or 1 - otherwise
    public int drawsRemaining; // 3,2,1,0
    public int raises;         // 0,1,2,3,4
    public int handActiveLength; // 1,2,3,4
    public int handActiveFirstRank; // 1,2,..,13
    public int opponentDrew;       // -1,0,1,2,3,4
    public int agentDrew;    // -1,0,1,2,3,4
}
