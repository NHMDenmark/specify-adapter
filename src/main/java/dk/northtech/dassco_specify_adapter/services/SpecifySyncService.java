package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.AMQP.QueueBroadcaster;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
import dk.northtech.dassco_specify_adapter.domain.specify.LoginInfo;
import dk.northtech.dassco_specify_adapter.domain.specify.SpecifyCollectionLogin;
import dk.northtech.dassco_specify_adapter.domain.sync.*;
import dk.northtech.dassco_specify_adapter.repository.SpecifyArsSyncRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SpecifySyncService {
    private static final Logger log = LoggerFactory.getLogger(SpecifySyncService.class);
    private final QueueBroadcaster queueBroadcaster;
    private final SpecifyEndpointService specifyEndpointService;
    private final MappingService mappingService;
    private final SpecifyQueryService specifyQueryService;
    private final SpecifyTargetResolverService specifyTargetResolverService;
    private final Jdbi jdbi;
    // Specify timestamps is in the local timezone.
    private final DateTimeFormatter specifyDateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneId.of("Europe/Copenhagen"));
    private static final Instant DEFAULT_SYNC_MILLIS = Instant.ofEpochMilli(1782746709772L); //29 Jun 2026

    @Inject
    public SpecifySyncService(QueueBroadcaster queueBroadcaster, SpecifyEndpointService specifyEndpointService, MappingService mappingService, SpecifyQueryService specifyQueryService, SpecifyTargetResolverService specifyTargetResolverService, Jdbi jdbi) {
        this.queueBroadcaster = queueBroadcaster;
        this.specifyEndpointService = specifyEndpointService;
        this.mappingService = mappingService;
        this.specifyQueryService = specifyQueryService;
        this.specifyTargetResolverService = specifyTargetResolverService;
        this.jdbi = jdbi;
    }


    public void arsToSpecifySync(String arsUpdateJson) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                //We do not control what specify returns so this is set to false to avoid breaking the application if Specify adds a field.
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ARSUpdate arsUpdate = mapper.readValue(arsUpdateJson, ARSUpdate.class);
            try {
                ResolvedSpecifyTarget target = specifyTargetResolverService.resolveForAsset(arsUpdate.asset);
                CollectionObjectAttachment attachment = mappingService.getAttachment(arsUpdate.asset);
                List<AssetSpecimen> specimen = specifyEndpointService.pushAssetToSpecify(attachment, arsUpdate.asset);
                Instant syncTimestamp = Instant.now();
                jdbi.withHandle(handle -> {
                    SpecifyArsSyncRepository repository = handle.attach(SpecifyArsSyncRepository.class);
                    for (AssetSpecimen specimenEntry : specimen) {
                        if (specimenEntry.specify_collection_object_attachment_id == null) {
                            continue;
                        }

                        Instant modifiedTimestamp = Instant.from(specifyDateFormat.parse(specimenEntry.specifyAttachmentModifiedTimestamp));
                        repository.insertSyncLog(new SpecifySyncLogEntry(
                                null,
                                modifiedTimestamp,
                                SpecifySyncStatus.SUCCEEDED,
                                specimenEntry.specify_collection_object_attachment_id,
                                null,
                                syncTimestamp,
                                null,
                                target.collectionConfig().id(),
                                normalizeArsAssetGuidForSyncLog(arsUpdate.asset.asset_guid),
                                SyncDirection.ARS_TO_SPECIFY
                        ));
                    }
                    return handle;
                });
                queueBroadcaster.sendMessage(new Acknowledge(arsUpdate.asset.asset_guid, AcknowledgeStatus.SUCCESS, null, syncTimestamp, specimen));
            } catch (SpecifyAdapterException spx) {
                queueBroadcaster.sendMessage(new Acknowledge(arsUpdate.asset.asset_guid, spx.status(), spx.getMessage(), Instant.now(), null));
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                queueBroadcaster.sendMessage(new Acknowledge(arsUpdate.asset.asset_guid, AcknowledgeStatus.UNKNOWN_ERROR, "Error syncing file to specify, please check the logs of Specify Bridge", Instant.now(), null));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void specifyToArsSync() {
        Instant now = Instant.now();
        for (ResolvedSpecifyTarget target : specifyTargetResolverService.listSpecifyToArsTargets()) {
            try {
                syncSpecifyTargetToArs(target, now);
            } catch (Exception exception) {
                log.error("Specify to ARS sync failed for institution {} collection {}", target.institutionConfig().name(), target.collectionConfig().name(), exception);
                createFailedBatch(target.collectionConfig().id(), now, exception.getMessage());
            }
        }
    }

    private void syncSpecifyTargetToArs(ResolvedSpecifyTarget target, Instant now) {
        Optional<SpecifyArsSyncBatch> latestSuccessfulBatch = getLatestSuccessfulBatch(target.collectionConfig().id());
        Instant fromInstant = latestSuccessfulBatch
                .map(SpecifyArsSyncBatch::specify_to_timestamp)
                .orElse(DEFAULT_SYNC_MILLIS);
        try {
            List<SpecifyAttachmentContext> attachmentContextsToSync = specifyQueryService.findUpdatedAttachmentContextsSince(target, fromInstant);
            List<SpecifySyncLogEntry> specifySyncLogEntries = new ArrayList<>();
            log.info("Found {} attachment contexts to sync for institution {} collection {}", attachmentContextsToSync.size(), target.institutionConfig().name(), target.collectionConfig().name());


//            List<MappedAsset> mappedAssets = collectionObjectsToSync.stream()
//                    .flatMap(collectionObject -> mappingService.mapAsset(collectionObject).stream())
//                    .toList();
            Map<Long, MappedAsset> entryIdAsset = new HashMap<>();
            Instant finalFromInsant = fromInstant;
            jdbi.inTransaction(handle -> {
                SpecifyArsSyncRepository repository = handle.attach(SpecifyArsSyncRepository.class);
                Integer batchId = repository.createNewBatch(new SpecifyArsSyncBatch(null, now, finalFromInsant, now, SpecifyArsSyncBatchStatus.STARTED, null, target.collectionConfig().id(), specifySyncLogEntries));
                int[] totalLogEntriesCreated = {0};
                int[] startedEntries = {0};
                int[] failedEntries = {0};

                attachmentContextsToSync.stream()
                        .map(context -> mappingService.mapAssetFromContext(context, target.institutionConfig().name(), target.collectionConfig().name()))
                        .filter(mappedAsset -> !wasRecentlySyncedFromArs(repository, mappedAsset, target.collectionConfig().id()))
                        .forEach(
                mappedAsset -> {
                    SpecifySyncStatus specifySyncStatus = mappedAsset.error == null ? SpecifySyncStatus.STARTED : SpecifySyncStatus.FAILED;
                    totalLogEntriesCreated[0]++;
                    if (specifySyncStatus == SpecifySyncStatus.STARTED) {
                        startedEntries[0]++;
                    } else {
                        failedEntries[0]++;
                    }
                    SpecifySyncLogEntry specifySyncLogEntry = new SpecifySyncLogEntry(null
                            , mappedAsset.SpecifyModifiedDate
                            , specifySyncStatus
                            , mappedAsset.specifyCollectionObjectAttachmentId
                            , mappedAsset.error
                            , now
                            , batchId
                            , target.collectionConfig().id()
                            , normalizeArsAssetGuidForSyncLog(mappedAsset.asset.asset_guid)
                            , SyncDirection.SPECIFY_TO_ARS);
                    Long entryId = repository.insertSyncLog(specifySyncLogEntry);
                    entryIdAsset.put(entryId, mappedAsset);
                });
                if (totalLogEntriesCreated[0] == 0) {
                    repository.updateSpecifyArsSyncBatch(batchId, SpecifyArsSyncBatchStatus.EMPTY, "No new attachments found");
                } else if (startedEntries[0] == 0 && failedEntries[0] > 0) {
                    repository.updateSpecifyArsSyncBatch(batchId, SpecifyArsSyncBatchStatus.FAILED, "All entries failed before queue dispatch");
                }
                handle.commit();
                return handle;
            });
            entryIdAsset.forEach((entryId, mappedAsset) -> {

                    if (mappedAsset.error == null) {
                        try {
                            queueBroadcaster.sendMessage(new SpecifyArsSyncMessage(mappedAsset.asset, mappedAsset.updatedFields, entryId));
                        } catch (Exception e) {
                            mappedAsset.error = e.getMessage();
                            //TODO set err
//                            specifySyncStatus = SpecifySyncStatus.FAILED;
                        }
                    }
            });


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createFailedBatch(Long collectionConfigId, Instant now, String message) {
        Instant fromInstant = getLatestSuccessfulBatch(collectionConfigId)
                .map(SpecifyArsSyncBatch::specify_to_timestamp)
                .orElse(DEFAULT_SYNC_MILLIS);
        jdbi.withHandle(handle -> {
            SpecifyArsSyncRepository attach = handle.attach(SpecifyArsSyncRepository.class);
            attach.createNewBatch(new SpecifyArsSyncBatch(null, now, fromInstant, now, SpecifyArsSyncBatchStatus.FAILED, message, collectionConfigId, List.of()));
            return handle;
        });
    }

    private boolean wasRecentlySyncedFromArs(SpecifyArsSyncRepository repository, MappedAsset mappedAsset, Long collectionConfigId) {
        if (mappedAsset.specifyCollectionObjectAttachmentId == null || mappedAsset.SpecifyModifiedDate == null) {
            return false;
        }
        // Any specify update that is distinct from the ARS update will be synced as we cant determine what is most important (Specify is master for some of the data, ARS for other).
        Instant fromTimestamp = mappedAsset.SpecifyModifiedDate.minusSeconds(1);
        Instant toTimestamp = mappedAsset.SpecifyModifiedDate.plusSeconds(1);
        boolean recentlySynced = repository.hasArsToSpecifySyncNearTimestamp(mappedAsset.specifyCollectionObjectAttachmentId
                , collectionConfigId
                , fromTimestamp
                , toTimestamp);
        if (recentlySynced) {
            log.info("Skipping collection object attachment {} because it was recently synced from ARS to Specify around {}", mappedAsset.specifyCollectionObjectAttachmentId, mappedAsset.SpecifyModifiedDate);
        }
        return recentlySynced;
    }

    private String normalizeArsAssetGuidForSyncLog(String assetGuid) {
        if (assetGuid == null) {
            return null;
        }
        String normalized = assetGuid.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        int lastDotIndex = normalized.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return normalized.substring(0, lastDotIndex);
        }
        return normalized;
    }

    private String mergeGuidWithPreviousFileEnding(String incomingAssetGuid, String existingSyncLogAssetGuid) {
        if (incomingAssetGuid == null) {
            return null;
        }
        if (existingSyncLogAssetGuid == null || existingSyncLogAssetGuid.isBlank()) {
            return incomingAssetGuid;
        }
        int lastDotIndex = existingSyncLogAssetGuid.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == existingSyncLogAssetGuid.length() - 1) {
            return incomingAssetGuid;
        }
        String ending = existingSyncLogAssetGuid.substring(lastDotIndex + 1).trim();
        if (ending.isEmpty()) {
            return incomingAssetGuid;
        }
        return incomingAssetGuid + "." + ending;
    }

    public void handleAcknowledge(SyncAcknowledge acknowledge) {
        jdbi.withHandle(h -> {
            int countNotStarted = 0;
            int countSuccess = 0;
            int countFailed = 0;
            Integer specifySyncBatchId = null;
            SpecifyArsSyncRepository syncRepository = h.attach(SpecifyArsSyncRepository.class);
            List<SpecifySyncLogEntry> entries = syncRepository.getSyncLogEntriesBySyncLogId(acknowledge.specifySyncLogId());
            log.info("Found {} SyncLogEntries", entries.size());
            if (!entries.isEmpty()) {
                specifySyncBatchId = entries.getFirst().specify_ars_sync_batch_id();
            }
            for (SpecifySyncLogEntry entry : entries) {
                SpecifySyncLogEntry syncLogEntry = entry;
                log.info("Found SyncLogEntry {}", syncLogEntry);
                if (entry.specify_sync_log_id().equals(acknowledge.specifySyncLogId())) {
                    log.info("Found matching synclog");
                    log.info("Icoming guid {}", acknowledge.assetGuid());

                    String incomingAssetGuid = normalizeArsAssetGuidForSyncLog(acknowledge.assetGuid());
                    String syncLogAssetGuid = syncLogEntry.ars_asset_guid();
                    log.info("Found incoming asset guid: {}", incomingAssetGuid);
                    log.info("Found synclog asset guid: {}", syncLogAssetGuid);
                    if (incomingAssetGuid != null && !Objects.equals(syncLogAssetGuid, incomingAssetGuid)) {
                        log.info("Guid is different from incoming asset guid {}", incomingAssetGuid);
                        if (syncLogEntry.specify_collection_object_attachment_id() != null) {
                            log.info("Updateting collection object attachment");

                            CollectionObjectAttachment updatedAttachment = updateSpecifyAttachmentGuid(syncLogEntry.specify_collection_object_attachment_id(), incomingAssetGuid, syncLogEntry.collectionId());
                            if (updatedAttachment != null && updatedAttachment.attachment != null && updatedAttachment.attachment.timestampmodified != null) {

                                Instant modifiedTimestamp = Instant.from(specifyDateFormat.parse(updatedAttachment.attachment.timestampmodified));
                                syncRepository.insertSyncLog(new SpecifySyncLogEntry(
                                        null,
                                        modifiedTimestamp,
                                        SpecifySyncStatus.SUCCEEDED,
                                        syncLogEntry.specify_collection_object_attachment_id(),
                                        null,
                                        Instant.now(),
                                        null,
                                        syncLogEntry.collectionId(),
                                        incomingAssetGuid,
                                        SyncDirection.ARS_TO_SPECIFY
                                ));
                            }
                        }
                    }
                    syncLogEntry = new SpecifySyncLogEntry(syncLogEntry.specify_sync_log_id()
                            , syncLogEntry.specify_modified_date()
                            , acknowledge.specifySyncStatus()
                            , syncLogEntry.specify_collection_object_attachment_id()
                            , acknowledge.additionalInfo()
                            , syncLogEntry.sync_attempt_update_timestamp()
                            , syncLogEntry.specify_ars_sync_batch_id()
                            , syncLogEntry.collectionId()
                            , syncLogEntry.ars_asset_guid()
                            , syncLogEntry.sync_direction());
                    syncRepository.updateSyncLog(syncLogEntry);
                }
                if (!SpecifySyncStatus.STARTED.equals(syncLogEntry.status())) {
                    countNotStarted++;
                }
                if (SpecifySyncStatus.FAILED.equals(syncLogEntry.status())) {
                    countFailed++;
                }
                if (SpecifySyncStatus.SUCCEEDED.equals(syncLogEntry.status())) {
                    countSuccess++;
                }
            }
            // Set batch status when all started synchronisations are accounted for
            if (countNotStarted == entries.size() && specifySyncBatchId != null) {
                if (countSuccess == entries.size()) {
                    syncRepository.updateSpecifyArsSyncBatch(specifySyncBatchId, SpecifyArsSyncBatchStatus.SUCCEEDED, null);
                } else if (countFailed == entries.size()) {
                    syncRepository.updateSpecifyArsSyncBatch(specifySyncBatchId, SpecifyArsSyncBatchStatus.FAILED, "All assets failed");
                } else {
                    syncRepository.updateSpecifyArsSyncBatch(specifySyncBatchId, SpecifyArsSyncBatchStatus.FAILED_ENTRIES, "Some assets failed");
                }
            }
            return h;
        });
    }


    private CollectionObjectAttachment updateSpecifyAttachmentGuid(Long specifyCollectionObjectAttachmentId, String incomingAssetGuid, Long collectionConfigId) {
        ResolvedSpecifyTarget target = specifyTargetResolverService.getTargetByCollectionConfigId(collectionConfigId)
                .orElseThrow(() -> new RuntimeException("No collection config found for collection id " + collectionConfigId));
        LoginInfo loginInfo = specifyEndpointService.login(target.institutionConfig().id());
        Integer specifyCollectionId = loginInfo.collections.get(target.collectionConfig().name());
        if (specifyCollectionId == null) {
            throw new RuntimeException("No collection was found in specify");
        }
        SpecifyCollectionLogin specifyLogin = specifyEndpointService.loginToCollection(target.institutionConfig().id(), specifyCollectionId, loginInfo.csrftoken);
        CollectionObjectAttachment collectionObjectAttachment = specifyEndpointService.getSpecifyObject(
                specifyLogin,
                "/api/specify/collectionobjectattachment/" + specifyCollectionObjectAttachmentId + "/",
                CollectionObjectAttachment.class
        );
        if (collectionObjectAttachment.attachment == null) {
            throw new RuntimeException("No attachment was found on collectionObjectAttachment " + specifyCollectionObjectAttachmentId);
        }

        String updatedAssetGuid = mergeGuidWithPreviousFileEnding(incomingAssetGuid, collectionObjectAttachment.attachment.attachmentlocation);
        collectionObjectAttachment.attachment.attachmentlocation = updatedAssetGuid;
        return specifyEndpointService.putCollectionObjectAttachment(collectionObjectAttachment, specifyLogin);
    }

    public Optional<SpecifyArsSyncBatch> getLatestSuccessfulBatch(Long collectionConfigId) {
        return jdbi.withHandle(h -> {
            SpecifyArsSyncRepository repository = h.attach(SpecifyArsSyncRepository.class);
            SpecifyArsSyncBatch latestNonFailed = repository.getLatestNonFailedByCollectionId(collectionConfigId);
            if (latestNonFailed == null) {
                return Optional.empty();
            }
            return Optional.of(latestNonFailed);
        });
    }

    public List<SpecifyArsSyncBatch> listSyncBatches() {
        return jdbi.withHandle(h -> {
            SpecifyArsSyncRepository repository = h.attach(SpecifyArsSyncRepository.class);
            return repository.getSyncBatches();
        });
    }

    public List<SpecifyArsSyncBatch> listSyncBatches(Integer offset, Integer limit) {
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return jdbi.withHandle(h -> {
            SpecifyArsSyncRepository repository = h.attach(SpecifyArsSyncRepository.class);
            return repository.getSyncBatchesPaged(safeLimit, safeOffset);
        });
    }

    public List<SpecifySyncLogEntry> listSyncBatchEntries(Integer batchId) {
        return jdbi.withHandle(h -> {
            SpecifyArsSyncRepository repository = h.attach(SpecifyArsSyncRepository.class);
            return repository.getSyncLog(batchId);
        });
    }

    public List<SpecifySyncLogEntry> listSyncBatchEntries(Integer batchId, Integer offset, Integer limit) {
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return jdbi.withHandle(h -> {
            SpecifyArsSyncRepository repository = h.attach(SpecifyArsSyncRepository.class);
            return repository.getSyncLogPaged(batchId, safeLimit, safeOffset);
        });
    }

    SpecifyArsSyncBatch startSyncBatch(SpecifyArsSyncBatch syncBatch) {
        if (syncBatch.entries() == null || syncBatch.entries().isEmpty()) {
            throw new RuntimeException("Sync batch must have entries");
        }
        List<SpecifySyncLogEntry> entries = new ArrayList<>();
        return jdbi.inTransaction(h -> {
            SpecifyArsSyncRepository repo = h.attach(SpecifyArsSyncRepository.class);
            Integer newBatchId = repo.createNewBatch(syncBatch);
            syncBatch.entries().forEach(entry -> {
                Long new_entry_id = repo.insertSyncLog(new SpecifySyncLogEntry(entry, newBatchId, null));
                entries.add(new SpecifySyncLogEntry(entry, newBatchId, new_entry_id));
            });
            return new SpecifyArsSyncBatch(newBatchId, syncBatch.batch_timestamp(), syncBatch.specify_from_timestamp(), syncBatch.specify_to_timestamp(), syncBatch.status(), syncBatch.additional_info(), syncBatch.collectionId(), entries);
        });
    }
}
