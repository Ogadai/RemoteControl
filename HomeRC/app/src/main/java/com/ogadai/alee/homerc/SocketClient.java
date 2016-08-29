package com.ogadai.alee.homerc;

import org.glassfish.tyrus.client.auth.AuthenticationException;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Created by alee on 15/04/2016.
 */
@ClientEndpoint(subprotocols = {"echo-protocol"})
public class SocketClient {
    private WebSocketContainer mContainer;
    private Session mUserSession = null;
    private MessageHandler mMessageHandler = null;

    public SocketClient() {
        mContainer = ContainerProvider.getWebSocketContainer();
    }

    public void Connect(URI endpointURI) throws AuthenticationException, IOException, DeploymentException {
        try {
            mContainer.connectToServer(this, endpointURI);
        } catch(DeploymentException depEx) {
            Throwable cause = depEx.getCause();
            if (cause instanceof AuthenticationException) {
                throw (AuthenticationException)cause;
            }
            throw depEx;
        }
    }

    public void Disconnect() {
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
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket :" + reason.getCloseCode().getCode() + " - " + reason.getReasonPhrase());
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
        System.out.println("websocket message:" + message);
        if (mMessageHandler != null) {
            mMessageHandler.handleMessage(message);
        }
    }

    @OnMessage
    public void onMessage(byte[] message) {
        System.out.println("websocket message: " + message.length + " bytes");
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

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        mUserSession.getAsyncRemote().sendText(message);
    }
    public void sendMessage(byte[] message) {
        mUserSession.getAsyncRemote().sendBinary(ByteBuffer.wrap(message));
    }

    public static interface MessageHandler {
        public void handleMessage(String message);
        public void handleMessage(byte[] message);
    }

}

