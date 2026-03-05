package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.AMQP.QueueBroadcaster;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObject;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
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
    private final Jdbi jdbi;
    // Specify timestamps is in the local timezone.
    private final DateTimeFormatter specifyDateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneId.of("Europe/Copenhagen"));
    private static final Instant DEFAULT_SYNC_MILLIS = Instant.ofEpochMilli(1767272493000L);

    @Inject
    public SpecifySyncService(QueueBroadcaster queueBroadcaster, SpecifyEndpointService specifyEndpointService, MappingService mappingService, SpecifyQueryService specifyQueryService, Jdbi jdbi) {
        this.queueBroadcaster = queueBroadcaster;
        this.specifyEndpointService = specifyEndpointService;
        this.mappingService = mappingService;
        this.specifyQueryService = specifyQueryService;
        this.jdbi = jdbi;
    }


    public void sync(String arsUpdateJson) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ARSUpdate arsUpdate = mapper.readValue(arsUpdateJson, ARSUpdate.class);
            try {
                CollectionObjectAttachment attachment = mappingService.getAttachment(arsUpdate.asset);
                List<AssetSpecimen> specimen = specifyEndpointService.pushImageToSpecify(attachment, arsUpdate.asset, arsUpdate.deleteAttachment);
//                2026-02-03T05:09:29
                queueBroadcaster.sendMessage(new Acknowledge(arsUpdate.asset.asset_guid, AcknowledgeStatus.SUCCESS, null, Instant.now(), specimen));
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
        Optional<SpecifyArsSyncBatch> latestSuccessfulBatch = getLatestSuccessfulBatch();
        String specifyFromDate = null;
        Instant now = Instant.now();
        String specifyToDate = specifyDateFormat.format(now);

        Instant fromInstant = null;
        if (latestSuccessfulBatch.isPresent()) {
            SpecifyArsSyncBatch batch = latestSuccessfulBatch.get();
            fromInstant = batch.specify_to_timestamp();
        } else {
            fromInstant = DEFAULT_SYNC_MILLIS;
        }
        specifyFromDate = specifyDateFormat.format(fromInstant);
        try {
            List<CollectionObject> collectionObjectsToSync = specifyQueryService.findCollectionObjectsToSync(specifyFromDate, specifyToDate);
            List<SpecifySyncLogEntry> specifySyncLogEntries = new ArrayList<>();
            log.info("Found {} collectionObjects to sync", collectionObjectsToSync.size());


//            List<MappedAsset> mappedAssets = collectionObjectsToSync.stream()
//                    .flatMap(collectionObject -> mappingService.mapAsset(collectionObject).stream())
//                    .toList();
            Map<Long, MappedAsset> entryIdAsset = new HashMap<>();
            Instant finalFromInsant = fromInstant;
            jdbi.inTransaction(handle -> {
                SpecifyArsSyncRepository repository = handle.attach(SpecifyArsSyncRepository.class);
                Integer batchId = repository.createNewBatch(new SpecifyArsSyncBatch(null, now, finalFromInsant, now, SpecifyArsSyncBatchStatus.STARTED, null, specifySyncLogEntries));
                collectionObjectsToSync.stream()
                        .flatMap(collectionObject -> mappingService.mapAsset(collectionObject).stream()).forEach(
                mappedAsset -> {
                    SpecifySyncStatus specifySyncStatus = mappedAsset.error == null ? SpecifySyncStatus.STARTED : SpecifySyncStatus.FAILED;
                    SpecifySyncLogEntry specifySyncLogEntry = new SpecifySyncLogEntry(null
                            , mappedAsset.SpecifyModifiedDate
                            , specifySyncStatus
                            , mappedAsset.specifyCollectionObjectAttachmentId
                            , mappedAsset.error
                            , now
                            , batchId
                            , mappedAsset.asset.asset_guid
                            , SyncDirection.SPECIFY_TO_ARS);
                    Long entryId = repository.insertSyncLog(specifySyncLogEntry);
                    entryIdAsset.put(entryId, mappedAsset);
                });
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
            Instant finalFromInstant = fromInstant;
            jdbi.withHandle(handle -> {
                SpecifyArsSyncRepository attach = handle.attach(SpecifyArsSyncRepository.class);
                attach.createNewBatch(new SpecifyArsSyncBatch(null, now, finalFromInstant, now, SpecifyArsSyncBatchStatus.FAILED, e.getMessage()));
                return handle;
            });
            throw new RuntimeException(e);
        }


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
                    syncLogEntry = new SpecifySyncLogEntry(syncLogEntry.specify_sync_log_id()
                            , syncLogEntry.specify_modified_date()
                            , acknowledge.specifySyncStatus()
                            , syncLogEntry.specify_collection_object_attachment_id()
                            , acknowledge.additional_info()
                            , syncLogEntry.sync_attempt_update_timestamp()
                            , syncLogEntry.specify_ars_sync_batch_id()
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

    public Optional<SpecifyArsSyncBatch> getLatestSuccessfulBatch() {
        return jdbi.withHandle(h -> {
            SpecifyArsSyncRepository repository = h.attach(SpecifyArsSyncRepository.class);
            SpecifyArsSyncBatch latestNonFailed = repository.getLatestNonFailed();
            if (latestNonFailed == null) {
                return Optional.empty();
            }
            return Optional.of(latestNonFailed);
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
            return new SpecifyArsSyncBatch(newBatchId, syncBatch.batch_timestamp(), syncBatch.specify_from_timestamp(), syncBatch.specify_to_timestamp(), syncBatch.status(), syncBatch.additional_info(), entries);
        });
    }
}
