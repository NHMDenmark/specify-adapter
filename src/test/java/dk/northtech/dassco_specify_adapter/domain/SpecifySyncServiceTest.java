package dk.northtech.dassco_specify_adapter.domain;

import com.google.common.net.UrlEscapers;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class SpecifySyncServiceTest {
//    @Test
//    void getSyncService() {
//        SpecifyEndpointService specifyEndpointService = new SpecifyEndpointService(new SpecifyProperties("", "", "", ""), null, null);
//        SpecifySyncService specifySyncService = new SpecifySyncService(null, specifyEndpointService);
//        ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
//        Asset testAsset = MappingServiceTest.getTestAsset();
//        ARSUpdate arsUpdate = new ARSUpdate(testAsset);
//        try {
//            String assetJson = ow.writeValueAsString(arsUpdate);
//            specifySyncService.sync(assetJson);
//        } catch (JsonProcessingException e) {
//            fail(e);
//        }
//    }
//    ZoneOffset.
    @Test
    public void test() {
        DateTimeFormatter format = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(
                ZoneId.of("Europe/Copenhagen")
        );
        System.out.println(format.format(Instant.now()));
    }
}