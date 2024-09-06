package dk.northtech.dassco_specify_adapter.domain;

public record HttpInfo(
        String path,
        String hostname,
        int total_storage_mb,
        int cache_storage_mb,
        int remaining_storage_mb,
        int allocated_storage_mb,
        String allocation_status_text,
        HttpAllocationStatus http_allocation_status) {
    public HttpInfo {
    }

    public HttpInfo(String proxy_allocation_status_text, HttpAllocationStatus httpAllocationStatus) {
        this(null, null, 0, 0, 0, 0, proxy_allocation_status_text, httpAllocationStatus);
    }
}
