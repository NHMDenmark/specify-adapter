package dk.northtech.dassco_specify_adapter.webapi.exceptionmappers;

import jakarta.annotation.Nonnull;

public class DaSSCoError {
    public final String type = "Error";
    public final String protocolVersion;
    public final DaSSCoErrorCode errorCode;
    public final String errorMessage;
    public String body;

    public DaSSCoError(
            @Nonnull String protocolVersion,
            @Nonnull DaSSCoErrorCode errorCode,
            String errorMessage,
            String body
    ) {
        this.protocolVersion = protocolVersion;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.body = body;
    }

    public DaSSCoError(
            @Nonnull String protocolVersion,
            @Nonnull DaSSCoErrorCode errorCode,
            String errorMessage
    ) {
        this.protocolVersion = protocolVersion;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public DaSSCoError(
            @Nonnull String protocolVersion,
            @Nonnull DaSSCoErrorCode errorCode,
            Throwable t,
            String body
    ) {
        this(protocolVersion, errorCode, getCauseMessage(t), body);
    }

    public DaSSCoError(
            @Nonnull String protocolVersion,
            @Nonnull DaSSCoErrorCode errorCode,
            Throwable t
    ) {
        this(protocolVersion, errorCode, getCauseMessage(t));
    }

    public static String getCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    public static Class<?> getCauseType(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass();
    }
}
