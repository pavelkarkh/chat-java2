package ru.pkarh.chat.core;

import ru.pkarh.chat.library.Messages;
import ru.pkarh.chat.network.ServerSocketThread;
import ru.pkarh.chat.network.ServerSocketThreadListener;
import ru.pkarh.chat.network.SocketThread;
import ru.pkarh.chat.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private ServerSocketThread serverSocketThread;
    private final ChatServerListener listener;
    private final DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss: ");

    private Vector<SocketThread> clients = new Vector<>();

    public ChatServer (ChatServerListener listener) {
        this.listener = listener;
    }

    public void start(int port) {
        if (serverSocketThread != null && serverSocketThread.isAlive())
            putLog("Server is already running");
        else
            serverSocketThread = new ServerSocketThread(this, "Server thread", port, 2000);
        SqlClient.connect();
    }

    public void stop() {
        if (serverSocketThread == null || !serverSocketThread.isAlive())
            putLog("Server is not running");
        else
            serverSocketThread.interrupt();
        SqlClient.disconnect();
    }

    private void putLog(String msg) {
        msg = dateFormat.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerLog(this, msg);
    }

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread clientThread = (ClientThread) clients.get(i);
            if(!clientThread.isAuthorized()) continue;
            sb.append(clientThread.getNickname()).append(Messages.DELIMITER);

        }
        return sb.toString();
    }

    /**
     * События Server socket thread'a
     * */

    @Override
    public void onStartServerSocketThread(ServerSocketThread thread) {
        putLog("Сервер запущен");
    }

    @Override
    public void onStopServerSocketThread(ServerSocketThread thread) {
        putLog("сервер остановлен");
    }

    @Override
    public void onCreateServerSocket(ServerSocketThread thread, ServerSocket serverSocket) {
        putLog("создан Server socket");
    }

    @Override
    public void onAcceptTimeout(ServerSocketThread thread, ServerSocket serverSocket) {

    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, Socket socket) {
        putLog("Клиент подключился: " + socket);
        String threadName = "SocketThread " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, threadName, socket);
    }

    @Override
    public void onServerSocketException(ServerSocketThread thread, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
    }

    /**
     * Методы Socket thread'a
     */

    @Override
    public synchronized void onStartSocketThread(SocketThread thread, Socket socket) {
        putLog("started");
    }

    @Override
    public synchronized void onStopSocketThread(SocketThread thread) {
        putLog("stopped");
        ClientThread clientThread = (ClientThread) thread;
        clients.remove(thread);
        if(clientThread.isAuthorized()) {
            sendToAuthorizedClients(Messages.getTypeBroadcast("Server", clientThread.getNickname() + " has left"));
            sendToAuthorizedClients(Messages.getUserList(getUsers()));
        }
    }

    @Override
    public synchronized void onSocketIsReady(SocketThread thread, Socket socket) {
        putLog("is ready");
        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String value) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthMessages(client, value);
        } else {
            handleNonAuthMessages(client, value);
        }
    }

    @Override
    public synchronized void onSocketThreadException(SocketThread thread, Exception e) {
        e.printStackTrace();
    }

    private synchronized ClientThread findUser(String value){
        for (int i = 0; i < clients.size(); i++) {
            ClientThread clientThread = (ClientThread) clients.get(i);
            if(clientThread.isAuthorized()) continue;
            if(clientThread.getNickname().equals(value)) {
                return clientThread;
            }
        }
        return null;
    }

    void handleAuthMessages(ClientThread client, String value) {
        String[] msg = value.split(Messages.DELIMITER);
        switch (msg[0]) {
            case Messages.TYPE_RANGECATS:
                sendToAuthorizedClients(Messages.getTypeBroadcast(client.getNickname(), msg[1]));
                break;
            default:
                client.msgFormatError(value);
        }

    }

    void handleNonAuthMessages(ClientThread client, String value) {
        String[] arr = value.split(Messages.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Messages.AUTH_REQUEST)) {
            client.msgFormatError(value);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SqlClient.getNick(login, password);
        if (nickname == null) {
            putLog("Invalid login/password: login '" +
                    login + "' password: '" + password + "'");
            client.authorizeError();
            return;
        }
        ClientThread clientThread = findUser(nickname);
        if(clientThread != null){
            client.authorizeAccept(nickname);
            sendToAuthorizedClients(Messages.getTypeBroadcast("Server", nickname + " connected"));
        } else {
            clientThread.setReconnected();
            clients.remove(clientThread);
        }

        sendToAuthorizedClients(Messages.getUserList(getUsers()));
    }

    private void sendToAuthorizedClients(String value) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if(!client.isAuthorized()) continue;
            client.sendMessage(value);
        }
    }
}

