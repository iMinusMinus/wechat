package weixin.mp.infrastructure.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ClientError extends ResponseStatusException {

    private final int code;

    private final String message;

    public ClientError(int code, String message, HttpStatus status) {
        super(status, message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
