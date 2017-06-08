package com.ogadai.alee.homerc;

import android.content.Context;

import org.glassfish.tyrus.client.auth.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

public interface RemoteDevice {
    void connect(Context context, String address) throws AuthenticationException, IOException, DeploymentException, URISyntaxException;
    void disconnect();
    void addMessageHandler(MessageHandler msgHandler);
    void sendMessage(DeviceMessage message);
}
