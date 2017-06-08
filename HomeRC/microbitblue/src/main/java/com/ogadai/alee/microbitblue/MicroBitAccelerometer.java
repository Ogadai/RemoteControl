package com.ogadai.alee.microbitblue;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitAccelerometer extends MicroBitXYZBase {

    private static final UUID ACCELEROMETER_SERVICE_UUID = UUID.fromString("e95d0753-251d-470a-a062-fa1922dfa9a8");
    private static final UUID ACCELEROMETER_CHARACTERISTIC_UUID = UUID.fromString("e95dca4b-251d-470a-a062-fa1922dfa9a8");

    public MicroBitAccelerometer(MicroBitXYZBase.IHandler handler) {
        super(ACCELEROMETER_SERVICE_UUID, ACCELEROMETER_CHARACTERISTIC_UUID, handler);
    }
}
