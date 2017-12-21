package ru.track.prefork.nioserver;

public class ProtocolException extends Exception {

    ProtocolException(String message) {
        super(message);
    }

    ProtocolException(String message, Exception cause) {
        super(message, cause);
    }
}
