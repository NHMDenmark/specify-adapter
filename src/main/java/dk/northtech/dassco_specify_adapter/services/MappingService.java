package dk.northtech.dassco_specify_adapter.services;

import com.google.common.base.Strings;
import dk.northtech.dassco_specify_adapter.assets.SpecifyMappingsProperties;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObject;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
import dk.northtech.dassco_specify_adapter.domain.sync.MappedAsset;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MappingService {
    private final SpecifyMappingsProperties specifyMappingsProperties;
    private static final Logger logger = LoggerFactory.getLogger(MappingService.class);
    private DateTimeFormatter format = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    private final DateTimeFormatter specifyDateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneId.of("Europe/Copenhagen"));

    private static final String NHMD = "NHMD";
    private static final String VASCULAR_PLANTS_COLLECTION = "NHMD Vascular Plants";

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

    public List<MappedAsset> mapAsset(CollectionObject collectionObject) {
        String values = readConfigARSToSpecify(NHMD, VASCULAR_PLANTS_COLLECTION);
        values = values.replace("\r\n", "${split}")
                .replace("\n", "${split}");
        Map<String, String> specifyArsValues = getMappedValues(values);
        logger.info("collection o {}", collectionObject);
        ArrayList<MappedAsset> mappedAssets = new ArrayList<>();
        for (CollectionObjectAttachment collectionObjectAttachment : collectionObject.collectionobjectattachments) {
            logger.info("mapping attachment {}", collectionObjectAttachment);
            MappedAsset mappedAsset = new MappedAsset();
            mappedAsset.asset = new Asset();
            specifyArsValues.forEach((mappedKey, mappedValue) -> {
                mappedAsset.attachment = collectionObjectAttachment.attachment;
                if (mappedValue.equals("${asset_guid}.${file_format}")) {
                    String specifyValue = getSpecifyStringValue(mappedKey, mappedAsset.attachment);
                    String[] split = specifyValue.split("\\.");
                    if (split.length == 2) {
                        mappedAsset.asset.asset_guid = split[0];
                        if(!Strings.isNullOrEmpty(split[1])){
                            mappedAsset.asset.file_formats.add(split[1].toUpperCase());
                        }
                    } else {
                        mappedAsset.error = "origfilename filename of specify asset did not follow the format ${asset_guid}.${file_format}, was: " + specifyValue;
                    }
                } else {
                    mapValueToAsset(mappedAsset, mappedValue, mappedKey);
                }

//                List<String> strings = resolveValuePattern(mappedValue);
//                for (int i = 0; i < strings.size(); i += 2) {
//                    Attachment attachment = collectionObjectAttachment.attachment;
//                    getSpecifyValue(mappedKey, attachment);
//                    String token = strings.get(i);
//                    if (token.startsWith("${")) {
//
//                    }
//                }
            });
            if(mappedAsset.asset.status == null) {
                //TODO what is the status actually
                mappedAsset.asset.status = "completed";
            }
            if(mappedAsset.asset.collection == null) {
                mappedAsset.asset.collection = VASCULAR_PLANTS_COLLECTION;
            }
            if(mappedAsset.asset.institution == null) {
                mappedAsset.asset.institution = NHMD;
            }
            HashSet<String> preparationTypes = new HashSet<>();
            //TODO Can we get this from specify
            preparationTypes.add("unknown");
            Specimen specimen = new Specimen(NHMD, VASCULAR_PLANTS_COLLECTION, collectionObject.catalognumber, "NHMD.NHMD Vascular Plants" + collectionObject.catalognumber, preparationTypes, null, null, List.of());
            AssetSpecimen assetSpecimen = new AssetSpecimen(false, collectionObjectAttachment.id, "unknown", specimen.specimen_pid(), mappedAsset.asset.asset_guid);
            assetSpecimen.specimen = specimen;
            mappedAsset.asset.asset_specimen.add(assetSpecimen);
            mappedAsset.specifyCollectionObjectAttachmentId = collectionObjectAttachment.id;
            System.out.println(" Tsest          tezt  " + collectionObject.timestampmodified);
            mappedAsset.SpecifyModifiedDate = Instant.from(specifyDateFormat.parse(collectionObject.timestampmodified));
            mappedAssets.add(mappedAsset);
            mappedAsset.updatedFields.addAll(specifyArsValues.values());
        }

        return mappedAssets;
    }

    public void mapValueToAsset(MappedAsset mappedAsset, String arsProperty, String specifyProperty) {
        try {

            switch (arsProperty) {
                case "${file_format}":
                    String specifyStringValue = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    if(!Strings.isNullOrEmpty(specifyStringValue)) {
                        mappedAsset.asset.file_formats.add(specifyStringValue.toUpperCase());
                    }
                    break;
                case "${asset_guid}":
                    mappedAsset.asset.asset_guid = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${asset_pid}":
                    mappedAsset.asset.asset_pid = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${make_public}":
                    mappedAsset.asset.make_public = getSpecifyBooleanValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${date_asset_deleted_ars}":
                    mappedAsset.asset.date_asset_deleted_ars = getSpecifyDateValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${date_asset_taken}":
                    mappedAsset.asset.date_asset_taken = getSpecifyDateValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${legality.copyright}":
                    String copyright = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    if (copyright == null) {
                        break;
                    }
                    if (mappedAsset.asset.legality == null) {
                        mappedAsset.asset.legality = new Legality();
                    }
                    mappedAsset.asset.legality.copyright = copyright;
                    break;

                case "${legality.credit}":
                    String credit = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    if (credit == null) {
                        break;
                    }
                    if (mappedAsset.asset.legality == null) {
                        mappedAsset.asset.legality = new Legality();
                    }
                    mappedAsset.asset.legality.credit = credit;
                    break;

                case "${legality.license}":
                    String license = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    if (license == null) {
                        break;
                    }
                    if (mappedAsset.asset.legality == null) {
                        mappedAsset.asset.legality = new Legality();
                    }
                    mappedAsset.asset.legality.license = license;
                    break;

                case "${specify_attachment_remarks}":
                    mappedAsset.asset.specify_attachment_remarks = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${specify_attachment_title}":
                    mappedAsset.asset.specify_attachment_title = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${pipeline}":
                    mappedAsset.asset.pipeline = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${metadata_updated_by}":
                    mappedAsset.asset.metadata_updated_by = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${mos_id}":
                    mappedAsset.asset.mos_id = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${metadata_source}":
                    mappedAsset.asset.metadata_source = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${metadata_version}":
                    mappedAsset.asset.metadata_version = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${camera_setting_control}":
                    mappedAsset.asset.camera_setting_control = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${workstation}":
                    mappedAsset.asset.workstation = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${date_audited}":
                    mappedAsset.asset.date_audited = getSpecifyDateValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${status}":
                    mappedAsset.asset.status = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${institution}":
                    mappedAsset.asset.institution = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${collection}":
                    mappedAsset.asset.collection = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                case "${payload_type}":
                    mappedAsset.asset.payload_type = getSpecifyStringValue(specifyProperty, mappedAsset.attachment);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown placeholder: " + arsProperty);
            }
        } catch (RuntimeException e) {
            mappedAsset.error = e.getMessage();
        }
    }

    public Instant getSpecifyDateValue(String token, Attachment attachment) {
        if (token == null) {
            throw new IllegalArgumentException("token is null");
        }
        String date = switch (token) {
            case "copyrightdate" -> attachment.copyrightdate;
            default -> null;
        };
        if (!Strings.isNullOrEmpty(date)) {
            return Instant.parse(date);
        }
        return null;
    }

    public String getSpecifyStringValue(String token, Attachment attachment) {
        if (token == null) {
            throw new IllegalArgumentException("token is null");
        }
        String s = switch (token) {
            case "attachmentlocation" -> attachment.attachmentlocation;
            case "origfilename" -> attachment.origfilename;
            case "copyrightdate" -> attachment.copyrightdate;
            case "mimetype" -> attachment.mimetype;
            case "copyrightholder" -> attachment.copyrightholder;
            case "credit" -> attachment.credit;
            case "license" -> attachment.license;
            case "ispublic" -> attachment.ispublic + "";
            case "remarks" -> attachment.remarks;
            case "title" -> attachment.title;
            default -> null;
        };
        return !Strings.isNullOrEmpty(s) ? s.trim() : null;
    }

    public boolean getSpecifyBooleanValue(String token, Attachment attachment) {
        if (token == null) {
            throw new IllegalArgumentException("token is null");
        }
        return switch (token) {
            case "ispublic" -> attachment.ispublic;
            default -> throw new IllegalArgumentException("Unknown token: " + token);
        };
    }


    public List<String> resolveValuePattern(String placeholder) {
        // The specify value can be made out of multiple ARS values connected by constants, example: origfilename=${asset_guid}.${file_format}
        List<String> valueToken = new ArrayList<>();

        int index = 0;

        while (!placeholder.isEmpty()) {
            int tokenStart = placeholder.indexOf("${");
            int tokenEnd = placeholder.indexOf("}");
            if (tokenStart == -1) {
                valueToken.add(placeholder);
                break;
            }
            if (tokenStart != 0) {
                valueToken.add(placeholder.substring(0, tokenStart));
            }
            valueToken.add(placeholder.substring(tokenStart, tokenEnd + 1));
            placeholder = placeholder.substring(tokenEnd + 1);

            System.out.println(placeholder);
            System.out.println(index);
            System.out.println("tokenStart: " + tokenStart);
            System.out.println("tokenend" + tokenEnd);
            index = tokenEnd;

        }

        return valueToken;
    }

    public CollectionObjectAttachment mapAsset(Asset asset, String template) {
        //Replace placeholders with actual values from the asset
        String values = replacePlaceholders(template, asset);
        //Split the String in to key value pairs
        Map<String, String> mappedValues = getMappedValues(values);
        //Fill the collection attachment
        CollectionObjectAttachment collectionObjectAttachment = new CollectionObjectAttachment();
        collectionObjectAttachment.ordinal = 0;
//        collectionObjectAttachment.ars_collection = asset.collection;
//        collectionObjectAttachment.ars_institution = asset.institution;
//        collectionObjectAttachment.ars_assetguid = asset.asset_guid;
        if (asset.asset_specimen.isEmpty()) {
            throw new SpecifyAdapterException("No specimens found for asset: " + asset, AcknowledgeStatus.MAPPING_ERROR);
        }
//        collectionObjectAttachment.ars_barcode = asset.specimens.getFirst().barcode();
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
                .replace("${date_asset_deleted_ars}", formatDate(asset.date_asset_deleted_ars))
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
        baseAsset = baseAsset.replace("${pipeline}", asset.pipeline == null ? "" : asset.pipeline);
        baseAsset = baseAsset.replace("${metadata_updated_by}", asset.metadata_updated_by == null ? "" : asset.metadata_updated_by)
                .replace("${mos_id}", asset.mos_id == null ? "" : asset.mos_id)
                .replace("${metadata_source}", asset.metadata_source == null ? "" : asset.metadata_source)
                .replace("${metadata_version}", asset.metadata_version == null ? "" : asset.metadata_version)
                .replace("${camera_setting_control}", asset.camera_setting_control == null ? "" : asset.camera_setting_control)
                .replace("${workstation}", asset.workstation == null ? "" : asset.workstation)
                .replace("${date_audited}", formatDate(asset.date_audited))
                .replace("${status}", asset.status)
                .replace("${institution}", asset.institution)
                .replace("${collection}", asset.institution)
                .replace("${date_asset_deleted_ars}", formatDate(asset.date_asset_deleted_ars))
                .replace("${multi_specimen}", String.valueOf(asset.multi_specimen))
//                .replace("${internal_status}", String.valueOf(asset.internal_status))
                .replace("${payload_type}", asset.payload_type == null ? "" : asset.payload_type);
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
            if (!file.exists()) {
                throw new SpecifyAdapterException("No mapping found on: " + file, AcknowledgeStatus.MAPPING_ERROR);
            }
            return Files.readString(Path.of(file.getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readConfigARSToSpecify(String institution, String collection) {
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
            if (!file.exists()) {
                throw new SpecifyAdapterException("No mapping found on: " + file, AcknowledgeStatus.MAPPING_ERROR);
            }
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
