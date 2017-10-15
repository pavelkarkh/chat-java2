package ru.pkarh.chat.library;

public class Messages {
    /*
/auth_request§login§password
/auth_accept§nickname
/auth_denied

/msg_format_error

/type_broadcast
*/

    public static final String DELIMITER =          "§";
    public static final String AUTH_REQUEST =       "/auth_request";
    public static final String AUTH_ACCEPT =        "/auth_accept";
    public static final String AUTH_DENIED =        "/auth_denied";
    public static final String MSG_FORMAT_ERROR =   "/msg_format_error";
    public static final String TYPE_BROADCAST =     "/bcast";
    public static final String USER_LIST =          "/userlist";
    public static final String TYPE_RANGECATS =     "/rangecast";

    public static String getAuthRequest(String login, String password) {
        return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
    }

    public static String getTypeRangecast(String value) {
        return TYPE_RANGECATS + DELIMITER + value;
    }

    public static String getAuthAccept(String nickname) {
        return AUTH_ACCEPT + DELIMITER + nickname;
    }

    public static String getAuthDenied() {
        return AUTH_DENIED;
    }

    public static String getMsgFormatError(String message) {
        return MSG_FORMAT_ERROR + DELIMITER + message;
    }

    public static String getTypeBroadcast(String src, String message) {
        return TYPE_BROADCAST + DELIMITER + System.currentTimeMillis() +
                DELIMITER + src + DELIMITER + message;
    }

    public static String getUserList(String users) {
        return USER_LIST + DELIMITER + users;
    }

}
