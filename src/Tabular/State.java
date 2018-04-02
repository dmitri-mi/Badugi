package Tabular;

public class State implements Comparable<State>{

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
