package com.ogadai.alee.microbitblue;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitMagnetometer extends MicroBitXYZBase {

    private static final UUID MAGNETOMETER_SERVICE_UUID = UUID.fromString("e95df2d8-251d-470a-a062-fa1922dfa9a8");
    private static final UUID MAGNETOMETER_CHARACTERISTIC_UUID = UUID.fromString("e95dfb11-251d-470a-a062-fa1922dfa9a8");

    public MicroBitMagnetometer(MicroBitXYZBase.IHandler handler) {
        super(MAGNETOMETER_SERVICE_UUID, MAGNETOMETER_CHARACTERISTIC_UUID, handler);
    }
}
