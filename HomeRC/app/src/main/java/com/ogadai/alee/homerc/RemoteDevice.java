package com.ogadai.alee.homerc;

import org.glassfish.tyrus.client.auth.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

public interface RemoteDevice {
    void Connect(String address) throws AuthenticationException, IOException, DeploymentException, URISyntaxException;
    void Disconnect();
    void addMessageHandler(MessageHandler msgHandler);
    void sendMessage(DeviceMessage message);
}
