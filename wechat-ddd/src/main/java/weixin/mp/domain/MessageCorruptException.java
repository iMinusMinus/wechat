package weixin.mp.domain;

public class MessageCorruptException extends RuntimeException {

    public MessageCorruptException(String message) {
        super(message);
    }

    public MessageCorruptException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public MessageCorruptException(Throwable throwable) {
        super(throwable);
    }
}
