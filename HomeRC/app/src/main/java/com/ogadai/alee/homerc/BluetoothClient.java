package com.ogadai.alee.homerc;

import org.glassfish.tyrus.client.auth.AuthenticationException;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

/**
 * Created by alee on 24/05/2017.
 */

public class BluetoothClient implements RemoteDevice {
    @Override
    public void Connect(String address) throws AuthenticationException, IOException, DeploymentException, URISyntaxException {
        System.out.println("Connected : " + address);
    }

    @Override
    public void Disconnect() {
        System.out.println("Disonnected bluetooth device");
    }

    @Override
    public void addMessageHandler(MessageHandler msgHandler) {

    }

    @Override
    public void sendMessage(DeviceMessage message)
    {
        System.out.println(message.getName() + ": " + message.getState());
    }
}
