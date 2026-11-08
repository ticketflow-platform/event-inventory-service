package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage.EventImageId;

public interface EventImageRepository extends JpaRepository<@NonNull EventImage, @NonNull EventImageId> {
}
