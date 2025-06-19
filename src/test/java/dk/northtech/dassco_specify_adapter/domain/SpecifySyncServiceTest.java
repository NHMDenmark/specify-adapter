package dk.northtech.dassco_specify_adapter.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.UrlEscapers;
import dk.northtech.dassco_specify_adapter.assets.SpecifyProperties;
import dk.northtech.dassco_specify_adapter.services.MappingServiceTest;
import dk.northtech.dassco_specify_adapter.services.SpecifyEndpointService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    @Test
    public void test() {
        String s = "http://test tst.dk/asdf asdf/asdf?lor t=lortiande n";
        String escape = UrlEscapers.urlFragmentEscaper().escape(s);
        System.out.println(escape);
    }
}