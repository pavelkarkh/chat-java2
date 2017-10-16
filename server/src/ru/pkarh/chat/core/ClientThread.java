package ru.pkarh.chat.core;

import ru.pkarh.chat.library.Messages;
import ru.pkarh.chat.network.SocketThread;
import ru.pkarh.chat.network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {

    ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    private String nickname;
    private boolean authorized;
    private boolean reconnected;
    private int timeLive;

    public boolean isReconnected() {
        return reconnected;
    }

    String getNickname() {
        return nickname;
    }

    boolean isAuthorized() {
        return authorized;
    }

    void authorizeAccept(String nickname) {
        authorized = true;
        this.nickname = nickname;
        sendMessage(Messages.getAuthAccept(nickname));
    }

    void authorizeError() {
        sendMessage(Messages.getAuthDenied());
    }

    void timeout() {
        sendMessage(Messages.getTimeout());
        close();
    }

    void msgFormatError(String value) {
        sendMessage(Messages.getMsgFormatError(value));
        close();
    }

    void setReconnected(){
        reconnected = true;
        close();
    }

    int getTimeLive() {
        return timeLive;
    }

    void increaseTimeLive(int timeLive) {
        this.timeLive += timeLive;
    }

}
