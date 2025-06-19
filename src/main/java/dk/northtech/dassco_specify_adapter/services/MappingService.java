package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.assets.SpecifyMappingsProperties;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MappingService {
    private final SpecifyMappingsProperties specifyMappingsProperties;
    private static final Logger logger = LoggerFactory.getLogger(MappingService.class);
    private DateTimeFormatter format = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    @Inject
    public MappingService(SpecifyMappingsProperties specifyMappingsProperties) {
        this.specifyMappingsProperties = specifyMappingsProperties;
    }

    // The mapping works by reading a template from the disk and replace the placeholders with values from the ARS asset metadata.
    // Example of a single line in template file before values are replaced: origfilename=${asset_guid}.${file_format}
    // After the template has been filled it is split by linebreaks and then the values after the equals sign is added to the CollectionObjectAttachment.
    public CollectionObjectAttachment getAttachment(Asset asset) {
        String template = readConfig(asset);
        CollectionObjectAttachment collectionObjectAttachment = mapAsset(asset, template);
        return collectionObjectAttachment;
    }

    public CollectionObjectAttachment mapAsset(Asset asset, String template) {
        //Replace placeholders with actual values from the asset
        String values = replacePlaceholders(template, asset);
        //Split the String in to key value pairs
        Map<String, String> mappedValues = getMappedValues(values);
        //Fill the collection attachment
        CollectionObjectAttachment collectionObjectAttachment = new CollectionObjectAttachment();
        collectionObjectAttachment.ordinal = 0;
        collectionObjectAttachment.ars_collection = asset.collection;
        collectionObjectAttachment.ars_institution = asset.institution;
        collectionObjectAttachment.ars_assetguid = asset.asset_guid;
        if(asset.specimens.isEmpty()) {
            throw new SpecifyAdapterException("No specimens found for asset: " + asset, AcknowledgeStatus.MAPPING_ERROR);
        }
        collectionObjectAttachment.ars_barcode = asset.specimens.getFirst().barcode();
        Attachment attachment = new Attachment();
        attachment.attachmentlocation = mappedValues.get("attachmentlocation");
        attachment.origfilename = mappedValues.get("origfilename");
        attachment.copyrightdate = mappedValues.get("copyrightdate");
        attachment.filecreateddate = mappedValues.get("filecreateddate");
        attachment.mimetype = mappedValues.get("mimetype");
        attachment.copyrightholder = mappedValues.get("copyrightholder");
        attachment.credit = mappedValues.get("credit");
        attachment.license = mappedValues.get("license");
        attachment.ispublic = Boolean.parseBoolean(mappedValues.get("ispublic"));
        attachment.remarks = mappedValues.get("remarks");
        attachment.title = mappedValues.get("title");
        collectionObjectAttachment.attachment = attachment;
        return collectionObjectAttachment;
    }

    public Map<String, String> getMappedValues(String values) {
        String[] valueLines = values.split("\\$\\{split}");
        return Arrays.stream(valueLines)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#")) // remove comments
                // anything before the equals sign is the key and anything after is the value to be added to the Attachment
                .map(line -> {
                    int index = line.indexOf("=");
                    if (index == -1) {
                        throw new SpecifyAdapterException("Mapping configuration error, missing equals, line is " + line, AcknowledgeStatus.MAPPING_ERROR);
                    }
                    String value = line.substring(index + 1);
                    if (value.isEmpty()) {
                        value = null;
                    }
                    return new String[]{line.substring(0, index), value};
                })
                // We filter on null because Collectors.toMap gives error on null values, even if key is present.
                .filter(line -> line[1] != null)
                // Create a lookup map
                .collect(Collectors.toMap(
                        splitLine -> splitLine[0]
                        , splitLine -> splitLine[1]));
    }

    public String replacePlaceholders(String template, Asset asset) {
        // Inserted values can have linebreaks. This replaces linebreaks with token that is unlikely to be used in data
        template = template.replace("\r\n", "${split}")
                .replace("\n", "${split}");
        //This uses a bruteforce way by replacing all viable placeholders
        String baseAsset = template.replace("${file_format}", asset.file_formats.isEmpty() ? "" : asset.file_formats.getFirst().toLowerCase())
                .replace("${asset_guid}", asset.asset_guid)
                .replace("${asset_pid}", asset.asset_pid)
                .replace("${make_public}", String.valueOf(asset.make_public))
                .replace("${date_asset_deleted}", formatDate(asset.date_asset_deleted))
                .replace("${date_asset_taken}", formatDate(asset.date_asset_taken));
        if (asset.legality != null) {
            baseAsset = baseAsset.replace("${legality.copyright}", asset.legality.copyright() == null ? "" : asset.legality.copyright())
                    .replace("${legality.credit}", asset.legality.credit() == null ? "" : asset.legality.credit())
                    .replace("${legality.license}", asset.legality.license() == null ? "" : asset.legality.license());
        } else {
            baseAsset = baseAsset.replace("${legality.copyright}", "")
                    .replace("${legality.credit}", "")
                    .replace("${legality.license}", "");
        }
        baseAsset = baseAsset.replace("${specify_attachment_remarks}", asset.specify_attachment_remarks == null ? "" : asset.specify_attachment_remarks)
                .replace("${specify_attachment_title}", asset.specify_attachment_title == null ? "" : asset.specify_attachment_title);
        return baseAsset;
    }

    public String readConfig(Asset asset) {
        String institution = asset.institution;
        String collection = asset.collection;
        File institutionConfig = new File(specifyMappingsProperties.location() + "institution");
        if (institutionConfig.exists()) {
            logger.info("No mapping found on: " + institutionConfig.toString());
            throw new SpecifyAdapterException("No mapping found for institution: " + institution, AcknowledgeStatus.MAPPING_ERROR);
        }
        File file = new File(specifyMappingsProperties.location() + institution + "/" + collection + ".conf");
        if (!file.exists()) {
            logger.info("No mapping found on: " + file + " using default.conf");
            file = new File(specifyMappingsProperties.location() + institution + "/default.conf");
        }
        try {
            return Files.readString(Path.of(file.getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ;

    public String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return format.format(instant);
    }
}
