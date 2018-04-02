package Tabular;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class StateTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void compareTo() {
    }

    @Test
    void s1(){
        Tabular.State s1 = Tabular.State.Encode(
0,      //        int position, // 0 if the agent is the dealer, 1 - otherwise
3,//        int drawsRemaining,  // 3,2,1,0
0,//        int raises,          // 0,1,2,3,4
1,//        int handActiveLength,// 1,2,3,4
13,//        int handActiveFirstRank, // 1,2,..,13
4,//        int opponentDrew,        // -1 , 0,1,2,3,4
4//        int agentDrew     // -1,0,1,2,3,4
        );

        Tabular.DecodedState d = s1.Decode(s1);

        assertEquals(0, d.position);
        assertEquals(3, d.drawsRemaining);
        assertEquals(0, d.raises);
        assertEquals(1-1, d.handActiveLength);
        assertEquals(13-1, d.handActiveFirstRank);
        assertEquals(4, d.opponentDrew);
        assertEquals(4, d.agentDrew);
    }

    @Test
    void s2(){
        Tabular.State s1 = Tabular.State.Encode(
                1,      //        int position, // 0 if the agent is the dealer, 1 - otherwise
                3,//        int drawsRemaining,  // 3,2,1,0
                4,//        int raises,          // 0,1,2,3,4
                4,//        int handActiveLength,// 1,2,3,4
                13,//        int handActiveFirstRank, // 1,2,..,13
                4,//        int opponentDrew,        // -1 , 0,1,2,3,4
                4//        int agentDrew     // -1,0,1,2,3,4
        );

        Tabular.DecodedState d = s1.Decode(s1);

        assertEquals(1, d.position);
        assertEquals(3, d.drawsRemaining);
        assertEquals(4, d.raises);
        assertEquals(4-1, d.handActiveLength);
        assertEquals(13-1, d.handActiveFirstRank);
        assertEquals(4, d.opponentDrew);
        assertEquals(4, d.agentDrew);
    }
}