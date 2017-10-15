package ru.pkarh.chat.core;

import ru.pkarh.chat.library.Messages;
import ru.pkarh.chat.network.SocketThread;
import ru.pkarh.chat.network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    private String nickname;
    private boolean isAuthorized;
    private boolean isReconnected;

    public boolean isReconnected() {
        return isReconnected;
    }

    String getNickname() {
        return nickname;
    }

    boolean isAuthorized() {
        return isAuthorized;
    }

    void authorizeAccept(String nickname) {
        isAuthorized = true;
        this.nickname = nickname;
        sendMessage(Messages.getAuthAccept(nickname));
    }

    void authorizeError() {
        sendMessage(Messages.getAuthDenied());
        close();
    }

    void msgFormatError(String value) {
        sendMessage(Messages.getMsgFormatError(value));
        close();
    }

    void setReconnected(){
        isReconnected = true;
        close();
    }

}
