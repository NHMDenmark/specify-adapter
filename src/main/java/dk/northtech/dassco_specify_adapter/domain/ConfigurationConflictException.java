package dk.northtech.dassco_specify_adapter.domain;

public class ConfigurationConflictException extends RuntimeException {
    public ConfigurationConflictException(String message) {
        super(message);
    }
}
