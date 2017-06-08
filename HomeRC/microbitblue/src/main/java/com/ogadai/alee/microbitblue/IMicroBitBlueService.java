package com.ogadai.alee.microbitblue;

/**
 * Created by alee on 08/06/2017.
 */

public interface IMicroBitBlueService {
    void initialise(MicroBitBlueController controller);
    void connect(Runnable finished);
}
