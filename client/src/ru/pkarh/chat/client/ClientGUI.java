package ru.pkarh.chat.client;

import ru.pkarh.chat.library.Messages;
import ru.pkarh.chat.network.SocketThread;
import ru.pkarh.chat.network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ClientGUI extends JFrame implements Thread.UncaughtExceptionHandler,
        ActionListener, SocketThreadListener {

    public static final String CHAT_CLIENT = "Chat Client";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientGUI();
            }
        });
    }

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;

    private final JTextArea log = new JTextArea();

    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    // private final JTextField tfIPAddress = new JTextField("95.84.209.91");
    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField tfLogin = new JTextField("Pavel");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");
    private final DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss:");

    private final JList<String> userList = new JList<>();

    private SocketThread socketThread;

    ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setTitle(CHAT_CLIENT);

        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        tfIPAddress.addActionListener(this);
        tfLogin.addActionListener(this);
        tfPassword.addActionListener(this);
        tfPort.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(btnLogin);
        add(panelTop, BorderLayout.NORTH);

        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);
        add(panelBottom, BorderLayout.SOUTH);
        panelBottom.setVisible(false);

        JScrollPane scrollUsers = new JScrollPane(userList);
        scrollUsers.setPreferredSize(new Dimension(100, 0));
        add(scrollUsers, BorderLayout.EAST);

        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        add(scrollLog, BorderLayout.CENTER);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == tfIPAddress || src == tfLogin ||
                src == tfPassword || src == tfPort || src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    void sendMessage() {
        String msg = tfMessage.getText();
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.requestFocusInWindow();
        socketThread.sendMessage(Messages.getTypeRangecast(msg));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        String message;
        if (stackTraceElements.length == 0) {
            message = "Empty Stacktrace";
        } else {
            message = e.getClass().getCanonicalName() +
                    ": " + e.getMessage() + "\n" +
                    "\t at " + stackTraceElements[0];
        }

        JOptionPane.showMessageDialog(this, message, "Exception", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private void connect() {
        Socket socket = null;
        try {
            socket = new Socket(tfIPAddress.getText(),
                    Integer.parseInt(tfPort.getText()));
        } catch (IOException e) {
            log.append("Exception: " + e.getMessage());
        }
        socketThread = new SocketThread(this, "SocketTHread", socket);
    }

    private void putLog(String message) {
        log.append(message + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override
    public void onStartSocketThread(SocketThread thread, Socket socket) {
        putLog("Поток сокета стартовал");
    }

    @Override
    public void onStopSocketThread(SocketThread thread) {
        putLog("Соединение разорвано");
        panelBottom.setVisible(false);
        panelTop.setVisible(true);
    }

    @Override
    public void onSocketIsReady(SocketThread thread, Socket socket) {
        putLog("Соединение установлено");
        String login = tfLogin.getText();
        String password = new String(tfPassword.getPassword());
        thread.sendMessage(Messages.getAuthRequest(login, password));
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addMessage(value);
            }
        });
    }

    private void addMessage(String message) {
        String[] msg = message.split(Messages.DELIMITER);
        if(msg[0] != null){
            switch (msg[0]){
                case Messages.TYPE_BROADCAST:
                    putLog(dateFormat.format(Long.parseLong(msg[1])) + " " + msg[2] + ": " + msg[3]);
                    break;
                case Messages.AUTH_ACCEPT:
                    putLog(msg[1] + " connected");
                    setTitle(CHAT_CLIENT + " nickname: " + msg[1]);
                    break;
                case Messages.AUTH_DENIED:
                    putLog(message);
                    break;
                case Messages.MSG_FORMAT_ERROR:
                    putLog(message);
                    socketThread.close();
                    break;
                case Messages.USER_LIST:
                    String users = message.substring(Messages.USER_LIST.length() + Messages.DELIMITER.length());
                    String[] userArray = users.split(Messages.DELIMITER);
                    Arrays.sort(userArray);
                    userList.setListData(userArray);
                    break;
                default:
                    throw new RuntimeException("Unknown protocol: " + msg[0]);
            }
        }
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {

    }
}
