package lk.ijse.dep7.pos.pos.exception;

public class FailedOperationException extends Exception{
    public FailedOperationException() {
    }

    public FailedOperationException(String message) {
        super(message);
    }

    public FailedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedOperationException(Throwable cause) {
        super(cause);
    }
}
