package dk.northtech.dassco_specify_adapter.domain;

public class DasscoIllegalActionException extends RuntimeException {
    private String body;

    public DasscoIllegalActionException() {
    }

    public DasscoIllegalActionException(String message) {
        super(message);
    }

    public DasscoIllegalActionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DasscoIllegalActionException(String message, String body) {
        super(message);
        this.body = body;
    }

    public String body() {
        return body;
    }
}
