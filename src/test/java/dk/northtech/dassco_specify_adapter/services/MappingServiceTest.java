package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.assets.SpecifyMappingsProperties;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.Agent;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObject;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyAttachmentContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @TempDir
    Path tempDir;

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


    @Test
    void mapAssetNewLines() throws IOException {
        Asset testAsset = getTestAsset();
        testAsset.legality = new Legality(null, "copyright", "loicense", "credz");
        testAsset.specify_attachment_remarks = "remarkable\n remark";
        testAsset.specify_attachment_title = "Titlecious";
        testAsset.make_public = true;
//        testAsset.date_asset_deleted = Instant.parse("2023-07-17T09:01:51.312Z");
        testAsset.date_asset_taken = Instant.parse("2023-07-24T09:01:51.312Z");
        Path mappingsPath = createSyncMappingDirectory();
        Files.writeString(
                mappingsPath.resolve("NHMD/default.conf"),
                "origfilename=${asset_guid}.${file_format}\n" +
                "attachmentlocation=${asset_pid}\n" +
                "copyrightdate=${date_asset_deleted_ars}\n" +
                "filecreateddate=${date_asset_taken}\n" +
                "mimetype=${file_format}\n" +
                "copyrightholder=${legality.copyright}\n" +
                "credit=${legality.credit}\n" +
                "license=${legality.license}\n" +
                "ispublic=${make_public}\n" +
                "remarks=${specify_attachment_remarks}\n" +
                "title=${specify_attachment_title}\n"
        );
        MappingService mappingService = new MappingService(new SpecifyMappingsProperties(withTrailingSlash(mappingsPath)));
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
    void mapAssetUsesInstitutionDefaults() throws IOException {
        Attachment attachment = new Attachment();
        attachment.attachmentlocation = "guid-from-attachment";
        attachment.title = "pipeline-from-attachment";
        attachment.remarks = "status-from-attachment";
        attachment.origfilename = "workstation-from-attachment";

        Path mappingsPath = createSyncMappingDirectory();
        writeCollectionMappingFile(mappingsPath);
        writeSyncDefaultsFile(mappingsPath, "pipeline=PIPE_DEFAULT\nstatus=STATUS_DEFAULT\nworkstation=WORK_DEFAULT\n");

        MappingService mappingService = new MappingService(new SpecifyMappingsProperties(withTrailingSlash(mappingsPath)));
        CollectionObjectAttachment collectionObjectAttachment = new CollectionObjectAttachment();
        collectionObjectAttachment.attachment = attachment;
        CollectionObject collectionObject = new CollectionObject();
        collectionObject.collectionobjectattachments = List.of(collectionObjectAttachment);
        collectionObject.catalognumber = "NHMD0001";
        collectionObject.timestampmodified = "2026-02-03T05:09:28";

        var mapped = mappingService.mapAsset(collectionObject);

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().asset.pipeline).isEqualTo("PIPE_DEFAULT");
        assertThat(mapped.getFirst().asset.status).isEqualTo("STATUS_DEFAULT");
        assertThat(mapped.getFirst().asset.workstation).isEqualTo("WORK_DEFAULT");
    }

    @Test
    void mapAssetUsesCollectionSpecificSyncDefaultsWhenPresent() throws IOException {
        Attachment attachment = new Attachment();
        attachment.attachmentlocation = "guid-from-attachment";
        attachment.title = "pipeline-from-attachment";
        attachment.remarks = "status-from-attachment";
        attachment.origfilename = "workstation-from-attachment";

        Path mappingsPath = createSyncMappingDirectory();
        writeCollectionMappingFile(mappingsPath);
        writeSyncDefaultsFile(mappingsPath, "pipeline=PIPE_DEFAULT\nstatus=STATUS_DEFAULT\nworkstation=WORK_DEFAULT\n");
        Files.writeString(mappingsPath.resolve("NHMD/NHMD Vascular Plants.sync-defaults.conf"), "pipeline=PIPE_COLLECTION\nstatus=STATUS_COLLECTION\nworkstation=WORK_COLLECTION\n");

        MappingService mappingService = new MappingService(new SpecifyMappingsProperties(withTrailingSlash(mappingsPath)));
        CollectionObjectAttachment collectionObjectAttachment = new CollectionObjectAttachment();
        collectionObjectAttachment.attachment = attachment;
        CollectionObject collectionObject = new CollectionObject();
        collectionObject.collectionobjectattachments = List.of(collectionObjectAttachment);
        collectionObject.catalognumber = "NHMD0002";
        collectionObject.timestampmodified = "2026-02-03T05:09:28";

        var mapped = mappingService.mapAsset(collectionObject);

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().asset.pipeline).isEqualTo("PIPE_COLLECTION");
        assertThat(mapped.getFirst().asset.status).isEqualTo("STATUS_COLLECTION");
        assertThat(mapped.getFirst().asset.workstation).isEqualTo("WORK_COLLECTION");
    }

    @Test
    void mapAssetFailsWhenInstitutionDefaultSyncDefaultsIsMissing() throws IOException {
        Path mappingsPath = createSyncMappingDirectory();
        writeCollectionMappingFile(mappingsPath);

        MappingService mappingService = new MappingService(new SpecifyMappingsProperties(withTrailingSlash(mappingsPath)));

        Attachment attachment = new Attachment();
        attachment.attachmentlocation = "guid-from-attachment";
        CollectionObjectAttachment collectionObjectAttachment = new CollectionObjectAttachment();
        collectionObjectAttachment.attachment = attachment;
        CollectionObject collectionObject = new CollectionObject();
        collectionObject.collectionobjectattachments = List.of(collectionObjectAttachment);
        collectionObject.catalognumber = "NHMD0003";
        collectionObject.timestampmodified = "2026-02-03T05:09:28";

        Throwable thrown = org.junit.jupiter.api.Assertions.assertThrows(SpecifyAdapterException.class, () -> mappingService.mapAsset(collectionObject));
        assertThat(thrown).hasMessageThat().contains("No sync defaults found for institution: NHMD");
    }

    private Path createSyncMappingDirectory() throws IOException {
        Path mappingsPath = tempDir.resolve("mappings");
        Files.createDirectories(mappingsPath.resolve("NHMD"));
        return mappingsPath;
    }

    private void writeCollectionMappingFile(Path mappingsPath) throws IOException {
        Files.writeString(
                mappingsPath.resolve("NHMD/default.conf"),
                "attachmentlocation=${asset_guid}\n" +
                "title=${pipeline}\n" +
                "remarks=${status}\n" +
                "origfilename=${workstation}\n"
        );
    }

    private void writeSyncDefaultsFile(Path mappingsPath, String values) throws IOException {
        Files.writeString(mappingsPath.resolve("NHMD/default.sync-defaults.conf"), values);
    }

    private String withTrailingSlash(Path path) {
        String value = path.toString().replace('\\', '/');
        if (value.endsWith("/")) {
            return value;
        }
        return value + "/";
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
        TemporalAccessor parse = specifyDateFormat.parse("2026-04-09T14:42:43.154630");
        System.out.println(parse);
//        mappedAsset.SpecifyModifiedDate = Instant.from(specifyDateFormat.parse(timestampToParse));
        System.out.println(Instant.from(specifyDateFormat.parse("2026-04-09T14:42:43.154630")));
    }
    ;
//    public static Instant format(String isoZ) {
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ssZ");
//        TemporalAccessor parse = dateTimeFormatter.parse("2017-08-27T17:43:11Z");
//        return null;
//    }

    @Test
    void getSpecifyDateValueMapsFilecreateddateToDateAssetTaken() {
        MappingService mappingService = new MappingService(new SpecifyMappingsProperties("./mappings/"));
        Attachment attachment = new Attachment();
        attachment.filecreateddate = "2024-01-31";

        Instant mappedDate = mappingService.getSpecifyDateValue("filecreateddate", attachment);

        assertThat(mappedDate).isEqualTo(Instant.parse("2024-01-31T00:00:00Z"));
    }

    @Test
    void getSpecifyDateValueReturnsNullWhenFilecreateddateIsNull() {
        MappingService mappingService = new MappingService(new SpecifyMappingsProperties("./mappings/"));
        Attachment attachment = new Attachment();

        Instant mappedDate = mappingService.getSpecifyDateValue("filecreateddate", attachment);

        assertThat(mappedDate).isNull();
    }

    @Test
    void mapAssetFromContextSetsDigitiserFromModifiedByAgentName() throws IOException {
        Path mappingsPath = createSyncMappingDirectory();
        writeCollectionMappingFile(mappingsPath);
        writeSyncDefaultsFile(mappingsPath, "pipeline=PIPE_DEFAULT\nstatus=STATUS_DEFAULT\nworkstation=WORK_DEFAULT\n");

        MappingService mappingService = new MappingService(new SpecifyMappingsProperties(withTrailingSlash(mappingsPath)));
        Attachment attachment = new Attachment();
        attachment.attachmentlocation = "guid-from-attachment";
        attachment.timestampmodified = "2026-02-03T05:09:28";

        CollectionObject collectionObject = new CollectionObject();
        collectionObject.catalognumber = "NHMD0004";
        collectionObject.timestampmodified = "2026-02-03T05:09:28";

        Agent modifiedByAgent = new Agent();
        modifiedByAgent.firstname = "Jane";
        modifiedByAgent.lastname = "Doe";

        SpecifyAttachmentContext context = new SpecifyAttachmentContext(attachment, modifiedByAgent, collectionObject, List.of(), 100L);

        var mappedAsset = mappingService.mapAssetFromContext(context);

        assertThat(mappedAsset.asset.digitiser).isEqualTo("Jane Doe");
        assertThat(mappedAsset.updatedFields).contains("${digitiser}");
    }

    @Test
    void mapAssetFromContextDoesNotSetDigitiserWhenAgentNameMissing() throws IOException {
        Path mappingsPath = createSyncMappingDirectory();
        writeCollectionMappingFile(mappingsPath);
        writeSyncDefaultsFile(mappingsPath, "pipeline=PIPE_DEFAULT\nstatus=STATUS_DEFAULT\nworkstation=WORK_DEFAULT\n");

        MappingService mappingService = new MappingService(new SpecifyMappingsProperties(withTrailingSlash(mappingsPath)));
        Attachment attachment = new Attachment();
        attachment.attachmentlocation = "guid-from-attachment";
        attachment.timestampmodified = "2026-02-03T05:09:28";

        CollectionObject collectionObject = new CollectionObject();
        collectionObject.catalognumber = "NHMD0005";
        collectionObject.timestampmodified = "2026-02-03T05:09:28";

        Agent modifiedByAgent = new Agent();
        modifiedByAgent.firstname = "  ";
        modifiedByAgent.lastname = "";

        SpecifyAttachmentContext context = new SpecifyAttachmentContext(attachment, modifiedByAgent, collectionObject, List.of(), 101L);

        var mappedAsset = mappingService.mapAssetFromContext(context);

        assertThat(mappedAsset.asset.digitiser).isNull();
        assertThat(mappedAsset.updatedFields).doesNotContain("${digitiser}");
    }
}
