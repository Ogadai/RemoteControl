package com.ogadai.alee.homerc;

import android.content.Context;

import com.google.gson.Gson;

import org.glassfish.tyrus.client.auth.AuthenticationException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

@ClientEndpoint(subprotocols = {"echo-protocol"})
public class SocketClient implements RemoteDevice {
    private WebSocketContainer mContainer;
    private Session mUserSession = null;
    private MessageHandler mMessageHandler = null;

    public SocketClient() {
        mContainer = ContainerProvider.getWebSocketContainer();
    }

    @Override
    public void connect(Context context, String address) throws AuthenticationException, IOException, DeploymentException, URISyntaxException {
        try {
            URI endpointURI = new URI(address);
            mContainer.connectToServer(this, endpointURI);
        } catch(DeploymentException depEx) {
            Throwable cause = depEx.getCause();
            if (cause instanceof AuthenticationException) {
                throw (AuthenticationException)cause;
            }
            throw depEx;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (mUserSession != null) {
                Session session = mUserSession;
                mUserSession = null;
                session.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession, EndpointConfig config) {
        System.out.println("opening websocket");
        mUserSession = userSession;
        if (mMessageHandler != null) {
            mMessageHandler.connected();
        }
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        String message = reason.getCloseCode().getCode() + " - " + reason.getReasonPhrase();
        System.out.println("closing websocket :" + message);
        if (mMessageHandler != null) {
            mMessageHandler.disconnected(message);
        }
        if (mUserSession != null) {
            mUserSession = null;
        }
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        if (mMessageHandler != null) {
            Gson gson = new Gson();
            DeviceMessage deviceMessage = gson.fromJson(message, DeviceMessage.class);

            mMessageHandler.handleMessage(deviceMessage);
        }
    }

    @OnMessage
    public void onMessage(byte[] message) {
        if (mMessageHandler != null) {
            mMessageHandler.handleMessage(message);
        }
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        mMessageHandler = msgHandler;
    }

    @Override
    public void sendMessage(DeviceMessage message) {
        Gson gson = new Gson();
        String messageStr = gson.toJson(message);

        mUserSession.getAsyncRemote().sendText(messageStr);
    }
}
