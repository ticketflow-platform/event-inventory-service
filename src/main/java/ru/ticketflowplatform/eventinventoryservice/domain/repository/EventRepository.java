package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;

public interface EventRepository extends JpaRepository<@NonNull Event, @NonNull UUID> {
}
