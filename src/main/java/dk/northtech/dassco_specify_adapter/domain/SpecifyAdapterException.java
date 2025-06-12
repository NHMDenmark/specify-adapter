package dk.northtech.dassco_specify_adapter.domain;

public class SpecifyAdapterException extends RuntimeException  {
    private AcknowledgeStatus status;

//    public DasscoIllegalActionException() {
//    }
//
//    public DasscoIllegalActionException(String message) {
//        super(message);
//    }
//
//    public DasscoIllegalActionException(String message, Throwable cause) {
//        super(message, cause);
//    }

    public SpecifyAdapterException(String message, AcknowledgeStatus acknowledgeStatus) {
        super(message);
        this.status = acknowledgeStatus;
    }

    public AcknowledgeStatus status() {
        return status;
    }
    }
