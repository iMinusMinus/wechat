package weixin.mp.infrastructure.exceptions;

public class RetryableException extends RuntimeException {

    public RetryableException() {
        super();
    }

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public RetryableException(Throwable throwable) {
        super(throwable);
    }
}
