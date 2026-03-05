package dk.northtech.dassco_specify_adapter.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Event {
    public String user;
    public Instant timestamp;
    public DasscoEvent event;
    public String pipeline;
    public List<String> change_list = new ArrayList<>();
    public String bulk_update_uuid;



    @JdbiConstructor
    public Event(String user, Instant timestamp, DasscoEvent event, String pipeline, List<String> change_list, String bulk_update_uuid) {
        this.timestamp = timestamp;
        this.event = event;
        this.user = user;
        this.pipeline = pipeline;
        this.change_list = change_list;
        this.bulk_update_uuid = bulk_update_uuid;
    }

    public Event() {
    }

    public Event(String user, Instant timestamp, DasscoEvent event, String pipeline) {
        this.timestamp = timestamp;
        this.event = event;
        this.user = user;
        this.pipeline = pipeline;
    }

    public Event(String user, Instant timestamp, DasscoEvent event) {
        this.user = user;
        this.timestamp = timestamp;
        this.event = event;
    }

    @Override
    public String toString() {
        return "Event{" +
                "user='" + user + '\'' +
                ", timeStamp=" + timestamp +
                ", event=" + event +
                ", pipeline='" + pipeline + '\'' +
                ", change_list=" + change_list +
                '}';
    }
}
