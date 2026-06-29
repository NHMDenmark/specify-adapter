package dk.northtech.dassco_specify_adapter.domain.specify;

public record SpecifyCollectionLogin(
        String sessionid,
        String csrftoken,
        String collection,
        String rootUrl
) {
}
