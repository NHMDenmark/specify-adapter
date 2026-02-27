package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.assets.SpecifyMappingsProperties;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObject;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class MappingServiceTest {

    private static String default_conf = """
            #Always take the first entry in file_formats
            origfilename=${asset_guid}.${file_format}
            attachmentlocation=${asset_pid}
            #This is intentional
            copyrightdate=${date_asset_deleted}
            filecreateddate=${date_asset_taken}
            #Firste entry in file_formats, lowercase
            mimeType=${file_format}
            copyrightholder=${legality.copyright}
            credit=${legality.credit}
            license=${legality.license}
            ispublic=${make_public}
            remarks=${specify_attachment_remarks}
            title=${specify_attachment_title}
            """;
//    @Test
//    void mapAsset() {
//        Asset testAsset = getTestAsset();
//        MappingService mappingService = new MappingService(new SpecifyMappingsProperties("./mappings/"));
//        CollectionObjectAttachment collectionObjectAttachment = mappingService.getAttachment(testAsset);
//        Attachment attachment = collectionObjectAttachment.attachment;
//        assertThat(attachment).isNotNull();
//        assertThat(attachment.origfilename).isEqualTo("test-guid-1.jpeg");
//        assertThat(attachment.attachmentlocation).isEqualTo("test-guid-1_pid");
//        assertThat(attachment.copyrightdate).isEqualTo("2025-01-12");
//    }

    @Test
    void mapAssetNewLines() {
        Asset testAsset = getTestAsset();
        testAsset.legality = new Legality(null, "copyright", "loicense", "credz");
        testAsset.specify_attachment_remarks = "remarkable\n remark";
        testAsset.specify_attachment_title = "Titlecious";
        testAsset.make_public = true;
//        testAsset.date_asset_deleted = Instant.parse("2023-07-17T09:01:51.312Z");
        testAsset.date_asset_taken = Instant.parse("2023-07-24T09:01:51.312Z");
        MappingService mappingService = new MappingService(new SpecifyMappingsProperties("./mappings/"));
        CollectionObjectAttachment collectionObjectAttachment = mappingService.getAttachment(testAsset);
        Attachment attachment = collectionObjectAttachment.attachment;
        assertThat(attachment).isNotNull();
        assertThat(attachment.origfilename).isEqualTo("test-guid-1.jpeg");
        assertThat(attachment.copyrightdate).isNull();
        assertThat(attachment.filecreateddate).isEqualTo("2023-07-24");
        assertThat(attachment.mimetype).isEqualTo("jpeg");
        assertThat(attachment.copyrightholder).isEqualTo("copyright");
        assertThat(attachment.license).isEqualTo("loicense");
        assertThat(attachment.credit).isEqualTo("credz");
        assertThat(attachment.ispublic).isTrue();
        assertThat(attachment.remarks).isEqualTo("remarkable\n remark");
        assertThat(attachment.title).isEqualTo("Titlecious");
    }

    public static Asset getTestAsset() {
        Asset asset = new Asset();
        Specimen specimen = new Specimen("barcode", "specimen_pid", new HashSet<>(Arrays.asList("slide")));
        asset.asset_specimen = Arrays.asList(new AssetSpecimen(false, null, "slide", null, "specimen_pid", asset.asset_guid, null));
        asset.asset_specimen.get(0).specimen = specimen;
        asset.asset_locked = false;
        asset.status = "BEING_PROCESSED";
        asset.digitiser = "Karl-Børge";
        asset.asset_guid = "test-guid-1";
        asset.asset_pid = "test-guid-1" + "_pid";
        asset.funding = new ArrayList<>(List.of("Hundredetusindvis af dollars"));
        asset.date_asset_taken = Instant.now();
        asset.asset_subject = "Folder";
        asset.file_formats = Arrays.asList("JPEG");
        asset.payload_type = "nuclear";
        asset.updateUser = "Basviola";
        asset.institution = "NHMD";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.collection = "i2_c1";
        asset.camera_setting_control = "Mom get the camera!";
        asset.date_asset_finalised = Instant.now();
        asset.metadata_source = "I made it up";
        asset.metadata_version = "1.0.0";
        asset.date_metadata_ingested = Instant.now();
        asset.internal_status = InternalStatus.ASSET_RECEIVED;
        asset.make_public = true;
        asset.push_to_specify = true;
        return asset;
    }

    @Test
    void mapAsset() {
        Attachment attachment = new Attachment();
        MappingService mappingService = new MappingService(new SpecifyMappingsProperties("./mappings/"));
        CollectionObjectAttachment collectionObjectAttachment = new CollectionObjectAttachment();
        collectionObjectAttachment.attachment = attachment;
        CollectionObject collectionObject = new CollectionObject();
        collectionObject.collectionobjectattachments = List.of(collectionObjectAttachment);
        mappingService.mapAsset(collectionObject);


    }

    @Test
    void testtest() {
        List<String> valueToken = new ArrayList<>();
        List<String> constantToken = new ArrayList<>();
        String example = "asdasdff${asset_guid}adsf.${file_format}${asdf}";
        int index = 0;

        int attempts = 0;
        while (!example.isEmpty()) {
            int tokenStart = example.indexOf("${");
            int tokenEnd = example.indexOf("}");
            if (tokenStart == -1) {
                if (!example.isEmpty()) {
                    constantToken.add(example);
                }
                break;
            }
            if(tokenStart != 0) {
                constantToken.add(example.substring(0, tokenStart));
            }
            valueToken.add(example.substring(tokenStart, tokenEnd + 1));
            example = example.substring(tokenEnd + 1);

            System.out.println(example);
            System.out.println(index);
            System.out.println("tokenStart: " + tokenStart);
            System.out.println("tokenend" + tokenEnd);
            index = tokenEnd;
            attempts++;
        }
        System.out.println(valueToken);
        System.out.println(constantToken);

    }

    Attachment getTestAttachment() {
        Attachment attachment = new Attachment();
        attachment.attachmentlocation = "attachmentlocation";
        attachment.origfilename = "origfilename";
        attachment.copyrightdate = "copyrightdate";
        attachment.filecreateddate = "filecreateddate";
        attachment.mimetype = "mimetype";
        attachment.copyrightholder = "copyrightholder";
        attachment.credit = "credit";
        attachment.license = "license";
        attachment.ispublic = true;
        attachment.remarks = "remarks";
        attachment.title = "title";
        return attachment;
    }

    @Test
    void testtest2() {
        DateTimeFormatter specifyDateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.of("Europe/Copenhagen"));
        TemporalAccessor parse = specifyDateFormat.parse("2026-02-03T05:09:28");

        System.out.println(Instant.from(parse));
    }
    ;
//    public static Instant format(String isoZ) {
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ssZ");
//        TemporalAccessor parse = dateTimeFormatter.parse("2017-08-27T17:43:11Z");
//        return null;
//    }
}