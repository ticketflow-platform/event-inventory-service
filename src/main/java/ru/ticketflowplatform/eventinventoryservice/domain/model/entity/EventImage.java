package ru.ticketflowplatform.eventinventoryservice.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(schema = "events", name = "events_images")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventImage {

    @EmbeddedId
    private EventImageId eventImageId;

    @Enumerated(STRING)
    private ImageType type;

    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = LAZY)
    @MapsId("imageId")
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Embeddable
    public static class EventImageId {
        private UUID eventId;
        private UUID imageId;
    }
}
