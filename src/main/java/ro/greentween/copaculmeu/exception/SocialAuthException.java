package ro.greentween.copaculmeu.exception;

import org.springframework.http.HttpStatus;

public class SocialAuthException extends RuntimeException {

    private final HttpStatus status;

    public SocialAuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
