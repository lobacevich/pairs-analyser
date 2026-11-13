package by.lobacevich.analyzer.exception;

import lombok.Getter;

@Getter
public class BinanceRetryableException extends RuntimeException {

    private final int retryAfterSeconds;

    public BinanceRetryableException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
